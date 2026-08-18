package kr.yeokkeum.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.yeokkeum.config.IeumProperties;
import org.junit.jupiter.api.Test;

class EmbeddingUnitTest {

    @Test
    void vectorsRoundtripAndCosine() {
        float[] v = { 0.1f, -0.2f, 0.3f, 0.4f };
        assertThat(Vectors.fromBytes(Vectors.toBytes(v))).containsExactly(v);
        assertThat(Vectors.cosine(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(Vectors.cosine(new float[] { 1, 0 }, new float[] { 0, 1 })).isEqualTo(0.0);
        assertThat(Vectors.cosine(new float[] { 1, 0 }, new float[] { 1, 2, 3 })).isEqualTo(0.0); // 차원 불일치
    }

    @Test
    void stubEmbedderIsDeterministicAndLexicallyMeaningful() {
        IeumProperties.Embedding cfg = new IeumProperties().getEmbedding();
        StubEmbeddingGateway ex = new StubEmbeddingGateway(cfg);

        float[] a1 = ex.embedOne("웹접근성 KWCAG 기준을 준수");
        float[] a2 = ex.embedOne("웹접근성 KWCAG 기준을 준수");
        assertThat(a1).containsExactly(a2); // 결정적

        float[] related = ex.embedOne("웹접근성 준수 여부");
        float[] unrelated = ex.embedOne("점심 메뉴 추천");
        // 어휘 겹침이 큰 쪽이 코사인 유사도 더 큼
        assertThat(Vectors.cosine(a1, related)).isGreaterThan(Vectors.cosine(a1, unrelated));
    }

    @Test
    void batchPreservesOrder() {
        StubEmbeddingGateway ex = new StubEmbeddingGateway(new IeumProperties().getEmbedding());
        List<float[]> out = ex.embed(List.of("가", "나", "다"));
        assertThat(out).hasSize(3);
        assertThat(out.get(0)).containsExactly(ex.embedOne("가"));
    }
}
