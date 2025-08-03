# Multi-stage Docker build for Code Guard
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk add --no-cache \
    bash \
    curl \
    git \
    && rm -rf /var/cache/apk/*

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/code-guard-*.jar app.jar

# Create directories for reports and logs
RUN mkdir -p /app/reports /app/logs /app/input && \
    chown -R appuser:appgroup /app

# Copy entrypoint script
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && \
    chown appuser:appgroup /app/entrypoint.sh

# Switch to non-root user
USER appuser

# Expose ports (for potential web interface or API)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD java -version || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    APP_HOME="/app" \
    REPORTS_DIR="/app/reports" \
    LOGS_DIR="/app/logs"

# Volume for reports and input files
VOLUME ["/app/reports", "/app/input", "/app/logs"]

# Entry point
ENTRYPOINT ["/app/entrypoint.sh"]

# Default command
CMD ["--help"]
