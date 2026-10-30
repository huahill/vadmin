# Verification

Run the repository Maven wrapper from the repository root. The Java baseline is the root POM's `java.version`; do not copy its current patch level into this skill. Do not use a system `mvn` when the wrapper is available.

Start with the narrowest useful test:

```bash
./mvnw -pl <module> test
./mvnw -pl <module> -am test
```

| Change | Command |
| --- | --- |
| Contract | `./mvnw -pl vadmin-contracts test` |
| Platform use case | `./mvnw -pl vadmin-platform test` |
| Flow pattern, route guard, or anchor | `./mvnw -pl vadmin-flow test` |
| Security or OIDC adapter | `./mvnw -pl vadmin-spring/vadmin-spring-security -am test` |
| JPA adapter | `./mvnw -pl vadmin-spring/vadmin-spring-jpa -am test` |
| JDBC adapter | `./mvnw -pl vadmin-spring/vadmin-spring-jdbc -am test` |
| Boot error handling or correlation | `./mvnw -pl vadmin-spring/vadmin-spring-boot -am test` |
| Module assembly or translations | `./mvnw -pl vadmin-spring/vadmin-spring-flow -am test` |
| Default shell or system administration | `./mvnw -pl vadmin-spring/vadmin -am test` |
| Starter contents | `./mvnw -pl vadmin-spring/vadmin-spring-boot-starter -am test` |
| Reference app, BOM contract, or architecture rules | `./mvnw -pl vadmin-reference-app -am test` |
| Browser or OIDC integration | `./mvnw -pl vadmin-reference-app -am verify` |

`BrowserE2EIT` and `KeycloakOidcIT` are Failsafe integration tests. `test` does not run them; `verify` does.

For a public API, starter dependency, BOM entry, auto-configuration, route registration, or production frontend change, run both:

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
```

`.github/workflows/verify.yml` is the repository gate: wrapper `verify`, then production `verify`. A green module test does not replace that gate for a release-sensitive change.

A change to permissions, database migrations, security adapters, or Flow interaction needs a unit, integration, or browser test beside the affected behavior. Run `git diff --check` before committing.

A documentation-only edit to a getting-started, deployment, release, or contribution guide still needs `ConsumerBomContractTest`, because that test locks the BOM and Flow plugin wording in both languages.
