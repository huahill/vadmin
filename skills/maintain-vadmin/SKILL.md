---
name: maintain-vadmin
description: Use when modifying the VAdmin repository itself, including module boundaries, Flow patterns, the default administration shell, Spring adapters, the BOM, the Spring Boot starter, production frontend anchors, or product documentation. Do not use for host applications that only consume VAdmin.
---

# Maintain VAdmin

## Purpose

Use this skill when changing this repository as the VAdmin product. It protects module ownership, starter and BOM semantics, production frontend anchors, and verification.

Do not apply it to a host application that only consumes VAdmin. Consumer guidance, including `using-vadmin` when that plugin is available, is not authority for changing this repository. Do not import JFoundry's multi-runtime, messaging, or framework-maintenance rules here. Spring Boot is the only runtime and Vaadin Flow is the only UI model.

`AGENTS.md` owns commit messages, theme governance, and product composition. Do not restate or weaken those rules here.

## Maintenance Workflow

1. Classify the task: contract, platform use case, Flow pattern or navigation, default shell or system administration, Spring Security, JPA, JDBC, Boot error handling, Flow assembly, starter, BOM, reference app, documentation, or release compatibility.
2. Read only the matching reference:
   - `references/module-placement.md` before adding or moving code.
   - `references/starter-bom-and-anchors.md` before changing dependency management, starter contents, dynamic routes, or production anchors.
   - `references/verification.md` before choosing a Maven command.
3. Inspect the existing module and the test that already guards the same boundary.
4. Make the smallest change that keeps contracts, platform use cases, and Flow patterns free of Spring and persistence details.
5. Add or update the focused test beside the affected module. The architecture tests are boundary enforcement, not optional style checks.
6. Run the narrowest `./mvnw` command from `references/verification.md`. Use the broader verify commands when a public API, starter dependency, BOM entry, auto-configuration, route registration, or production frontend behavior changed.
7. State the compatibility impact when changing published coordinates, starter dependencies, configuration properties, route registration, permission checks, or identity behavior.
8. When module ownership, starter contents, or verification commands change, update the matching reference in the same change. If an English document changes and a Chinese counterpart exists, update both or say why one language is unaffected.

## Non-Negotiable Boundaries

- Keep `vadmin-contracts`, `vadmin-platform`, and `vadmin-flow` independent of Spring, Jakarta Persistence, and Flyway.
- Keep Java and `AutoConfiguration.imports` in the capability module that owns the behavior. The starter is a dependency aggregate and has no main sources.
- Keep `vadmin-spring-jdbc` opt-in. Do not add it to the default starter.
- Put a repeated administration workflow in a `vadmin-flow` pattern. Do not copy it into the default shell or the reference app.
- Keep the reference app as proof that a normal application can adopt the starter. It does not own the default shell, theme, or system administration.
- Register product dynamic system views with `@Uses` on `DefaultMainLayout`. Consumer dynamic views subclass `AdminModuleViewAnchor`; they do not use `@Route`, and their anchor is neither a Spring bean nor an SPI service.
- Treat a different shell as a complete replacement. Do not add a setting that replaces an isolated header, navigation item, page, or style.
- Before adding a runtime, persistence, identity, file, or external-service adapter, record which contract port it implements, its dependency direction, failure semantics, configuration, and verification. Do not introduce that SDK into contracts, platform, or Flow without that decision.

## Source Documents

Prefer the current repository over this skill when they disagree, then update the skill:

- `AGENTS.md`
- `docs/en/dev/architecture.md` and `docs/zh-CN/dev/architecture.md`
- `docs/en/dev/release-guide.md` and `docs/zh-CN/dev/release-guide.md`
- `docs/en/dev/theme-tokens.md` and `docs/zh-CN/dev/theme-tokens.md`
- `docs/dev/contributing.md`
- `docs/en/user/getting-started.md`, `docs/en/user/deployment.md`, `docs/en/user/security.md`, and their `docs/zh-CN/user/` counterparts
- the root `pom.xml`, `vadmin-bom/pom.xml`, and `vadmin-spring/vadmin-spring-boot-starter/pom.xml`
- `ArchitectureTest`, `AdministrationArchitectureTest`, `StarterDependencyTest`, and `ConsumerBomContractTest`

## Output Discipline

When reporting a product change, include:

- modules touched
- boundary decision made
- verification command run
- compatibility or migration impact
