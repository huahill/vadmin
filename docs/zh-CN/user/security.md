# 安全说明

[English](../../en/user/security.md) | 简体中文

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
