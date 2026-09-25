# Appearance

[简体中文](../../zh-CN/user/appearance.md) | English

VAdmin uses Vaadin Aura as-is. Users pick system, light, or dark color schemes
from the shell's user menu through Vaadin's `ColorScheme` API.

There is no visual-language setting. The default shell does not ship Lumo or
an Ant Design overlay.

In business modules, use standard Vaadin component variants and `--vaadin-*`
properties. Do not mix `--lumo-*` tokens into shared or module CSS.
