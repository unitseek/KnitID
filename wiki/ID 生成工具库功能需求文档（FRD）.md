# ID 生成工具库功能需求文档（FRD）

**文档版本**：v0.3
**文档状态**：已评审（12 项关键决策已确认），待详细设计
**发布日期**：2026-10-29
**适用架构**：统一 API + 模块化自动配置
**目标产物**：`knitid-spring-boot-starter` 及配套算法模块、BOM、自动配置、文档与示例
**Maven 坐标**：groupId `org.unitseek.knitid`；基础包 `org.unitseek.knitid`

---

## 0. 文档信息

### 0.1 变更记录

| 版本 | 日期 | 变更内容 |
| --- | --- | --- |
| v0.1 | 2026-10-29 | 初稿：统一 API、模块化自动配置、五类 ID、SPI、NFR |
| v0.2 | 2026-10-29 | 按《ID 生成工具库 FRD 评审意见》整改：落实 12 项关键决策；解决 10 个阻断项；补充术语、用户故事、能力矩阵、装配模型、启动行为矩阵、配置参考、故障模式、安全、兼容矩阵、测试与验收、里程碑退出准则、决策记录；细化雪花/业务 ID/解析器/SPI/异常/指标 |
| v0.3 | 2026-10-29 | 将雪花 ID 更名为 **KSID**（Knit + Snowflake + ID）：类型常量、模块名、配置块、类名/包名与 FR 编号同步更名；v0.2 原文归档 |

### 0.2 评审整改对照（v0.1 阻断项 → v0.2）

| 编号 | v0.1 问题 | v0.2 处理 | 章节 |
| --- | --- | --- | --- |
| B-01 | 缺模块时"告警"与"启动失败"矛盾 | 统一为启动行为矩阵（7 种场景） | 7.5 |
| B-02 | 默认生成器装配顺序未定义 | 引入生成器工厂 SPI + 中央编排，明确装配顺序 | 5.3、7.2 |
| B-03 | 配置前缀/校验不完整 | 前缀改为 `knitid`，严格校验（未知属性失败） | 7.3、7.4、附录 A |
| B-04 | KSID 位分配/回拨/WorkerId 生命周期缺失 | 补位分配校验、回拨策略表、WorkerId 租约与释放 | 8.4 |
| B-05 | 业务 ID 唯一性/重置/校验位缺失 | 补模板语法、序列重置、溢出、校验算法、唯一性声明 | 8.5 |
| B-06 | 自研 vs 第三方库悬空 | 决策：核心算法自研，core 零第三方依赖 | 5.2、15 |
| B-07 | 性能目标与随机源/返回类型冲突 | 按生成器定义随机源与性能目标；明确 JMH 方法学 | 10.2 |
| B-08 | 兼容性表述不可执行 | 明确 4.0.x + 4.1.x 矩阵、Java 21、不支持 3.x | 10.1 |
| B-09 | P0 与里程碑冲突 | 明确 P0 = 1.0 发布范围，M1-M3 为交付顺序 | 1.2、13 |
| B-10 | 注册中心/SPI 缺生命周期 | 补生命周期、关闭顺序、SPI 契约表 | 6.10、9 |

### 0.3 文档约定

- **需求编号**：`FR-<域>-<序号>`；非功能需求为 `NFR-<序号>`。
- **优先级**：**P0** = 1.0 发布必须完成并通过验收；**P1** = 1.0 完成，资源不足时可顺延至 1.x（需评审同意）；**P2** = 非本期。
- **验收**：每条 P0 至少一条可执行验收；关键行为采用 Given/When/Then 描述（见 12.3）；测试用例与需求建立可追踪关系（`docs/traceability.md`）。
- **关键词**：必须（MUST）/应该（SHOULD）/可以（MAY），与 RFC 2119 语义一致。
- **行号**：本文档不引用行号；评审意见中的行号仅对应 v0.1。

---

## 1. 背景与目标

### 1.1 背景

业务中同时存在 UUID7、UUID8、ULID、KSID、业务 ID 等多种 ID 需求。若每种 ID 类型单独提供 Starter，会导致依赖膨胀、版本冲突、用户选型成本高、通用能力重复实现。

### 1.2 目标

提供一个 Spring Boot 工具库，满足：

1. **统一 API**：用户面向 `IdGenerator` / `IdGeneratorRegistry` 编程，不直接依赖具体算法。
2. **模块化自动配置**：每种算法独立 Maven 模块，按 classpath 自动装配。
3. **单一 Starter 入口**：用户只引入 `knitid-spring-boot-starter`，按需引入算法模块。
4. **配置驱动**：统一前缀 `knitid`，支持全局默认 ID 类型和多命名生成器。
5. **可扩展**：支持 WorkerId 分配、序列号提供、业务 ID 模板、时钟、随机源等 SPI；核心算法零第三方依赖。
6. **可观测**：提供指标、健康检查、日志与异常体系。
7. **可移植**：`knitid-core` 不依赖 Spring，可在非 Spring 环境使用；支持 GraalVM native。

**优先级语义**：本文 P0 指 1.0 发布范围（必须完成），与 M1/M2/M3 交付顺序无关（见 13）。

### 1.3 成功度量（1.0）

| 度量 | 目标 |
| --- | --- |
| 功能 | 5 类生成器、统一 API、自动配置、SPI、Actuator、指标全部可用 |
| 验收 | 所有 P0 需求的验收用例通过（`mvn verify` 绿） |
| 性能 | 达到 10.2 目标（JMH 报告入库） |
| 兼容 | Spring Boot 4.0.x / 4.1.x × Java 21 CI 矩阵通过 |
| native | 示例工程 GraalVM native 构建并启动通过 |
| 质量 | 公共 API 有 Javadoc；配置元数据完整；依赖树断言通过；无已知高危漏洞 |
| 文档 | README、配置参考、SPI 指南、迁移指南、基准报告齐全 |

### 1.4 非目标

- 不提供分布式强一致的全局序列数据库方案（仅 SPI 与示例）。
- 不提供非 Spring 环境的完整自动配置（仅提供零依赖 core 与静态门面）。
- 不提供图形化控制台、ID 管理后台、ID 反查服务。
- 不支持 Spring Boot 3.x 及更早版本。
- 不提供跨语言 SDK。
- 不保证"绝对唯一"（概率型生成器按概率唯一，见 8 与 10.3）。
- 不提供 ID 加密/脱敏算法本身（仅日志不打印完整 ID）。

---

## 2. 范围

### 2.1 本期范围（1.0）

- UUID7、UUID8（固定默认布局 + 预设）、ULID、自定义KSID、业务 ID。
- 统一生成 API、批量生成、注册中心、默认生成器、ID 解析。
- Spring Boot 自动配置、配置属性与严格校验、条件装配、启动行为矩阵。
- KSID：位分配、WorkerId 分配（固定/环境变量/Redis/ZooKeeper/Kubernetes）、时钟回拨策略。
- 业务 ID：模板化生成、序列来源、校验位、时区与日切。
- 指标、健康检查、Actuator 端点、异常体系、GraalVM native 支持。
- 单元/集成/并发/属性/基准测试、示例工程、文档。

### 2.2 不在本期

- 分布式强一致数据库序列实现（仅 SPI 与 Redis 示例）。
- 非 Spring Boot 环境的完整集成（core 可独立用，但无自动配置）。
- 图形化控制台。
- UUID8 任意位布局自由配置（仅预设）。
- 租户隔离的 ID 空间治理、配额与审计。

---

## 3. 术语表

| 术语 | 说明 |
| --- | --- |
| UUIDv7 | RFC 9562 定义的时间有序 UUID，48 位毫秒时间戳 + 74 位随机/计数器 |
| UUIDv8 | RFC 9562 的自定义布局 UUID，语义由应用定义，不保证跨系统互操作 |
| ULID | 26 位 Crockford Base32，48 位毫秒时间戳 + 80 位随机，可按字典序排序 |
| Monotonic ULID | 同毫秒内随机段递增的 ULID 变体，保证同生成器内单调递增 |
| KSID（Knit + Snowflake + ID） | 基于 Snowflake 算法的 64 位 long，由时间戳、数据中心、WorkerId、序列号拼接 |
| WorkerId | KSID 中标识生成节点的编号（含或不含数据中心位） |
| epoch | KSID 时间戳的起始纪元，决定可用年限 |
| 时钟回拨 | 系统时钟因 NTP 校时等原因向过去跳变 |
| BORROW_FUTURE | 时钟回拨策略之一：不等待，直接推进逻辑时间到未来 |
| Crockford Base32 | 排除 I/L/O/U 的 Base32 编码，ULID 使用 |
| 序列位 | KSID 中每毫秒内自增的位数 |
| 变体位 | UUID 中标识 RFC 变体的 2 位（10xx） |
| 唯一性范围 | 生成器保证不重复的边界：单 JVM / 集群 / 全局概率唯一 |
| 生成器工厂 | 算法模块向自动配置暴露的 `IdGeneratorFactory`，负责按配置创建生成器 |
| 逻辑时间 | KSID 生成器内部维护的"最后时间戳 + 序列"，可能领先于系统时钟 |

---

## 4. 用户故事与选型指南

### 4.1 典型用户故事

| 编号 | 用户故事 | 关联需求 |
| --- | --- | --- |
| US-01 | 订单服务需要数值型、可排序、高并发的 64 位主键，并能在多实例间自动分配 WorkerId | 8.4、9 |
| US-02 | 日志/事件服务需要可排序、字符串形式的 ID，写入对象存储后仍可按时间排序 | 8.3 |
| US-03 | 对外 API 需要跨系统、无中心协调的唯一 ID，数据库主键可直接使用 | 8.1 |
| US-04 | 运营需要可读的业务单号（如 ORD-20261029-000123-7），并要求可按前缀/日期检索 | 8.5 |
| US-05 | 内部系统需要自定义位布局的 UUID，但不要求跨系统互操作 | 8.2 |
| US-06 | K8s StatefulSet 部署的服务希望无需额外中间件即可获得稳定 WorkerId | 8.4、9 |

### 4.2 选型决策树

1. 需要 **数值型 long** 且追求性能 → **KSID**（需 WorkerId 规划）。
2. 需要 **可读业务单号** / 含前缀、日期、序列 → **BIZ**（需序列来源）。
3. 需要 **跨系统、标准格式** 且可接受字符串主键 → **UUID7**（推荐）或 **ULID**（更短、可排序）。
4. 需要 **自定义布局** 且明确不跨系统 → **UUID8**。
5. 需要按时间排序、存储紧凑、日志友好 → **ULID**。
6. 数据库主键偏好 UUID 且需要时间有序 → **UUID7**。
7. 安全敏感（ID 不可预测）→ 避免 BIZ 连续序列与 ULID 性能随机源；使用 UUID7 或 ULID 安全模式，并评估时间泄露风险（见 10.5）。

