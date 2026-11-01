# AI 辅助使用指南

本文说明如何让 AI 编码助手正确使用或修改 KnitID。它不是 API 文档，权威内容以
[README](https://github.com/unitseek/knitid/blob/main/README.md)、[配置参考](configuration.md) 和 [Javadoc](https://javadoc.io/doc/org.unitseek.knitid) 为准。

## 给 AI 的上下文

让助手先读：

1. [README](https://github.com/unitseek/knitid/blob/main/README.md)
2. [配置参考](configuration.md)
3. [模块说明](modules.md)
4. [ID 选型](id-selection.md)
5. [兼容性](compatibility.md)
6. 当前任务相关模块的源码

仓库根目录的 [AGENTS.md](https://github.com/unitseek/knitid/blob/main/AGENTS.md) 提供了构建、架构和修改约束；`llms.txt` 提供精简索引。

## 推荐提示词

### 添加依赖和配置

```text
在 Spring Boot 4.1 项目中用 KnitID 生成 UUIDv7。
只引入必要模块，给出 Maven 依赖、application.yml 和最小测试。
不要编造未在 docs/configuration.md 中出现的配置键。
```

### 选型

```text
根据以下约束推荐 KnitID ID 类型：需要时间有序、单机生成、可读性低、不能依赖 Redis。
引用 docs/id-selection.md 的理由，并说明取舍。
```

### 排障

```text
应用启动报 KNITID-1002。请根据 docs/troubleshooting.md 和模块说明定位缺失依赖，
给出最小修复，不要建议修改 core。
```

### 自定义扩展

```text
按 docs/spi.md 实现一个自定义 SequenceProvider，并补单元测试。
保持 knitid-core 零 Spring 依赖。
```

## 安全边界

- 不要把生产数据库 URL、Redis 密码、私钥、passphrase 粘贴给 AI。
- 不要把真实用户 ID 或业务数据放进公开 issue/PR。
- 生成示例配置时使用占位符，不要使用真实凭据。
- 让 AI 修改代码后，必须运行测试和 `node scripts/check-docs.mjs`。

## 让 AI 修改本仓库

建议流程：

1. 明确目标模块和验收标准；
2. 要求先读 `AGENTS.md` 和相关文档；
3. 要求先补测试；
4. 要求运行：

```bash
mvn -B -ntp verify
node scripts/check-docs.mjs
```

5. 检查 diff，确认没有无关重构、密钥、生成物。

## CLI 与 AI

CLI 适合让 AI 生成可复现的终端命令：

```bash
java -jar knitid-cli/target/knitid-cli-1.0.0.jar gen --type UUID7
java -jar knitid-cli/target/knitid-cli-1.0.0.jar parse <id>
```

CLI v1 不支持 BIZ、KSID 完整生成/位布局、Redis/DB 序列和 WorkerId；这些场景要求 AI 使用
Java/Spring API，而不是猜测 CLI 参数。

## 不允许的“帮助”

- 不要建议把 `knitid-core` 改成依赖 Spring；
- 不要建议关闭配置校验来“修复”启动失败；
- 不要在未说明兼容性影响时改 KSID epoch、位宽或 BIZ 模板；
- 不要声称测试通过而没有实际输出；
- 不要建议把 secret 写进 `application.yml` 示例。

## 相关文档

- [AGENTS.md](https://github.com/unitseek/knitid/blob/main/AGENTS.md)
- [llms.txt](https://github.com/unitseek/knitid/blob/main/llms.txt)
- [CLI](cli.md)
- [SPI](spi.md)
- [排障](troubleshooting.md)
