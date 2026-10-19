# Multi-stage Dockerfile for TaskFlow Backend
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build JAR with tests skipped for container image creation
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Stage 2: Lightweight Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl && \
    addgroup -S taskflow && adduser -S taskflow -G taskflow

COPY --from=builder /workspace/target/taskflow-backend-*.jar app.jar
RUN chown -R taskflow:taskflow /app

USER taskflow

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
