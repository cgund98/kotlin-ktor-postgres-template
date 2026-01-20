# Multi-stage build for production applications (API and Worker)
# Builds both applications in a single image
# Usage:
#   docker build -f resources/docker/app.Dockerfile -t kotlin-ktor-postgres-template-prod:latest .

# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Gradle wrapper and configuration files first (for better layer caching)
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Copy buildSrc for build conventions
COPY buildSrc/ ./buildSrc/

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy all modules (source code and build files)
COPY modules/ ./modules/

# Build both application distributions and generate OpenAPI schema
# This creates complete distributions with all dependencies for both API and Worker
# The buildOpenApi task generates openapi.json in the project root
RUN ./gradlew :modules:app-api:installDist :modules:app-worker:installDist :modules:presentation:buildOpenApi --no-daemon --warning-mode all

# Runtime stage - use JRE for smaller image size
FROM eclipse-temurin:21-jre-jammy

# Install curl for potential health checks or debugging
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
ARG USERNAME=app
ARG USER_UID=1000
ARG USER_GID=1000

RUN groupadd --gid ${USER_GID} ${USERNAME} && \
    useradd --uid ${USER_UID} --gid ${USER_GID} -m ${USERNAME}

# Set working directory
WORKDIR /app

# Copy both built applications from builder stage
# API application
COPY --from=builder --chown=${USERNAME}:${USERNAME} /build/modules/app-api/build/install/app-api/ ./api/
# Worker application
COPY --from=builder --chown=${USERNAME}:${USERNAME} /build/modules/app-worker/build/install/app-worker/ ./worker/
# Copy OpenAPI schema to API directory (API looks for it in working directory)
COPY --from=builder --chown=${USERNAME}:${USERNAME} /build/openapi.json ./api/openapi.json

# Switch to non-root user
USER ${USERNAME}

# Expose port for API (Worker doesn't need it, but harmless)
EXPOSE 8000

# Default command (will be overridden by docker-compose)
# Note: Health checks should be configured in your orchestration layer (docker-compose, k8s, etc.)
CMD ["sh", "-c", "echo 'Please specify which application to run: api or worker'"]
