# Configuration Reference

English | [简体中文](../../zh-CN/user/configuration.md)

All settings are ordinary Spring Boot properties. Only the datasource is
required; everything else has a working default.

## Product Settings

| Property | Default | Purpose |
| --- | --- | --- |
| `vadmin.brand.name` | `VAdmin` | Product name shown in the shell brand area. Blank values fall back to the default. |
| `vadmin.shell.workplace-enabled` | `true` | Shows or hides the built-in workplace home entry. |

```yaml
vadmin:
  brand:
    name: Inventory Operations
  shell:
    workplace-enabled: true
```

## Identity And Access

| Property | Default | Purpose |
| --- | --- | --- |
| `vadmin.local-iam.enabled` | `true` | Enables the built-in local IAM: username/password login and the Users, Roles, Permissions, and Audit administration module. Disable it only when the host application provides its own identity and authorization beans. |
| `vadmin.local-iam.table-prefix` | `vadmin` | Prefix for the six local IAM tables. The default produces `vadmin_users` and its siblings. A blank value is rejected and does not fall back to unprefixed names. |
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

Local IAM tables use the `vadmin` prefix by default: `vadmin_users`,
`vadmin_roles`, `vadmin_permissions`, `vadmin_user_roles`,
`vadmin_role_permissions`, and `vadmin_audit_entries`. Setting
`vadmin.local-iam.table-prefix: ci` produces the `ci_users` family instead.
A missing value means `vadmin`. An empty or whitespace-only value is rejected;
it is neither trimmed nor treated as “no prefix”.

`V1` still creates the original unprefixed tables, and `V2` only renames them
with `ALTER TABLE ... RENAME TO`. A fresh database that already contains a
business `users` table therefore still conflicts; that is intentional, so
VAdmin does not attach to someone else's table. Constraint and index names,
such as `uk_users_username`, stay unchanged. Once `V2` has reached an
environment the prefix is frozen: changing it later makes
`ddl-auto: validate` fail because Hibernate looks for the new names.
