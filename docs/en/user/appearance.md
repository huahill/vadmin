# Appearance

[简体中文](../../zh-CN/user/appearance.md) | English

VAdmin offers two visual languages. Both support system/light/dark color
schemes through Vaadin's `ColorScheme` API — each user can pick their
preference from the shell's user menu.

| Visual language | Description |
| --- | --- |
| `vaadin` | Default. Uses Vaadin Lumo as-is, no custom CSS. |
| `ant` | Ant Design-inspired alternative with its own styling. |

## Configuration

```yaml
app:
  appearance:
    visual-language: ant # vaadin | ant
```

You can also set it via the `APP_APPEARANCE_VISUAL_LANGUAGE` environment
variable. Unknown or blank values fall back to `vaadin`.

Both languages share the same routes, permissions, modules, and translations —
switching is purely visual. In your business modules, use standard Vaadin
component variants and Lumo properties for styling. VAdmin handles the
visual-language-specific presentation (icons, spacing, density) for its own
shell and system pages.
