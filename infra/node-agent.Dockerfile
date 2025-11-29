# Build-Stage
FROM gradle:9.2.1-jdk21-alpine AS build
WORKDIR /workspace

COPY backend/. .
RUN gradle :node-agent:installDist --no-daemon

# Runtime-Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/node-agent/build/install/node-agent/ ./

ENTRYPOINT ["bin/node-agent"]
