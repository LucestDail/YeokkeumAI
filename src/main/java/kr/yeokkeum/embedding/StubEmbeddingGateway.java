package kr.yeokkeum.embedding;

import java.util.ArrayList;
import java.util.List;
import kr.yeokkeum.config.YeokkeumProperties;
import kr.yeokkeum.rag.Tokenizer;

/**
 * 오프라인 결정적 임베더 — 토큰 해싱 bag-of-words 를 고정 차원에 투영 후 L2 정규화.
 * 외부 모델 없이 동작(폐쇄망 데모·결정적 테스트). 같은 어휘 → 유사 벡터라 하이브리드 배선을 검증 가능.
 * ⚠️ 의미 품질은 실제 BGE-M3 에 못 미침(엔드포인트 연결 시 자동 대체).
 */
public class StubEmbeddingGateway implements EmbeddingGateway {

    private final int dim;

    public StubEmbeddingGateway(YeokkeumProperties.Embedding cfg) {
        this.dim = Math.max(16, cfg.getStubDimension());
    }

    @Override
    public String name() { return "stub"; }

    @Override
    public String model() { return "stub-hash"; }

    @Override
    public List<float[]> embed(List<String> inputs) {
        List<float[]> out = new ArrayList<>(inputs.size());
        for (String in : inputs) out.add(vec(in));
        return out;
    }

    private float[] vec(String text) {
        float[] v = new float[dim];
        for (String tok : Tokenizer.tokenize(text == null ? "" : text)) {
            int h = (tok.hashCode() & 0x7fffffff) % dim;
            v[h] += 1.0f;
        }
        double norm = 0;
        for (float f : v) norm += (double) f * f;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dim; i++) v[i] /= (float) norm;
        return v;
    }
}
