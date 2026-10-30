# 发布流程（1.0）

## 版本与分支

- 采用 SemVer；1.0.0 起公共 API/SPI/配置键/输出格式遵循兼容承诺。
- 发布前从 `main` 创建发布分支（或直接在 `main` 打 tag），版本必须为 `1.0.0`。
- 打 tag：`git tag v1.0.0 && git push origin v1.0.0`。

## 发布前检查

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Prelease -DskipTests package
node scripts/check-docs.mjs
```

需要 Docker（Testcontainers 的 Redis/ZooKeeper 用例）。

## 本地与内部 Nexus（可选）

发布 POM 不再包含内部 Nexus 地址，也不会把内部仓库写入 Central 元数据。

如需继续使用内部 Nexus，请在个人/CI 的 `settings.xml` 中配置镜像或 repository，而不是修改根 POM：

```xml
<servers>
  <server>
    <id>nexus-releases</id>
    <username>YOUR_USER</username>
    <password>YOUR_PASSWORD</password>
  </server>
  <server>
    <id>nexus-snapshots</id>
    <username>YOUR_USER</username>
    <password>YOUR_PASSWORD</password>
  </server>
</servers>
```

本地安装模块供示例运行：

```bash
mvn -B -ntp -DskipTests install
mvn -pl knitid-samples spring-boot:run
```

## GPG 发布密钥准备

首次发布前在本机准备独立的发布密钥：

```powershell
pwsh -File scripts/setup-gpg-release.ps1
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

根 POM 已配置：

- `distributionManagement` 指向 `https://central.sonatype.com/api/v1/publisher`；
- `central-publishing-maven-plugin` 使用 server id `central`；
- 首次发布使用 `autoPublish=false`、`waitUntil=validated`，在 Portal 检查后人工 Publish；
- `maven-gpg-plugin` 在 `verify` 阶段签名；
- `knitid-samples`、`knitid-benchmarks` 的 `maven-deploy-plugin` 已设置 `skip=true`。

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
  - `release` job 使用 GitHub Environment（建议 required reviewers），并校验 tag/version。

## 文档与 Javadoc 发布

- `docs.yml` 在 `main` 推送时执行 `mvn -DskipTests javadoc:aggregate`，并发布到 GitHub Pages；
- 聚合输出目录：`target/reports/apidocs`；
- 仓库 Settings → Pages 的 Source 选择 **GitHub Actions**；
- 页面地址：https://unitseek.github.io/knitid/（以实际仓库为准）。

## 发布检查清单

- [ ] 所有测试通过（含 Testcontainers）
- [ ] `node scripts/check-docs.mjs` 通过
- [ ] `mvn -Prelease clean deploy` 成功
- [ ] Central Portal Namespace `org.unitseek` 已 Verified
- [ ] CHANGELOG 更新
- [ ] README/配置参考/Javadoc 更新
- [ ] 示例工程可运行
- [ ] 基准报告更新
- [ ] Maven Central 构件包含 sources/javadoc/asc
- [ ] 发布说明（Release Notes）发布
