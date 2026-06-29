# Stage 1: Build
FROM gradle:8-jdk21 AS builder
WORKDIR /app
# Copy project files
COPY build.gradle settings.gradle ./
COPY src ./src
# Build the application without running tests
RUN gradle build -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Download OpenTelemetry Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
# Expose port
EXPOSE 8080
# Entrypoint to run the jar with OpenTelemetry
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
