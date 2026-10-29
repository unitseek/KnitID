# SPI 扩展指南

> 状态：🚧 P1 骨架。所有扩展点位于 `org.unitseek.knitid.core.spi` 与 `parser`。

## 扩展点

| SPI | 用途 | 注册方式 |
| --- | --- | --- |
| `IdGeneratorFactory` | 新增生成器类型 | Spring Bean |
| `WorkerIdAssigner` / `WorkerIdAssignerProvider` | 自定义 WorkerId 分配 | Spring Bean |
| `SequenceProvider` | 自定义 BIZ 序列 | Bean 名引用 |
| `IdTypeParser` | 自定义 ID 解析 | Spring Bean，优先于内置 |
| `IdGeneratorDecorator` | 包装生成器（指标/审计） | Spring Bean |
| `IdGeneratorCustomizer` | 修改生成器配置 | Spring Bean |
| `RandomProvider` | 自定义随机源 | 构造注入/自定义工厂 |

## 示例：自定义 SequenceProvider

TODO：给出数据库号段/Redis/etcd 实现与 `sequence-provider` 配置。

## 示例：自定义 WorkerIdAssigner

TODO：给出基于 CMDB/配置中心的实现，并说明租约与释放语义。

## 示例：自定义 IdTypeParser

TODO：给出 BIZ 模板解析与 KSID 位布局解析示例。

## 兼容性要求

- 实现类必须线程安全；
- `IdGeneratorFactory` 返回的生成器需声明正确 `IdType`；
- 失败必须抛出 `IdGenerationException` 子类并带稳定错误码；
- 不依赖自动配置内部实现。
