FROM eclipse-temurin:21-jdk-jammy

# 1. Install standard utilities for your Makefile/Shell
RUN apt-get update && apt-get install -y \
    curl \
    git \
    make \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# 2. Create a non-root user (Standard practice for 2026)
# This prevents 'root' from owning your local files when you mount volumes
ARG USERNAME=kotlin-dev
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && apt-get update \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

USER $USERNAME
WORKDIR /workspace

# 3. Pre-warm Gradle (Optional but recommended)
# This downloads Gradle so your first 'make lint' is instant
COPY --chown=kotlin-dev:kotlin-dev gradlew .
COPY --chown=kotlin-dev:kotlin-dev gradle gradle
RUN ./gradlew --version

# Keep the container running for 'docker exec'
CMD ["sleep", "infinity"]