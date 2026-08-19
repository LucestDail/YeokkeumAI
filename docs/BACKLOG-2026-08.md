# YeokkeumAI 잔여 작업 목록 (2026-08-19)

소스 전체(백엔드 44 Java · 프론트 Vue · docs · pom/yml/Docker)를 4개 차원(백엔드 기능 / 운영 하드닝 / 프론트 / 문서·로드맵 정합성)으로 병렬 분석해 도출한 코드 근거 기반 백로그. 현 HEAD `c285358`, 테스트 19 GREEN.

**현 실체**: Phase 0 + Phase 1 대부분 구현됨 — 벤더무관 LLM 게이트웨이(SpringAI/OpenAI-compat/stub), 하이브리드 RAG(BM25+dense RRF), 규정검토(`/api/review`), PDF+HWP(rhwp) 파싱, SSE 채팅, Spring Security 토큰 RBAC(secure-by-default), 감사로그, Vue3+KRDS 프론트, 비root Docker. **A4~A7(민원챗봇·MCP·HITL·데이터분석·관리콘솔) 백엔드는 미착수.**

**가장 시급한 블로커 3** (온프렘 배포하면 즉시 드러남): ① H2 인메모리 기본 → 재시작 시 감사로그·문서 소실 ② 리눅스 Docker에서 HWP 파서 불능(arm64 바이너리) ③ Vue KRDS 프론트가 Docker 이미지에 미포함.

---

## P0 — 배포·데이터·법규 즉시 블로커

| ID | 항목 | 근거 | 규모 |
|---|---|---|---|
| **DATA-1** | **H2 인메모리 기본 → 재시작 시 감사로그·문서·임베딩 전량 소실.** PostgreSQL prod 프로파일·Flyway 마이그레이션 부재(`ddl-auto:update`가 유일 스키마 관리) | `application.yml:16`, `docker-compose.yml:19`, pom flyway 없음, `application-prod.yml` 없음 | M |
| **DEPLOY-1** | **리눅스 Docker에서 HWP 파서 불능.** `bin/rhwp`=Mach-O arm64(macOS), Dockerfile에 리눅스 rhwp 빌드/조달 스텝 없음 → HWP/HWPX 업로드 항상 503(간판 기능이 실배포서 깨짐) | `bin/rhwp`(arm64), `Dockerfile`, `HwpExtractor.java:61` | M |
| **DEPLOY-2** | **Vue KRDS 프론트가 컨테이너에 미포함.** Dockerfile에 frontend 빌드 스텝 없음 → `docker compose up` 시 `static/index.html` 데모 콘솔만 서빙, 실제 KRDS UI 안 뜸 | `Dockerfile`, `frontend/README.md`(수동 복사 권고만) | M |
| **SEC-1(PII)** | **개인정보 비식별/마스킹/암호화 전무.** 프롬프트·응답 PII 마스킹(A1-4) 없음, DB·토큰 평문(jasypt 없음), 파기 절차 없음 → 개인정보보호법(C3) 위반 | `grep 비식별/mask/encrypt`=0, `AuditService`, `application.yml` | L |
| **SEC-2(업로드)** | **파일 업로드 크기·MIME·매직바이트·개수 검증 부재**, `spring.servlet.multipart.max-*` 미설정 → 대용량/악성 업로드로 OOM·DoS(전량 `file.getBytes()` 메모리 로드) | `ApiController.java:107`, `application.yml`(상한 없음) | S |
| **RAG-1** | **RAG가 매 쿼리 `chunkRepo.findAll()` 전수 적재** 후 BM25·코사인 인메모리 계산 → 문서 수 증가 시 OOM·레이턴시 선형폭증. 상한·페이징·벡터DB 없음 | `RagService.java:104` | M |
| **FEAT-1(삭제)** | **문서/청크 삭제 API·UI 부재** — 등록만 가능, 오등록/PII 문서 파기 불가(SEC-1과 연동) | `ApiController`(DeleteMapping 0), `DocsView.vue` | S |

---

## P1 — 핵심 기능·운영 하드닝

