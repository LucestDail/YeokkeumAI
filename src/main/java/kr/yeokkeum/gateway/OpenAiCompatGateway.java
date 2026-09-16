package kr.yeokkeum.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import kr.yeokkeum.common.OutboundPii;
import kr.yeokkeum.config.YeokkeumProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OpenAI 호환 /chat/completions. OpenRouter·사내 게이트웨이·국산 K-AI·vLLM 등 연결. */
public class OpenAiCompatGateway implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatGateway.class);

    private final YeokkeumProperties.Llm cfg;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient client;

    public OpenAiCompatGateway(YeokkeumProperties.Llm cfg) {
        this.cfg = cfg;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(cfg.getTimeoutSeconds(), 30))).build();
    }

    @Override
    public String name() { return "openai_compat"; }

    @Override
    public String model() { return cfg.getModel(); }

    private String url() {
        String b = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + "/chat/completions";
    }

    private HttpRequest.Builder req() {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url()))
                .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                .header("Content-Type", "application/json");
        String key = cfg.getApiKey() == null ? "" : cfg.getApiKey().trim();
        if (!key.isEmpty()) b.header("Authorization", "Bearer " + key);
        return b;
    }

    /**
     * 🔴 <b>여기가 외부로 나가는 유일한 길목이다</b> — {@code chat()} 과 {@code stream()} 이 둘 다 이걸 쓴다.
     * 그래서 마스킹을 <b>여기 한 곳</b>에 건다. 호출부마다 걸면 언젠가 한 곳이 빠진다.
     */
    private String payload(List<ChatMessage> messages, double temp, int maxTokens, boolean stream) throws Exception {
        List<ChatMessage> outbound = maskOutbound(messages);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModel());
        body.put("messages", outbound);
        body.put("temperature", temp);
        body.put("max_tokens", maxTokens);
        if (stream) body.put("stream", true);
        return om.writeValueAsString(body);
    }

    /**
     * 나가는 메시지의 개인정보를 가린다 [R3].
     *
     * <p>⚠️ <b>몇 건 가렸는지 로그로 낸다</b> — 0 이면 "설정은 켰는데 실제로는 아무것도 안 가렸다" 는
     * 뜻이고, 그걸 결과가 스스로 알려 주게 해야 "적용된 척" 이 불가능하다.
     * <p>🔴 <b>가린 내용 자체는 로그에 쓰지 않는다.</b> 그러면 마스킹한 의미가 없다.
     */
    private List<ChatMessage> maskOutbound(List<ChatMessage> messages) {
        boolean on = cfg.isMaskPii();
        if (!on || messages == null || messages.isEmpty()) return messages;

        List<String> contents = messages.stream().map(ChatMessage::content).toList();
        OutboundPii.Result r = OutboundPii.maskAll(contents, true);
        if (!r.changed()) return messages;

        log.info("[pii] 외부 모델로 나가기 전 {}개 메시지에서 개인정보를 가렸습니다 (전체 {}개)",
                r.maskedCount(), messages.size());
        List<ChatMessage> out = new java.util.ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            out.add(new ChatMessage(messages.get(i).role(), r.texts().get(i)));
        }
        return out;
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, double temperature, int maxTokens) {
        try {
            HttpResponse<String> r = client.send(
                    req().POST(HttpRequest.BodyPublishers.ofString(payload(messages, temperature, maxTokens, false)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) {
                throw new RuntimeException("LLM HTTP " + r.statusCode() + ": " + clip(r.body(), 300));
            }
            JsonNode root = om.readTree(r.body());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            Map<String, Object> usage = Map.of();
            if (root.has("usage")) usage = om.convertValue(root.get("usage"), Map.class);
            return new ChatResult(text, usage);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("LLM 호출 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public void stream(List<ChatMessage> messages, double temperature, int maxTokens, Consumer<String> onToken) {
        try {
            HttpResponse<Stream<String>> r = client.send(
                    req().POST(HttpRequest.BodyPublishers.ofString(payload(messages, temperature, maxTokens, true)))
                            .build(),
                    HttpResponse.BodyHandlers.ofLines());
            if (r.statusCode() / 100 != 2) {
                throw new RuntimeException("LLM HTTP " + r.statusCode());
            }
            r.body().forEach(line -> {
                if (line == null || !line.startsWith("data:")) return;
                String chunk = line.substring("data:".length()).trim();
                if (chunk.isEmpty() || "[DONE]".equals(chunk)) return;
                try {
                    JsonNode obj = om.readTree(chunk);
                    String piece = obj.path("choices").path(0).path("delta").path("content").asText("");
                    if (!piece.isEmpty()) onToken.accept(piece);
                } catch (Exception ignore) {
                    // 부분 라인/키프얼라이브 무시
                }
            });
        } catch (Exception e) {
            log.warn("stream 실패, 비스트림 폴백: {}", e.getMessage());
            String text = chat(messages, temperature, maxTokens).text();
            onToken.accept(text);
        }
    }

    private static String clip(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }
}
