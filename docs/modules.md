# 模块说明

> 状态：🚧 P1 骨架。

## 模块图

TODO：补充 core ← generator → autoconfigure/starter 与 worker/sequence 的依赖图。

## 模块选择

| 需求 | 引入 |
| --- | --- |
| Spring Boot 自动配置 | `knitid-spring-boot-starter` |
| UUIDv7 | `knitid-generator-uuid7` |
| UUIDv8 | `knitid-generator-uuid8` |
| ULID | `knitid-generator-ulid` |
| KSID | `knitid-generator-ksid` |
| BIZ | `knitid-generator-biz` |
| Redis WorkerId | `knitid-worker-redis` |
| ZooKeeper WorkerId | `knitid-worker-zookeeper` |
| Kubernetes WorkerId | `knitid-worker-kubernetes` |
| Redis 序列 | `knitid-sequence-redis` |
| 非 Spring 使用 | `knitid-core` + 算法模块 |

## BOM

TODO：补充 `knitid-bom` import 示例与版本管理建议。

## 依赖规则

- `knitid-core` 不依赖 Spring/算法库；
- 生成器模块只依赖 core；
- 自动配置/Starter 依赖可选集成模块；
- 示例与基准不进入生产依赖。
