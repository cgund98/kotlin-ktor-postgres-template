# Development Guide

This document outlines the development workflow and commands available in the `Makefile`.

## Prerequisites

- Docker
- Docker Compose

## Getting Started

1. **Build the workspace:**
   ```bash
   make workspace-build
   ```

2. **Start the workspace:**
   ```bash
   make workspace-up
   ```

3. **Enter the shell:**
   ```bash
   make shell
   ```

4. **Run database migrations:**
   ```bash
   make migrate
   ```
   
   Note: Ensure PostgreSQL is running (`docker compose up -d postgres`) before running migrations.

5. **Set up LocalStack (for event-driven features):**
   ```bash
   make localstack-setup
   ```

## Common Commands

The `Makefile` provides several commands to streamline development. Run `make help` to see a full list.

### Workspace Management

- **Build workspace:** `make workspace-build` - Build the Docker workspace image
- **Start workspace:** `make workspace-up` - Start the workspace container in the background
- **Stop workspace:** `make workspace-down` - Stop the workspace container
- **Open shell:** `make shell` or `make workspace-shell` - Open a bash shell in the workspace container

### Code Quality

- **Linting:** `make lint` - Run code quality checks using ktlint and detekt
- **Formatting:** `make fix` - Automatically fix formatting issues (ktlint and yamlfmt)

### Testing & Building

- **Test:** `make test` - Execute unit and integration tests
- **Build:** `make build-all` - Run a full build (compile, check, test)
- **Refresh Dependencies:** `make refresh` - Force Gradle to refresh dependencies
- **Clean:** `make gradle-clean` - Clean Gradle build artifacts
- **Watch:** `make gradle-watch` - Run Gradle in continuous mode to recompile on changes

### Running Applications

- **Run API:** `make run-api` - Start the API application
  - **Live Reload:** To enable live reloading for the API, run `make gradle-watch` in a separate terminal window alongside `make run-api`. This ensures that changes are recompiled and picked up by the running server.
- **Run Worker:** `make run-worker` - Start the worker application with continuous build

### Database Migrations

- **Run migrations:** `make migrate` - Apply all pending database migrations
- **Migration status:** `make migrate-info` - Show current migration status and history
- **Repair migrations:** `make migrate-repair` - Repair Flyway schema history table (use if corrupted)

See `resources/db/README.md` for detailed migration documentation.

### LocalStack (AWS Local Development)

- **Start LocalStack:** `make localstack-up` - Start LocalStack and wait for it to be ready
- **Setup resources:** `make localstack-setup` - Set up LocalStack resources (SNS topics and SQS queues)
- **Stop LocalStack:** `make localstack-down` - Stop LocalStack
- **View logs:** `make localstack-logs` - Show LocalStack logs

## Project Structure

- `modules/` - Application modules organized by 3-tier architecture
  - `app-api/` - REST API application entry point
  - `app-worker/` - Event consumer worker application
  - `core/` - Core configuration and utilities
  - `domain/` - Domain layer (business logic and domain models)
  - `infrastructure/` - Infrastructure layer (database, AWS, events)
  - `presentation/` - Presentation layer (API routes and DTOs)
- `resources/` - Non-code assets
  - `db/migrations/` - Database migration files (Flyway format)
  - `docker/` - Dockerfile definitions
  - `scripts/` - Utility scripts for AWS LocalStack
- `buildSrc/` - Gradle build logic and conventions
- `gradle/` - Gradle wrapper and version catalogs

## Development Workflow

### Typical Development Session

1. Start the workspace: `make workspace-up`
2. Enter the shell: `make shell`
3. Run migrations: `make migrate`
4. Set up LocalStack (if needed): `make localstack-setup`
5. In separate terminals:
   - Run `make gradle-watch` for continuous compilation
   - Run `make run-api` for the API server
   - Run `make run-worker` for the worker (if needed)

### Code Quality Workflow

Before committing:
1. Run `make lint` to check for issues
2. Run `make fix` to automatically fix formatting issues
3. Run `make test` to ensure all tests pass
4. Run `make build-all` for a complete build check

## Environment Configuration

Configuration is managed through environment variables and `.env` files:

1. `.env.local` - Committed default configuration for local development
2. `.env` - Optional file for sensitive values (gitignored, not required for local development)
3. Environment variables - Can override any setting

The application automatically loads `.env.local` and `.env` files on startup. See `modules/core/src/main/kotlin/com/github/cgund98/template/core/config/AppConfig.kt` for all available configuration options.

## Architecture Overview

This application follows a clean 3-tier architecture:

1. **Presentation Layer** - HTTP routes, DTOs, and API validation
2. **Domain Layer** - Business logic, domain models, and repository interfaces
3. **Infrastructure Layer** - Database implementations, AWS integrations, and external services

See the [README.md](./README.md) for detailed architecture documentation.

## Troubleshooting

### Gradle Build Issues

- Run `make gradle-clean` to clear build artifacts
- Run `make refresh` to refresh dependencies
- Ensure you're running commands inside the workspace container (`make shell`)

### Database Connection Issues

- Ensure PostgreSQL is running: `docker compose ps postgres`
- Check database health: `docker compose logs postgres`
- Verify migrations have run: `make migrate-info` or `make migrate`
- Check Flyway schema history: `make migrate-info` shows applied migrations

### LocalStack Issues

- Check LocalStack status: `make localstack-logs`
- Restart LocalStack: `make localstack-down && make localstack-up`
- Re-run setup: `make localstack-setup`

### Port Conflicts

- API runs on port 8000 by default (configurable via `API_PORT`)
- PostgreSQL runs on port 5432
- LocalStack runs on port 4566

If ports are in use, modify `docker-compose.yml` or use environment variables to change them.
