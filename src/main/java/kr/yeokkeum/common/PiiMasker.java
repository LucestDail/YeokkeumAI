package kr.yeokkeum.common;

import java.util.regex.Pattern;

/**
 * 개인정보 마스킹 [SEC-1] — 로그·감사에 남는 텍스트에서 주민등록번호·전화·이메일·카드번호를 비식별.
 * 개인정보보호법(안전조치) 대응. ⚠️ 저장 데이터 암호화(at-rest)는 별도(jasypt/DB) 과제.
 */
public final class PiiMasker {

    private PiiMasker() {}

    // 주민등록번호 6자리-7자리 → 뒤 7자리 마스킹
    private static final Pattern RRN = Pattern.compile("(\\d{6})-(\\d{7})");
    // 휴대전화 01X-XXXX-XXXX (하이픈 유무)
    private static final Pattern PHONE = Pattern.compile("(01[016789])[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})");
    // 카드번호 16자리(4-4-4-4, 하이픈/공백 유무)
    private static final Pattern CARD = Pattern.compile("(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})");
    // 이메일
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    public static String mask(String s) {
        if (s == null || s.isEmpty()) return s;
        String r = s;
        r = CARD.matcher(r).replaceAll("$1-****-****-$4");   // 카드: 앞4·뒤4만
        r = RRN.matcher(r).replaceAll("$1-*******");
        r = PHONE.matcher(r).replaceAll("$1-****-$3");
        r = EMAIL.matcher(r).replaceAll("$1***$2");
        return r;
    }
}