### 4.3 生成器能力矩阵

| 类型 | 字符串长度 | 字符集 | 时间精度 | 单调性 | 唯一性范围 | 可解析 | 默认随机源 | 典型用途 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UUID7 | 36（含连字符） | 十六进制 + 连字符 | 1 ms | 同毫秒单调（可配置） | 全局概率唯一 | 是（version=7） | SecureRandom | 数据库主键、跨系统 |
| UUID8 | 36（含连字符） | 十六进制 + 连字符 | 预设决定 | 预设决定 | 全局概率唯一 | 是（version=8） | SecureRandom | 内部自定义布局 |
| ULID | 26 | Crockford Base32 | 1 ms | 单调（可配置） | 全局概率唯一 | 是 | PerformanceRandom（可配 Secure） | 可排序 ID、日志/事件 |
| KSID | 19（十进制 long） | 数字 | 1 ms | 同节点严格递增 | 依赖 WorkerId 全局唯一 | 是（需位布局） | 无 | 高并发、数值主键 |
| BIZ | 模板决定（≤128） | 模板决定 | 取决于占位符 | 取决于序列 | 取决于序列来源 | 需模板 | 取决于占位符 | 可读业务单号 |

### 4.4 与现有方案的关系

| 方案 | 能力覆盖 | 主要局限 | 与 KnitID 的差异 |
| --- | --- | --- | --- |
| uuid-creator | UUID v1-v8 生成与解析 | 仅 UUID；无 Spring Boot 自动配置、无统一注册与可观测 | KnitID 覆盖多类型并提供统一 API、Starter、指标与健康检查 |
| ulid-creator | ULID 生成/解析 | 仅 ULID | 同上 |
| cosid | 分布式 ID（雪花、Segment 等）与 Spring Boot 集成 | API 与扩展模型不同；类型覆盖与本文范围不完全一致 | KnitID 的差异化在统一 `IdGenerator` API、五类 ID 模块化按需引入、零依赖 core 与统一可观测 |
| MyBatis-Plus IdentifierGenerator | ORM 内置 UUID/雪花 | 绑定 MyBatis-Plus，非通用 ID 库 | KnitID 与 ORM 解耦，可用于任意场景 |
| 自研工具类 | 按需实现 | 无统一契约、无 WorkerId 治理、无可观测、难以复用 | KnitID 提供契约、SPI 与运维能力 |

> 说明：上表用于说明定位差异，竞品能力以其官方文档为准；正式选型前需复核版本与许可证。

---

## 5. 总体架构

### 5.1 Maven 模块

| 模块 | 职责 | 是否必须 | 依赖 |
| --- | --- | --- | --- |
| `knitid-bom` | 统一依赖版本管理（`import` scope） | 是 | 无 |
| `knitid-core` | 统一 API、IdType、IdContext、异常、SPI、解析接口、工具、静态门面、固定/环境变量 WorkerId 分配 | 是 | 零第三方、零 Spring |
| `knitid-spring-boot-autoconfigure` | 配置属性、严格校验、中央编排、默认生成器、注册中心、Actuator、健康检查、指标 | 是 | core；Actuator/Micrometer 可选 |
| `knitid-spring-boot-starter` | 聚合 core + autoconfigure | 是，唯一 Starter | 上述二者 |
| `knitid-generator-uuid7` | UUID7 实现与生成器工厂 | 按需 | core；spring-boot-autoconfigure（provided） |
| `knitid-generator-uuid8` | UUID8 实现与生成器工厂 | 按需 | core；spring-boot-autoconfigure（provided） |
| `knitid-generator-ulid` | ULID 实现与生成器工厂 | 按需 | core；spring-boot-autoconfigure（provided） |
| `knitid-generator-ksid` | KSID 实现与生成器工厂 | 按需 | core；spring-boot-autoconfigure（provided） |
| `knitid-generator-biz` | 业务 ID 实现与生成器工厂 | 按需 | core；spring-boot-autoconfigure（provided） |
| `knitid-sequence-redis` | Redis 序列号提供者（业务 ID） | 按需 | core、spring-data-redis（optional） |
| `knitid-worker-redis` | Redis WorkerId 分配实现 | 按需 | core、spring-data-redis（optional） |
| `knitid-worker-zookeeper` | ZooKeeper WorkerId 分配实现（临时节点会话租约） | 按需 | core、Curator、ZooKeeper |
| `knitid-worker-kubernetes` | K8s WorkerId 分配实现（StatefulSet ordinal / Downward API） | 按需 | core |
| `knitid-test` | 测试工具：`MutableClock`、测试向量、断言工具 | 否（test scope） | core、JUnit 5 |
| `knitid-benchmarks` | JMH 基准，不发布 | 否 | 各算法模块 |
| `knitid-samples` | 示例工程 | 否（随源码交付） | starter + 各模块 |

### 5.2 依赖原则

1. `knitid-core` 零 Spring、零第三方依赖（仅 JDK 21）；核心算法自研，不包装第三方 ID 库。
2. 算法模块的 ID 算法实现仅依赖 `knitid-core`，不依赖任何第三方 ID/工具库；自动配置类可使用 `spring-boot-autoconfigure`（provided scope）注册工厂，不引入运行时第三方依赖。
3. 集成模块（Redis/ZooKeeper）允许引入客户端库，但必须：独立模块、`optional=true`、不影响未引入者启动、在文档中声明传递依赖。
4. `knitid-spring-boot-autoconfigure` 不编译依赖任何算法模块；通过 `IdGeneratorFactory` Bean 与 `ObjectProvider` 协作。
5. 主 Starter 不传递算法模块，避免依赖膨胀；用依赖树断言测试保证。
6. 第三方库版本（仅集成模块）由 `knitid-bom` 统一管理。
7. 自动配置使用 `@AutoConfiguration` 与 `AutoConfiguration.imports`（Spring Boot 4 机制）。
8. 所有模块的公共 API 必须在 `org.unitseek.knitid` 包下，内部实现放 `internal` 子包，不承诺兼容。

### 5.3 装配模型与顺序

**核心思想**：算法模块声明"工厂"，中央自动配置按配置"实例化生成器"，注册中心聚合，默认委托 Bean 提供 `@Primary` 注入。

1. 各算法模块的 `@AutoConfiguration` 注册一个 `IdGeneratorFactory` Bean（条件：`@ConditionalOnClass` 实现类、`@ConditionalOnMissingBean` 同名工厂）。
2. `KnitIdAutoConfiguration` 绑定 `@ConfigurationProperties(prefix="knitid")` 并做严格校验（启动期 fail-fast）。
3. 中央配置通过 `ObjectProvider<IdGeneratorFactory>` 与用户定义的 `IdGenerator` Bean 收集所有生成器来源。
4. 按配置创建生成器实例：默认生成器（`default-generator` / `default-type`）+ 命名生成器（`generators.<name>`）；名称与类型冲突立即失败。
5. 构建只读 `IdGeneratorRegistry`；创建 `@Primary` 的 `IdGenerator` 委托 Bean（`knitidDefaultIdGenerator`），委托到 registry 默认生成器。
6. 注册生命周期：按创建顺序登记 `AutoCloseable`，上下文关闭时逆序释放；`WorkerIdAssigner` 释放租约。
7. Actuator/健康检查/指标自动配置在 `@AutoConfigureAfter` 中央配置之后执行。

**装配顺序保证**：算法模块只依赖 `knitid-core` 与 `spring-boot-autoconfigure`（provided），不能反向依赖 `knitid-spring-boot-autoconfigure`，因此模块配置类不使用 `@AutoConfigureBefore(KnitIdAutoConfiguration.class)`；中央配置通过 `ObjectProvider`/`Map` 注入延迟解析工厂与用户 Bean，模块注册顺序不影响结果；Actuator/健康/指标等后置配置使用 `@AutoConfigureAfter(KnitIdAutoConfiguration.class)`。

### 5.4 包结构（约定）

| 模块 | 基础包 |
| --- | --- |
| core | `org.unitseek.knitid.core`（`api`、`spi`、`parser`、`worker`、`exception`、`util`） |
| autoconfigure | `org.unitseek.knitid.autoconfigure` |
| uuid7/uuid8/ulid/ksid/biz | `org.unitseek.knitid.generator.<type>` |
| sequence-redis / worker-redis / worker-zookeeper / worker-kubernetes | `org.unitseek.knitid.sequence.redis`、`org.unitseek.knitid.worker.<kind>` |
| test | `org.unitseek.knitid.test` |

---

## 6. 统一 API 功能需求

### FR-API-01 统一生成接口（P0）

```java
public interface IdGenerator {
    IdType type();
    String generate();
    String generate(IdContext context);
    default List<String> generateBatch(int size) { ... }
    default List<String> generateBatch(int size, IdContext context) { ... }
}
```

- 所有实现必须线程安全；`generate` 不得返回 `null`。
- 同一次批量调用内不得重复（在算法保证范围内，见 4.3 与 10.3）。
- 默认方法实现为循环调用；实现可以覆盖以优化（例如一次取多个序列号）。
- 可能阻塞的实现（KSID `WAIT`、业务 ID 依赖远程序列）必须在实现类 Javadoc 中声明"可能阻塞"，并在本文档对应章节说明。
- 验收：批量与逐条结果格式一致；`size <= 0` 抛 `IllegalArgumentException`；并发测试无重复。

### FR-API-02 数值型生成接口（P0）

```java
public interface NumericIdGenerator extends IdGenerator {
    long generateLong();
    long generateLong(IdContext context);
    default long[] generateLongBatch(int size) { ... }
    default long[] generateLongBatch(int size, IdContext context) { ... }
}
```

- KSID 实现该接口；`generate()` 返回 `Long.toString(generateLong())`。
- 数值与字符串转换无损：long 为正数范围；解析工具对超出范围/非数字输入返回失败（`Optional` 或异常，见 11）。
- 验收：与字符串结果一致；批量长度与内容正确。

### FR-API-03 批量生成与上限（P0）

- 默认上限 `IdGeneratorConstants.DEFAULT_MAX_BATCH_SIZE = 10_000`；Spring 环境可由 `knitid.limits.max-batch-size` 覆盖（默认 10_000）。
- 超过上限抛 `IllegalArgumentException`，消息包含上限值与配置 key。
- 批量语义：同一批内保持生成器的单调性；结果顺序稳定；不保证与单条交错调用之间的相对顺序。
- 验收：边界值 1 / 10_000 / 10_001；批量吞吐目标见 10.2。

### FR-API-04 ID 类型（P0）

- `IdType` 为不可变值对象，预置常量：`UUID7`、`UUID8`、`ULID`、`KSID`、`BIZ`。
- 支持 `IdType.of("CUSTOM_TYPE")` 供第三方/自定义生成器扩展。
- 字符串规范化：去首尾空白、转大写、忽略 `-` 与 `_`（如 `uuid-7` → `UUID7`）；合法值匹配 `[A-Z0-9][A-Z0-9_-]{0,63}`。
- `equals`/`hashCode` 基于规范化值；提供 `value()`、`toString()`。
- 验收：未知类型在配置绑定时失败；自定义类型可注册工厂并通过注册中心查找。

