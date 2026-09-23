# 快速开始

[English](../../en/user/getting-started.md) | 简体中文

向已有的 Spring Boot 应用添加一个依赖并启动，即可获得：引导管理员登录、
按权限过滤的导航、默认外壳与主题、Users/Roles/Permissions/Audit 系统管理模块、
`zh-CN` 与 `en-US` 语言、浅色/深色配色方案以及视觉语言选择。你不需要构建布局、
主题或系统页面——VAdmin 提供了它们。

## 前置条件

- 基于 Java 25 的 Spring Boot 应用。
- 一个 PostgreSQL 数据库（VAdmin 的表结构由 Flyway 迁移管理）。
- classpath 上的 Vaadin Flow 25.x。

如果你的应用尚未使用 Vaadin Flow，starter 会传递引入。VAdmin 不包含 Hilla、
React 或 TypeScript。

## 1. 添加依赖

```xml
<dependency>
  <groupId>io.github.youngledo</groupId>
  <artifactId>vadmin-spring-boot-starter</artifactId>
  <version>${vadmin.version}</version>
</dependency>
```

如果应用显式声明了 `@EnableVaadin`，请加入 VAdmin 根包，让 Vaadin 发现默认外壳
与系统视图。这是 Vaadin 的路由发现要求，不是自行组装外壳：

```java
@EnableVaadin({"com.example.inventory", "io.github.youngledo.vadmin"})
@SpringBootApplication
public class InventoryApplication {
}
```

通过 `spring-boot:run` 开发时，还需直接声明 Vaadin 的可选开发服务器。它有意不
传递，且不进入生产制品：

```xml
<dependency>
  <groupId>com.vaadin</groupId>
  <artifactId>vaadin-dev</artifactId>
  <version>${vaadin.version}</version>
  <optional>true</optional>
</dependency>
```

## 2. 指向你的 PostgreSQL

VAdmin 使用标准 Spring Boot 数据源。如果你的应用已经配置了 PostgreSQL，无需额外
的数据库设置——VAdmin 通过 Flyway 管理自己的表，与你已有的迁移并列运行：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory
    username: inventory
    password: change-me
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration
```

使用方的迁移应与 VAdmin 自身的迁移放在不同位置，且不得改写已进入任何环境的
迁移。迁移操作见[部署与运维](deployment.md)。

## 3. 启动应用

空数据库首次启动时设置一次 `APP_BOOTSTRAP_PASSWORD`，初始 `admin` 账户使用该
密码。之后更改不会重置已有账户。

```bash
APP_BOOTSTRAP_PASSWORD='replace-this-secret' ./mvnw -B -ntp spring-boot:run
```

访问 `http://localhost:8080`，以 `admin` 登录。这就是完整的基线——无需构建外壳、
主题或系统页面。

可选地设置外壳中展示的产品名称，无需替换布局：

```yaml
app:
  brand:
    name: 库存运营台
```

其余属性均有可用默认值；需要调优时参见[配置参考](configuration.md)。

## 体验参考应用

仓库内附带一个精简的使用方应用，端到端演示接入路径。它不属于 starter——它证明
普通应用只需依赖 starter 并贡献自身功能即可运行。

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

访问 `http://localhost:8080`，用 `admin` 和 `.env` 中的 `APP_BOOTSTRAP_PASSWORD`
登录。

## 下一步

- 用[模块开发](modules.md)指南添加业务页面。
- 上线前阅读[安全说明](security.md)。
- 用[部署与运维](deployment.md)指南完成部署。
- 用[升级指南](upgrade.md)在版本间迁移。
- 用[外观配置](appearance.md)调整外观。

只有有意完全替换外壳时，使用方才需要提供 `AdminHostLayout` 和
`AppShellConfigurator`。
