# 基准测试

> 状态：🚧 P1 骨架。完整结果见 [基准报告](benchmark-report.md)。

## 运行

```bash
mvn -pl knitid-benchmarks -am package -DskipTests
java -jar knitid-benchmarks/target/knitid-benchmarks.jar
```

## 方法

TODO：硬件、JDK、fork/迭代、预热、黑盒、GC、噪声控制。

## 结果解读

TODO：吞吐目标、单线程 vs 多线程、Redis 序列网络开销、租约续约开销。
