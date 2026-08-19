package kr.yeokkeum.web;

import java.util.Map;
import kr.yeokkeum.audit.AuditService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    /** admin 전용(SecurityConfig 가 /api/audit** 를 ROLE_ADMIN 으로 강제). 필터: action·actor·from·to·limit. */
    @GetMapping("/api/audit")
    public Map<String, Object> auditLog(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(defaultValue = "200") int limit) {
        return Map.of("items", audit.search(action, actor, from, to, limit));
    }

    /** 감사로그 CSV 내보내기(admin). */
    @GetMapping("/api/audit/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(defaultValue = "1000") int limit) {
        String csv = audit.toCsv(audit.search(action, actor, from, to, limit));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }
}
