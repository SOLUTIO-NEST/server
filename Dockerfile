FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# -XX:-UseBASE64Intrinsics mitigates CVE-2026-114938 (Java4Shell, AVX-512 Base64 RCE) until the JDK is upgraded to a patched build
ENTRYPOINT ["java", "-XX:-UseBASE64Intrinsics", "-jar", "app.jar"]
