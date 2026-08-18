package kr.yeokkeum.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.util.Map;
import kr.yeokkeum.config.IeumProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RBAC — /api/** 게이트. secure-by-default: 토큰 미구성 시 CLOSED(명시 open만 개방).
 * /api/audit* = admin, 그 외 = user.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Map<String, Integer> RANK = Map.of("none", 0, "user", 1, "admin", 2);

    private final IeumProperties props;

    public AuthInterceptor(IeumProperties props) {
        this.props = props;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        Principal p = resolve(req);
        req.setAttribute(Principal.ATTR, p);
        String path = req.getRequestURI();
        String minRole = path.startsWith("/api/audit") ? "admin" : "user";
        if (RANK.getOrDefault(p.role(), 0) < RANK.get(minRole)) {
            res.setStatus("none".equals(p.role()) ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"detail\":\"unauthorized\"}");
            return false;
        }
        return true;
    }

    private Principal resolve(HttpServletRequest req) {
        IeumProperties.Auth a = props.getAuth();
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
        return MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String prefix(String t) { return t.length() <= 6 ? t : t.substring(0, 6); }
}
