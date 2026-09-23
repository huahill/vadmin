# Upgrade

[简体中文](../../zh-CN/user/upgrade.md) | English

## General Steps

1. Update `vadmin-spring-boot-starter` to the target version in your
   dependency management. All VAdmin modules are versioned together.
2. Ensure your application runs on Java 25 with the Spring Boot and Vaadin
   versions listed in the release notes.
3. Review Flyway migrations against a production-like database copy. Apply
   each migration once and never rewrite an applied migration.
4. Re-run your tests. Pay attention to authorization, locale switching,
   and appearance. If you use OIDC, verify your external-to-local-account
   mapping still works.
5. Changing `APP_BOOTSTRAP_PASSWORD` does not reset an existing `admin`
   account — see [Security](security.md).

See the [Modules](modules.md) guide if you need to review your module
declarations, and the [Configuration](configuration.md) reference for
property changes.

## Upgrading To 0.2.0

- **Runtime baseline**: Spring Boot 4.1.1, Vaadin Flow 25.3.0, Flyway 13.7.0.
- **If you only depend on `vadmin-spring-boot-starter`**: no changes needed.
  Your build keeps working.
- **If your code imported VAdmin internal classes**: packages moved from
  `io.github.huahill.vadmin.starter.*` to `io.github.huahill.vadmin.*`.
  Update your imports and the `@EnableVaadin` package list (now just
  `io.github.huahill.vadmin`).
- **Narrow screens**: the navigation drawer no longer stays open by default
  on phone-width viewports. Desktop behavior is unchanged.
