package kr.yeokkeum.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 구조 규칙 — <b>외부로 텍스트를 보내는 길에 마스킹이 빠지지 않게</b> 한다 [R3].
 *
 * <h2>왜 필요한가</h2>
 *
 * {@link OutboundPii} 자체는 7개 테스트로 초록불이지만, <b>호출부를 지워도 전부 통과한다.</b>
 * 실제로 이 저장소는 {@code PiiMasker} 를 <b>2026-09-16 까지 {@code AuditService} 한 곳에서만</b>
 * 쓰고 있었다 — 로그에는 안 남기면서 <b>외부 모델에는 원문을 보내고</b> 있었다.
 * (이 워크스페이스가 열 번 넘게 기록한 <i>"수집해 놓고 안 쓰는"</i> 패턴.)
 *
 * <h2>🔴 나가는 길이 둘이다</h2>
 *
 * 채팅({@code /chat/completions})과 <b>임베딩</b>({@code /embeddings}). 임베딩도 RAG 청크
 * 원문을 보내므로 같은 위험인데, <b>한 곳만 고치면 반쪽</b>이다.
 * 그래서 <b>파일 목록에서 거꾸로 계정한다</b> — 외부로 나가는 게이트웨이는 전부
 * {@code 마스킹함} / {@code 면제됨(이유)} 중 하나여야 하고, 어디에도 안 들면 실패한다.
 */
class OutboundPiiWiringRuleTest {

    private static final Path MAIN = Path.of("src/main/java/kr/yeokkeum");

    /** 면제 — <b>이유 없는 면제는 곧 구멍이다.</b> */
    private static final Map<String, String> EXEMPT = Map.of(
            "agent/Gov24Client.java",
            "공공데이터포털 조회 전용 — 사용자 문서를 보내지 않고 검색어(기관명·서비스명)만 보낸다"
    );
    // ⚠️ `GatewayConfig` 를 넣었다가 뺐다 — HTTP 를 쏘지 않아 **애초에 대상이 아니다.**
    //    대상도 아닌 것을 면제하면 그건 장식이고, 아래 exemptionsAreReal 이 그걸 잡았다.

    private static List<Path> egressFiles() throws IOException {
        assertThat(Files.isDirectory(MAIN))
                .withFailMessage("검사 대상을 못 찾았다: %s (패키지 루트에서 돌려야 한다)", MAIN.toAbsolutePath())
                .isTrue();
        try (Stream<Path> w = Files.walk(MAIN)) {
            List<Path> out = new ArrayList<>();
            for (Path p : w.filter(x -> x.toString().endsWith(".java")).toList()) {
                String src = Files.readString(p);
                // 외부로 HTTP 를 쏘는 파일
                if (src.contains("HttpRequest.newBuilder") || src.contains("BodyPublishers.ofString")) {
                    out.add(p);
                }
            }
            return out;
        }
    }

    private static String rel(Path p) {
        return MAIN.relativize(p).toString();
    }

    @Test
    @DisplayName("검사 대상이 실제로 있다 (0건 통과가 가장 나쁜 실패다)")
    void hasTargets() throws IOException {
        assertThat(egressFiles()).hasSizeGreaterThanOrEqualTo(2);
    }

    /**
     * 🔴 <b>거꾸로 계정한다.</b> "몇 개가 통과했나" 가 아니라
     * <b>모든 외부 송신 파일이 {@code 마스킹함}/{@code 면제됨(이유)} 중 하나인가</b>를 본다.
     * 새 게이트웨이가 생기면 자동으로 강제된다.
     */
    @Test
    @DisplayName("외부로 텍스트를 보내는 모든 곳이 마스킹하거나, 이유와 함께 면제된다")
    void everyEgressIsAccounted() throws IOException {
        List<String> unmasked = new ArrayList<>();
        List<String> accounted = new ArrayList<>();

        for (Path p : egressFiles()) {
            String name = rel(p);
            if (EXEMPT.containsKey(name)) {
                accounted.add(name);
                continue;
            }
            String src = Files.readString(p);
            if (src.contains("OutboundPii")) {
                accounted.add(name);
                continue;
            }
            unmasked.add(name);
        }

        assertThat(unmasked)
                .withFailMessage("외부로 텍스트를 보내면서 마스킹하지 않는 곳: %s%n"
                        + "→ OutboundPii 를 걸거나, 안 걸어도 되는 이유를 EXEMPT 에 적어라", unmasked)
                .isEmpty();
        assertThat(accounted).hasSameSizeAs(egressFiles());
    }

    /** ⚠️ 면제 항목이 <b>장식이 아닌지</b> — 사라진 파일을 면제해 두면 다음에 같은 이름이 생겨도 조용히 봐준다. */
    @Test
    @DisplayName("면제 항목이 전부 실재한다")
    void exemptionsAreReal() throws IOException {
        List<String> names = egressFiles().stream().map(OutboundPiiWiringRuleTest::rel).toList();
        List<String> ghosts = EXEMPT.keySet().stream().filter(k -> !names.contains(k)).toList();
        assertThat(ghosts).withFailMessage("실재하지 않는 파일을 면제하고 있다: %s", ghosts).isEmpty();
    }

    /** 🔴 <b>둘 다</b> 걸려 있어야 한다. 한 곳만 고치면 반쪽이다. */
    @Test
    @DisplayName("채팅과 임베딩 양쪽에 걸려 있다")
    void bothGatewaysMasked() throws IOException {
        for (String f : List.of("gateway/OpenAiCompatGateway.java",
                                "embedding/OpenAiCompatEmbeddingGateway.java")) {
            assertThat(Files.readString(MAIN.resolve(f)))
                    .withFailMessage("%s 에 마스킹이 없다 — 나가는 길이 둘인데 한쪽만 막으면 반쪽이다", f)
                    .contains("OutboundPii");
        }
    }

    /**
     * ⚠️ <b>가린 내용 자체를 로그에 쓰면 마스킹한 의미가 없다.</b>
     * 개수만 남기는지 확인한다.
     */
    @Test
    @DisplayName("로그에 가린 내용을 남기지 않는다 (개수만)")
    void logsCountNotContent() throws IOException {
        for (String f : List.of("gateway/OpenAiCompatGateway.java",
                                "embedding/OpenAiCompatEmbeddingGateway.java")) {
            String src = Files.readString(MAIN.resolve(f));
            int at = src.indexOf("[pii]");
            assertThat(at).withFailMessage("%s 에 [pii] 로그가 없다 — 0건이면 안 돈 것을 알려줄 수 없다", f)
                    .isGreaterThan(0);
            /*
             * ⚠️ **로그문 자체만 본다.** 처음엔 뒤 400자를 통째로 봤다가 **로그 다음 줄의
             *    `r.texts()`(마스킹 결과를 다시 조립하는 정상 코드)를 위반으로 찍었다** —
             *    제품이 아니라 자가 틀린 것이다(2026-09-16, 같은 실수를 어제도 했다).
             */
            int end = src.indexOf(");", at);
            assertThat(end).isGreaterThan(at);
            String stmt = src.substring(at, end);
            assertThat(stmt)
                    .withFailMessage("%s 의 [pii] 로그가 텍스트 원본을 찍는다: %s", f, stmt)
                    .doesNotContain(".texts()")
                    .doesNotContain("content");
        }
    }
}
