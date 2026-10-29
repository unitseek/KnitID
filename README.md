# KnitID

统一、模块化的 Spring Boot ID 生成工具库：UUIDv7、UUIDv8、ULID、KSID（Snowflake 算法）、业务 ID。

## 状态

M1/M2 进行中（v0.3.0-SNAPSHOT）：core API/SPI、UUIDv7、KSID（位分配/回拨/租约）、Redis WorkerId 租约分配器、Actuator 健康检查、Spring Boot 自动配置与契约测试已完成；其余模块为占位模块。

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
| `knitid-worker-redis` | 已完成实现 + 测试（SET NX/TTL 租约、续约、释放、槽位扫描） |
| `knitid-spring-boot-autoconfigure` | 已完成骨架（配置/注册中心/默认选择/自动配置矩阵/健康检查/生命周期关闭） |
| `knitid-spring-boot-starter` | 已完成 |
| 其余算法/集成模块 | 占位模块（M1/M2 实现） |

## 测试

当前共 67 个测试，全部通过（`mvn verify`）：

- `knitid-core`：IdType 规范化契约（3）；
- `knitid-generator-uuid7`：RFC 9562 附录 A.6 向量、属性测试、边界与溢出策略（11）；
- `knitid-generator-ksid`：位分配校验、回拨 `WAIT`/`THROW`/`BORROW_FUTURE`、序列溢出、WorkerId 越界、租约生命周期、健康快照（23）；
- `knitid-worker-redis`：槽位占用/冲突、租约续约失败、释放、provider 配置校验、自动配置（9）；
- `knitid-spring-boot-autoconfigure`：12 个启动行为矩阵场景 + 7 个健康检查场景（19）；
- `knitid-samples`：Starter + UUIDv7/KSID 集成启动测试（2）。

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
