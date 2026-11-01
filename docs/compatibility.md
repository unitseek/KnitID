# 兼容性与版本策略

## Runtime Matrix

| 组件 | 支持范围 | 说明 |
| --- | --- | --- |
| Java | **21+** | 编译 target 21；CI 使用 Temurin 21 |
| Spring Boot | **4.0.x / 4.1.x** | 仅支持 Boot 4；不支持 3.x |
| Maven | 3.9+ | 构建与发布 |
| Redis（可选） | 6.2+ / 7.x / 8.x | WorkerId 租约、Redis 序列 |
| ZooKeeper（可选） | 3.9.x | WorkerId 分配 |
| Kubernetes（可选） | Downward API / StatefulSet | WorkerId 分配 |
| GraalVM Native | 支持（需按模块验证） | 核心无反射依赖；Spring AOT 由 Boot 处理 |
| Micrometer | Boot 4 管理版本 | 指标可选 |
| KSUID/TypeID | v1.0.0 可选模块 | 普通 starter 不包含；需显式引入 |
| Spring Boot 4.0 | 兼容矩阵 | `mvn -Pboot-4.0 verify`；默认 4.1.x 构建 |
| Spring Boot 4.1 | 默认 | 根 POM 当前 4.1.2 |

> 库的核心模块（`knitid-core`）零 Spring 依赖，可脱离 Spring Boot 使用；但 Spring Boot Starter/自动配置仅支持 Boot 4.x。

## Compatibility Test Matrix

| 组合 | 状态 |
| --- | --- |
| Java 21 + Boot 4.1.x | ✅ CI 主路径 |
| Java 21 + Boot 4.0.x | ⚠️ 目标支持，尚未纳入 CI 矩阵 |
| Java 25 + Boot 4.1.x | ⚠️ 开发机验证（基准/本地）；正式支持以 Java 21 为准 |
| Boot 3.x | ❌ 不支持；请使用 0.2 之前版本或升级 Boot |
| Java < 21 | ❌ 不支持 |

## Versioning

采用 [Semantic Versioning](https://semver.org/)：

- **MAJOR**：不兼容的公共 API/配置/输出格式变更；
- **MINOR**：向后兼容的新功能；
- **PATCH**：向后兼容的修复。

0.x 阶段仍可能发生破坏性变更；**1.0 起公共 API、SPI、配置键和 ID 输出格式视为兼容承诺**。

## API Stability

- 公共 API：`org.unitseek.knitid.core.api`、`spi`、`parser`、各模块公开类；
- `internal`/`autoconfigure` 中标为内部的类不保证兼容；
- 配置键一旦发布，1.x 内不删除；废弃先经过一个 MINOR 周期；
- ID 输出格式（UUID 字符串、ULID 26 字符、KSID 数值、BIZ 模板）一旦发布不改变语义。

## Deprecation Policy

1. 在 Javadoc 使用 `@Deprecated` 并注明替代方案；
2. 在 `CHANGELOG.md` 标注；
3. 至少保留一个 MINOR 版本，下一个 MAJOR 才移除；
4. 配置键废弃时保留解析并记录警告，MAJOR 才移除。

## Spring Boot / Java Upgrade Policy

- 跟随 Spring Boot 4.x 的 OSS 支持窗口；
- Boot 大版本升级在 KnitID MINOR 版本中提供，并在兼容性矩阵中更新；
- Java 最低版本提升仅发生在 MAJOR 版本（安全原因除外，并在 CHANGELOG 说明）。

## Reporting Compatibility Issues

请附上 Java、Boot、KnitID 版本与最小复现，使用 Bug Issue 模板。