### 게이트웨이·RAG 품질
- **GW-1** SpringAiGateway가 `temperature`/`maxTokens` 무시 + `usage` 빈값 → 온도·토큰상한 제어·비용집계 불가(A1-3). 실 LLM 경로 자동검증 없음(테스트 전부 stub). `gateway/SpringAiGateway.java:48-61` · M
- **GW-2** 모델 라우팅·폴백·레이트리밋·쿼터 없음(단일 모델 고정). osh-ai-gateway/HARU 기법 미흡수. A1-3 · M
- **EMB-1** 실 임베딩(BGE-M3) 엔드포인트 응답파싱·차원불일치·RRF 순위 자동검증 없음(stub만 테스트). 모델 교체 시 차원 불일치를 `Vectors.cosine`이 조용히 0 반환→dense 무경고 사망. `OpenAiCompatEmbeddingGateway`, `Vectors.java:26` · M
- **RAG-2** 재랭커(cross-encoder) 추상화·구현 없음(RRF 융합만). A1-5/A2-2 · M
- **RAG-3** KB 멀티테넌시·버전·권한 격리 없음(`Document`/`Chunk`에 orgId/tenant/version 없음) → 기관별 격리(A2-4) 불가. L
- **RAG-4** 인용 정밀도 부족 — `filename+chunk idx`만, 실제 페이지/문단 번호 없음(A2-2 "문단·페이지 인용"). M

### 문서 처리
- **DOC-1** DOCX/XLSX/이미지 OCR/표 추출 없음(POI 등 의존성 없음). PDF 스캔이미지는 텍스트 0 → 침묵 처리. 기능목록 A2-1 상당수 미지원. `DocText.java` · L
- **DOC-2** HWP **생성(쓰기/서식보존)** 없음 — 읽기만(A3-4 "입출력" 미충족). M

### 미착수 대분류(A4~A7)
- **FEAT-2(민원)** 민원 챗봇·자동분류·라우팅·상담이력·만족도(A4) 백엔드 전무. `/api/chat`은 무상태 단발(세션·이력 없음). L
- **FEAT-3(MCP)** MCP 도구 서버 연동·레지스트리·allowlist·오케스트레이션(A5-1/2/3) 전무(pom·코드 0건). L
- **FEAT-4(HITL)** 사람 승인 게이트(A5-4) 전무 — 브랜드 핵심("통제·책임성")인데 통제 메커니즘 없음. 감사로그는 사후기록만. PLAN 원칙2 미이행. `grep hitl/approval`=주석 1건 · L
- **FEAT-5(콘솔)** 관리자 콘솔·조직/워크스페이스·역할 세분화·프롬프트/도구 구성 UI·사용량 대시보드(A7) 없음. RBAC는 admin/user 토큰 2개뿐. L
- **FEAT-6(작성 심화)** 요약/초안이 단발 프롬프트 1회(요약 maxTokens 512 고정 장문잘림). 템플릿·문체 프리셋·행정용어 교정·다문서 통합요약·결재정합성(A3) 없음. M

### 보안·인증
- **SEC-3** 정적 단일 토큰(회전·만료·개인귀속 없음) → 감사 actor가 개인 아닌 토큰 프리픽스. SSO(GPKI/OIDC) 없음. `TokenAuthFilter.java:50-74` · L
- **SEC-4** TLS/HTTPS 앱단 강제·문서 없음(평문 Bearer). CORS 전면 `disable()`(프론트 분리배포 시 재설계). `SecurityConfig.java:34` · M
- **SEC-5** `INSECURE_OPEN_MODE=true` 오배포 방어가 `log.warn`뿐(fail-fast/prod 금지 가드 없음). `StartupChecks.java:28` · S
- **SEC-6** jasypt 등 설정/시크릿 암호화 없음(토큰·DB비번 평문 env). S

