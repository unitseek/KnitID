# Roadmap

本路线图不构成发布承诺；优先级由维护者根据用户反馈、风险和维护成本决定。

## 1.0.0 — 稳定发布

状态：核心代码、自动化测试、Docker Testcontainers、Spring Boot 4.0 矩阵、Native 冒烟和安全审查已完成；等待少量发布门禁后发布到 Maven Central。

### 核心能力

- [x] 七类生成器：UUIDv7、UUIDv8、ULID、KSID、BIZ、KSUID、TypeID
- [x] WorkerId：fixed、environment、Redis、ZooKeeper、Kubernetes
- [x] Redis 序列与 JDBC 数据库号段序列（BIZ 多实例）
- [x] Spring Boot 4 自动配置、严格配置校验、FailureAnalyzer
- [x] IdParser、BIZ/KSID 自动绑定解析器、Actuator 端点、健康检查、Micrometer 指标
- [x] 脱敏 `knitid-config` 配置诊断端点
- [x] 异步/流式批量 API、多租户/业务类型示例
- [x] OpenTelemetry 风格指标 tags、回拨指标
- [x] JMH 基准、示例工程、CLI v1
- [x] `AGENTS.md`、`llms.txt` 与 AI 使用指南

### 兼容性与质量

- [x] Spring Boot 4.0/4.1 CI 兼容矩阵
- [x] Java 21 构建与测试
- [x] 226 个测试（含 Redis/ZooKeeper Testcontainers）
- [x] Docker Testcontainers 全量执行（Redis 序列、Redis WorkerId、ZooKeeper WorkerId）
- [x] GraalVM Native 冒烟构建与运行
- [x] Actuator 健康异常脱敏、CLI UUID variant 校验
- [x] JDBC/Redis 自动配置顺序加固
- [x] CI checkout 关闭凭据持久化
- [x] 文档链接、测试计数、CLI/解析器/发布文档一致性检查

### 发布前门禁

- [x] `mvn -B -ntp verify`：226 个测试通过
- [x] `mvn -B -ntp "-Pboot-4.0" verify`：227 个测试通过（含 Boot 4.0 兼容测试）
- [x] `node scripts/check-docs.mjs`：40 个 Markdown 文件检查通过
- [x] Native smoke：构建、运行、三个生成器输出通过
- [x] 代码安全审查：密钥/SQL 注入/异常泄露/输入边界/自动配置顺序
- [ ] 文档站点与版本化文档（当前只有 Javadoc Pages）
- [ ] Kotlin 使用示例与扩展
- [x] OSV Scanner 对 CycloneDX SBOM 扫描：0 个已知漏洞
- [ ] 在 Java 21 固定硬件复测正式基准报告
- [ ] 1.0.0 正式发布到 Maven Central
- [ ] 发布后创建 GitHub Release Notes

## 1.x — 后续版本

- 文档搜索与 API 变更对比
- 更多 WorkerId 分配器（etcd、Consul）
- 数据库原生 UUIDv7 支持检测与优化建议
- 与 Spring Cloud/Service Mesh 身份体系集成
- CLI 后续能力：BIZ/KSID 本地生成、配置文件、批量解析
- GitHub Actions 固定到 commit SHA，进一步强化供应链安全
- 持续补充依赖 CVE 扫描策略

## 非目标

- 不兼容 Spring Boot 3；
- 不提供全局事务或跨系统强一致；
- 不替代密码学令牌/随机数；
- 不保证 BIZ 序列无空洞。

## 如何提议

请使用 GitHub Feature Request 模板，说明场景、约束、兼容性影响与替代方案。破坏性变更需要 ADR。
