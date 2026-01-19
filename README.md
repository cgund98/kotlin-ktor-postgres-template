# Kotlin PostgreSQL Template

A production-ready Kotlin web API template demonstrating modern best practices for building scalable backend services. This template includes a RESTful API server and an event-driven worker application that consumes messages from AWS SQS.

## Overview

This template provides a solid foundation for building production-ready Kotlin applications with:

- **RESTful API Server** - Built with Ktor, featuring OpenAPI documentation, request validation, and comprehensive error handling
- **Event Consumer Worker** - Asynchronous message processing using AWS SQS for event-driven architecture
- **Clean 3-Tier Architecture** - Strict separation of concerns with Presentation, Domain, and Infrastructure layers
- **Database Migrations** - Version-controlled database schema migrations using migrate
- **Configuration Management** - Support for `.env` files and environment variables for flexible deployment

## Key Features

- ✅ **Production-Ready** - Includes error handling, logging, validation, and testing infrastructure
- ✅ **Event-Driven Architecture** - SNS/SQS integration for pub/sub messaging patterns
- ✅ **Environment Configuration** - Supports `.env.local` (committed) and `.env` (for sensitive values) with environment variable overrides
- ✅ **OpenAPI Documentation** - Auto-generated API documentation available at `/docs`
- ✅ **Database Migrations** - Version-controlled schema changes with rollback support
- ✅ **Docker Development** - Complete Docker Compose setup for local development
- ✅ **Code Quality** - Integrated linting (ktlint, detekt) and formatting tools

## Tech Stack

### Core Framework
- **Kotlin** - Modern JVM language with coroutines support
- **Ktor** - Lightweight web framework for building async applications
- **Koin** - Dependency injection framework

### Database
- **PostgreSQL** - Relational database
- **Exposed** - Type-safe SQL framework
- **HikariCP** - High-performance JDBC connection pool
- **migrate** - Database migration tool

### AWS Services
- **AWS SDK Kotlin** - Official AWS SDK for Kotlin
- **SNS** - Pub/sub messaging for event publishing
- **SQS** - Message queue for event consumption
- **LocalStack** - Local AWS services for development

### Serialization & Validation
- **Kotlinx Serialization** - JSON serialization
- **Ktor Request Validation** - Built-in request validation

### Logging & Observability
- **Kotlin Logging** - Structured logging facade
- **Logback** - Logging framework with Logstash encoder support

### Code Quality
- **ktlint** - Kotlin code style checker
- **detekt** - Static code analysis for Kotlin

### Development Tools
- **Gradle** - Build tool with version catalogs
- **Docker & Docker Compose** - Containerized development environment

## Architecture

This application is designed with **clean 3-tier architecture** principles, ensuring clear separation of concerns and maintainability:

### Three-Tier Architecture

1. **Presentation Layer** (`modules/presentation/`)
   - HTTP routes and request/response DTOs
   - API validation and error handling
   - OpenAPI documentation

2. **Domain Layer** (`modules/domain/`)
   - Business logic and domain models
   - Domain services and validators
   - Repository interfaces (contracts)
   - Event handlers for processing domain events
   - Pure business rules with no external dependencies

3. **Infrastructure Layer** (`modules/infrastructure/`)
   - Database implementations (Exposed)
   - AWS integrations (SNS/SQS)
   - Event publishing and consumption
   - External service adapters

### Module Structure

```
modules/
├── app-api/          # REST API application entry point
├── app-worker/       # Event consumer worker application
│   └── WorkerModule.kt  # Event handler registration and consumer setup
├── core/             # Core configuration and utilities
├── domain/            # Domain layer - business logic
│   └── user/          # Example domain module
│       ├── User.kt           # Domain model
│       ├── UserService.kt    # Business logic
│       ├── UserValidator.kt  # Validation rules
│       ├── handlers/         # Event handlers
│       │   ├── UserCreatedHandler.kt
│       │   ├── UserUpdatedHandler.kt
│       │   └── UserDeletedHandler.kt
│       └── repo/             # Repository interfaces
├── infrastructure/    # Infrastructure layer - external integrations
│   ├── db/           # Database implementations
│   ├── events/       # Event infrastructure
│   │   ├── consumer/ # SQS consumers and supervisor
│   │   ├── publisher/ # SNS event publisher
│   │   ├── registry/  # Event type definitions
│   │   └── serializer/ # Event serialization
│   └── exposed/      # Exposed ORM integration
└── presentation/      # Presentation layer - API
    └── user/          # User API routes and DTOs
```

