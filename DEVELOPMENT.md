# Development Guide

This document outlines the development workflow and commands available in the `Makefile`.

## Prerequisites

- Docker
- Docker Compose

## Getting Started

1.  **Build the workspace:**
    ```bash
    make workspace-build
    ```
2.  **Start the workspace:**
    ```bash
    make workspace-up
    ```
3.  **Enter the shell:**
    ```bash
    make shell
    ```

## Common Commands

The `Makefile` provides several commands to streamline development. Run `make help` to see a full list.

### Code Quality

-   **Linting:** Run `make lint` to check for code style issues using ktlint and detekt.
-   **Formatting:** Run `make fix` to automatically fix formatting issues.

### Testing & Building

-   **Test:** Run `make test` to execute unit and integration tests.
-   **Build:** Run `make build-all` for a full build (compile, check, test).
-   **Refresh Dependencies:** Run `make refresh` to force Gradle to refresh dependencies.

### Running Applications

-   **Run API:** `make run-api` starts the API application.
    *   **Live Reload:** To enable live reloading for the API, you must run `make gradle-watch` in a separate terminal window alongside `make run-api`. This ensures that changes are recompiled and picked up by the running server.
-   **Run Worker:** `make run-worker` starts the worker application (with continuous build).

## Project Structure

-   `modules/`: Contains the application modules (api, worker, domain, etc.).
-   `buildSrc/`: Contains build logic and conventions.
-   `gradle/`: Gradle wrapper and configuration.
