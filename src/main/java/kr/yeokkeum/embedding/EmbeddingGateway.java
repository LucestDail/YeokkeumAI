package kr.yeokkeum.embedding;

import java.util.List;

/** 임베딩(dense 벡터) 게이트웨이 — 벤더무관. BGE-M3 등을 OpenAI 호환 /embeddings 로, 또는 오프라인 stub. */
public interface EmbeddingGateway {

    String name();

    String model();

    /** 여러 입력을 배치 임베딩. 반환 순서는 입력 순서와 일치. */
    List<float[]> embed(List<String> inputs);

    default float[] embedOne(String text) {
        return embed(List.of(text)).get(0);
    }
}
