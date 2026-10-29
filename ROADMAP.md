# Roadmap

> 状态：🚧 P2 骨架。路线图不构成发布承诺。

## 1.0

- [x] 五类生成器（UUIDv7/UUIDv8/ULID/KSID/BIZ）
- [x] WorkerId 分配（fixed/env/Redis/ZooKeeper/Kubernetes）
- [x] Redis 序列
- [x] IdParser、Actuator、健康、指标
- [x] JMH 基准、示例、文档与发布流水线
- [ ] 正式 1.0.0 发布与 Maven Central 上线

## 1.1

- [ ] 文档站点（MkDocs/Docusaurus）与版本化文档
- [ ] Spring Boot 4.0/4.1 CI 兼容矩阵
- [ ] 数据库号段 SequenceProvider
- [ ] 更多解析器（BIZ 模板/KSID 位布局内置）
- [ ] 回拨指标

## 1.2

- [ ] Kotlin DSL/扩展
- [ ] GraalVM Native 集成测试
- [ ] 性能调优与多线程基准

## Backlog

- 与 OpenTelemetry 指标命名对齐
- 批量 API 的流式/异步变体
- 管理端点中的配置诊断
