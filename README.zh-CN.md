# VAdmin

简体中文 | [English](README.md)

VAdmin 是一个 Java 优先、面向 Vaadin Flow 的管理基线。它的可复用核心——
身份与权限契约、RBAC 用例和共享外壳模式——框架无关，运行时专属行为归入适配层，
因此 Spring Boot 之外的运行时（如 Quarkus、Helidon、Jakarta EE）仍是未来可能的
适配方向。Spring Boot 是当前唯一受支持的运行时：添加 `vadmin-spring-boot-starter`
即可获得本地登录、按权限过滤的导航、默认外壳与主题、Users、Roles、Permissions、
Audit、语言选择和外观控制。它不是一组展示页面，也不是运行时插件平台。

首发版本使用 Java 25、Maven 4.0.0-rc-6、Spring Boot 4.1.1、Vaadin Flow 25.3.0、
PostgreSQL 和 Flyway。Flow 是唯一的 UI 模型；不包含 Hilla、React 和 TypeScript。

VAdmin 通过默认管理模块提供完整的基线体验。使用方以 `AdminModule` Bean 和 Flow
View Bean 添加业务能力，无需为正常接入创建外壳、主题或系统管理模块。模块、翻译
和生产锚点契约详见[模块开发](docs/zh-CN/user/modules.md)指南。

## 快速开始

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

访问 `http://localhost:8080`，用 `admin` 和 `.env` 中的 `APP_BOOTSTRAP_PASSWORD`
登录。

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
| 升级 | [阅读](docs/zh-CN/user/upgrade.md) |

**开发者指南**

| 指南 | 链接 |
| --- | --- |
| 架构 | [阅读](docs/zh-CN/dev/architecture.md) |
| 主题策略 | [阅读](docs/zh-CN/dev/theme-tokens.md) |
| 发布指南 | [阅读](docs/zh-CN/dev/release-guide.md) |
| 需求文档 | [阅读](docs/dev/requirements.md) |
| 贡献指南 | [阅读](docs/dev/contributing.md) |

## 验证

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
docker compose --env-file .env.example config
docker build -t vadmin:local .
```

Vaadin 是 Vaadin Ltd. 的商标。本项目与 Vaadin Ltd. 无隶属、赞助或背书关系。
