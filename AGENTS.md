# AGENTS.md

本文件面向在本仓库工作的编码智能体。人类贡献者请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 项目定位

KnitID 是 Java 21 的统一 ID 生成工具库，提供：

- UUIDv7、UUIDv8、ULID、KSID、BIZ、KSUID、TypeID；
- Spring Boot 4 自动配置、健康检查、指标、Actuator；
- Redis/JDBC 序列、Redis/ZooKeeper/Kubernetes WorkerId；
- 解析 API、JMH 基准、CLI。

当前版本：`1.0.0`。Maven 多模块工程，根 POM 使用 Spring Boot 4.1.2。

## 权威文档

修改行为前先读：

- [README](README.md)
- [配置参考](docs/configuration.md)
- [模块说明](docs/modules.md)
- [SPI 扩展](docs/spi.md)
- [兼容性](docs/compatibility.md)
- [CLI](docs/cli.md)
- [ADR](docs/design/adr/README.md)
- [AI 使用指南](docs/ai-usage.md)

## 构建与验证

```bash
# 默认 Spring Boot 4.1 + 全部模块
mvn -B -ntp verify

# Spring Boot 4.0 兼容矩阵（profile 增加兼容测试模块）
mvn -B -ntp -Pboot-4.0 verify

# 只测 CLI
mvn -B -ntp -pl knitid-cli -am test

# 文档链接/配置键检查
node scripts/check-docs.mjs
```

Native 需要 GraalVM JDK 21，普通 JDK 不能构建：

```bash
mvn -B -ntp -pl knitid-samples -am -Pnative -DskipTests package
```

## 架构约束

- `knitid-core` 只能依赖 JDK；不得引入 Spring 或第三方算法库。
- Spring 相关代码只放在 `knitid-spring-boot-autoconfigure` 或各模块的 `autoconfigure` 包。
- 生成器模块通过 `IdGeneratorFactory` 注册；不要在 core 中硬编码具体生成器。
- 新增模块必须同时更新：根 `pom.xml`、`knitid-bom`、README 模块表、docs/modules、测试计数、必要文档。
- 新公共 API 需要 Javadoc；保持 SemVer，避免破坏已发布契约。
- CLI 位于 `knitid-cli`，不得依赖 Spring；v1 不访问 Redis/数据库/WorkerId。
- 不在仓库写入密钥、passphrase、token 或真实连接串。

## 代码约定

- Java 21，4 空格缩进，不用 wildcard import。
- 公共类型/方法使用英文 Javadoc；注释解释“为什么”，不要复述代码。
- 异常使用现有 `IdErrorCodes` / 领域异常，不新增无语义错误码。
- 测试使用 JUnit 5 + AssertJ；新增行为必须有测试。
- 修改配置键、错误码、公共 API 时同步更新文档。
- 不要修改 `target/` 生成物；`*.asc`、私钥文件不得提交。

## 工作流

1. 先读相关文档和现有实现；
2. 先写/补测试，再改实现；
3. 运行受影响模块测试；
4. 完成前运行 `mvn -B -ntp verify`、`node scripts/check-docs.mjs`；
5. 报告实际命令和结果，不要凭推测声称通过；
6. 未得到明确指示时，不要 commit、push、tag 或改动远程。

## 常见陷阱

- `knitid-core` 引入 Spring 类型会破坏非 Spring/CLI 使用场景。
- KSID 默认 epoch 是 `2018-11-16T00:00:00Z`；已有 ID 的 epoch 变更属兼容性变更。
- BIZ/KSID parser 自动绑定只选默认/第一个匹配 generator；多布局需自定义 parser。
- JDBC 号段预留使用 `REQUIRES_NEW`，业务事务回滚不会回退号段。
- 指标同时有旧 tags（`type`/`name`）和 OTel 风格 tags（`id.type`/`id.name`），不要只删一侧。
- CLI v1 只支持无外部基础设施的类型，详见 [docs/cli.md](docs/cli.md)。
