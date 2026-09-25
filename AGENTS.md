# Repository Guidelines

## Git Commits

- Commit messages must be written in English, including the subject and body.
- Follow the Conventional Commits specification: `type(optional scope): imperative summary`.
- Use a concise imperative subject, no longer than 72 characters. Prefer standard types such as `feat`, `fix`, `docs`, `refactor`, `test`, `build`, and `chore`.
- Add a body when context, rationale, migration steps, or breaking-change details are needed.
- Describe breaking changes in the commit footer using `BREAKING CHANGE:`.

## Theme Governance

- The default appearance uses Vaadin Aura as-is. VAdmin must not ship or
  register CSS that changes its visual appearance; compose the shell with
  documented Vaadin component APIs instead.
- The product-owned shell may set a documented Vaadin component custom
  property from Flow only when no component API exists for structural behavior
  such as the AppLayout drawer width. This is not a VAdmin visual token or a
  stylesheet override.
- Use `ColorScheme` and `Page.setColorScheme()` for system, light, and dark
  color schemes. Do not implement color schemes by mutating the `theme` HTML
  attribute or by duplicating light and dark component rules.
- Do not introduce VAdmin color, radius, spacing, density, control, or
  notification token systems.
- Business modules express business semantics and use VAdmin Flow patterns and
  Vaadin variants. They do not add global visual CSS or override Vaadin
  component internals.

## Product Composition

- VAdmin is an opinionated, ready-to-use administration product for Java
  applications, comparable in scope to an administration baseline rather than
  a collection of individual Vaadin examples. The default administration module (`vadmin`) owns the responsive
  application shell, navigation, global utilities, system administration,
  common interaction patterns, and their accessibility behavior.
- A normal host application configures deployment, authentication integration,
  and a small set of product settings such as its display name. It contributes
  `AdminModule` metadata, permissions, translations, and domain views. It does
  not recreate an `AppLayout`, `AdminHostLayout`, application shell, global
  theme, or system pages.
- Domain views own only domain-specific content and commands. They use the
  highest-level VAdmin Flow pattern that fits the workflow. When a repeated
  administration workflow is missing, extend VAdmin's shared pattern instead
  of making every consuming application assemble the same layout and behavior
  from primitive Flow components.
- A different shell is an intentional, complete product replacement. It is not
  a customization mechanism for isolated header, navigation, page, or styling
  fragments. VAdmin configuration must expose coherent product choices, not
  arbitrary layout or component-level visual controls.
