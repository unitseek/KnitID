# Security Policy

KnitID 是 ID 生成基础库，安全问题的核心是**唯一性、可预测性、WorkerId/租约正确性与供应链安全**。

## Supported Versions

| Version | Supported | Notes |
| --- | --- | --- |
| `1.0.x` | ✅ | 当前稳定版本；接收安全修复 |
| `< 1.0` | ❌ | 1.0 之前的版本，不再支持 |

## Reporting a Vulnerability

**请勿通过公开 Issue 报告安全漏洞。**

请使用 GitHub Security Advisory 私密报告：

- https://github.com/unitseek/KnitID/security/advisories/new

报告请包含：

- 受影响版本、Java/Spring Boot 版本；
- 最小复现步骤或 PoC；
- 影响说明（重复 ID、WorkerId 冲突、租约丢失、拒绝服务、信息泄露等）；
- 是否已公开、期望的修复时间。

我们会在 **3 个工作日内**确认收到，并根据严重程度在 **30 天内**给出修复或缓解方案。修复发布后，会在 Advisory 中致谢报告者（除非要求匿名）。

## Scope

属于安全范围：

- ID 唯一性/单调性被破坏（包括 WorkerId 冲突、序列重复、时钟回拨处理错误）；
- Redis/ZooKeeper WorkerId 租约失效导致重复；
- 解析器崩溃、拒绝服务或异常信息泄露；
- 构建/发布供应链问题（依赖、脚本、CI 凭证）；
- 文档导致的危险配置（例如默认关闭关键校验）。

不属于安全范围：

- 使用者主动暴露 WorkerId、序列或内部配置；
- 第三方依赖自身漏洞（请同时报告上游，我们会跟进升级）；
- 部署环境、网络、Redis/ZooKeeper 权限配置问题；
- 仅影响测试代码、基准或示例的问题。

## Optional Modules

- KSUID `PERFORMANCE` random source 仅用于性能和基准测试，生产环境必须使用 `SECURE`；
- KSUID 没有格式标记，解析器接受任意合法 27 位 Base62 串，解析成功不代表来源可信；
- TypeID 前缀只是类型元数据，不是认证信息，不能作为权限或来源证明；
- UUIDv7、ULID、TypeID 的时间前缀会泄露大致生成时间，不适合需要隐藏生成时间的场景。

## Security Best Practices

- KSID 多实例必须使用 Redis/ZooKeeper/Kubernetes 分配 WorkerId，或确保 `fixed` 的 WorkerId 全局唯一；
- 部署环境必须启用 NTP/时钟同步，并监控回拨；
- Redis/ZooKeeper 使用认证、TLS、网络隔离，禁止公网暴露；
- Actuator 端点按需暴露，生产环境限制 `/actuator/knitid`、`/actuator/knitid-config` 访问；
- 不要把密钥、连接串、WorkerId 写入日志或公开配置；
- BIZ 模板中的 `{random:n}` 不是安全随机源，不能用于令牌/密码；
- 需要不可预测 ID 时使用 UUIDv7/SecureRandom 或 UUIDv8 `RANDOM`，不要使用可枚举序列。
