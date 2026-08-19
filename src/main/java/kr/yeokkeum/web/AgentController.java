package kr.yeokkeum.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import kr.yeokkeum.agent.AgentService;
import kr.yeokkeum.agent.ApprovalService;
import kr.yeokkeum.agent.Tool;
import kr.yeokkeum.agent.ToolRegistry;
import kr.yeokkeum.audit.AuditService;
import kr.yeokkeum.auth.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 업무 에이전트 — 도구 실행 + HITL 승인.
 * 안전 도구는 즉시 실행, 위험(변경) 도구는 승인 큐 등록 후 admin 승인 시 실행. 전 과정 감사.
 * (승인/관리는 admin 전용 — SecurityConfig 의 /api/approvals** 참조)
 */
@RestController
public class AgentController {

    private final ToolRegistry registry;
    private final ApprovalService approvals;
    private final AuditService audit;
    private final AgentService agent;

    public AgentController(ToolRegistry registry, ApprovalService approvals, AuditService audit, AgentService agent) {
        this.registry = registry;
        this.approvals = approvals;
        this.audit = audit;
        this.agent = agent;
    }

    private static Principal principal(HttpServletRequest req) {
        return (Principal) req.getAttribute(Principal.ATTR);
    }

    @GetMapping("/api/tools")
    public Map<String, Object> tools() {
        List<Map<String, Object>> items = registry.all().stream()
                .map(t -> Map.<String, Object>of("name", t.name(), "description", t.description(), "risky", t.risky()))
                .toList();
        return Map.of("items", items);
    }

    /** 도구 호출 — 안전=즉시 실행, 위험=승인 대기 등록. */
    @PostMapping("/api/tools/{name}")
    public Map<String, Object> invoke(@PathVariable String name,
                                      @RequestBody(required = false) Map<String, Object> args,
                                      HttpServletRequest req) {
        Principal p = principal(req);
        Tool tool = registry.get(name);
        if (tool == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "도구를 찾을 수 없습니다: " + name);
        }
        return runTool(tool, args, p);
    }

    /** 자연어 지시 → LLM 도구 라우팅 → 실행/HITL. */
    @PostMapping("/api/agent")
    public Map<String, Object> agent(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Principal p = principal(req);
        String instruction = body == null ? null : body.get("instruction");
        if (instruction == null || instruction.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instruction 이 필요합니다.");
        }
        AgentService.Decision d = agent.decide(instruction);
        Tool tool = d.tool() == null ? null : registry.get(d.tool());
        audit.record(p.actor(), p.role(), "agent", Map.of("tool", d.tool() == null ? "(none)" : d.tool()));
        if (tool == null) {
            return Map.of("status", "no_tool",
                    "message", "적절한 도구를 찾지 못했습니다. 「대화」 탭을 사용하거나 요청을 구체화하세요.");
        }
        Map<String, Object> out = new java.util.LinkedHashMap<>(runTool(tool, d.args(), p));
        out.put("chosenTool", d.tool());
        out.put("args", d.args());
        return out;
    }

    /** 도구 실행 공용 로직 — 안전=즉시, 위험=승인대기. */
    private Map<String, Object> runTool(Tool tool, Map<String, Object> args, Principal p) {
        String name = tool.name();
        audit.record(p.actor(), p.role(), "tool_invoke", Map.of("tool", name, "risky", tool.risky()));
        if (tool.risky()) {
            ApprovalService.Pending pending = approvals.create(name, args, p.actor());
            audit.record(p.actor(), p.role(), "approval_request", Map.of("tool", name, "approvalId", pending.getId()));
            return Map.of("status", "pending_approval", "approvalId", pending.getId(), "tool", name,
                    "message", "위험(변경) 작업입니다. 관리자 승인 후 실행됩니다.");
        }
        String result = tool.execute(args);
        audit.record(p.actor(), p.role(), "tool_exec", Map.of("tool", name));
        return Map.of("status", "done", "tool", name, "result", result);
    }

    /** 대기 중 승인 목록(admin). */
    @GetMapping("/api/approvals")
    public Map<String, Object> listApprovals() {
        List<Map<String, Object>> items = approvals.pending().stream()
                .map(a -> Map.<String, Object>of("id", a.getId(), "tool", a.getTool(),
                        "args", a.getArgs(), "requestedBy", a.getRequestedBy(), "createdAt", a.getCreatedAt()))
                .toList();
        return Map.of("items", items);
    }

    /** 승인/거부(admin). approve 시 도구 실행, reject 시 미실행. */
    @PostMapping("/api/approvals/{id}")
    public Map<String, Object> decide(@PathVariable String id,
                                      @RequestBody Map<String, String> body,
                                      HttpServletRequest req) {
        Principal p = principal(req);
        ApprovalService.Pending pending = approvals.get(id);
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "승인 요청을 찾을 수 없습니다.");
        }
        if (!"PENDING".equals(pending.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 요청입니다: " + pending.getStatus());
        }
        boolean approve = "approve".equalsIgnoreCase(body == null ? null : body.get("decision"));
        if (!approve) {
            approvals.resolve(id, false, "거부됨");
            audit.record(p.actor(), p.role(), "approval_reject", Map.of("tool", pending.getTool(), "approvalId", id));
            return Map.of("status", "rejected", "tool", pending.getTool());
        }
        Tool tool = registry.get(pending.getTool());
        if (tool == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "도구가 더 이상 없습니다: " + pending.getTool());
        }
        String result = tool.execute(pending.getArgs());
        approvals.resolve(id, true, result);
        audit.record(p.actor(), p.role(), "approval_approve", Map.of("tool", pending.getTool(), "approvalId", id));
        audit.record(p.actor(), p.role(), "tool_exec", Map.of("tool", pending.getTool(), "viaApproval", true));
        return Map.of("status", "approved", "tool", pending.getTool(), "result", result);
    }
}
