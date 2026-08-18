package kr.yeokkeum.embedding;

import kr.yeokkeum.config.IeumProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 임베더 선택: provider=stub → Stub, openai_compat → 엔드포인트,
 * auto → base-url 있으면 엔드포인트(BGE-M3) 아니면 Stub(오프라인).
 */
@Configuration
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

    @Bean
    public EmbeddingGateway embeddingGateway(IeumProperties props) {
        IeumProperties.Embedding cfg = props.getEmbedding();
        String provider = cfg.getProvider() == null ? "auto" : cfg.getProvider().trim().toLowerCase();
        boolean hasEndpoint = cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank();

        if ("stub".equals(provider)) {
            log.info("Embedding: stub(오프라인 결정적) — dense 검색은 데모/테스트 품질");
            return new StubEmbeddingGateway(cfg);
        }
        if ("openai_compat".equals(provider) || ("auto".equals(provider) && hasEndpoint)) {
            log.info("Embedding: openai_compat model={} base-url={}", cfg.getModel(), cfg.getBaseUrl());
            return new OpenAiCompatEmbeddingGateway(cfg);
        }
        log.info("Embedding: 엔드포인트 미구성 → stub 폴백(하이브리드는 BM25 우위로 동작)");
        return new StubEmbeddingGateway(cfg);
    }
}
