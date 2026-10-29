# 0001. 使用 KSID 作为 Snowflake 风格生成器名称

- Status: Accepted
- Date: 2026-10-29

## Context

Snowflake 是通用算法名，直接作为类型名容易与既有实现/商标混淆。

## Decision

生成器类型命名为 `KSID`（Knit + Snowflake），Maven 模块为 `knitid-generator-ksid`。

## Consequences

- 公共 API/配置使用 `KSID`，不再使用 `SNOWFLAKE`；
- 文档需解释 KSID 是 Snowflake 风格算法；
- 旧内部命名不保留别名。
