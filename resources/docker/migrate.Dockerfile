FROM migrate/migrate:v4.17.0

# Copy migrations into the image
COPY resources/db/migrations /migrations

# Set working directory
WORKDIR /migrations

# Default command (can be overridden)
CMD ["-path", "/migrations", "-database", "postgres://postgres:postgres@postgres:5432/app?sslmode=disable", "up"]

