# Architecture Decision Records

ADR 记录不可逆或影响公共契约的决策。

| 编号 | 标题 | 状态 |
| --- | --- | --- |
| [0001](0001-ksid-name.md) | 使用 KSID 作为 Snowflake 风格生成器名称 | Accepted |
| [0002](0002-spring-boot-4-only.md) | 仅支持 Spring Boot 4 / Java 21 | Accepted |
| [0003](0003-biz-defaults.md) | BIZ 默认模板策略 | Accepted |
| [0004](0004-core-zero-spring.md) | 核心模块零 Spring 依赖 | Accepted |
| [0005](0005-uuid7-method2.md) | UUIDv7 单调使用 RFC 9562 Method 2 | Accepted |
| [0006](0006-cli.md) | 提供独立 CLI 模块 | Accepted |

## 模板

新增 ADR 时复制以下结构：

```markdown
# NNNN. 标题

- Status: Proposed | Accepted | Superseded
- Date: YYYY-MM-DD
- Deciders: ...

## Context

## Decision

## Consequences
```
