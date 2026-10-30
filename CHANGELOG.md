# Changelog

## 1.0.0 (2026-10-29)

### 修复

- Redis WorkerId 租约 renew/release 增加 token fencing，避免租约被接管后重复 WorkerId；
- Redis 序列按持久/日切 scope 处理 TTL，日切刷新、NEVER 不过期；
- ZK 续约校验 ephemeral owner，释放仅作用于本实例租约；
- KSID close 与生成/续约同步，避免关闭窗口重复；
- BIZ memory provider 按工厂共享，隐式 default 不再覆盖显式配置；
- UUIDv7 非单调回拨使用当前时间戳；UUID 解析器校验 canonical form 与 RFC variant；
- 批大小配置接入、配置解析/健康/指标边界加固、发布工作流权限与版本校验。

### 新增

- 开源文档与发布准备：SECURITY/CONTRIBUTING/CODE_OF_CONDUCT/SUPPORT、Issue/PR 模板、兼容性/迁移文档、Javadoc 站点工作流；
- POM 增加 Central 所需的 licenses/developers/scm 元数据；
- P2 社区与文档工程：ROADMAP/GOVERNANCE/MAINTAINERS/CONTRIBUTORS、CODEOWNERS、Dependabot 分组、GitHub Release Notes 分类、第三方许可证说明、文档链接/结构检查工作流；
- 统一 API（`IdGenerator`/`IdGeneratorRegistry`）、SPI、异常体系、IdParser；
- UUIDv7、UUIDv8、ULID、KSID（Snowflake）、业务 ID（BIZ）生成器；
- KSUID、TypeID 可选兼容模块；
- knitid-spring-boot-starter-all 全家桶；
- WorkerId 分配：fixed/environment/Redis/ZooKeeper/Kubernetes；
- Redis 序列提供者（BIZ 多实例序列）；
- Spring Boot 4 自动配置、严格配置校验、FailureAnalyzer；
- Actuator 端点、健康检查、Micrometer 指标；
- JMH 基准、示例工程、配置参考与发布流水线；
- 197 个测试（含 Redis/ZooKeeper Testcontainers）。
