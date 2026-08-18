# PLAN — 엮음AI(YeokkeumAI) AI 플랫폼

## 설계 원칙
1. **범용 사용성 먼저, 공공 통과조건은 설계에 내장**(나중에 덧대면 리팩터 비용 큼): 접근성·RBAC·감사로그·비식별·온프렘 배포는 Phase 0부터 골격에 반영.
2. **자율성 아님, 통제·책임성**: 모든 변경/실행/전송은 HITL 승인 + 감사로그. 읽기·조회는 무승인.
3. **벤더무관**: 모델·임베딩·벡터스토어·스토리지 전부 인터페이스 뒤로. 국산 K-AI 핫스왑.
4. **온프렘·폐쇄망 우선**: 외부 SaaS 의존 최소화, 오프라인 설치 패키지 목표.
5. **작게 시작**: Phase 0 MVP는 게이트웨이+채팅+요약+기본 RAG. 과설계 금지.

## 스택 (기준 = Java, eGov 호환)
- **기준 백엔드: Java 17 + Spring Boot 3.x + Maven** — **eGovFrame 4.3 호환 baseline**(JDK 17·Spring 기반·표준 3-tier Controller-Service-Repository). 공공 감리·표준프레임워크 대응에 정석. **엄격 eGov 통합 시 JPA→MyBatis 스왑**(레이어 유지).
- 프론트: 현재 정적 콘솔(접근성 lang/label) → Phase1 **Vue 3 + Vite** + 웹접근성 인증(KWCAG 2.2).
- LLM 게이트웨이: OpenAI 호환 벤더무관(OpenRouter·사내 게이트웨이·국산 K-AI·vLLM) + 오프라인 stub. **osh-ai-gateway** 패턴 정렬.
- RAG: 현재 순수 자바 **BM25**(온프렘·무의존) → Phase1 **pgvector 하이브리드**+재랭커, HWP/PDF 파서.
- 에이전트/도구: **MCP**(my-computer/HARU 기법), 도구 allowlist, HITL 승인 큐 (Phase2).
- 영속: **JPA + H2(dev) / PostgreSQL(prod)** → Phase3 pgvector. 감사로그 테이블.
- 배포: **비root 컨테이너** docker-compose(MVP) → k8s/폐쇄망 패키지·이중화/DR(공공).
- ※ Python(FastAPI) MVP는 `prototype-python/`에 **기능 검증 프로토타입으로 보존**(BM25·게이트웨이·RAG 설계 참조). Java가 기준 베이스.
- **eGovFrame 5.0 정렬(2026-03, Spring Boot 3.5.6)**: 적용=Boot 3.5.6·**Spring AI 1.0.1(AI layer, SpringAiGateway)**·JPA/Hibernate 6.6·JUnit5. 계획=Spring Security 6.5.5 이관·springdoc·(선택)MyBatis 3.5.19·Batch 5.2.3·Quartz·POI. 상세=[docs/스택-eGov5.0.md](docs/스택-eGov5.0.md).
- **프론트=KRDS(krds.go.kr)**: 디자인 토큰·컴포넌트·웹접근성 내장. Phase1 Vue3+KRDS로 웹접근성 인증(WA) 대응.

## 아키텍처
`docs/기능목록.md` §E 참조. 4엔진(게이트웨이·RAG·에이전트/MCP·데이터분석) + API Gateway(인증·RBAC·레이트리밋·감사·HITL) + 웹 콘솔.

## 단계 (로드맵)
- **Phase 0 — MVP**: LLM 게이트웨이 + 채팅(SSE) + 문서 요약/작성 + 기본 RAG(업로드→색인→인용검색) + RBAC + 감사로그 + docker-compose 온프렘.
- **Phase 1 — 규정검토·상담**: 규정검토 에이전트, 민원 챗봇(다국어·정직거절/이관), HWP 입출력, 웹접근성 대응.
- **Phase 2 — 에이전트·연계**: MCP 도구 + HITL 승인, 데이터 분석/이상탐지, 오픈API·SSO(GPKI/간편인증).
- **Phase 3 — 공공 통과조건**: CSAP·N2SF·개인정보(PIA)·표준프레임워크·감리 산출물, 국산모델, DR/이중화.
- **Phase 4 — 조달 진입**: 디지털서비스몰/혁신제품 등록, PoC 레퍼런스.

## 계약(Contract) — Phase 0 완료 정의
- [ ] `/api/chat` SSE 스트리밍 응답(게이트웨이 경유, 모델 env 핫스왑)
- [ ] 문서 업로드→색인→질의 시 **출처 인용** 포함 응답
- [ ] 문서 요약/초안 엔드포인트
- [ ] RBAC(관리자/사용자) + 모든 요청 **감사로그** 적재(누가·언제·무엇·모델·토큰)
- [ ] docker-compose 로 온프렘 1커맨드 기동, /health 200
- [ ] 검증: pytest(엔진·API) GREEN + 브라우저 스모크

## 정직 리스크
- 시장 SI 독식, CSAP/감리는 시간·비용. 기술로 제품력→레퍼런스→조달 순.
- "모든 RFP 수용"은 방향 목표이지 단일 릴리스 보장 아님. 설정/모듈 조합으로 커버.
- 국산모델·N2SF·폐쇄망은 실 환경 접근 필요(파트너/PoC처).
