# 公开需求摘要

> 本文是面向使用者的需求摘要；内部评审记录不随仓库发布。

## 1. 背景与目标

业务系统需要多种 ID 形态（标准 UUID、可排序字符串、数字主键、业务单号），但各库 API、配置、WorkerId 方案不统一。KnitID 目标：

- 统一 `IdGenerator` API 与 Spring Boot 4 自动配置；
- 覆盖 UUIDv7/UUIDv8/ULID/KSID/BIZ，并提供 KSUID/TypeID 可选兼容模块；
- 多实例 WorkerId 分配与 Redis 序列；
- 可观测性（健康/指标/端点）与可扩展 SPI；
- 核心零 Spring 依赖，支持非 Spring 场景。

## 2. 非目标

- 不提供数据库、事务或全局锁；
- 不保证 BIZ 序列连续无空洞（生成失败会消耗序列）；
- 不替代密码学随机/令牌生成；
- 不兼容 Spring Boot 3；
- 不内置分布式事务/跨数据中心时钟同步。

## 3. 功能需求

| 编号 | 需求 | 验收 |
| --- | --- | --- |
| FR-GEN-01 | 统一字符串/数字生成接口、批量、上下文 | 契约测试通过 |
| FR-UUID7-01 | RFC 9562 UUIDv7，时间有序，单调模式 | 附录 A 向量、属性测试 |
| FR-UUID8-01 | 三种固定预设（TIME_RANDOM/TIME_NODE_RANDOM/RANDOM） | 版本/变体/布局测试 |
| FR-ULID-01 | 26 字符 Crockford Base32，单调/溢出策略 | 规范向量、属性测试 |
| FR-KSID-01 | 63 位布局可配置，回拨/序列溢出策略 | 位宽、策略、边界测试 |
| FR-KSID-02 | WorkerId fixed/environment/Redis/ZooKeeper/K8s | 各分配器测试 |
| FR-BIZ-01 | 模板、前缀、日切、校验位、上下文 | 模板/校验/并发测试 |
| FR-BIZ-02 | memory/Redis/自定义序列 | Redis 跨实例测试 |
| FR-KSUID-01 | 27 位 Base62 KSUID、可配置随机源 | 格式/时间戳/解析测试 |
| FR-TYPEID-01 | TypeID v0.3.0、多前缀、UUIDv7 | 前缀/编解码/解析测试 |
| FR-AUTO-01 | Spring Boot 4 自动配置、严格校验、默认选择 | 自动配置矩阵 |
| FR-AUTO-02 | 普通 starter 不含算法；starter-all 含七类算法 | starter/all 集成测试 |
| FR-OBS-01 | 健康/端点/指标 | 集成测试 |
| FR-PARSE-01 | 类型识别与元数据解析 | 解析器测试 |
| FR-DOC-01 | 文档、示例、基准、发布流程 | 本仓库文档 |

## 4. 非功能需求

| 类别 | 目标 |
| --- | --- |
| 性能（单线程开发机） | UUIDv7 ≥ 500k、ULID ≥ 500k、KSID ≥ 1M、BIZ ≥ 300k ops/s |
| 可靠性 | WorkerId 租约丢失后停止生成；Redis 序列失败不回退 |
| 可观测性 | 健康键、生成耗时/错误/批量/WorkerId 指标 |
| 兼容性 | Java 21、Spring Boot 4.0/4.1；Boot 3 不支持 |
| 安全 | SecureRandom 默认；不把可枚举 ID 当令牌 |
| 可扩展 | 工厂/WorkerId/序列/解析器/装饰器 SPI |

基准结果见 [基准报告](../benchmark-report.md)。

## 5. 约束

- 公共 API 在 1.0 后冻结；破坏性变更需 MAJOR；
- KSID 位宽/epoch 变更属于破坏性变更；
- 配置严格校验，未知键失败；
- 核心模块不得引入 Spring/算法三方库。

## 6. 验收与追溯

- 单元/属性/Testcontainers 测试 179 个；
- `mvn verify` 全绿；
- `docs/` 覆盖配置、迁移、运维、排障、SPI；
- 发布流水线见 [发布流程](../release.md)。
