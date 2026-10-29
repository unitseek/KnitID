# ID 生成工具库功能需求文档（FRD）

**文档版本**：v0.1  
**适用架构**：统一 API + 模块化自动配置  
**目标产物**：`knitid-spring-boot-starter` 及配套算法模块、BOM、自动配置、文档与示例

---

## 1. 背景与目标

### 1.1 背景
业务中同时存在 UUID7、UUID8、ULID、雪花 ID、业务 ID 等多种 ID 需求。若每种 ID 类型单独提供 Starter，会导致依赖膨胀、版本冲突、用户选型成本高、通用能力重复实现。

### 1.2 目标
提供一个 Spring Boot 工具库，满足：

1. **统一 API**：用户面向 `IdGenerator` / `IdGeneratorRegistry` 编程，不直接依赖具体算法。
2. **模块化自动配置**：每种算法独立 Maven 模块，按 classpath 自动装配。
3. **单一 Starter 入口**：用户只引入 `knitid-spring-boot-starter`，按需引入算法模块。
4. **配置驱动**：支持全局默认 ID 类型和多命名生成器。
5. **可扩展**：支持 WorkerId 分配、序列号提供、业务 ID 模板、时钟等 SPI。
6. **可观测**：提供指标、健康检查、日志与异常体系。

---

## 2. 范围

### 2.1 本期范围
- UUID7、UUID8、ULID、自定义雪花 ID、业务 ID。
- 统一生成 API、批量生成、注册中心、默认生成器。
- Spring Boot 自动配置、配置属性、条件装配。
- 雪花 ID 的 WorkerId 分配与时钟回拨策略。
- 业务 ID 的模板化生成。
- 指标、健康检查、异常体系、测试与示例。

### 2.2 不在本期范围
- 分布式全局唯一序列的强一致数据库方案完整实现（仅提供 SPI 与 Redis/内存示例）。
- 非 Spring Boot 环境的完整集成。
- 图形化控制台。

---

## 3. 总体架构与模块划分

### 3.1 Maven 模块

| 模块                               | 职责                                                  | 是否必须         |
| ---------------------------------- | ----------------------------------------------------- | ---------------- |
| `knitid-core`                      | 统一 API、枚举、上下文、异常、SPI、工具类             | 是               |
| `knitid-spring-boot-autoconfigure` | 配置属性、自动配置、注册中心、Actuator                | 是               |
| `knitid-spring-boot-starter`       | 聚合 `knitid-core + knitid-spring-boot-autoconfigure` | 是，唯一 Starter |
| `knitid-generator-uuid7`           | UUID7 实现与自动配置                                  | 按需             |
| `knitid-generator-uuid8`           | UUID8 实现与自动配置                                  | 按需             |
| `knitid-generator-ulid`            | ULID 实现与自动配置                                   | 按需             |
| `knitid-generator-snowflake`       | 雪花 ID 实现与自动配置                                | 按需             |
| `knitid-generator-biz`             | 业务 ID 实现与自动配置                                | 按需             |
| `knitid-bom`                       | 统一依赖版本管理                                      | 是               |
| `knitid-samples`                   | 示例工程                                              | 否               |

### 3.2 依赖原则
1. 主 Starter 不强制依赖所有算法模块，避免依赖膨胀。
2. 算法模块依赖 `knitid-core`，并提供自身自动配置。
3. 所有第三方 ID 库（若有）版本由 `knitid-bom` 统一管理。
4. 自动配置使用 `@ConditionalOnClass` 按 classpath 生效。
5. 自动配置注册文件使用 Spring Boot 3.x 的 `AutoConfiguration.imports`。

---

## 4. 统一 API 功能需求

### FR-API-01 统一生成接口
**优先级：P0**  
提供：

```java
public interface IdGenerator {
    String generate();
    String generate(IdContext context);
    List<String> generateBatch(int size);
    IdType type();
}
```

验收：
- 所有算法实现该接口。
- 线程安全。
- `size <= 0` 抛出参数异常。
- 批量生成不得返回 null、重复项（在算法保证范围内）。

### FR-API-02 数值型生成接口
**优先级：P0**  
雪花 ID 等支持数值返回：

```java
public interface NumericIdGenerator extends IdGenerator {
    long generateLong();
    long generateLong(IdContext context);
}
```

验收：
- 雪花生成器实现该接口。
- 字符串与数值转换不丢失精度。

### FR-API-03 ID 类型枚举
**优先级：P0**  
```java
public enum IdType { UUID7, UUID8, ULID, SNOWFLAKE, BIZ }
```

### FR-API-04 生成器注册中心
**优先级：P0**  
提供 `IdGeneratorRegistry`：

```java
public interface IdGeneratorRegistry {
    IdGenerator getDefault();
    IdGenerator get(String name);
    Optional<IdGenerator> find(String name);
    Set<String> names();
}
```

验收：
- 支持默认生成器与命名生成器。
- 未找到时抛出明确异常。
- 支持运行时只读查询。

