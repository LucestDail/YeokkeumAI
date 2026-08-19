package kr.yeokkeum.doc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import kr.yeokkeum.config.YeokkeumProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HWP/HWPX 텍스트 추출 — Rust 오픈소스 rhwp(export-text) 바이너리에 위임.
 * 근거: rhwp(github.com/edwardkim/rhwp, MIT)가 HWP5/HWPX/HML을 페이지별 TXT로 추출.
 * 폐쇄망/온프렘: 바이너리 동봉(bin/rhwp) 또는 RHWP_PATH 로 지정. 미설치 시 명확히 실패(할루시네이션 금지).
 */
@Component
public class HwpExtractor {

    private static final Logger log = LoggerFactory.getLogger(HwpExtractor.class);

    private final YeokkeumProperties props;

    public HwpExtractor(YeokkeumProperties props) {
        this.props = props;
    }

    /** RHWP_PATH(env) > yeokkeum.doc.rhwp-path(bin/rhwp) > PATH의 "rhwp". */
    private String resolveBinary() {
        String env = System.getenv("RHWP_PATH");
        if (env != null && !env.isBlank()) return env.trim();
        String configured = props.getDoc().getRhwpPath();
        if (configured != null && !configured.isBlank() && Files.isRegularFile(Path.of(configured))) {
            return configured;
        }
        return "rhwp"; // PATH 조회
    }

    /**
     * HWP/HWPX 바이트에서 전체 텍스트 추출(페이지 결합).
     * @throws HwpExtractionException 바이너리 미설치·실행 실패·빈 결과
     */
    public String extract(String filename, byte[] bytes) {
        String ext = filename != null && filename.toLowerCase().endsWith(".hwpx") ? ".hwpx" : ".hwp";
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("yk-hwp-");
            Path input = tmpDir.resolve("input" + ext);
            Path outDir = tmpDir.resolve("out");
            Files.write(input, bytes);
            Files.createDirectories(outDir);

            String bin = resolveBinary();
            Process proc;
            try {
                proc = new ProcessBuilder(bin, "export-text", input.toString(), "-o", outDir.toString())
                        .redirectErrorStream(true)
                        .start();
            } catch (IOException e) {
                throw new HwpExtractionException(
                        "HWP 파서(rhwp) 실행 불가 — 바이너리 미설치 또는 경로 오류(" + bin + "). "
                        + "bin/rhwp 동봉 또는 RHWP_PATH 설정 필요.", e);
            }

            String consoleOut = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = proc.waitFor(props.getDoc().getRhwpTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new HwpExtractionException("HWP 파싱 시간 초과(" + props.getDoc().getRhwpTimeoutMs() + "ms)", null);
            }
            if (proc.exitValue() != 0) {
                throw new HwpExtractionException("HWP 파싱 실패(exit " + proc.exitValue() + "): "
                        + snippet(consoleOut), null);
            }

            String text = readAndJoin(outDir);
            if (text.isBlank()) {
                throw new HwpExtractionException("HWP에서 추출된 텍스트가 없습니다(스캔 이미지 문서일 수 있음).", null);
            }
            return text;
        } catch (IOException e) {
            throw new UncheckedIOException("HWP 임시파일 처리 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HwpExtractionException("HWP 파싱 중단됨", e);
        } finally {
            deleteQuietly(tmpDir);
        }
    }

    private static String readAndJoin(Path outDir) throws IOException {
        try (Stream<Path> files = Files.list(outDir)) {
            List<Path> txts = files
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".txt"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
            StringBuilder sb = new StringBuilder();
            for (Path p : txts) {
                sb.append(Files.readString(p, StandardCharsets.UTF_8));
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
                sb.append('\n');
            }
            return sb.toString().strip();
        }
    }

    private static String snippet(String s) {
        if (s == null) return "";
        String t = s.strip();
        return t.length() <= 300 ? t : t.substring(0, 300);
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { /* best-effort */ }
            });
        } catch (IOException e) {
            log.warn("임시 디렉토리 정리 실패: {}", dir, e);
        }
    }

    /** HWP 추출 실패(바이너리 미설치/파싱오류/빈결과) — 컨트롤러에서 415/503 매핑. */
    public static class HwpExtractionException extends RuntimeException {
        public HwpExtractionException(String message, Throwable cause) { super(message, cause); }
    }
}
