package kr.yeokkeum.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.yeokkeum.config.IeumProperties;

/**
 * OpenAI 호환 /embeddings — BGE-M3(HuggingFace TEI·vLLM·사내/국산 게이트웨이) 연결.
 * 요청: {model, input:[...]}, 응답: {data:[{embedding:[...]}, ...]}.
 */
public class OpenAiCompatEmbeddingGateway implements EmbeddingGateway {

    private final IeumProperties.Embedding cfg;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient client;

    public OpenAiCompatEmbeddingGateway(IeumProperties.Embedding cfg) {
        this.cfg = cfg;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(cfg.getTimeoutSeconds(), 30))).build();
    }

    @Override
    public String name() { return "openai_compat"; }

    @Override
    public String model() { return cfg.getModel(); }

    private String url() {
        String b = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + "/embeddings";
    }

    @Override
    public List<float[]> embed(List<String> inputs) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", cfg.getModel());
            body.put("input", inputs);
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url()))
                    .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                    .header("Content-Type", "application/json");
            String key = cfg.getApiKey() == null ? "" : cfg.getApiKey().trim();
            if (!key.isEmpty()) req.header("Authorization", "Bearer " + key);
            req.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)));

            HttpResponse<String> res = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new EmbeddingException("임베딩 서버 오류 HTTP " + res.statusCode() + ": " + snippet(res.body()));
            }
            JsonNode data = om.readTree(res.body()).path("data");
            if (!data.isArray() || data.size() != inputs.size()) {
                throw new EmbeddingException("임베딩 응답 형식 오류(data 크기 불일치)");
            }
            List<float[]> out = new ArrayList<>(inputs.size());
            for (JsonNode item : data) {
                JsonNode emb = item.path("embedding");
                float[] v = new float[emb.size()];
                for (int i = 0; i < v.length; i++) v[i] = (float) emb.get(i).asDouble();
                out.add(v);
            }
            return out;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("임베딩 호출 실패: " + e.getMessage(), e);
        }
    }

    private static String snippet(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200);
    }

    /** 임베딩 호출/응답 실패 — 상위(RAG)에서 dense 생략하고 BM25 로 폴백. */
    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String m) { super(m); }
        public EmbeddingException(String m, Throwable c) { super(m, c); }
    }
}
