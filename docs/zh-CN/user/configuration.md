# 配置参考

简体中文 | [English](../../en/user/configuration.md)

所有配置都是普通的 Spring Boot 属性。只有数据源是必需项，其余配置都有可用默认值。

## 产品设置

| 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `vadmin.brand.name` | `VAdmin` | 外壳品牌区展示的产品名称，留空回退默认值。 |
| `vadmin.shell.workplace-enabled` | `true` | 显示或隐藏内置的工作台首页入口。 |

```yaml
vadmin:
  brand:
    name: 库存运营台
  shell:
    workplace-enabled: true
```

## 身份与访问

| 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `vadmin.local-iam.enabled` | `true` | 启用内置本地 IAM：用户名/密码登录以及 Users、Roles、Permissions、Audit 系统管理模块。仅当宿主应用自行提供身份与授权 Bean 时才应关闭。 |
| `vadmin.local-iam.table-prefix` | `vadmin` | 本地 IAM 六张表的前缀。默认写成 `vadmin_users` 这一组；空字符串会被拒绝，不会退回没有前缀的表名。 |
| `vadmin.oidc.registration-id` | `oidc` | 可选 OIDC 登录使用的 Spring Security 客户端注册 ID。注册本身是标准的 `spring.security.oauth2.client.*` 配置。 |

OIDC 登录只会将已认证的外部身份映射到已有且启用的本地账户，不会创建账户或同步角色；
详见[安全说明](security.md)。

## 首个管理员

| 属性 | 来源 | 用途 |
| --- | --- | --- |
| `vadmin.bootstrap.password` | 通常为 `${APP_BOOTSTRAP_PASSWORD}` | 数据库为空时创建初始 `admin` 账户所用的密码。`development` profile 回退为 `change-me`；其他 profile 在空数据库启动时缺少该值会拒绝启动。 |

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

本地 IAM 默认使用 `vadmin` 前缀，物理表名是 `vadmin_users`、`vadmin_roles`、
`vadmin_permissions`、`vadmin_user_roles`、`vadmin_role_permissions`、
`vadmin_audit_entries`。`vadmin.local-iam.table-prefix: ci` 会改成 `ci_users` 这一组。
未配置时按 `vadmin` 处理；空字符串和纯空白直接拒绝，不会裁剪，也不会退回没有前缀的名字。

`V1` 仍先创建没有前缀的 `users` 等表，`V2` 只执行 `ALTER TABLE ... RENAME TO`。
全新库如果已经有业务表 `users`，迁移仍会冲突；这是故意的，避免接到别人的表上。
约束名和索引名保持不变，例如 `uk_users_username`。迁移进入任何环境后前缀即冻结：
之后再改这项配置，Hibernate 会按新表名做 `ddl-auto: validate` 并失败。
