# Security

English | [简体中文](../../zh-CN/user/security.md)

## Sign-In

VAdmin provides local username/password sign-in out of the box. On first
startup with an empty database, it creates an `admin` account with the
`administrator` role and grants it all permissions.

Set `APP_BOOTSTRAP_PASSWORD` before the first start — the initial `admin`
account receives that password. The `development` profile allows a
`change-me` fallback for local use; any other profile refuses to start on an
empty database without it. Changing the password later does not reset an
existing account.

In production, inject the bootstrap password from a secret-management system
or protected environment variable. Never commit `.env` files, passwords, or
tokens to the repository.

## Permissions

Permission codes follow the format `domain:resource:action`, for example
`system:user:read` or `inventory:item:update`. Roles are configurable sets of
permission codes — an administrator grants permissions to roles through the
Roles administration page.

VAdmin checks each permission in two layers:

- **Navigation and route access** — controlled by the permission declared in
  your module's page metadata. Hides pages the user cannot access and blocks
  direct navigation.
- **Service-level authorization** — the authoritative boundary. Check the
  permission again inside your service or command before any write. Do not
  rely on hiding UI controls alone to protect mutations.

The permission catalog is defined in code at startup; administrators cannot
invent new permission codes through the UI.

## Shared User And Role Management

The Users page creates accounts, enables/disables them, and assigns roles. The Roles page
creates roles and grants registered permissions. `system:role:create` controls role creation;
`system:user:roles` controls membership changes and is a privileged delegation capability.
Membership and permission changes increment the affected accounts' authentication version,
requiring existing sessions to sign in again. The last enabled administrator cannot be
disabled or lose the `administrator` role.

For a host-owned database, set `vadmin.local-iam.enabled=false` and
`vadmin.system-admin.enabled=true`. Supply `AdministrationStore` and
`AccessControlRepository` alongside the existing identity, authorization, and audit contracts.
VAdmin assembles the management services and transactional permission-grant use case; hosts
do not recreate system pages. An existing `GrantPermissionUseCase` bean takes precedence.

For a standard VAdmin schema on a host-owned database, add
`io.github.huahill:vadmin-spring-jdbc` and enable `vadmin.jdbc.enabled=true`.
It supplies account lookup, session validation, redacted audit persistence,
administration storage, permission grants, and first-account initialization;
the host does not implement those contracts or its own initializer.
Keep `vadmin.system-admin.enabled=true` to use the shared pages and
enable the VAdmin Spring Security integration in the host.

All six tables share `vadmin.jdbc.table-prefix` (default `vadmin`).
`vadmin.jdbc.uuid-storage=string` uses MySQL `CHAR(36)` identifiers;
`native` uses PostgreSQL UUIDs and JSONB audit metadata. Existing contract beans
take precedence over the JDBC defaults. Exclude the starter's `vadmin-spring-jpa`
and PostgreSQL driver when replacing the default persistence setup.
Management writes, audit, and initialization must share one transaction manager
and DataSource. The host owns Flyway schema migrations; this module never creates tables.

Set `vadmin.bootstrap.password` or inject `APP_BOOTSTRAP_PASSWORD` on the first start.
An empty schema receives `admin`, the `administrator` role, and the declared permissions
in one transaction. Missing credentials fail initialization outside the `dev` profile;
`dev` alone falls back to `change-me`. Existing accounts are never reset. Later starts
grant newly declared permissions to the administrator role and invalidate affected sessions.

## OIDC (Optional)

VAdmin supports optional standard OIDC authorization-code login through
Spring Security. It maps an authenticated external identity to an existing,
enabled local account — it does not create accounts, synchronize roles, or
expose provider tokens to your views.

Configure it with standard `spring.security.oauth2.client.*` properties and
set `vadmin.oidc.registration-id` (defaults to `oidc`). The external identity
must map to a pre-existing local account; unmapped identities are rejected.

SAML, LDAP, MFA, SCIM, multi-tenancy, and row-level data permissions are not
built in. Integrate them through your own authentication or authorization
adapter — never bypass the service-level permission check.

## Audit And Error Handling

Successful administration operations are audited in the same transaction as
the data change. Audit records never contain passwords, tokens, SQL, or
stack traces.

HTTP failures return RFC 9457 `application/problem+json` with a stable error
type, HTTP status, safe details, and a correlation ID. Vaadin Flow UI
failures (navigation, validation) render safe generic messages instead of
exposing internals. Each request carries an `X-Correlation-Id` for
troubleshooting; share that ID instead of sensitive request content.
