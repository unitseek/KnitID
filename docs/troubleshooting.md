# 排障与错误码

> 状态：🚧 P1 骨架。错误码为稳定公共契约，详见 `IdErrorCodes`。

## 错误码

| 错误码 | 含义 | 常见原因 | 处理 |
| --- | --- | --- | --- |
| `KNITID-1001` | 配置无效 | 未知属性/非法值 | 检查 `knitid.*` 拼写与范围 |
| `KNITID-1002` | 模块缺失 | 未引入生成器/WorkerId 模块 | 添加对应 Maven 依赖 |
| `KNITID-1003` | 配置冲突 | 多个默认生成器/重复名称 | 显式设置 `default-type`/`default-generator` |
| `KNITID-2001` | 生成失败 | 内部异常 | 查看 cause 与日志 |
| `KNITID-2002` | 时钟回拨 | NTP/VM 时间跳变 | 检查时钟，调整回拨策略 |
| `KNITID-2003` | WorkerId 不可用 | Redis/ZK/K8s 分配失败 | 检查连接、权限、槽位 |
| `KNITID-2004` | 序列不可用 | Redis 序列故障 | 检查 Redis 与序列配置 |
| `KNITID-2005` | 序列耗尽 | BIZ 位数/KSID 每毫秒容量不足 | 加宽位数/调整溢出策略 |
| `KNITID-2006` | 模板过长 | BIZ 模板超过 128 字符 | 缩短模板 |
| `KNITID-2007` | Worker 租约丢失 | 续约失败/会话过期 | 检查 Redis/ZK、网络、健康状态 |
| `KNITID-3001` | 解析失败 | 格式不匹配 | 检查输入与 parser |
| `KNITID-3002` | 校验失败 | BIZ 校验位不匹配 | 检查模板/算法/输入 |
| `KNITID-4001` | 生成器不存在 | 名称错误/未注册 | 检查 `knitid.generators` |

## 诊断流程

TODO：补充日志、健康端点、指标、配置 dump（脱敏）的排查顺序。

## 典型问题

TODO：WorkerId 冲突、重复 ID、时钟回拨、序列重复、Actuator 不显示。
