# Theme Policy

VAdmin publication coordinate: `io.github.huahill:vadmin-spring-boot-starter`.

[简体中文](../../zh-CN/dev/theme-tokens.md) | English

`vadmin` owns the default Vaadin Flow shell. Its
`DefaultApplicationShell` loads Aura with `@StyleSheet(Aura.STYLESHEET)`.
Normal consumers and business modules do not define a competing shell or global
theme.

## Aura Appearance

The default appearance is Vaadin Aura as-is, plus Vaadin base-style properties
(`--vaadin-*`). Use component variants before writing CSS. Color scheme
selection uses `ColorScheme` and `Page.setColorScheme()` with System
preference, Light, and Dark choices.

The shell does not override App Layout, Grid, fields, buttons, dialogs,
overlays, or notifications. Aura's standard sizing is retained without a
VAdmin density setting. Do not mix `--lumo-*` properties into shared patterns
or business CSS.

Use `AdminIconName` and `AdminIconCatalog` from `vadmin-flow` for standard
navigation icons. Do not ship a competing icon language from a business module.

## Business Module CSS

Keep CSS scoped to a module-owned component class:

```css
.inventory-summary {
  background: var(--vaadin-background-color);
  border: 1px solid var(--vaadin-border-color-secondary);
  border-radius: var(--vaadin-radius-m);
  color: var(--vaadin-text-color);
  padding: var(--vaadin-padding-m);
}
```

Do not hard-code a competing brand color, replace global page backgrounds, ship
arbitrary global selectors, or target Flow overlay and Grid internals. Use the
shared `vadmin-flow` page patterns and normal Vaadin state APIs.

## Full Shell Replacement

Only a consumer deliberately replacing the complete shell owns an alternative
`AppShellConfigurator`, stylesheet, and token implementation. It must preserve a
coherent accessible appearance for all assembled modules. Partial replacement
of default shell pieces or selected system pages is not supported.
