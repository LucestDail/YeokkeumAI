# CLAUDE.md — YeokkeumAI(엮음AI)

공공기관 업무보조 AI 플랫폼. **프론트엔드는 KRDS(범정부 UI/UX 디자인시스템)를 강제 준수한다.**

## 스택 / 배포 (요약)
- 백엔드: Java 17 · Spring Boot 3.5.6(eGovFrame 5.0 정렬) · Spring AI · PostgreSQL+Flyway(prod)/H2(dev). `./mvnw test`.
- 프론트: Vue 3 + Vite + TypeScript(vue-tsc). `frontend/`. context-path `/yeokkeum`.
- 온프렘 systemd 배포(포트/호스트·토큰·게이트웨이 키 등 운영정보는 로컬 메모리 참조, 배포 스크립트도 로컬 전용).
- ⚠️ **repo 공개** — 인프라 주소/IP/토큰/비번/도메인 커밋 금지.

## ★ KRDS 디자인 강제 준수 규칙 (프론트 작업 시 매번)

**근거 자료(반드시 이것만 따른다, 추측 금지)**
- 공식 킷: `frontend/ref/krds-uiux-main/` — `resources/cdn/krds.min.css`+`krds.min.js`(통합 번들), `resources/css/{common,component,token}`, `html/code/*.html`(실제 컴포넌트 마크업). **이 킷의 클래스·마크업·토큰을 그대로 사용**한다. 임의 재현 금지.
- 공식 가이드: `~/Downloads/디지털 정부서비스 UIUX 가이드라인(2025.08).pdf`(1181p). 아래 규칙은 이 문서 근거.

**스타일 시스템(표준형 Standard)**
- 색·타이포·radius·간격은 **KRDS 디자인 토큰만** 사용. 임의 hex/px 하드코딩 금지.
- 폰트 **Pretendard GOV**, 본문 기본 **17px**, **모든 텍스트 line-height 150%**, weight 400/700만. rem 기준 10px.
- 타입스케일: h1 40px(모바일28)/h2 32/h3 24/h4 19/h5 15, body medium 17. heading↔body 1.25~1.5배.
- radius: element 2 / chip·tag·checkbox 4 / **button·input·select·textarea 6** / card·dialog 10 / banner 12. 원형만 %.
- 간격 **8pt 그리드**(4·8·16·24…). 카드리스트 gap 24, 인풋 내부 gap 8/패딩 16.

**색상 사용**
- 역할: Primary=주요 버튼·링크 / Secondary=사이드메뉴·보조 / Gray=배경·텍스트·구분선 / Accent=알림 배지 **≤5%** / System=상태.
- **60-30-10** 비율(중립60/보조30/주요10).
- 상태는 반드시 **System 색 + 아이콘/텍스트 병행**(색상 단독 금지). Danger/Warning/Success/Info.
- 명도대비: 본문 텍스트 ≥4.5:1, 큰 텍스트/아이콘·UI경계 ≥3:1. disabled 투명도색 금지.

**레이아웃**
- 콘텐츠 최대폭 **1200px 고정**, 스크린마진 small 16 / medium·large·xlarge 24px.
- 브레이크포인트: small 360 / medium 768 / large 1024 / xlarge 1280, 컬럼 4/8/12/12, 가터 16/16/24/24.
- 서브페이지 순서: Header → Left menu → Main → Right(In-page nav/Help panel) → Footer.

**전역 페이지 골격(모든 화면 필수, DOM 순서)**
`건너뛰기 링크(문서 첫 요소) → 공식 배너(Masthead) → 헤더(서비스아이덴티티→유틸리티→검색→메인메뉴) → 브레드크럼 → main(h1) → footer(→운영기관 식별자)`
- ⚠️ **공식 배너/운영기관 식별자는 실제 정부 공식 서비스에만 사용.** 본 프로젝트는 비공식 데모이므로 공식 문구 사칭 금지 — 배너는 정직 문구("비공식 데모")로 준용하거나 미사용.
- 헤더: 로고 좌상단 / 유틸리티 우상단(≥5개는 드롭다운) / **언어 전환에 국기 금지, 언어명 사용** / 아이콘 버튼에 텍스트 레이블.
- 푸터: 로고→연락처→유틸리티→정책→저작권 순, **'개인정보 처리방침' 명칭 노출**, `<footer>` 랜드마크.

**컴포넌트**
- 버튼: 한 화면 강조(면채움) **1개만**, 레이블 동사형, 터치 ≥44×44px. 화면 이동=`<a>`, 동작=`<button>`.
- 폼: 단일 열 수직, 모든 필드 `<label>`, **플레이스홀더로 레이블 대체 금지**, 필수/선택 일관 구분, 오류 시 해당 필드로 초점 이동+입력값 유지.
- 표: 숫자 우측/텍스트 좌측 정렬, 빈 셀 대시(-), 각형(radius 0).
- 배지: 비대화형·요소당 1개·주조색 배경 금지. 긴급공지: 화면당 1개·숨김버튼 없음.
- 다단계 폼: 스텝 인디케이터 3~7단계, 한 단계 내 탭/아코디언 분할 금지, 임시저장, 완료 단계 필수.
- 페이지네이션 화면당 1개·≤10개. 사이드메뉴 ≤2수준, In-page nav ≤3단계.

**접근성 KWCAG 2.2 / WCAG 2.1 AA (무조건)**
- landmark(header/nav/main/footer), 건너뛰기 링크 최상단, 로고 대체텍스트에 '로고' 단어 제외.
- 전 인터랙션 키보드 조작, 포커스 가시(인접 3:1↑), 모달 포커스 트랩, Tab 순서 논리적.
- 색상만으로 정보 전달 금지.

**커밋 전 자체 검증 체크리스트**
1) 토큰 외 하드코딩 색/치수 없음 2) 본문 17px·150% 3) 명도대비 통과 4) 전역 골격 순서·개인정보처리방침 존재 5) 키보드·포커스·대체텍스트 6) 터치 44px 7) 상태=색+아이콘/텍스트 8) `frontend/ref` 킷 클래스/마크업 사용(임의 재현 아님).

## 참고
- 백엔드 상세·백로그: `docs/BACKLOG-2026-08.md`, 루트 워크스페이스 메모리 `project_yeokkeumai`.
- 응답 언어 한국어. 커밋/배포는 명시 요청 시. 커밋 메시지 말미 `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
