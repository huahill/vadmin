# VAdmin

简体中文 | [English](README.md)

**开箱即用的 Java 管理后台，让你专注业务开发。**

VAdmin 基于 Spring Boot 和 [Vaadin Flow](https://vaadin.com/docs/latest/flow)，
为 Java 应用提供完整的管理后台：登录认证、用户与角色管理、权限控制、操作审计，
以及统一的应用布局和常用页面模式。

项目灵感来自 `Ant Design Pro`：把管理后台中反复需要的基础
能力准备好，让开发者从业务功能开始。VAdmin 将这一思路带到 Java 技术栈，使用
Java 编写业务页面，并与现有 Spring 服务集成。

## 核心能力

| 能力 | 开箱即用的内容 |
| --- | --- |
| 登录与权限控制 | 本地登录、基于角色的权限、按权限显示导航，以及页面路由访问检查。 |
| 系统管理 | 内置用户、角色、权限和操作审计页面。 |
| 应用布局 | 响应式导航、用户菜单，以及可配置的产品名称。 |
| 常用页面模式 | 页面标题、工具栏、数据工作区、详情弹窗，以及加载、空数据和失败状态。 |
| 业务模块扩展 | 通过 `AdminModule` 统一注册 Java 视图、导航、权限和翻译。 |
| 多语言与外观 | 内置中英文，直接使用 Vaadin Aura，支持跟随系统、浅色和深色配色方案。 |

适用于内部管理工具、运营工作台，以及以 Java 和 Spring Boot 为主要技术栈的业务
管理系统。

## 本地体验

仓库附带一个参考应用，演示普通 Spring Boot 应用如何接入 VAdmin。安装 Docker 后，
在仓库目录执行：

```bash
cp .env.example .env
```

在 `.env` 中设置 `APP_BOOTSTRAP_PASSWORD`，然后启动应用：

```bash
docker compose --env-file .env up --build
```

访问 `http://localhost:8080`，用 `admin` 和该密码登录。

## 接入自己的应用

在 Spring Boot 应用中导入 `vadmin-bom`，并添加 `vadmin-spring-boot-starter`。
starter 提供默认布局和系统管理功能，你通过 Java 视图和 `AdminModule` Bean 添加
自己的业务页面。

1. 按[快速开始](docs/zh-CN/user/getting-started.md)配置依赖、数据库并完成首次登录。
2. 按[模块开发](docs/zh-CN/user/modules.md)添加业务页面、导航、权限和翻译。
3. 使用共享 Flow 页面模式，构建一致的管理操作流程。

## 技术栈

当前支持 Java 25、Spring Boot 4.1 和 Vaadin Flow 25.3，默认界面直接使用 Vaadin
Aura。参考应用使用 PostgreSQL 和 Flyway，并提供 Docker 部署示例。

具体依赖版本见[根 POM](pom.xml)，模块边界与扩展方式见[架构说明](docs/zh-CN/dev/architecture.md)。

## 文档

**用户指南**

| 指南 | 链接 |
| --- | --- |
| 快速开始 | [阅读](docs/zh-CN/user/getting-started.md) |
| 模块开发 | [阅读](docs/zh-CN/user/modules.md) |
| 配置参考 | [阅读](docs/zh-CN/user/configuration.md) |
| 外观配置 | [阅读](docs/zh-CN/user/appearance.md) |
| 安全说明 | [阅读](docs/zh-CN/user/security.md) |
| 部署 | [阅读](docs/zh-CN/user/deployment.md) |

**开发者指南**

| 指南 | 链接 |
| --- | --- |
| 架构 | [阅读](docs/zh-CN/dev/architecture.md) |
| 主题策略 | [阅读](docs/zh-CN/dev/theme-tokens.md) |
| 发布指南 | [阅读](docs/zh-CN/dev/release-guide.md) |
| 需求文档 | [阅读](docs/dev/requirements.md) |
| 贡献指南 | [阅读](docs/dev/contributing.md) |

## 开发与验证

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
docker compose --env-file .env.example config
docker build -t vadmin:local .
```

## 开源许可

VAdmin 使用 [Apache License 2.0](LICENSE) 开源许可证。

Vaadin 是 Vaadin Ltd. 的商标。本项目与 Vaadin Ltd. 无隶属、赞助或背书关系。
