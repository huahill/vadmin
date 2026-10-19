# 安全说明

简体中文 | [English](../../en/user/security.md)

## 登录

VAdmin 自带本地用户名/密码登录。空数据库首次启动时创建 `admin` 账户，赋予
`administrator` 角色和全部权限。

首次启动前设置 `APP_BOOTSTRAP_PASSWORD`——初始 `admin` 账户使用该密码。
`development` profile 允许 `change-me` 作为本地回退值；其他 profile 在空数据库
启动时缺少该值会拒绝启动。之后修改不会重置已有账户。

生产环境中，引导密码应从密钥管理系统或受保护的环境变量注入。不要将 `.env` 文件、
密码或令牌提交到仓库。

## 权限

权限代码格式为 `领域:资源:动作`，例如 `system:user:read` 或
`inventory:item:update`。角色是可配置的权限集合——管理员通过角色管理页面将
权限授予角色。

VAdmin 在两层检查权限：

- **导航和路由访问**——由模块页面元数据中声明的权限控制。隐藏用户无权访问的页面，
  并拦截直接导航。
- **服务层授权**——权威边界。任何写操作都需要在你的服务或命令中再次检查权限再
  变更状态，不能仅靠隐藏 UI 控件来保护。

权限目录在启动时由代码定义；管理员无法通过 UI 创建新的权限代码。

## 公共用户与角色管理

用户页支持新增账户、启停账户和分配角色；角色页支持新增角色和授予已登记权限。
`system:role:create` 控制创建角色，`system:user:roles` 控制角色分配，属于高权限委派能力。
角色分配和权限变更会增加受影响账户的认证版本，已有会话需要重新登录。
不能停用最后一个已启用管理员，也不能移除其 `administrator` 角色。

宿主自行管理身份表时，设置 `vadmin.local-iam.enabled=false` 和
`vadmin.system-admin.enabled=true`，并在已有身份、授权、审计契约之外提供
`AdministrationStore` 与 `AccessControlRepository`。VAdmin 自动组装管理服务和带事务的
权限授予用例，宿主无需复制系统页面；已有 `GrantPermissionUseCase` Bean 优先。

使用标准 VAdmin 表结构时，引入 `io.github.huahill:vadmin-spring-jdbc` 并设置
`vadmin.jdbc.enabled=true`，框架自动提供账号查询、会话校验、脱敏审计、管理存储、
权限授予和首个管理员初始化，宿主无需实现这些契约或编写初始化 runner。
保留 `vadmin.system-admin.enabled=true` 以启用共享页面，并接入 VAdmin Spring Security。

六张表统一使用 `vadmin.jdbc.table-prefix`（默认 `vadmin`）。
`vadmin.jdbc.uuid-storage=string` 对应 MySQL `CHAR(36)`；`native` 对应 PostgreSQL UUID
和 JSONB 审计字段。已有契约 Bean 优先于 JDBC 默认实现。替换默认持久化时，排除 starter
的 `vadmin-spring-jpa` 和 PostgreSQL 驱动。管理写操作、审计和初始化必须共享 DataSource
及事务管理器；宿主仍负责 Flyway 表结构迁移，本模块不会创建表。

首次启动配置 `vadmin.bootstrap.password` 或注入 `APP_BOOTSTRAP_PASSWORD`。
空表中会在一个事务内创建 `admin`、`administrator` 角色及已登记权限。
非 `dev` 环境缺少密码时初始化失败；仅 `dev` 可回退到 `change-me`。
已有账户不会重置密码，后续启动补授新增权限并使受影响会话失效。

## OIDC（可选）

VAdmin 通过 Spring Security 支持可选的标准 OIDC 授权码登录。它将已认证的外部身份
映射到已有且启用的本地账户——不会创建账户、同步角色或向视图暴露提供商令牌。

使用标准 `spring.security.oauth2.client.*` 属性配置，并设置
`vadmin.oidc.registration-id`（默认 `oidc`）。外部身份必须映射到已有的本地账户，
未映射的身份会被拒绝。

SAML、LDAP、MFA、SCIM、多租户和行级数据权限不在内置范围内。通过你自己的认证或
授权适配器集成——不得绕过服务层权限检查。

## 审计与错误处理

成功的管理操作与数据变更在同一事务中写入审计记录。审计记录不含密码、令牌、SQL
或堆栈。

HTTP 失败返回 RFC 9457 `application/problem+json`，包含稳定的错误类型、HTTP 状态、
安全详情和关联 ID。Vaadin Flow UI 失败（导航、校验）显示安全的通用消息，不暴露
内部信息。每个请求携带 `X-Correlation-Id` 用于排障；分享该 ID 而非敏感请求内容。
