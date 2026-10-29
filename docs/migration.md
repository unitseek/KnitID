# 迁移指南

从其他 ID 库迁移到 KnitID 时，核心变化是：**统一 `IdGenerator` API、按模块引入算法、Spring Boot 自动配置**。ID 一旦写入数据库，格式迁移需要额外规划。

## 1. 通用步骤

1. 引入 `knitid-spring-boot-starter` 和目标算法模块；
2. 配置生成器（`knitid.generators.<name>`）；
3. 注入 `IdGenerator` 或 `IdGeneratorRegistry`；
4. 替换旧调用点；
5. 灰度/双写期间保留旧 ID 解析能力；
6. 监控唯一性、时钟回拨、WorkerId 冲突。

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-generator-uuid7</artifactId>
</dependency>
```

## 2. 从 uuid-creator 迁移

旧代码通常直接调用 `UuidCreator.getTimeOrderedEpoch()`。迁移后：

```java
@Service
class OrderService {
    private final IdGenerator ids;

    OrderService(IdGenerator ids) {
        this.ids = ids;
    }

    String newOrderId() {
        return ids.generate(); // UUIDv7 字符串
    }
}
```

配置：

```yaml
knitid:
  default-type: UUID7
```

注意：UUIDv7 与 UUIDv4 的数据库列类型相同（UUID/`varchar(36)`），但**时间排序语义不同**；不要假设旧数据可按时间排序。

## 3. 从 ulid-creator 迁移

KnitID ULID 输出 26 字符 Crockford Base32，可直接写入原 `char(26)` 列：

```yaml
knitid:
  default-type: ULID
  generators:
    event-id:
      type: ULID
      ulid:
        monotonic: true
        random-source: PERFORMANCE
```

若旧库使用小写 ULID，KnitID 输出大写；读取端需大小写不敏感，或迁移时统一。

## 4. 从 MyBatis-Plus 迁移

MyBatis-Plus 的 `IdentifierGenerator` 是单方法接口，可以用 KnitID 生成器适配：

```java
@Component
class KnitIdIdentifierGenerator implements IdentifierGenerator {
    private final IdGenerator ids;

    KnitIdIdentifierGenerator(IdGenerator ids) {
        this.ids = ids;
    }

    @Override
    public Number nextId(Object entity) {
        return Long.parseLong(ids.generate()); // 仅当生成器返回数字字符串（KSID）
    }
}
```

- 若使用 UUID/ULID/BIZ，返回类型应改为 `String`，并调整实体主键字段；
- 多实例 KSID 需配置 Redis/ZooKeeper WorkerId，不能再用固定 `worker-id`。

## 5. 从 CosId 迁移

| CosId | KnitID |
| --- | --- |
| SnowflakeIdGenerator | KSID（`knitid-generator-ksid`） |
| UUID | UUIDv7 / UUIDv8 |
| ULID | ULID |
| 业务号 | BIZ |
| MachineId 分配 | WorkerIdAssigner（Redis/ZK/K8s） |

迁移时核对：纪元、位宽、WorkerId 范围、序列溢出策略。**不要直接复用旧 ID 的位布局**，否则新老 ID 可能冲突或无法解析。

## 6. 从自研 Snowflake 迁移

1. 记录旧布局：时间位、数据中心位、Worker 位、序列位、纪元；
2. 在 KnitID 中用 `ksid.*` 配置等价布局（或选择新布局）；
3. 确保 WorkerId 分配器全局唯一；
4. 若新旧布局不同，旧数据保留旧解析器，新数据写入新列/新前缀；
5. 双写/回放期间校验唯一性与顺序。

## 7. 从可枚举序列迁移到 BIZ

若旧系统使用数据库自增 ID，迁移到 BIZ 时：

- `seq` 不是全局唯一；唯一性由 `prefix + date + seq` 等组合保证；
- 多实例使用 `sequence-provider: redis`；
- 不要在迁移中假设新旧 ID 可比较大小。

## 8. 迁移检查清单

- [ ] 目标生成器/位布局已确认
- [ ] 数据库列长度/类型匹配（UUID、char(26)、bigint、varchar）
- [ ] WorkerId 全局唯一
- [ ] 时钟同步与回拨策略
- [ ] 旧 ID 读取/解析兼容
- [ ] 灰度与回滚方案
- [ ] 唯一性监控与告警
