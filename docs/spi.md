# SPI 扩展指南

KnitID 的扩展点位于 `org.unitseek.knitid.core.spi` 与 `org.unitseek.knitid.core.parser`。所有扩展实现应为线程安全，并通过稳定错误码报告失败。

## 扩展点一览

| SPI | 是否已接入 | 用途 |
| --- | --- | --- |
| `IdGeneratorFactory` | ✅ | 新增生成器类型 |
| `WorkerIdAssignerProvider` / `WorkerIdAssigner` | ✅ | 自定义 WorkerId 分配与租约 |
| `SequenceProvider` | ✅ | 自定义 BIZ 序列 |
| `IdTypeParser` | ✅ | 自定义 ID 解析 |
| `IdGeneratorDecorator` | ✅ | 包装生成器（指标/审计/限流） |
| `IdGeneratorCustomizer` | ⚠️ 预留 | 当前未接入注册中心，请使用 Decorator |
| `IdFormatter` / `IdValidator` | ⚠️ 预留 | 当前未接入调用链 |

## 1. 自定义生成器

实现 `IdGeneratorFactory`，Spring 会自动收集：

```java
public final class TicketIdFactory implements IdGeneratorFactory {

    @Override
    public IdType type() {
        return IdType.of("TICKET");
    }

    @Override
    public IdGenerator create(GeneratorConfig config) {
        int width = config.getInt("ticket.width", 12);
        return new TicketIdGenerator(width);
    }
}
```

```java
@Configuration
class TicketConfiguration {
    @Bean
    IdGeneratorFactory ticketIdFactory() {
        return new TicketIdFactory();
    }
}
```

```yaml
knitid:
  generators:
    ticket:
      type: TICKET
      ticket:
        width: 12
```

注意：`knitid.generators.<name>` 下除 `enabled`/`type` 外，只允许与类型同名的小写块（如 `uuid7`、`ticket`），其余键会启动失败。

## 2. 自定义 WorkerId 分配器

`WorkerIdAssignerProvider` 暴露名称和工厂；`WorkerIdAssigner` 负责获取、续约、释放：

```java
public final class CmdbWorkerIdProvider implements WorkerIdAssignerProvider {

    @Override
    public String name() {
        return "cmdb";
    }

    @Override
    public WorkerIdAssigner create(int datacenterId, int workerIdBits) {
        return new CmdbWorkerIdAssigner(datacenterId, (1 << workerIdBits) - 1);
    }
}
```

续约约定：

- `renewable()` 返回 `true` 时，KSID 会按 `renewalInterval()`（或 `ksid.lease-renew-interval`）调度续约；
- 连续失败达到 `ksid.lease-max-renew-failures` 后健康变 DOWN，生成抛 `KNITID-2007`；
- `release()` 在应用关闭时尽力释放；实现必须幂等；
- 失败必须抛 `WorkerIdUnavailableException`，不要返回重复 WorkerId。

## 3. 自定义序列（BIZ）

```java
public final class DatabaseSequenceProvider implements SequenceProvider {

    private final JdbcTemplate jdbc;
    private final String table;

    // 省略构造器

    @Override
    public long next(String scope) {
        return next(scope, 1)[0];
    }

    @Override
    public long[] next(String scope, int count) {
        // 在一个数据库事务中用 SELECT ... FOR UPDATE / UPDATE ... RETURNING 取号
        throw new UnsupportedOperationException("example");
    }
}
```

注册为 Bean 后，在配置中引用 Bean 名称：

```yaml
knitid:
  generators:
    order-id:
      type: BIZ
      biz:
        template: "ORD-{yyyyMMdd}-{seq:6}"
        zone-id: Asia/Shanghai
        sequence-provider: databaseSequenceProvider
        reset: DAILY
```

要求：`next(scope, count)` 必须原子；返回值从 0 开始或连续，不能重复；失败抛 `SequenceUnavailableException`。

KnitID 1.0 还提供持久化提示：`SequenceProvider.next(scope, true)` / `next(scope, count, true)` 表示长生命周期 scope。默认实现忽略该提示；Redis 提供者据此不设置 TTL。

## 4. 自定义解析器

```java
@Bean
IdTypeParser bizParser() {
    return new IdTypeParser() {
        @Override
        public IdType type() {
            return IdType.BIZ;
        }

        @Override
        public Optional<IdInfo> tryParse(String raw) {
            // 按业务模板解析；不匹配返回 Optional.empty()
            return Optional.empty();
        }
    };
}
```

用户 Bean 优先于内置解析器；内置支持 UUIDv7、UUIDv8、ULID、纯数字 KSID（仅类型/原值）。

## 5. 装饰器

```java
@Bean
IdGeneratorDecorator auditDecorator() {
    return (generator, name) -> new IdGenerator() {
        @Override public IdType type() { return generator.type(); }
        @Override public String generate() {
            // 审计逻辑
            return generator.generate();
        }
        @Override public String generate(IdContext context) {
            return generator.generate(context);
        }
    };
}
```

装饰器按 Spring `@Order` 顺序应用；内置 Micrometer 装饰器会记录耗时/错误/批量大小。不要改变生成器类型或输出格式。

## 6. 自定义随机源

生成器构造器接受 `RandomProvider`（`nextBytes(int)`/`nextInt(int)`）。自定义工厂可注入硬件随机/测试固定随机；生产默认使用 `SecureRandomProvider` 或 ULID `PERFORMANCE` 随机源。

## 兼容性要求

- 仅依赖 `knitid-core` 的公共包；
- 不依赖自动配置内部类；
- 不修改全局状态（`IdGenerators` 静态门面与 Spring 注册中心互不共享）；
- 新增公共 API 的 Javadoc 与配置文档需同步。
