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
    private final kr.yeokkeum.doc.HwpExtractor hwpExtractor;

    public StartupChecks(
            YeokkeumProperties props,
            LlmGateway gateway,
            Environment env,
            kr.yeokkeum.doc.HwpExtractor hwpExtractor) {
        this.props = props;
        this.gateway = gateway;
        this.env = env;
        this.hwpExtractor = hwpExtractor;
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
        checkHwpBinary();
        log.info("엮음AI 준비 완료 · gateway={} · model={}", gateway.name(), gateway.model());
    }

    /**
     * HWP 추출기(rhwp)가 이 플랫폼에서 실제로 도는지 기동 때 확인한다 [DEPLOY-1].
     *
     * <p>그전에는 <b>업로드하는 순간에야</b> 알 수 있었다 — 저장소에 동봉된 {@code bin/rhwp} 는
     * macOS arm64 이고, 리눅스 서버에는 별도로 x86-64 바이너리를 넣어야 한다. 그 사실이
     * 어디에도 적혀 있지 않아서 재배포하거나 서버를 옮기면 <b>간판 기능이 조용히 죽는다</b>.
     * 기동 로그에 남겨 두면 배포 직후 바로 보인다.
     */
    private void checkHwpBinary() {
        String path = hwpExtractor.resolveBinaryForDiagnostics();
        try {
            Process p = new ProcessBuilder(path, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean done = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .strip();
            if (done && p.exitValue() == 0) {
                log.info("HWP 추출기 사용 가능 · {} ({})", out.isBlank() ? "version?" : out.lines().findFirst().orElse(""), path);
            } else {
                log.warn("HWP 추출기 실행 실패 — HWP/HWPX 업로드는 503 이 된다. path={} out={}", path, out);
            }
        } catch (Exception e) {
            // 동봉 바이너리가 다른 아키텍처면 여기로 온다(macOS arm64 를 리눅스에서 실행하는 등).
            log.warn("HWP 추출기를 쓸 수 없다 — HWP/HWPX 업로드는 503 이 된다. "
                    + "리눅스 x86-64 바이너리를 RHWP_PATH 로 지정하거나 bin/rhwp 를 교체하라 "
                    + "(조달 절차: deploy/RHWP.md). path={} 원인={}", path, e.toString());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
