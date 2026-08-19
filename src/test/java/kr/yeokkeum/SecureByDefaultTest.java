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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** 토큰 미구성 + insecure=false → secure-by-default CLOSED. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "yeokkeum.auth.admin-token=",
        "yeokkeum.auth.user-token=",
        "yeokkeum.auth.insecure-open-mode=false"
})
class SecureByDefaultTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void healthStaysOpen() {
        assertThat(rest.getForEntity("/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void apiClosedByDefault() {
        // GET(무바디)로 게이트 검증 — POST+401 은 JDK HttpURLConnection 재시도 퀴크가 있어 GET 사용.
        assertThat(rest.getForEntity("/api/docs", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
