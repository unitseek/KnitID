# 非 Spring 使用

KnitID 的核心 API 与算法模块零 Spring 依赖，可在普通 Java、Kotlin、CLI、批处理或 GraalVM Native 中使用。自动配置、健康检查、Actuator、Micrometer 集成仅在 Spring Boot 4 下可用。

## 1. 依赖

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-core</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-generator-uuid7</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 2. 直接构造生成器

### UUIDv7

```java
IdGenerator ids = new Uuid7Generator(Clock.systemUTC(), new SecureRandomProvider());
String id = ids.generate();
```

### ULID

```java
IdGenerator ids = new UlidGenerator(Clock.systemUTC(), new PerformanceRandomProvider());
String id = ids.generate();
```

### KSID

```java
KsidConfig config = KsidConfig.defaults();
WorkerId worker = new WorkerId(0, 1); // 必须全局唯一
IdGenerator ids = new KsidGenerator(Clock.systemUTC(), worker, config);
long numericId = ((NumericIdGenerator) ids).generateLong();
```

> 直接构造不提供租约续约。多实例请使用 Spring 模块的 Redis/ZooKeeper/K8s 分配器，或自行实现 `WorkerIdAssigner`。

### BIZ

`BizIdGenerator` 需要编译后的模板与 `SequenceProvider`，推荐通过工厂创建：

```java
Map<String, Object> values = Map.of(
        "biz.template", "{prefix}-{yyyyMMdd}-{seq:6}-{check}",
        "biz.prefix", "ORD",
        "biz.zone-id", "Asia/Shanghai",
        "biz.sequence-provider", "memory",
        "biz.reset", "DAILY",
        "biz.check-algorithm", "MOD37_36");

GeneratorConfig config = new GeneratorConfig() {
    @Override public String name() { return "order-id"; }
    @Override public IdType type() { return IdType.BIZ; }
    @Override public Optional<Object> get(String key) { return Optional.ofNullable(values.get(key)); }
};

IdGenerator ids = new BizIdGeneratorFactory(
        Clock.systemUTC(),
        name -> new MemorySequenceProvider()).create(config);
```

多实例需替换为共享 `SequenceProvider`（如 Redis/数据库号段）。

## 3. 静态门面

`IdGenerators` 适合测试/简单应用：

```java
IdGenerators.register("event", new Uuid7Generator(Clock.systemUTC(), new SecureRandomProvider()));
IdGenerators.register("order", new UlidGenerator(Clock.systemUTC(), new PerformanceRandomProvider()));
IdGenerators.setDefault("event");

String id = IdGenerators.getDefault().generate();
String order = IdGenerators.get("order").generate();
```

注意：静态门面与 Spring 注册中心**不共享状态**；生产 Spring 应用请使用注入的 `IdGeneratorRegistry`。

## 4. 解析

`IdParser`/`IdTypeParser` 在 core 中只有接口。内置实现分布：

- UUIDv7/UUIDv8/ULID/纯数字 KSID：`knitid-spring-boot-autoconfigure`；
- KSUID/TypeID：各自生成器模块；
- BIZ 模板/KSID 位布局：各自生成器模块，Spring Boot 下按 generator 自动绑定。

非 Spring 场景可以自行组合 `IdTypeParser`，或直接引入对应模块的 parser 类。只做解析时不要为了 `SimpleIdParser` 引入整个 autoconfigure/Spring 依赖。

## 5. CLI

无需 Spring 即可使用 CLI：

```bash
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type UUID7
java -jar knitid-cli/target/knitid-cli-1.0.0.jar parse <id>
```

CLI v1 只支持无外部基础设施的类型；完整范围见 [CLI](cli.md)。

## 6. GraalVM Native

- 核心算法无反射、无动态代理、无 Jackson；
- `SecureRandom`/`Clock`/`System.Logger` 均为 native-image 友好 API；
- Spring Boot 应用使用 Boot 4 的 AOT 处理自动配置；
- 仓库提供 `.github/workflows/native.yml`（workflow_dispatch / 每周计划）做 GraalVM Native 冒烟；不是每个 PR 都执行，发布前仍需在目标 GraalVM 版本验证；
- 如自定义 `RandomProvider`/`IdTypeParser`，确保注册方式 AOT 可分析（避免运行时反射扫描）。

## 7. Kotlin

```kotlin
val ids: IdGenerator = Uuid7Generator(Clock.systemUTC(), SecureRandomProvider())
val id = ids.generate()

fun nextOrderId(registry: IdGeneratorRegistry): String =
    registry.get("order-id").generate()
```

Kotlin 可空性：`IdContext` 参数可传 `null`；`IdInfo` 字段可为空，按需判空。

## 8. 线程安全与阻塞

- 所有内置生成器线程安全；
- KSID 在 `clock-backwards.strategy=WAIT` 时可能阻塞，`blocking()` 返回 `true`；
- UUIDv7/ULID 的单调溢出等待只发生在每毫秒随机空间耗尽时；
- 非 Spring 场景需自行管理生命周期（`KsidGenerator` 实现 `AutoCloseable`，用于关闭租约线程）。
