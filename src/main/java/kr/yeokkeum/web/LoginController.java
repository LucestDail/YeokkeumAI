package kr.yeokkeum.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import kr.yeokkeum.config.YeokkeumProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인 — 아이디/비번을 검증해 해당 역할의 토큰을 반환(클라이언트가 이후 Bearer 로 사용).
 * 게스트 로그인=사용자 토큰(비번 없이, guest-enabled 시). 공개 엔드포인트(SecurityConfig permitAll).
 */
@RestController
public class LoginController {

    private final YeokkeumProperties props;

    public LoginController(YeokkeumProperties props) {
        this.props = props;
    }

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        YeokkeumProperties.Auth a = props.getAuth();
        boolean guest = Boolean.TRUE.equals(body.get("guest"));
        if (guest) {
            if (!a.isGuestEnabled() || isBlank(a.getUserToken())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게스트 로그인이 비활성화되어 있습니다.");
            }
            return Map.of("token", a.getUserToken(), "role", "user", "name", "게스트");
        }
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        if (!isBlank(a.getAdminPass()) && username.equals(a.getAdminUser()) && eq(password, a.getAdminPass())
                && !isBlank(a.getAdminToken())) {
            return Map.of("token", a.getAdminToken(), "role", "admin", "name", username);
        }
        if (!isBlank(a.getUserPass()) && username.equals(a.getUserUser()) && eq(password, a.getUserPass())
                && !isBlank(a.getUserToken())) {
            return Map.of("token", a.getUserToken(), "role", "user", "name", username);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static boolean eq(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
