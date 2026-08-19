package kr.yeokkeum.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import kr.yeokkeum.audit.AuditService;
import kr.yeokkeum.auth.Principal;
import kr.yeokkeum.doc.DocText;
import kr.yeokkeum.doc.HwpExtractor;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import kr.yeokkeum.rag.IngestResult;
import kr.yeokkeum.rag.RagResult;
import kr.yeokkeum.rag.RagService;
import kr.yeokkeum.web.dto.DraftRequest;
import kr.yeokkeum.web.dto.IngestRequest;
import kr.yeokkeum.web.dto.RagQueryRequest;
import kr.yeokkeum.web.dto.ReviewRequest;
import kr.yeokkeum.web.dto.SummarizeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ApiController {

    private final LlmGateway gateway;
    private final RagService rag;
    private final AuditService audit;
    private final HwpExtractor hwp;

    public ApiController(LlmGateway gateway, RagService rag, AuditService audit, HwpExtractor hwp) {
        this.gateway = gateway;
        this.rag = rag;
        this.audit = audit;
        this.hwp = hwp;
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

    /** 문서 삭제 — 오등록·PII 문서 파기(청크 포함). */
    @DeleteMapping("/api/docs/{docId}")
    public Map<String, Object> deleteDoc(@PathVariable String docId, HttpServletRequest req) {
        Principal p = principal(req);
        boolean removed = rag.deleteDocument(docId);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
        audit.record(p.actor(), p.role(), "delete", Map.of("docId", docId));
        return Map.of("deleted", docId);
    }

    @PostMapping("/api/rag/query")
    public RagResult ragQuery(@Valid @RequestBody RagQueryRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        RagResult res = rag.query(body.query(), body.topK());
        audit.record(p.actor(), p.role(), "rag_query",
                Map.of("grounded", res.grounded(), "nCitations", res.citations().size()));
        return res;
    }

    /** 규정검토 에이전트 — 작성물을 등록된 규정/근거에 비추어 위반·리스크 지적 + 수정문안(다국어). */
    @PostMapping("/api/review")
    public RagResult review(@Valid @RequestBody ReviewRequest body, HttpServletRequest req) {
        Principal p = principal(req);
        RagResult res = rag.review(body.text(), body.lang());
        audit.record(p.actor(), p.role(), "review",
                Map.of("grounded", res.grounded(), "nCitations", res.citations().size(),
                        "lang", body.lang() == null ? "ko" : body.lang()));
        return res;
    }

    private static final java.util.Set<String> ALLOWED_EXT =
            java.util.Set.of(".pdf", ".txt", ".md", ".hwp", ".hwpx");

    /** 파일 업로드 색인 — PDF/텍스트 + HWP/HWPX(rhwp export-text). 크기 상한(multipart)·확장자 화이트리스트 적용. */
    @PostMapping("/api/docs/upload")
    public IngestResult upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        Principal p = principal(req);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일입니다.");
        }
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String lower = filename.toLowerCase();
        if (ALLOWED_EXT.stream().noneMatch(lower::endsWith)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "지원하지 않는 형식입니다. 허용: PDF · 텍스트(.txt/.md) · HWP/HWPX");
        }
        String text;
        try {
            if (lower.endsWith(".hwp") || lower.endsWith(".hwpx")) {
                text = hwp.extract(filename, file.getBytes());
            } else {
                text = DocText.extract(filename, file.getBytes());
            }
        } catch (HwpExtractor.HwpExtractionException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 처리 실패: " + e.getMessage());
        }
        IngestResult res = rag.ingest(filename, text);
        audit.record(p.actor(), p.role(), "upload",
                Map.of("filename", filename, "nChunks", res.nChunks(), "type", fileType(lower)));
        return res;
    }

    private static String fileType(String lower) {
        if (lower.endsWith(".hwp") || lower.endsWith(".hwpx")) return "hwp";
        if (lower.endsWith(".pdf")) return "pdf";
        return "text";
    }
}
