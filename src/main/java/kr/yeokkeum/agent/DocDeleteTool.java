package kr.yeokkeum.agent;

import java.util.Map;
import kr.yeokkeum.rag.RagService;
import org.springframework.stereotype.Component;

/** 위험(변경) 도구 — 문서 삭제. HITL 승인 후에만 실행된다. */
@Component
public class DocDeleteTool implements Tool {

    private final RagService rag;

    public DocDeleteTool(RagService rag) {
        this.rag = rag;
    }

    @Override
    public String name() { return "doc_delete"; }

    @Override
    public String description() { return "등록 문서를 삭제한다(파기). args: {docId}. ⚠️ 변경 작업 → 승인 필요."; }

    @Override
    public boolean risky() { return true; }

    @Override
    public String execute(Map<String, Object> args) {
        String docId = Tool.str(args, "docId");
        if (docId.isBlank()) return "docId 가 필요합니다.";
        boolean removed = rag.deleteDocument(docId);
        return removed ? "문서 삭제됨: " + docId : "문서를 찾을 수 없음: " + docId;
    }
}
