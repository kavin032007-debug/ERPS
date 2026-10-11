# Multi-stage Dockerfile for Examination Result Processing System (ERPS)
# Stage 1: Build the application fat JAR
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /build

# Cache maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests=true -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="ERPS Engineering Team"

# Set up non-root system user for security compliance
RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser

WORKDIR /app

# Copy the built fat JAR from stage 1
COPY --from=build /build/target/erps-system-*.jar app.jar

# Adjust file ownership
RUN chown -R appuser:appgroup /app

# Switch to unprivileged non-root user
USER appuser

# Expose default port (overridable by cloud platforms via $PORT)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Run with container-aware JVM flags and dynamic PORT support
ENTRYPOINT ["sh", "-c", "java -Xmx384m -XX:+UseG1GC -Dserver.port=${PORT:-8080} -jar app.jar"]
