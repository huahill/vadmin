# Getting Started

[简体中文](../../zh-CN/user/getting-started.md) | English

Add one dependency to an existing Spring Boot application and start it. You
get sign-in with a bootstrap administrator, permission-filtered navigation,
the default shell and theme, the Users, Roles, Permissions, and Audit
administration module, `zh-CN` and `en-US` locales, light/dark color schemes,
and a choice of visual languages. You do not need to build a layout, theme,
or system pages — VAdmin provides them.

## Prerequisites

- A Spring Boot application built with Java 25.
- Vaadin Flow 25.x on the classpath.

If your application does not yet use Vaadin Flow, the starter brings it in
transitively. VAdmin does not include Hilla, React, or TypeScript.

The default local IAM (Users, Roles, Permissions, Audit) needs a PostgreSQL
database — VAdmin manages its own schema through Flyway. If your application
already uses PostgreSQL, point VAdmin at it (step 2). If you bring your own
identity and authorization, disable local IAM with
`vadmin.local-iam.enabled=false` and no database is required.

## 1. Add The Dependency

```xml
<dependency>
  <groupId>io.github.huahill</groupId>
  <artifactId>vadmin-spring-boot-starter</artifactId>
  <version>${vadmin.version}</version>
</dependency>
```

If your application declares `@EnableVaadin` explicitly, add the VAdmin root
package so Vaadin discovers the default shell and system views. This is
Vaadin's route-discovery requirement, not custom shell composition:

```java
@EnableVaadin({"com.example.inventory", "io.github.huahill.vadmin"})
@SpringBootApplication
public class InventoryApplication {
}
```

For `spring-boot:run` development, also add Vaadin's optional development
server directly. It is intentionally not transitive and is excluded from the
production artifact:

```xml
<dependency>
  <groupId>com.vaadin</groupId>
  <artifactId>vaadin-dev</artifactId>
  <version>${vaadin.version}</version>
  <optional>true</optional>
</dependency>
```

## 2. Point At Your PostgreSQL

VAdmin works with a standard Spring Boot datasource. If your application
already configures PostgreSQL, no extra database setup is needed — VAdmin
manages its own tables through Flyway, alongside any migrations you already
have:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory
    username: inventory
    password: change-me
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration
```

Keep your migrations in a location separate from VAdmin's own and never
rewrite a migration that has reached any environment. See the
[Deployment](deployment.md) guide for migration operations.

## 3. Start The Application

On an empty database, set `APP_BOOTSTRAP_PASSWORD` once; the initial `admin`
account receives that password. Changing it later does not reset an existing
account.

```bash
APP_BOOTSTRAP_PASSWORD='replace-this-secret' ./mvnw -B -ntp spring-boot:run
```

Open `http://localhost:8080` and sign in as `admin`. That is the complete
baseline — no shell, theme, or system pages to build.

Optionally set the product name shown in the shell without replacing the
layout:

```yaml
vadmin:
  brand:
    name: Inventory Operations
```

Every other property has a working default; see the
[Configuration Reference](configuration.md) when you need to tune something.

## Try The Reference Application

The repository includes a thin consumer application that demonstrates the
adoption path end-to-end. It is not part of the starter — it proves that a
normal application can depend on the starter and contribute only its own
functionality.

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

Open `http://localhost:8080` and sign in with `admin` and the value of
`APP_BOOTSTRAP_PASSWORD` in `.env`.

## Next Steps

- Add business pages with the [Modules](modules.md) guide.
- Review the [Security](security.md) model before production.
- Deploy with the [Deployment](deployment.md) guide.
- Move between versions with the [Upgrade](upgrade.md) guide.
- Change the look with the [Appearance](appearance.md) guide.

Only a deliberate full shell replacement needs a consumer `AdminHostLayout`
and `AppShellConfigurator`.
