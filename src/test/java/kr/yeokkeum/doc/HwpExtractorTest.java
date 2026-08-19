package kr.yeokkeum.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import kr.yeokkeum.config.YeokkeumProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * rhwp 바이너리 연동 통합 스모크 — 바이너리가 현재 플랫폼에서 실제 실행 가능할 때만 수행.
 * (동봉 바이너리는 플랫폼별이므로 실행 불가 시 assume 으로 스킵)
 */
class HwpExtractorTest {

    private final YeokkeumProperties props = new YeokkeumProperties();

    private boolean rhwpRunnable() {
        try {
            Process p = new ProcessBuilder(props.getDoc().getRhwpPath(), "--version")
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    void extractsHangulFromHwp() throws IOException {
        Assumptions.assumeTrue(rhwpRunnable(),
                "rhwp 바이너리(" + props.getDoc().getRhwpPath() + ")가 이 플랫폼에서 실행 불가 → 스킵");
        HwpExtractor ex = new HwpExtractor(props);
        byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/samples/hangul-sample.hwp"));
        String text = ex.extract("hangul-sample.hwp", bytes);
        assertThat(text).contains("가나다라마바사");
    }

    @Test
    void missingBinaryFailsClearly() {
        YeokkeumProperties p = new YeokkeumProperties();
        p.getDoc().setRhwpPath("/nonexistent/definitely-not-rhwp");
        HwpExtractor ex = new HwpExtractor(p);
        try {
            ex.extract("x.hwp", new byte[] {1, 2, 3});
        } catch (HwpExtractor.HwpExtractionException e) {
            assertThat(e.getMessage()).contains("rhwp");
            return;
        }
        throw new AssertionError("바이너리 미설치 시 HwpExtractionException 을 던져야 함");
    }
}
