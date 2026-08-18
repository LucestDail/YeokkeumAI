package kr.yeokkeum.config;

import kr.yeokkeum.gateway.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupChecks {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final IeumProperties props;
    private final LlmGateway gateway;

    public StartupChecks(IeumProperties props, LlmGateway gateway) {
        this.props = props;
        this.gateway = gateway;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        IeumProperties.Auth a = props.getAuth();
        boolean noTokens = isBlank(a.getAdminToken()) && isBlank(a.getUserToken());
        if (noTokens) {
            if (a.isInsecureOpenMode()) {
                log.warn("SECURITY: 토큰 미설정 + INSECURE_OPEN_MODE=true → /api/** OPEN(로컬 전용).");
            } else {
                log.error("SECURITY: 토큰 미설정 → secure-by-default CLOSED. ADMIN_TOKEN/USER_TOKEN 설정 필요.");
            }
        }
        log.info("엮음AI 준비 완료 · gateway={} · model={}", gateway.name(), gateway.model());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
