# ── Stage 1: build the fat JAR ────────────────────────────────
# Uses the official Gradle image so no gradlew wrapper jar is needed.
FROM gradle:8.6-jdk17 AS build

WORKDIR /workspace

# Copy the shared proto directory first (changes rarely → better layer cache)
COPY proto/ proto/

# Copy the backend project
COPY backend/ backend/

WORKDIR /workspace/backend

# buildFatJar is provided by the io.ktor.plugin — produces a single
# executable JAR with all dependencies bundled.
RUN gradle buildFatJar --no-daemon --info

# ── Stage 2: minimal runtime image ────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=build /workspace/backend/build/libs/*-all.jar app.jar

# gRPC on 50051, REST/health on 8080
EXPOSE 50051 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
