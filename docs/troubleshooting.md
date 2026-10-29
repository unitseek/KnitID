# 排障与错误码

## 错误码

| 错误码 | 含义 | 常见原因 | 处理 |
| --- | --- | --- | --- |
| `KNITID-1001` | 配置无效 | 未知属性、非法枚举、位宽和不为 63、缺少 epoch/prefix | 按消息修正 `knitid.*` |
| `KNITID-1002` | 模块缺失 | 未引入对应 `knitid-generator-*` | 添加依赖 |
| `KNITID-1003` | 配置冲突 | 多个工厂、多个默认候选、`default-generator` 不存在 | 显式指定默认；移除重复模块 |
| `KNITID-2001` | 生成失败 | 时间戳超范围、内部状态异常 | 查看 cause 与系统时间 |
| `KNITID-2002` | 时钟回拨 | NTP 跳变、VM 恢复、手动改时间 | 修复时间源；选择 WAIT/THROW |
| `KNITID-2003` | WorkerId 不可用 | Redis/ZK 连接失败、无空槽、K8s ordinal 非法 | 检查连接、容量、Pod 名 |
| `KNITID-2004` | 序列不可用 | Redis 序列连接/权限失败 | 检查 Redis 与 `sequence.redis` |
| `KNITID-2005` | 序列耗尽 | BIZ `seq` 位数不足、KSID 每毫秒超限 | 加宽位数/调整溢出策略 |
| `KNITID-2006` | 模板过长 | BIZ 输出超过 128 字符 | 缩短模板/前缀 |
| `KNITID-2007` | Worker 租约丢失 | 续约连续失败、ZK 会话过期 | 检查 Redis/ZK；实例恢复后重新分配 |
| `KNITID-3001` | 解析失败 | 输入不是已注册格式 | 检查 parser/格式 |
| `KNITID-3002` | 预留校验码 | 当前版本未发射（保留给 IdValidator 扩展） | 不要依赖该错误码做业务判断 |
| `KNITID-4001` | 生成器不存在 | 名称拼写错误、未配置 | 检查 `knitid.generators` 与名称 |

错误码定义见 `org.unitseek.knitid.core.exception.IdErrorCodes`，属于稳定公共契约。

## 诊断顺序

1. **日志**：定位第一个异常与错误码，不要只看最终包装异常；
2. **配置**：确认生成器名称、类型、嵌套块、默认选择；
3. **Actuator**：`GET /actuator/knitid`、`GET /actuator/health/knitId`；
4. **指标**：`knitid.id.generation.errors{reason=...}`、duration、worker.id；
5. **基础设施**：Redis `PING/SCAN`、ZooKeeper `ls /workers`、Pod 名/ordinal；
6. **时钟**：`timedatectl`/NTP 偏移、容器时间；
7. **依赖**：`mvn dependency:tree` 确认模块与版本。

## 典型问题

### 启动失败：Unknown property

KnitID 默认严格校验。`knitid.generators.order-id.ksid.foo` 中未识别的键会失败；生成器配置必须放在与类型同名的块下：

```yaml
knitid:
  generators:
    order-id:
      type: KSID
      ksid:
        worker-id: 1
```

### 启动失败：Multiple generators available

引入多个算法模块但未指定默认：

```yaml
knitid:
  default-type: UUID7
```

或用 `default-generator` 指向命名生成器。

### No free WorkerId

- 检查 `ksid.worker-id-bits` 容量与实例数；
- Redis：`SCAN <prefix>:<datacenter>:*` 查看残留 lease；
- ZooKeeper：`ls /<namespace>/workers/<datacenter>`；
- 不要直接删除仍存活实例的 lease。

### WorkerId 租约丢失

`KNITID-2007` 表示健康已 DOWN，生成会继续失败，直到实例重启并重新分配。检查：

- Redis/ZK 网络与延迟；
- `renew-interval` 是否过接近 `lease-ttl`；
- 应用是否长时间 GC/暂停；
- ZooKeeper session timeout。

### 时钟回拨

`KNITID-2002`：

- 立即修复 NTP/主机时间；
- 评估 `WAIT`（可容忍短暂等待）或 `THROW`（快速失败）；
- 只有明确接受逻辑时间领先时才使用 `BORROW_FUTURE`。

### BIZ 校验失败

- 校验算法必须与生成时一致；
- `{check}` 必须是模板最后一个元素；
- 含非字母数字分隔符时算法会忽略分隔符，但内容类型仍需匹配（MOD11_2/LUHN 只支持数字）。

### Actuator 端点 404

- 引入 `spring-boot-starter-actuator`；
- `management.endpoints.web.exposure.include=knitid`；
- 检查 `management.endpoint.knitid.enabled` 与安全配置。

### Testcontainers 跳过

无 Docker 或 Docker 不可用时，`@Testcontainers(disabledWithoutDocker = true)` 会跳过集成测试；CI 需要 Docker。可用 `KNITID_TEST_REDIS_IMAGE`/`KNITID_TEST_ZOOKEEPER_IMAGE` 指定镜像。
