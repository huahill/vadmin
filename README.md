# VAdmin

VAdmin is published as `io.github.youngledo:vadmin-spring-boot-starter`.

VAdmin is a Java-first administration baseline for Vaadin Flow. Its reusable
core — identity and permission contracts, RBAC use cases, and the shared
shell patterns — is framework-neutral, and runtime-specific behavior lives in
adapter modules, so runtimes beyond Spring Boot (such as Quarkus, Helidon, or
Jakarta EE) remain possible future adapters. Spring Boot is the only supported
runtime today: add `vadmin-spring-boot-starter` to receive local login,
permission-filtered navigation, the default shell and theme, Users, Roles,
Permissions, Audit, locale selection, and appearance controls. It is not a
collection of showcase pages or a runtime plugin platform.

The first release uses Java 25, Maven 4.0.0-rc-6, Spring Boot 4.1.1, Vaadin
Flow 25.3.0, PostgreSQL, and Flyway. Flow is the only UI model in scope; Hilla,
React, and TypeScript are not included.

VAdmin owns the baseline experience through its default administration module. Consumers add business capabilities
as `AdminModule` beans and Flow view beans; they do not create a shell, theme,
or system-administration module for normal adoption. See the English
[Modules](docs/en/user/modules.md) guide for the module, translation, and
production-anchor contract.

## Quick Start

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

Open `http://localhost:8080` and sign in with `admin` and the value of
`APP_BOOTSTRAP_PASSWORD` in `.env`.

## Documentation

**User guides**

| Guide | English | 简体中文 |
| --- | --- | --- |
| Getting Started | [Read](docs/en/user/getting-started.md) | [阅读](docs/zh-CN/user/getting-started.md) |
| Modules | [Read](docs/en/user/modules.md) | [阅读](docs/zh-CN/user/modules.md) |
| Configuration | [Read](docs/en/user/configuration.md) | [阅读](docs/zh-CN/user/configuration.md) |
| Appearance | [Read](docs/en/user/appearance.md) | [阅读](docs/zh-CN/user/appearance.md) |
| Security | [Read](docs/en/user/security.md) | [阅读](docs/zh-CN/user/security.md) |
| Deployment | [Read](docs/en/user/deployment.md) | [阅读](docs/zh-CN/user/deployment.md) |
| Upgrade | [Read](docs/en/user/upgrade.md) | [阅读](docs/zh-CN/user/upgrade.md) |

**Developer guides**

| Guide | English | 简体中文 |
| --- | --- | --- |
| Architecture | [Read](docs/en/dev/architecture.md) | [阅读](docs/zh-CN/dev/architecture.md) |
| Theme Policy | [Read](docs/en/dev/theme-tokens.md) | [阅读](docs/zh-CN/dev/theme-tokens.md) |
| Release Guide | [Read](docs/en/dev/release-guide.md) | [阅读](docs/zh-CN/dev/release-guide.md) |
| Requirements | [Read](docs/dev/requirements.md) | -- |
| Contributing | [Read](docs/dev/contributing.md) | [阅读](docs/dev/contributing.md) |

## Verification

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
docker compose --env-file .env.example config
docker build -t vadmin:local .
```

Vaadin is a trademark of Vaadin Ltd. This project is not affiliated with,
sponsored by, or endorsed by Vaadin Ltd.
