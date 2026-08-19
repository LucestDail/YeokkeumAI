package kr.yeokkeum.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import kr.yeokkeum.config.YeokkeumProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 토큰 → 역할 해석 후 SecurityContext 인증 설정(Spring Security).
 * Bearer/X-API-Key 로 admin/user 매핑, secure-by-default(토큰 미구성 시 none → 인증 없음 → 401).
 * 컨트롤러 호환을 위해 {@link Principal} 을 요청 속성으로도 남긴다.
 */
@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final YeokkeumProperties props;

    public TokenAuthFilter(YeokkeumProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Principal p = resolve(req);
        req.setAttribute(Principal.ATTR, p);
        if (!"none".equals(p.role())) {
            var authority = new SimpleGrantedAuthority("ROLE_" + p.role().toUpperCase());
            var auth = new UsernamePasswordAuthenticationToken(p, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }

    private Principal resolve(HttpServletRequest req) {
        YeokkeumProperties.Auth a = props.getAuth();
        String admin = safe(a.getAdminToken());
        String user = safe(a.getUserToken());
        String token = present(req);
        if (!admin.isEmpty() && eq(token, admin)) return new Principal("token:" + prefix(token), "admin");
        if (!user.isEmpty() && eq(token, user)) return new Principal("token:" + prefix(token), "user");
        if (admin.isEmpty() && user.isEmpty()) {
            return a.isInsecureOpenMode() ? new Principal("open", "admin") : new Principal("anon", "none");
        }
        return new Principal("anon", "none");
    }

    private static String present(HttpServletRequest req) {
        String authz = req.getHeader("Authorization");
        if (authz != null && authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authz.substring(7).trim();
        }
        String key = req.getHeader("X-API-Key");
        return key == null ? "" : key.trim();
    }

    private static boolean eq(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String prefix(String t) { return t.length() <= 6 ? t : t.substring(0, 6); }
}
