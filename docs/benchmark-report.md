# KnitID JMH 基准报告

**版本**：1.0.0-SNAPSHOT
**日期**：2026-10-29
**JMH**：1.37
**运行方式**：单 fork、单线程、3 × 1s warmup、5 × 1s measurement，返回 `String`（KSID 返回 `long`）
**原始结果**：`docs/benchmark-results.json`

## 环境

| 项 | 值 |
| --- | --- |
| CPU | Intel Xeon W-10885M @ 2.40GHz（8 核 / 16 线程） |
| 内存 | 64 GB |
| 操作系统 | Windows 11 Pro for Workstations |
| JDK | OpenJDK 25.0.4.1 (Microsoft) |
| JMH | 1.37 |
| 构建 | Maven 3.9.16，`release=21` |

> 说明：库基线为 Java 21，本次基准在 JDK 25 上运行；正式发布报告建议在目标 CI/生产硬件（Java 21）复测。

## 生成器配置

| 基准 | 配置 |
| --- | --- |
| UUIDv7 | 默认单调、SecureRandom |
| UUIDv8 | `TIME_RANDOM`、SecureRandom |
| ULID | 单调、PERFORMANCE 随机源 |
| KSID | 默认 41/5/5/12 位、固定 WorkerId、无租约 |
| BIZ | `{prefix}-{yyyyMMdd}-{seq:12}-{check}`、memory 序列、MOD37_36 |

## 结果

| 基准 | 吞吐（ops/s） | 99.9% 误差 | FRD 目标 | 结论 |
| --- | --- | --- | --- | --- |
| UUIDv7 | 17,614,816 | ± 8,518,938 | ≥ 500,000 | 达标（误差受环境噪声影响较大） |
| UUIDv8 | 2,743,475 | ± 236,242 | 无硬性目标 | — |
| ULID | 6,047,289 | ± 1,067,261 | ≥ 500,000 | 达标 |
| KSID | 2,798,557 | ± 434,633 | ≥ 1,000,000 | 达标 |
| BIZ | 627,569 | ± 279,210 | ≥ 300,000 | 达标 |

> 吞吐为单线程、返回对象被 JMH 消费后的结果。UUIDv7 的误差区间较宽，建议在固定硬件、
> 关闭后台负载、增加 fork 数后复测。

## 复现

```bash
mvn -pl knitid-benchmarks -am package -DskipTests
java -jar knitid-benchmarks/target/knitid-benchmarks.jar -wi 3 -i 5 -f 1 \
  -rf json -rff knitid-benchmarks/target/jmh-results.json
```

## 结论与注意事项

- 五个生成器均达到或超过 FRD 的吞吐目标（UUID8 无硬性目标）；
- 本报告为开发机快速基准，不是严格的发布基准；正式 1.0 报告应固定硬件、增加 fork/迭代、
  记录 GC 与 CPU 频率，并在 Java 21 上复测；
- KSID 基准使用固定 WorkerId 且无续约线程，实际部署需计入租约续约开销；
- BIZ 使用内存序列，Redis 序列的额外网络开销需单独基准。
