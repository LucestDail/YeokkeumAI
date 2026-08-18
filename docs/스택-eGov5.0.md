# 엮음AI 백엔드 — eGovFrame 5.0 구성 정렬표 (2026-08)

> 기준 = 전자정부 표준프레임워크 **5.0**(2026-03 공개, JDK 17+, Spring Boot 3.5.6). 아래 버전은 eGov 5.0 실행환경 baseline.
> 상태: ✅적용 / 🔜계획 / ◻선택(필요시). 엮음AI은 5.0 실행환경 baseline에 맞추되, MVP에 불필요한 요소는 계획으로 둔다.

## 실행환경(Execution) 정렬
| eGov 5.0 서비스 | 오픈소스 · 버전 | 라이선스 | 엮음AI 상태 |
|---|---|---|---|
| Bootstrapping/Auto-Config | **Spring Boot 3.5.6** | Apache 2.0 | ✅ 적용(parent 3.5.6) |
| Core/IoC/AOP/Property/Resource/Tx | **Spring Framework 6.2.11** | Apache 2.0 | ✅ (Boot BOM) |
| Server Security | **Spring Security 6.5.5** | Apache 2.0 | 🔜 현재 AuthInterceptor(RBAC)→Security FilterChain 이관 예정 |
| Data Access | **Spring Data JPA 3.5.3 / Hibernate 6.6.12** | Apache/LGPL | ✅ JPA 사용(H2 dev/PG prod) |
| Data Access(대안) | MyBatis 3.5.19 | Apache 2.0 | ◻ 엄격 eGov 시 스왑 |
| **AI layer — AI Support** | **Spring AI 1.0.1** | Apache 2.0 | ✅ **적용**(SpringAiGateway, OpenAI 호환 base-url→국산/사내) |
| AI layer(대안) | LangChain4j 1.8.0 | Apache 2.0 | ◻ 대안 구현 가능(LlmGateway 인터페이스 드롭인) |
| Rest API 문서 | springdoc 2.8.13 | Apache 2.0 | 🔜 Swagger UI 추가(RFP 단골 요구) |
| Batch | Spring Batch 5.2.3 | Apache 2.0 | 🔜 대량 색인/집계 배치 시 |
| Scheduling | Quartz 2.5.0 | Apache 2.0 | 🔜 예약작업(프로액티브) |
| Cache | EHCache 3.10.8 | Apache 2.0 | 🔜 RAG/응답 캐시 |
| Excel/문서 | POI 5.4.0, jXLS 3.0.0 | Apache 2.0 | 🔜 HWP/문서 파싱·리포팅 인접 |
| Encryption | jasypt 1.9.3 | Apache 2.0 | 🔜 설정/개인정보 암호화 |
| Logging | Log4j2 2.25.2 / slf4j 2.0.17 | Apache 2.0 | ✅ slf4j(Boot 기본 logback; Log4j2 교체 선택) |
| Web Service | CXF 3.5.8 | Apache 2.0 | ◻ 레거시 SOAP 연계 필요시 |
| Cloud Data Stream | Spring Cloud Stream 4.3.0 | Apache 2.0 | ◻ |

## 개발환경(Development) 정렬
- JDK **17**(eGov 5.0는 21도 지원; 엮음AI=17로 4.3/5.0 양립). Build: **Maven 3.9.9**(로컬 3.9.16 OK). Lombok 1.18.38 ◻(현재 미사용, 순수 자바). 코드검사 PMD 7.15/SpotBugs 4.9 🔜 CI. 테스트 JUnit 5.12/Maven 🔜(현재 JUnit5 사용).

## 운영환경(Operations) 정렬 (공공 배포 시)
- k8s 1.32.5 · Istio 1.26 · Prometheus 2.53 · Grafana 11.3(⚠️AGPL-3.0 검토) · OpenTelemetry 0.120 · Jaeger 1.63 · Loki 3.2(⚠️AGPL) · Kiali · AlertManager. → 🔜 온프렘 배포/관측 스택(라이선스=AGPL은 용도 검토).

## 프론트엔드 — KRDS(디지털정부 서비스 디자인 시스템)
- 기준 = **KRDS**(krds.go.kr): 디자인 원칙 · **디자인 토큰(색상·타이포·레이아웃, 코드정의)** · 컴포넌트(identity/navigation/forms/feedback/content) · **웹접근성 내장(KWCAG)** · 개발 리소스(GitHub/Figma).
- 엮음AI 적용: 현재 정적 콘솔(접근성 lang/label) → **Phase1: Vue 3 + KRDS 디자인 토큰/컴포넌트** 채택, KRDS 접근성 준수로 **웹접근성 인증(WA/KWA-WC)** 대응. KRDS 이용조건(MOIS) 확인 필요.

## 요약
- **이번 적용**: Spring Boot 3.5.6 · Spring AI 1.0.1(AI layer) · JPA/Hibernate · JUnit5 · 비root 컨테이너.
- **다음(정렬 잔여)**: Spring Security 이관 · springdoc(API문서) · KRDS Vue3 프론트 · (필요시)MyBatis·Batch·Quartz·POI·관측스택.
