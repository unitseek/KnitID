# 运维指南

> 状态：🚧 P1 骨架。

## WorkerId 规划

TODO：fixed/environment/Redis/ZooKeeper/Kubernetes 的适用场景、容量、故障处理。

## 时钟与 NTP

TODO：NTP 要求、回拨策略选择、监控指标、容器时钟注意事项。

## Redis / ZooKeeper

TODO：高可用、认证/TLS、key 前缀、TTL、容量、故障降级。

## Kubernetes

TODO：StatefulSet ordinal、Downward API、Pod 重建、滚动发布。

## 健康与指标

TODO：`/actuator/knitid`、`knitId` 健康键、指标含义与告警阈值。

## 容量与性能

TODO：单实例 QPS、批量、Redis 序列延迟、基准报告链接。

## 发布/回滚

TODO：配置变更、WorkerId 迁移、回滚风险。
