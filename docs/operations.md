# 运维指南

本文覆盖多实例部署、WorkerId、时钟、Redis/ZooKeeper/Kubernetes、健康与容量。

## 1. 部署拓扑

| 拓扑 | 生成器 | WorkerId 方案 |
| --- | --- | --- |
| 单 JVM | 任意 | `fixed` 或无需 WorkerId |
| 多实例，无共享存储 | UUIDv7/UUIDv8/ULID | 无需 WorkerId |
| 多实例，数字 ID | KSID | Redis / ZooKeeper / Kubernetes / 全局固定分配 |
| 多实例，业务单号 | BIZ + 序列 | Redis 序列或数据库号段 |
| Kubernetes | KSID | `kubernetes`（StatefulSet ordinal）或 Redis/ZK |

> `fixed` 仅在所有实例的 `datacenter-id + worker-id` 组合全局唯一时安全。

## 2. WorkerId 容量

KSID 布局：`timestamp + datacenter + worker + sequence = 63` 位。

- Worker 容量：`2^workerIdBits`（默认 5 位 = 32 个，编号 0–31）；
- Datacenter 容量：`2^datacenterIdBits`（默认 5 位 = 32 个）；
- 每毫秒序列：`2^sequenceBits`（默认 12 位 = 4096）。

Redis/ZooKeeper 分配器支持 `worker-id-bits` 1–16；Kubernetes 同样 1–16，但受 Pod ordinal 限制。扩大位数需要同步调整其他位，保持总和 63，属于破坏性变更。

## 3. Redis WorkerId

Configuration:

```yaml
knitid:
  generators:
    order-id:
      type: KSID
      ksid:
        worker-id-assigner: redis
  worker:
    redis:
      key-prefix: knitid:worker
      lease-ttl: 60s
      renew-interval: 20s
```

- Key：`<prefix>:<datacenter>:<worker>`，使用 `SET NX + TTL` 原子占用；续约/释放使用 token compare-and-set/delete，避免旧实例操作新持有者的 lease；
- 启动时从 worker 0 开始扫描空槽，槽满抛 `KNITID-2003`；
- KSID 每 `renew-interval` 续约；连续失败达到 `ksid.lease-max-renew-failures` 后健康 DOWN、生成抛 `KNITID-2007`；
- 应用关闭时释放 lease；
- 约束：`renew-interval < lease-ttl`，否则启动失败。

建议：

- Redis 使用持久化/主从或集群，避免 lease 数据丢失；
- TTL 不要过短（网络抖动会导致误判）；不要过长（宕机后槽位回收慢）；
- 监控 `knitid.id.worker.id`、`knitId` 健康、Redis 延迟。

## 4. ZooKeeper WorkerId

```yaml
knitid:
  generators:
    order-id:
      type: KSID
      ksid:
        worker-id-assigner: zookeeper
  worker:
    zookeeper:
      connect-string: zk1:2181,zk2:2181,zk3:2181
      namespace: knitid
      session-timeout: 10s
      connection-timeout: 10s
```

- 每个槽位是临时节点 `/workers/<datacenter>/<worker>`（在 namespace 下）；
- 会话断开后 ZooKeeper 自动删除临时节点，即 lease 释放；
- 续约时检查连接、节点存在以及临时节点 owner 是否为当前 session；release 同样只删除本 session 的节点；
- 需要 Curator/ZooKeeper 依赖与可达的 ensemble；
- 适合已有 ZooKeeper 的团队；否则优先 Redis 或 Kubernetes。

## 5. Kubernetes WorkerId

```yaml
knitid:
  generators:
    order-id:
      type: KSID
      ksid:
        worker-id-assigner: kubernetes
  worker:
    kubernetes:
      mode: STATEFULSET_ORDINAL   # 默认；从 HOSTNAME 读取 pod 名
      # pod-name: order-3          # 显式覆盖，测试/特殊场景
```

- `STATEFULSET_ORDINAL`：从 `HOSTNAME`（如 `order-3`）解析 ordinal；
- `DOWNWARD_API`：从 `POD_NAME` 读取（需配置 Downward API）；
- ordinal 必须能放入 `worker-id-bits`；
- 依赖 StatefulSet 保证同一 ordinal 同时只有一个 Pod；滚动发布时确保先终止旧 Pod；
- 不提供分布式 lease，不要用于 Deployment 随机 Pod 名。