### 관측성·안정성
- **OBS-1** Actuator 전무 → readiness/liveness·메트릭 없음. `/health`는 DB/LLM/임베딩 다운 미감지(무조건 ok) → 좀비 컨테이너 트래픽 수신. `PublicController.java:11`, pom · S
- **OBS-2** 임베딩/LLM 실패·비용·타임아웃 메트릭 없음 → 폴백(dense→BM25)이 조용히 발생, 품질 저하 무인지. `RagService.java:59,124` · S
- **STAB-1** SSE `new SseEmitter(0L)` 무한 타임아웃 + `newCachedThreadPool()` 무제한 스레드 → 느린/끊긴 클라이언트 누적 시 스레드 고갈. `ChatController.java:26,41` · M
- **API-1** 페이지네이션 부재(`/api/docs`, `/api/audit`는 limit만, offset/cursor 없음) → 목록 커지면 전량 반환. S

### 프론트엔드
- **FE-1** 알림을 커스텀 `.app-alert`로 때움 — 공식 `krds-critical-alerts`가 component.css에 실재(19회)하는데 미사용. 주석/README "KRDS 미제공" 서술이 사실과 불일치. 전 뷰 · S
- **FE-2** 주 내비게이션이 자작 `.appnav`(WriteView는 올바른 `krds-tab-area` 사용 → 앱 내 탭 구현 2갈래). WriteView 방식으로 통일 시 키보드 role도 동시 해결. `App.vue:38` · S
- **FE-3(a11y)** SSE 라이브리전이 토큰마다 append(`aria-live=polite`) → 스크린리더 폭주. 탭 키보드 role/화살표 이동 없음. 에러·검증 aria/포커스이동/`aria-invalid` 없음. `ChatView.vue:48`, `App.vue:38` · M
- **FE-4** 감사로그 필터/검색/페이지네이션/CSV 내보내기 없음(백엔드도 기간·actor·action 필터 없음). "감사성"이 핵심가치인데 열람만. `AuditController.java:20`, `AuditView.vue` · M
- **FE-5** 상태코드별 에러 UX 없음 — raw `HTTP 403 — forbidden` 등 개발자 문자열 그대로 노출(비개발 공무원 대상). 401/403/415/503 한국어 안내 매핑 필요. `api.ts:17-20` · S
- **FE-6** 토큰 역할(admin/user) 표시·탭 게이팅 없음(user가 감사탭 클릭→raw 403). `App.vue:50` · M
- **FE-7** 프로덕션 API 베이스/프록시 전략 부재(vite 프록시는 dev 전용, `VITE_API_BASE` 없음) — 통합 서빙 vs 분리배포 결정 필요(DEPLOY-2·SEC-4와 연동). S

### 테스트
- **TEST-1** PostgreSQL·prod 프로파일 통합 테스트 전무(Testcontainers 없음) → prod 컬럼타입/제약 버그 배포 후 발견(과거 printscan CLOB→LONGVARCHAR 전례). M
- **TEST-2** 실 임베딩 엔드포인트·dense RRF 순위 미검증(stub만). M
- **TEST-3** rhwp 정상경로가 CI(리눅스)서 항상 스킵(arm64) → HWP 추출이 어느 타깃서도 미검증. M
- **TEST-4** 업로드 거부·CORS·INSECURE_OPEN_MODE·SSE 조기종료/누수 미검증. S

### 공공 통과조건(코드 레벨 착수 필요)
- **PUB-1(C2)** 웹접근성 KWCAG 2.2 전체 점검·WA 인증(부분: skip-link/aria 일부). M
- **PUB-2(C9)** AI 품질 회귀셋·환각율/근거일치 측정 하니스 없음. M
- **PUB-3(C10)** 국산 파운데이션 모델 실연결·검증 없음(기본 openrouter/deepseek). 게이트웨이로 가능성만. S
- **EGOV-1** springdoc/OpenAPI(Swagger) 없음(RFP 단골·SIR). S
- **EGOV-2** POI 5.4(DOCX/XLSX 리포팅) 없음(DOC-1과 연동). M
- **EGOV-3** 관측 스택(actuator/micrometer/Prometheus) 없음(OBS-1과 연동). M
- **EGOV-4** jasypt 암호화(SEC-6), PMD/SpotBugs 정적분석 CI(C6) 없음. S

