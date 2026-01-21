# Variables
SERVICE_NAME = workspace
EXEC = docker compose exec -it $(SERVICE_NAME)

.PHONY: help workspace-build workspace-up workspace-down workspace-shell shell lint fix test build-all refresh gradle-watch gradle-clean run-api run-worker migrate migrate-info migrate-repair localstack-up localstack-setup localstack-down localstack-logs build-docker

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
lint: ## Run checks
	$(EXEC) yamlfmt -lint .
	$(EXEC) ktlint --relative "**/*.kt" "**/*.kts" "!**/bin/**" "!**/build/**"
	$(EXEC) detekt --input modules --excludes "**/bin/**,**/build/**" --config gradle-config/detekt.yml --build-upon-default-config --parallel --base-path .

fix: ## Automatically fix formatting
	$(EXEC) yamlfmt .
	$(EXEC) ktlint -F --relative "**/*.kt" "**/*.kts" "!**/bin/**" "!**/build/**"

# Testing
test: ## Run tests (unit + integration)
	./gradlew test

# Build
build-all: ## Run a full build (compilation + check + test)
	./gradlew build

refresh: ## Force Gradle to refresh dependencies
	./gradlew build --refresh-dependencies

gradle-watch: ## Run Gradle in continuous mode to recompile on changes
	./gradlew -t classes

gradle-clean: ## Clean Gradle build artifacts
	./gradlew clean

# Run dev servers
run-api: ## Run the API application
	./gradlew -t clean :modules:app-api:run

run-worker: ## Run the Worker application with continuous build
	./gradlew -t :modules:app-worker:run

# Migrations
migrate: ## Run database migrations (apply all pending)
	./gradlew :modules:infrastructure:flywayMigrate

migrate-info: ## Show migration status
	./gradlew :modules:infrastructure:flywayInfo

migrate-repair: ## Repair Flyway schema history table (use if migrations are corrupted)
	./gradlew :modules:infrastructure:flywayRepair

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

# Docker build commands
build-docker: ## Build Docker image from pre-built artifacts (build artifacts first with: make build-all)
	@echo "Building Docker image from pre-built artifacts..."
	@if [ ! -d "modules/app-api/build/install/app-api" ] || [ ! -d "modules/app-worker/build/install/app-worker" ]; then \
		echo "Error: Artifacts not found. Please run 'make build-all' first."; \
		exit 1; \
	fi
	docker build -f resources/docker/app-runtime.Dockerfile -t kotlin-ktor-postgres-template-prod:latest .

build-all: ## Build all artifacts (for Docker image)
	./gradlew :modules:app-api:installDist :modules:app-worker:installDist :modules:presentation:buildOpenApi
