package kr.yeokkeum;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Phase1Test {

    @Autowired
    TestRestTemplate rest;

    private HttpHeaders userJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth("usr");
        return h;
    }

    @Test
    void reviewGroundsOnRegulations() {
        rest.exchange("/api/docs", HttpMethod.POST, new HttpEntity<>(
                Map.of("filename", "규정.txt",
                        "text", "공공 웹은 KWCAG 2.2 웹접근성을 준수해야 한다.\n\n개인정보는 암호화하여 저장한다."),
                userJson()), String.class);
        ResponseEntity<String> r = rest.exchange("/api/review", HttpMethod.POST, new HttpEntity<>(
                Map.of("text", "우리 사이트는 웹접근성을 고려하지 않았고 개인정보를 평문으로 저장한다."),
                userJson()), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"grounded\":true").contains("규정.txt");
    }

    @Test
    void reviewRequiresAuth() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        assertThat(rest.exchange("/api/review", HttpMethod.POST,
                new HttpEntity<>(Map.of("text", "x"), h), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void uploadTextFileIngests() {
        ByteArrayResource file = new ByteArrayResource(
                "업로드 문서 내용입니다. 조달 규정 테스트 문단.".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "up.txt";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        h.setBearerAuth("usr");
        ResponseEntity<String> r = rest.exchange("/api/docs/upload", HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("nChunks").contains("up.txt");
    }

    @Test
    void deleteDocumentRemovesIt() {
        ResponseEntity<String> ing = rest.exchange("/api/docs", HttpMethod.POST, new HttpEntity<>(
                Map.of("filename", "del.txt", "text", "삭제 대상 문서 내용."), userJson()), String.class);
        String docId = ing.getBody().replaceAll(".*\"docId\":\"([0-9a-f]+)\".*", "$1");
        ResponseEntity<String> del = rest.exchange("/api/docs/" + docId, HttpMethod.DELETE,
                new HttpEntity<>(userJson()), String.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(del.getBody()).contains("deleted");
        // 없는 문서 → 404
        assertThat(rest.exchange("/api/docs/nonexistent", HttpMethod.DELETE,
                new HttpEntity<>(userJson()), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadRejectsDisallowedExtension() {
        ByteArrayResource file = new ByteArrayResource("malware".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "evil.exe";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        h.setBearerAuth("usr");
        ResponseEntity<String> r = rest.exchange("/api/docs/upload", HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
