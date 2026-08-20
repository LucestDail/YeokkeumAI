package kr.yeokkeum.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 정부24(plus.gov.kr) AI 검색 연동 도구 [외부 연계].
 * 2단계: makeCnvrsId → fabrix/chat(SSE)로 정부 민원·혜택 서비스 근거 기반 답변을 받아 반환.
 * 읽기(안전) 도구. 외부 공개 beta API(무인증) — 폐쇄망에선 미동작(정직 실패).
 */
@Component
public class Gov24SearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(Gov24SearchTool.class);
    private static final String BASE = System.getenv().getOrDefault("GOV24_BASE_URL", "https://plus.gov.kr");

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    @Override
    public String name() { return "gov24_search"; }

    @Override
    public String description() { return "정부24(plus.gov.kr) AI에 질의해 정부 민원·혜택·생활 서비스 정보를 근거 기반으로 검색·안내받는다. args: {query}"; }

    @Override
    public boolean risky() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        String query = Tool.str(args, "query");
        if (query.isBlank()) return "query 가 필요합니다.";
        try {
            String cid = makeConversation(query);
            return chat(cid, query);
        } catch (Exception e) {
            log.warn("정부24 연동 실패: {}", e.getMessage());
            return "정부24 연동에 실패했습니다(외부 API 오류·네트워크·폐쇄망): " + e.getMessage();
        }
    }

    private String makeConversation(String query) throws Exception {
        Map<String, Object> body = Map.of("cnvrsQstnCn", query, "mode", "search", "userId", "");
        HttpRequest req = base(BASE + "/ai/search_beta/chat/makeCnvrsId")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("makeCnvrsId HTTP " + res.statusCode());
        JsonNode n = om.readTree(res.body());
        if (!n.path("ok").asBoolean(false)) throw new IllegalStateException("makeCnvrsId ok=false");
        return n.path("cnvrsId").asText();
    }

    private String chat(String cid, String query) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("cnvrsId", cid);
        body.put("cnvrsQstnCn", query);
        body.put("modelSource", "samsung");
        body.put("location", "");
        body.put("mode", "search");
        body.put("userId", "");
        body.put("unityLgnUseYn", "");
        HttpRequest req = base(BASE + "/ai/search_beta/api/fabrix/chat")
                .timeout(Duration.ofSeconds(90))
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();

        HttpResponse<java.io.InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("chat HTTP " + res.statusCode());

        StringBuilder answer = new StringBuilder();
        List<String> sources = new ArrayList<>();
        int confidence = -1;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.equals("[DONE]") || data.isEmpty()) continue;
                JsonNode o;
                try { o = om.readTree(data); } catch (Exception ex) { continue; }
                if ("CHUNK".equals(o.path("event_status").asText())) {
                    answer.append(o.path("content").asText(""));
                }
                if (o.has("confidence") && confidence < 0) confidence = o.path("confidence").asInt(-1);
                if (o.has("content_references")) collectSources(o.get("content_references"), sources);
            }
        }
        String out = answer.toString().strip();
        if (out.isEmpty()) return "정부24에서 답변을 받지 못했습니다.";
        StringBuilder sb = new StringBuilder(out);
        if (confidence >= 0) sb.append("\n\n신뢰도(정부24): ").append(confidence);
        if (!sources.isEmpty()) {
            sb.append("\n\n[정부24 출처]");
            sources.stream().distinct().limit(5).forEach(s -> sb.append("\n- ").append(s));
        }
        return sb.toString();
    }

    private void collectSources(JsonNode refs, List<String> out) {
        if (!refs.isArray()) return;
        for (JsonNode group : refs) {
            JsonNode inner = group.path("references");
            if (!inner.isArray()) continue;
            for (JsonNode r : inner) {
                JsonNode add = r.path("sourceInfo").path("additionalInfo");
                String name = add.path("service_name").asText(r.path("title").asText(""));
                String url = add.path("notice").asText("");
                if (!name.isBlank()) out.add(url.isBlank() ? name : name + " (" + url + ")");
            }
        }
    }

    private HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", BASE + "/")
                .header("Origin", BASE);
    }
}
