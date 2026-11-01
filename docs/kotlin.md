# Kotlin 使用示例

KnitID 的 core 与生成器模块是普通 Java API，可直接从 Kotlin 调用。Spring Boot 自动配置同样适用于 Kotlin 项目。

## 依赖

Maven：

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter-all</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle Kotlin DSL：

```kotlin
implementation("org.unitseek.knitid:knitid-spring-boot-starter-all:1.0.0")
```

## 直接生成

```kotlin
import java.time.Clock
import org.unitseek.knitid.core.api.IdGenerator
import org.unitseek.knitid.core.random.SecureRandomProvider
import org.unitseek.knitid.generator.uuid7.Uuid7Generator

val ids: IdGenerator = Uuid7Generator(Clock.systemUTC(), SecureRandomProvider())
val id: String = ids.generate()
```

## Spring Boot 注入

```kotlin
import org.springframework.stereotype.Service
import org.unitseek.knitid.core.api.IdGenerator
import org.unitseek.knitid.core.api.IdGeneratorRegistry
import org.unitseek.knitid.core.api.IdContext

@Service
class OrderService(
    private val ids: IdGenerator,
    private val registry: IdGeneratorRegistry,
) {
    fun newOrderId(): String = ids.generate()

    fun newTenantOrderId(tenant: String): String =
        registry.get("biz").generate(
            IdContext.builder()
                .tenantId(tenant)
                .prefix("ORDER")
                .build(),
        )
}
```

## 批量与流

```kotlin
fun nextOrderIds(count: Int): List<String> = ids.generateBatch(count)

fun orderIdSequence(count: Int) = ids.generateStream(count)
```

批量上限由 `knitid.limits.max-batch-size` 控制；异步/流式 API 与 Java 版本一致。

## 扩展

Kotlin 可以实现 core SPI：

```kotlin
import org.unitseek.knitid.core.api.IdGenerator
import org.unitseek.knitid.core.spi.GeneratorConfig
import org.unitseek.knitid.core.spi.IdGeneratorFactory

class TicketIdFactory : IdGeneratorFactory {
    override fun type() = org.unitseek.knitid.core.api.IdType.of("TICKET")

    override fun create(config: GeneratorConfig): IdGenerator =
        TicketIdGenerator(config.getInt("ticket.width", 12))
}
```

在 Spring Boot 中把 `TicketIdFactory` 注册为 `@Bean`，即可通过 `knitid.generators.*.type=TICKET` 使用。

## 注意

- Kotlin 可空性：`IdContext` 参数和 `IdInfo` 字段允许为 `null`；
- 协程/Flow 不是内置 API，使用 `generateStream` 或异步批量后自行包装；
- 不要用 `{random:n}` 或可枚举序列生成令牌。
