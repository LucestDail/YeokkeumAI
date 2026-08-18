package kr.yeokkeum.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kr.yeokkeum.audit.AuditService;
import kr.yeokkeum.auth.Principal;
import kr.yeokkeum.gateway.ChatMessage;
import kr.yeokkeum.gateway.LlmGateway;
import kr.yeokkeum.web.dto.ChatRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {

    private final LlmGateway gateway;
    private final AuditService audit;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    public ChatController(LlmGateway gateway, AuditService audit) {
        this.gateway = gateway;
        this.audit = audit;
    }

    @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest body, HttpServletRequest req) {
        Principal p = (Principal) req.getAttribute(Principal.ATTR);
        List<ChatMessage> messages = new ArrayList<>();
        if (body.system() != null && !body.system().isBlank()) messages.add(ChatMessage.system(body.system()));
        messages.add(ChatMessage.user(body.message()));
        audit.record(p.actor(), p.role(), "chat", Map.of("model", gateway.model(), "chars", body.message().length()));

        SseEmitter emitter = new SseEmitter(0L);  // no timeout
        exec.execute(() -> {
            try {
                gateway.stream(messages, 0.3, 1024, tok -> {
                    try {
                        emitter.send(SseEmitter.event().data(Map.of("t", tok)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
