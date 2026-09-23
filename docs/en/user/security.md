# Security

VAdmin publication coordinate: `io.github.youngledo:vadmin-spring-boot-starter`.

[简体中文](../../zh-CN/user/security.md) | English

## Identity And Bootstrap Account

The default sign-in method is a local username and password. On an empty
database, startup creates an `admin` account, an `administrator` role, and
grants every permission in the current permission catalog to that role.

- The `development` profile may use `change-me` as a local-only fallback
  password.
- Any non-`development` profile must set `APP_BOOTSTRAP_PASSWORD` when
  starting on an empty database; otherwise the application refuses to start.
- The bootstrap password only creates the first administrator; existing
  users never have their password or roles overwritten.
- Production must inject the bootstrap password from a secret-management
  system, deployment-platform secret, or protected environment variable. Do
  not commit `.env` files, passwords, tokens, or database backups to the
  repository.

The optional Spring Security OIDC adapter supports standard discovery and
authorization-code login. It only maps an authenticated issuer and subject to
an existing, enabled local account; failed mappings are rejected. It never
creates accounts, synchronizes groups or roles, exposes provider tokens to
Flow modules, or changes local permission checks. Keycloak appears only as a
Testcontainers interoperability fixture, not as a production runtime
dependency.

SAML, LDAP, MFA, SCIM, multi-tenancy, organization hierarchies, and row-level
data permissions are out of current scope. They should arrive through explicit
authentication or authorization adapter designs, never by bypassing the
platform use-case checks inside views.

## Authorization Boundary

Permission codes are the only authorization vocabulary, formatted
`domain:resource:action`, for example `system:user:update`. Roles are merely
configurable permission sets.

The same permission code applies in four places: the menu projection, the
Flow route check, interactive controls such as buttons, and the final
platform use-case check. The first three improve the experience; the
platform use-case check is the non-bypassable security boundary. Never
protect write operations by hiding buttons or front-end navigation alone.

The permission catalog is defined in code and synchronized to the database at
startup. Administrators may grant catalog permissions to roles, but cannot
invent new permission codes in the UI.

## Errors And Audit

The core layer exposes only business failure codes and safe field details.
Custom HTTP/MVC APIs return mapped failures as RFC 9457
`application/problem+json` with a stable error type, HTTP status,
`errorCode`, safe details, field errors, and a correlation ID. Never return
stack traces, SQL, passwords, tokens, or sensitive metadata in Problem
Details.

Vaadin Flow RPC and navigation are not REST APIs: validation failures show a
safe generic message, access denial lands on a 403 view, and other failures
land on a 500 view instead of forcing Problem Details. Business failures
still carry field errors so future form binders can render them per field,
but the generic Flow error presenters do not expose them to users directly.

Every HTTP request accepts or generates an `X-Correlation-Id`, used in the
response header, logging MDC, audit records, and Problem Details. When
troubleshooting, record and share that ID instead of copying sensitive
request content.

Successful administration operations and their audit records are written in
the same database transaction. Audit data must never contain password hashes,
plaintext passwords, tokens, SQL, or stack traces.
