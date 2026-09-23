# 配置参考

VAdmin 的发布坐标为 `io.github.youngledo:vadmin-spring-boot-starter`。

[English](../../en/user/configuration.md) | 简体中文

所有配置都是普通的 Spring Boot 属性。只有数据源是必需项，其余配置都有可用默认值。

## 产品设置

| 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `app.brand.name` | `VAdmin` | 外壳品牌区展示的产品名称，留空回退默认值。 |
| `app.appearance.visual-language` | `vaadin` | 外壳视觉语言：`vaadin`（原生 Lumo）或 `ant`（Ant Design 风格），未知值回退 `vaadin`。 |
| `app.shell.workplace-enabled` | `true` | 显示或隐藏内置的工作台首页入口。 |

```yaml
app:
  brand:
    name: 库存运营台
  appearance:
    visual-language: ant
  shell:
    workplace-enabled: true
```

## 身份与访问

| 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `vadmin.local-iam.enabled` | `true` | 启用内置本地 IAM：用户名/密码登录以及 Users、Roles、Permissions、Audit 系统管理模块。仅当宿主应用自行提供身份与授权 Bean 时才应关闭。 |
| `vadmin.oidc.registration-id` | `oidc` | 可选 OIDC 登录使用的 Spring Security 客户端注册 ID。注册本身是标准的 `spring.security.oauth2.client.*` 配置。 |

OIDC 登录只会将已认证的外部身份映射到已有且启用的本地账户，不会创建账户或同步角色；
详见[安全说明](security.md)。

## 首个管理员

| 属性 | 来源 | 用途 |
| --- | --- | --- |
| `app.bootstrap.password` | 通常为 `${APP_BOOTSTRAP_PASSWORD}` | 数据库为空时创建初始 `admin` 账户所用的密码。`development` profile 回退为 `change-me`；其他 profile 在空数据库启动时缺少该值会拒绝启动。 |

引导密码只用于创建第一个管理员，不会重置已有账户，事后修改对已有用户没有影响。

## 数据库

VAdmin 使用标准 Spring Boot 数据源配置加 Flyway。建议 `ddl-auto: validate`；
VAdmin 的表结构完全由迁移管理。

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

使用方迁移应与 VAdmin 自身迁移放在不同位置，且不得改写已进入任何环境的迁移。
备份与迁移操作见[部署与运维](deployment.md)。
