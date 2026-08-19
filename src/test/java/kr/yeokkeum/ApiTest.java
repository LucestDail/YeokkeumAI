package kr.yeokkeum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiTest {

    @Autowired
    TestRestTemplate rest;

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    private <T> ResponseEntity<String> post(String path, Object body, String token) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), String.class);
    }

    @Test
    void health() {
        ResponseEntity<String> r = rest.getForEntity("/health", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("ok");
    }

    @Test
    void chatRequiresAuth() {
        assertThat(post("/api/chat", Map.of("message", "안녕"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void chatStubStreams() {
        ResponseEntity<String> r = post("/api/chat", Map.of("message", "테스트 질문"), "usr");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("stub").contains("[DONE]");
    }

    @Test
    void ingestAndRagCites() {
        String doc = "조달청은 나라장터를 운영한다.\n\n웹접근성은 KWCAG 2.2 기준을 따른다.\n\n개인정보는 암호화한다.";
        assertThat(post("/api/docs", Map.of("filename", "지침.txt", "text", doc), "usr").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        ResponseEntity<String> q = post("/api/rag/query", Map.of("query", "웹접근성은 무슨 기준을 따르나"), "usr");
        assertThat(q.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q.getBody()).contains("\"grounded\":true").contains("지침.txt");
    }

    @Test
    void summarizeWorks() {
        ResponseEntity<String> r = post("/api/summarize", Map.of("text", "첫문장. 둘째문장. 셋째문장. 넷째문장."), "usr");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("요약");
    }

    @Test
    void auditIsAdminOnly() {
        // user 는 감사로그 접근 불가(403), admin 은 200
        assertThat(rest.exchange("/api/audit", HttpMethod.GET, new HttpEntity<>(headers("usr")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.exchange("/api/audit", HttpMethod.GET, new HttpEntity<>(headers("adm")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void auditFilterAndCsvExport() {
        post("/api/docs", Map.of("filename", "audit-a.txt", "text", "감사 이벤트 생성용"), "usr");
        // action 필터
        ResponseEntity<String> f = rest.exchange("/api/audit?action=ingest&limit=50", HttpMethod.GET,
                new HttpEntity<>(headers("adm")), String.class);
        assertThat(f.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(f.getBody()).contains("ingest");
        // CSV 내보내기
        ResponseEntity<String> c = rest.exchange("/api/audit/export?limit=50", HttpMethod.GET,
                new HttpEntity<>(headers("adm")), String.class);
        assertThat(c.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getBody()).contains("ts,actor,role,action,detail");
        // user 는 export 도 금지
        assertThat(rest.exchange("/api/audit/export", HttpMethod.GET, new HttpEntity<>(headers("usr")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
