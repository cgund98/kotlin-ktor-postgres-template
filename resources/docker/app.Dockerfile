# Multi-stage build for production applications (API and Worker)
# Builds both applications in a single image
# Usage:
#   docker build -f resources/docker/app.Dockerfile -t kotlin-ktor-postgres-template-prod:latest .

# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Amper wrapper and configuration files first
COPY amper ./
COPY project.yaml ./
COPY libs.versions.toml ./gradle/

# Copy all modules (source code and Amper module files)
COPY modules/ ./modules/

# Build all modules using Amper
# This will produce executable jars (with BOOT-INF/lib) for jvm/app modules
RUN ./amper package --module app-api --module app-worker --format executable-jar

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

# Copy built artifacts and Amper for running
COPY --from=builder --chown=${USERNAME}:${USERNAME} /build /app

# Switch to non-root user
USER ${USERNAME}

# Expose port for API
EXPOSE 8000

# Default command (will be overridden by docker-compose)
CMD ["sh", "-c", "echo 'Please specify which application to run: api or worker'"]
