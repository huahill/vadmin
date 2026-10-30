# Starter, BOM, And Anchors

## Starter

`vadmin-spring-boot-starter` is the supported adoption artifact. It aggregates `vadmin`, `vadmin-spring-security`, `vadmin-spring-jpa`, `vadmin-spring-boot`, and `vadmin-spring-flow`, plus `spring-boot-starter`, optional `vaadin-dev`, and runtime-scoped PostgreSQL.

Do not add main Java, auto-configuration, the reference app, an example module, or `vadmin-spring-jdbc`. JDBC remains an explicit consumer dependency, enabled with `vadmin.jdbc.enabled=true`. A consumer that chooses JDBC excludes the starter's `vadmin-spring-jpa`; do not make that exclusion the default. `StarterDependencyTest` guards the composition.

## BOM

`vadmin-bom` is the consumer-imported bill of materials. It aligns the published VAdmin modules and imports `vaadin-bom`. It does not manage Spring Boot.

Its managed dependency list must match `dependencyManagement` in the root `pom.xml`. `ConsumerBomContractTest` rejects drift, rejects managing `vadmin-bom` or `vadmin-reference-app`, and rejects exporting internal lines such as `spring-boot-dependencies`, the Testcontainers BOM, and Flyway. Do not add an internal-only line to the parent section merely because the BOM imports the parent.

Adoption guides import `vadmin-bom`. They do not tell a consumer to import `vaadin-bom` or pin `${vaadin.version}`. The `flow-maven-plugin` version is the root POM's `flow-maven-plugin.version` property, not `vaadin.version`. Deployment guides in both languages must track the plugin property; `ConsumerBomContractTest` checks that wording.

## Production Frontend

Flow's production scanner does not visit a Spring Boot application class, and it does not visit a view registered only through `AdminModule`.

- For a built-in dynamic system view, add `@Uses` to `DefaultMainLayout`.
- For a consumer dynamic view, the consumer subclasses `AdminModuleViewAnchor` and adds one `@Uses` on that subclass. VAdmin does not declare `@Uses` for consumer views.
- Plain `test` does not prove production frontend discovery. Use the production command in `references/verification.md`.

## Shell Replacement

The default shell is all-or-nothing. A consumer that needs a different shell supplies `AdminHostLayout`, its own `AppShellConfigurator`, and its stylesheet configuration. It still uses module assembly, permissions, route guards, and the composite translation provider. Do not add product configuration that replaces only a header, navigation item, page, or visual fragment.