### Event-Driven Architecture Flow

The application implements an event-driven architecture pattern:

1. **Event Publishing** (API → Infrastructure)
   - Domain services publish events via `EventPublisher` interface
   - Events are serialized and published to AWS SNS topics
   - Example: `UserService.createUser()` publishes `UserCreated` event

2. **Event Consumption** (Infrastructure → Domain)
   - SQS queues subscribe to SNS topics
   - `SqsEventConsumer` polls queues and deserializes messages
   - `ConsumerSupervisor` manages multiple consumers concurrently
   - Domain event handlers process events (e.g., `UserCreatedHandler`)

3. **Event Handlers** (Domain Layer)
   - Handlers implement `EventHandler<EventType>` interface
   - Located in `modules/domain/*/handlers/`
   - Contain business logic for processing domain events
   - Can call domain services to perform side effects
   - Registered in `WorkerModule` and bound to SQS queues

**Example Flow:**
```
API Request → UserService.createUser() 
  → Publishes UserCreated event (SNS)
  → SQS queue receives message
  → UserCreatedHandler.handleEvent()
  → Performs business logic (logging, notifications, etc.)
```

### Resources Directory

The `resources/` directory contains non-code assets:

```
resources/
├── db/
│   └── migrations/          # Database migration files (.up.sql, .down.sql)
├── docker/
│   ├── workspace.Dockerfile # Development workspace container
│   └── migrate.Dockerfile   # Migration tool container
└── scripts/
    ├── awslocal.sh          # AWS CLI wrapper for LocalStack
    ├── localstack_setup.sh  # LocalStack resource setup
    └── migrate.sh           # Migration helper script
```

## Quick Start

### Prerequisites

- Docker
- Docker Compose

### Getting Started

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd kotlin-postgres-template
   ```

2. **Review environment configuration**
   - `.env.local` is committed to the repository with default values suitable for local development
   - Create a `.env` file if you need to override with sensitive values (not required for local development)

3. **Build and start the workspace**
   ```bash
   make workspace-build
   make workspace-up
   make shell
   ```

4. **Run database migrations**
   ```bash
   make migrate
   ```

5. **Start the API server**
   ```bash
   make run-api
   ```

6. **Start the worker (in a separate terminal)**
   ```bash
   make run-worker
   ```

The API will be available at `http://localhost:8000` with OpenAPI documentation at `http://localhost:8000/docs`.

## Configuration

Configuration is managed through environment variables and `.env` files. The application loads configuration in the following order:

1. `.env.local` - Committed default configuration for local development
2. `.env` - Optional file for sensitive values (gitignored, not required for local development)
3. Environment variables - Can override any setting

**Note:** This template does not contain sensitive values, so `.env.local` is sufficient for local development. In production, use environment variables or a `.env` file for sensitive configuration.

Key configuration variables:

- `POSTGRES_URL` - Database connection string
- `API_PORT` - API server port (default: 8000)
- `AWS_REGION` - AWS region for SNS/SQS
- `AWS_USE_LOCALSTACK` - Use LocalStack for local development
- `EVENTS_TOPIC_ARN` - SNS topic ARN for event publishing
- `EVENTS_QUEUE_URL_*` - SQS queue URLs for event consumption

See `modules/core/src/main/kotlin/com/github/cgund98/template/core/config/AppConfig.kt` for all available configuration options.

## Development

For detailed development instructions, see [DEVELOPMENT.md](./DEVELOPMENT.md).

### Common Commands

```bash
make help          # Show all available commands
make lint          # Run code quality checks
make fix           # Auto-fix formatting issues
make test          # Run tests
make build-all     # Full build
make run-api       # Start API server
make run-worker    # Start worker application
```

## Project Structure

- `modules/` - Application modules organized by 3-tier architecture
- `resources/` - Non-code assets
  - `db/migrations/` - Database migration files (version-controlled schema changes)
  - `docker/` - Dockerfile definitions for workspace and migration containers
  - `scripts/` - Utility scripts for AWS LocalStack and migrations
- `buildSrc/` - Gradle build logic and conventions
- `gradle/` - Gradle wrapper and version catalogs

## License

[Add your license here]

## Contributing

[Add contributing guidelines here]
