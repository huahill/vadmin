# 主题策略

VAdmin 的发布坐标为 `io.github.youngledo:vadmin-spring-boot-starter`。

[English](../../en/dev/theme-tokens.md) | 简体中文

`vadmin` 拥有默认的 Vaadin Flow 外壳。其 `DefaultApplicationShell` 显式加载
Lumo；内置的 Ant 资源完全作用域化，宿主未选择 `ant` 语言时不产生任何影响。
普通使用方和业务模块不得定义竞争性的外壳或全局主题。

## 默认 vaadin 视觉语言

`vaadin` 是默认视觉语言，直接使用 Lumo 与 Vaadin 的基础样式属性
（`--lumo-*` 与 `--vaadin-*`）。优先使用组件变体而不是编写 CSS。配色方案
选择使用 `ColorScheme` 与 `Page.setColorScheme()`，提供跟随系统、浅色与深色
三种选择。

外壳不覆写 App Layout、Grid、字段、按钮、对话框、覆盖层或通知。保留 Lumo
标准尺寸，不提供 VAdmin 密度设置。

## ant 视觉语言

`ant` 是显式的备选视觉语言。其选择器全部由
`[data-vadmin-visual-language="ant"]` 限定，通过 `--vadmin-ant-*` token 与
定向的 `::part()` 覆写实现 Ant Design 行为。任何模块不得依赖这些选择器或
token。

标准操作图标请使用 `vadmin-flow` 提供的 `AdminIcon` 与 `AdminIconName`。
业务模块不得导入 VAdmin 主题图标、赋值 `data-admin-icon` 或设置图标遮罩
变量。VAdmin 拥有外观专属的图标资源与 Vaadin 回退。

## 业务模块 CSS

CSS 必须限定在模块自有的组件类之内：

```css
.inventory-summary {
  background: var(--lumo-base-color);
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

只有有意完整替换外壳的使用方才拥有备选的 `AppShellConfigurator`、`@Theme`
与 token 实现，且必须为所有已组装模块保持连贯的无障碍外观。不支持零散替换
默认外壳组件或个别系统页面。
