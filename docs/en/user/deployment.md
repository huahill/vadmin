# Deployment

[简体中文](../../zh-CN/user/deployment.md) | English

VAdmin is a regular Spring Boot library — deploy your application the way you
already do. This guide covers the VAdmin-specific concerns to check before
production.

## Database

VAdmin manages its own tables through Flyway and validates them with
`ddl-auto: validate`. Make sure your production PostgreSQL is reachable and
that Flyway can run before Hibernate initializes.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.example:5432/vadmin
    username: vadmin
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

Never rewrite a Flyway migration that has reached any environment. Fix schema
or data issues by adding the next versioned migration. Validate migrations
and rollback plans against a production data copy before upgrading.

## Secrets

Provide all sensitive values through the runtime platform's secret mechanism,
not in image layers or Compose files:

- `DATABASE_PASSWORD` — your PostgreSQL credentials.
- `APP_BOOTSTRAP_PASSWORD` — only required on the first start of an empty
  database to create the initial `admin` account.

Changing `APP_BOOTSTRAP_PASSWORD` after the initial start has no effect on
existing accounts. See [Security](security.md) for the bootstrap account
behavior.

## Container

If you containerize your application, use a JRE base image and run as a
non-root user. VAdmin itself does not require any special container
configuration — it is a standard Spring Boot fat JAR.

For reference, the VAdmin repository `Dockerfile` builds with a JDK stage and
runs the final image on a JRE as UID `10001` with `SPRING_PROFILES_ACTIVE=prod`.
You can use it as a template for your own Dockerfile.

## Backup And Recovery

VAdmin persists access-control and audit data in PostgreSQL. A restore must
include the database and its Flyway schema history. If your business modules
store files or other external data, document and rehearse their backup and
recovery separately before switching production.
