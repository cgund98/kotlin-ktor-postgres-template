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

- **Test:** `make test` - Execute unit tests only
- **Integration Test:** `./gradlew :modules:domain:integrationTest` - Execute integration tests against Postgres testcontainer
- **All Tests:** `./gradlew test integrationTest` - Run both unit and integration tests
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

### Code Generation

- **Generate jOOQ classes:** `make generate-jooq` - Generate jOOQ classes from database schema using testcontainer (requires Docker)
  - This command starts a Postgres testcontainer, runs migrations, and generates jOOQ classes
  - Useful when database schema changes and you need to regenerate type-safe database access code
  - Note: Docker must be running for this command to work

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

## Testing Strategies

This project uses two complementary testing strategies to ensure code quality and reliability:

### 1. Unit Tests (`test` source set)

**Location:** `modules/*/src/test/kotlin/`

**Purpose:** Fast, isolated tests that verify business logic without external dependencies.

**Characteristics:**
- **Fast execution** - No database or network calls
- **Isolated** - Each test is independent with mocked dependencies
- **Comprehensive coverage** - Test edge cases, validation logic, and business rules
- **Uses mocks** - Dependencies are mocked using MockK

**When to use:**
- Testing business logic in services
- Testing validation rules
- Testing domain model behavior
- Testing error handling and edge cases

**Example:**
```kotlin
// modules/domain/src/test/kotlin/.../UserServiceTest.kt
class UserServiceTest : FunSpec() {
    val userRepository = mockk<UserRepository>()
    val eventPublisher = mockk<EventPublisher>()
    // ... test business logic with mocked dependencies
}
```

**Running unit tests:**
```bash
./gradlew test
```

### 2. Integration Tests (`integrationTest` source set)

**Location:** `modules/*/src/integrationTest/kotlin/`

**Purpose:** End-to-end tests that verify the system works with real database interactions.

**Characteristics:**
- **Real database** - Uses Postgres testcontainer for actual database operations
- **Full stack** - Tests the complete flow from service to repository to database
- **Real migrations** - Runs actual Flyway migrations against testcontainer
- **Isolated per test** - Database is cleaned between tests for isolation

**When to use:**
- Testing repository implementations (jOOQ queries)
- Testing transaction behavior
- Testing database constraints and relationships
- Verifying end-to-end CRUD operations
- Testing complex queries and pagination

**Example:**
```kotlin
// modules/domain/src/integrationTest/kotlin/.../UserServiceIntegrationTest.kt
class UserServiceIntegrationTest : FunSpec(), KoinTest {
    override suspend fun beforeSpec(spec: Spec) {
        stopKoin = setupTestKoin() // Sets up testcontainer + Koin
    }
    // ... test against real Postgres database
}
```

**Running integration tests:**
```bash
./gradlew :modules:domain:integrationTest
```

**Requirements:**
- Docker must be running (for testcontainers)
- Tests automatically start/stop Postgres container

### Test Configuration

**Unit Tests:**
- Source set: `test`
- Task: `test`
- Dependencies: MockK, Kotest
- Runs by default with `./gradlew test`

**Integration Tests:**
- Source set: `integrationTest`
- Task: `integrationTest`
- Dependencies: Testcontainers, Flyway, Koin Test
- Must be explicitly run: `./gradlew integrationTest`
- Does NOT run with `./gradlew test` (by design)

### CI/CD Integration

Both test strategies run in CI:
- **Unit tests** run in the `test` job
- **Integration tests** run in the `integrationTest` job (requires Docker)

See `.github/workflows/ci.yml` for the complete CI configuration.

### Best Practices

1. **Write unit tests first** - They're faster and catch logic errors early
2. **Use integration tests sparingly** - Focus on critical paths and repository implementations
3. **Keep tests isolated** - Each test should be independent and not rely on execution order
4. **Clean database state** - Integration tests clean the database between tests
5. **Mock external services** - Even in integration tests, mock AWS services (SNS/SQS)

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
