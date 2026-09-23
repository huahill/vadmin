# 升级指南

VAdmin 的发布坐标为 `io.github.youngledo:vadmin-spring-boot-starter`。

[English](../../en/user/upgrade.md) | 简体中文

## 步骤

1. 将所有 `io.github.youngledo` 依赖升级到相同目标版本，不得混用已发布制品和
   snapshot 制品。
2. 使用 Java 25 运行使用方，并按目标版本的验证基线对齐 Spring Boot 与 Vaadin
   （见发布说明与[发布指南](../dev/release-guide.md)的基线表）。
3. 对照生产类数据库副本检查 Flyway 迁移。每条迁移只应用一次，且不得改写已应用
   的迁移。
4. 保持文档化的模块契约：`AdminModule` 元数据、声明的权限、路由、图标 key、
   `zh-CN` 与 `en-US` 资源、无 `@Route` 的原型 View Bean，以及每个使用方动态
   View 的宿主 `@Uses` 锚点。详见[模块开发](modules.md)。
5. 除非有意完整替换外壳，否则保留 VAdmin 默认外壳、主题和系统管理。
6. 保留外部密钥与部署配置。修改 `APP_BOOTSTRAP_PASSWORD` 不会重置已有的
   `admin` 账户。
7. 重新运行授权、语言、外观和代表性业务流程测试。OIDC 使用方还需验证
   已有本地账户映射仍然有效。

## 重要版本说明

### 升级到 0.2.0

- 验证基线升级为 Spring Boot 4.1.1、Vaadin Flow 25.3.0 和 Flyway 13.7.0。
- 默认管理产品迁入 `vadmin` 制品，根聚合 POM 更名为 `vadmin-parent`。仅依赖
  `vadmin-spring-boot-starter` 的使用方无需任何改动。
- Java 包从 `io.github.youngledo.vadmin.starter.*` 迁移到
  `io.github.youngledo.vadmin.*`。引用过内部类的代码需要更新导入；
  `@EnableVaadin` 示例现在列出 `io.github.youngledo.vadmin` 根包。
- 窄屏（overlay 抽屉）视口下外壳不再默认保持抽屉打开；桌面行为不变。
