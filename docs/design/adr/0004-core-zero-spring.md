# 0004. 核心模块零 Spring 依赖

- Status: Accepted
- Date: 2026-10-29

## Context

KnitID 既要在 Spring Boot 应用中使用，也要能用于普通 Java/CLI/批处理。

## Decision

`knitid-core` 只依赖 JDK：API、SPI、异常、解析接口、健康快照、静态门面均在 core。Spring 自动配置/健康/指标/端点放在 `knitid-spring-boot-autoconfigure`。

## Consequences

- 非 Spring 用户只引入 core + 算法模块；
- 自动配置模块可自由依赖 Boot 4，但不得把 Spring 类型泄漏到公共 API；
- 新增核心能力时先评估是否可在无 Spring 下使用。
