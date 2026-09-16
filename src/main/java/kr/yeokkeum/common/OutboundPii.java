package kr.yeokkeum.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 외부로 <b>나가는</b> 텍스트의 개인정보 비식별 [R3].
 *
 * <h2>왜 만들었나 (2026-09-16)</h2>
 *
 * {@link PiiMasker} 는 예전부터 있었지만 <b>{@code AuditService} 한 곳만 쓰고 있었다.</b>
 * 즉 <b>로그에는 안 남기면서 외부 모델에는 원문을 그대로 보내고</b> 있었다 —
 * 공공 RFP 문서(계약서·신청서)에는 주민등록번호·연락처가 흔하다.
 *
 * <p>🔴 이 워크스페이스가 09-08 에 기록한 것이 <i>"마스킹은 값을 <b>모으는 자리</b>에서"</i> 였는데,
 * 여기서는 <b>나가는 자리</b>가 비어 있었다. 같은 병의 다른 얼굴이다.
 *
 * <h2>어디에 거는가</h2>
 *
 * 외부로 텍스트를 보내는 길은 <b>둘</b>이다 — 채팅({@code /chat/completions})과
 * <b>임베딩</b>({@code /embeddings}). 임베딩도 RAG 청크 원문을 그대로 보내므로 같은 위험이다.
 * ⚠️ <b>한 곳만 고치면 반쪽이다</b>(이 워크스페이스의 "한 곳 고치면 저장소 전수 스캔" 규칙).
 *
 * <h2>⚠️ 오탐이 기능을 망가뜨린다</h2>
 *
 * 계약서를 올리고 <i>"상대방 연락처가 뭐야"</i> 라고 물으면, 가려진 탓에 모델이 답을 못 한다.
 * 이건 보안이 아니라 <b>기능 손상</b>이다. 그래서
 * <ul>
 *   <li>설정으로 끌 수 있고({@code yeokkeum.llm.mask-pii})</li>
 *   <li><b>몇 건 가렸는지</b>를 함께 낸다 — 0 이면 "안 돌았다" 를 결과가 스스로 알려 준다</li>
 *   <li>가리는 대상은 {@link PiiMasker} 의 <b>모양이 분명한 것</b>뿐이다(주민번호·전화·카드·이메일).
 *       이름·주소처럼 <b>경계가 모호한 것은 넣지 않는다</b> — 모호한 것을 자에 넣으면 자가 거짓말한다</li>
 * </ul>
 */
public final class OutboundPii {

    private OutboundPii() {}

    /** 마스킹 결과 + <b>실제로 몇 건을 가렸는지</b>. 증거 없이 "적용했다" 고 말하지 않기 위해 함께 낸다. */
    public record Result(List<String> texts, int maskedCount) {
        public boolean changed() { return maskedCount > 0; }
    }

    /**
     * 나가기 직전의 텍스트 묶음을 가린다.
     *
     * @param enabled 꺼져 있으면 <b>원본을 그대로</b> 돌려준다(개수는 0)
     */
    public static Result maskAll(List<String> texts, boolean enabled) {
        if (texts == null) return new Result(List.of(), 0);
        if (!enabled) return new Result(texts, 0);

        List<String> out = new ArrayList<>(texts.size());
        int changed = 0;
        for (String t : texts) {
            String m = PiiMasker.mask(t);
            // ⚠️ `equals` 로 센다 — 길이는 마스킹해도 같을 수 있다(`010-1234-5678` → `010-****-5678`)
            if (!Objects.equals(t, m)) changed++;
            out.add(m);
        }
        return new Result(out, changed);
    }

    /** 문자열 하나. */
    public static Result mask(String text, boolean enabled) {
        return maskAll(text == null ? List.of() : List.of(text), enabled);
    }
}
