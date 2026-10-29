# 0005. UUIDv7 单调使用 RFC 9562 Method 2

- Status: Accepted
- Date: 2026-10-29

## Context

UUIDv7 同毫秒内需要可排序/不重复；RFC 9562 提供多种单调方式。

## Decision

同毫秒内把 74 位随机区当作计数器递增（Method 2）；空间耗尽默认等待下一毫秒，可配置 `THROW`。非单调模式每次重新随机。

## Consequences

- 同进程同毫秒 ID 严格递增；
- 每毫秒容量 2^74，实际不会耗尽；
- 与 RFC 向量兼容（附录 A.6）；
- 跨进程仍依赖时钟与随机位，不保证全局顺序。
