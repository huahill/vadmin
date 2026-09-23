# Configuration Reference

[简体中文](../../zh-CN/user/configuration.md) | English

All settings are ordinary Spring Boot properties. Only the datasource is
required; everything else has a working default.

## Product Settings

| Property | Default | Purpose |
| --- | --- | --- |
| `vadmin.brand.name` | `VAdmin` | Product name shown in the shell brand area. Blank values fall back to the default. |
| `vadmin.appearance.visual-language` | `vaadin` | Shell visual language: `vaadin` (native Lumo) or `ant` (Ant-Design-inspired). Unknown values fall back to `vaadin`. |
| `vadmin.shell.workplace-enabled` | `true` | Shows or hides the built-in workplace home entry. |

```yaml
vadmin:
  brand:
    name: Inventory Operations
  appearance:
    visual-language: ant
  shell:
    workplace-enabled: true
```

## Identity And Access

| Property | Default | Purpose |
| --- | --- | --- |
| `vadmin.local-iam.enabled` | `true` | Enables the built-in local IAM: username/password login and the Users, Roles, Permissions, and Audit administration module. Disable it only when the host application provides its own identity and authorization beans. |
| `vadmin.oidc.registration-id` | `oidc` | The Spring Security client registration used by the optional OIDC login. The registration itself is standard `spring.security.oauth2.client.*` configuration. |

OIDC login maps an authenticated external identity to an existing, enabled
local account. It never provisions accounts or synchronizes roles; see the
[Security](security.md) guide.

## First Administrator

| Property | Source | Purpose |
| --- | --- | --- |
| `vadmin.bootstrap.password` | usually `${APP_BOOTSTRAP_PASSWORD}` | Password assigned to the initial `admin` account when the database is empty. The `development` profile falls back to `change-me`; any other profile refuses to start on an empty database without it. |

The bootstrap password is only used to create the first administrator. It
never resets an existing account, and changing it later has no effect on
existing users.

## Database

VAdmin expects standard Spring Boot datasource configuration plus Flyway.
`ddl-auto: validate` is recommended; VAdmin owns its schema through
migrations.

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

Keep consumer migrations in a location separate from VAdmin's own migrations
and never rewrite a migration that has reached any environment. See the
[Deployment](deployment.md) guide for backup and migration operations.
