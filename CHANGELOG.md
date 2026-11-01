# Changelog

本文件记录 KnitID 的公开版本变更，格式参考 Keep a Changelog，版本遵循 Semantic Versioning。

## 1.0.0 - 2026-11-01

首个公开版本。1.0.0 之前没有已发布的公共版本，因此本节不包含“从旧版本升级”的修复项，而是列出初始版本提供的能力和兼容性决定。

### 新增

- 统一 API：`IdGenerator`、`NumericIdGenerator`、`IdGeneratorRegistry`、`IdParser`；
- 七类 ID：UUIDv7、UUIDv8、ULID、KSID（Snowflake 风格）、BIZ、KSUID、TypeID；
- 批量、异步批量、流式 API，以及 `IdContext` 前缀/租户上下文；
- WorkerId 分配：fixed、environment、Redis、ZooKeeper、Kubernetes；
- Redis 序列与 JDBC 数据库号段序列，支持 BIZ 多实例；
- Spring Boot 4 自动配置、严格配置校验、FailureAnalyzer；
- Actuator 健康检查、生成器端点、脱敏配置诊断端点和 Micrometer 指标；
- BIZ 模板/KSID 位布局内置解析器，并在 Spring Boot 下自动绑定；
- JMH 基准、示例工程、CLI、发布流水线；
- CLI `knitid-cli` v1：`gen` / `parse`，文本与 JSON 输出，不依赖 Spring；
- 文档：配置、迁移、运维、排障、SPI、CLI、AI 使用指南、ADR。

### 可靠性保证

- Redis WorkerId 租约 renew/release 使用 token fencing，避免租约被接管后重复 WorkerId；
- Redis 序列按持久/日切 scope 处理 TTL，日切刷新、NEVER 不过期；
- ZooKeeper 续约校验 ephemeral owner，释放仅作用于本实例租约；
- KSID close 与生成/续约同步，避免关闭窗口重复；
- BIZ memory provider 按工厂共享，隐式 `default` 不覆盖显式配置；
- UUIDv7 非单调回拨使用当前时间戳；UUID 解析器校验 canonical form 与 RFC variant；
- 批大小配置、配置解析、健康/指标边界和发布工作流权限均加入校验；
- OSV 对 CycloneDX SBOM 扫描无已知漏洞，Curator test 的 Guava 升级到 33.7.2-jre。

### 变更

- KSID 默认 epoch 为 `2018-11-16T00:00:00Z`；
- 提供 Spring Boot 4.0 兼容测试模块与 CI 矩阵，默认构建使用 Spring Boot 4.1.x；
- 指标同时保留旧 tags（`type`/`name`）和 OTel 风格 tags（`id.type`/`id.name`）；
- 新增回拨指标 `knitid.id.clock.backwards`；
- `knitid-config` 端点仅暴露脱敏后的配置诊断信息；
- 多租户/业务类型示例通过 `IdContext.prefix` 覆盖 BIZ 前缀。

### 质量与兼容性

- 226 个单元/属性/Testcontainers 测试通过；
- Spring Boot 4.0 兼容矩阵另加 1 个启动测试；
- 支持 Java 21+，兼容 Spring Boot 4.0.x / 4.1.x；
- 不支持 Spring Boot 3；
- 1.0 起公共 API、SPI、配置键和 ID 输出格式遵循兼容承诺。
