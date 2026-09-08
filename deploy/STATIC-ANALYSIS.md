# 정적 분석 (R5)

> 공공 납품에서 자주 요구되는 항목이다. **pom 에 상시 도입하지 않았다** — 아래 실측에서
> 실제 결함이 0건이라 지금 붙여도 얻는 게 없고, 빌드 시간과 유지보수 대상만 늘기 때문이다.
> 필요해질 때(감리·납품 심사) 이 문서대로 돌리면 된다. **pom 을 건드리지 않는 일회성 실행**이다.

## SpotBugs — 일회성 실행

```bash
mvn -DskipTests package
mvn com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs \
  -Dspotbugs.effort=Max -Dspotbugs.threshold=Low
# 결과: target/spotbugsXml.xml
# 사람이 볼 UI: mvn com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:gui
```

요약 보기:

```bash
python3 - <<'PY'
import xml.etree.ElementTree as ET
from collections import Counter
r = ET.parse('target/spotbugsXml.xml').getroot()
bugs = r.findall('.//BugInstance')
print(len(bugs), '건'); print(Counter(b.get('category') for b in bugs))
for b in bugs:
    if b.get('priority') != '1': continue
    sl = b.find('.//SourceLine'); lm = b.find('LongMessage')
    print(f"[{b.get('type')}] {sl.get('sourcepath')}:{sl.get('start')} — {lm.text}")
PY
```

## 2026-09-08 실측 — **실제 버그 0건**

| 항목 | 값 |
|---|---|
| 총 | 57건 |
| 우선순위 1(높음) | **2건** |
| 카테고리 | MALICIOUS_CODE 34 · STYLE 12 · I18N 9 · BAD_PRACTICE 2 |

전부 확인했고 **동작을 바꾸는 결함은 없었다**:

- **`Bm25.java:35` UC_USELESS_CONDITION** — 20행에서 이미 `n == 0` 을 걸러 리턴하는데
  35행에서 다시 검사한다. **중복 방어일 뿐 버그가 아니다.** BM25 공식 자체도 정확하고,
  `B * dl[i] / avgdl` 은 `B` 가 double 이라 정수 나눗셈 함정도 없다.
- **`Gov24Controller.done()` DE_MIGHT_IGNORE** — SSE 종료 시 예외를 삼킨다.
  이미 끝난 스트림을 닫는 경로라 흔한 패턴이고, 여기서 예외를 올려도 할 수 있는 일이 없다.
- **I18N 9건 (`DM_CONVERT_CASE`)** — `Locale` 을 주지 않은 `toLowerCase()`.
  터키어 로케일에서 `"I".toLowerCase()` 가 `"ı"` 가 되는 문제인데, 대상은 파일 확장자·
  프로파일 이름 판정이고 서버 로케일은 `ko_KR` 이다. **온프렘 납품처의 로케일이 `tr` 이면**
  확장자 판정이 어긋날 수 있으니, 그런 환경이 실제로 생기면 `Locale.ROOT` 를 붙인다.
- **MALICIOUS_CODE 34건 (`EI_EXPOSE_REP`)** — DTO 가 내부 리스트/배열 참조를 그대로
  주고받는다. Lombok `@Data` 를 쓰는 프로젝트에서 늘 나오는 항목이고, 이 서비스는
  DTO 를 외부에 신뢰 경계로 노출하지 않는다.

⇒ **"부채가 적을 때 붙인다"는 전제가 실측으로 확인됐다.** 도입 시점을 미뤄도
쌓이는 것이 없다는 뜻이므로, 감리에서 요구될 때 붙이는 편이 낫다.

## 함께 요구되곤 하는 것 — 의존성 취약점

```bash
mvn org.owasp:dependency-check-maven:check
```

⚠️ 첫 실행은 NVD 전체를 내려받아 **수십 분** 걸린다(이후 캐시). CI 에 넣을 때는
`nvdApiKey` 를 받아 두는 편이 빠르다. 2026-09-08 시점에는 돌리지 않았다 —
필요 시점(납품 심사)에 시간을 확보해서 할 것.