### FR-API-05 上下文（P1）

`IdContext` 不可变，由 Builder 创建，包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `businessType` | String | 业务类型标识，可空 |
| `prefix` | String | 覆盖业务模板中 `{prefix}` 的配置值，可空 |
| `tenantId` | String | 租户标识，预留给自定义生成器，可空 |
| `nodeId` | Long | 节点标识，预留给自定义生成器，可空 |
| `businessDate` | `LocalDate` | 业务日期，仅 BIZ 的日期占位符生效，可空 |
| `attributes` | `Map<String,Object>` | 不可变附加属性，可空 |

- 生成器不支持某字段时**忽略**，不抛异常；`null` 上下文等价于空上下文；未知属性忽略。
- 验收：不可变性测试；优先级测试（`context.prefix` 覆盖配置 `prefix`，见 8.5）。

### FR-API-06 注册中心（P0）

```java
public interface IdGeneratorRegistry {
    IdGenerator getDefault();
    IdGenerator get(String name);
    Optional<IdGenerator> find(String name);
    Optional<IdGenerator> find(IdType type);
    Set<String> names();
    Map<String, IdGenerator> asMap();
}
```

- 启动期构建，运行期只读；`get` 未找到抛 `IdGeneratorNotFoundException`，消息列出已注册名称。
- `names()` / `asMap()` 返回不可修改视图，顺序为注册顺序。
- 验收：默认/命名/按类型查找；不存在异常；并发只读安全。

### FR-API-07 默认注入（P0）

- Spring 环境在存在默认生成器时提供 `@Primary IdGenerator` 委托 Bean（`knitidDefaultIdGenerator`），用户可直接注入 `IdGenerator`。
- 用户自定义 `IdGenerator` Bean 时，自动委托 Bean 退让（`@ConditionalOnMissingBean(IdGenerator.class)`），此时由用户保证注入唯一性。
- 验收：仅引 Starter + 单模块时可注入；多模块未配置默认时启动失败（见 7.5）。

### FR-API-08 解析与信息（P1）

- `IdParser`：`Optional<IdInfo> tryParse(String raw)`；`IdInfo parse(String raw)` 失败抛 `IdParseException`。
- `IdInfo` 字段：`type`、`raw`、`timestamp`（`Instant`，可空）、`version`（可空）、`nodeId`（可空）、`sequence`（可空）、`businessType`（可空）、`attributes`（不可变）。
- 每种类型由独立 `IdTypeParser` SPI 实现；解析按优先级尝试，未识别返回 `Optional.empty()`。
- 默认解析顺序：UUID7/UUID8（含连字符或 32 位十六进制）→ ULID（26 位 Crockford）→ KSID（需已知位布局）→ BIZ（需已知模板）；可通过 `Ordered` 调整。
- 验收：UUID7/UUID8 版本、ULID 时间戳、KSID 位段、BIZ 模板解析；非法输入不抛非受控异常。

### FR-API-09 静态门面（P1）

- core 提供 `IdGenerators` 静态门面（非 Spring 使用/测试）：`register(String name, IdGenerator)`、`get(String)`、`getDefault()`、`reset()`，线程安全。
- 静态门面与 Spring 自动配置互不共享状态；文档明确二者关系。
- 验收：无 Spring 环境下可创建/获取生成器；并发注册安全。

### FR-API-10 生命周期（P0）

- 自动配置按创建顺序登记生成器与分配器；上下文关闭时逆序调用 `AutoCloseable.close()`，单个异常记录后继续。
- `WorkerIdAssigner.release` 必须被调用（优雅停机）；JVM 崩溃依赖租约 TTL 自动过期。
- 验收：关闭后资源释放（Redis 连接、租约删除）可测试（Testcontainers）。

### FR-API-11 线程安全与阻塞语义（P0）

- 所有生成器实例线程安全；公共 API 不返回内部可变结构。
- 阻塞行为必须在实现类 Javadoc 与本文档声明：KSID `WAIT`、BIZ 远程序列。
- 验收：并发测试（≥16 线程 × 10 万次）无重复/无异常；文档检查清单。

### FR-API-12 参数与边界（P0）

- `size <= 0`、`size > max`、名称/类型为 `null`：抛 `IllegalArgumentException`，消息含参数名与合法范围。
- 配置类错误抛 `IdConfigurationException`；运行期生成失败抛 `IdGenerationException` 及其子类（见 11）。
- 验收：异常类型/错误码/消息符合第 11 章。

---

## 7. 自动配置与配置

### FR-AUTO-01 单一 Starter 入口（P0）

- 用户引入 `knitid-spring-boot-starter` + 按需算法模块；starter 不传递算法模块。
- 仅引 starter 未引算法模块时的行为见 7.5。
- 验收：依赖树断言（starter 的 compile 依赖仅 core + autoconfigure）；样例工程覆盖组合。

### FR-AUTO-02 生成器工厂装配（P0）

- 算法模块注册 `IdGeneratorFactory`，条件：`@ConditionalOnClass` 实现类、`@ConditionalOnMissingBean`。
- 工厂接口（core）：

```java
public interface IdGeneratorFactory {
    IdType type();
    IdGenerator create(GeneratorConfig config);
}
```

- `GeneratorConfig` 是算法所需配置的只读视图（含 `name`、`type`、扁平化参数 map 或强类型访问器），由自动配置模块实现。
- 中央编排按 5.3 执行；任何工厂创建失败 → 启动失败，异常含生成器名/类型/原因。
- 验收：模块有无组合、工厂重复注册、创建失败提示。

### FR-AUTO-03 配置模型与前缀（P0）

- 前缀：`knitid`。
- 顶层属性：`enabled`、`default-type`、`default-generator`、`generators.<name>`、`limits`、`health`、`actuator`。
- 示例：

```yaml
knitid:
  enabled: true
  default-type: UUID7
  default-generator: order-id      # 可选，优先级高于 default-type
  limits:
    max-batch-size: 10000
  generators:
    order-id:
      type: KSID
      ksid:
        epoch: 2024-01-01T00:00:00Z
```

- `default-generator` 与 `default-type` 同时配置时以 `default-generator` 为准；都不配置时见 7.5。
- 验收：全属性绑定、默认值、优先级、元数据完整。

### FR-AUTO-04 严格校验（P0）

- 未知属性视为配置错误（`ignoreUnknownFields=false` 或等效自定义校验），启动失败并列出未知 key 与合法 key。
- `type` 未知或缺失 → 失败；`generators` 名称不匹配 `[A-Za-z0-9._-]{1,64}` → 失败；名称重复 → 失败。
- 生成器类型与配置块不匹配（如 `type: ULID` 却只配 `ksid`）→ 失败。
- 校验在启动期完成，不延迟到首次生成。
- 验收：错误配置矩阵（未知属性、未知类型、块不匹配、非法名称、空 generators）。

### FR-AUTO-05 默认生成器与启动行为（P0）

| 场景 | 已引入算法模块 | 默认配置 | 启动结果 | 默认 Bean | 健康检查 |
| --- | --- | --- | --- | --- | --- |
| A | 无 | 未配置 | 成功 | 无 | `UNKNOWN`（"未引入生成器模块"） |
| B | 无 | 配置 default-type/default-generator | 失败 | — | —（FailureAnalyzer：提示缺失 artifactId） |
| C | 1 个 | 未配置 | 成功 | 唯一生成器 | `UP` |
| D | ≥2 个 | 未配置 | 失败 | — | —（提示配置 default-type/default-generator，列出可用类型/名称） |
| E | ≥1 | 已配置 | 成功 | 指定生成器 | `UP`（WorkerId 异常时 `DOWN`） |
| F | — | 名称重复/类型冲突 | 失败 | — | —（冲突项与来源） |
| G | — | 配置非法（见 7.4） | 失败 | — | —（含 key 与合法值） |

- 场景 B/D 使用 `FailureAnalyzer` 输出可操作提示（缺失模块坐标/配置示例）。
- 验收：7 种场景的 `ApplicationContextRunner` 测试；提示文案断言。

### FR-AUTO-06 命名生成器（P1）

- `generators.<name>` 注册到 registry，名称即查找键；同类型多实例允许。
- 验收：`registry.get("order-id")` 返回对应生成器；重复名启动失败。

### FR-AUTO-07 用户 Bean 优先级（P1）

- 用户 `IdGenerator` Bean 参与 registry（名称 = Bean 名），优先级高于同名配置生成器（记录 WARN）。
- 用户 `IdGeneratorFactory` Bean 可覆盖同类型内建工厂（记录 INFO）。
- 默认委托 Bean 在用户定义任意 `IdGenerator` Bean 时退让。
- 验收：覆盖/共存测试。

### FR-AUTO-08 Actuator 端点（P1）

- 端点 id：`knitid`；URL：`/actuator/knitid`；暴露：`management.endpoints.web.exposure.include=knitid`。
- 响应 DTO：`default`（name/type）、`generators[]`（name、type、default、blocking、description）、`configVersion`（可选）。
- 条件：`@ConditionalOnAvailableEndpoint`、`@ConditionalOnClass(Endpoint.class)`；未引入 Actuator 不影响启动。
- 只读；WorkerId 默认不展示，`knitid.actuator.show-worker-id=true` 才展示。
- 验收：端点 JSON 契约测试；未暴露时 404；仅 JMX 时可用。

### FR-AUTO-09 健康检查（P1）

- 健康贡献者 Bean：`knitIdHealthIndicator` → 健康键 `knitId`。
- 状态映射：
  - 无生成器 → `UNKNOWN`；
  - 默认生成器存在且校验通过 → `UP`（details 含 type/name）；
  - WorkerId 租约失效/越界 → `DOWN`；
  - 瞬时时钟回拨（WAIT 已恢复）→ `UP` + `details.backwardsCount`；
  - 持续回拨超过阈值（`knitid.health.backwards-down-count` 默认 5，窗口 `knitid.health.backwards-down-window` 默认 1m）→ `DOWN`；
  - 检查异常 → `DOWN`（details 含原因，不含敏感信息）。
- 检查开销：结果缓存 `knitid.health.cache-ttl`（默认 5s）。
- 验收：状态矩阵测试；缓存生效测试。

### FR-AUTO-10 指标（P1）

- Micrometer 可选（`@ConditionalOnClass(MeterRegistry.class)`）；指标清单见 10.4。
- 生成热路径只打点预注册的 Timer/Counter；不做字符串拼接；标签值有界。
- 验收：指标名/标签/单位契约测试（`SimpleMeterRegistry`）。

### FR-AUTO-11 GraalVM native / AOT（P0）

