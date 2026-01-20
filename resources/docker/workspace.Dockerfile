FROM eclipse-temurin:21-jdk-jammy

# Install standard utilities for your Makefile/Shell
RUN apt-get update && apt-get install -y \
    curl \
    git \
    make \
    unzip \
    xz-utils \
    && rm -rf /var/lib/apt/lists/*

# Install yamlfmt
RUN curl -L https://github.com/google/yamlfmt/releases/download/v0.21.0/yamlfmt_0.21.0_Linux_x86_64.tar.gz | tar xz -C /usr/local/bin yamlfmt

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip -q awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Install watchexec
RUN curl -L https://github.com/watchexec/watchexec/releases/download/v1.23.0/watchexec-1.23.0-x86_64-unknown-linux-musl.tar.xz | tar xJ -C /usr/local/bin --strip-components=1 watchexec-1.23.0-x86_64-unknown-linux-musl/watchexec

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

RUN mkdir -p /home/workspace/.gradle && chown -R $USERNAME:$USERNAME /home/workspace/.gradle

USER $USERNAME
WORKDIR /workspace

# Keep the container running for 'docker exec'
CMD ["sleep", "infinity"]
