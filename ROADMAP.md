# Roadmap

本路线图不构成发布承诺；优先级由维护者根据用户反馈、风险和维护成本决定。

## 1.0.0 — 稳定发布

状态：核心代码、自动化测试、Docker Testcontainers、Spring Boot 4.0 矩阵、Native 冒烟、安全审查、MkDocs 版本化文档和 Java 21 基准已完成；等待发布工程与 Maven Central 授权。

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
- [x] MkDocs 站点、版本化文档、Kotlin 示例、AI 使用指南

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
- [x] OSV Scanner 对 CycloneDX SBOM 扫描：0 个已知漏洞
- [x] Java 21 固定开发机 1.0.0 基准报告

### 发布前门禁

- [x] `mvn -B -ntp verify`：226 个测试通过
- [x] `mvn -B -ntp "-Pboot-4.0" verify`：227 个测试通过（含 Boot 4.0 兼容测试）
- [x] `node scripts/check-docs.mjs`：文档检查通过
- [x] Native smoke：构建、运行、三个生成器输出通过
- [x] 文档站点与版本化文档 workflow（首次推送后生效）
- [x] GitHub Actions 固定到 commit SHA
- [x] CI 定期 OSV 扫描 workflow（每周/手动）
- [ ] 1.0.0 正式发布到 Maven Central（需要推送/发布授权）
- [ ] 发布后创建 GitHub Release Notes（需要发布授权）

## 1.1 — CLI、兼容性与数据库建议

- [ ] CLI BIZ/KSID 本地生成（显式模板/布局，不依赖外部基础设施）
- [ ] CLI 配置文件支持（无 Spring 依赖）
- [ ] CLI 批量解析（stdin/JSON）
- [ ] API 变更对比与兼容性检查（japicmp/revapi）
- [ ] 数据库原生 UUIDv7 使用建议与迁移指南（文档优先）

> 文档搜索已由 MkDocs Material 内置搜索提供，不再作为独立 1.x 条目。

## Backlog

- Spring Cloud / Service Mesh 身份体系集成（需要具体场景与安全边界）
- 更多数据库 UUID 类型与索引优化建议
- 文档内容按用户反馈持续补充

## 非目标

- 不兼容 Spring Boot 3；
- 不提供全局事务或跨系统强一致；
- 不替代密码学令牌/随机数；
- 不保证 BIZ 序列无空洞。

## 如何提议

请使用 GitHub Feature Request 模板，说明场景、约束、兼容性影响与替代方案。破坏性变更需要 ADR。
