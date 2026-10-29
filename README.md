# KnitID

[![CI](https://github.com/unitseek/knitid/actions/workflows/ci.yml/badge.svg)](https://github.com/unitseek/knitid/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21+-blue.svg)](https://adoptium.net/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0%20%7C%204.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Javadoc](https://img.shields.io/badge/javadoc-site-blue)](https://unitseek.github.io/knitid/)

统一、模块化的 Spring Boot ID 生成工具库：UUIDv7、UUIDv8、ULID、KSID（Snowflake 算法）、业务 ID。

## 状态

1.0 候选（v0.3.0-SNAPSHOT）：五类 ID 算法（UUIDv7、UUIDv8、ULID、KSID、BIZ）、IdParser、Redis/ZooKeeper/Kubernetes WorkerId 分配器、Redis 序列模块、Actuator 端点/健康检查/指标、JMH 基准、示例工程、Spring Boot 自动配置与契约测试均已完成。

## 快速开始

要求 JDK 21+、Spring Boot 4.0/4.1。引入 Starter 与 UUIDv7 模块：

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
  <version>0.3.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-generator-uuid7</artifactId>
</dependency>
```

```yaml
knitid:
  default-type: UUID7
```

```java
@Service
class OrderService {
    private final IdGenerator ids;

    OrderService(IdGenerator ids) {
        this.ids = ids;
    }

    String newOrderId() {
        return ids.generate();
    }
}
```

完整步骤见 [快速开始](docs/getting-started.md)，配置见 [配置参考](docs/configuration.md)。

## 文档

- [文档索引](docs/index.md)
- [配置参考](docs/configuration.md)
- [兼容性与版本策略](docs/compatibility.md)
- [迁移指南](docs/migration.md)
- [基准报告](docs/benchmark-report.md)
- [发布流程](docs/release.md)
- [CHANGELOG](CHANGELOG.md)
- [贡献指南](CONTRIBUTING.md)
- [支持渠道](SUPPORT.md)
- [安全策略](SECURITY.md)
- [行为准则](CODE_OF_CONDUCT.md)
- [Roadmap](ROADMAP.md)
- [治理](GOVERNANCE.md)
- [维护者](MAINTAINERS.md)
- [贡献者](CONTRIBUTORS.md)
- [第三方许可证](docs/third-party.md)

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
| `knitid-generator-uuid8` | 已完成实现 + 测试（TIME_RANDOM/TIME_NODE_RANDOM/RANDOM 预设、版本/变体） |
| `knitid-generator-ulid` | 已完成实现 + 契约/属性测试（ULID 规范向量、Crockford Base32、单调/溢出） |
| `knitid-generator-biz` | 已完成实现 + 测试（模板、序列、日切、校验位、上下文） |
| `knitid-generator-ksid` | 已完成实现 + 测试（位分配校验、回拨策略、序列溢出、WorkerId 租约、健康快照） |
| `knitid-worker-redis` | 已完成实现 + 单测 + Testcontainers 集成测试（SET NX/TTL 租约、续约、释放、槽位扫描、断连） |
| `knitid-worker-zookeeper` | 已完成实现 + 测试 + Testcontainers 集成测试（临时节点会话租约、槽位扫描、续约检查、释放、断连） |
| `knitid-worker-kubernetes` | 已完成实现 + 测试（StatefulSet ordinal / Downward API） |
| `knitid-spring-boot-autoconfigure` | 已完成骨架（配置/注册中心/默认选择/自动配置矩阵/健康检查/生命周期关闭/IdParser/Actuator 端点/指标） |
| `knitid-spring-boot-starter` | 已完成 |
| `knitid-benchmarks` | 已完成 JMH 基准（UUID7/UUID8/ULID/KSID/BIZ） |
| `knitid-samples` | 已完成五类 ID + Actuator 集成示例 |

## 测试

当前共 160 个测试，全部通过（`mvn verify`；Testcontainers 用例在无 Docker 时自动跳过）：

- `knitid-core`：IdType 规范化契约（3）；
- `knitid-generator-uuid7`：RFC 9562 附录 A.6 向量、属性测试、边界与溢出策略（11）；
- `knitid-generator-uuid8`：三种预设布局、版本/变体位、计数溢出、节点字段、随机预设（10）；
- `knitid-generator-ulid`：ULID 规范示例向量、Crockford Base32、单调递增、溢出策略、属性测试（15）；
- `knitid-generator-biz`：模板编译、序列日切/永不重置、校验位（MOD37_36/MOD11_2/LUHN）、前缀/业务日期上下文、并发唯一（16）；
- `knitid-generator-ksid`：位分配校验、回拨 `WAIT`/`THROW`/`BORROW_FUTURE`、序列溢出、WorkerId 越界、租约生命周期、健康快照（23）；
- `knitid-sequence-redis`：原子 INCRBY、TTL、批量区间、跨实例共享、失败映射、自动配置（5 + 4 Testcontainers + 2），含两个 BIZ 生成器共享序列的集成测试；
- `knitid-worker-redis`：槽位占用/冲突、租约续约失败、释放、provider 配置校验、自动配置（9），以及 Testcontainers 真实 Redis 的 SET NX/TTL/续约/释放/断连（5）；
- `knitid-worker-zookeeper`：临时节点分配、重新占用、续约、配置校验、自动配置（6），以及 Testcontainers 真实 ZooKeeper 的分配/释放与断连（2）；
- `knitid-worker-kubernetes`：StatefulSet ordinal、Downward API、越界/非法 pod 名、配置校验、自动配置（9）；
- `knitid-spring-boot-autoconfigure`：12 个启动行为矩阵场景 + 7 个健康检查场景 + 7 个 IdParser 场景 + 7 个端点/指标场景 + 1 个禁用场景（34）；
- `knitid-samples`：UUIDv7/UUID8/ULID/KSID/BIZ + Actuator 集成启动测试（6）。

## 业务 ID 快速配置

```yaml
knitid:
  generators:
    order-id:
      type: BIZ
      biz:
        template: "{prefix}-{yyyyMMdd}-{seq:6}-{check}"
        prefix: ORD
        zone-id: Asia/Shanghai
        node-id: 1
        sequence-provider: memory   # memory / redis（需 knitid-sequence-redis）/ 自定义 Bean
        reset: DAILY                # DAILY | NEVER
        check-algorithm: MOD37_36   # MOD37_36 | MOD11_2 | LUHN | NONE
```

## IdParser

`knitid-spring-boot-autoconfigure` 提供 `IdParser` 自动配置：

- 内置识别 UUIDv7、UUIDv8、ULID、纯数字 KSID；
- UUIDv7 提取时间戳/版本，ULID 提取时间戳；UUIDv8 仅暴露版本/变体；数字 KSID 仅暴露类型与原值（位布局由配置决定）；
- BIZ 或需要解析 KSID 时间戳/节点/序列时，注册自己的 `IdTypeParser` Bean，用户 Bean 优先于内置解析器；
- `parse` 失败抛 `IdParseException`，`tryParse` 返回 `Optional.empty()`。

## JMH 基准测试

```bash
# 构建可执行基准 jar
mvn -pl knitid-benchmarks -am package -DskipTests

# 全量运行
java -jar knitid-benchmarks/target/knitid-benchmarks.jar

# 快速冒烟（单 fork、单轮）
java -jar knitid-benchmarks/target/knitid-benchmarks.jar -wi 1 -i 1 -f 1 -w 1 -r 1 Uuid7Benchmark
```

内置基准：`Uuid7Benchmark`、`Uuid8Benchmark`、`UlidBenchmark`、`KsidBenchmark`（数字）、`BizBenchmark`（模板 + 内存序列）。

## Actuator

- **端点**：`/actuator/knitid`，返回默认生成器、生成器列表（名称/类型/默认标记/是否阻塞）；
  暴露配置：`management.endpoints.web.exposure.include=knitid`。
- **指标**（Micrometer）：
  - `knitid.id.generation.duration`（Timer，tags `type`/`name`）；
  - `knitid.id.generation.errors`（Counter，tags `type`/`name`/`reason`）；
  - `knitid.id.batch.size`（DistributionSummary，tags `type`/`name`）；
  - `knitid.id.worker.id`（Gauge，tags `name`，仅 KSID 且有 WorkerId 时注册）。
- 指标包装通过 `IdGeneratorDecorator` SPI 完成，Micrometer 不存在时不生效、不影响启动。

### Testcontainers 集成测试

以下集成测试使用 Testcontainers：

- `knitid-worker-redis`：真实 Redis 的 SET NX、TTL、续约、释放与断连；
- `knitid-worker-zookeeper`：真实 ZooKeeper 的临时节点分配/释放与容器停止后的租约丢失/连接失败。

- 需要 Docker；无 Docker 时测试通过 `@Testcontainers(disabledWithoutDocker = true)` 自动跳过；
- 默认镜像：`redis:8.10.3`、`zookeeper:3.9.6`（固定版本保证可复现）；
- 网络受限或需要私有镜像时可用环境变量覆盖：

```bash
KNITID_TEST_REDIS_IMAGE=your-registry/redis:8.10.3 \
KNITID_TEST_ZOOKEEPER_IMAGE=your-registry/zookeeper:3.9.6 \
mvn -pl knitid-worker-redis,knitid-worker-zookeeper -am test
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

## 贡献

欢迎提交 Issue/PR，请先阅读 [贡献指南](CONTRIBUTING.md) 与 [行为准则](CODE_OF_CONDUCT.md)。安全问题请按 [SECURITY.md](SECURITY.md) 私密报告。
