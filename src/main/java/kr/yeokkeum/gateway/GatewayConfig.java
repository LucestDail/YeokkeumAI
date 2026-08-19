package kr.yeokkeum.gateway;

import kr.yeokkeum.config.YeokkeumProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    /**
     * provider: springai(eGov AI layer) | openai_compat(경량 HttpClient) | stub(오프라인) | auto.
     * auto → 키 있으면 Spring AI, 없으면 stub.
     */
    @Bean
    public LlmGateway llmGateway(YeokkeumProperties props, ObjectProvider<ChatModel> chatModelProvider) {
        YeokkeumProperties.Llm cfg = props.getLlm();
        String provider = cfg.getProvider() == null ? "auto" : cfg.getProvider().trim().toLowerCase();
        boolean hasKey = cfg.getApiKey() != null && !cfg.getApiKey().trim().isEmpty();
        ChatModel chatModel = chatModelProvider.getIfAvailable();

        LlmGateway gw;
        switch (provider) {
            case "stub" -> gw = new StubGateway();
            case "openai_compat" -> gw = new OpenAiCompatGateway(cfg);
            case "springai" -> gw = (chatModel != null) ? new SpringAiGateway(chatModel, cfg.getModel())
                                                        : fallbackStub("springai(ChatModel 없음)");
            default -> { // auto
                if (hasKey && chatModel != null) gw = new SpringAiGateway(chatModel, cfg.getModel());
                else gw = new StubGateway();
            }
        }
        log.info("LLM 게이트웨이 = {} (model={})", gw.name(), gw.model());
        return gw;
    }

    private LlmGateway fallbackStub(String why) {
        log.warn("Spring AI 미가용({}) → stub 폴백", why);
        return new StubGateway();
    }
}
