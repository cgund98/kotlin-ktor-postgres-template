# 🚀 Kotlin PostgreSQL Template

<div align="center">

**A production-ready Kotlin web API template demonstrating modern best practices for building scalable backend services**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-3.3.3-green.svg)](https://ktor.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue.svg)](https://www.postgresql.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.3+-orange.svg)](https://gradle.org/)
[![JDK](https://img.shields.io/badge/JDK-21-orange.svg)](https://adoptium.net/)

*Built with ❤️ using Clean Architecture and Event-Driven Design*

[Features](#-key-features) • [Quick Start](#-quick-start) • [Architecture](#-architecture) • [Development Guide](DEVELOPMENT.md)

</div>

## 👥 Who Is This For?

This template is designed for **backend engineers** building:

- RESTful APIs with clean separation between HTTP handlers, business logic, and data access
- Event-driven microservices with async message processing (SQS/SNS)
- Type-safe codebases with comprehensive type checking and fast unit tests
- Production-ready systems with proper transaction management and error handling

## Overview

This template provides a solid foundation for building production-ready Kotlin applications with:

- **RESTful API Server** - Built with Ktor, featuring OpenAPI documentation, request validation, and comprehensive error handling
- **Event Consumer Worker** - Asynchronous message processing using AWS SQS for event-driven architecture
- **Clean 3-Tier Architecture** - Strict separation of concerns with Presentation, Domain, and Infrastructure layers
- **Database Migrations** - Version-controlled database schema migrations using Flyway
- **Configuration Management** - Support for `.env` files and environment variables for flexible deployment

## Why Kotlin?

This template uses Kotlin for several compelling reasons:

- **Modern Ergonomics** - Kotlin's concise syntax, null safety, and smart type inference reduce boilerplate while improving code readability. Features like data classes, extension functions, and when expressions make code more expressive and maintainable.

- **Advanced Type System** - Kotlin's powerful type system catches errors at compile-time, reducing runtime bugs. Features like sealed classes, inline classes, and reified generics enable expressive domain modeling and type-safe abstractions.

- **Excellent Concurrency Model** - Kotlin Coroutines provide a lightweight, efficient concurrency model that's perfect for modern async applications. Unlike traditional threading models, coroutines enable structured concurrency, making it easier to write and reason about concurrent code. This is especially valuable for event-driven architectures and high-throughput APIs.

- **JVM Ecosystem** - Full interoperability with Java libraries and frameworks means you can leverage the vast JVM ecosystem while enjoying Kotlin's modern language features.

- **Production Proven** - Used by companies like Google, JetBrains, and Square in production systems, demonstrating its reliability and performance at scale.

## ✨ Key Features

- ✅ **Production-Ready** - Includes error handling, logging, validation, and testing infrastructure
- ✅ **Flexible Logging** - Toggle between JSON (production) and human-readable (development) formats via `LOG_FORMAT` environment variable
- ✅ **Event-Driven Architecture** - SNS/SQS integration for pub/sub messaging patterns
- ✅ **Environment Configuration** - Supports `.env.local` (committed) and `.env` (for sensitive values) with environment variable overrides
- ✅ **OpenAPI Documentation** - Auto-generated API documentation available at `/docs`
- ✅ **Database Migrations** - Version-controlled schema changes with rollback support
- ✅ **Docker Development** - Complete Docker Compose setup for local development
- ✅ **Code Quality** - Linting (ktlint, detekt) and formatting tools

## Tech Stack

### Core Framework
- **Kotlin** - Modern JVM language with coroutines support
- **Ktor** - Lightweight web framework for building async applications
- **Koin** - Dependency injection framework

### Database
- **PostgreSQL** - Relational database
- **Exposed** - Type-safe SQL framework
- **HikariCP** - High-performance JDBC connection pool
- **Flyway** - Database migration tool

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
└── scripts/
    ├── awslocal.sh          # AWS CLI wrapper for LocalStack
    ├── localstack_setup.sh  # LocalStack resource setup
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

5. **Set up LocalStack (required for local development)**
   ```bash
   make localstack-setup
   ```
   LocalStack mocks AWS services (SNS/SQS) locally, allowing you to develop and test event-driven features without connecting to real AWS services.

6. **Start the API server**
   ```bash
   make run-api
   ```

7. **Start the worker (in a separate terminal)**
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
- `LOG_FORMAT` - Logging format: `JSON` for structured logs (production) or `PRETTY` for human-readable logs (development).
- `LOG_LEVEL` - Logging level (default: `INFO`)
- `AWS_REGION` - AWS region for SNS/SQS
- `AWS_USE_LOCALSTACK` - Set to `true` to use LocalStack for mocking AWS services locally (required for local development)
- `AWS_ENDPOINT` - AWS endpoint URL (set to `http://localstack:4566` when using LocalStack)
- `EVENTS_TOPIC_ARN` - SNS topic ARN for event publishing
- `EVENTS_QUEUE_URL_*` - SQS queue URLs for event consumption

**LocalStack Setup:** LocalStack is required for local development as it mocks AWS services (SNS/SQS) locally. The API and Worker applications depend on these services for event publishing and consumption. Run `make localstack-setup` to start LocalStack and create the necessary SNS topics and SQS queues. Ensure `AWS_USE_LOCALSTACK=true` is set in your `.env.local` file.

**Logging Format:** Set `LOG_FORMAT=JSON` for structured JSON logs (ideal for production and log aggregation tools) or `LOG_FORMAT=PRETTY` for human-readable colored logs (ideal for local development). This can be set in your `.env.local` file or as an environment variable.

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
