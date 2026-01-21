# Database Migrations

This directory contains database migrations managed by [Flyway](https://flywaydb.org/).

## Prerequisites

Flyway is integrated into the Gradle build, so no separate installation is required. Migrations run automatically via Gradle tasks.

## Migration Files

Migrations are stored in the `migrations/` directory and follow Flyway's naming convention:
- `V{version}__{description}.sql` - Versioned migrations (e.g., `V1__initial_schema.sql`)
- `R__{description}.sql` - Repeatable migrations (run every time checksums change)

**Important:** Flyway uses versioned migrations that only run once. Unlike golang-migrate, Flyway does not use separate `.up.sql` and `.down.sql` files. Rollbacks are handled by creating new migrations that reverse changes.

## Usage

### Using Makefile (Recommended)

```bash
# Apply all pending migrations
make migrate

# Show migration status and history
make migrate-info

# Repair Flyway schema history table (use if migrations are corrupted)
make migrate-repair
```

### Using Gradle Directly

```bash
# Apply all pending migrations
./gradlew :modules:infrastructure:flywayMigrate

# Show migration status
./gradlew :modules:infrastructure:flywayInfo

# Repair schema history table
./gradlew :modules:infrastructure:flywayRepair

# Validate migrations (check for changes)
./gradlew :modules:infrastructure:flywayValidate

# Clean database (removes all objects - use with caution!)
./gradlew :modules:infrastructure:flywayClean
```

### Environment Variables

Flyway uses the following environment variables (with defaults for local development):
- `POSTGRES_URL` (default: `jdbc:postgresql://localhost:5432/app`)
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `postgres`)

These can be set in `.env.local` or `.env` files, or as environment variables.

## Creating Migrations

When creating a new migration:

1. **Use descriptive names** that explain what the migration does
2. **Follow the naming convention**: `V{version}__{description}.sql`
   - Version must be unique and incrementing (e.g., V1, V2, V3)
   - Description should be in lowercase with underscores (e.g., `add_user_preferences`)
3. **Test migrations** before committing
4. **Use transactions** where possible (PostgreSQL supports DDL in transactions)
5. **Use IF EXISTS/IF NOT EXISTS** to make migrations idempotent where appropriate

Example:
```sql
-- File: V2__add_user_preferences.sql
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(20) NOT NULL DEFAULT 'light',
    notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Migration Best Practices

1. **Never modify existing migrations** - Once a migration has been applied to a database, it should never be changed. Create new migrations instead.
2. **Keep migrations small** - One logical change per migration makes it easier to understand and debug.
3. **Test rollbacks** - While Flyway doesn't use `.down.sql` files, you should test that you can create new migrations to reverse changes if needed.
4. **Use IF EXISTS/IF NOT EXISTS** - Makes migrations more resilient to partial failures.
5. **Document complex migrations** - Add comments explaining why a migration is needed.
6. **Version carefully** - Use sequential version numbers. Gaps are allowed, but avoid going backwards.

## Schema Versioning

Flyway automatically tracks the current schema version in a `flyway_schema_history` table. This table is created automatically on first migration run and contains:
- Installed version numbers
- Checksums of migration files
- Execution timestamps
- Success/failure status

## Rollbacks

Flyway does not support automatic rollbacks like golang-migrate's `.down.sql` files. To rollback a migration:

1. **Create a new migration** that reverses the changes
2. **Use Flyway's repair** if the schema history is corrupted
3. **Manually fix** the database if needed (not recommended for production)

Example rollback migration:
```sql
-- File: V3__remove_user_preferences.sql
DROP TABLE IF EXISTS user_preferences;
```

## Differences from golang-migrate

- **No separate up/down files**: Flyway uses single SQL files per migration
- **Versioned migrations**: Migrations run once and are tracked by version number
- **No automatic rollback**: Rollbacks require creating new migrations
- **Gradle integration**: Migrations run via Gradle tasks, not a separate CLI tool
- **Schema history table**: Uses `flyway_schema_history` instead of `schema_migrations`
