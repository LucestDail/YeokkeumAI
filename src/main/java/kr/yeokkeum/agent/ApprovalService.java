package kr.yeokkeum.agent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.yeokkeum.common.Ids;
import org.springframework.stereotype.Service;

/**
 * HITL 승인 큐 — 위험 도구 실행 요청을 보관하고 사람 승인/거부를 기다린다(통제·책임성).
 * 인메모리(단일 노드). 승인은 전이(PENDING→APPROVED/REJECTED)만, 실행은 호출부가 수행.
 */
@Service
public class ApprovalService {

    public static final class Pending {
        private final String id;
        private final String tool;
        private final Map<String, Object> args;
        private final String requestedBy;
        private final long createdAt;
        private volatile String status = "PENDING";
        private volatile String result;

        Pending(String id, String tool, Map<String, Object> args, String requestedBy, long createdAt) {
            this.id = id;
            this.tool = tool;
            this.args = args;
            this.requestedBy = requestedBy;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getTool() { return tool; }
        public Map<String, Object> getArgs() { return args; }
        public String getRequestedBy() { return requestedBy; }
        public long getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
        public String getResult() { return result; }
    }

    private final Map<String, Pending> store = new ConcurrentHashMap<>();

    public Pending create(String tool, Map<String, Object> args, String requestedBy) {
        Pending p = new Pending(Ids.newId(), tool,
                args == null ? Map.of() : args, requestedBy, System.currentTimeMillis());
        store.put(p.id, p);
        return p;
    }

    public Pending get(String id) {
        return store.get(id);
    }

    public boolean isPending(String id) {
        Pending p = store.get(id);
        return p != null && "PENDING".equals(p.status);
    }

    public void resolve(String id, boolean approved, String result) {
        Pending p = store.get(id);
        if (p != null) {
            p.status = approved ? "APPROVED" : "REJECTED";
            p.result = result;
        }
    }

    public List<Pending> pending() {
        return store.values().stream()
                .filter(p -> "PENDING".equals(p.status))
                .sorted(Comparator.comparingLong(Pending::getCreatedAt))
                .toList();
    }
}
