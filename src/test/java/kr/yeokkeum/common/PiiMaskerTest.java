package kr.yeokkeum.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    @Test
    void masksResidentNumber() {
        assertThat(PiiMasker.mask("주민번호 900101-1234567 입니다"))
                .contains("900101-*******").doesNotContain("1234567");
    }

    @Test
    void masksPhone() {
        assertThat(PiiMasker.mask("연락처 010-1234-5678")).contains("010-****-5678").doesNotContain("1234");
    }

    @Test
    void masksEmail() {
        assertThat(PiiMasker.mask("메일 hong@example.com"))
                .contains("h***@example.com").doesNotContain("hong@");
    }

    @Test
    void masksCard() {
        assertThat(PiiMasker.mask("카드 1234-5678-9012-3456"))
                .contains("1234-****-****-3456").doesNotContain("5678-9012");
    }

    @Test
    void leavesNormalTextUntouched() {
        assertThat(PiiMasker.mask("filename=규정.txt nChunks=3")).isEqualTo("filename=규정.txt nChunks=3");
    }
}
