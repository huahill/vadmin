# 升级

[English](../../en/user/upgrade.md) | 简体中文

## 通用步骤

1. 在依赖管理中将 `vadmin-spring-boot-starter` 升级到目标版本。VAdmin 的全部模块
   统一版本。
2. 确保应用在 Java 25 上运行，Spring Boot 和 Vaadin 版本与发布说明一致。
3. 对照生产类数据库副本检查 Flyway 迁移。每条迁移只应用一次，不得改写已应用的
   迁移。
4. 重新运行测试。重点关注授权、语言切换和外观。如果使用了 OIDC，验证外部到本地
   账户的映射仍然有效。
5. 修改 `APP_BOOTSTRAP_PASSWORD` 不会重置已有的 `admin` 账户——见
   [安全说明](security.md)。

如需复查模块声明见[模块开发](modules.md)，属性变更见[配置参考](configuration.md)。

## 升级到 0.2.0

- **运行时基线**：Spring Boot 4.1.1、Vaadin Flow 25.3.0、Flyway 13.7.0。
- **仅依赖 `vadmin-spring-boot-starter`**：无需任何改动，构建照常。
- **代码中导入过 VAdmin 内部类**：包从 `io.github.youngledo.vadmin.starter.*` 迁移
  到 `io.github.youngledo.vadmin.*`。更新导入和 `@EnableVaadin` 包列表（现在只需
  `io.github.youngledo.vadmin`）。
- **窄屏**：手机宽度视口下导航抽屉不再默认保持打开。桌面行为不变。