- 支持 Spring Boot 4 native 构建；算法实现不得使用反射/动态代理；配置类通过 `@ConfigurationProperties` 与 AOT 处理。
- 自动配置模块提供 `RuntimeHintsRegistrar` 注册必要资源/反射（如有）。
- CI 增加 `native` profile：示例工程 `nativeCompile` + 启动冒烟。
- 验收：native 示例启动并生成 5 类 ID。

### FR-AUTO-12 配置元数据（P0）

- 使用 `spring-boot-configuration-processor`；元数据包含 description、defaultValue、deprecation（如适用）。
- CI 断言元数据覆盖所有属性。
- 验收：IDE 提示可用；元数据测试通过。

---

## 8. 各 ID 类型功能需求

### 8.1 UUID7

#### FR-UUID7-01 生成（P0）

- 按 RFC 9562 `5.7` 生成：48 位 Unix 毫秒时间戳 + 版本位（7）+ 变体位（10）+ `rand_a`（12 位）+ `rand_b`（62 位）。
- 字符串输出：小写、标准 36 位、8-4-4-4-12 格式；解析大小写不敏感。
- 随机源：`SecureRandom`（每线程持有实例或使用缓冲池，避免全局争用）。
- 验收：
  - 版本号 = 7；变体位符合 RFC；
  - 使用 RFC 9562 附录 A 测试向量校验字段布局；
  - 解析时间戳与生成时间误差 ≤ 1ms；
  - 并发（16 线程 × 10 万）无重复。

#### FR-UUID7-02 单调性（P0）

- 默认开启：同一毫秒内采用 RFC 9562 `6.2` Method 2（单调随机，随机段加 1），保证同生成器内严格递增。
- 同毫秒随机段用尽时，`monotonic-overflow: WAIT`（默认，等待下一毫秒）或 `THROW`（抛 `IdGenerationException`）。
- `monotonic: false` 时关闭单调，仅保证随机唯一（同毫秒不保证递增）。
- 验收：固定时钟下 10 万次严格递增；溢出行为符合配置；关闭单调后不承诺递增但无重复。

#### FR-UUID7-03 配置（P1）

- `knitid.generators.<name>.uuid7.monotonic`（默认 `true`）。
- `knitid.generators.<name>.uuid7.monotonic-overflow`（默认 `WAIT`）。
- 验收：配置生效；非法值启动失败。

### 8.2 UUID8

#### FR-UUID8-01 生成（P1）

- 按 RFC 9562 `5.8` 生成：版本位 = 8、变体位 = 10；`custom_a`（48 位）+ `custom_b`（12 位）+ `custom_c`（62 位）。
- 仅支持预设布局（决策 Q6）：

| 预设 | custom_a（48） | custom_b（12） | custom_c（62） | 说明 |
| --- | --- | --- | --- | --- |
| `TIME_RANDOM`（默认） | 毫秒时间戳 | 同毫秒单调计数 | 随机 | 时间有序，语义接近 UUIDv7 |
| `TIME_NODE_RANDOM` | 毫秒时间戳 | 数据中心 4 位 + Worker 8 位 | 随机 | 可定位节点，同毫秒不保证递增 |
| `RANDOM` | 随机 | 随机 | 随机 | 无时间语义 |

- 不支持任意位布局配置（非目标）；预设外配置启动失败。
- 文档必须警示：UUID8 语义由应用定义，不保证跨系统互操作，不建议作为公共契约 ID。
- 验收：版本/变体位；各预设字段解析；与 UUID7 不混淆；并发无重复。

#### FR-UUID8-02 配置（P2）

- `knitid.generators.<name>.uuid8.preset`（默认 `TIME_RANDOM`）；随机源固定 `SecureRandom`。
- 验收：非法预设启动失败；元数据包含预设枚举值。

### 8.3 ULID

#### FR-ULID-01 生成（P0）

- 26 位 Crockford Base32：48 位毫秒时间戳 + 80 位随机；输出大写为规范形式，解析大小写不敏感。
- 随机源（决策 Q7）：`random-source: PERFORMANCE`（默认，`ThreadLocalRandom`）或 `SECURE`（`SecureRandom`）。
- 时间戳超出 48 位范围时抛 `IdGenerationException`（错误码 `KNITID-2001`）。
- 非法字符（Crockford 排除的 `I`、`L`、`O`、`U`）在解析时拒绝。
- 验收：长度/字符集/时间戳解析/规范形式；非法字符拒绝；时间越界异常。

#### FR-ULID-02 单调模式（P0）

- `monotonic: true`（默认）：同毫秒内随机段 +1，保证同生成器内严格递增。
- 随机段用尽时：`monotonic-overflow: WAIT`（默认，等待下一毫秒）或 `THROW`。
- `monotonic: false`：同毫秒随机，不保证递增。
- 文档警示：`PERFORMANCE` 随机源不具备密码学安全性，不应用于安全令牌；安全场景必须配置 `SECURE`。
- 验收：固定时钟下严格递增；溢出行为；两种随机源切换。

#### FR-ULID-03 配置（P1）

- `knitid.generators.<name>.ulid.monotonic`（默认 `true`）。
- `knitid.generators.<name>.ulid.monotonic-overflow`（默认 `WAIT`）。
- `knitid.generators.<name>.ulid.random-source`（默认 `PERFORMANCE`）。
- 验收：配置矩阵；文档警示存在。

### 8.4 KSID（Snowflake 算法）

#### FR-KSID-01 生成与位布局（P0）

- 64 位正 long：1 位符号（0）+ 时间戳 + 数据中心 + WorkerId + 序列号。
- 默认位分配：时间戳 41、数据中心 5、WorkerId 5、序列 12，总和 63。
- 启动校验（任一失败即启动失败，异常含非法项、合法范围与配置 key）：
  1. `timestamp-bits + datacenter-id-bits + worker-id-bits + sequence-bits = 63`；
  2. `timestamp-bits ∈ [1,42]`；`sequence-bits >= 1`；`datacenter-id-bits >= 0`；`worker-id-bits >= 0`；
  3. 配置的 `datacenter-id` / `worker-id` 在对应位宽内；位宽为 0 时必须为 0；
  4. `epoch` 为 ISO-8601 `Instant`，且不晚于启动时刻；
  5. 启动日志与 Actuator 端点输出最大可用时间（epoch + 2^timestamp-bits 毫秒）。
- 每节点 QPS 理论上限：`1000 × 2^sequence-bits`；文档给出公式与示例。
- 验收：合法/非法位布局矩阵；位段解码；结果为非负 long；同毫秒内序列递增。

#### FR-KSID-02 时钟回拨与序列溢出（P0）

| 策略 | 触发条件 | 行为 | 等待上限 | 指标 | 默认 |
| --- | --- | --- | --- | --- | --- |
| `WAIT` | clock < lastTimestamp 且差值 ≤ max-wait | 阻塞至时钟追上（可中断） | `max-wait`（默认 5s），超时抛 `ClockBackwardsException` | `knitid.id.clock.backwards` | 是 |
| `THROW` | 任意回拨 | 立即抛 `ClockBackwardsException` | — | `knitid.id.clock.backwards` | 否 |
| `BORROW_FUTURE` | 任意回拨 | 保留逻辑时间继续发号，时间戳可能领先系统时钟 | 无 | `knitid.id.clock.backwards` + WARN 日志 | 否（默认禁用） |

- `BORROW_FUTURE` 风险：透支未来时间；若逻辑时间领先于系统时钟后重启，可能重复发号。启用需同时设置 `allow-borrow-future: true`，仅允许单机/可接受时间跳跃场景；本期不提供 lastTimestamp 持久化（见 14）。
- 时钟前跳：允许，进入新毫秒后序列归零。
- 同毫秒序列用尽（超过 `2^sequence-bits - 1`）：`sequence-overflow: WAIT`（默认，自旋至下一毫秒）或 `THROW`（抛 `IdGenerationException`）。
- 验收：可控 `Clock` 仿真回拨（1ms/1s/超过 max-wait）；WAIT 超时；BORROW_FUTURE 风险文案与告警日志；序列溢出两种策略。

#### FR-KSID-03 WorkerId 分配（P0）

| 方式 | 模块 | 说明 | 分配时机 | 冲突检测 |
| --- | --- | --- | --- | --- |
| `fixed` | core | 配置 `worker-id` / `datacenter-id` | 启动 | 无（运维保证） |
| `environment` | core | 环境变量 `KNITID_WORKER_ID` / `KNITID_DATACENTER_ID`（或同名系统属性） | 启动 | 无 |
| `redis` | worker-redis | Redis 原子占位 + TTL 续约；key 为 `<key-prefix>:<datacenter>:<worker>` | 启动，周期续约 | 有（SET NX） |
| `zookeeper` | worker-zookeeper | Curator 临时节点/临时顺序节点 | 启动，会话维持 | 有 |
| `kubernetes` | worker-kubernetes | StatefulSet 序号（`HOSTNAME` 后缀）或 Downward API | 启动 | 依赖编排唯一性 |

- 交付节奏：`fixed`/`environment` 于 M1 交付；`redis`/`zookeeper`/`kubernetes` 已完成（M2）；五种分配器均已实现。
- 选择顺序：显式 `worker-id-assigner` > 配置了 `worker-id` 则用 `fixed` > 失败并提示可选项。
- 契约：`WorkerId assign()` 可阻塞；失败抛 `WorkerIdUnavailableException` 并终止启动；`renew()` 周期续约；`release(WorkerId)` 优雅释放。
- 租约模块后台续约；连续续约失败 ≥ 3 次 → 健康检查 `DOWN`，后续生成请求抛 `IdGenerationException`（错误码 `KNITID-2007`），不静默降级。
- 配置：
  - Redis：`knitid.worker.redis.key-prefix`（默认 `knitid:worker`）、`lease-ttl`（默认 `60s`）、`renew-interval`（默认 `20s`）。
  - ZooKeeper：`knitid.worker.zookeeper.connect-string`、`namespace`（默认 `knitid`）、`session-timeout`（默认 `10s`）、`connection-timeout`（默认 `10s`）。
  - Kubernetes：`knitid.worker.kubernetes.mode`（`STATEFULSET_ORDINAL` 默认 / `DOWNWARD_API`）、`pod-name`（默认读取环境变量 `HOSTNAME`）。
- 验收：固定/环境变量；Redis、ZooKeeper 以 Testcontainers 测试抢占、续约、释放；Kubernetes 以模拟环境变量测试；越界/缺失启动失败。

#### FR-KSID-04 重启与时钟约束（P1）

- 部署必须使用 NTP/PTP 同源时钟，节点间最大偏差建议 ≤ 5s（写入运维文档）。
- 非 `BORROW_FUTURE` 模式下，重启后由系统时钟单调性保证不重复；启用 `BORROW_FUTURE` 时要求停机窗口或人工确认。
- 验收：仿真重启（新实例 + 固定 `Clock`）；运维文档含时钟要求。

