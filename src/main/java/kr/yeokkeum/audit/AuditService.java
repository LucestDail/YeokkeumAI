package kr.yeokkeum.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.yeokkeum.common.Ids;
import kr.yeokkeum.common.PiiMasker;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    public void record(String actor, String role, String action, Map<String, Object> detail) {
        String json;
        try {
            json = om.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (Exception e) {
            json = "{}";
        }
        json = PiiMasker.mask(json); // [SEC-1] 감사로그 PII 비식별
        repo.save(new AuditLog(Ids.newId(), System.currentTimeMillis(), actor, role, action, json));
    }

    public List<AuditLog> list(int limit) {
        return repo.findAllByOrderByTsDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 1000)));
    }
}
