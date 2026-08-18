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