#### FR-KSID-05 配置（P0）

```yaml
knitid:
  generators:
    order-id:
      type: KSID
      ksid:
        epoch: 2024-01-01T00:00:00Z
        timestamp-bits: 41
        datacenter-id-bits: 5
        worker-id-bits: 5
        sequence-bits: 12
        datacenter-id: 1
        worker-id: 1
        worker-id-assigner: fixed
        clock-backwards:
          strategy: WAIT
          max-wait: 5s
        sequence-overflow: WAIT
        allow-borrow-future: false
        lease-renew-interval: 20s
        lease-max-renew-failures: 3
```

- 验收：全属性绑定/校验/元数据；非法组合启动失败。

### 8.5 业务 ID

#### FR-BIZ-01 模板（P0）

| 占位符 | 含义 | 约束 |
| --- | --- | --- |
| `{prefix}` | 业务前缀 | 配置 `prefix` 或 `context.prefix`；匹配 `[A-Za-z0-9_-]{1,16}` |
| `{yyyy}` `{MM}` `{dd}` `{HH}` `{mm}` `{ss}` | 日期时间分量 | 使用 `zone-id`（必填，见 FR-BIZ-05） |
| `{yyyyMMdd}` `{yyyyMMddHHmmss}` | 日期 / 日期时间 | 同上 |
| `{seq:n}` | 序列号，左补零，共 n 位 | n ∈ [1,18]；溢出见 FR-BIZ-03 |
| `{random:n}` | 随机段，字符集 `0-9A-Z` | n ∈ [1,32] |
| `{worker}` | 节点号 | 需 WorkerId 或 `node-id`；否则启动失败 |
| `{check}` | 校验位（1 位） | 算法见 FR-BIZ-04 |

- 转义：`{{` → `{`，`}}` → `}`；未知占位符、未闭合占位符 → 启动失败。
- 模板总长 ≤ 128；生成结果总长 ≤ 128；超出抛 `IdGenerationException`（错误码 `KNITID-2006`）。
- 模板为静态配置，不支持运行期传入。
- 验收：占位符矩阵；转义；非法模板启动失败；长度边界。

#### FR-BIZ-02 序列来源与唯一性（P0）

- `sequence-provider` 通过 Bean 名称引用 `SequenceProvider`；内建 `memory`；`redis` 由 `knitid-sequence-redis` 模块提供；数据库等由用户 SPI 提供。
- 重置策略：`reset: DAILY`（默认，按 `zone-id` 的本地日期）或 `NEVER`。
- scope：`DAILY` → `<prefix>:<yyyyMMdd>`；`NEVER` → `<prefix>`。
- 唯一性声明（文档必须为每个生成器声明实际范围）：

| 来源 | 唯一性范围 | 多实例 | 故障行为 |
| --- | --- | --- | --- |
| `memory` | 单 JVM | 不支持（各实例独立计数，可能重复） | 无外部依赖 |
| `redis` | 集群内（依赖 Redis 原子性与主从正确性） | 支持 | Redis 不可用 → 生成失败（不降级） |
| 数据库（SPI） | 由实现定义 | 支持 | 由实现定义 |

- 验收：日切重置；并发唯一；多实例模拟（Testcontainers Redis）。

#### FR-BIZ-03 溢出与并发（P0）

- `{seq:n}` 溢出（超过 `10^n - 1`）：默认抛 `IdGenerationException`（错误码 `KNITID-2005`）；不支持回绕或自动加长（避免重复与格式漂移）。
- 批量：调用 `SequenceProvider.next(scope, n)` 一次取 n 个，保证批内连续；实现不支持批量时退化为逐次并加锁。
- 验收：溢出抛错；批量连续；高并发无重复。

#### FR-BIZ-04 校验位（P1）

- `check-algorithm`：`MOD37_36`（默认，字母数字，ISO 7064 MOD 37,36）、`MOD11_2`（纯数字）、`LUHN`（纯数字）、`NONE`。
- `{check}` 最多出现一次，且必须位于模板末尾，否则启动失败。
- 校验位基于 `{check}` 之前的实际输出计算。
- 验收：各算法已知向量；非法校验位可被 `IdValidator` 检测（可选）。

#### FR-BIZ-05 时区与上下文（P0）

- `zone-id` 必填（如 `Asia/Shanghai`），无默认值；非法时区启动失败（决策 Q8）。
- `context.businessDate` 存在时覆盖日期占位符的"当前日期"（仅 BIZ；文档警示会影响唯一性/序列 scope）。
- `context.prefix` 覆盖配置 `prefix`；覆盖后序列 scope 使用实际 prefix。
- 验收：时区边界（23:59:59 → 次日 00:00:00 序列切换）；`businessDate` 覆盖；`prefix` 覆盖。

---

## 9. 扩展点 SPI

### 9.1 SPI 总表

| SPI | 所在模块 | 核心方法 | 线程安全 | 是否阻塞 | 失败语义 | 生命周期 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `IdGeneratorFactory` | core | `type()`、`create(GeneratorConfig)` | 是 | 否（启动期） | `IdConfigurationException` | 启动期调用一次 | P0 |
| `WorkerIdAssigner` | core | `assign()`、`renew()`、`release(WorkerId)` | 是 | `assign` 可阻塞 | `WorkerIdUnavailableException` | 启动获取、周期续约、关闭释放 | P0 |
| `SequenceProvider` | core | `next(scope)`、`next(scope, n)` | 是 | 可阻塞 | `IdGenerationException` / `SequenceUnavailableException` | 常驻 | P0 |
| `RandomProvider` | core | `nextBytes(byte[])`、`nextInt(int)`、`secure()` | 是 | 否 | 由实现定义（包装为 `IdGenerationException`） | 常驻 | P1 |
| `Clock` | JDK | `java.time.Clock`（可选 Spring Bean） | 是 | 否 | — | 常驻 | P1 |
| `IdFormatter` | core | `format(String, IdContext)` | 是 | 否 | `IdGenerationException` | 常驻 | P1 |
| `IdValidator` | core | `validate(String, IdContext)` | 是 | 否 | `IdValidationException` | 常驻 | P1 |
| `IdGeneratorCustomizer` | core | `customize(IdGenerator, name)` | 是 | 否（启动期） | `IdConfigurationException` | 启动期按 `Ordered` 调用 | P1 |
| `IdTypeParser` | core | `type()`、`tryParse(String)` | 是 | 否 | 非法输入返回 `Optional.empty()` | 常驻 | P1 |

### 9.2 契约明细

#### FR-SPI-01 注册与发现方式（P0）

- Spring 环境：SPI 通过 Spring Bean 注册生效；同类型多个 Bean 时按 `@Order`/`Ordered` 排序。
- 非 Spring 环境：通过构造器/`IdGenerators` 静态门面显式注册；不使用 `ServiceLoader`（避免 classpath 隐式行为）。
- 用户 Bean 覆盖内建实现时记录 INFO/WARN，不静默覆盖。
- 验收：Spring 与非 Spring 两种注册方式；覆盖行为可观测。

#### FR-SPI-02 `WorkerIdAssigner` 契约（P0）

- `assign()` 返回 `WorkerId`（含 `datacenterId`、`workerId`），可阻塞（如 Redis 占位重试）；获取失败抛 `WorkerIdUnavailableException`。
- `renew()` 由实现决定是否需要；内建 fixed/environment 为空实现。
- `release(WorkerId)` 必须幂等；关闭时调用；租约实现删除 key/节点。
- 一个 Assigner Bean 可能被多个生成器使用；实现必须支持多次 `assign()` 并返回互不冲突的 ID（fixed/environment 除外，需运维保证）。
- 验收：重复 assign、release 幂等、失败注入。

#### FR-SPI-03 `SequenceProvider` 契约（P0）

- `next(scope)` 原子自增并返回；`next(scope, n)` 返回长度 n 的连续区间（实现不支持时由调用方加锁退化）。
- 可阻塞（远端存储）；失败必须抛出，禁止返回重复值或静默回退到本地。
- 实现必须声明唯一性范围与重置语义（见 8.5）。
- 验收：并发唯一；批量连续；故障注入失败。

#### FR-SPI-04 时钟与随机源（P1）

- 时钟统一使用 `java.time.Clock`（Spring 可选 Bean）；生产默认 `Clock.systemUTC()`；测试使用 `MutableClock`（`knitid-test`）。
- `RandomProvider` 提供 `nextBytes`/`nextInt`；`secure()` 表示是否密码学安全；生成器按配置选择。
- 验收：注入固定时钟/随机源后输出可复现；随机源切换生效。

#### FR-SPI-05 格式化与校验（P1）

- 处理顺序：生成原始值 → `IdValidator.validate` → `IdFormatter.format` → 返回。
- 格式化不得改变 ID 的类型语义与唯一性（文档警示）；校验失败抛 `IdValidationException`。
- 验收：顺序测试；格式化异常映射。

#### FR-SPI-06 `IdGeneratorCustomizer` 契约（P1）

- `customize(IdGenerator generator, String name)` 在注册前调用一次，返回非 `null`；不得改变 `type()` 与唯一性保证。
- 支持包装（监控、限流、审计）；按 `Ordered` 顺序执行。
- 验收：顺序、包装生效、返回 null 失败。

#### FR-SPI-07 `IdTypeParser` 契约（P1）

- 每种类型一个解析器；`tryParse` 对非法输入返回 `Optional.empty()`，不抛异常。
- 解析器按 `Ordered` 顺序尝试；BIZ 解析器需模板配置，未配置时禁用。
- 验收：优先级、非法输入、模板缺失。

---

## 10. 非功能需求

### 10.1 NFR-01 兼容性（P0）

| 项 | 支持范围 |
| --- | --- |
| Java | **21**（编译 `release=21`，必须） |
| Spring Boot | **4.0.x、4.1.x**（CI 矩阵逐版本测试），不承诺 3.x 及更早 |
| Spring Framework | 7.x（随 Boot 4） |
| GraalVM | 21+（native 构建与冒烟在 CI `native` profile） |
| 操作系统 | 平台无关；CI 以 Linux 为主，Windows/macOS 可选 |
| 构建工具 | Maven 3.9+ |

- 4.1.x 为当前稳定线（最新 4.1.2）；4.0.x 支持至其 OSS EOL。
- 4.2 发布后 30 天内给出兼容性结论（支持或声明不支持）。
- 验收：CI 矩阵（Boot 4.0.x、4.1.x × Java 21）全绿；native 冒烟通过。

### 10.2 NFR-02 性能（P0）

