package kr.yeokkeum.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * eGovFrame 5.0 AI layer 정렬 — Spring AI ChatModel(OpenAI 호환).
 * base-url 로 OpenRouter·사내 게이트웨이·국산 K-AI·vLLM 연결.
 */
public class SpringAiGateway implements LlmGateway {

    private final ChatModel chatModel;
    private final String model;

    public SpringAiGateway(ChatModel chatModel, String model) {
        this.chatModel = chatModel;
        this.model = model;
    }

    @Override
    public String name() { return "springai"; }

    @Override
    public String model() { return model; }

    private List<Message> toMessages(List<ChatMessage> messages) {
        List<Message> sm = new ArrayList<>();
        for (ChatMessage m : messages) {
            switch (m.role() == null ? "user" : m.role()) {
                case "system" -> sm.add(new SystemMessage(m.content()));
                case "assistant" -> sm.add(new AssistantMessage(m.content()));
                default -> sm.add(new UserMessage(m.content()));
            }
        }
        return sm;
    }

    private Prompt toPrompt(List<ChatMessage> messages, double temperature, int maxTokens) {
        ChatOptions opts = ChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .model(model)
                .build();
        return new Prompt(toMessages(messages), opts);
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, double temperature, int maxTokens) {
        ChatResponse resp = chatModel.call(toPrompt(messages, temperature, maxTokens));
        String text = resp.getResult() != null ? resp.getResult().getOutput().getText() : "";
        return new ChatResult(text == null ? "" : text, usageOf(resp));
    }

    private static Map<String, Object> usageOf(ChatResponse resp) {
        if (resp.getMetadata() == null || resp.getMetadata().getUsage() == null) return Map.of();
        Usage u = resp.getMetadata().getUsage();
        return Map.of(
                "promptTokens", nz(u.getPromptTokens()),
                "completionTokens", nz(u.getCompletionTokens()),
                "totalTokens", nz(u.getTotalTokens()));
    }

    private static long nz(Integer v) {
        return v == null ? 0L : v.longValue();
    }

    @Override
    public void stream(List<ChatMessage> messages, double temperature, int maxTokens, Consumer<String> onToken) {
        chatModel.stream(toPrompt(messages, temperature, maxTokens)).toStream().forEach(cr -> {
            if (cr.getResult() == null) return;
            String piece = cr.getResult().getOutput().getText();
            if (piece != null && !piece.isEmpty()) onToken.accept(piece);
        });
    }
}
