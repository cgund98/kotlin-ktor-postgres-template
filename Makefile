# The name of your workspace service in docker-compose.yml
SERVICE_NAME = workspace
# Standard exec command - ensures a TTY for pretty colors
EXEC = docker compose exec -it $(SERVICE_NAME)

.PHONY: lint format test build-all help workspace-build workspace-up workspace-shell gradle-watch gradle-clean run-api run-worker migrate migrate-up migrate-down build-migrations

help: ## Show this help
	@echo "See DEVELOPMENT.md for more details."
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# Workspace commands
workspace-build: ## Build the docker workspace image
	docker compose build workspace

workspace-up: ## Start the docker workspace in the background
	docker compose up -d workspace

workspace-down: ## Stop the workspace container
	docker compose down workspace

workspace-shell: ## Open a shell in the workspace container (alias for shell)
	$(EXEC) /bin/bash

# Linting and formatting
lint: ## Run checkstyle/ktlint checks
	$(EXEC) ./gradlew ktlintCheck detekt

fix: ## Automatically fix Kotlin formatting
	$(EXEC) yamlfmt .
	$(EXEC) ./gradlew ktlintFormat
	$(EXEC) ./gradlew detekt --auto-correct

test: ## Run tests (unit + integration)
	$(EXEC) ./gradlew test

# Build
build-all: ## Run a full build (compilation + check + test)
	$(EXEC) ./gradlew build

refresh: ## Force Gradle to refresh dependencies
	$(EXEC) ./gradlew build --refresh-dependencies

gradle-watch: ## Run Gradle in continuous mode to recompile on changes
	$(EXEC) ./gradlew -t classes

gradle-clean:
	$(EXEC) ./gradlew clean

# Run dev servers
run-api: ## Run the API application
	# OpenAPI generation sometimes breaks when you tweak a route
	# So we run clean before we build as a workaround
	$(EXEC) ./gradlew clean :modules:app-api:run

run-worker: ## Run the Worker application with continuous build
	$(EXEC) ./gradlew -t :modules:app-worker:run

# Migrations
build-migrations:
	docker compose build migrate

migrate: build-migrations ## Run database migrations (up)
	docker compose run --rm migrate -path /migrations -database "postgres://postgres:postgres@postgres:5432/app?sslmode=disable" up

migrate-up: migrate ## Alias for migrate

migrate-down: build-migrations ## Rollback last migration
	docker compose run --rm migrate -path /migrations -database "postgres://postgres:postgres@postgres:5432/app?sslmode=disable" down
