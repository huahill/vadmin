# 发布指南

VAdmin 的发布坐标为 `io.github.huahill:vadmin-spring-boot-starter`。

简体中文 | [English](../../en/dev/release-guide.md)

## 范围

本指南定义 `0.2.0` 版本。该版本将 `vadmin-spring-boot-starter` 作为受支持的接入制品：
默认外壳和主题、系统管理、模块组装，以及使用方所需的 Spring 适配器。

## 已验证兼容性基线

| 领域 | `0.2.0` 已验证基线 | 发布边界 |
| --- | --- | --- |
| JDK | Java 25 | 构建和运行使用方均要求 Java 25。 |
| 构建工具 | Maven Wrapper 4.0.0-rc-7 | 升级到 Maven GA 属于独立工作。 |
| 运行时 | Spring Boot 4.1.1 | Spring Boot 是唯一受支持的运行时。 |
| UI | Vaadin Flow 25.3.1 | Flow 是唯一 UI 模型；不包含 Hilla、React、Vue 或 TypeScript。 |
| 数据库与迁移 | PostgreSQL 18 Compose 基线，Flyway 13.7.0 | Testcontainers 覆盖不构成额外部署承诺。 |
| 外观 | Aura；跟随系统/浅色/深色 | Aura 与配色方案选择共享同一套 Flow、权限、模块和 i18n 契约。 |
| UI 语言 | `zh-CN`、`en-US` | VAdmin 与使用方模块资源都必须支持这两种语言。 |
| 外部身份 | 可选的标准 OIDC 授权码登录 | 提供商生命周期、SCIM、SAML、LDAP、MFA、租户和数据范围策略由使用方负责。 |

升级 Java、Maven、Spring Boot 或 Vaadin 必须使用独立 PR，更新兼容性证据，并完成普通和生产
验证。

## 使用方升级规则

1. 将所有 `io.github.huahill` 依赖升级到相同目标版本。不得混用已发布制品和
   snapshot 同级制品。
2. 使用 Java 25 运行使用方，并导入同一版本的 `vadmin-bom`。由它对齐 VAdmin 模块和 Vaadin 依赖；不要另外定义 Vaadin 版本或导入 `vaadin-bom`。`flow-maven-plugin` 是插件，BOM 不能管理它。插件版本必须等于该次 `vadmin-bom` 所导入的 Vaadin 平台 BOM 管理的 Flow 版本；它可能比平台版本号新一个补丁。不要把平台版本号或上表中的历史基线当作插件版本。
3. 在类生产数据库副本上审查 Flyway 迁移。每个迁移只执行一次，且不得改写已应用的迁移。
4. 保持文档化模块契约：`AdminModule` 元数据、已声明权限、路由、图标 key、`zh-CN` 与
   `en-US` 资源、没有 `@Route` 的 prototype View Bean，以及一个继承 `AdminModuleViewAnchor`、为每个使用方动态 View 声明 `@Uses` 的子类。不要把这些锚点放在 Spring Boot 应用类上。
5. 保留 VAdmin 默认外壳、主题和系统管理，除非使用方有意使用自己拥有的
   `AdminHostLayout`、`AppShellConfigurator` 和样式表完全替换外壳。
6. 保留外部提供的密钥和部署配置。更改 `APP_BOOTSTRAP_PASSWORD` 不会重置已有 `admin` 账户。
7. 重新运行授权、语言、外观和代表性业务流测试。OIDC 使用方还必须验证已有本地账户映射。

## 发布检查清单

在准备发布提交的干净检出中运行。记录命令输出、工具版本、镜像摘要、发布提交和标签。

### 源码与文档

- [ ] 清理有意生成的制品后，`git status --short` 为空。
- [ ] `git diff --check` 没有输出。
- [ ] README、架构、快速开始、扩展和发布指南一致说明：`vadmin` 提供默认
  外壳、主题和系统管理。
