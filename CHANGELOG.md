# Changelog

## 0.3.0 (2026-10-29)

### 新增

- 开源文档与发布准备：SECURITY/CONTRIBUTING/CODE_OF_CONDUCT/SUPPORT、Issue/PR 模板、兼容性/迁移文档、Javadoc 站点工作流；
- POM 增加 Central 所需的 licenses/developers/scm 元数据；
- P2 社区与文档工程：ROADMAP/GOVERNANCE/MAINTAINERS/CONTRIBUTORS、CODEOWNERS、Dependabot 分组、GitHub Release Notes 分类、第三方许可证说明、文档链接/结构检查工作流；
- 统一 API（`IdGenerator`/`IdGeneratorRegistry`）、SPI、异常体系、IdParser；
- UUIDv7、UUIDv8、ULID、KSID（Snowflake）、业务 ID（BIZ）生成器；
- WorkerId 分配：fixed/environment/Redis/ZooKeeper/Kubernetes；
- Redis 序列提供者（BIZ 多实例序列）；
- Spring Boot 4 自动配置、严格配置校验、FailureAnalyzer；
- Actuator 端点、健康检查、Micrometer 指标；
- JMH 基准、示例工程、配置参考与发布流水线；
- 160 个测试（含 Redis/ZooKeeper Testcontainers）。
