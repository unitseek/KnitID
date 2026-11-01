# KnitID JMH 基准报告

**版本**：1.0.0
**日期**：2026-11-01
**JMH**：1.37
**运行方式**：3 forks、单线程、5 × 1s warmup、10 × 1s measurement，返回 `String`（KSID 返回 `long`）
**原始结果**：`docs/benchmark-results.json`

## 环境

| 项 | 值 |
| --- | --- |
| CPU | Intel Xeon W-10885M @ 2.40GHz（8 核 / 16 线程） |
| 内存 | 64 GB |
| 操作系统 | Windows 11 Pro for Workstations |
| JDK | OpenJDK 21.0.8 (Microsoft) |
| JMH | 1.37 |
| 构建 | Maven 3.9.16，`release=21` |

> 这是 1.0.0 发布前的固定开发机 Java 21 基准。正式生产容量仍需按目标硬件、JVM 参数和实际负载复测。

## 生成器配置

| 基准 | 配置 |
| --- | --- |
| UUIDv7 | 默认单调、SecureRandom |
| UUIDv8 | `TIME_RANDOM`、SecureRandom |
| ULID | 单调、PERFORMANCE 随机源 |
| KSID | 默认 41/5/5/12 位、固定 WorkerId、无租约 |
| BIZ | `{prefix}-{yyyyMMdd}-{seq:12}-{check}`、memory 序列、MOD37_36 |

## 结果

| 基准 | 吞吐（ops/s） | 99.9% 误差 | 需求目标 | 结论 |
| --- | --- | --- | --- | --- |
| UUIDv7 | 21,458,510 | ± 1,719,754 | ≥ 500,000 | 达标 |
| UUIDv8 | 3,328,708 | ± 235,680 | 无硬性目标 | — |
| ULID | 5,173,384 | ± 422,448 | ≥ 500,000 | 达标 |
| KSID | 2,532,802 | ± 445,671 | ≥ 1,000,000 | 达标 |
| BIZ | 336,043 | ± 11,183 | ≥ 300,000 | 达标 |

> 结果为单线程、结果被 JMH 消费后的吞吐。KSID/UUID8 处于时间序列高负载路径，短窗口等待在 Windows 上曾受约 15.6ms 定时器粒度影响；生成器已改为先短暂自旋再 park，表中为修复后的数据。

## 附加基准

以下基准不改变上面的正式单线程结果：

- `ConcurrentSequenceBenchmark`：8 线程内存序列吞吐；
- `RedisSequenceBenchmark`：内存 store 下的 RedisSequenceProvider 本地开销，不含真实网络 RTT。

运行方式见 [基准测试](benchmarks.md)。

## 复现

```bash
mvn -pl knitid-benchmarks -am package -DskipTests
java -jar knitid-benchmarks/target/knitid-benchmarks.jar \
  Uuid7Benchmark Uuid8Benchmark UlidBenchmark KsidBenchmark BizBenchmark \
  -wi 5 -i 10 -f 3 -rf json -rff docs/benchmark-results.json
```

## 结论与注意事项

- 五个生成器均达到或超过需求吞吐目标（UUID8 无硬性目标）；
- KSID 基准使用固定 WorkerId 且无续约线程，实际部署需计入租约续约开销；
- BIZ 使用内存序列，Redis 序列的额外网络开销需单独基准；
- 开发机结果不构成生产 SLA，生产容量需结合目标硬件、JVM、线程数和 Redis/DB 延迟评估。