- [ ] 当前指南不包含已移除的示例领域引用，也不声称普通使用方必须提供外壳或默认
  `AdminHostLayout`。
- [ ] 根 `pom.xml` 使用发布版本；第一方 reactor 制品均解析到该精确版本。
- [ ] 许可证、第三方 notices 和内置图标归属信息仍包含在源码和发布归档中。

### 验证与打包

```bash
./mvnw -B -ntp verify
./mvnw -B -ntp -Pproduction verify
docker compose --env-file .env.example config
docker build -t vadmin:0.5.0 .
```

- [ ] 两条 Maven 命令均以 `BUILD SUCCESS` 结束。
- [ ] 单元、架构、Testcontainers、OIDC、基线浏览器和视觉 profile 浏览器套件均为零 failures、
  零 errors。
- [ ] 生产输出包含 VAdmin 系统页面和使用方动态模块路由，且不依赖 Vaadin 开发服务器。
- [ ] Compose 配置渲染时没有未解析变量。
- [ ] 镜像以非 root 运行时启动，并使用外部凭据连接全新的 PostgreSQL 18 服务。
- [ ] 评审者登录后切换语言和浅色/深色，打开 Users、Roles、Permissions 和 Audit，再确认
  按权限过滤的使用方模块页面能在 Aura 和所选色彩模式下正常显示。

## 发布

Maven 的 `release` profile 会要求非 SNAPSHOT 版本、附加源码和 Javadoc 归档、使用 GPG 签名
全部发布文件，并通过 Maven Central Publisher Portal 发布。日常开发和 CI 继续使用仓库内置的
Maven 4 Wrapper。Reactor 源码 POM 保持 model `4.1.0`：父坐标由 `<parent/>` 推断，子项目用
`<subprojects>` 声明。Maven Central 不接受这种构建模型。发布不会改写源码模型，只部署一棵
一次性的发布树：把它改写为 model `4.0.0`，把 `<subprojects>` 改为 `<modules>`，并根据
`relativePath` 为每个空的 `<parent/>` 补上父坐标。Apache Maven 3.9.16 只在这棵树上执行
Central `deploy`。生成的发布树不会提交，也不会打标签。

推荐的发布路径是推送不带预发布后缀的 annotated 稳定标签 `vX.Y.Z`。该推送会运行
`Dispatch stable release`。这个分发工作流没有 Central 凭据或签名密钥。它确认标签为
annotated，且提交可从 `main` 到达后，在 `main` 上启动受信任的 `Publish Release`，并传入
这个精确的 `release_tag`。发布 job 绑定到 `vadmin` Environment，因此该环境中的 secrets
和仓库级 secrets 均可用；上传 Maven Central 之前，由维护者批准该环境。`Publish Release`
拒绝工作流引用不是 `refs/heads/main` 的运行：它始终使用已审查的工作流定义，再检出不可变
标签作为源码。

像 `v0.5.0-RC1` 这样的预发布标签不会启动分发工作流。预发布和失败重试仍从 Actions 页面、
以 `main` 为工作流引用手动运行 `Publish Release`，并填写精确的 `release_tag`。该次运行
同样需要批准 `vadmin` Environment。

首次发布前，配置以下受保护的值，绝不可提交它们：

- `MAVEN_CENTRAL_USERNAME` 和 `MAVEN_CENTRAL_PASSWORD`：Maven Central Portal 发布凭据，
  SNAPSHOT 发布也使用这两个凭据。
- `GPG_PRIVATE_KEY`：ASCII-armored 格式的私有签名密钥。
- `GPG_PASSPHRASE`：私钥口令。

在 `Verify` 工作流成功后，从已验证的发布提交创建并推送 annotated `vX.Y.Z` 标签。稳定标签会按
上文自动开始发布。工作流会校验标签为 annotated、工作树干净、提交可从
`main` 到达，且与根 Maven 版本一致。它先用 Maven 4 Wrapper 签名并验证完整 reactor。如果
Central 上还没有该 starter，就下载 Apache Maven 3.9.16，从标签源码生成一次性发布树并部署。
工作流要求 Central Publisher 报告 deployment ID，等待 starter POM 可被公开下载，再通过干净的
Maven 使用方仓库解析该制品。只有这些检查全部成功后，才会创建或更新 GitHub Release，并以
annotated 标签 `vX.Y.Z` 作为发布标题，然后上传 Central 发布证据。

