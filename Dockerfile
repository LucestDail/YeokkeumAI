# ⚠️ 미완 (2026-09-05 확인): frontend 빌드와 rhwp 조달 스텝이 없다.
#    이 이미지는 백엔드 jar 만 담는다 → KRDS UI 안 뜸 · HWP 업로드 503.
#    실배포 정본은 systemd 다. 컨테이너로 옮기려면 아래 두 가지를 넣어야 한다:
#      1) frontend(npm ci && npm run build) → static 으로 복사
#      2) 리눅스 x86-64 rhwp 배치 (deploy/RHWP.md)

# --- build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- run (비root) ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/yeokkeumai-*.jar app.jar
RUN useradd -m appuser && chown -R appuser /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
