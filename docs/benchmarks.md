# 基准测试

完整结果见 [基准报告](benchmark-report.md)，原始数据见 [benchmark-results.json](benchmark-results.json)。

## 运行

```bash
mvn -pl knitid-benchmarks -am package -DskipTests
java -jar knitid-benchmarks/target/knitid-benchmarks.jar
# 快速冒烟
java -jar knitid-benchmarks/target/knitid-benchmarks.jar -wi 1 -i 1 -f 1 -w 1 -r 1 Uuid7Benchmark
```

## 方法

- JMH 1.37，3 forks、单线程、5×1s warmup、10×1s measurement；
- 返回 `String`（KSID 返回 `long`），由 JMH 消费；
- 环境：Intel Xeon W-10885M（8C/16T）、64 GB、Windows 11、OpenJDK 21.0.8；
- 已在本机 Java 21 固定环境（3 forks、5×1s warmup、10×1s measurement）完成 1.0.0 基准复测。

## 结果摘要

| 基准 | 吞吐（ops/s） | 99.9% 误差 | 目标 |
| --- | --- | --- | --- |
| UUIDv7 | 21,458,510 | ± 1,719,754 | ≥ 500k |
| UUIDv8 | 3,328,708 | ± 235,680 | — |
| ULID | 5,173,384 | ± 422,448 | ≥ 500k |
| KSID | 2,532,802 | ± 445,671 | ≥ 1M |
| BIZ | 336,043 | ± 11,183 | ≥ 300k |

## 解读

- UUIDv7 最快，但误差区间受开发机噪声影响大；
- ULID 需要 Base32 编码，约为 UUIDv7 的 1/3；
- KSID 有同步与序列逻辑，仍满足 1M 目标；
- BIZ 含模板拼接/校验位，吞吐最低，但通常不是瓶颈；
- 附加基准覆盖多线程 memory 序列与 RedisSequenceProvider 的本地逻辑开销；
- 官方报告未覆盖真实 Redis 网络 RTT、Redis 集群/故障、WorkerId 续约与数据库写入。

## 附加基准

```bash
java -jar knitid-benchmarks/target/knitid-benchmarks.jar ConcurrentSequenceBenchmark
java -jar knitid-benchmarks/target/knitid-benchmarks.jar RedisSequenceBenchmark
```

- `ConcurrentSequenceBenchmark`：8 线程内存序列；
- `RedisSequenceBenchmark`：用内存 store 隔离 Redis 网络，测 provider 自身开销；不代表真实 Redis 延迟。

## 添加基准

在 `knitid-benchmarks` 新建类，使用 `@State(Scope.Thread)` + `@Benchmark`，并在 `@Setup` 初始化生成器；避免把日志/IO 混入基准。
