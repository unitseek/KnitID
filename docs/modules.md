# 模块说明

## 依赖关系

```text
knitid-core  ←  knitid-generator-{uuid7,uuid8,ulid,ksid,biz,ksuid,typeid}
     ↑
knitid-spring-boot-autoconfigure  ←  knitid-spring-boot-starter
     ↑
knitid-spring-boot-starter-all (starter + all seven generators)
     ↑
knitid-worker-{redis,zookeeper,kubernetes} / knitid-sequence-redis
     ↑
knitid-samples / knitid-benchmarks

knitid-core + generator-*  ←  knitid-cli（独立可执行 JAR）
```

- `knitid-core`：API、SPI、异常、解析接口，零 Spring/三方算法依赖；
- 生成器模块：通常只依赖 core，各自注册 `IdGeneratorFactory`；TypeID 额外依赖 UUIDv7 以复用其时间/随机位布局。
- 自动配置模块：注册中心、默认选择、健康、指标、端点、解析器；
- Starter：core + autoconfigure，不包含算法；starter-all 额外聚合七类生成器；
- 集成模块：WorkerId 与 Redis 序列，按需引入；
- Test/Benchmarks/Samples：不进入生产依赖。

## 模块清单

| 模块 | 作用 | 需要额外依赖 |
| --- | --- | --- |
| `knitid-bom` | 版本管理 | — |
| `knitid-core` | 公共 API/SPI | — |
| `knitid-test` | `MutableClock` 测试工具 | 仅测试 |
| `knitid-generator-uuid7` | UUIDv7 | — |
| `knitid-generator-uuid8` | UUIDv8 | — |
| `knitid-generator-ulid` | ULID | — |
| `knitid-generator-ksid` | KSID（Snowflake） | — |
| `knitid-generator-biz` | 业务 ID | — |
| `knitid-generator-ksuid` | KSUID | — |
| `knitid-generator-typeid` | TypeID（依赖 UUIDv7） | KnitID UUIDv7 |
| `knitid-spring-boot-autoconfigure` | Spring Boot 自动配置 | Spring Boot 4 |
| `knitid-spring-boot-starter` | Starter（不含算法） | Spring Boot 4 |
| `knitid-spring-boot-starter-all` | 全家桶七类算法 | Spring Boot 4 |
| `knitid-worker-redis` | Redis WorkerId 租约 | spring-data-redis |
| `knitid-worker-zookeeper` | ZooKeeper WorkerId | Curator/ZooKeeper |
| `knitid-worker-kubernetes` | K8s StatefulSet WorkerId | 无（读环境变量） |
| `knitid-sequence-redis` | Redis 业务序列 | spring-data-redis |
| `knitid-sequence-jdbc` | 数据库号段业务序列 | spring-jdbc / DataSource |
| `knitid-boot-compat-4.0` | Spring Boot 4.0 兼容测试 | 仅 profile `boot-4.0` |
| `knitid-benchmarks` | JMH | 仅开发 |
| `knitid-samples` | 示例 | 仅开发 |
| `knitid-cli` | 无 Spring 的命令行 gen/parse | JDK + 对应生成器模块 |

## 最小依赖组合

| 目标 | 依赖 |
| --- | --- |
| UUIDv7 + Spring Boot | starter + generator-uuid7 |
| KSID + Redis WorkerId | starter + generator-ksid + worker-redis + spring-data-redis |
| BIZ + Redis 序列 | starter + generator-biz + sequence-redis + spring-data-redis |
| BIZ + JDBC 序列 | starter + generator-biz + sequence-jdbc + DataSource |
| Boot 4.0 兼容验证 | `mvn -Pboot-4.0 verify` |
| KSUID | starter + generator-ksuid（或 starter-all） |
| TypeID | starter + generator-typeid（会传递引入 UUIDv7） |
| 全部七类 | starter-all |
| 非 Spring 使用 | core + 算法模块 |
| CLI | `knitid-cli` 可执行 JAR |

## BOM

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

之后引入 KnitID 模块无需版本号。

## 依赖规则

- 不要同时引入重复的生成器模块（同一类型多个 factory 会启动失败）；
- 集成模块只在需要时引入，避免无谓的 Redis/ZooKeeper 连接配置；
- `knitid-samples`/`knitid-benchmarks` 不要作为生产依赖；
- 自定义生成器只需依赖 `knitid-core`，实现 `IdGeneratorFactory` 并注册 Bean。
