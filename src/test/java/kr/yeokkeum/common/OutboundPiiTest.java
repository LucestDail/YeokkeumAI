package kr.yeokkeum.common;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🔴 <b>오탐 테스트를 발동 테스트보다 먼저 쓴다.</b>
 *
 * <p>이 가드는 <b>기능을 망가뜨리는 방향</b>이 있다 — 공공 RFP 문서의 사업자번호·공고번호·금액을
 * 잘못 가리면 모델이 문서를 못 읽는다. 그건 보안이 아니라 <b>제품이 나빠지는 것</b>이다.
 * (이 워크스페이스 09-11 기록: <i>"가드의 오탐 테스트가 발동 테스트보다 중요할 때가 있다"</i>.)
 */
class OutboundPiiTest {

    // ── 오탐: 가리면 안 되는 것 ─────────────────────────────────

    @Test
    @DisplayName("🔴 공공문서에 흔한 숫자를 가리지 않는다 — 가리면 모델이 문서를 못 읽는다")
    void doesNotMaskOrdinaryGovDocNumbers() {
        List<String> safe = List.of(
                "사업자등록번호 123-45-67890",
                "공고번호 제2026-114호",
                "계약금액 1,250,000,000원",
                "납품기한 2026-12-31",
                "전화 02-1234-5678",        // 지역번호 유선 — PHONE 패턴(01X)이 아니다
                "조달청 나라장터 입찰공고",
                "면적 12345 제곱미터"
        );
        OutboundPii.Result r = OutboundPii.maskAll(safe, true);

        assertThat(r.texts()).containsExactlyElementsOf(safe);
        assertThat(r.maskedCount())
                .withFailMessage("공공문서 상용 숫자를 가렸다 — 가린 것: %s", r.texts())
                .isZero();
    }

    @Test
    @DisplayName("설정이 꺼져 있으면 원문 그대로 나간다")
    void disabledPassesThrough() {
        List<String> raw = List.of("홍길동 010-1234-5678");
        OutboundPii.Result r = OutboundPii.maskAll(raw, false);

        assertThat(r.texts()).containsExactlyElementsOf(raw);
        assertThat(r.maskedCount()).isZero();
        assertThat(r.changed()).isFalse();
    }

    @Test
    @DisplayName("빈 입력·null 에 터지지 않는다")
    void emptyIsSafe() {
        assertThat(OutboundPii.maskAll(null, true).texts()).isEmpty();
        assertThat(OutboundPii.maskAll(List.of(), true).maskedCount()).isZero();
        assertThat(OutboundPii.mask(null, true).maskedCount()).isZero();
    }

    // ── 발동: 가려야 하는 것 ───────────────────────────────────

    @Test
    @DisplayName("주민등록번호·휴대전화·카드·이메일을 가린다")
    void masksRealPii() {
        OutboundPii.Result r = OutboundPii.maskAll(List.of(
                "신청인 주민등록번호 000000-0000000",   // ⚠️ 명백히 합성 — 공개 저장소라 실재할 수 있는 번호를 쓰지 않는다
                "연락처 010-0000-0000",
                "카드 0000-0000-0000-0000",
                "메일 hong@example.com"
        ), true);

        assertThat(r.maskedCount()).isEqualTo(4);
        assertThat(String.join("\n", r.texts()))
                .doesNotContain("0000000")
                .doesNotContain("010-0000")
                .doesNotContain("0000-0000-0000-0000")
                .doesNotContain("hong@");
    }

    /**
     * ⚠️ <b>개수를 길이로 세면 틀린다</b> — {@code 010-1234-5678} 은 가려도 길이가 같다.
     * 그래서 {@code equals} 로 센다. 이 테스트가 그 구현을 고정한다.
     */
    @Test
    @DisplayName("가려도 길이가 같은 경우를 놓치지 않는다")
    void countsByContentNotLength() {
        OutboundPii.Result r = OutboundPii.mask("010-1234-5678", true);
        assertThat(r.texts().get(0)).hasSameSizeAs("010-1234-5678");
        assertThat(r.maskedCount()).isOne();
    }

    // ── 순서·개수 불변식 ───────────────────────────────────────

    /**
     * 🔴 <b>임베딩은 입력 개수와 응답 벡터 개수가 맞아야 한다.</b>
     * 마스킹이 항목을 늘리거나 줄이면 그 검사가 깨지고, RAG 가 통째로 실패한다.
     */
    @Test
    @DisplayName("개수와 순서를 절대 바꾸지 않는다")
    void preservesCountAndOrder() {
        List<String> in = List.of("첫째", "010-1111-2222", "셋째", "", "다섯째 hong@a.com");
        OutboundPii.Result r = OutboundPii.maskAll(in, true);

        assertThat(r.texts()).hasSameSizeAs(in);
        assertThat(r.texts().get(0)).isEqualTo("첫째");
        assertThat(r.texts().get(2)).isEqualTo("셋째");
        assertThat(r.texts().get(3)).isEmpty();
        assertThat(r.maskedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("★ 자의 판별력 — 늘 0을 내지도, 늘 전부를 가리지도 않는다")
    void rulerDiscriminates() {
        assertThat(OutboundPii.maskAll(List.of("평범한 문장", "또 평범한 문장"), true).maskedCount()).isZero();
        assertThat(OutboundPii.maskAll(List.of("010-1111-2222", "a@b.co"), true).maskedCount()).isEqualTo(2);
        // 섞이면 섞인 만큼만
        assertThat(OutboundPii.maskAll(List.of("평범", "010-1111-2222"), true).maskedCount()).isOne();
    }
}
