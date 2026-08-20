package kr.yeokkeum.agent;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 정부24(plus.gov.kr) AI 검색 연동 도구 [외부 연계]. 읽기(안전).
 * 스트리밍은 {@link kr.yeokkeum.web.Gov24Controller}(/api/gov24/chat)가 담당하고,
 * 이 도구(에이전트 라우팅용)는 전체 답변을 모아 반환한다. 폐쇄망/외부오류 시 정직 실패.
 */
@Component
public class Gov24SearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(Gov24SearchTool.class);

    private final Gov24Client client;

    public Gov24SearchTool(Gov24Client client) {
        this.client = client;
    }

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
            StringBuilder sb = new StringBuilder();
            Gov24Client.Meta meta = client.stream(query, sb::append, p -> { });
            String out = sb.toString().strip();
            if (out.isEmpty()) return "정부24에서 답변을 받지 못했습니다.";
            if (meta.confidence() >= 0) sb.append("\n\n신뢰도(정부24): ").append(meta.confidence());
            if (!meta.sources().isEmpty()) {
                sb.append("\n\n[정부24 출처]");
                meta.sources().stream().distinct().limit(5).forEach(s -> sb.append("\n- ").append(s));
            }
            return sb.toString().strip();
        } catch (Exception e) {
            log.warn("정부24 연동 실패: {}", e.getMessage());
            return "정부24 연동에 실패했습니다(외부 API 오류·네트워크·폐쇄망): " + e.getMessage();
        }
    }
}
