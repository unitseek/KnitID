# KnitID 配置参考

本文覆盖 v1.0.0 全部配置项。所有属性前缀为 `knitid`；未知属性会导致启动失败（快速失败）。

## 1. 依赖

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
<!-- 按需引入算法模块 -->
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-generator-uuid7</artifactId>
</dependency>
```

算法模块：`knitid-generator-uuid7`、`-uuid8`、`-ulid`、`-ksid`、`-biz`。
集成模块：`knitid-worker-redis`、`-zookeeper`、`-kubernetes`、`knitid-sequence-redis`。

## 2. 根属性

| 属性 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `knitid.enabled` | boolean | `true` | 总开关 |
| `knitid.default-type` | String | 无 | 默认生成器类型（如 `UUID7`） |
| `knitid.default-generator` | String | 无 | 默认生成器名称，优先于 `default-type` |
| `knitid.limits.max-batch-size` | int | `10000` | 批量上限 |
| `knitid.health.cache-ttl` | Duration | `5s` | 健康检查缓存 |
| `knitid.health.backwards-down-count` | int | `5` | 窗口内回拨次数达到即 DOWN |
| `knitid.health.backwards-down-window` | Duration | `1m` | 回拨窗口 |

## 3. 生成器通用属性

```yaml
knitid:
  generators:
    <name>:
      enabled: true
      type: UUID7   # UUID7 | UUID8 | ULID | KSID | BIZ
```

| 属性 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `generators.<name>.enabled` | boolean | `true` | 是否创建 |
| `generators.<name>.type` | String | 必填 | 类型名（不区分大小写，忽略 `-`/`_`） |

## 4. UUIDv7

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `uuid7.monotonic` | `true` | 同毫秒单调 |
| `uuid7.monotonic-overflow` | `WAIT` | `WAIT`/`THROW` |

## 5. UUIDv8

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `uuid8.preset` | `TIME_RANDOM` | `TIME_RANDOM`/`TIME_NODE_RANDOM`/`RANDOM` |
| `uuid8.datacenter-id` | `0` | 0-15，仅 `TIME_NODE_RANDOM` |
| `uuid8.worker-id` | `0` | 0-255，仅 `TIME_NODE_RANDOM` |

> UUIDv8 为应用自定义语义，不保证跨系统互操作。

## 6. ULID

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `ulid.monotonic` | `true` | 同毫秒单调 |
| `ulid.monotonic-overflow` | `WAIT` | `WAIT`/`THROW` |
| `ulid.random-source` | `PERFORMANCE` | `PERFORMANCE`/`SECURE` |

## 7. KSID（Snowflake 算法）

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `ksid.epoch` | `2024-01-01T00:00:00Z` | 纪元；修改属破坏性变更 |
| `ksid.timestamp-bits` | `41` | 与其余位合计 63 |
| `ksid.datacenter-id-bits` | `5` | — |
| `ksid.worker-id-bits` | `5` | — |
| `ksid.sequence-bits` | `12` | ≥1 |
| `ksid.datacenter-id` | `0` | 位宽为 0 时必须为 0 |
| `ksid.worker-id` | 无 | `fixed` 分配方式必填 |
| `ksid.worker-id-assigner` | `fixed` | `fixed`/`environment`/`redis`/`zookeeper`/`kubernetes` |
| `ksid.clock-backwards.strategy` | `WAIT` | `WAIT`/`THROW`/`BORROW_FUTURE` |
| `ksid.clock-backwards.max-wait` | `5s` | `WAIT` 上限 |
| `ksid.sequence-overflow` | `WAIT` | `WAIT`/`THROW` |
| `ksid.allow-borrow-future` | `false` | `BORROW_FUTURE` 显式开关 |
| `ksid.lease-renew-interval` | `20s` | 租约续约周期 |
| `ksid.lease-max-renew-failures` | `3` | 连续失败阈值 |

## 8. 业务 ID（BIZ）

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `biz.template` | 必填 | 模板，≤128 字符 |
| `biz.prefix` | 无 | 模板含 `{prefix}` 时必填 |
| `biz.zone-id` | 必填 | 如 `Asia/Shanghai` |
| `biz.node-id` | 无 | 模板含 `{worker}` 时必填 |
| `biz.sequence-provider` | `memory` | `memory`/`redis`/自定义 Bean 名 |
| `biz.reset` | `DAILY` | `DAILY`/`NEVER` |
| `biz.check-algorithm` | `MOD37_36` | `MOD37_36`/`MOD11_2`/`LUHN`/`NONE` |

模板占位符：`{prefix}`、`{yyyy}`、`{MM}`、`{dd}`、`{HH}`、`{mm}`、`{ss}`、`{yyyyMMdd}`、`{yyyyMMddHHmmss}`、`{seq:n}`、`{random:n}`、`{worker}`、`{check}`；转义 `{{`/`}}`。

## 9. WorkerId 分配

- `fixed`：使用 `ksid.worker-id` / `ksid.datacenter-id`；
- `environment`：环境变量/系统属性 `KNITID_WORKER_ID`、`KNITID_DATACENTER_ID`；
- `redis`：`knitid.worker.redis.key-prefix`（默认 `knitid:worker`）、`lease-ttl`（60s）、`renew-interval`（20s）；
- `zookeeper`：`knitid.worker.zookeeper.connect-string`（必填）、`namespace`（`knitid`）、`session-timeout`（10s）、`connection-timeout`（10s）；
- `kubernetes`：`knitid.worker.kubernetes.mode`（`STATEFULSET_ORDINAL`/`DOWNWARD_API`）、`pod-name`（默认读 `HOSTNAME`/`POD_NAME`）。

## 10. Redis 序列

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `knitid.sequence.redis.enabled` | `true` | 开关 |
| `knitid.sequence.redis.key-prefix` | `knitid:seq` | key 前缀 |
| `knitid.sequence.redis.ttl` | `2d` | 序列 key 的空闲 TTL；`biz.reset=NEVER` 的 scope 不设置 TTL |

Bean 名称：`redis` 或 `redisSequenceProvider`。

## 11. Actuator

- 端点：`/actuator/knitid`，暴露配置 `management.endpoints.web.exposure.include=knitid`；
- 健康键：`knitId`；开关 `management.health.knitid.enabled`；
- 指标：`knitid.id.generation.duration`、`knitid.id.generation.errors`、`knitid.id.batch.size`、`knitid.id.worker.id`。

## 12. 完整示例

```yaml
knitid:
  default-type: UUID7
  limits:
    max-batch-size: 10000
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
    biz-id:
      type: BIZ
      biz:
        template: "{prefix}-{yyyyMMdd}-{seq:6}-{check}"
        prefix: ORD
        zone-id: Asia/Shanghai
        sequence-provider: redis
        reset: DAILY
        check-algorithm: MOD37_36
  worker:
    redis:
      key-prefix: knitid:worker
      lease-ttl: 60s
      renew-interval: 20s
  sequence:
    redis:
      key-prefix: knitid:seq
      ttl: 2d
```
