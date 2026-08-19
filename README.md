# 엮음AI (YeokkeumAI)

**공공기관 업무보조 AI 플랫폼** — 온프렘/내부망에서 도는, 국산 K-AI도 물리는 벤더무관 게이트웨이 위에 **규정검토 RAG · 문서 작성/요약 · 민원 상담 · 업무 에이전트**를 얹고, 모든 처리에 **사람 승인(HITL)과 감사로그**를 붙인 범용 AI 플랫폼.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-green.svg)](https://spring.io/projects/spring-boot)
[![eGovFrame](https://img.shields.io/badge/eGovFrame_5.0-aligned-informational.svg)](https://www.egovframe.go.kr/)

> **엮음(Yeokkeum) = "엮다/연계".** 자율성이 아니라 **통제·책임성·연계**를 파는 제품 — 공공이 실제로 원하는 것에 정조준.

---

## 왜?

2026 공공 AI 수요(조달청·NIA)는 명확하다: **RAG 규정검토 · 문서 작성/요약 · 민원 챗봇 · 업무 에이전트(MCP) · AI 게이트웨이(국산모델 포함) · 데이터 분석** — 전부 **온프렘/N2SF · CSAP · 사람 승인 · 감사가능성** 위에서. "자율 코드생성/자기배포" 수요는 없다. 엮음AI는 이 수요에 맞춰 **연계·통제·책임성**을 설계 원칙으로 삼는다.

## 핵심 기능 (4엔진)

- **🔌 LLM 게이트웨이 (벤더무관)** — OpenAI 호환 엔드포인트면 무엇이든(OpenRouter · 사내 게이트웨이 · **국산 K-AI** · vLLM). `base-url`/`model`/`key` 로 핫스왑. 키 없으면 **오프라인 stub**로 동작(폐쇄망 데모/결정적 테스트).
- **🔎 하이브리드 RAG (규정/지식 검색·검토)** — 문단 청킹 + **BM25(어휘) + BGE-M3 dense(의미)** 를 **RRF 융합** + **근거 인용**. 임베딩은 OpenAI 호환 `/embeddings`(BGE-M3/TEI·vLLM·국산 게이트웨이)로 벤더무관, 벡터는 인메모리 코사인(소규모 온프렘엔 벡터DB 불필요). **엔드포인트 없으면 오프라인 stub 임베더로 자동 폴백**(폐쇄망/결정적 테스트). 근거 없으면 정직 거절.
- **📄 문서 작성·요약 + HWP** — 보고서/공문/RFP 초안·다문서 요약. 업로드 색인은 **PDF(PDFBox)·텍스트·HWP/HWPX**(오픈소스 [rhwp](https://github.com/edwardkim/rhwp) `export-text` 바이너리 위임) 지원.
- **🧾 감사로그 + RBAC** — 모든 요청을 **누가·언제·무엇을·어떤 모델로** 기록. admin/user 역할, **secure-by-default**(토큰 없으면 CLOSED).

## 기술 스택 — eGovFrame 5.0 정렬

| 구분 | 채택 |
|---|---|
| 언어/프레임워크 | **Java 17 · Spring Boot 3.5.6** (전자정부 표준프레임워크 5.0 baseline) |
| 아키텍처 | 표준 3-tier (Controller → Service → Repository) |
| AI layer | **Spring AI 1.0.1** (OpenAI 호환 ChatModel) + 경량 HttpClient + 오프라인 stub |
| 검색 | **하이브리드**: 순수 자바 BM25 + BGE-M3 dense(OpenAI 호환 `/embeddings`, 인메모리 코사인) + RRF |
| 영속 | Spring Data JPA · H2(dev) / PostgreSQL(prod) |
| 인증 | **Spring Security 6.5.x** 토큰 RBAC(무상태) · secure-by-default |
| 프론트 | **Vue 3 + Vite + KRDS**(디지털정부 디자인시스템, `frontend/`) · KWCAG 2.2 지향 |
| 배포 | 비root 컨테이너 · docker-compose |

정렬 상세 → [docs/스택-eGov5.0.md](docs/스택-eGov5.0.md)

## 빠른 시작

```bash
# 로컬 실행 (키 없이 stub로 즉시 동작)
export ADMIN_TOKEN=change-me USER_TOKEN=change-me
mvn spring-boot:run
# → http://localhost:8080  (웹 콘솔)

# 실제 LLM 연결 (OpenAI 호환 엔드포인트)
export LLM_API_KEY=sk-...  LLM_BASE_URL=https://openrouter.ai/api/v1  LLM_MODEL=deepseek/deepseek-chat
# 사내/국산 게이트웨이 예: LLM_BASE_URL=http://<gateway>/v1

# 온프렘 컨테이너
docker compose up -d --build   # → http://localhost:8088
```

> **secure-by-default**: `ADMIN_TOKEN`/`USER_TOKEN` 미설정 시 전 API가 닫힙니다. 로컬 개방은 `INSECURE_OPEN_MODE=true`.

### HWP/HWPX 파서(rhwp)

HWP/HWPX 업로드는 오픈소스 [rhwp](https://github.com/edwardkim/rhwp)(Rust·MIT)의 `export-text` 바이너리에 위임합니다. 리포에 macOS(arm64) 바이너리(`bin/rhwp`)를 동봉했고, **리눅스 배포는 대상 플랫폼에서 빌드**합니다(Docker rust 이미지 사용 — Rust 미설치 환경 OK):

```bash
deploy/build-rhwp-linux.sh              # 클론+빌드(Docker) → target/release/rhwp
sudo install -m755 .../rhwp /opt/yeokkeum/bin/rhwp
# /etc/yeokkeum.env 에  RHWP_PATH=/opt/yeokkeum/bin/rhwp
```

경로 우선순위: `RHWP_PATH` > `ieum.doc.rhwp-path`(bin/rhwp) > PATH. 바이너리가 현재 플랫폼에서 실행 불가하면 HWP 업로드만 503으로 명확히 실패하고(다른 기능 정상), 텍스트/PDF는 영향 없습니다. (.25 배포에는 리눅스 x86-64 바이너리를 `/opt/yeokkeum/bin/rhwp`로 설치·검증 완료)

## API

| 메서드 | 경로 | 설명 | 권한 |
|---|---|---|---|
| GET | `/health` | 헬스체크 | 공개 |
| GET | `/` | 웹 콘솔(UI) | 공개 |
| POST | `/api/chat` | 채팅 (SSE 스트리밍) | user |
| POST | `/api/summarize` | 요약 | user |
| POST | `/api/draft` | 문서 초안 | user |
| POST | `/api/docs` | 문서 색인(텍스트) | user |
| POST | `/api/docs/upload` | 파일 업로드 색인(PDF/텍스트) | user |
| GET | `/api/docs` | 문서 목록 | user |
| POST | `/api/rag/query` | 근거기반 질의(인용) | user |
| POST | `/api/review` | 규정검토(위반·수정문안, 다국어) | user |
| GET | `/api/audit` | 감사로그 | **admin** |

인증: `Authorization: Bearer <token>` 또는 `X-API-Key: <token>`.

## 프로젝트 구조

```
src/main/java/kr/yeokkeum/
├── gateway/   LlmGateway · SpringAiGateway · OpenAiCompatGateway · StubGateway
├── rag/       Tokenizer(CJK) · Chunker · Bm25 · RagService
├── doc/       Document · Chunk (JPA)
├── audit/     AuditLog · AuditService
├── auth/      Principal · AuthInterceptor (RBAC, secure-by-default)
├── web/       Chat/Api/Audit/Public 컨트롤러 + DTO
└── config/    IeumProperties · WebConfig · GatewayConfig · StartupChecks
src/main/resources/static/index.html   ← 내장 정적 콘솔(백엔드 단독 데모용)
frontend/                              ← Vue 3 + KRDS 프론트(권장 UI, npm run build) → frontend/README.md
docs/                                  ← 공공 RFP 기능목록 · eGov5.0 정렬표
```

## 로드맵

- **Phase 0 (완료)** — 게이트웨이 · RAG · 요약/초안 · RBAC · 감사로그 · 온프렘 컨테이너
- **Phase 1** — 규정검토 에이전트 · **HWP/PDF 파싱(완료)** · 다국어 상담 · **Vue 3 + KRDS 프론트(완료)** · Spring Security 이관(완료) · **BM25+BGE-M3 하이브리드 검색(완료)** · pgvector(대규모 시 예정, 현재 인메모리)
- **Phase 2** — 업무 에이전트 + MCP 도구 + HITL 승인 · 데이터 분석/이상탐지 · 오픈API/SSO
- **Phase 3** — CSAP · N2SF · 개인정보(PIA) · 감리 산출물 · 국산모델 · DR/이중화
- **Phase 4** — 디지털서비스몰/혁신제품 등록

기능·통과조건 전체 → [docs/기능목록.md](docs/기능목록.md)

## 라이선스

[Apache License 2.0](LICENSE) — eGov 생태계 관례 정렬.
