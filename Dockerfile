# Build context: repository root (needs proto/ + backend/)
FROM gradle:8.6-jdk17 AS build

WORKDIR /workspace

COPY proto/ proto/
COPY backend/ backend/

WORKDIR /workspace/backend

RUN gradle buildFatJar --no-daemon --info

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /workspace/backend/build/libs/*-all.jar app.jar

EXPOSE 50051 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
