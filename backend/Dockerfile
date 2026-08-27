# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=cf_access_client_id \
    --mount=type=secret,id=cf_access_client_secret \
    chmod +x ./gradlew && \
    if [ -s /run/secrets/cf_access_client_id ] && [ -s /run/secrets/cf_access_client_secret ]; then \
      export CF_ACCESS_AUTHORIZATION="$(printf '{"cf-access-client-id":"%s","cf-access-client-secret":"%s"}' \
        "$(cat /run/secrets/cf_access_client_id)" \
        "$(cat /run/secrets/cf_access_client_secret)")"; \
    fi && \
    ./gradlew test bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
