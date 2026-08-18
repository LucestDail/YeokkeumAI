package kr.yeokkeum.gateway;

import java.util.List;
import java.util.function.Consumer;

/**
 * 벤더무관 LLM 게이트웨이. OpenAI 호환 엔드포인트(OpenRouter·사내 게이트웨이·국산 K-AI·vLLM)
 * 또는 오프라인 stub. base-url/model/api-key 로 핫스왑.
 */
public interface LlmGateway {

    String name();

    String model();

    ChatResult chat(List<ChatMessage> messages, double temperature, int maxTokens);

    /** 토큰을 onToken 으로 흘림(SSE). */
    void stream(List<ChatMessage> messages, double temperature, int maxTokens, Consumer<String> onToken);

    default ChatResult chat(List<ChatMessage> messages) {
        return chat(messages, 0.3, 1024);
    }
}
