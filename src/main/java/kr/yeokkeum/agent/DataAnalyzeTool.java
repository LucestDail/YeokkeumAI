package kr.yeokkeum.agent;

import java.util.List;
import java.util.Map;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import kr.yeokkeum.rag.RagService;
import org.springframework.stereotype.Component;

/**
 * 안전(읽기) 도구 — 업로드된 문서(표/CSV/XLSX/텍스트)의 데이터를 분석한다(A6 슬라이스).
 * 핵심 수치·추세·이상치를 항목별로 요약. 근거는 문서 내용에 한정(지어내기 금지).
 */
@Component
public class DataAnalyzeTool implements Tool {

    private static final int MAX_CHARS = 12_000; // 컨텍스트 보호

    private final RagService rag;
    private final LlmGateway gateway;

    public DataAnalyzeTool(RagService rag, LlmGateway gateway) {
        this.rag = rag;
        this.gateway = gateway;
    }

    @Override
    public String name() { return "data_analyze"; }

    @Override
    public String description() { return "업로드된 문서(표·CSV·XLSX·텍스트)의 데이터를 분석해 핵심 수치·추세·이상치를 요약한다. args: {docId}"; }

    @Override
    public boolean risky() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        String docId = Tool.str(args, "docId");
        if (docId.isBlank()) return "docId 가 필요합니다.";
        String text = rag.documentText(docId);
        if (text == null) return "문서를 찾을 수 없습니다: " + docId;
        if (text.length() > MAX_CHARS) text = text.substring(0, MAX_CHARS);
        String system = "당신은 공공기관 데이터 분석 보조입니다. 아래 <데이터>를 분석해 "
                + "①핵심 수치 ②눈에 띄는 추세 ③이상치·특이점 을 항목별로 한국어로 간결히 정리하세요. "
                + "데이터에 없는 내용은 추정하지 말고 '데이터 범위 밖'이라고 하세요.\n\n<데이터>\n" + text;
        ChatResult r = gateway.chat(List.of(ChatMessage.system(system), ChatMessage.user("이 데이터를 분석해줘")), 0.2, 1024);
        return r.text();
    }
}
