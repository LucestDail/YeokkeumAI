package kr.yeokkeum.agent;

import java.util.Map;
import java.util.stream.Collectors;
import kr.yeokkeum.rag.RagResult;
import kr.yeokkeum.rag.RagService;
import org.springframework.stereotype.Component;

/** 안전(읽기) 도구 — 등록 문서에서 근거 기반 검색. HITL 불필요. */
@Component
public class DocSearchTool implements Tool {

    private final RagService rag;

    public DocSearchTool(RagService rag) {
        this.rag = rag;
    }

    @Override
    public String name() { return "doc_search"; }

    @Override
    public String description() { return "등록된 규정/지식 문서에서 질의에 대한 근거 기반 답변을 검색한다. args: {query}"; }

    @Override
    public boolean risky() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        String query = Tool.str(args, "query");
        if (query.isBlank()) return "query 가 필요합니다.";
        RagResult r = rag.query(query, null);
        String cites = r.citations().stream()
                .map(c -> c.filename() + "#" + c.idx())
                .collect(Collectors.joining(", "));
        return r.answer() + (cites.isEmpty() ? "" : "\n[근거] " + cites);
    }
}
