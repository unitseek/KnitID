# 架构

> 状态：🚧 P1 骨架。

## 模块分层

TODO：core/api、spi、generator、integration、autoconfigure、starter 的依赖图。

## 核心抽象

- `IdGenerator` / `NumericIdGenerator`
- `IdGeneratorRegistry`
- `IdGeneratorFactory`
- `IdGeneratorHealth`
- `IdTypeParser` / `IdParser`

## 自动配置流程

TODO：`AutoConfiguration.imports`、注册中心构建、默认生成器选择、装饰器与健康检查顺序。

## 启动行为矩阵

TODO：无默认、单候选、多候选、显式默认、模块缺失等场景。

## 扩展点

见 [SPI 指南](spi.md)。

## 设计文档

- [需求摘要](design/requirements.md)
- [ADR](design/adr/README.md)
