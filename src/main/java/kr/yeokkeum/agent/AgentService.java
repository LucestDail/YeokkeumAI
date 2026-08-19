package kr.yeokkeum.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 자연어 지시 → 도구 라우팅. LLM 이 도구 카탈로그를 보고 {tool,args} JSON 을 결정한다.
 * 실행/HITL 은 AgentController 가 담당(라우팅만 여기서). 파싱 실패/무도구는 안전하게 null.
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final LlmGateway gateway;
    private final ToolRegistry registry;
    private final ObjectMapper om = new ObjectMapper();

    public AgentService(LlmGateway gateway, ToolRegistry registry) {
        this.gateway = gateway;
        this.registry = registry;
    }

    public record Decision(String tool, Map<String, Object> args, String raw) {}

    public Decision decide(String instruction) {
        String catalog = registry.all().stream()
                .map(t -> "- " + t.name() + (t.risky() ? " (변경)" : " (읽기)") + ": " + t.description())
                .collect(Collectors.joining("\n"));
        String system = "너는 공공기관 업무보조 AI의 도구 라우터다. 사용 가능한 도구:\n" + catalog
                + "\n\n사용자 요청에 가장 적합한 도구 1개와 인자를 JSON 으로만 답하라. "
                + "형식: {\"tool\":\"도구이름\",\"args\":{...}}. 적합한 도구가 없으면 {\"tool\":null}. "
                + "설명·마크다운 없이 JSON 객체만 출력하라.";
        ChatResult r = gateway.chat(List.of(ChatMessage.system(system), ChatMessage.user(instruction)), 0.0, 300);
        String raw = r.text() == null ? "" : r.text();
        String json = extractJson(raw);
        if (json == null) return new Decision(null, Map.of(), raw);
        try {
            JsonNode node = om.readTree(json);
            JsonNode toolNode = node.get("tool");
            if (toolNode == null || toolNode.isNull() || toolNode.asText().isBlank()) {
                return new Decision(null, Map.of(), raw);
            }
            Map<String, Object> args = new LinkedHashMap<>();
            JsonNode argsNode = node.get("args");
            if (argsNode != null && argsNode.isObject()) {
                argsNode.fields().forEachRemaining(e -> args.put(e.getKey(),
                        e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString()));
            }
            return new Decision(toolNode.asText(), args, raw);
        } catch (Exception e) {
            log.warn("도구 라우팅 JSON 파싱 실패: {}", e.getMessage());
            return new Decision(null, Map.of(), raw);
        }
    }

    /** 문자열에서 첫 균형중괄호 JSON 객체 추출(코드펜스/프로즈 방어). */
    static String extractJson(String s) {
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        boolean inStr = false, esc = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
            } else if (c == '"') {
                inStr = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return null;
    }
}
