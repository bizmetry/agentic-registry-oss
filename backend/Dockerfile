# =========================
# STAGE 1 - Build
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Código
COPY src ./src

# Build jar
RUN mvn -B -q clean package -DskipTests


# =========================
# STAGE 2 - Runtime
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos todos los jars generados
COPY --from=builder /build/target/*.jar /app/build/

# Copiamos entrypoint
COPY entrypoint.sh /app/entrypoint.sh

# Seleccionamos el jar ejecutable (no -plain)
RUN set -eux; \
    chmod +x /app/entrypoint.sh; \
    ls -la /app/build; \
    JAR="$(ls /app/build/*.jar | grep -v -- '-plain\.jar$' | head -n 1)"; \
    if [ -z "$JAR" ]; then \
      echo "ERROR: No runnable jar found (did you build a Spring Boot fat jar?)."; \
      exit 1; \
    fi; \
    mv "$JAR" /app/app.jar; \
    rm -rf /app/build

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]