---

## P2 — 이후

- **RAG-5** 소스 갱신 감지·재색인·만료경고(A2-5) 없음. M
- **FEAT-7** 데이터 분석·NL→SQL·이상탐지(A6) 전무. L
- **FEAT-8** 다단계 오케스트레이션·예약/트리거(A5-3, Quartz/Batch) 없음. L
- **SEC-7** 아웃바운드(임베딩/LLM base-url) 폐쇄망 화이트리스트·SSRF 가드 없음(운영자 설정이라 위험 낮음). S
- **OBS-3** 구조적 로깅·상관ID(MDC) 없음, 내부 예외메시지 클라이언트 노출. S
- **AUD-1** 감사로그 내보내기·해시체인/서명 불변성 없음(C9). S
- **FE-8** KRDS 헤더/푸터 미채택(자작), 스피너·모달·브레드크럼·아코디언 미사용. M
- **FE-9** UI i18n(vue-i18n) 없음 — 레이블 한국어 하드코딩(review 응답 언어만 다국어). L
- **DEPLOY-3** Dockerfile healthcheck·JVM 메모리 플래그·`dependency:go-offline` 폐쇄망 빌드 전략 없음. M
- **EGOV-5** MyBatis/EHCache 등 ◻선택 항목(엄격 eGov 감리 대응 시). M

---

## 정직성 리스크 — 문서-코드 불일치 (즉시 정정 권장, 규모 S)

| ID | 문서 주장 | 코드 실측 |
|---|---|---|
| **HON-1** | 감사로그에 "모델·**토큰**" 적재(PLAN Contract), 토큰·비용 집계(A1-3) | audit는 model/chars/filename/nChunks만, **토큰 미기록**. `ChatResult.usage`는 audit에 미연결 |
| **HON-2** | A2-1 파싱: "DOCX·XLSX·이미지 OCR·표" | 실제 PDF/txt/HWP만. **DOCX/XLSX/OCR/표 없음** |
| **HON-3** | "국산 K-AI 물리는" 반복 강조 | 가능성만, 기본·테스트 전부 openrouter/deepseek. **국산모델 실연결 증거 0** |
| **HON-4** | README "HWP 파싱(완료)" + 온프렘 리눅스 배포 | arm64 바이너리라 **리눅스 Docker서 HWP 항상 503**(DEPLOY-1) |
| **HON-5** | `docs/스택-eGov5.0.md`: Security "🔜 이관 예정" | **이미 완료**(SecurityConfig). 문서 내부 모순 |
| **HON-6** | README 구조도: `AuthInterceptor`, `WebConfig` | 실제 `TokenAuthFilter`, WebConfig 없음(이관 전 상태) |
| **HON-7** | `krds.css:107` "KRDS 알림 미제공" | component.css에 `krds-critical-alerts` 19회 실재(FE-1) |
| **HON-8** | 코드 전반 레거시 `ieum/이음` 네이밍 39건(IeumApplication·IeumProperties·`ieum.*`·`jdbc:h2:mem:ieum`) | 브랜드 `YeokkeumAI/엮음`과 불일치 |

---

## 착수 순서 제안 (근거 기반)

1. **온프렘 실배포 최소셋(P0)**: DATA-1(PostgreSQL+Flyway) → DEPLOY-1(리눅스 rhwp) → DEPLOY-2(프론트 빌드 통합) → SEC-2(업로드 검증) → OBS-1(actuator). 여기까지가 "배포하면 실제로 도는" 최소선.
2. **법규·책임성(P0/P1)**: SEC-1(PII 마스킹/암호화) → FEAT-1(문서 삭제) → FE-4(감사 필터/내보내기) → HON-1(토큰 집계).
3. **브랜드 핵심(P1)**: FEAT-4(HITL) + FEAT-3(MCP) — PLAN 원칙2 이행. HARU/my-computer approval·MCP 패턴 흡수.
4. **정직성 정정(즉시)**: HON-1~8 문서/주석/네이밍 정정 — 저비용, 신뢰 리스크 제거.
