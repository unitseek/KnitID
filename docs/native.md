# Native Image 冒烟

KnitID 提供 GraalVM Native Image 冒烟验证，用于确认 core、算法模块和 CLI 依赖在 native image 下可以构建并生成 ID。

## 范围

当前 native 冒烟入口：

```text
knitid-samples/src/main/java/org/unitseek/knitid/samples/NativeSmokeMain.java
```

它不启动 Spring 上下文，直接验证：

- UUIDv7；
- KSUID；
- TypeID。

这是生成器/依赖层面的 native 验证，不等同于完整 Spring Boot Native 上下文验证。

## 构建

要求 GraalVM for JDK 21；普通 OpenJDK 不包含 `native-image`。

```bash
export JAVA_HOME=/path/to/graalvm-jdk-21
export GRAALVM_HOME="$JAVA_HOME"
mvn -B -ntp -pl knitid-samples -am -Pnative -DskipTests package
./knitid-samples/target/knitid-samples
```

Windows：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\graalvm-jdk-21.0.12+7.1"
$env:GRAALVM_HOME = $env:JAVA_HOME
mvn -B -ntp -pl knitid-samples -am -Pnative -DskipTests package
.\knitid-samples\target\knitid-samples.exe
```

## 验证输出

```text
uuid7  <uuid>
ksuid  <27 chars>
typeid sample_<26 chars>
```

本次 1.0.0 验证结果：

```text
BUILD SUCCESS
Finished generating 'knitid-samples' in 2m 10s
EXIT=0
```

## CI

- 工作流：`.github/workflows/native.yml`
- 触发：`workflow_dispatch` + 每周计划
- GraalVM：`graalvm/setup-graalvm@v1`，JDK 21
- 依赖：`aspectjweaver`（仅为 Spring reachability metadata）和短等待相关 native 参数

## 已知边界

- 当前冒烟不启动 Spring Boot 上下文；
- Spring Boot Native AOT 的完整应用验证需单独工程/版本矩阵；
- 生成器性能基准在 JVM 上运行，不是 native 性能结论；
- Native 构建资源开销较大，不适合作为每个 PR 的必跑项。

## 相关文档

- [非 Spring 用法](non-spring.md)
- [基准测试](benchmarks.md)
- [发布流程](release.md)
