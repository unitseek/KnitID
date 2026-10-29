# Architecture Decision Records

> 状态：🚧 P1 骨架。ADR 记录不可逆或影响公共契约的决策。

| 编号 | 标题 | 状态 |
| --- | --- | --- |
| 0001 | 使用 KSID 作为 Snowflake 风格生成器名称 | Accepted |
| 0002 | 仅支持 Spring Boot 4 / Java 21 | Accepted |
| 0003 | BIZ 默认 MOD37_36 / DAILY / THROW | Accepted |
| 0004 | 核心模块零 Spring 依赖 | Accepted |
| 0005 | UUIDv7 单调使用 RFC 9562 Method 2 | Accepted |

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