稳定发布成功后，`Prepare next SNAPSHOT` 会打开一个拉取请求，只把根 POM 的补丁版本加一，
例如从 `0.5.0` 到 `0.5.1-SNAPSHOT`。请审查并合并该请求后再继续开发。该工作流不推送到
`main`，不发布制品，也会跳过预发布标签。由标签分发启动的发布，会从受信任的发布工作流再次
启动这次准备，因为 GitHub 不会为 `GITHUB_TOKEN` 启动的运行投递 `workflow_run`。从 Actions
页面手动启动的发布仍使用 `workflow_run` 触发。仓库需要允许 GitHub Actions 创建拉取请求；
这些工作流不会批准或合并自己创建的拉取请求。

Central 接受某个版本后不得移动其标签。在版本尚未发布前，可以替换错误标签；最终标签必须为
annotated tag，且准确指向已验证的发布提交。

如确有必要在本地发布，凭据应只存在于本机 Maven `settings.xml` 中名为 `central` 的 server
条目，绝不可提交。请单独安装 Apache Maven 3.9.16，并用它的 `mvn` 部署一次性发布树。
`./mvnw -Pproduction,release deploy` 不会发布到 Central：Wrapper 可以为 reactor 签名，但
Central 会拒绝 model `4.1.0` 构建 POM。

```bash
scripts/generate-maven3-publication-tree.sh /tmp/vadmin-maven3-publication
cd /tmp/vadmin-maven3-publication
mvn -B -T 1 -Pproduction,release -DskipTests -DskipITs deploy
```

该 profile 会等待 Central Portal 发布完成。然后在干净的使用方项目中确认
`io.github.huahill:vadmin-spring-boot-starter:0.2.0` 可以解析。

## SNAPSHOT 发布

必须先在 Central Portal 中为 `io.github.huahill` 命名空间启用 SNAPSHOT 发布。对于推送到
`main` 的非文档变更，`Snapshot` GitHub Actions 工作流会自动运行，也可以从 Actions 页面
手动启动。该工作流使用 `vadmin` Environment，并复用正式发布工作流所用的
`MAVEN_CENTRAL_USERNAME` 和 `MAVEN_CENTRAL_PASSWORD` secrets。

工作流读取根 Maven 版本，只有版本以 `-SNAPSHOT` 结尾时才会部署。它使用标准 Maven deploy
生命周期和 Central Portal snapshots 仓库，不启用 `release` profile，也不进行 GPG 签名。

Maven 4 把 server id `central` 绑定到 `https://repo.maven.apache.org`。快照部署因此使用
单独的 server id `central-snapshots`，并且 settings 里必须声明同 id、指向快照仓库的
repository。继续复用 `central` 会丢掉凭据，Central 返回 HTTP 401：

```bash
./mvnw -B -ntp -DskipTests deploy \
  -DaltDeploymentRepository=central-snapshots::https://central.sonatype.com/repository/maven-snapshots/
```

普通 `Verify` 工作流仍是每次推送到 `main` 时的构建与测试门禁。SNAPSHOT 是可变的开发制品，
不得替代生产环境使用的正式版本。

使用方必须显式启用 Central Portal snapshots 仓库：

```xml
<repositories>
  <repository>
    <id>central-portal-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

## 发布后记录

记录发布版本与标签、提交 SHA、验证日期、Java/Maven/Spring Boot/Vaadin 版本、命令结果、
镜像摘要、已知限制，以及发布说明和 staged 或已发布制品链接。将记录保存在发布系统中，
不要写入源码控制的密钥或本地 shell 历史。
