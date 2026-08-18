package kr.yeokkeum.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import kr.yeokkeum.audit.AuditService;
import kr.yeokkeum.auth.Principal;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import kr.yeokkeum.rag.IngestResult;
import kr.yeokkeum.rag.RagResult;
import kr.yeokkeum.rag.RagService;
import kr.yeokkeum.web.dto.DraftRequest;
import kr.yeokkeum.web.dto.IngestRequest;
import kr.yeokkeum.web.dto.RagQueryRequest;
import kr.yeokkeum.web.dto.SummarizeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    private final LlmGateway gateway;
    private final RagService rag;
    private final AuditService audit;

    public ApiController(LlmGateway gateway, RagService rag, AuditService audit) {
        this.gateway = gateway;
        this.rag = rag;
        this.audit = audit;
    }

    private static Principal principal(HttpServletRequest req) {
        return (Principal) req.getAttribute(Principal.ATTR);
    }

    @PostMapping("/api/summarize")
    public Map<String, Object> summarize(@Valid @RequestBody SummarizeRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        ChatResult r = gateway.chat(List.of(
                ChatMessage.system("다음 텍스트를 한국어로 3문장 이내로 요약하세요."),
                ChatMessage.user(body.text())), 0.3, 512);
        audit.record(p.actor(), p.role(), "summarize", Map.of("model", gateway.model(), "chars", body.text().length()));
        return Map.of("summary", r.text(), "model", gateway.model());
    }

    @PostMapping("/api/draft")
    public Map<String, Object> draft(@Valid @RequestBody DraftRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        String kind = (body.kind() == null || body.kind().isBlank()) ? "보고서" : body.kind();
        ChatResult r = gateway.chat(List.of(
                ChatMessage.system("당신은 공공기관 문서 작성 보조입니다. '" + kind + "' 초안을 한국어로 작성하세요."),
                ChatMessage.user(body.brief())), 0.3, 1024);
        audit.record(p.actor(), p.role(), "draft", Map.of("model", gateway.model(), "kind", kind));
        return Map.of("draft", r.text(), "kind", kind, "model", gateway.model());
    }

    @PostMapping("/api/docs")
    public IngestResult ingest(@Valid @RequestBody IngestRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        IngestResult res = rag.ingest(body.filename(), body.text());
        audit.record(p.actor(), p.role(), "ingest", Map.of("filename", body.filename(), "nChunks", res.nChunks()));
        return res;
    }

    @GetMapping("/api/docs")
    public Map<String, Object> listDocs(HttpServletRequest req) {
        return Map.of("items", rag.listDocuments());
    }

    @PostMapping("/api/rag/query")
    public RagResult ragQuery(@Valid @RequestBody RagQueryRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        RagResult res = rag.query(body.query(), body.topK());
        audit.record(p.actor(), p.role(), "rag_query",
                Map.of("grounded", res.grounded(), "nCitations", res.citations().size()));
        return res;
    }
}
