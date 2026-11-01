# KnitID

[![CI](https://github.com/unitseek/knitid/actions/workflows/ci.yml/badge.svg)](https://github.com/unitseek/knitid/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21+-blue.svg)](https://adoptium.net/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0%20%7C%204.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Javadoc](https://img.shields.io/badge/javadoc-site-blue)](https://unitseek.github.io/knitid/)

统一、模块化的 Spring Boot ID 生成工具库：UUIDv7、UUIDv8、ULID、KSID（Snowflake 算法）、业务 ID、KSUID、TypeID。

## 状态

1.0.0 发布准备版：七类 ID 算法（UUIDv7、UUIDv8、ULID、KSID、BIZ、KSUID、TypeID）、IdParser、Redis/ZooKeeper/Kubernetes WorkerId 分配器、Redis 序列模块、Actuator 端点/健康检查/指标、JMH 基准、示例工程、CLI v1、Spring Boot 自动配置与契约测试均已完成；包含 JDBC 序列、BIZ/KSID 内置解析器、异步/流式批量、脱敏配置诊断和 Spring Boot 4.0 兼容测试。

## 快速开始

要求 JDK 21+、Spring Boot 4.0/4.1。引入 Starter 与 UUIDv7 模块：

```xml
<dependency>
  <groupId>org.unitseek.knitid</groupId>
  <artifactId>knitid-spring-boot-starter</artifactId>
  <version>1.0.0</version>
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

## GraalVM Native

示例工程支持 Native Image 冒烟构建：

```bash
mvn -B -ntp -pl knitid-samples -am -Pnative -DskipTests package
./knitid-samples/target/knitid-samples
```

CI 工作流：`.github/workflows/native.yml`（workflow_dispatch / 每周计划）。

## 模块

| 模块 | 状态 |
| --- | --- |
| `knitid-bom` | 已完成（Spring Boot 依赖 + 模块版本管理） |
| `knitid-core` | 已完成（API/SPI/异常/静态门面/DurationParser/健康快照） |
| `knitid-generator-uuid7` | 已完成实现 + 契约/属性测试 |
| `knitid-generator-uuid8` | 已完成实现 + 测试（TIME_RANDOM/TIME_NODE_RANDOM/RANDOM 预设、版本/变体） |
| `knitid-generator-ulid` | 已完成实现 + 契约/属性测试（ULID 规范向量、Crockford Base32、单调/溢出） |
| `knitid-generator-biz` | 已完成实现 + 测试（模板、序列、日切、校验位、上下文） |
| `knitid-generator-ksid` | 已完成实现 + 测试（位分配校验、回拨策略、序列溢出、WorkerId 租约、健康快照） |
| `knitid-generator-ksuid` | 已完成实现 + 测试（27 位 Base62、可配置随机源、解析） |
| `knitid-generator-typeid` | 已完成实现 + 测试（TypeID v0.3.0、多前缀、解析） |
| `knitid-sequence-redis` | 已完成实现 + Testcontainers 集成测试（原子 INCRBY、日切 TTL、NEVER 持久 scope、批量、跨实例） |
| `knitid-sequence-jdbc` | 已完成实现（数据库号段、SELECT FOR UPDATE 事务预留、H2 并发测试） |
| `knitid-worker-redis` | 已完成实现 + 单测 + Testcontainers 集成测试（SET NX/TTL 租约、token fencing、续约、释放、槽位扫描、断连） |
| `knitid-worker-zookeeper` | 已完成实现 + 测试 + Testcontainers 集成测试（临时节点会话租约、ownership/release 校验、槽位扫描、续约检查、断连） |
| `knitid-worker-kubernetes` | 已完成实现 + 测试（StatefulSet ordinal / Downward API） |
| `knitid-spring-boot-autoconfigure` | 已完成（配置/注册中心/默认选择/自动配置矩阵/健康检查/生命周期关闭/IdParser/Actuator 端点/指标） |
| `knitid-spring-boot-starter` | 已完成（不含算法模块） |
| `knitid-spring-boot-starter-all` | 已完成（包含全部七类算法模块） |
| `knitid-benchmarks` | 已完成 JMH 基准（UUID7/UUID8/ULID/KSID/BIZ） |
| `knitid-samples` | 已完成七类 ID + Actuator 集成示例 |
| `knitid-cli` | 已完成 CLI v1（gen/parse，无 Spring、无外部基础设施） |

## 测试

当前共 220 个测试（Boot 4.0 兼容矩阵另加 1 个），全部通过（`mvn verify`；Testcontainers 用例在无 Docker 时自动跳过）：

- `knitid-core`：IdType 规范化契约 + DurationParser 边界 + 异步/流式批量 API（9）；
- `knitid-generator-uuid7`：RFC 9562 附录 A.6 向量、属性测试、边界、溢出与回拨策略（12）；
- `knitid-generator-uuid8`：三种预设布局、版本/变体位、计数溢出、节点字段、随机预设（10）；
- `knitid-generator-ulid`：ULID 规范示例向量、Crockford Base32、单调递增、溢出策略、属性测试（15）；
- `knitid-generator-biz`：模板编译、序列日切/永不重置、校验位（MOD37_36/MOD11_2/LUHN）、前缀/业务日期上下文、并发唯一、内置模板解析器（19）；
- `knitid-generator-ksid`：位分配校验、回拨 `WAIT`/`THROW`/`BORROW_FUTURE`、序列溢出、WorkerId 越界、租约生命周期、健康快照、内置位布局解析器（24）；
- `knitid-generator-ksuid`：27 位 Base62、时间戳、编解码往返、解析、配置与自动配置（8）；
- `knitid-generator-typeid`：官方向量、前缀规则、多前缀、编解码往返、解析、配置与自动配置（13）；
- `knitid-sequence-redis`：原子 INCRBY、TTL/持久 scope、批量区间、跨实例共享、失败映射、自动配置（7 + 5 Testcontainers + 2），含两个 BIZ 生成器共享序列的集成测试；
- `knitid-sequence-jdbc`：数据库号段、事务行锁、跨块连续、并发唯一、回滚隔离、自动配置（4）；
- `knitid-boot-compat-4.0`：Spring Boot 4.0.9 兼容启动与 UUIDv7 生成（1，仅 `-Pboot-4.0`）；
- `knitid-worker-redis`：槽位占用/冲突、token fencing、租约续约失败、释放、provider 配置校验、自动配置（9），以及 Testcontainers 真实 Redis 的 SET NX/TTL/续约/释放/断连（8）；
- `knitid-worker-zookeeper`：临时节点分配、重新占用、续约、配置校验、自动配置（6），以及 Testcontainers 真实 ZooKeeper 的分配/释放、foreign release 与断连（3）；
- `knitid-worker-kubernetes`：StatefulSet ordinal、Downward API、越界/非法 pod 名、配置校验、自动配置（9）；
- `knitid-spring-boot-autoconfigure`：启动矩阵/健康/解析/端点/指标/配置诊断/回拨指标/索引 Map/自动绑定解析器（42）；
- `knitid-spring-boot-starter`：依赖边界，确认不含生成器类，且无生成器模块时可启动（2）；
- `knitid-spring-boot-starter-all`：依赖边界，确认包含七类生成器（1）；
- `knitid-samples`：七类 ID + Actuator + 多租户前缀示例启动测试（7）；
- `knitid-cli`：help/version、UUID7 批量、自动解析、JSON 输出、非法类型退出码（5）。

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

- 内置识别 UUIDv7、UUIDv8、ULID、纯数字 KSID；引入 KSUID/TypeID 模块后，其解析器 Bean 自动加入；
- 配置 BIZ/KSID generator 时，自动绑定对应模板/位布局解析器；用户自定义 `IdTypeParser` Bean 优先；
- UUIDv7 提取时间戳/版本，ULID 提取时间戳；UUIDv8 仅暴露版本/变体；BIZ/KSID 提取模板/布局可解析的元数据；
- `parse` 失败抛 `IdParseException`，`tryParse` 返回 `Optional.empty()`。

## 示例工程

`knitid-samples` 配置了 UUIDv7、UUIDv8、ULID、KSID、BIZ、KSUID、TypeID 七个生成器，并提供一个 `CommandLineRunner`，启动时在控制台输出七类 ID：

```bash
# 首次运行：先把本地模块安装到 ~/.m2
mvn -B -ntp -DskipTests install
mvn -pl knitid-samples spring-boot:run
# 或在 IDEA 直接运行 SampleApplication
```

输出结构：

```text
KnitID sample IDs (one per generator)
-------------------------------------
uuid7  UUID7 ...
uuid8  UUID8 ...
ulid   ULID  ...
ksid   KSID  ...
biz    BIZ   ...
ksuid  KSUID ...
typeid TYPEID sample_...
-------------------------------------
```

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

- **端点**：`/actuator/knitid`（生成器列表）与 `/actuator/knitid-config`（脱敏配置诊断）；
  暴露配置：`management.endpoints.web.exposure.include=knitid`。
- **指标**（Micrometer）：
  - `knitid.id.generation.duration`（Timer，tags `type`/`name`/`id.type`/`id.name`）；
  - `knitid.id.generation.errors`（Counter，tags `type`/`name`/`reason`）；
  - `knitid.id.batch.size`（DistributionSummary，tags `type`/`name`）；
  - `knitid.id.worker.id`（Gauge，tags `name`/`id.type`/`id.name`）；
- `knitid.id.clock.backwards`（Gauge，健康窗口内回拨次数）。
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
