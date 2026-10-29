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

- JMH 1.37，单 fork、单线程、3×1s warmup、5×1s measurement；
- 返回 `String`（KSID 返回 `long`），由 JMH 消费；
- 环境：Intel Xeon W-10885M（8C/16T）、64 GB、Windows 11、OpenJDK 25.0.4.1；
- 库基线 Java 21，正式报告应在 Java 21 固定硬件复测。

## 结果摘要

| 基准 | 吞吐（ops/s） | 99.9% 误差 | 目标 |
| --- | --- | --- | --- |
| UUIDv7 | 17,614,816 | ± 8,518,938 | ≥ 500k |
| UUIDv8 | 2,743,475 | ± 236,242 | — |
| ULID | 6,047,289 | ± 1,067,261 | ≥ 500k |
| KSID | 2,798,557 | ± 434,633 | ≥ 1M |
| BIZ | 627,569 | ± 279,210 | ≥ 300k |

## 解读

- UUIDv7 最快，但误差区间受开发机噪声影响大；
- ULID 需要 Base32 编码，约为 UUIDv7 的 1/3；
- KSID 有同步与序列逻辑，仍满足 1M 目标；
- BIZ 含模板拼接/校验位，吞吐最低，但通常不是瓶颈；
- 未覆盖：多线程、Redis 序列 RTT、WorkerId 续约、数据库写入。

## 添加基准

在 `knitid-benchmarks` 新建类，使用 `@State(Scope.Thread)` + `@Benchmark`，并在 `@Setup` 初始化生成器；避免把日志/IO 混入基准。
