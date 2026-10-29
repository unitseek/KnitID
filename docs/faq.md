# FAQ

> 状态：🚧 P1 骨架。欢迎通过 PR 补充问题。

## 唯一性

- KnitID 保证 ID 全局唯一吗？
- 多实例 KSID 如何避免 WorkerId 冲突？
- BIZ 的序列会重复吗？

## 时间与时钟

- 时钟回拨会怎样？
- `WAIT` / `THROW` / `BORROW_FUTURE` 怎么选？
- 容器环境需要 NTP 吗？

## 安全

- UUIDv7/ULID 会泄露生成时间吗？
- 哪些 ID 不适合做令牌？
- WorkerId 是否敏感？

## 运维

- 内存序列能用于生产吗？
- Redis 序列 vs 数据库号段？
- 如何升级/迁移已有 ID？

## 兼容性

- 支持 Spring Boot 3 吗？
- 支持 Java 17 吗？
- 支持 GraalVM Native 吗？
