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

    private String payload(List<ChatMessage> messages, double temp, int maxTokens, boolean stream) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModel());
        body.put("messages", messages);
        body.put("temperature", temp);
        body.put("max_tokens", maxTokens);
        if (stream) body.put("stream", true);
        return om.writeValueAsString(body);
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