### FR-API-05 上下文与业务参数
**优先级：P1**  
`IdContext` 支持：
- `businessType`
- `prefix`
- `attributes`（Map<String,Object>）
- 预留租户、节点、业务日期等字段。

### FR-API-06 解析与校验能力
**优先级：P1**  
提供可选 `IdParser`：
- 判断 ID 类型。
- 提取时间戳、版本、随机段等元数据。
- 校验格式。

---

## 5. 自动配置与配置需求

### FR-AUTO-01 单一 Starter 入口
**优先级：P0**  
用户引入：

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>com.example</groupId>
  <artifactId>knitid-generator-snowflake</artifactId>
</dependency>
```

验收：
- 只引 Starter 不引算法模块时，核心 API 可用，默认生成器缺失时给出明确启动提示或健康检查告警。
- 引入算法模块后自动注册对应生成器。

### FR-AUTO-02 条件装配
**优先级：P0**  
每个算法模块提供自动配置，使用：
- `@ConditionalOnClass`
- `@ConditionalOnProperty(prefix="app.id", name="enabled", matchIfMissing=true)`
- `@ConditionalOnMissingBean`

验收：
- 未引入算法依赖时不报错。
- 用户自定义 Bean 优先于默认实现。

### FR-AUTO-03 配置属性
**优先级：P0**  
前缀：`app.id`。示例：

```yaml
app:
  id:
    enabled: true
    default-type: UUID7
    generators:
      order-id:
        type: SNOWFLAKE
        snowflake:
          worker-id: 1
          datacenter-id: 1
          epoch: 2024-01-01T00:00:00Z
          clock-backwards-strategy: WAIT
      user-id:
        type: ULID
      biz-id:
        type: BIZ
        biz:
          template: "{prefix}-{yyyyMMdd}-{seq:6}-{check}"
          prefix: ORD
