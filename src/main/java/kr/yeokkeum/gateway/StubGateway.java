package kr.yeokkeum.gateway;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** 오프라인 결정적 게이트웨이 — 키 없이 파이프라인(채팅·요약·RAG 인용) 시연/검증. 폐쇄망 데모용. */
public class StubGateway implements LlmGateway {

    @Override
    public String name() { return "stub"; }

    @Override
    public String model() { return "stub"; }

    @Override
    public ChatResult chat(List<ChatMessage> messages, double temperature, int maxTokens) {
        String sys = messages.stream().filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content).findFirst().orElse("");
        String lastUser = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role())) { lastUser = messages.get(i).content(); break; }
        }
        String text;
        if (sys.contains("요약") || sys.toLowerCase().contains("summ")) {
            text = "[요약·stub] " + naiveSummary(lastUser);
        } else if (sys.contains("근거")) {
            text = "[근거기반·stub] 제공된 근거를 바탕으로 답변합니다. " + clip(lastUser, 120);
        } else {
            text = "[엮음AI·stub 응답] 입력을 확인했습니다: " + clip(lastUser, 200);
        }
        return new ChatResult(text, Map.of("prompt_chars", lastUser.length(), "completion_chars", text.length()));
    }

    @Override
    public void stream(List<ChatMessage> messages, double temperature, int maxTokens, Consumer<String> onToken) {
        String text = chat(messages, temperature, maxTokens).text();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            buf.append(c);
            if (c == ' ' || c == '\n' || "。.!?,，".indexOf(c) >= 0) {
                onToken.accept(buf.toString());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) onToken.accept(buf.toString());
    }

    private static String clip(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n);
    }

    private static String naiveSummary(String text) {
        String[] parts = text.replace("\n", " ").replace("。", ".").split("\\.");
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for (String p : parts) {
            p = p.trim();
            if (p.isEmpty()) continue;
            sb.append(p).append(". ");
            if (++c >= 3) break;
        }
        return sb.length() == 0 ? clip(text, 200) : sb.toString().trim();
    }
}
