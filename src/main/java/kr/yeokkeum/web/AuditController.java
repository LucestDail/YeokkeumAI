package kr.yeokkeum.web;

import java.util.Map;
import kr.yeokkeum.audit.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    /** admin 전용(인터셉터가 /api/audit* 를 admin 으로 강제). */
    @GetMapping("/api/audit")
    public Map<String, Object> auditLog(@RequestParam(defaultValue = "200") int limit) {
        return Map.of("items", audit.list(limit));
    }
}
