package kr.yeokkeum.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * 정부24(plus.gov.kr) AI 연동 클라이언트.
 * makeCnvrsId → fabrix/chat(SSE)를 읽어 CHUNK 토큰을 onToken 으로 흘리고(스트리밍 패스스루),
 * step progress 는 onProgress 로, 종료 시 출처·신뢰도(Meta)를 반환한다.
 */
@Component
public class Gov24Client {

    private static final String BASE = System.getenv().getOrDefault("GOV24_BASE_URL", "https://plus.gov.kr");

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public record Meta(List<String> sources, int confidence) {}

    public Meta stream(String query, Consumer<String> onToken, Consumer<String> onProgress) throws Exception {
        String cid = makeConversation(query);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cnvrsId", cid);
        body.put("cnvrsQstnCn", query);
        body.put("modelSource", "samsung");
        body.put("location", "");
        body.put("mode", "search");
        body.put("userId", "");
        body.put("unityLgnUseYn", "");
        HttpRequest req = base(BASE + "/ai/search_beta/api/fabrix/chat")
                .timeout(Duration.ofSeconds(90))
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();

        HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("정부24 chat HTTP " + res.statusCode());

        List<String> sources = new ArrayList<>();
        int confidence = -1;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.equals("[DONE]") || data.isEmpty()) continue;
                JsonNode o;
                try { o = om.readTree(data); } catch (Exception ex) { continue; }
                if ("CHUNK".equals(o.path("event_status").asText())) {
                    String c = o.path("content").asText("");
                    if (!c.isEmpty()) onToken.accept(c);
                } else if (o.hasNonNull("progress")) {
                    onProgress.accept(o.path("progress").asText());
                }
                if (o.has("confidence") && confidence < 0) confidence = o.path("confidence").asInt(-1);
                if (o.has("content_references")) collectSources(o.get("content_references"), sources);
            }
        }
        return new Meta(sources, confidence);
    }

    private String makeConversation(String query) throws Exception {
        Map<String, Object> body = Map.of("cnvrsQstnCn", query, "mode", "search", "userId", "");
        HttpRequest req = base(BASE + "/ai/search_beta/chat/makeCnvrsId")
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("makeCnvrsId HTTP " + res.statusCode());
        JsonNode n = om.readTree(res.body());
        if (!n.path("ok").asBoolean(false)) throw new IllegalStateException("makeCnvrsId ok=false");
        return n.path("cnvrsId").asText();
    }

    /** 출처 목록: service_name (+ notice url). */
    public List<String> collectSources(JsonNode refs, List<String> out) {
        if (refs == null || !refs.isArray()) return out;
        for (JsonNode group : refs) {
            JsonNode inner = group.path("references");
            if (!inner.isArray()) continue;
            for (JsonNode r : inner) {
                JsonNode add = r.path("sourceInfo").path("additionalInfo");
                String name = add.path("service_name").asText(r.path("title").asText(""));
                String url = add.path("notice").asText("");
                if (!name.isBlank()) out.add(url.isBlank() ? name : name + " (" + url + ")");
            }
        }
        return out;
    }

    private HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", BASE + "/")
                .header("Origin", BASE);
    }
}
