# 架构

## 1. 分层

```text
                +-----------------------------+
                | knitid-spring-boot-starter  |
                +-------------+---------------+
                              |
                +-------------v---------------+
                | knitid-spring-boot-autoconfigure |
                | registry / health / metrics / endpoint / parser |
                +------+--------------+--------+
                       |              |
          +------------v----+   +-----v------------------+
          | generator-*     |   | worker-* / sequence-redis |
          | factories       |   | providers               |
          +--------+--------+   +-------------------------+
                   |
          +--------v--------+
          | knitid-core      |
          | api / spi / parser / exception |
          +-----------------+
```

- **core**：无 Spring；定义契约、异常、健康快照、静态门面；
- **generator-***：实现算法与 `IdGeneratorFactory`；
- **autoconfigure**：把配置、工厂、装饰器组装成注册中心；
- **worker-*/sequence-redis**：通过 SPI 提供 WorkerId/序列；
- **starter**：只聚合 core + autoconfigure，不包含算法；
- **starter-all**：聚合普通 starter 与全部七类生成器，面向“开箱即用”场景。

## 2. 核心抽象

| 类型 | 职责 |
| --- | --- |
| `IdGenerator` | 字符串生成、批量、`blocking()`、`generate(IdContext)` |
| `NumericIdGenerator` | `generateLong()`/数字批量 |
| `IdGeneratorRegistry` | 只读注册中心，按名称/类型查找，默认生成器 |
| `IdGeneratorFactory` | 类型到生成器的工厂，读取扁平化配置 |
| `IdGeneratorDecorator` | 注册前包装（指标、审计） |
| `IdGeneratorHealth` | 健康快照（KSID 提供 lease/回拨详情） |
| `IdTypeParser` / `IdParser` | 类型识别与元数据解析 |
| `WorkerIdAssigner` | WorkerId 获取/续约/释放 |
| `SequenceProvider` | BIZ 序列 |

## 3. 自动配置顺序

1. 各生成器模块注册 `IdGeneratorFactory` Bean（`AutoConfiguration.imports`）；
2. Worker 模块注册 `WorkerIdAssignerProvider`；KSID 工厂收集这些 provider；
3. `KnitIdAutoConfiguration` 创建 `IdGeneratorRegistry`：
   - 建立类型 → 工厂映射，重复类型直接失败；
   - 遍历 `knitid.generators.*`，校验键、创建、应用 `IdGeneratorDecorator`；
   - 选择默认生成器；
4. 创建 `@Primary` `IdGenerator`（`DelegatingIdGenerator`），每次调用委托给注册中心默认生成器；
5. 健康（`knitId`）、指标装饰器、Actuator 端点、IdParser 按条件装配。

## 4. 默认生成器选择

按以下顺序：

1. `knitid.default-generator`：必须指向已配置命名生成器，否则失败；
2. `knitid.default-type`：取第一个同类型命名生成器；没有则用空配置创建名为 `default` 的隐式生成器（KSID/BIZ 可能因缺少必填项启动失败）；
3. 只有一个命名生成器：选它；
4. 没有命名生成器：
   - 没有工厂：无默认，运行期查找默认生成器抛 `KNITID-4001`；
   - 只有一个工厂：用空配置创建 `default`；
   - 多个工厂：启动失败 `KNITID-1003`；
5. 其他情况（多个命名生成器且未指定默认）：启动失败 `KNITID-1003`。

## 5. 启动行为矩阵

| 场景 | 结果 |
| --- | --- |
| `knitid.enabled=false` | 不创建注册中心/默认生成器；健康与端点因缺少注册中心跳过；解析器仍可独立使用 |
| 未引入任何生成器模块 | 无默认生成器；注入 `IdGenerator` 后调用会抛 `KNITID-4001`；健康 UNKNOWN |
| 只引入一个生成器模块 | 自动创建 `default` 生成器 |
| 多个模块 + `default-type` | 创建/选择对应类型默认生成器 |
| 多个模块 + `default-generator` | 必须指向已配置名称 |
| 多个模块 + 未指定默认 | 启动失败 `KNITID-1003` |
| 生成器块缺少 `type` | 启动失败 `KNITID-1001` |
| 生成器块有未知键 | 启动失败 `KNITID-1001` |
| 重复类型工厂 | 启动失败 `KNITID-1003` |

## 6. 配置校验

- `KnitIdProperties` 使用 `ignoreUnknownFields=false`，未知根属性失败；
- 生成器块只允许 `enabled`/`type` 与类型同名块；
- 各工厂自行校验位宽、枚举、必填项、范围；
- 失败抛 `IdConfigurationException`（`KNITID-1001`/`1003`）。

## 7. 健康与指标

- KSID 健康快照含 `workerId`、`leaseValid`、`clockBackwardsCount`、策略、epoch；
- `KnitIdHealthIndicator` 缓存 `5s`，DOWN 条件：快照 DOWN 或窗口内回拨次数 ≥ 阈值；
- 指标装饰器记录 duration/errors/batch；KSID 生成器始终注册 WorkerId gauge，其他生成器仅在健康快照含 `workerId` 时注册；
- Actuator 端点只读，受 `management.endpoint.knitid.enabled` 控制。

## 8. 线程模型

- 生成器实例线程安全；KSID/ULID/UUIDv7 在同毫秒内使用同步递增；
- 租约续约由 KSID 单线程 daemon scheduler 执行；
- 关闭时 `KnitIdAutoConfiguration` 的 disposable closer 仅关闭自动创建的注册中心中的生成器并释放 WorkerId，不关闭用户提供的 registry；
- 任何可能阻塞的生成器通过 `blocking()` 暴露。

## 9. 扩展点

见 [SPI 指南](spi.md)；关键决策见 [ADR](design/adr/README.md)。
