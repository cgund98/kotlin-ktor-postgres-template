FROM eclipse-temurin:21-jdk-jammy

# Tool versions (pinned for reproducible containers)
ARG YAMLFMT_VERSION=0.21.0
ARG WATCHEXEC_VERSION=1.23.0
ARG KTLINT_VERSION=1.8.0
ARG DETEKT_VERSION=1.23.8

# Install standard utilities for your Makefile/Shell
RUN apt-get update && apt-get install -y \
    curl \
    git \
    make \
    unzip \
    xz-utils \
    && rm -rf /var/lib/apt/lists/*

# Install yamlfmt
RUN curl -L "https://github.com/google/yamlfmt/releases/download/v${YAMLFMT_VERSION}/yamlfmt_${YAMLFMT_VERSION}_Linux_x86_64.tar.gz" | tar xz -C /usr/local/bin yamlfmt

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip -q awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Install watchexec
RUN curl -L "https://github.com/watchexec/watchexec/releases/download/v${WATCHEXEC_VERSION}/watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl.tar.xz" | tar xJ -C /usr/local/bin --strip-components=1 "watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl/watchexec"

# Install ktlint (standalone CLI)
RUN curl -sSLo /usr/local/bin/ktlint "https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint" \
    && chmod +x /usr/local/bin/ktlint

# Install detekt (standalone CLI)
RUN curl -sSLo /tmp/detekt.zip "https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}.zip" \
    && unzip -q /tmp/detekt.zip -d /opt \
    && ln -sf "/opt/detekt-cli-${DETEKT_VERSION}/bin/detekt-cli" /usr/local/bin/detekt \
    && rm -f /tmp/detekt.zip

# Create a non-root user (Standard practice for 2026)
# This prevents 'root' from owning your local files when you mount volumes
ARG USERNAME=workspace
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && apt-get update \
    && apt-get install -y sudo \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

RUN mkdir -p /home/workspace/.cache/JetBrains/Amper && chown -R $USERNAME:$USERNAME /home/workspace/.cache/JetBrains/Amper

USER $USERNAME
WORKDIR /workspace

# Keep the container running for 'docker exec'
CMD ["sleep", "infinity"]