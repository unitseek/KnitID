# 发布流程（1.0）

## 版本与分支

- 采用 SemVer；1.0.0 起公共 API/SPI/配置键/输出格式遵循兼容承诺。
- 发布前从 `main` 创建发布分支（或直接在 `main` 打 tag），版本必须为 `1.0.0`。
- 打 tag：`git tag v1.0.0 && git push origin v1.0.0`。

## 发布前检查

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Prelease -DskipTests verify
node scripts/check-docs.mjs
```

需要 Docker（Testcontainers 的 Redis/ZooKeeper 用例）。

首次执行 `-Prelease` 时需要可用的 GPG 发布密钥和 passphrase。若只想校验 POM/Central 配置而不签名：

```bash
mvn -B -ntp -Prelease -DskipTests -Dgpg.skip=true verify
```

## 内部 Nexus（仅本地开发可选）

发布 POM 不再包含内部 Nexus 地址，也不会把内部仓库写入 Central 元数据。

如果需要内部镜像，请在个人 `settings.xml` 中配置 mirror/profile，不要修改项目 POM。例如：

```xml
<mirrors>
  <mirror>
    <id>internal-nexus</id>
    <mirrorOf>central</mirrorOf>
    <url>https://nexus.example.com/repository/maven-public/</url>
  </mirror>
</mirrors>
```

如需认证，再为该 mirror id 配置 `<server>` 用户名/密码。仅做 Central 发布时不需要内部 Nexus。

本地安装模块供示例运行：

```bash
mvn -B -ntp -DskipTests install
mvn -pl knitid-samples spring-boot:run
mvn -B -ntp -pl knitid-cli -am -DskipTests package
java -jar knitid-cli/target/knitid-cli-1.0.0.jar --version
```

## GPG 发布密钥准备

首次发布前在本机准备独立的发布密钥：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup-gpg-release.ps1
```

脚本会：

- 定位 GnuPG；
- 必要时启动交互式密钥生成；
- 导出 `.asc` 公钥/私钥到 `~/.knitid-release`；
- 上传公钥到 keyserver；
- 打印 GitHub Environment secrets 配置命令。

脚本不会保存或打印 passphrase；私钥只生成在仓库目录之外。配置完 GitHub Secret 后删除本地私钥文件。

## Maven Central 发布

前置条件：

1. Central Portal Namespace `org.unitseek` 已验证；
2. GitHub Environment `release` 配置 required reviewers；
3. Secrets：`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`GPG_PRIVATE_KEY`、`MAVEN_GPG_PASSPHRASE`；
4. GPG 公钥已上传到 `keys.openpgp.org` / `keyserver.ubuntu.com`；
5. tag 必须等于 `v${project.version}`，且版本不得为 `SNAPSHOT`。

当前 `release` Environment 的实际配置：

- required reviewer：`unitseek`；
- 部署策略：仅 `v*` tag；
- Environment secrets：`GPG_PRIVATE_KEY`、`MAVEN_GPG_PASSPHRASE`、`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`。

根 POM 已配置：

- `distributionManagement` 指向 `https://central.sonatype.com/api/v1/publisher`；
- `central-publishing-maven-plugin` 使用 server id `central`；
- 首次发布使用 `autoPublish=false`、`waitUntil=validated`，在 Portal 检查后人工 Publish；
- `maven-gpg-plugin` 在 `verify` 阶段签名；
- `knitid-samples`、`knitid-benchmarks` 的 `maven-deploy-plugin` 已设置 `skip=true`；
- `knitid-cli` 作为可执行 JAR 随其他模块发布，构建时会生成带依赖的主 artifact。

执行：

```bash
mvn -B -ntp -Prelease clean deploy
```

发布后在 Central Portal 完成校验/发布，并创建 GitHub Release Notes。

## GitHub Actions

- `.github/workflows/ci.yml`：push/PR 构建，JDK 21，`mvn verify`（Ubuntu runner 提供 Docker）；
- `.github/workflows/docs-check.yml`：PR/push 检查文档结构、链接与代码围栏；
- `.github/workflows/release.yml`：推送 `v*` tag 时执行 `mvn -Prelease clean deploy`；
  - Secrets：`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`GPG_PRIVATE_KEY`、`MAVEN_GPG_PASSPHRASE`；
  - `release` job 使用 GitHub Environment `release`，并校验 tag/version。

## 文档与 Javadoc 发布

- `docs.yml` 构建 MkDocs 站点、聚合 Javadoc，并使用 `mike` 发布版本化文档到 `gh-pages`；
- 聚合 Javadoc 通过 `docs/site_hooks.py` 复制到站点 `apidocs/`；
- 仓库 Settings → Pages 的 Source 选择 **Deploy from a branch**，分支选择 `gh-pages`；
- `main` 发布 `dev` 版本，`v*` tag 发布对应版本并更新 `latest`；
- 页面地址：https://unitseek.github.io/knitid （以实际仓库为准）。

## 发布检查清单

- [ ] 所有测试通过（含 Testcontainers）
- [ ] `node scripts/check-docs.mjs` 通过
- [ ] `mvn -Prelease clean deploy` 成功
- [ ] Central Portal Namespace `org.unitseek` 已 Verified
- [ ] CHANGELOG 更新
- [ ] README/配置参考/Javadoc 更新
- [ ] 示例工程可运行
- [ ] CLI `java -jar knitid-cli/target/knitid-cli-1.0.0.jar --version` 可运行
- [ ] 基准报告更新
- [ ] Maven Central 构件包含 sources/javadoc/asc
- [ ] 发布说明（Release Notes）发布
