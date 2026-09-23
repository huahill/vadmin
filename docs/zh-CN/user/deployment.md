# 部署

[English](../../en/user/deployment.md) | 简体中文

VAdmin 是一个普通的 Spring Boot 库——按你已有的方式部署应用即可。本指南涵盖上线前
需要检查的 VAdmin 相关事项。

## 数据库

VAdmin 通过 Flyway 管理自己的表，并以 `ddl-auto: validate` 校验。确保生产 PostgreSQL
可达，且 Flyway 能在 Hibernate 初始化前运行。

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.example:5432/vadmin
    username: vadmin
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

不得改写已进入任何环境的 Flyway 迁移。修复表结构或数据问题应新增下一个版本化迁移。
升级前先在生产数据副本上验证迁移和回滚方案。

## 密钥

所有敏感值应通过运行平台的密钥机制提供，不要放在镜像层或 Compose 文件中：

- `DATABASE_PASSWORD`——你的 PostgreSQL 凭据。
- `APP_BOOTSTRAP_PASSWORD`——仅在空数据库首次启动时需要，用于创建初始 `admin`
  账户。

首次启动后修改 `APP_BOOTSTRAP_PASSWORD` 对已有账户无效。引导账户行为见
[安全说明](security.md)。

## 容器

如果你将应用容器化，使用 JRE 基础镜像并以非 root 用户运行。VAdmin 本身不需要任何
特殊容器配置——它是标准的 Spring Boot fat JAR。

VAdmin 仓库的 `Dockerfile` 可作为模板参考：用 JDK 阶段构建，最终镜像在 JRE 上以
UID `10001` 运行，并设置 `SPRING_PROFILES_ACTIVE=prod`。

## 备份与恢复

VAdmin 将访问控制和审计数据持久化在 PostgreSQL 中。恢复必须包含数据库及其 Flyway
schema history。如果你的业务模块存储文件或其他外部数据，需要在切换生产前单独记录
并演练其备份与恢复。
