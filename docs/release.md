# 发布流程（1.0）

## 版本与分支

- 采用 SemVer；0.x 阶段允许破坏性变更，1.0 后遵循 SemVer。
- 发布前从 `main` 创建发布分支（或直接在 `main` 打 tag），执行 `mvn versions:set -DnewVersion=1.0.0` 并提交。
- 打 tag：`git tag v1.0.0 && git push origin v1.0.0`。

## 本地发布前检查

```bash
mvn -B -ntp clean verify
mvn -B -ntp -Prelease -DskipTests package   # 校验 javadoc/source/gpg 配置
```

需要 Docker（Testcontainers 的 Redis/ZooKeeper 用例）。

## Maven Central 发布

发布依赖：

1. `~/.m2/settings.xml` 中配置 server id `central` 的 Sonatype Central 用户名/Token；
2. GPG 私钥已导入，环境变量 `MAVEN_GPG_PASSPHRASE` 设置密码；
3. 执行：

```bash
mvn -B -ntp -Prelease clean deploy
```

`release` profile 启用：

- `maven-gpg-plugin` 签名所有构件；
- `central-publishing-maven-plugin` 自动发布；
- 所有模块附带 sources/javadoc jar。

## GitHub Actions

- `.github/workflows/ci.yml`：push/PR 构建，JDK 21，`mvn verify`（Ubuntu runner 提供 Docker）；
- `.github/workflows/docs-check.yml`：PR/push 检查文档结构、链接与代码围栏；
- `.github/workflows/release.yml`：推送 `v*` tag 时执行 `mvn -Prelease clean deploy`；
  - Secrets：`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`GPG_PRIVATE_KEY`、`MAVEN_GPG_PASSPHRASE`。

## 文档与 Javadoc 发布

- `docs.yml` 在 `main` 推送时执行 `mvn -DskipTests javadoc:aggregate`，并发布到 GitHub Pages；
- 聚合输出目录：`target/reports/apidocs`；
- 仓库 Settings → Pages 的 Source 选择 **GitHub Actions**；
- 页面地址：https://unitseek.github.io/knitid/（以实际仓库为准）。

## 发布检查清单

- [ ] 所有测试通过（含 Testcontainers）
- [ ] `node scripts/check-docs.mjs` 通过
- [ ] `mvn -Prelease clean deploy` 成功
- [ ] CHANGELOG 更新
- [ ] README/配置参考/Javadoc 更新
- [ ] 示例工程可运行
- [ ] 基准报告更新
- [ ] Maven Central 构件包含 sources/javadoc
- [ ] 发布说明（Release Notes）发布