| 生成器 | 单线程目标吞吐 | 随机源 | 说明 |
| --- | --- | --- | --- |
| KSID | ≥ 1,000,000 ID/s | 无 | 12 位序列理论上限 4.096M/s；String 返回为限制因素 |
| UUID7 | ≥ 500,000 ID/s | SecureRandom | 需按线程持有随机源，避免争用 |
| ULID（PERFORMANCE） | ≥ 500,000 ID/s | ThreadLocalRandom | 默认模式 |
| ULID（SECURE） | ≥ 300,000 ID/s | SecureRandom | 安全模式目标可下调，需实测 |
| BIZ（memory 序列） | ≥ 300,000 ID/s | 视模板 | 模板长度与随机段影响 |
| 批量 API | 吞吐 ≥ 单条 API | — | 同 size、同配置下比较 |

- 目标为 JMH 单线程吞吐（返回 `String`，含分配）；以基准报告实测为准，报告入库。
- 基准方法学：JMH 1.37+、JDK 21、8 vCPU/16 GB/Linux、`@Fork(3)`、`@Warmup(5 × 1s)`、`@Measurement(5 × 1s)`；报告记录硬件、JVM 参数、GC、ops/s 与 P99 延迟。
- 理论公式：KSID 每节点 `1000 × 2^sequence-bits`；UUID7 每毫秒最多 `2^74` 个（实际受随机源限制）；ULID 每毫秒最多 `2^80` 个。
- CI：每日夜间在专用 runner 跑基准；回归超过 10% 视为失败（噪声环境不设硬门槛）。
- 验收：基准报告达标；批量对比数据入库。

### 10.3 NFR-03 可靠性（P0）

**唯一性声明**：

| 生成器 | 唯一性保证 | 前提 |
| --- | --- | --- |
| UUID7 | 概率唯一（74 位随机/毫秒内单调） | 随机源正常；同毫秒不溢出或按策略等待 |
| UUID8 | 概率唯一 | 预设布局；随机源正常 |
| ULID | 概率唯一（80 位随机） | 随机源正常 |
| KSID | 确定性唯一 | WorkerId 全局唯一 + 时钟不倒退（或按策略处理） |
| BIZ | 由序列来源声明（单 JVM / 集群 / 全局） | 序列原子性；不跨来源混用 |

- 碰撞概率：UUID7 在 10^9 个 ID 量级下碰撞概率约 `n²/2^75`；ULID 约 `n²/2^81`；以此为文档说明，不承诺绝对唯一。
- 不静默降级：外部依赖（Redis/ZooKeeper/时钟）异常时必须失败并告警，禁止回退到本地随机/计数（会造成重复）。
- 线程安全：所有生成器实例线程安全；注册中心只读。
- 验收：概率模型文档；故障注入失败测试；并发唯一性测试。

### 10.4 NFR-04 可观测性（P1）

| 指标名 | 类型 | 标签 | 单位 | 说明 |
| --- | --- | --- | --- | --- |
| `knitid.id.generation.duration` | Timer | `type`、`name` | seconds（Micrometer 基准） | 生成耗时与次数，P50/P95/P99 |
| `knitid.id.generation.errors` | Counter | `type`、`name`、`reason` | — | reason：`CLOCK_BACKWARDS`、`WORKER_UNAVAILABLE`、`SEQ_EXHAUSTED`、`CONFIG`、`INTERNAL` |
| `knitid.id.clock.backwards` | Counter | `name`、`strategy` | — | 回拨次数；幅度可用 DistributionSummary |
| `knitid.id.worker.id` | Gauge | `name` | — | 当前 WorkerId；仅展示，不作为标签 |
| `knitid.id.batch.size` | DistributionSummary | `type`、`name` | — | 批量大小分布 |
| `knitid.id.parser.duration` | Timer | `type` | seconds | 可选，解析性能 |

- 标签基数：`name` 数量由配置决定（有界）；禁止把生成的 ID 值作为标签。
- 日志：不输出完整 ID；记录生成器名、类型、错误码、原因；WARN 记录 BORROW_FUTURE 与配置覆盖；INFO 记录自动选择的默认生成器；配置变更不产生日志（无动态刷新）。
- 健康检查：见 FR-AUTO-09。
- 验收：指标契约测试；日志脱敏检查。

### 10.5 NFR-05 安全（P0）

- UUID7/UUID8 随机源必须为 `SecureRandom`；ULID 默认 `PERFORMANCE`，文档必须警示其不适用于安全令牌（决策 Q7）。
- 时间可推断风险：UUID7/ULID 的时间戳可被解析，可能泄露生成时间；安全敏感场景需评估或改用业务 ID + 随机段（文档给建议）。
- BIZ 序列可枚举：连续序列可被推测；如需不可枚举，必须包含足够长度的 `{random:n}`（文档给建议与碰撞概率）。
- WorkerId 信任边界：Redis/ZooKeeper 分配具备互斥性；fixed/environment 依赖运维与部署隔离；文档说明伪造风险。
- 日志与指标不含完整 ID；异常消息不含 ID 值。
- 配置文件中不得存放密钥；集成模块凭据由 Spring 配置/环境变量提供。
- 验收：安全清单检查；ULID 警示文档存在；日志/指标脱敏测试。

### 10.6 NFR-06 文档（P0）

- README：快速开始、模块选择、最小配置、常见问题。
- 配置参考：全量属性（类型、默认值、取值范围、示例、变更影响）。
- SPI 指南：每个 SPI 的契约与示例实现。
- 迁移指南：从 `uuid-creator`、`ulid-creator`、`cosid`、MyBatis-Plus 雪花等迁移的映射与注意事项。
- 基准报告：硬件、方法、数据、结论。
- Javadoc：公共 API 全量；包级说明。
- GraalVM native 指南。
- 验收：文档评审；示例代码可编译。

### 10.7 NFR-07 可维护性与演进（P0）

- 版本策略：SemVer；0.x 阶段允许破坏性变更（需 CHANGELOG 说明）；1.0 后按 SemVer。
- **API 冻结**（决策 Q12）：M2 结束冻结公共 Java API 与配置 key；M3 只修 bug、补文档、性能与 native 验证，不新增/修改公共 API。
- 弃用策略：`@Deprecated` + Javadoc 说明替代方案；至少保留 1 个 minor 周期；配置 key 弃用需元数据标注。
- SPI 兼容：新增方法优先使用 `default` 方法；修改签名视为破坏性变更。
- 验收：冻结后 CI 增加 API 兼容性检查（如 japicmp 或 Revapi，M3 前接入）。

### 10.8 NFR-08 GraalVM native（P0）

- 支持 Spring Boot 4 native 构建；示例工程提供 `native` profile。
- 算法模块不使用反射/动态代理；自动配置提供 `RuntimeHintsRegistrar`（如需要）。
- 验收：native 构建成功、启动、5 类 ID 各生成 1 个。

---

## 11. 异常体系

### 11.1 异常层级

```text
IdException (RuntimeException)
├── IdConfigurationException        配置/启动错误
├── IdGenerationException           运行期生成失败
│   ├── ClockBackwardsException     时钟回拨且策略为 THROW 或 WAIT 超时
│   └── SequenceUnavailableException 序列来源不可用
├── WorkerIdUnavailableException    WorkerId 获取/续约/越界
├── IdGeneratorNotFoundException    命名生成器不存在
├── IdParseException                ID 解析失败
└── IdValidationException           自定义校验失败
```

- 全部为 `RuntimeException`（不引入受检异常）；保留 cause。
- `IllegalArgumentException` 仅用于 API 参数误用（`size`、`null` 名称）。

### 11.2 错误码

| 错误码 | 异常 | 场景 | 提示要求 |
| --- | --- | --- | --- |
| `KNITID-1001` | IdConfigurationException | 配置非法/未知属性/未知类型 | 列出合法值与配置 key |
| `KNITID-1002` | IdConfigurationException | 默认类型模块缺失 | 给出缺失 artifactId 与引入示例 |
| `KNITID-1003` | IdConfigurationException | 生成器名称/类型冲突 | 列出冲突项与来源 |
| `KNITID-2001` | IdGenerationException | 通用生成失败（含时间越界） | 生成器名/类型/原因 |
| `KNITID-2002` | ClockBackwardsException | 时钟回拨（THROW 或 WAIT 超时） | 差值、策略、等待上限 |
| `KNITID-2003` | WorkerIdUnavailableException | WorkerId 获取/续约失败/越界 | 分配方式、合法范围、租约状态 |
| `KNITID-2007` | IdGenerationException | WorkerId 租约在运行期失效 | 分配方式、最后续约时间、恢复建议 |
| `KNITID-2004` | SequenceUnavailableException | 序列来源不可用 | 来源名、scope、失败原因 |
| `KNITID-2005` | IdGenerationException | 序列溢出 | 当前值、位数、重置策略 |
| `KNITID-2006` | IdGenerationException | 模板输出超长 | 实际长度、上限 |
| `KNITID-3001` | IdParseException | 解析失败 | 输入类型、期望格式 |
| `KNITID-3002` | IdValidationException | 校验失败 | 校验器名、原因 |
| `KNITID-4001` | IdGeneratorNotFoundException | 生成器不存在 | 请求名称 + 已注册名称列表 |

### 11.3 消息格式

```text
[KNITID-xxxx] <message>; generator=<name>; type=<type>; action=<可操作建议>
```

- 消息不包含完整 ID（如必须包含，仅保留前 8 位 + 长度）。
- 异常到指标的映射：`reason` 由错误码映射（见 10.4）。
- 验收：每个错误码至少一个测试；消息含 `generator`/`type`/`action`；不含敏感 ID。

---

## 12. 测试与验收

### 12.1 测试类型

| 类型 | 范围 | 工具/方式 | 门禁 |
| --- | --- | --- | --- |
| 单元测试 | 每个算法格式、位段、边界、校验位 | JUnit 5 + AssertJ | 必须 |
| 属性测试 | 格式不变量、单调性、唯一性 | jqwik | 必须 |
| 规范向量 | RFC 9562 附录 A、ULID 规范向量、交叉实现比对 | 单元测试 | 必须 |
| 自动配置测试 | 模块组合、启动行为矩阵、用户 Bean 覆盖、非法配置 | `ApplicationContextRunner` | 必须 |
| 集成测试 | Starter + 各算法模块 + Actuator + 示例 | Spring Boot Test | 必须 |
| 并发测试 | 多线程、多进程唯一性 | JUnit + 多 JVM 脚本 | 必须 |
| 时钟故障 | 回拨、前跳、重启、序列溢出 | `MutableClock`（`knitid-test`） | 必须 |
| 外部依赖 | Redis/ZooKeeper 抢占、续约、故障 | Testcontainers | 必须（集成模块） |
| fuzz | 模板解析、IdParser 随机输入 | jqwik | 必须 |
| 架构约束 | core 零 Spring、autoconfigure 不依赖算法模块 | ArchUnit | 必须 |
| 依赖树 | starter 不传递算法模块；集成模块依赖 optional | Maven Enforcer / 断言 | 必须 |
| 性能基准 | 10.2 指标 | JMH（`knitid-benchmarks`） | 报告入库 |
| 兼容矩阵 | Boot 4.0.x / 4.1.x | CI matrix | 必须 |
| native | 构建 + 启动冒烟 | GraalVM native profile | 必须 |
| API 兼容 | 冻结后变更检查 | japicmp / Revapi（M3 前接入） | 必须 |

