# Database Migrations

This directory contains database migrations managed by [golang-migrate](https://github.com/golang-migrate/migrate).

## Prerequisites

Install golang-migrate CLI:

**macOS:**
```bash
brew install golang-migrate
```

**Linux:**
```bash
# Download from releases page or use package manager
# See: https://github.com/golang-migrate/migrate/blob/master/database/postgres/TUTORIAL.md
```

**Verify installation:**
```bash
migrate -version
```

## Migration Files

Migrations are stored in the `migrations/` directory and follow the naming convention:
- `{version}_{name}.up.sql` - Migration to apply
- `{version}_{name}.down.sql` - Migration to rollback

## Usage

### Using Makefile (Recommended)

```bash
# Apply all pending migrations
make migrate

# Rollback the last migration
make migrate-down

# Show current migration version
make migrate-version

# Create a new migration
make migrate-create NAME=add_user_preferences

# Force set migration version (use with caution)
make migrate-force VERSION=1

# Run migrations using Docker
make migrate-docker
```

### Using Docker Compose

The migrations are packaged in a Docker image (`app-migrations:latest`) that contains all migration files.

```bash
# Build the migrations image (includes all migration files)
make build-migrations

# Run migrations using Docker Compose
make migrate-docker
# or
docker-compose run --rm migrate

# Run specific migration commands
make migrate-docker-up      # Apply migrations
make migrate-docker-down    # Rollback migrations
make migrate-docker-version # Show current version
```

### Using the Script Directly

```bash
# Apply migrations
./resources/scripts/migrate.sh up

# Rollback last migration
./resources/scripts/migrate.sh down

# Show version
./resources/scripts/migrate.sh version

# Create new migration
./resources/scripts/migrate.sh create my_migration_name

# Migrate to specific version
./resources/scripts/migrate.sh goto 2

# Force version (use with caution)
./resources/scripts/migrate.sh force 1
```

## Environment Variables

The migration script uses the following environment variables (with defaults):

- `POSTGRES_HOST` (default: `localhost`)
- `POSTGRES_PORT` (default: `5432`)
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `postgres`)
- `POSTGRES_DB` (default: `app`)
- `MIGRATIONS_PATH` (default: `./resources/db/migrations`)

## Creating Migrations

When creating a new migration:

1. Use descriptive names that explain what the migration does
2. Always provide both `.up.sql` and `.down.sql` files
3. Test both up and down migrations
4. Use transactions where possible (golang-migrate runs each migration in a transaction by default)

Example:
```bash
make migrate-create NAME=add_user_preferences
```

This creates:
- `resources/db/migrations/000002_add_user_preferences.up.sql`
- `resources/db/migrations/000002_add_user_preferences.down.sql`

## Migration Best Practices

1. **Never modify existing migrations** - Create new migrations instead
2. **Always test rollbacks** - Ensure `.down.sql` properly reverses changes
3. **Use IF EXISTS/IF NOT EXISTS** - Makes migrations idempotent
4. **Keep migrations small** - One logical change per migration
5. **Document complex migrations** - Add comments explaining why

## Schema Versioning

The migration tool automatically tracks the current schema version in a `schema_migrations` table. This table is created automatically on first migration run.

