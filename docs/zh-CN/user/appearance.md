# 外观配置

[English](../../en/user/appearance.md) | 简体中文

VAdmin 直接使用 Vaadin Aura。用户可通过外壳用户菜单，用 Vaadin 的
`ColorScheme` API 在跟随系统、浅色和深色之间切换。

没有视觉语言配置。默认外壳不再提供 Lumo，也不再提供 Ant Design 叠层。

业务模块请使用标准 Vaadin 组件变体和 `--vaadin-*` 属性，不要在共享或模块
CSS 中混用 `--lumo-*` token。
