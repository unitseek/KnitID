# 0002. 仅支持 Spring Boot 4 / Java 21

- Status: Accepted
- Date: 2026-10-29

## Context

Boot 3 与 Boot 4 的自动配置/Actuator/健康 API 存在不兼容变化，同时支持两代成本高。

## Decision

1.0 仅支持 Spring Boot 4.0.x/4.1.x 与 Java 21+；Boot 3 用户需升级。

## Consequences

- 自动配置基于 `AutoConfiguration.imports`；
- 健康检查使用 `spring-boot-health` 的 `HealthContributor`；
- 不支持 Boot 3 是明确边界，需在 README/兼容性文档中说明。
