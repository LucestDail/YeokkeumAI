package kr.yeokkeum.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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

    private Prompt toPrompt(List<ChatMessage> messages) {
        List<Message> sm = new ArrayList<>();
        for (ChatMessage m : messages) {
            switch (m.role() == null ? "user" : m.role()) {
                case "system" -> sm.add(new SystemMessage(m.content()));
                case "assistant" -> sm.add(new AssistantMessage(m.content()));
                default -> sm.add(new UserMessage(m.content()));
            }
        }
        return new Prompt(sm);
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, double temperature, int maxTokens) {
        ChatResponse resp = chatModel.call(toPrompt(messages));
        String text = resp.getResult() != null ? resp.getResult().getOutput().getText() : "";
        return new ChatResult(text == null ? "" : text, Map.of());
    }

    @Override
    public void stream(List<ChatMessage> messages, double temperature, int maxTokens, Consumer<String> onToken) {
        chatModel.stream(toPrompt(messages)).toStream().forEach(cr -> {
            if (cr.getResult() == null) return;
            String piece = cr.getResult().getOutput().getText();
            if (piece != null && !piece.isEmpty()) onToken.accept(piece);
        });
    }
}
