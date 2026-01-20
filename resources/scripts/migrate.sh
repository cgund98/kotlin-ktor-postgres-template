#!/bin/bash
# Database migration script using golang-migrate
# Requires golang-migrate CLI to be installed: https://github.com/golang-migrate/migrate

set -e

# Default values
MIGRATE_ACTION="${1:-up}"
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-app}"
MIGRATIONS_PATH="${MIGRATIONS_PATH:-./resources/db/migrations}"

# Build database URL
DATABASE_URL="postgres://${POSTGRES_USER}:${POSTGRES_PASSWORD}@${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}?sslmode=disable"

# Check if migrate command exists
if ! command -v migrate &> /dev/null; then
    echo "Error: golang-migrate CLI not found"
    echo "Please install it from: https://github.com/golang-migrate/migrate"
    echo ""
    echo "macOS: brew install golang-migrate"
    echo "Linux: See https://github.com/golang-migrate/migrate/blob/master/database/postgres/TUTORIAL.md"
    exit 1
fi

echo "Running database migrations..."
echo "Action: ${MIGRATE_ACTION}"
echo "Database: ${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
echo "Migrations path: ${MIGRATIONS_PATH}"
echo ""

# Run migration based on action
case "${MIGRATE_ACTION}" in
    up)
        migrate -path "${MIGRATIONS_PATH}" -database "${DATABASE_URL}" up
        ;;
    down)
        migrate -path "${MIGRATIONS_PATH}" -database "${DATABASE_URL}" down
        ;;
    force)
        VERSION="${2}"
        if [ -z "${VERSION}" ]; then
            echo "Error: force requires a version number"
            echo "Usage: $0 force <version>"
            exit 1
        fi
        migrate -path "${MIGRATIONS_PATH}" -database "${DATABASE_URL}" force "${VERSION}"
        ;;
    version)
        migrate -path "${MIGRATIONS_PATH}" -database "${DATABASE_URL}" version
        ;;
    create)
        NAME="${2}"
        if [ -z "${NAME}" ]; then
            echo "Error: create requires a migration name"
            echo "Usage: $0 create <name>"
            exit 1
        fi
        migrate create -ext sql -dir "${MIGRATIONS_PATH}" -seq "${NAME}"
        ;;
    goto)
        VERSION="${2}"
        if [ -z "${VERSION}" ]; then
            echo "Error: goto requires a version number"
            echo "Usage: $0 goto <version>"
            exit 1
        fi
        migrate -path "${MIGRATIONS_PATH}" -database "${DATABASE_URL}" goto "${VERSION}"
        ;;
    *)
        echo "Usage: $0 {up|down|force|version|create|goto} [args]"
        echo ""
        echo "Commands:"
        echo "  up        - Apply all available migrations"
        echo "  down      - Rollback the last migration"
        echo "  force     - Force set migration version (use with caution)"
        echo "  version   - Show current migration version"
        echo "  create    - Create a new migration file"
        echo "  goto      - Migrate to a specific version"
        exit 1
        ;;
esac

echo ""
echo "Migration completed successfully."