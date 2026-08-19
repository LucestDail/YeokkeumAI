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
        return repo.findAllByOrderByTsDesc(PageRequest.of(0, clamp(limit)));
    }

    /** 필터 검색(action/actor/기간). blank/null 은 무시. */
    public List<AuditLog> search(String action, String actor, Long from, Long to, int limit) {
        String ac = (action == null || action.isBlank()) ? null : action;
        String at = (actor == null || actor.isBlank()) ? null : actor;
        long f = from == null ? 0L : from;
        long t = to == null ? Long.MAX_VALUE : to;
        return repo.search(ac, at, f, t, PageRequest.of(0, clamp(limit)));
    }

    /** 감사로그 CSV(내보내기). 헤더 + RFC4180 인용. */
    public String toCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder("ts,actor,role,action,detail\n");
        for (AuditLog a : logs) {
            sb.append(a.getTs()).append(',')
              .append(csv(a.getActor())).append(',')
              .append(csv(a.getRole())).append(',')
              .append(csv(a.getAction())).append(',')
              .append(csv(a.getDetail())).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 1000);
    }
}
