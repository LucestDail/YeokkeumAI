package kr.yeokkeum.rag;

import java.util.ArrayList;
import java.util.List;
import kr.yeokkeum.common.Ids;
import kr.yeokkeum.config.IeumProperties;
import kr.yeokkeum.doc.Chunk;
import kr.yeokkeum.doc.ChunkRepository;
import kr.yeokkeum.doc.Document;
import kr.yeokkeum.doc.DocumentRepository;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagService {

    private final DocumentRepository docRepo;
    private final ChunkRepository chunkRepo;
    private final LlmGateway gateway;
    private final IeumProperties props;

    public RagService(DocumentRepository docRepo, ChunkRepository chunkRepo,
                      LlmGateway gateway, IeumProperties props) {
        this.docRepo = docRepo;
        this.chunkRepo = chunkRepo;
        this.gateway = gateway;
        this.props = props;
    }

    @Transactional
    public IngestResult ingest(String filename, String text) {
        List<String> chunks = Chunker.chunk(text, props.getRag().getChunkChars());
        if (chunks.isEmpty()) throw new IllegalArgumentException("빈 문서");
        String docId = Ids.newId();
        int chars = chunks.stream().mapToInt(String::length).sum();
        docRepo.save(new Document(docId, filename, chars, chunks.size(), System.currentTimeMillis()));
        List<Chunk> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            rows.add(new Chunk(Ids.newId(), docId, filename, i, chunks.get(i)));
        }
        chunkRepo.saveAll(rows);
        return new IngestResult(docId, filename, chunks.size());
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments() {
        return docRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public RagResult query(String question, Integer topK) {
        List<Chunk> all = chunkRepo.findAll();
        if (all.isEmpty()) {
            return new RagResult("관련 근거를 찾지 못했습니다. 문서를 먼저 등록하거나 질문을 구체화해주세요.",
                    List.of(), false, gateway.model());
        }
        List<List<String>> docsTokens = new ArrayList<>(all.size());
        for (Chunk c : all) docsTokens.add(Tokenizer.tokenize(c.getText()));
        double[] scores = Bm25.scores(Tokenizer.tokenize(question), docsTokens);

        Integer[] order = new Integer[all.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        int k = topK != null ? topK : props.getRag().getTopK();
        List<Chunk> hits = new ArrayList<>();
        List<Citation> citations = new ArrayList<>();
        StringBuilder ctx = new StringBuilder();
        int rank = 0;
        for (int oi : order) {
            if (rank >= k || scores[oi] <= 0) break;
            Chunk c = all.get(oi);
            hits.add(c);
            citations.add(new Citation(c.getFilename(), c.getIdx(),
                    Math.round(scores[oi] * 10000.0) / 10000.0, snippet(c.getText())));
            ctx.append("[근거 ").append(rank + 1).append("] (").append(c.getFilename())
               .append(" #").append(c.getIdx()).append(")\n").append(c.getText()).append("\n\n");
            rank++;
        }
        if (hits.isEmpty()) {
            return new RagResult("관련 근거를 찾지 못했습니다. 질문을 구체화해주세요.", List.of(), false, gateway.model());
        }
        String system = "당신은 공공기관 업무보조 AI입니다. 아래 <근거>만을 바탕으로 한국어로 정확히 답하세요. "
                + "근거에 없으면 모른다고 정직히 답하고 지어내지 마세요. 답변 끝에 사용한 [근거 n]을 표기하세요.\n\n"
                + "<근거>\n" + ctx + "</근거>";
        ChatResult r = gateway.chat(List.of(ChatMessage.system(system), ChatMessage.user(question)));
        return new RagResult(r.text(), citations, true, gateway.model());
    }

    private static String snippet(String text) {
        return text.length() <= 240 ? text : text.substring(0, 240);
    }
}
