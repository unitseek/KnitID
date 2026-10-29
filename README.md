# KnitID

统一、模块化的 Spring Boot ID 生成工具库：UUIDv7、UUIDv8、ULID、KSID（Snowflake 算法）、业务 ID。

## 状态

M1/M2 进行中（v0.3.0-SNAPSHOT）：core API/SPI、UUIDv7、KSID（位分配/回拨/租约）、Redis/ZooKeeper/Kubernetes WorkerId 分配器、Actuator 健康检查、Spring Boot 自动配置与契约测试已完成；其余模块为占位模块。

## 文档

- [功能需求文档 v0.3](<wiki/ID 生成工具库功能需求文档（FRD）.md>)
- [FRD 评审意见](<wiki/ID 生成工具库 FRD 评审意见.md>)

## 构建

要求 JDK 21+、Maven 3.9+：

```bash
mvn clean verify
```

## 模块

| 模块 | 状态 |
| --- | --- |
| `knitid-core` | 已完成骨架（API/SPI/异常/静态门面/DurationParser/健康快照） |
| `knitid-generator-uuid7` | 已完成实现 + 契约/属性测试 |
| `knitid-generator-ksid` | 已完成实现 + 测试（位分配校验、回拨策略、序列溢出、WorkerId 租约、健康快照） |
| `knitid-worker-redis` | 已完成实现 + 单测 + Testcontainers 集成测试（SET NX/TTL 租约、续约、释放、槽位扫描、断连） |
| `knitid-worker-zookeeper` | 已完成实现 + 测试（临时节点会话租约、槽位扫描、续约检查、释放） |
| `knitid-worker-kubernetes` | 已完成实现 + 测试（StatefulSet ordinal / Downward API） |
| `knitid-spring-boot-autoconfigure` | 已完成骨架（配置/注册中心/默认选择/自动配置矩阵/健康检查/生命周期关闭） |
| `knitid-spring-boot-starter` | 已完成 |
| 其余算法/集成模块 | 占位模块（M1/M2 实现） |

## 测试

当前共 87 个测试，全部通过（`mvn verify`；Testcontainers 用例在无 Docker 时自动跳过）：

- `knitid-core`：IdType 规范化契约（3）；
- `knitid-generator-uuid7`：RFC 9562 附录 A.6 向量、属性测试、边界与溢出策略（11）；
- `knitid-generator-ksid`：位分配校验、回拨 `WAIT`/`THROW`/`BORROW_FUTURE`、序列溢出、WorkerId 越界、租约生命周期、健康快照（23）；
- `knitid-worker-redis`：槽位占用/冲突、租约续约失败、释放、provider 配置校验、自动配置（9），以及 Testcontainers 真实 Redis 的 SET NX/TTL/续约/释放/断连（5）；
- `knitid-worker-zookeeper`：临时节点分配、重新占用、续约、配置校验、自动配置（6）；
- `knitid-worker-kubernetes`：StatefulSet ordinal、Downward API、越界/非法 pod 名、配置校验、自动配置（9）；
- `knitid-spring-boot-autoconfigure`：12 个启动行为矩阵场景 + 7 个健康检查场景（19）；
- `knitid-samples`：Starter + UUIDv7/KSID 集成启动测试（2）。

### Testcontainers 集成测试

`knitid-worker-redis` 使用 Testcontainers 对真实 Redis 验证 SET NX、TTL、续约、释放与断连行为：

- 需要 Docker；无 Docker 时测试通过 `@Testcontainers(disabledWithoutDocker = true)` 自动跳过；
- 默认镜像 `redis:8.10.3`（真实 Redis，固定版本保证可复现），网络受限时可用环境变量覆盖：

```bash
KNITID_TEST_REDIS_IMAGE=your-registry/redis:8.10.3 mvn -pl knitid-worker-redis -am test
```

## Redis WorkerId 快速配置

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
