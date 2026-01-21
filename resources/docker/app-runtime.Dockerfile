# Runtime-only Dockerfile - copies pre-built artifacts from host
# Usage:
#   1. Build artifacts on host: ./gradlew :modules:app-api:installDist :modules:app-worker:installDist :modules:presentation:buildOpenApi
#   2. Build image: docker build -f resources/docker/app-runtime.Dockerfile -t kotlin-ktor-postgres-template-prod:latest .
#
# This approach allows jOOQ generation to run on the host (with Testcontainers)
# and avoids Docker-in-Docker complexity.

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

# Copy pre-built applications from host
# These should be built with: ./gradlew :modules:app-api:installDist :modules:app-worker:installDist
# API application
COPY --chown=${USERNAME}:${USERNAME} modules/app-api/build/install/app-api/ ./api/
# Worker application
COPY --chown=${USERNAME}:${USERNAME} modules/app-worker/build/install/app-worker/ ./worker/
# Copy OpenAPI schema to API directory (if it exists - build will fail if required but missing)
COPY --chown=${USERNAME}:${USERNAME} openapi.json ./api/openapi.json

# Switch to non-root user
USER ${USERNAME}

# Expose port for API (Worker doesn't need it, but harmless)
EXPOSE 8000

# Default command (will be overridden by docker-compose)
# Note: Health checks should be configured in your orchestration layer (docker-compose, k8s, etc.)
CMD ["sh", "-c", "echo 'Please specify which application to run: api or worker'"]
