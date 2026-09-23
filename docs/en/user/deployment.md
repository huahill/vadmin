# Deployment And Operations

VAdmin publication coordinate: `io.github.youngledo:vadmin-spring-boot-starter`.

[简体中文](../../zh-CN/user/deployment.md) | English

## Container Image

The `Dockerfile` runs the production build in a Java 25 JDK stage; the final
stage uses a Java 25 JRE, copies only the executable JAR, and runs as a
non-root user with UID `10001`. It listens on 8080 by default and sets
`SPRING_PROFILES_ACTIVE=prod`.

```bash
docker build -t vadmin:local .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://db.example:5432/vadmin \
  -e DATABASE_USERNAME=vadmin \
  -e DATABASE_PASSWORD='replace-me' \
  -e APP_BOOTSTRAP_PASSWORD='replace-on-first-start' \
  vadmin:local
```

Production deployments should provide the database password, bootstrap
password, and any object-storage credentials through the platform's secret
mechanism. Never bake these values into images, Compose files, or image tags.

## Database And Migrations

Flyway applies versioned SQL migrations before the JPA adapter starts, and
JPA then validates the schema with `ddl-auto: validate`. A migration that has
reached a deployed environment must never be rewritten; fix schema or data
changes by adding the next versioned migration. Validate migrations and
rollback plans against a production data copy before upgrading.

The default PostgreSQL URL, username, and password come from `DATABASE_URL`,
`DATABASE_USERNAME`, and `DATABASE_PASSWORD`. Keep database connectivity
inside a trusted network, and let the runtime platform enforce TLS, access
control, log collection, and health-check policies.

## Backup And Recovery

The VAdmin baseline persists only access-control and audit data, so a restore
must include the PostgreSQL database and its Flyway schema history. If a
business module integrates `FileStorage` or other external persistence, the
consumer must document its consistency boundaries, backup scope, and recovery
order, and rehearse the restore in an isolated environment before switching
production.

## Compose Development Stack

`docker-compose.yml` uses PostgreSQL 18 and a named `postgres-data` volume.
The application service starts only after the PostgreSQL `pg_isready` health
check passes. Inspect the rendered configuration:

```bash
docker compose --env-file .env.example config
```

`.env.example` is a local sample, not a production secret file. Create a
protected `.env` or use platform secret injection for deployments, and never
commit it to Git.
