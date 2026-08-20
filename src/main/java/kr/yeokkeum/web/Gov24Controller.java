package kr.yeokkeum.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import kr.yeokkeum.agent.Gov24Client;
import kr.yeokkeum.audit.AuditService;
import kr.yeokkeum.auth.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 정부24 AI 스트리밍 프록시 — 정부24 SSE(CHUNK)를 우리 클라이언트로 그대로 흘린다.
 * 프론트의 SSE 파서(data:{"t":tok}/[DONE])와 동일 포맷. 진행상태는 data:{"p":...}.
 */
@RestController
public class Gov24Controller {

    private static final long SSE_TIMEOUT_MS = 120_000;

    private final Gov24Client client;
    private final AuditService audit;
    private final ExecutorService exec = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32),
            new ThreadPoolExecutor.CallerRunsPolicy());

    public Gov24Controller(Gov24Client client, AuditService audit) {
        this.client = client;
        this.audit = audit;
    }

    @PostMapping(value = "/api/gov24/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Principal p = (Principal) req.getAttribute(Principal.ATTR);
        String query = body == null ? "" : body.getOrDefault("query", "");
        audit.record(p.actor(), p.role(), "gov24_chat", Map.of("chars", query.length()));

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> { });
        exec.execute(() -> {
            try {
                if (query.isBlank()) { send(emitter, Map.of("t", "질문을 입력하세요.")); done(emitter); return; }
                Gov24Client.Meta meta = client.stream(query,
                        tok -> send(emitter, Map.of("t", tok)),
                        prog -> send(emitter, Map.of("p", prog)));
                StringBuilder tail = new StringBuilder();
                if (meta.confidence() >= 0) tail.append("\n\n신뢰도(정부24): ").append(meta.confidence());
                if (!meta.sources().isEmpty()) {
                    tail.append("\n\n**[정부24 출처]**");
                    meta.sources().stream().distinct().limit(5).forEach(s -> tail.append("\n- ").append(s));
                }
                if (tail.length() > 0) send(emitter, Map.of("t", tail.toString()));
                done(emitter);
            } catch (Exception e) {
                try {
                    send(emitter, Map.of("t", "정부24 연동 실패: " + e.getMessage()));
                    done(emitter);
                } catch (Exception ignored) {
                    emitter.completeWithError(e);
                }
            }
        });
        return emitter;
    }

    private static void send(SseEmitter e, Map<String, Object> data) {
        try { e.send(SseEmitter.event().data(data)); } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private static void done(SseEmitter e) {
        try { e.send(SseEmitter.event().data("[DONE]")); e.complete(); } catch (Exception ignored) { }
    }
}
