# Variables
SERVICE_NAME = workspace
EXEC = docker compose exec -it $(SERVICE_NAME)
MIGRATE_DB_URL = postgres://postgres:postgres@postgres:5432/app?sslmode=disable

.PHONY: help workspace-build workspace-up workspace-down workspace-shell shell lint fix test build-all refresh gradle-watch gradle-clean run-api run-worker build-migrations migrate migrate-up migrate-down localstack-up localstack-setup localstack-down localstack-logs

help: ## Show this help
	@echo "See DEVELOPMENT.md for more details."
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Workspace commands
workspace-build: ## Build the docker workspace image
	docker compose build workspace

workspace-up: ## Start the docker workspace in the background
	docker compose up -d workspace

workspace-down: ## Stop the workspace container
	docker compose stop workspace

workspace-shell: ## Open a shell in the workspace container
	$(EXEC) /bin/bash

shell: workspace-shell ## Alias for workspace-shell

# Linting and formatting
lint: ## Run checkstyle/ktlint checks
	$(EXEC) ./gradlew ktlintCheck detekt

fix: ## Automatically fix Kotlin formatting
	$(EXEC) yamlfmt .
	$(EXEC) ./gradlew ktlintFormat
	$(EXEC) ./gradlew detekt --auto-correct

# Testing
test: ## Run tests (unit + integration)
	$(EXEC) ./gradlew test

# Build
build-all: ## Run a full build (compilation + check + test)
	$(EXEC) ./gradlew build

refresh: ## Force Gradle to refresh dependencies
	$(EXEC) ./gradlew build --refresh-dependencies

gradle-watch: ## Run Gradle in continuous mode to recompile on changes
	$(EXEC) ./gradlew -t classes

gradle-clean: ## Clean Gradle build artifacts
	$(EXEC) ./gradlew clean

# Run dev servers
run-api: ## Run the API application using Amper
	$(EXEC) ./amper run --module app-api

watch-api: ## Run the API application with live reload using watchexec and Amper
	$(EXEC) watchexec --force-poll 500 -r -e kt -- ./amper run --module app-api

run-worker: ## Run the Worker application using Amper
	$(EXEC) ./amper run --module app-worker

watch-worker: ## Run the Worker application with live reload using watchexec and Amper
	$(EXEC) watchexec --force-poll 500 -r -e kt -- ./amper run --module app-worker

# Migrations
build-migrations: ## Build the migration Docker image
	docker compose build migrate

migrate: build-migrations ## Run database migrations (up)
	docker compose run --rm migrate -path /migrations -database "$(MIGRATE_DB_URL)" up

migrate-up: migrate ## Alias for migrate

migrate-down: build-migrations ## Rollback last migration
	docker compose run --rm migrate -path /migrations -database "$(MIGRATE_DB_URL)" down

# LocalStack commands
localstack-up: ## Start LocalStack and wait for it to be ready
	@echo "Starting LocalStack..."
	docker compose up -d localstack
	@echo "Waiting for LocalStack to be ready..."
	@timeout=60; \
	while [ $$timeout -gt 0 ]; do \
		if docker compose exec -T localstack curl -f http://localhost:4566/_localstack/health >/dev/null 2>&1; then \
			echo "LocalStack is ready!"; \
			exit 0; \
		fi; \
		sleep 2; \
		timeout=$$((timeout - 2)); \
	done; \
	echo "Warning: LocalStack may not be fully ready yet"

localstack-setup: localstack-up ## Set up LocalStack resources (SNS topics and SQS queues)
	@echo "Setting up LocalStack resources (SNS topics and SQS queues)..."
	@$(EXEC) bash resources/scripts/localstack_setup.sh

localstack-down: ## Stop LocalStack
	@echo "Stopping LocalStack..."
	docker compose stop localstack

localstack-logs: ## Show LocalStack logs
	docker compose logs -f localstack
