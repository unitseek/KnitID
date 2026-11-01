# 快速开始

本指南在 Spring Boot 4.1 工程中生成第一个 UUIDv7，并验证 Actuator 端点。

## 1. 环境

| 组件 | 版本 |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| Spring Boot | 4.0.x / 4.1.x |
| KnitID | 1.0.0 |

> Spring Boot 3.x 不支持。非 Spring 工程见 [非 Spring 用法](non-spring.md)。

## 2. 引入依赖

普通 starter 只包含自动配置，不包含任何算法。按需引入生成器；若希望一次引入全部七类算法，可使用 `knitid-spring-boot-starter-all`。按需示例：

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-generator-uuid7</artifactId>
  <version>1.0.0</version>
</dependency>
```

使用 BOM 可省略版本号：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.unitseek.knitid</groupId>
      <artifactId>knitid-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## 3. 配置默认生成器

```yaml
knitid:
  default-type: UUID7
```

只有一个算法模块时，即使不写 `default-type` 也会自动成为默认生成器；有多个模块时必须显式指定。

## 4. 注入并生成

```java
import org.springframework.stereotype.Service;
import org.unitseek.knitid.core.api.IdGenerator;

@Service
public class OrderService {

    private final IdGenerator ids;

    public OrderService(IdGenerator ids) {
        this.ids = ids;
    }

    public String newOrderId() {
        return ids.generate();
    }

    public List<String> newOrderIds(int count) {
        return ids.generateBatch(count);
    }
}
```

`IdGenerator` 是 `@Primary` 门面，委托给注册中心的默认生成器。需要按名称访问多个生成器时注入 `IdGeneratorRegistry`：

```java
@Service
public class Ids {
    private final IdGeneratorRegistry registry;

    public Ids(IdGeneratorRegistry registry) {
        this.registry = registry;
    }

    public String uuid() {
        return registry.get("event-id").generate();
    }

    public String ksid() {
        return registry.getDefault().generate();
    }
}
```

## 5. 命名/多个生成器

```yaml
knitid:
  default-generator: event-id
  generators:
    event-id:
      type: UUID7
    order-id:
      type: ULID
```

- `default-generator` 必须指向已配置的命名生成器；
- `default-type` 在没有匹配的命名生成器时会创建一个名为 `default` 的隐式生成器；
- 未知属性、未知类型、缺少模块都会导致启动失败。

## 6. 多租户/业务类型示例

通过 `IdContext` 覆盖 BIZ 前缀：

```java
String id = registry.get("biz").generate(
        IdContext.builder()
                .tenantId("tenant-a")
                .prefix("TENANT_A")
                .build());
```

适用场景：

- 多租户共享同一 BIZ 生成器；
- 不同业务类型使用不同前缀；
- 不需要为每个租户单独创建生成器。

## 7. 验证

```java
String id = ids.generate();
assert UUID.fromString(id).version() == 7;
```

## 8. Actuator

引入 `spring-boot-starter-actuator`，并暴露端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: knitid,knitid-config,health,metrics
```

```bash
curl http://localhost:8080/actuator/knitid
curl http://localhost:8080/actuator/knitid-config
curl http://localhost:8080/actuator/health/knitId
```

端点返回默认生成器与生成器列表；健康键为 `knitId`。详见 [配置参考](configuration.md) 的 Actuator 一节。

## 9. 下一步

- 选择算法：[ID 选型](id-selection.md)
- 了解模块：[模块说明](modules.md)
- 多实例 KSID：[运维指南](operations.md)
- 自定义扩展：[SPI 指南](spi.md)
- 常见问题：[FAQ](faq.md)
- 命令行工具：[CLI](cli.md)
- AI 辅助：[AI 使用指南](ai-usage.md)
