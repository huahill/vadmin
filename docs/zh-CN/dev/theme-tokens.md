# 主题策略

VAdmin 的发布坐标为 `io.github.huahill:vadmin-spring-boot-starter`。

[English](../../en/dev/theme-tokens.md) | 简体中文

`vadmin` 拥有默认的 Vaadin Flow 外壳。其 `DefaultApplicationShell` 通过
`@StyleSheet(Aura.STYLESHEET)` 加载 Aura。普通使用方和业务模块不得定义竞争性的
外壳或全局主题。

## Aura 外观

默认外观即 Vaadin Aura 原样，并使用 Vaadin 基础样式属性（`--vaadin-*`）。
优先使用组件变体而不是编写 CSS。配色方案选择使用 `ColorScheme` 与
`Page.setColorScheme()`，提供跟随系统、浅色与深色三种选择。

外壳不覆写 App Layout、Grid、字段、按钮、对话框、覆盖层或通知。保留 Aura
标准尺寸，不提供 VAdmin 密度设置。共享模式和业务 CSS 不要混用 `--lumo-*`
属性。

标准导航图标请使用 `vadmin-flow` 提供的 `AdminIconName` 与 `AdminIconCatalog`。
业务模块不得另行发布一套图标语言。

## 业务模块 CSS

CSS 必须限定在模块自有的组件类之内：

```css
.inventory-summary {
  background: var(--vaadin-background-color);
  border: 1px solid var(--vaadin-border-color-secondary);
  border-radius: var(--vaadin-radius-m);
  color: var(--vaadin-text-color);
  padding: var(--vaadin-padding-m);
}
```

不要硬编码竞争性品牌色、替换全局页面背景、发布任意全局选择器，或针对
Flow 覆盖层与 Grid 内部结构。使用共享的 `vadmin-flow` 页面模式与正常的
Vaadin 状态 API。

## 完整替换外壳

只有有意完整替换外壳的使用方才拥有备选的 `AppShellConfigurator`、样式表
与 token 实现，且必须为所有已组装模块保持连贯的无障碍外观。不支持零散替换
默认外壳组件或个别系统页面。
