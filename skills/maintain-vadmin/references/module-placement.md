# Module Placement

Read this before adding or moving a class, module, adapter, or document. If it disagrees with `docs/en/dev/architecture.md` or an architecture test, those win; update this file in the same change.

## Ownership

| Change | Location |
| --- | --- |
| Identity, authorization, audit, error, file, or navigation contract | `vadmin-contracts`, under `io.github.huahill.vadmin.contracts` |
| Framework-free RBAC use cases and ports | `vadmin-platform`, under `io.github.huahill.vadmin.platform` |
| Spring-free Flow patterns, route guards, module metadata, or the consumer anchor type | `vadmin-flow`, under `io.github.huahill.vadmin.flow` |
| Repeated administration page workflow | `vadmin-flow` package `io.github.huahill.vadmin.flow.patterns` |
| Local authentication or the optional OIDC adapter | `vadmin-spring/vadmin-spring-security` |
| JPA and Flyway RBAC or audit storage | `vadmin-spring/vadmin-spring-jpa` |
| Opt-in JDBC IAM for a host-migrated MySQL or PostgreSQL schema | `vadmin-spring/vadmin-spring-jdbc` |
| Correlation ID and Problem Details configuration | `vadmin-spring/vadmin-spring-boot` |
| Module assembly, dynamic routes, composite translations, or locale preference | `vadmin-spring/vadmin-spring-flow` |
| Default shell, system administration, default `AdminModule`, branding, or product translations | `vadmin-spring/vadmin` |
| Consumer dependency aggregation | `vadmin-spring/vadmin-spring-boot-starter` |
| Consumer version alignment for VAdmin modules and the Vaadin line | `vadmin-bom` |
| Proof that an ordinary application can adopt the starter | `vadmin-reference-app` |

## Dependency Direction

- `vadmin-contracts`, `vadmin-platform`, and `vadmin-flow` do not depend on Spring, Jakarta Persistence, or Flyway. `vadmin-reference-app` `ArchitectureTest` enforces this.
- Classes in `io.github.huahill.vadmin.administration` consume the platform storage contract. They do not depend on `springjpa`, `springjdbc`, Spring JDBC, or Jakarta Persistence. `AdministrationArchitectureTest` enforces this.
- Only the default administration packages, `vadmin-spring-flow`, and the reference app depend on `vadmin-spring-flow`.
- The reference app does not acquire classes whose names claim ownership of the default shell, theme, appearance, system administration, or module configuration.
- Put `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` in the capability module that owns the behavior. Current owners are `vadmin`, `vadmin-spring-jpa`, `vadmin-spring-jdbc`, and `vadmin-spring-flow`. Spring Security and Boot error configuration use explicit `@Configuration` in their own modules. The starter owns neither imports nor Java.

## Routes And Anchors

- A product entry point that Flow must visit directly may use `@Route` or `@Layout`. The current cases are login, home, access denied, system error, and `DefaultMainLayout`.
- Built-in dynamic system views do not declare their own `@Route`. `DefaultMainLayout` carries one `@Uses` for each of them.
- `AdminModuleViewAnchor` is the entry point for a consumer's dynamic business views. Keep `serviceInit` empty and final. Do not register an anchor subclass as a Spring bean or through `META-INF/services`.
- Do not put `@Route` on a consumer view, and do not put that consumer's `@Uses` on a Spring Boot application class.
