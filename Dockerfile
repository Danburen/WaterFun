# syntax=docker/dockerfile:1
# ============================================================
# WaterFun Backend — Single Dockerfile, Multi-Target
#
# Builds all modules once (settings.gradle includes all subprojects),
# then each runtime target extracts only its own JAR.
#
# Usage:
#   docker build --target gateway      -t waterfun-gateway:test      .
#   docker build --target user-service -t waterfun-service:test      .
#   docker build --target admin-service -t waterfun-admin-service:test .
#
# CI/CD:
#   docker/build-push-action@v6 with target: gateway / user-service / admin-service
# ============================================================
FROM gradle:jdk22 AS build
WORKDIR /app

# ── 1. Build definition layer (cached until gradle/* or build.gradle changes) ──
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ ./gradle/
RUN chmod +x gradlew

COPY waterfun-common-lib/ waterfun-common-lib/
COPY waterfun-service-core/build.gradle waterfun-service-core/
COPY waterfun-gateway/build.gradle waterfun-gateway/
COPY waterfun-service/build.gradle waterfun-service/
COPY waterfun-admin-service/build.gradle waterfun-admin-service/

# ── 2. Download dependencies (cached until build.gradle changes) ──
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# ── 3. Source code → full build ──
COPY waterfun-common-lib/ waterfun-common-lib/
COPY waterfun-service-core/ waterfun-service-core/
COPY waterfun-gateway/ waterfun-gateway/
COPY waterfun-service/ waterfun-service/
COPY waterfun-admin-service/ waterfun-admin-service/

RUN ./gradlew build -x test --no-daemon

# ==================== Runtime: Gateway ====================
FROM eclipse-temurin:22-jre AS gateway
WORKDIR /app
RUN groupadd -r waterfun && useradd -r -g waterfun waterfun
COPY --from=build --chown=waterfun:waterfun /app/waterfun-gateway/build/libs/*.jar app.jar
EXPOSE 8080
USER waterfun
ENTRYPOINT ["java", "-jar", "app.jar"]

# ==================== Runtime: User Service ====================
FROM eclipse-temurin:22-jre AS user-service
WORKDIR /app
RUN groupadd -r waterfun && useradd -r -g waterfun waterfun
COPY --from=build --chown=waterfun:waterfun /app/waterfun-service/build/libs/*.jar app.jar
EXPOSE 8081
USER waterfun
ENTRYPOINT ["java", "-jar", "app.jar"]

# ==================== Runtime: Admin Service ====================
FROM eclipse-temurin:22-jre AS admin-service
WORKDIR /app
RUN groupadd -r waterfun && useradd -r -g waterfun waterfun
COPY --from=build --chown=waterfun:waterfun /app/waterfun-admin-service/build/libs/*.jar app.jar
EXPOSE 8082
USER waterfun
ENTRYPOINT ["java", "-jar", "app.jar"]
