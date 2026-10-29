# Contributing to KnitID

感谢参与 KnitID！本文说明开发、测试、文档与提交规范。

## Prerequisites

- JDK 21（CI 使用 Temurin 21）；
- Maven 3.9+；
- Docker（Testcontainers 的 Redis/ZooKeeper 测试）；
- Git。

## Build & Test

```bash
git clone https://github.com/unitseek/knitid.git
cd knitid
mvn -B -ntp verify
```

常用命令：

```bash
mvn -B -ntp -pl knitid-core -am test       # 只跑某个模块
mvn -B -ntp -DskipTests package            # 跳过测试，仍生成 sources/javadoc
mvn -B -ntp -pl knitid-benchmarks -am package -DskipTests
```

Testcontainers 默认镜像：

- Redis：`redis:8.10.3`（可用 `KNITID_TEST_REDIS_IMAGE` 覆盖）
- ZooKeeper：`zookeeper:3.9.6`（可用 `KNITID_TEST_ZOOKEEPER_IMAGE` 覆盖）

没有 Docker 时，标记 `@Testcontainers(disabledWithoutDocker = true)` 的测试会跳过。

## Module Layout

| Module | 职责 |
| --- | --- |
| `knitid-core` | API、SPI、异常、解析接口；零 Spring/三方算法依赖 |
| `knitid-test` | 测试工具（MutableClock） |
| `knitid-generator-*` | 五类生成器 |
| `knitid-spring-boot-autoconfigure` | 自动配置、健康、端点、指标、解析器 |
| `knitid-sequence-redis` | Redis 序列 |
| `knitid-worker-*` | WorkerId 分配器 |
| `knitid-benchmarks` | JMH 基准 |
| `knitid-samples` | 示例与集成测试 |

## Code Style

- Java 21，4 空格缩进；
- 核心模块不得引入 Spring、Jackson、Micrometer 等三方依赖；
- 公共 API 必须有 Javadoc；新增/修改行为需同步 `docs/configuration.md` 或对应文档；
- 优先不可变对象、`final` 字段、明确异常；
- 配置项必须提供默认值、校验和 `spring-configuration-metadata.json` 元数据；
- 测试命名使用 `should...`/`rejects...`，覆盖边界与失败路径。

## Branching & Commits

- 从 `main` 创建分支：`feat/...`、`fix/...`、`docs/...`；
- 提交信息使用 Conventional Commits：`feat:`、`fix:`、`docs:`、`test:`、`chore:`；
- 一个 PR 只做一件事，避免混入格式化/无关重构；
- 不提交 `target/`、IDE 配置或内部 `wiki/` 文档。

## Pull Request

- 关联 Issue（如适用）；
- 通过 `mvn verify`（含 Testcontainers，若本机有 Docker）；
- 新增行为包含测试；修复 Bug 包含回归测试；
- 更新 `CHANGELOG.md`、`docs/` 与 Javadoc；
- 保持公共 API 兼容；破坏性变更需在 PR 中说明并遵循版本策略（见 `docs/compatibility.md`）。

PR 模板会自动提醒检查项。提交即表示你同意以 Apache-2.0 许可贡献代码。

## Documentation

- 配置项：`docs/configuration.md`
- 兼容性与版本：`docs/compatibility.md`
- 迁移：`docs/migration.md`
- 发布：`docs/release.md`
- 安全：`SECURITY.md`

欢迎补充文档、示例与治理流程；新增文档请同步 `docs/index.md`，并运行 `node scripts/check-docs.mjs`。

## Community

- 行为准则：`CODE_OF_CONDUCT.md`
- 支持渠道：`SUPPORT.md`
- 安全问题：不要开公开 Issue，使用 `SECURITY.md` 的私密渠道。
