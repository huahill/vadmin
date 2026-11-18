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

## Why VAdmin?

Internal tools often need the same foundation: authentication, navigation,
access control, user administration, audit trails, and consistent ways to
browse and edit data. Rebuilding these for every application takes time away
from the business workflows your team needs to deliver.

VAdmin provides that foundation as Maven dependencies. Your application owns
its domain logic and business views; VAdmin supplies the shared administration
experience. Business pages use Vaadin Flow's server-side Java programming
model and can call your Spring services directly. Vaadin's tooling builds the
frontend resources for production.

## What You Get

| Capability | Included |
| --- | --- |
| Authentication and access control | Local login, role-based permissions, permission-filtered navigation, and protected page routes. |
| System administration | Built-in Users, Roles, Permissions, and Audit pages. |
| Application shell | Responsive navigation, a user menu, and a configurable product name. |
| Common page patterns | Page headers, toolbars, data workspaces, detail dialogs, and loading, empty, and failure states. |
| Business modules | Register Java views, navigation, permissions, and translations together through `AdminModule`. |
| Language and appearance | English and Simplified Chinese, the Vaadin Aura theme, and system, light, and dark color schemes. |
| Operational diagnostics | Request correlation IDs and RFC 9457 Problem Details for HTTP errors. |

Use VAdmin for internal tools, operations consoles, and business administration
applications where your team already works with Java and Spring Boot.

## Your Application and VAdmin

| Your application contributes | VAdmin provides |
| --- | --- |
| Domain logic, Spring services, and business commands | Shared identity, authorization, and audit contracts. |
| Java business views built with the shared Flow patterns | The application shell, navigation, and system administration pages. |
| Module metadata: pages, permissions, and translations | Startup validation, route registration, permission-filtered navigation, and combined translations. |
| Deployment configuration and authentication integration | Spring Boot adapters, a starter, and a BOM for dependency alignment. |

Normal adoption starts with the default product shell. Configure its display
name and contribute business modules as your application grows.

## Add VAdmin to Your Application

Import `vadmin-bom` and add `vadmin-spring-boot-starter` to your Spring Boot
application. The starter provides the default shell and system administration;
you contribute your business pages as Java views and `AdminModule` beans.

Module metadata connects a business page to its route, navigation group,
permission, and translations. For example, this bean in a module configuration
declares an inventory page:

```java
@Bean
AdminModule inventoryModule() {
    var read = PermissionCode.of("inventory:item:read");
    return AdminModule.of("inventory",
        List.of(new AdminNavigationGroup("inventory", "inventory.navigation", 200)),
        List.of(new AdminPage("inventory.items", "inventory", "inventory.items.title",
            "inventory.items.intent", "cube", 100, "inventory/items", read,
            InventoryView.class)),
        Set.of(read),
        List.of(new AdminMessageBundle("inventory", "i18n.inventory")));
}
```

Register `InventoryView` as a prototype-scoped Spring bean. VAdmin registers
its route from the module metadata; the view does not declare `@Route`.
Provide the module's translation resources and an anchor for the production
frontend build:

```java
@Uses(InventoryView.class)
public final class InventoryFrontendAnchor extends AdminModuleViewAnchor {
}
```

The module follows three stages:

1. **Declare** pages, routes, permissions, navigation groups, and message bundles.
2. **Validate at startup**: reject duplicate module IDs, page IDs, routes,
   permission codes, and bundle names; check metadata references and required
   default translations.
3. **Assemble** the permission catalog, routes, navigation, and combined
   translation provider from the validated declarations.

This catches configuration mistakes before someone encounters a broken page.
See [Getting Started](docs/en/user/getting-started.md) for dependencies and
database setup, and [Modules](docs/en/user/modules.md) for the complete example,
imports, view registration, translations, and production anchor requirements.

## Architecture

VAdmin separates the administration product, runtime integration, and reusable
contracts and use cases. The default shell belongs to VAdmin; the reference
application demonstrates how a normal application consumes it.

| Area | Modules and responsibilities |
| --- | --- |
| Consumer integration | `vadmin-spring-boot-starter` aggregates default capabilities; `vadmin-bom` aligns VAdmin and Vaadin dependencies. |
| Administration product | `vadmin` owns the default shell, global utilities, and system administration pages. |
| Spring Boot integration | `vadmin-spring-flow` assembles modules; `vadmin-spring-security` integrates authentication; `vadmin-spring-boot` handles request correlation and HTTP errors. |
| Persistence | `vadmin-spring-jpa` provides default IAM and audit storage; `vadmin-spring-jdbc` is an opt-in IAM adapter for host-managed schemas. |
| Reusable core | `vadmin-contracts` defines contracts; `vadmin-platform` implements RBAC use cases and ports; `vadmin-flow` provides Vaadin Flow patterns, navigation metadata, and view guards. |

The contracts, platform, and Flow modules do not depend on Spring or Jakarta
Persistence. The Flow module uses Vaadin Flow. Architecture tests enforce
these boundaries. Spring Boot is the currently supported runtime.

Read the [architecture guide](docs/en/dev/architecture.md) for dependency
direction and extension points.

## Access Control

Business modules declare permission codes such as `inventory:item:read` and
`inventory:item:update`. These declarations form the permission catalog;
administrators assign permissions to roles and roles to users.

VAdmin filters navigation and checks access to registered page routes.
`PermissionProtectedView` also offers a permission check before entering a
view. Business services and commands must enforce authorization before
reading protected data or changing state; hiding navigation or a button does
not replace that check.

See [Security](docs/en/user/security.md) for local authentication, optional
OIDC integration, authorization changes, and deployment considerations.

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
The reference application uses the same starter and module integration path
as a consuming application.

## Technology

VAdmin currently supports Java 25, Spring Boot 4.1, and Vaadin Flow 25.3.
The default interface uses Vaadin Aura. The reference application uses
PostgreSQL and Flyway and includes a Docker deployment example.

See the [root POM](pom.xml) for exact dependency versions.

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
