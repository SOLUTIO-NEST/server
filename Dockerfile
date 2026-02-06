# Multi-stage build for Spring Boot
# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Gradle 캐싱 최적화: 먼저 의존성만 다운로드
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 빌드 결과물만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 비root 유저로 실행 (보안)
RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# HEALTHCHECK 설정
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/health || exit 1


ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