### 12.2 不变量与测试向量

- UUID7/UUID8：版本位、变体位、字段布局；RFC 9562 附录 A 向量；字符串格式与解析往返。
- ULID：长度 26、Crockford 字符集、时间戳解码、规范大写；单调模式下固定时钟严格递增；溢出策略。
- KSID：非负 long；位段解码；同毫秒序列递增；跨毫秒重置；回拨与序列溢出策略；位布局非法矩阵。
- BIZ：模板占位符矩阵、转义、长度边界、序列重置/溢出、校验位算法向量、时区边界。
- 注册/配置：启动行为矩阵 7 场景、未知属性/未知类型/块不匹配/名称冲突。
- 并发：≥16 线程 × 10 万次无重复；批量与单条混合无重复。
- 多进程：两个 JVM（不同 WorkerId）无重复；相同 WorkerId 时 Redis 分配器应阻止第二个启动（集成模块）。

### 12.3 验收样例（Given/When/Then）

**示例 1：FR-KSID-01 / FR-KSID-02**

> **Given** 位布局 timestamp=41、datacenter=5、worker=5、sequence=12，epoch 为 2024-01-01T00:00:00Z，WorkerId 固定为 1；
> **When** 单线程连续生成 100 万个 ID，并注入一次 100ms 时钟回拨（策略 WAIT）；
> **Then** 所有 ID 为正 long；相邻 ID 严格递增；回拨期间阻塞不超过 max-wait 后恢复；解析出的时间戳落在 [开始时间, 结束时间] 内；每毫秒序列不超过 4095；吞吐达到 10.2 目标；指标 `clock.backwards` 增加 1。

> **Given** 位分配总和为 62、sequence=0 或 worker-id 越界的配置；**When** 应用启动；**Then** 启动失败，异常含 `KNITID-1001`、非法项、合法范围与配置 key。

**示例 2：FR-UUID7-02**

> **Given** 单调开启、固定时钟（`MutableClock`）、同一毫秒内连续生成 10 万个 ID；
> **When** 比较相邻 ID；**Then** 严格递增（`String` 字典序与字节序一致）；版本位/变体位正确；随机段用尽时按配置 WAIT/THROW。

**示例 3：FR-BIZ-01 / FR-BIZ-03**

> **Given** 模板 `{prefix}-{yyyyMMdd}-{seq:6}-{check}`、prefix=ORD、zone-id=Asia/Shanghai、memory 序列、DAILY 重置；
> **When** 单实例生成 10 万个 ID；**Then** 格式为 ORD-8 位日期-6 位序号-1 位校验位；同一自然日内序号单调不重复；校验位可按 MOD37_36 验证；第 1,000,001 个（溢出）抛 `KNITID-2005`。

**示例 4：FR-AUTO-05**

> **Given** 仅引入 starter 与 `knitid-generator-uuid7`，未配置 default-type；**When** 应用启动；**Then** 启动成功，注入的 `IdGenerator` 类型为 `UUID7`，健康检查 `UP`；
> **Given** 同时引入 UUID7 与 ULID 模块且未配置默认；**When** 启动；**Then** 启动失败，提示可用类型列表与 `default-type` 配置示例。

### 12.4 质量门禁

- `mvn verify` 通过：单元/属性/集成/并发/架构/依赖树/元数据测试。
- 覆盖率：`knitid-core` 与算法模块行覆盖 ≥ 90%，整体 ≥ 80%（JaCoCo）。
- 自动配置矩阵测试覆盖率：7.5 的 7 个场景全部覆盖。
- 无已知高危依赖漏洞（OWASP dependency-check 或同类）。
- 公共 API Javadoc 完整（`mvn javadoc:javadoc` 无 warning）。
- 性能基准报告入库；回归超阈值需评审。
- native 冒烟通过。
- 文档：配置参考与元数据一致；示例可运行。

### 12.5 基准方法学（摘要）

- 工具：JMH 1.37+；平台：JDK 21、8 vCPU/16 GB/Linux（以报告记录为准）。
- 配置：`@Fork(3)`、`@Warmup(5 × 1s)`、`@Measurement(5 × 1s)`；单线程模式；返回 `String` 并消费结果（Blackhole）。
- 报告内容：ops/s、P50/P99 延迟、GC 次数、分配速率、JVM 参数、硬件信息。
- CI：夜间专用 runner；回归 > 10% 失败；不将绝对吞吐作为共享 runner 的硬门槛。

---

## 13. 交付物与里程碑

### 13.1 交付物

| 交付物 | 说明 |
| --- | --- |
| `knitid-bom` | 依赖版本管理 |
| `knitid-core` | 零依赖 core |
| `knitid-spring-boot-autoconfigure` | 自动配置、Actuator、健康、指标 |
| `knitid-spring-boot-starter` | 唯一 Starter |
| 算法模块 5 个 | uuid7、uuid8、ulid、ksid、biz |
| 集成模块 4 个 | sequence-redis、worker-redis、worker-zookeeper、worker-kubernetes |
| `knitid-test` | 测试工具（test scope） |
| `knitid-samples` | 示例工程（含 native profile） |
| `knitid-benchmarks` | JMH 基准（不发布） |
| 文档 | README、配置参考、SPI 指南、迁移指南、Javadoc、基准报告、native 指南、CHANGELOG |

### 13.2 里程碑与退出准则

**M1：基础能力（目标 4 周，依赖设计评审通过）**

- 范围：core（API/IdType/IdContext/Registry/异常/SPI 接口/静态门面/解析接口/fixed+environment WorkerId）、UUID7、KSID（fixed/environment）、autoconfigure、starter、BOM、示例。
- 退出准则：
  1. M1 范围内 P0 验收用例通过（单元、属性、并发、自动配置矩阵）；
  2. 示例工程 `mvn spring-boot:run` 可启动并生成 UUID7/KSID；
  3. 依赖树断言通过（starter 不传递算法模块）；
  4. 配置元数据生成且覆盖 M1 属性；
  5. CI（编译/测试/兼容矩阵基础）绿。

**M2：完整能力与 API 冻结（目标 4 周）**

- 范围：ULID、UUID8、BIZ、解析器实现、sequence-redis、worker-redis/zookeeper/kubernetes、Actuator、健康检查、指标、配置参考初版。
- 退出准则：
  1. M1+M2 范围内的 P0 验收通过（native 相关 P0 在 M3 验收）；P1 中 Actuator/健康/指标/解析通过；
  2. 自动配置 7 场景矩阵全绿；
  3. Redis/ZooKeeper 集成测试（Testcontainers）通过；
  4. 配置元数据完整（全属性）；
  5. **API 与配置 key 冻结**，进入变更控制；
  6. 基准测试首次全量运行并记录基线。

**M3：发布 1.0（目标 2 周）**

- 范围：GraalVM native、基准报告、Javadoc、迁移指南、CHANGELOG、API 兼容检查接入、缺陷修复。
- 退出准则：
  1. 10.1 兼容矩阵与 native 冒烟通过；
  2. 10.2 性能目标达成或经评审调整并记录；
  3. 12.4 质量门禁全绿；
  4. 发布检查清单（版本号、签名、SBOM、发布说明）完成；
  5. 1.0.0 发布。

- 周期为工作量估算，实际排期以项目计划为准；M3 只允许 bugfix 与文档/性能/native 工作，不允许新增公共 API 或配置 key（决策 Q12）。

### 13.3 P0/P1 与里程碑映射

| 阶段 | P0 | P1 |
| --- | --- | --- |
| M1 | core API/SPI/异常、UUID7、KSID 基础、autoconfigure/starter、BOM | — |
| M2 | ULID、BIZ、KSID 完整（Redis/ZK/K8s）、解析接口、默认/命名生成器、严格校验、配置元数据、异常体系 | UUID8、解析实现、Actuator、健康、指标、静态门面、上下文高级字段 |
| M3 | native 支持 | 基准报告、文档、迁移指南（非代码） |

---

## 14. 风险与约束

| 风险 | 影响 | 概率 | 应对 | Owner | 触发信号 |
| --- | --- | --- | --- | --- | --- |
| `BORROW_FUTURE` 导致重启重复 | 数据重复 | 低 | 默认禁用；需显式开启并接受单机约束；文档警示 | 架构 | 配置开启 |
| 节点时钟偏差超过 `max-wait` | 生成失败/阻塞 | 中 | NTP/PTP 同源；监控偏差；WAIT 超时告警 | 运维 | 回拨指标上升 |
| WorkerId 租约丢失 | 生成失败或冲突 | 中 | 续约失败 3 次即 DOWN；Redis/ZK 高可用 | 运维 | 健康 DOWN |
| Redis/ZooKeeper 不可用 | BIZ/WorkerId 获取失败 | 中 | fail-fast；不降级；容量与高可用要求 | 运维 | 依赖告警 |
| SecureRandom 性能不达标 | UUID7 目标未达 | 中 | 线程本地随机源；基准前置验证；必要时调整目标 | 开发 | 基准回归 |
| ULID 性能随机源可预测 | 安全风险 | 中 | 文档警示；安全场景用 `SECURE` | 安全 | 安全评审 |
| 时间戳泄露生成时间 | 信息泄露 | 中 | 选型指南警示；敏感场景评估 | 安全 | 安全评审 |
| API 冻结后范围蔓延 | 兼容性破坏 | 中 | 变更控制；M3 禁止新增 API | 架构 | 变更申请 |
| Boot 4.0/4.1 行为差异 | 兼容失败 | 低 | CI 矩阵；版本相关测试 | 开发 | CI 失败 |
| native 反射/AOT 问题 | native 构建失败 | 中 | RuntimeHints；native CI；提前在 M2 验证 | 开发 | native 冒烟失败 |
| 集成模块第三方依赖冲突 | 依赖冲突 | 中 | optional + 独立模块 + BOM；依赖树测试 | 架构 | 依赖树告警 |
| 性能目标未达成 | 发布延期 | 中 | M1 起持续基准；目标与实现约束同步评审 | 架构 | 基准报告 |
| 文档与实现漂移 | 使用错误 | 中 | 可追踪矩阵；文档评审；示例即测试 | 文档 | 评审发现 |

**约束**：

- 核心算法自研，`knitid-core` 与算法模块零第三方依赖（决策 Q3）。
- Java 21 与 Spring Boot 4.0.x/4.1.x（决策 Q4、Q5）。
- KSID 依赖 WorkerId 全局唯一与合理时钟，属部署约束（运维文档必须声明）。
- 业务 ID 的全局唯一性依赖所选序列来源，库本身不做跨来源去重。

---

## 15. 关键决策记录

