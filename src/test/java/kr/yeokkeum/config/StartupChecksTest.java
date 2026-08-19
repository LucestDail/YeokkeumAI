package kr.yeokkeum.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StartupChecksTest {

    private IeumProperties propsWith(boolean insecure) {
        IeumProperties p = new IeumProperties();
        p.getAuth().setInsecureOpenMode(insecure);
        return p;
    }

    @Test
    void prodWithInsecureOpenModeFailsFast() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        StartupChecks sc = new StartupChecks(propsWith(true), null, env);
        assertThatThrownBy(sc::guardInsecureOpenMode)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INSECURE_OPEN_MODE");
    }

    @Test
    void devWithInsecureOpenModeAllowed() {
        MockEnvironment env = new MockEnvironment(); // 프로파일 없음(dev)
        StartupChecks sc = new StartupChecks(propsWith(true), null, env);
        assertThatCode(sc::guardInsecureOpenMode).doesNotThrowAnyException();
    }
}
