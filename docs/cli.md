# CLI

KnitID CLI 用于在终端、脚本和排障场景中生成/解析 ID，不依赖 Spring Boot。

- v1.0.0 状态：可用，覆盖无基础设施依赖的本地操作；
- 不访问 Redis、数据库、ZooKeeper、Kubernetes；
- 不启动 daemon，不读取隐式配置文件。

## 构建与运行

```bash
mvn -B -ntp -pl knitid-cli -am -DskipTests package
java -jar knitid-cli/target/knitid-cli-1.0.0.jar --help
```

打包产物是包含运行时依赖的可执行 JAR。

## 命令总览

```text
knitid <command> [options]

Commands:
  gen       生成 ID
  parse     解析 ID
  help      帮助
  version   版本
```

### gen

```text
knitid gen --type <TYPE> [options]
```

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `--type` | 必填 | `UUID7`、`UUID8`、`ULID`、`KSUID`、`TYPEID` |
| `--count` | `1` | 数量，范围 1..10000 |
| `--prefix` | `knitid` | 仅 TypeID 使用 |
| `--format` | `text` | `text` 或 `json` |

示例：

```bash
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type UUID7
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type ULID --count 5
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type TYPEID --prefix order
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type UUID7 --count 2 --format json
```

文本输出每行一个 ID：

```text
01a19673-2cb7-7ab2-a3b6-b7db7cca2b22
```

JSON 输出：

```json
[
  {"type":"UUID7","id":"01a19673-2cb7-7ab2-a3b6-b7db7cca2b22"},
  {"type":"UUID7","id":"01a19673-2cfa-7e7b-9112-fd2808001bc5"}
]
```

### parse

```text
knitid parse <id> [options]
```

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `--type` | 自动识别 | 强制指定类型 |
| `--format` | `text` | `text` 或 `json` |

支持：

| 类型 | 输出元数据 |
| --- | --- |
| UUID7 | 版本、时间戳 |
| UUID8 | 版本 |
| ULID | 时间戳 |
| KSUID | 时间戳、类型 |
| TypeID | 前缀、时间戳 |
| 纯数字 KSID | 类型、原值；无位布局信息 |

示例：

```bash
java -jar knitid-cli/target/knitid-cli-1.0.0.jar parse 01a19673-2cb7-7ab2-a3b6-b7db7cca2b22
java -jar knitid-cli/target/knitid-cli-1.0.0.jar parse order_01m6b76bmvfn98wgj883896zeh --format json
```

文本输出示例：

```text
type=UUID7
raw=01a19673-2cb7-7ab2-a3b6-b7db7cca2b22
timestamp=2026-11-01T09:31:33.115Z
version=7
```

JSON 输出示例：

```json
{"type":"UUID7","raw":"01a19673-2cb7-7ab2-a3b6-b7db7cca2b22","timestamp":"2026-11-01T09:31:33.115Z","version":7}
```

## 退出码

| 退出码 | 含义 |
| --- | --- |
| `0` | 成功 |
| `2` | 参数/用法错误 |
| `3` | 生成或解析失败 |

错误信息输出到 stderr，不把原始敏感输入拼进错误消息。

## v1 明确不支持

以下能力需要配置或外部基础设施，继续使用 Java/Spring API：

- BIZ 生成/解析；
- KSID 生成与完整位布局解析；
- Redis/数据库序列；
- Redis/ZooKeeper/Kubernetes WorkerId 分配；
- 长时间运行的 daemon/服务模式；
- 从配置文件读取多 generator 配置。

## 设计约束

- CLI 不依赖 Spring，保持可独立运行；
- 不引入额外 CLI 框架，v1 使用简单参数解析；
- JSON 输出字段保持稳定，新增字段不破坏旧消费者；
- 解析失败不通过异常堆栈暴露输入内容；
- 后续扩展优先增加只读/本地能力，不把基础设施依赖带进 v1 默认路径。

## 后续候选

- `--type BIZ`：显式提供模板、前缀、时区、内存序列；
- `--type KSID`：显式提供 epoch、位宽、worker/datacenter；
- `knitid inspect`：从标准输入批量解析；
- 配置文件支持；
- GraalVM native 单文件分发。

## 相关文档

- [ADR 0006](design/adr/0006-cli.md)
- [非 Spring 用法](non-spring.md)
- [模块说明](modules.md)
