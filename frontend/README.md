# 엮음AI 프론트엔드 (Vue 3 + KRDS)

**KRDS(디지털정부 디자인시스템)** 규격에 맞춘 엮음AI 웹 콘솔.

- **스택**: Vue 3 · Vite 6 · TypeScript(vue-tsc 타입체크)
- **디자인**: **공식 [KRDS](https://www.krds.go.kr) HTML 컴포넌트([krds-uiux](https://github.com/KRDS-uiux/krds-uiux)) 채택** — `public/krds/`에 공식 `component.css`(버튼 `krds-btn`·입력 `krds-input`·셀렉트 `krds-form-select`·탭 `krds-tab-area`·표 `krds-table-wrap`·뱃지 `krds-badge`)·디자인 토큰 `krds_tokens.css`·아이콘 91종을 vendor하고 원본 상대경로 유지. 화면은 공식 컴포넌트 마크업/클래스로 구성. Primary `#256EF4`, 1rem=10px(62.5%) 스케일.
- **서체**: **Pretendard GOV self-host**(`public/fonts/`, 폐쇄망 대응 — CDN 미의존)
- **앱 셸 스타일**: `src/styles/krds.css`(헤더/카드/토큰바 등 레이아웃 전용, 공식 토큰 참조 별칭)
- **접근성(KWCAG 2.2)**: 본문 바로가기, 포커스 가시화, 시맨틱 마크업, `aria-live`/`aria-current`, `lang="ko"`

## 화면

| 탭 | 기능 | API |
|---|---|---|
| 대화 | SSE 스트리밍 채팅 | `POST /api/chat` |
| 문서 작성·요약 | 보고서/공문 초안, 3문장 요약 | `/api/draft` · `/api/summarize` |
| 규정검토 | 작성물 위반·수정문안 + 근거 인용(다국어) | `POST /api/review` |
| 문서·검색 | 파일 업로드(PDF/텍스트)·텍스트 색인·RAG 검색 | `/api/docs` · `/api/docs/upload` · `/api/rag/query` |
| 감사로그 | 요청 이력(admin) | `GET /api/audit` |

상단 **API 토큰** 입력값은 브라우저(localStorage)에만 저장되어 `Authorization: Bearer` 로 전송됩니다.

## 실행

```bash
npm install
npm run dev      # http://localhost:5173  (/api·/health → localhost:8080 프록시)
npm run build    # vue-tsc 타입체크 + vite 번들 → dist/
npm run preview  # dist 미리보기
```

백엔드(Spring Boot)를 `:8080`에 먼저 띄우세요(`mvn spring-boot:run`). 운영 배포 시 `dist/`를 정적 호스팅하거나 백엔드 `static/`으로 복사합니다.

## 출처·라이선스

- 공식 컴포넌트 CSS·디자인 토큰(`public/krds/`)·아이콘·서체(`public/fonts/PretendardGOV-*`)는 **KRDS 공식 배포본 [KRDS-uiux/krds-uiux](https://github.com/KRDS-uiux/krds-uiux)** 에서 가져와 self-host 합니다. KRDS 자산은 배포처의 라이선스·이용조건을 따릅니다.
- Pretendard GOV 서체는 Pretendard(OFL) 기반입니다.
