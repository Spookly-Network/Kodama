# Build-Stage
FROM gradle:9.2.1-jdk21-alpine AS build
WORKDIR /workspace

COPY backend/. .
RUN gradle :brain:bootJar --no-daemon

# Runtime-Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/brain/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
