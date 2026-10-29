# FAQ

## 唯一性

### KnitID 保证 ID 绝对唯一吗？

在约定条件下保证：

- UUIDv7/UUIDv8/ULID：依赖随机位与时钟，理论上碰撞概率可忽略；
- KSID：依赖 `datacenter-id + worker-id` 全局唯一，以及时钟不早于 epoch；
- BIZ：只在业务 scope（prefix/date/sequence）内唯一，不是全局唯一。

违反 WorkerId 唯一、复用 epoch/位布局、手动重置序列都可能产生重复。

### 多实例 KSID 如何避免 WorkerId 冲突？

使用 Redis、ZooKeeper 或 Kubernetes 分配器，或由配置管理保证 `fixed` 组合全局唯一。不要所有实例都写 `worker-id: 0`。

### BIZ 的序列会重复吗？

`memory` 序列每 JVM 独立，多实例会重复；多实例必须使用 `redis` 或数据库号段等共享序列。日切后序列回到 0，但日期前缀不同。

## 时间与时钟

### 时钟回拨会怎样？

KSID 默认 `WAIT`，最多等 `5s`，超时抛 `KNITID-2002`；也可配置 `THROW` 或显式 `BORROW_FUTURE`。UUIDv7/ULID 单调模式只保证单进程内同毫秒不重复，不保证跨进程顺序。

### WAIT / THROW / BORROW_FUTURE 怎么选？

- `WAIT`：默认，适合大多数在线服务；回拨窗口短；
- `THROW`：不能阻塞的 API/批处理，由调用方重试；
- `BORROW_FUTURE`：仅在明确接受逻辑时间领先时使用，需要 `allow-borrow-future: true`。

### 容器环境需要 NTP 吗？

需要。宿主机/节点必须同步时钟；容器不会自动修正时间。VM 快照恢复、暂停/恢复后要检查时钟。

## 安全

### UUIDv7/ULID 会泄露生成时间吗？

会。时间前缀可推断大致生成时间，不适合隐藏时间的场景。对外的资源 ID 建议结合鉴权；不要依赖 ID 的不可猜测性。

### 哪些 ID 不适合做令牌？

KSID 和 BIZ 的序列部分可枚举；`{random:n}` 也不是密码学安全随机。令牌/邀请码请使用专门的加密随机生成器。

### WorkerId 是否敏感？

WorkerId 本身不是密钥，但泄露部署拓扑/容量信息；限制 Actuator 端点访问范围，避免公网暴露。

## 运维

### 内存序列能用于生产吗？

只能在单实例或测试中使用。多实例会产生重复；生产多实例使用 `knitid-sequence-redis` 或自定义 `SequenceProvider`。

### Redis 序列和数据库号段怎么选？

- Redis：实现简单、延迟低，依赖 Redis 可用性；
- 数据库号段：与业务库同事务/同可用性，但需要额外表与并发控制；
- 高吞吐多实例优先 Redis；强依赖数据库一致性时可选号段。

### 如何解析 KSID 的时间戳/WorkerId？

内置解析器只识别数字 KSID 类型与原值（位布局是配置相关的）。注册自定义 `IdTypeParser`，按 `epoch/位宽` 解码。

## 兼容性

### 支持 Spring Boot 3 吗？

不支持。KnitID 1.x 基于 Spring Boot 4.0/4.1 与 Java 21；Boot 3 的自动配置/健康 API 不兼容。

### 支持 Java 17 吗？

不支持，最低 Java 21。

### 支持 GraalVM Native 吗？

核心模块无反射依赖，设计上支持；Spring Boot 自动配置由 Boot AOT 处理。建议在目标版本上做 native 集成测试，见 [非 Spring 用法](non-spring.md)。

### 如何从其他 ID 库迁移？

见 [迁移指南](migration.md)。已有 ID 的格式/位布局不能直接假设兼容，需要双写/回放和新旧列规划。

## 其他

### 批量接口一次能生成多少？

默认上限 `knitid.limits.max-batch-size=10000`，超出抛参数异常。批量不是原子事务，失败可能已生成部分 ID。

### 生成器是线程安全的吗？

是。`IdGenerator` 实现必须线程安全；KSID/ULID/UUIDv7 在同毫秒内同步递增。KSID 在回拨 WAIT 或序列溢出 WAIT 时 `blocking()` 为 true；阻塞行为见 `blocking()`。
