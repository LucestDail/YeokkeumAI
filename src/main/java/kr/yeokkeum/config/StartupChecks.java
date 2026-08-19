package kr.yeokkeum.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import kr.yeokkeum.gateway.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupChecks {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final YeokkeumProperties props;
    private final LlmGateway gateway;
    private final Environment env;

    public StartupChecks(YeokkeumProperties props, LlmGateway gateway, Environment env) {
        this.props = props;
        this.gateway = gateway;
        this.env = env;
    }

    /** fail-fast: prod 프로파일에서 INSECURE_OPEN_MODE 오배포 금지 [SEC-5]. */
    @PostConstruct
    public void guardInsecureOpenMode() {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (prod && props.getAuth().isInsecureOpenMode()) {
            throw new IllegalStateException(
                    "SECURITY: prod 프로파일에서 INSECURE_OPEN_MODE=true 는 금지입니다(전면 개방 방지). 토큰을 설정하세요.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        YeokkeumProperties.Auth a = props.getAuth();
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
