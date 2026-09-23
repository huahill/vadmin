# Upgrade Guide

VAdmin publication coordinate: `io.github.youngledo:vadmin-spring-boot-starter`.

[简体中文](../../zh-CN/user/upgrade.md) | English

## Steps

1. Update all `io.github.youngledo` dependencies to the same target version.
   Do not mix released artifacts with snapshot siblings.
2. Run the consumer with Java 25 and align Spring Boot and Vaadin with the
   verified baseline of the target release (see the release notes and the
   project [release guide](../dev/release-guide.md) baseline table).
3. Review Flyway migrations against a production-like database copy. Apply
   each migration once and never rewrite an applied migration.
4. Keep the documented module contract: `AdminModule` metadata, declared
   permissions, routes, icon keys, `zh-CN` and `en-US` resources, prototype
   view beans without `@Route`, and a host `@Uses` anchor for each consumer
   dynamic view. See the [Modules](modules.md) guide.
5. Keep the VAdmin default shell, theme, and system administration unless the
   consumer intentionally replaces the complete shell.
6. Preserve external secrets and deployment configuration. Changing
   `APP_BOOTSTRAP_PASSWORD` does not reset an existing `admin` account.
7. Re-run authorization, locale, appearance, and representative business-flow
   tests. OIDC consumers must also verify their existing-local-account
   mapping.

## Notable Upgrades

### Upgrading to 0.2.0

- The verified baseline moves to Spring Boot 4.1.1, Vaadin Flow 25.3.0, and
  Flyway 13.7.0.
- The default administration product now ships as the `vadmin` artifact and
  the root aggregator POM is renamed `vadmin-parent`. Consumers that only
  depend on `vadmin-spring-boot-starter` keep building unchanged.
- Java packages moved from `io.github.youngledo.vadmin.starter.*` to
  `io.github.youngledo.vadmin.*`. Update imports if your code referenced
  internal classes; the `@EnableVaadin` example now lists the
  `io.github.youngledo.vadmin` root package.
- On narrow (overlay-drawer) viewports the shell no longer keeps the drawer
  open by default; desktop behavior is unchanged.
