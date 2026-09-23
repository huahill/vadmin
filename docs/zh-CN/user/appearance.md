# 外观配置

[English](../../en/user/appearance.md) | 简体中文

VAdmin 提供两种视觉语言，均支持跟随系统/浅色/深色配色方案——用户可从外壳的用户
菜单中切换。

| 视觉语言 | 说明 |
| --- | --- |
| `vaadin` | 默认，直接使用 Vaadin Lumo，无自定义 CSS。 |
| `ant` | 受 Ant Design 启发的替代方案，拥有自己的样式。 |

## 配置

```yaml
vadmin:
  appearance:
    visual-language: ant # vaadin | ant
```

也可通过 `APP_APPEARANCE_VISUAL_LANGUAGE` 环境变量设置。未知或留空值回退为
`vaadin`。

两种语言共享相同的路由、权限、模块和翻译——切换纯粹是视觉层面的。在你的业务模块
中，使用标准 Vaadin 组件变体和 Lumo 属性来编写样式。VAdmin 为自己的外壳和系统页面
处理视觉语言相关的呈现（图标、间距、密度）。
