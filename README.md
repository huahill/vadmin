# VAdmin

English | [简体中文](README.zh-CN.md)

**Build your admin application in Java.**

VAdmin is a batteries-included administration framework for Spring Boot and
[Vaadin Flow](https://vaadin.com/docs/latest/flow). It brings authentication,
role-based access control, user management, audit logs, and a responsive
application shell to your Java application.

Start with a working admin interface, then add your business features using
Java views and Spring services. VAdmin provides the shared navigation and
page patterns, so your team can focus on the workflows that matter to your
business.

## What You Get

| Capability | Included |
| --- | --- |
| Authentication and access control | Local login, role-based permissions, permission-filtered navigation, and protected page routes. |
| System administration | Built-in Users, Roles, Permissions, and Audit pages. |
| Application shell | Responsive navigation, a user menu, and a configurable product name. |
| Common page patterns | Page headers, toolbars, data workspaces, detail dialogs, and loading, empty, and failure states. |
| Business modules | Register Java views, navigation, permissions, and translations together through `AdminModule`. |
| Language and appearance | English and Simplified Chinese, the Vaadin Aura theme, and system, light, and dark color schemes. |

Use VAdmin for internal tools, operations consoles, and business administration
applications where your team already works with Java and Spring Boot.

## Try It Locally

The repository includes a reference application that demonstrates adopting
VAdmin in a Spring Boot application. With Docker installed, run these commands
from your checkout:

```bash
cp .env.example .env
```

Set `APP_BOOTSTRAP_PASSWORD` in `.env`, then start the application:

```bash
docker compose --env-file .env up --build
```

Open `http://localhost:8080` and sign in with `admin` and that password.

## Add VAdmin to Your Application

Import `vadmin-bom` and add `vadmin-spring-boot-starter` to your Spring Boot
application. The starter provides the default shell and system administration;
you contribute your business pages as Java views and `AdminModule` beans.

1. Follow [Getting Started](docs/en/user/getting-started.md) for dependencies,
   database configuration, and the first login.
2. Follow [Modules](docs/en/user/modules.md) to add business pages, navigation,
   permissions, and translations.
3. Use the shared Flow page patterns to build consistent administration
   workflows.

## Technology

VAdmin currently supports Java 25, Spring Boot 4.1, and Vaadin Flow 25.3.
The default interface uses Vaadin Aura. The reference application uses
PostgreSQL and Flyway and includes a Docker deployment example.

See the [root POM](pom.xml) for exact dependency versions and the
[architecture guide](docs/en/dev/architecture.md) for module boundaries and
extension points.

## Documentation

**User guides**

| Guide | Link |
| --- | --- |
| Getting Started | [Read](docs/en/user/getting-started.md) |
| Modules | [Read](docs/en/user/modules.md) |
| Configuration | [Read](docs/en/user/configuration.md) |
| Appearance | [Read](docs/en/user/appearance.md) |
| Security | [Read](docs/en/user/security.md) |
| Deployment | [Read](docs/en/user/deployment.md) |

**Developer guides**

| Guide | Link |
| --- | --- |
| Architecture | [Read](docs/en/dev/architecture.md) |
| Theme Policy | [Read](docs/en/dev/theme-tokens.md) |
| Release Guide | [Read](docs/en/dev/release-guide.md) |
| Requirements | [Read](docs/dev/requirements.md) |
| Contributing | [Read](docs/dev/contributing.md) |

## Development and Verification

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
docker compose --env-file .env.example config
docker build -t vadmin:local .
```

## License

VAdmin is licensed under the [Apache License 2.0](LICENSE).

Vaadin is a trademark of Vaadin Ltd. This project is not affiliated with,
sponsored by, or endorsed by Vaadin Ltd.
