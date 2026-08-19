package kr.yeokkeum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentHitlTest {

    @Autowired
    TestRestTemplate rest;

    private HttpHeaders h(String token) {
        HttpHeaders x = new HttpHeaders();
        x.setContentType(MediaType.APPLICATION_JSON);
        x.setBearerAuth(token);
        return x;
    }

    private String ingest(String filename) {
        ResponseEntity<String> r = rest.exchange("/api/docs", HttpMethod.POST,
                new HttpEntity<>(Map.of("filename", filename, "text", "에이전트 도구 테스트 문서"), h("usr")), String.class);
        return r.getBody().replaceAll(".*\"docId\":\"([0-9a-f]+)\".*", "$1");
    }

    private String docs() {
        return rest.exchange("/api/docs", HttpMethod.GET, new HttpEntity<>(h("usr")), String.class).getBody();
    }

    @Test
    void safeToolRunsImmediately() {
        ingest("agent-safe.txt");
        ResponseEntity<String> r = rest.exchange("/api/tools/doc_search", HttpMethod.POST,
                new HttpEntity<>(Map.of("query", "테스트"), h("usr")), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"status\":\"done\"");
    }

    @Test
    void riskyToolRequiresApprovalThenExecutes() {
        String docId = ingest("agent-risky.txt");
        // 위험 도구 호출 → 승인 대기(즉시 삭제 안 됨)
        ResponseEntity<String> inv = rest.exchange("/api/tools/doc_delete", HttpMethod.POST,
                new HttpEntity<>(Map.of("docId", docId), h("usr")), String.class);
        assertThat(inv.getBody()).contains("pending_approval");
        String approvalId = inv.getBody().replaceAll(".*\"approvalId\":\"([^\"]+)\".*", "$1");
        assertThat(docs()).contains("agent-risky.txt"); // 아직 삭제 안 됨

        // user 는 승인 목록/승인 불가(403)
        assertThat(rest.exchange("/api/approvals", HttpMethod.GET, new HttpEntity<>(h("usr")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // admin 승인 → 실행(삭제됨)
        ResponseEntity<String> dec = rest.exchange("/api/approvals/" + approvalId, HttpMethod.POST,
                new HttpEntity<>(Map.of("decision", "approve"), h("adm")), String.class);
        assertThat(dec.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dec.getBody()).contains("approved").contains("삭제됨");
        assertThat(docs()).doesNotContain("agent-risky.txt");
    }

    @Test
    void rejectDoesNotExecute() {
        String docId = ingest("agent-reject.txt");
        ResponseEntity<String> inv = rest.exchange("/api/tools/doc_delete", HttpMethod.POST,
                new HttpEntity<>(Map.of("docId", docId), h("usr")), String.class);
        String approvalId = inv.getBody().replaceAll(".*\"approvalId\":\"([^\"]+)\".*", "$1");
        ResponseEntity<String> dec = rest.exchange("/api/approvals/" + approvalId, HttpMethod.POST,
                new HttpEntity<>(Map.of("decision", "reject"), h("adm")), String.class);
        assertThat(dec.getBody()).contains("rejected");
        assertThat(docs()).contains("agent-reject.txt"); // 거부 → 미삭제
    }
}
