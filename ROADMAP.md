# Roadmap

本路线图不构成发布承诺；优先级由维护者根据用户反馈、风险和维护成本决定。

## 1.0 — 稳定发布

### 核心能力

- [x] 七类生成器：UUIDv7、UUIDv8、ULID、KSID、BIZ、KSUID、TypeID
- [x] WorkerId：fixed、environment、Redis、ZooKeeper、Kubernetes
- [x] Redis 序列（BIZ 多实例）
- [x] Spring Boot 4 自动配置、严格配置校验、FailureAnalyzer
- [x] IdParser、Actuator 端点、健康检查、Micrometer 指标
- [x] JMH 基准、示例工程、P0/P1 文档、发布流水线
- [x] 220 个测试（含 Redis/ZooKeeper Testcontainers）
- [ ] 1.0.0 正式发布到 Maven Central
- [ ] 在 Java 21 固定硬件复测正式基准报告

### 用户成功与兼容性（原 1.1，并入 1.0）

以下条目原属 1.1，现并入 1.0 发布范围；完成前不正式发布 1.0.0。

- [ ] 文档站点与版本化文档（当前只有 Javadoc Pages）
- [x] Spring Boot 4.0/4.1 CI 兼容矩阵
- [x] 数据库号段 `SequenceProvider` 内置实现
- [x] BIZ 模板/KSID 位布局的内置解析器
- [x] 回拨指标 `knitid.id.clock.backwards`
- [x] GraalVM Native 集成测试
- [ ] Kotlin 使用示例与扩展

### 生态与性能（原 1.2，并入 1.0）

以下条目原属 1.2，现并入 1.0 发布范围。

- [x] OpenTelemetry 指标命名对齐
- [x] 异步/流式批量 API
- [x] Actuator 配置诊断（不泄露敏感信息）
- [x] 多线程/Redis 序列基准
- [x] 多租户/业务类型扩展示例
- [x] CLI v1.0.0（gen/parse，无 Spring、无外部基础设施）

## Backlog

- 与 Spring Cloud/Service Mesh 身份体系集成
- 数据库原生 UUIDv7 支持检测与优化建议
- 更多 WorkerId 分配器（etcd、Consul）
- 文档搜索与 API 变更对比

## 非目标

- 不兼容 Spring Boot 3；
- 不提供全局事务或跨系统强一致；
- 不替代密码学令牌/随机数；
- 不保证 BIZ 序列无空洞。

## 如何提议

请使用 GitHub Feature Request 模板，说明场景、约束、兼容性影响与替代方案。破坏性变更需要 ADR。