## 6. 时钟与 NTP

- 所有节点必须启用 NTP/chrony，并监控时钟偏移；
- KSID 默认 `WAIT`：回拨时最多等待 `5s`，超时抛 `KNITID-2002`；
- `THROW`：立即失败，适合不允许阻塞的 API；
- `BORROW_FUTURE`：保持逻辑时间继续生成，必须显式 `allow-borrow-future: true`，并接受逻辑时间领先真实时间；
- UUIDv7/ULID 时间来自系统时钟；回拨时单调模式仍保证同进程不重复，但不同进程可能乱序；
- 容器/VM 暂停、快照恢复、手动改时间都可能导致回拨，需纳入演练。

## 7. BIZ 序列

- `memory`：每 JVM 独立，仅单实例/测试；
- `redis`：多实例共享，Lua `INCRBY + TTL`，0 基；key 由 scope 决定（日切时含日期）；
- 序列在生成 ID 前消耗；生成失败会留下空洞，业务单号通常可接受；
- Redis 不可用时 BIZ 失败（不静默回退），避免重复；
- `ttl` 默认 2d，表示空闲过期；日切 scope 每次递增都会刷新 TTL，`reset=NEVER` 的 scope 不设置 TTL。

## 8. 健康与指标

健康键 `knitId`：

| 状态 | 触发 |
| --- | --- |
| UP | 默认生成器正常 |
| DOWN | lease 丢失、回拨次数达到 `backwards-down-count`、生成器健康快照 DOWN |
| UNKNOWN | 没有默认生成器、生成器已关闭 |

缓存 `knitid.health.cache-ttl` 默认 5s。端点：

- `/actuator/knitid`：默认生成器与列表；
- `/actuator/health/knitId`：健康详情（KSID 含 workerId/leaseValid/clockBackwardsCount）。

指标：

| 指标 | 用途 |
| --- | --- |
| `knitid.id.generation.duration` | 生成耗时，按 type/name |
| `knitid.id.generation.errors` | 错误计数，reason 区分 |
| `knitid.id.batch.size` | 批量大小 |
| `knitid.id.worker.id` | KSID 当前 WorkerId |

告警建议：错误率、`KNITID-2002/2003/2007` 增长、`knitId` DOWN、WorkerId 接近容量、Redis/ZK 延迟。

## 9. 容量

开发机单线程 JMH（非正式）：UUIDv7 ≈ 17.6M、ULID ≈ 6.0M、KSID ≈ 2.8M ops/s。生产容量需考虑：

- 每实例线程数与锁竞争（KSID/ULID 同步）；
- Redis 序列每次生成一次 RTT，批量接口可降低往返；
- 续约线程/Redis/ZK 连接开销；
- 日志、序列化、数据库写入通常远大于 ID 生成成本。

建议用真实硬件和 Redis 集群压测，不要直接套用开发机数字。

## 10. 发布与关闭

- Spring 容器关闭时，`KnitIdAutoConfiguration` 的 closer 会关闭 KSID 生成器，释放 WorkerId；
- 确保配置 `server.shutdown=graceful` / 容器 terminationGracePeriod 足够；
- 不要在运行中删除 Redis WorkerId key 或 ZooKeeper 节点；如需清理，先停止实例并确认 lease 已过期；
- KSID 的 `epoch`/位宽变更需要迁移与停机评估。

## 11. 运行手册

| 现象 | 检查 | 处理 |
| --- | --- | --- |
| 启动报 `KNITID-1002` | 是否引入生成器模块 | 加依赖 |
| 启动报 `KNITID-1003` | 多个 factory/默认未指定 | 设置 `default-type` 或 `default-generator` |
| `KNITID-2003` | Redis/ZK 连接、槽位 | 扩容 WorkerId 位数/清理失效 lease |
| `KNITID-2007` | 续约失败、网络 | 恢复 Redis/ZK；实例会停止生成 |
| `KNITID-2002` | NTP/时钟跳变 | 修复时钟；调整策略 |
| BIZ 序列耗尽 | 位数/流量 | 加宽 `seq` 或改 Redis 序列 |