| # | 决策项 | 决策 | 理由/影响 | 落地位置 |
| --- | --- | --- | --- | --- |
| Q1 | groupId / 包名 | `org.unitseek.knitid` / `org.unitseek.knitid` | 统一坐标与包；影响发布与导入 | 文档头、5.1、5.4 |
| Q2 | 配置前缀 | `knitid` | 避免与业务 `app.*` 冲突；所有属性以 `knitid` 开头 | 7.3、附录 A |
| Q3 | 自研 vs 第三方 | 核心算法自研；core 与算法模块零第三方依赖 | 可控、可测、无传递依赖；集成模块允许 optional 客户端（见 A-01） | 5.2、14 |
| Q4 | Spring Boot 支持 | 4.0.x + 4.1.x 矩阵；不承诺 3.x 及更早 | 与当前稳定线一致；降低双机制维护成本 | 10.1 |
| Q5 | Java 基线 | Java 21 | 与 Boot 4 生态一致；可用现代语言特性；用户面收窄已接受 | 10.1 |
| Q6 | UUID8 布局 | 固定默认布局 + 预设（TIME_RANDOM/TIME_NODE_RANDOM/RANDOM） | 控制兼容与测试成本；避免任意布局导致语义漂移 | 8.2 |
| Q7 | 随机源 | UUID7/UUID8 用 SecureRandom；ULID 可配置，默认 PERFORMANCE | 兼顾安全与吞吐；安全场景切换 SECURE | 8.1、8.3、10.5 |
| Q8 | BIZ 唯一性/时区 | 逐序列来源声明唯一性；`zone-id` 必填 | 避免"全局唯一"误解；防止环境间日期漂移 | 8.5、10.3 |
| Q9 | WorkerId 分配 | 首期支持 fixed/environment/redis/zookeeper/kubernetes | 覆盖单机、容器、中间件与 K8s 场景 | 8.4、9.2 |
| Q10 | GraalVM native | 支持 | 满足云原生部署需求；需 AOT/native 测试 | 10.1、10.8、13.2 |
| Q11 | 非 Spring 使用 | core 零 Spring + 可选静态门面 `IdGenerators` | 提升复用性；静态门面成本低 | 5.2、6.9 |
| Q12 | API 冻结 | M2 结束冻结公共 API 与配置 key；M3 只修 bug | 保证 1.0 稳定；变更可控 | 10.7、13.2 |

---

## 16. 假设与待确认事项

| # | 假设/待确认 | 当前处理 | 需确认人 |
| --- | --- | --- | --- |
| A-01 | Q3"零第三方依赖"指 core 与算法模块；Redis/ZooKeeper 集成模块允许 optional 客户端依赖 | 已按此设计（5.2、8.4） | 架构负责人 |
| A-02 | 发布渠道先私有 Nexus，1.0 前评估 Maven Central；许可证 Apache-2.0 | 待确认后写入发布文档 | 项目负责人 |
| A-03 | Kubernetes 分配以 StatefulSet 序号为主，Downward API 为备选 | 已按此设计（8.4） | 运维 |
| A-04 | BIZ 默认校验算法 `MOD37_36`、重置 `DAILY`、溢出 `THROW` | 已按此设计（8.5） | 产品/架构 |
| A-05 | 性能基准硬件：8 vCPU/16 GB/Linux/JDK 21 | 写入基准报告与 10.2 | 架构负责人 |
| A-06 | 里程碑周期 M1 4 周、M2 4 周、M3 2 周（1 名后端 + 兼职评审） | 作为估算，排期以项目计划为准 | 项目负责人 |
| A-07 | 代码托管与 CI 平台（GitHub/GitLab/私有） | 待定；影响 CI 配置与发布流程 | 项目负责人 |
| A-08 | UUID8 预设名称与字段定义（TIME_RANDOM 等） | 已按 8.2 定义；如需调整需在 M2 冻结前完成 | 架构负责人 |

---

## 附录 A 配置参考

> 前缀 `knitid`；类型一栏中 `Duration` 使用 Spring Boot 时长格式（如 `5s`、`1m`、`2d`）。

### A.1 根属性

| 属性 | 类型 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `knitid.enabled` | boolean | `true` | 否 | 总开关；关闭后不注册任何生成器 |
| `knitid.default-type` | String | 无 | 条件 | 默认生成器类型（如 `UUID7`）；与 `default-generator` 二选一 |
| `knitid.default-generator` | String | 无 | 条件 | 默认生成器名称（registry 名）；优先级高于 `default-type` |
| `knitid.limits.max-batch-size` | int | `10000` | 否 | 批量上限，必须 > 0 |
| `knitid.health.cache-ttl` | Duration | `5s` | 否 | 健康检查结果缓存 |
| `knitid.health.backwards-down-count` | int | `5` | 否 | 窗口内回拨次数达到即 `DOWN` |
| `knitid.health.backwards-down-window` | Duration | `1m` | 否 | 回拨计数窗口 |
| `knitid.actuator.show-worker-id` | boolean | `false` | 否 | 端点是否展示 WorkerId |

### A.2 生成器通用属性（`knitid.generators.<name>`）

| 属性 | 类型 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `enabled` | boolean | `true` | 否 | 是否创建该生成器 |
| `type` | String | 无 | 是 | `UUID7`/`UUID8`/`ULID`/`KSID`/`BIZ` 或自定义类型 |

### A.3 各类型属性

| 属性 | 类型 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `...uuid7.monotonic` | boolean | `true` | 否 | 同毫秒单调 |
| `...uuid7.monotonic-overflow` | enum | `WAIT` | 否 | `WAIT`/`THROW` |
| `...uuid8.preset` | enum | `TIME_RANDOM` | 否 | `TIME_RANDOM`/`TIME_NODE_RANDOM`/`RANDOM` |
| `...ulid.monotonic` | boolean | `true` | 否 | 同毫秒单调 |
| `...ulid.monotonic-overflow` | enum | `WAIT` | 否 | `WAIT`/`THROW` |
| `...ulid.random-source` | enum | `PERFORMANCE` | 否 | `PERFORMANCE`/`SECURE` |
| `...ksid.epoch` | Instant | `2024-01-01T00:00:00Z` | 否 | 纪元；修改属破坏性变更 |
| `...ksid.timestamp-bits` | int | `41` | 否 | 与其余位合计 63 |
| `...ksid.datacenter-id-bits` | int | `5` | 否 | — |
| `...ksid.worker-id-bits` | int | `5` | 否 | — |
| `...ksid.sequence-bits` | int | `12` | 否 | ≥ 1 |
| `...ksid.datacenter-id` | int | `0` | 否 | 位宽为 0 时必须为 0 |
| `...ksid.worker-id` | int | 无 | 条件 | `fixed` 方式必填 |
| `...ksid.worker-id-assigner` | enum | `fixed` | 否 | `fixed`/`environment`/`redis`/`zookeeper`/`kubernetes` |
| `...ksid.clock-backwards.strategy` | enum | `WAIT` | 否 | `WAIT`/`THROW`/`BORROW_FUTURE` |
| `...ksid.clock-backwards.max-wait` | Duration | `5s` | 否 | `WAIT` 上限 |
| `...ksid.sequence-overflow` | enum | `WAIT` | 否 | `WAIT`/`THROW` |
| `...ksid.allow-borrow-future` | boolean | `false` | 否 | 启用 `BORROW_FUTURE` 的显式开关 |
| `...ksid.lease-renew-interval` | Duration | `20s` | 否 | WorkerId 租约续约周期（仅 renewable 分配器） |
| `...ksid.lease-max-renew-failures` | int | `3` | 否 | 连续续约失败多少次后判定租约丢失 |
| `...biz.template` | String | 无 | 是 | 模板，≤ 128 字符 |
| `...biz.prefix` | String | 无 | 条件 | 模板含 `{prefix}` 时必填 |
| `...biz.zone-id` | String | 无 | 是 | 如 `Asia/Shanghai` |
| `...biz.node-id` | Long | 无 | 条件 | 模板含 `{worker}` 且未配置 WorkerId 时必填 |
| `...biz.sequence-provider` | String | `memory` | 否 | Bean 名 |
| `...biz.reset` | enum | `DAILY` | 否 | `DAILY`/`NEVER` |
| `...biz.check-algorithm` | enum | `MOD37_36` | 否 | `MOD37_36`/`MOD11_2`/`LUHN`/`NONE` |
| `...biz.seq-overflow` | enum | `THROW` | 否 | 固定 `THROW`（保留扩展） |

### A.4 集成模块属性

| 属性 | 类型 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- | --- |
| `knitid.sequence.redis.key-prefix` | String | `knitid:seq` | 否 | 序列 key 前缀 |
| `knitid.sequence.redis.ttl` | Duration | `2d` | 否 | 日切序列 key 过期时间 |
| `knitid.worker.redis.key-prefix` | String | `knitid:worker` | 否 | WorkerId 租约 key 前缀 |
| `knitid.worker.redis.lease-ttl` | Duration | `60s` | 否 | 租约 TTL |
| `knitid.worker.redis.renew-interval` | Duration | `20s` | 否 | 续约周期，必须小于 TTL |
| `knitid.worker.zookeeper.connect-string` | String | 无 | 条件 | 使用 ZK 分配时必填 |
| `knitid.worker.zookeeper.namespace` | String | `knitid` | 否 | 节点命名空间 |
| `knitid.worker.zookeeper.session-timeout` | Duration | `10s` | 否 | 会话超时 |
| `knitid.worker.zookeeper.connection-timeout` | Duration | `10s` | 否 | 连接超时 |
| `knitid.worker.kubernetes.mode` | enum | `STATEFULSET_ORDINAL` | 否 | `STATEFULSET_ORDINAL`/`DOWNWARD_API` |
| `knitid.worker.kubernetes.pod-name` | String | 无 | 否 | 默认读取环境变量 `HOSTNAME` |

---

## 附录 B 参考规范与文档

- RFC 9562（UUIDv7 5.7、UUIDv8 5.8、单调性方法 6.2）：https://www.rfc-editor.org/rfc/rfc9562
- ULID 规范：https://github.com/ulid/spec
- Crockford Base32 规范：https://www.crockford.com/base32.html
- ISO 7064（MOD 37,36 / MOD 11,2）：ISO/IEC 7064
- Spring Boot 4.x 系统要求（Java 17+；4.1.x 为当前稳定线）：https://docs.spring.io/spring-boot/4.0/system-requirements.html
- Spring Boot 自动配置：https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html
- Spring Boot Actuator 端点：https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Micrometer 指标命名：https://micrometer.io/docs/concepts#_naming_meters
- GraalVM Native Image：https://www.graalvm.org/latest/reference-manual/native-image/

---

**文档结束**：v0.3 已落实 12 项决策、整改 10 个阻断项，并将雪花 ID 正式命名为 KSID；剩余待确认事项见第 16 章，确认后进入详细设计与 M1 开发。
