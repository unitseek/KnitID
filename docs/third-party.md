# 第三方依赖与许可证

KnitID 自身以 Apache-2.0 发布。下表列出主要直接/可选依赖及其许可证；完整依赖树以 Maven 为准，本页不构成法律意见。

## 运行时（可选）

| 依赖 | 用途 | 许可证 |
| --- | --- | --- |
| Spring Boot / Spring Framework | 自动配置、依赖管理 | Apache-2.0 |
| Spring Data Redis | Redis WorkerId/序列 | Apache-2.0 |
| Lettuce | Redis 客户端 | Apache-2.0 |
| Apache Curator | ZooKeeper 客户端 | Apache-2.0 |
| Apache ZooKeeper | WorkerId 协调 | Apache-2.0 |
| Micrometer | 指标 | Apache-2.0 |

## 构建/测试/基准（非运行时）

| 依赖 | 用途 | 许可证 |
| --- | --- | --- |
| JUnit | 测试 | EPL-2.0 |
| AssertJ | 断言 | Apache-2.0 |
| Mockito | Mock | MIT |
| Testcontainers | 集成测试 | MIT |
| JMH | 基准 | GPL-2.0 with Classpath Exception |
| Maven plugins | 构建/发布 | Apache-2.0 |
| Maven Assembly Plugin | CLI 可执行 JAR 打包 | Apache-2.0 |

## 说明

- 可选依赖不会传递到不使用对应功能的用户；
- 升级依赖时需检查许可证变化；
- 发布构件不包含第三方源代码（sources jar 仅含本仓库代码）；
- 如发现遗漏，请提交 Issue 或 PR。