```

验收：
- 支持全局默认类型。
- 支持多命名生成器。
- 配置缺失时使用安全默认值或明确报错。
- 提供配置元数据 `spring-configuration-metadata.json`。

### FR-AUTO-04 默认生成器
**优先级：P0**  
根据 `default-type` 注册 `@Primary` 默认 `IdGenerator`。

验收：
- 用户可直接注入 `IdGenerator`。
- `default-type` 对应模块未引入时启动失败并提示引入哪个模块。

### FR-AUTO-05 命名生成器
**优先级：P1**  
`generators.<name>` 注册到 `IdGeneratorRegistry`。

验收：
- `registry.get("order-id")` 返回对应生成器。
- 名称唯一，重复配置启动失败。

### FR-AUTO-06 Actuator 支持
**优先级：P1**  
提供：
- `/actuator/idGenerators`：列出生成器名称、类型、默认标记。
- 健康检查：默认生成器是否存在、WorkerId 是否有效、时钟是否回拨。

验收：
- 未引入 Actuator 时不影响启动。
- 健康检查可返回 `UP/DOWN/UNKNOWN`。

---

## 6. 各 ID 类型功能需求

### 6.1 UUID7
**FR-UUID7-01 P0**
- 生成 RFC 9562 UUIDv7。
- 48 位毫秒时间戳 + 版本位 + 随机段。
- 字符串格式标准 36 位。
- 支持 `SecureRandom`。
- 同一毫秒内保证单调递增或至少不重复（可配置策略）。

验收：
- 版本号 = 7。
- 变体位符合 RFC。
- 解析时间戳与生成时间误差在阈值内。
- 并发测试无重复。

### 6.2 UUID8
**FR-UUID8-01 P1**
- 生成 RFC 9562 UUIDv8。
- 默认布局：时间戳 + 自定义字段 + 随机段。
- 支持可配置字段布局与业务字段注入。
- 明确标注 UUID8 为自定义布局能力，非跨系统标准语义。

验收：
- 版本号 = 8。
- 变体位符合 RFC。
- 自定义布局配置生效。
- 文档明确兼容性边界。

### 6.3 ULID
**FR-ULID-01 P0**

- 26 位 Crockford Base32。
- 48 位时间戳 + 80 位随机。
- 支持单调 ULID 模式，同一毫秒内随机段递增。
- 支持大小写规范化输出。

验收：
- 长度 26，字符集合法。
- 按时间字典序有序。
- 并发无重复。
- 解析时间戳正确。

### 6.4 自定义雪花 ID
**FR-SNOW-01 P0**
- 64 位 long，正数。
- 位分配可配置：时间戳位、数据中心位、WorkerId 位、序列位。
- 支持自定义 epoch。
- 支持 `WAIT`、`THROW`、`BORROW_FUTURE` 时钟回拨策略。
- 支持 WorkerId 分配 SPI。

**FR-SNOW-02 P0 WorkerId 分配**
内置策略：

- 配置固定值。
- 环境变量/系统属性。
- 内存随机并校验范围（仅开发）。
- SPI 扩展：Redis、ZooKeeper、数据库、K8s 等。

验收：
- WorkerId 越界启动失败。
- 回拨按策略处理并记录指标。
- 单机基准测试达到文档承诺 QPS。

### 6.5 业务 ID
**FR-BIZ-01 P0**
- 模板化生成，支持占位符：
  - `{prefix}` 业务前缀
  - `{yyyyMMdd}`、`{yyyyMMddHHmmss}` 日期
  - `{seq:6}` 序列号，左侧补零
  - `{random:8}` 随机段
  - `{worker}` 节点
  - `{check}` 校验位
- 序列号来源 SPI：
  - 内存序列（单机）
  - Redis 序列（提供示例实现）
  - 数据库序列（SPI）
- 支持校验位算法可配置。

验收：
- 模板非法时启动失败。
- 同一业务前缀下序列不重复。
- 支持高并发批量生成。
- 校验位可验证。

---

## 7. 扩展点 SPI

| SPI                     | 用途               | 优先级 |
| ----------------------- | ------------------ | ------ |
| `WorkerIdAssigner`      | 雪花 WorkerId 分配 | P0     |
| `SequenceProvider`      | 业务 ID 序列号     | P0     |
| `Clock`                 | 可测试时钟         | P1     |
| `IdFormatter`           | 输出格式转换       | P1     |
| `IdValidator`           | 自定义校验         | P1     |
| `RandomProvider`        | 随机源             | P1     |
| `IdGeneratorCustomizer` | 生成器后置定制     | P1     |

验收：
- SPI 通过 Spring Bean 注册生效。
- 用户 Bean 覆盖默认实现。
- 文档提供示例。

---

## 8. 非功能需求

### NFR-01 兼容性
- 基线：Java 21 + Spring Boot 4.1.x。
- P1：兼容 Spring Boot 4.x。
- 提供 `knitid-bom` 管理版本。

### NFR-02 性能
- 雪花 ID：单机单线程 ≥ 100 万/秒，具体以基准测试报告为准。
- UUID7/ULID：单机单线程 ≥ 50 万/秒。
- 批量生成性能不低于逐条生成 80%。

### NFR-03 可靠性
- 生成器线程安全。
- 时钟回拨可观测、可配置。
- WorkerId 冲突检测。
- 关键异常包含类型、生成器名、配置提示。

### NFR-04 可观测性
指标：
- `id.generator.count{type,name}`
- `id.generator.duration{type,name}`
- `id.generator.error{type,name,reason}`
- `id.snowflake.clock_backwards.count`
- `id.snowflake.worker_id`

### NFR-05 安全
- 默认使用 `SecureRandom`。
- 不记录完整敏感 ID 到日志，可配置脱敏。

### NFR-06 文档
- README、配置说明、迁移指南、SPI 示例、性能报告。

---

## 9. 异常体系

| 异常                           | 场景                   |
| ------------------------------ | ---------------------- |
| `IdGenerationException`        | 通用生成失败           |
| `ClockBackwardsException`      | 时钟回拨且策略为 THROW |
| `WorkerIdUnavailableException` | WorkerId 获取失败/越界 |
| `IdConfigurationException`     | 配置非法               |
| `IdGeneratorNotFoundException` | 命名生成器不存在       |

验收：异常信息包含可操作提示。

---

## 10. 测试与验收

### 10.1 测试类型
- 单元测试：各算法格式、边界、并发。
- 自动配置测试：`ApplicationContextRunner`。
- 集成测试：Starter + 各算法模块组合。
- 并发测试：多线程批量生成无重复。
- 性能基准：JMH。
- 兼容性测试：Spring Boot 4.x。

### 10.2 发布验收标准
1. 所有 P0 功能通过测试。
2. 配置元数据完整。
3. 示例工程可启动。
4. 基准测试报告达标。
5. 文档覆盖全部配置与 SPI。
6. `mvn verify` 通过，依赖无冲突。

---

## 11. 交付物与里程碑

### 交付物
- `knitid-bom`
- `knitid-core`
- `knitid-spring-boot-autoconfigure`
- `knitid-spring-boot-starter`
- 各算法模块
- 示例工程
- 文档与性能报告

### 里程碑
- **M1**：`knitid-core + autoconfigure + starter + UUID7 + Snowflake`。
- **M2**：ULID、UUID8、业务 ID、SPI 示例。
- **M3**：Actuator、指标、性能报告、兼容性验证、1.0 发布。

---

## 12. 风险与约束

| 风险                           | 应对                         |
| ------------------------------ | ---------------------------- |
| UUID8 语义自定义，跨系统不通用 | 文档明确边界，默认布局稳定   |
| 第三方 ULID/UUID 库版本冲突    | BOM 统一管理，算法模块隔离   |
| WorkerId 冲突                  | 提供 SPI 与启动校验          |
| 业务 ID 序列强依赖外部存储     | 提供 SPI，示例不绑定生产方案 |
| 用户只引 Starter 未引算法模块  | 启动提示 + 健康检查 + 文档   |

---

**结论**：本需求文档以“一个 Starter + 统一 API + 算法模块按需引入 + 自动配置”为落地架构，P0 覆盖 UUID7、ULID、雪花 ID、业务 ID 和核心自动配置；UUID8 作为 P1 自定义布局能力交付。