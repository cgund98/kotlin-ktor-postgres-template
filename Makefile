# The name of your workspace service in docker-compose.yml
SERVICE_NAME = workspace
# Standard exec command - ensures a TTY for pretty colors
EXEC = docker compose exec -it $(SERVICE_NAME)

.PHONY: lint format test build-all shell help workspace-build workspace-up workspace-shell gradle-watch run-api run-worker

help: ## Show this help
	@echo "See DEVELOPMENT.md for more details."
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

workspace-build: ## Build the docker workspace image
	docker compose build workspace

workspace-up: ## Start the docker workspace in the background
	docker compose up -d workspace

workspace-shell: ## Open a shell in the workspace container (alias for shell)
	$(EXEC) /bin/bash

lint: ## Run checkstyle/ktlint checks
	$(EXEC) ./gradlew ktlintCheck detekt

fix: ## Automatically fix Kotlin formatting
	$(EXEC) ./gradlew ktlintFormat
	$(EXEC) ./gradlew detekt --auto-correct

test: ## Run tests (unit + integration)
	$(EXEC) ./gradlew test

build-all: ## Run a full build (compilation + check + test)
	$(EXEC) ./gradlew build

shell: ## Jump into the running container
	$(EXEC) /bin/bash

refresh: ## Force Gradle to refresh dependencies
	$(EXEC) ./gradlew build --refresh-dependencies

gradle-watch: ## Run Gradle in continuous mode to recompile on changes
	$(EXEC) ./gradlew -t classes

run-api: ## Run the API application
	$(EXEC) ./gradlew :modules:app-api:run

run-worker: ## Run the Worker application with continuous build
	$(EXEC) ./gradlew -t :modules:app-worker:run
