package kr.yeokkeum.rag;

import java.util.ArrayList;
import java.util.List;
import kr.yeokkeum.common.Ids;
import kr.yeokkeum.config.IeumProperties;
import kr.yeokkeum.doc.Chunk;
import kr.yeokkeum.doc.ChunkRepository;
import kr.yeokkeum.doc.Document;
import kr.yeokkeum.doc.DocumentRepository;
import kr.yeokkeum.embedding.EmbeddingGateway;
import kr.yeokkeum.embedding.Vectors;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.ChatResult;
import kr.yeokkeum.gateway.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final DocumentRepository docRepo;
    private final ChunkRepository chunkRepo;
    private final LlmGateway gateway;
    private final EmbeddingGateway embedder;
    private final IeumProperties props;

    public RagService(DocumentRepository docRepo, ChunkRepository chunkRepo,
                      LlmGateway gateway, EmbeddingGateway embedder, IeumProperties props) {
        this.docRepo = docRepo;
        this.chunkRepo = chunkRepo;
        this.gateway = gateway;
        this.embedder = embedder;
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
        // dense 임베딩(BGE-M3 등). 실패해도 색인은 진행 → BM25 로 검색 가능(회복력).
        try {
            List<float[]> vecs = embedder.embed(chunks);
            for (int i = 0; i < rows.size() && i < vecs.size(); i++) {
                rows.get(i).setEmbedding(Vectors.toBytes(vecs.get(i)));
            }
        } catch (RuntimeException e) {
            log.warn("임베딩 생략(엔드포인트 문제) — BM25 단독 색인: {}", e.getMessage());
        }
        chunkRepo.saveAll(rows);
        return new IngestResult(docId, filename, chunks.size());
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments() {
        return docRepo.findAllByOrderByCreatedAtDesc();
    }

    /** 문서 삭제(청크 포함) — 오등록·PII 문서 파기. 없으면 false. */
    @Transactional
    public boolean deleteDocument(String docId) {
        if (docId == null || !docRepo.existsById(docId)) return false;
        chunkRepo.deleteByDocId(docId);
        docRepo.deleteById(docId);
        return true;
    }

    @Transactional(readOnly = true)
    public RagResult query(String question, Integer topK) {
        Retrieved r = retrieve(question, topK != null ? topK : props.getRag().getTopK());
        if (r.citations.isEmpty()) {
            return new RagResult("관련 근거를 찾지 못했습니다. 문서를 먼저 등록하거나 질문을 구체화해주세요.",
                    List.of(), false, gateway.model());
        }
        String system = "당신은 공공기관 업무보조 AI입니다. 아래 <근거>만을 바탕으로 한국어로 정확히 답하세요. "
                + "근거에 없으면 모른다고 정직히 답하고 지어내지 마세요. 답변 끝에 사용한 [근거 n]을 표기하세요.\n\n"
                + "<근거>\n" + r.context + "</근거>";
        ChatResult res = gateway.chat(List.of(ChatMessage.system(system), ChatMessage.user(question)));
        return new RagResult(res.text(), r.citations, true, gateway.model());
    }

    /** 규정검토 — 작성물을 등록된 규정/근거에 비추어 위반·리스크 지적 + 수정문안(다국어). */
    @Transactional(readOnly = true)
    public RagResult review(String text, String lang) {
        Retrieved r = retrieve(text, props.getRag().getTopK());
        if (r.citations.isEmpty()) {
            return new RagResult("작성물과 관련된 규정 근거를 찾지 못했습니다. 규정 문서를 등록했는지 확인하세요.",
                    List.of(), false, gateway.model());
        }
        String langLine = (lang != null && !lang.isBlank()) ? ("\n답변 언어: " + lang) : "";
        String system = "당신은 공공기관 규정검토 AI입니다. 아래 <근거>(법령·규정·지침)에 비추어 <작성물>의 "
                + "위반·리스크를 항목별로 지적하고 각 항목에 수정문안을 제시하세요. 근거에 없는 사항은 지어내지 말고 "
                + "'근거 범위 밖'이라고 명시하세요. 각 지적 끝에 [근거 n]을 표기하세요." + langLine
                + "\n\n<근거>\n" + r.context + "</근거>";
        ChatResult res = gateway.chat(List.of(ChatMessage.system(system), ChatMessage.user("<작성물>\n" + text)));
        return new RagResult(res.text(), r.citations, true, gateway.model());
    }

    /** 하이브리드 검색: BM25 + dense(BGE-M3) 를 RRF 융합. dense 없으면 BM25 단독으로 폴백. */
    private Retrieved retrieve(String queryText, int k) {
        List<Chunk> all = chunkRepo.findAll();
        if (all.isEmpty()) return Retrieved.EMPTY;
        int n = all.size();

        // 1) BM25
        List<List<String>> docsTokens = new ArrayList<>(n);
        for (Chunk c : all) docsTokens.add(Tokenizer.tokenize(c.getText()));
        double[] bm = Bm25.scores(Tokenizer.tokenize(queryText), docsTokens);

        // 2) dense (선택)
        double[] dense = new double[n];
        boolean denseOn = false;
        if (props.getRag().isHybrid() && hasAnyEmbedding(all)) {
            try {
                float[] qv = embedder.embedOne(queryText);
                for (int i = 0; i < n; i++) {
                    byte[] e = all.get(i).getEmbedding();
                    dense[i] = e == null ? 0.0 : Vectors.cosine(qv, Vectors.fromBytes(e));
                }
                denseOn = true;
            } catch (RuntimeException ex) {
                log.warn("dense 검색 생략(임베딩 문제) — BM25 단독: {}", ex.getMessage());
            }
        }

        // 3) RRF 융합
        int[] rankBm = ranks(bm);
        int[] rankDense = denseOn ? ranks(dense) : null;
        double kk = props.getRag().getRrfK();
        double[] fused = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            if (bm[i] > 0) s += 1.0 / (kk + rankBm[i]);
            if (denseOn && dense[i] > 0) s += 1.0 / (kk + rankDense[i]);
            fused[i] = s;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(fused[b], fused[a]));

        List<Citation> citations = new ArrayList<>();
        StringBuilder ctx = new StringBuilder();
        int rank = 0;
        for (int oi : order) {
            if (rank >= k || fused[oi] <= 0) break;
            Chunk c = all.get(oi);
            citations.add(new Citation(c.getFilename(), c.getIdx(),
                    Math.round(bm[oi] * 10000.0) / 10000.0, snippet(c.getText())));
            ctx.append("[근거 ").append(rank + 1).append("] (").append(c.getFilename())
               .append(" #").append(c.getIdx()).append(")\n").append(c.getText()).append("\n\n");
            rank++;
        }
        return new Retrieved(citations, ctx.toString());
    }

    private static boolean hasAnyEmbedding(List<Chunk> chunks) {
        for (Chunk c : chunks) if (c.getEmbedding() != null) return true;
        return false;
    }

    /** 점수 내림차순 랭크(최고=0) 반환. */
    private static int[] ranks(double[] scores) {
        int n = scores.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));
        int[] rank = new int[n];
        for (int r = 0; r < n; r++) rank[idx[r]] = r;
        return rank;
    }

    private static String snippet(String text) {
        return text.length() <= 240 ? text : text.substring(0, 240);
    }

    private record Retrieved(List<Citation> citations, String context) {
        static final Retrieved EMPTY = new Retrieved(List.of(), "");
    }
}
