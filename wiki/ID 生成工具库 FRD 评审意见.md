# ID 生成工具库 FRD 评审意见

**评审对象**：`wiki/ID 生成工具库功能需求文档（FRD）.md`（v0.1，443 行；v0.2 已按本意见修订，v0.1 原文归档于 `wiki/archive/`）
**评审日期**：2026-10-29
**评审方式**：文档静态评审（工作区当前仅有该文档，无代码/原型/示例），对照 RFC 9562、ULID 规范、Spring Boot 4.x 官方文档、Actuator/Micrometer 约定
**评审结论**：**需要修订**。架构方向与模块划分正确，但当前版本尚不足以直接进入实现：存在 10 项阻断级缺口（内部矛盾、关键契约缺失、验收不可测试），另有约 10 类系统性补充项。

> **更新（2026-10-29）**：第 7 节的 12 项待决问题已由项目负责人拍板；FRD 已修订为 v0.3（v0.2 完成 10 个阻断项整改，v0.3 将雪花 ID 更名为 KSID；对照见 FRD v0.3 第 0.2 节，决策记录见第 15 章）。

> 优先级定义：**Blocker** = 不澄清会导致实现返工或验收无法编写；**High** = 影响可用性、演进或跨模块一致性；**Medium** = 完善度与文档质量；**Low** = 文字/格式。

## 0. 结论摘要

### 0.1 做得好的地方

1. **架构选型正确**：一个 Starter + 统一 API + 算法模块按需引入 + 自动配置，符合 Spring Boot 生态惯例，能同时解决"依赖膨胀"和"用户选型成本高"。
2. **模块边界清晰**：core / autoconfigure / starter / 各算法模块 / BOM / samples 职责划分合理，依赖方向明确（算法模块依赖 core）。
3. **非功能维度起点较高**：已覆盖指标、健康检查、异常、SPI、性能、安全、基准测试，比多数 v0.1 需求完整。
4. **风险意识**：已识别 UUID8 语义自定义、WorkerId 冲突、第三方库版本、业务 ID 依赖外部存储等真实风险。

### 0.2 主要不足

| 维度 | 问题 | 影响 |
| --- | --- | --- |
| 一致性 | 同一场景给出两种相反行为（L164 vs L216）；P0 范围与里程碑冲突（L303 vs L426） | 实现方无法判断以哪条为准 |
| 契约完整性 | 默认生成器如何装配、注册中心何时聚合、WorkerId 生命周期、序列重置语义均未定义 | 设计阶段必须返工补齐 |
| 可测试性 | 大量"支持/提供/可配置"式验收，缺 Given/When/Then、测试向量、基准方法 | 无法判定是否达标 |
| 文档工程 | 缺元信息、术语、用户故事、竞品对比、兼容矩阵、可追踪矩阵、ADR、发布策略 | 需求不可评审、不可追踪、不可交接 |
| 决策悬空 | 自研 vs 依赖第三方库、groupId/包名、配置前缀、Spring Boot 支持范围、随机源策略 | 影响架构、坐标、性能与后续演进 |

### 0.3 问题统计

| 级别 | 数量 | 说明 |
| --- | --- | --- |
| Blocker | 10 | 必须先拍板/补文档，否则实现返工 |
| High | 10 | 影响可用性与演进，建议 v0.2 补齐 |
| Medium | 14 | 文档结构、工程化与可追踪性完善 |
| Low | 12 | 措辞、格式与局部不一致 |

**建议**：本意见可直接作为 v0.2 的修订输入；优先解决第 1 节 10 个 Blocker 与第 7 节 12 个待决问题，再补第 5 节建议章节。

---

## 1. 阻断级问题（Blocker，10 项）

### B-01 启动行为自相矛盾（L164 vs L216）

- **位置**：L164「只引 Starter 不引算法模块时，核心 API 可用，默认生成器缺失时给出明确启动提示或健康检查告警」；L216「default-type 对应模块未引入时启动失败并提示引入哪个模块」。
- **问题**：同一类场景（所需算法模块缺失）分别要求"告警/提示"和"启动失败"，未给出区分条件；"明确启动提示"的载体（日志、异常、FailureAnalyzer）也未定义。
- **建议**：补一张**启动行为矩阵**（见 6.2），逐场景定义：是否启动成功、默认 Bean 是否存在、注册中心内容、健康检查状态、注入失败时的异常与提示。推荐语义：
  - 未配置 default-type 且无算法模块：**允许启动**；registry 为空；health 为 UNKNOWN；注入 IdGenerator 时通过 FailureAnalyzer 提示"请引入 knitid-generator-* 模块"。
  - 配置了 default-type 但模块缺失：**fail-fast**，异常信息含缺失的 artifactId 与配置 key。
  - 有且仅有一个算法模块且未配置 default-type：自动作为默认（降低使用成本，需确认）。
  - 多个算法模块且未配置 default-type：**fail-fast**，提示显式配置 default-type（避免 NoUniqueBeanDefinitionException 这类无行动指引的异常）。

### B-02 默认生成器的选择与装配顺序未定义（L166-216）

- **位置**：FR-AUTO-02（L166-177）只提到 @ConditionalOnClass、@ConditionalOnProperty、@ConditionalOnMissingBean；FR-AUTO-04（L210-216）要求按 default-type 注册 @Primary。
- **问题**：
  1. 若每个算法模块各自声明"默认 Bean"并用 @ConditionalOnMissingBean，多个模块共存时谁生效取决于自动配置处理顺序，文档未定义。
  2. default-type 是配置值而非 Bean 名，需要有一个统一配置类在**所有生成器 Bean 可见之后**选出默认并标 @Primary；该类的 @AutoConfigureAfter / ObjectProvider 策略未定义。
  3. knitid-spring-boot-autoconfigure 无法编译依赖算法模块（可选依赖），顺序声明只能用 @AutoConfigureAfter(name = "...")、@AutoConfigureOrder 或运行时 ObjectProvider / Map<String, IdGenerator> 注入；文档未给出做法。
  4. 用户自定义 IdGenerator Bean、配置的命名生成器、@Primary 默认生成器三者的优先级未定义。
- **建议**：明确"算法模块只声明普通 Bean，由注册中心配置类统一选默认"；给出装配顺序图、优先级规则（用户 Bean > 配置生成器 > 模块内建默认）、命名规则（Bean 名 = 配置中的 generators.<name>）。并补充失败提示使用 Spring Boot 的 FailureAnalyzer 机制（启动失败时输出含 artifactId 与配置 key 的可操作信息）。

### B-03 配置前缀、绑定与校验规则不完整（L178-208）

- **位置**：FR-AUTO-03，前缀 app.id。
- **问题**：
  1. app.id 过于通用，极易与业务应用自有配置冲突；建议改为 knitid（或 knitid.id），越早定成本越低。
  2. 未定义 default-type 的缺省值（null？UUID7？）、未知属性是否失败（建议 fail-fast）、未知 type 字符串的处理、大小写/别名（uuid7/UUID7/uuid-7）规则。
  3. 未定义每个生成器是否有独立 enabled、generators 数量上限、名称合法性（是否允许点、下划线、中文）。
  4. 多命名生成器的属性绑定模型（Map<String, GeneratorProperties> + 按类型的嵌套块）必须写清楚，Spring 的 Map key 与嵌套属性易踩坑，建议配 @Validated + 自定义校验 + 绑定测试。
  5. 未定义配置校验时机与错误信息格式（启动期 fail-fast，异常中含配置 key 与合法值）。
  6. epoch: 2024-01-01T00:00:00Z 未说明类型（Instant/LocalDate）与默认值；修改 epoch 属破坏性变更，需写版本约束。
- **建议**：prefix 改为 knitid；补全量属性表（含类型、默认值、取值范围、是否必填、示例、变更影响）；明确 ignoreUnknownFields=false 或等效的严格校验；给出错误配置示例与报错文案。

### B-04 雪花 ID 的位分配、回拨与 WorkerId 生命周期契约缺失（L281-300）

- **位置**：FR-SNOW-01/02。
- **问题**：
  1. 「位分配可配置」但没有**总位数校验**：1 符号位 + timestamp + datacenter + worker + sequence 必须等于 64（可用 63），且 sequence ≥ 1、timestamp 位数决定服务年限（需写明）；非法组合必须启动失败。
  2. 同毫秒**序列号溢出**策略未定义（自旋到下一毫秒/抛异常/借时间），直接决定高并发行为。
  3. BORROW_FUTURE 语义与风险未定义：会透支未来时间、可能放大时钟问题、跨节点不能解决冲突；应默认禁用并标注"仅单机、可接受时间跳跃的场景"。
  4. 回拨判定阈值、WAIT 的最大等待/超时/中断行为、回拨次数与持续时间的可观测性未定义。
  5. **WorkerId 生命周期缺失**：何时获取（启动同步）、租约 TTL、续约、释放（优雅停机/JVM 崩溃）、冲突检测（lease + fencing）、获取失败时 fail-fast 还是降级。
  6. 重启复用 WorkerId 的风险未提：若重启后时钟回退或落入同一毫秒，需要 lastTimestamp 持久化或启动等待，否则可能重复。
  7. 多机时钟源要求（NTP/PTP、最大偏差假设、容器时钟）未提。
  8. 验收「单机基准测试达到文档承诺 QPS」是循环定义——文档并未承诺具体数字。
- **建议**：
  - 补"位分配校验规则 + 服务年限公式"：时间跨度 = 2^timestampBits ms；QPS 上限 = 1000 × 2^sequenceBits / 节点。
  - 补"时钟回拨策略语义表"：策略/触发条件/行为/阻塞上限/指标/是否默认。
  - WorkerId 定义 WorkerId 值对象 + WorkerIdAssigner 契约（assign/renew/release），明确 Redis/ZK 示例的 key、TTL、失败处理。
  - 明确时钟源与运维前置条件。

### B-05 业务 ID 的唯一性边界、序列重置与校验位算法缺失（L302-321）

- **位置**：FR-BIZ-01。
- **问题**：
  1. 「同一业务前缀下序列不重复」未说明范围：单 JVM、单实例、集群？内存序列天然只能单机唯一；Redis/DB 序列的强依赖与故障行为也未定义。
  2. {yyyyMMdd} 使用哪个时区？日切边界如何与序列 key 的过期时间对齐？跨时区多实例如何一致？
  3. {seq:6} 溢出行为（超过 999999 后回绕会重复、抛错还是加长）、序列是"按天重置"还是"永不自增重置"未定义。
  4. {random:8} 的字符集、生成方式、碰撞概率、是否查重/重试均未定义（碰撞概率需给出数量级）。
  5. {check} 的校验位算法未指定（Luhn？ISO 7064 MOD 11,2？MOD 37,36？）；字母数字场景与纯数字场景算法不同；长度与是否可配置未定义。
  6. 模板语法不完整：占位符参数、转义（字面量花括号）、未知占位符、模板长度上限、前缀字符集、大小写、是否允许运行期传入模板（当前看是静态配置，需写明）。
  7. 序列号 SPI 的方法语义（next() / next(n)、原子性、超时、失败重试/降级）未定义。
- **建议**：补"唯一性边界声明"（单机/集群、按 prefix/按业务类型/按天）；补"日切时区"配置项（建议显式必填，避免开发/生产漂移）；补"序列重置与溢出"规则表；校验位提供默认算法 + SPI；模板语法给 BNF 或完整规则表；Redis 故障时明确 fail-fast（不降级，避免重复）。

### B-06 自研 vs 依赖第三方库未决策（L52-56 vs L436）

- **位置**：模块表写「UUID7 实现」「ULID 实现」；风险表又写「第三方 ULID/UUID 库版本冲突」。
- **问题**：每个算法模块是「从零实现」还是「包装现有库」（如 uuid-creator、ulid-creator、cosid）没有决策。这直接影响：BOM 依赖、许可证、测试向量来源、性能、维护成本、安全修复响应。
- **建议**：给出**逐模块决策表**（自研/包装/暂缓），并说明理由与替代方案；若包装，明确传递依赖、版本锁定与兼容策略；若自研，明确参考规范与测试向量（RFC 9562 附录 A、ULID spec）。该决策应记入 ADR。

### B-07 性能目标与安全默认值、API 形态可能冲突（L348-354 vs L370-372）

- **位置**：NFR-02 要求雪花单线程 ≥ 100 万/秒、UUID7/ULID ≥ 50 万/秒；NFR-05 要求默认 SecureRandom；FR-API-01 返回 String。
- **问题**：
  1. SecureRandom 的线程争用与系统熵行为可能使 UUID7/ULID 的 50 万/秒目标难以稳定达成；需要明确"哪种随机源用于哪个生成器"（RFC 9562 建议密码学随机；ULID 规范无此要求）。
  2. 雪花 100 万/秒依赖序列位配置（例如 12 位理论上限 409.6 万/秒），但 String 分配与格式化开销会显著拉低；目标需与实现约束（无锁、预分配、批量路径）绑定。
  3. 「批量生成性能不低于逐条生成 80%」逻辑倒置：批量理应不低于逐条；该指标无法验收。
  4. 「以基准测试报告为准」是循环论证，报告本身没有方法学要求（JMH、硬件、预热、GC、置信区间）。
- **建议**：按生成器定义"随机源与性能目标"矩阵；把批量指标改为"批量吞吐 ≥ 单条吞吐，且每 ID 平均分配开销不高于单条"；补 JMH 基准方法学与固定硬件说明；给出各配置下的理论吞吐上限公式，避免目标与位分配矛盾。

### B-08 兼容性表述不可执行（L346-349）

- **位置**：NFR-01「基线：Java 21 + Spring Boot 4.1.x；P1：兼容 Spring Boot 4.x」。
- **事实**：截至 2026-10，Spring Boot 4.1.x 为当前稳定线（最新 4.1.2），Boot 4 最低要求 Java 17（见参考依据）。因此"基线 4.1.x"本身可行。
- **问题**：
  1. "P1：兼容 Spring Boot 4.x"含义不明：4.0.x 是否支持？未来 4.2 是否承诺？"P1"作为兼容性优先级也不合理（兼容性应是验收项）。
  2. 未说明 Java 21 是硬性要求还是推荐：若库使用 Java 21 语法，则使用 Java 17 的 Boot 4 用户无法使用；若只是编译目标，应下调到 17 扩大用户面。
  3. 未提 Spring Boot 3.x 是否在范围内（若支持，需要两套自动配置注册机制；建议明确不支持）。
- **建议**：将 NFR-01 改为**支持矩阵**：Spring Boot 4.0.x / 4.1.x × Java 21（若强制）或 Java 17+；CI 矩阵逐格测试；明确未来 minor 的兼容承诺（如"4.x 内保证兼容，4.2 发布后 30 天内支持"）。

### B-09 P0 范围与里程碑冲突（L303 vs L425-426 vs L443）

- **位置**：FR-BIZ-01 标 **P0**；里程碑 M1 只含 core/autoconfigure/starter/UUID7/Snowflake，业务 ID 在 M2（L426）；结论（L443）又称"P0 覆盖 …业务 ID"。
- **问题**：P0 意味着 1.0 前必须完成且优先实现，但排期放在 M2，与 M1 的 P0 定义自相矛盾。
- **建议**：二选一并全文档同步：将业务 ID 降为 P1（M2），或把业务 ID（至少内存序列 + 模板）纳入 M1。同步修正结论段。另 M1 的 Snowflake 依赖 WorkerId SPI（P0），需明确 M1 支持的最小 WorkerId 策略（固定值/环境变量），Redis 等放 M2。

### B-10 注册中心与 SPI 缺少生命周期和运行期契约（L111-127、L325-340）

- **位置**：FR-API-04 只定义查询方法；第 7 章 SPI 表只有"用途"。
- **问题**：
  1. 注册中心是只读视图还是可变注册表？是否允许运行期注册/注销？配置刷新后行为？
  2. 生成器/SPI 的资源释放（WorkerId 释放、Redis 连接、后台续约线程）没有生命周期约定（SmartLifecycle / DisposableBean / 自定义 close）。
  3. IdGeneratorCustomizer 的语义（作用于哪些 Bean、先后顺序、幂等性、与 @Primary 的关系）未定义。
  4. 每个 SPI 的方法签名、线程安全、是否阻塞、失败语义、幂等要求都缺失（详见 H-07）。
- **建议**：明确注册中心为"启动期装配的只读视图 + 可选的动态注册扩展点"；所有需要释放资源的 SPI 提供 AutoCloseable/生命周期回调；为 customizer 定义 Ordered 语义与执行阶段；SPI 契约表补齐。

---

## 2. 高优先级问题（High，10 项）

### H-01 统一 API 细节（L75-103）

- IdType 是封闭枚举（L108），第三方/业务自定义生成器无法扩展类型，只能映射到 BIZ，与"可扩展"目标冲突。建议改为值对象（IdType.of(String) + 预置常量）或增加 CUSTOM 并规定扩展方式。
- generateBatch(int) 返回 List<String>，数值生成器没有批量接口；size 无上限（OOM 风险）；批量是否保持单调/有序未定义。建议增加 NumericIdGenerator.generateLongBatch(int)、generateBatch(int, IdContext)，并设置默认上限（可配置）。
- generate(IdContext) 对不支持的上下文属性是"忽略/报错/降级"未定义；context == null 的行为未定义。
- 未声明**阻塞语义**：雪花 WAIT、业务 ID 依赖 Redis 时调用可能阻塞，接口 Javadoc 必须写明。
- 缺少关闭/刷新能力的定义（与 B-10 呼应）。
- 建议补一个非 Spring 使用的静态门面（IdGenerators）或在范围中明确"core 可独立使用"。

### H-02 IdContext 语义（L129-136）

- attributes 的可变性、线程安全、防御性拷贝未定义。
- prefix 与业务 ID 模板中的 {prefix}、生成器级 prefix 的优先级未定义。
- businessType、租户、节点、业务日期等"预留字段"没有类型、默认值、是否参与生成、无值时的行为。
- 建议改为不可变 Builder；提供 IdContextKeys 常量；给"属性优先级表"；明确未知属性的处理策略。

### H-03 IdParser 检测规则与返回结构（L137-143）

- 类型判定顺序未定义：UUID 需看 version/variant 半字节；ULID 是 26 位 Crockford Base32；雪花是纯数字；业务 ID 依赖模板。存在歧义（例如 26 位纯数字、带前缀的 UUID），需明确"先按格式 → 再按 version → 再按长度/字符集 → 最后 biz 模板"的判定顺序与"无法判定"的返回。
- 返回结构未定义（建议 IdInfo：type、raw、timestamp、version、node、sequence、businessType）。
- 建议 API 为 Optional<IdInfo> tryParse(String) + 严格模式 parse（失败抛异常）；补性能说明与 fuzz 测试。

### H-04 自动配置工程规范（L60-65、L166-177）

- 明确使用 @AutoConfiguration（而非 @Configuration）；注册文件路径必须精确为 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports（Spring Boot 4 仍使用该机制，已核对官方文档）。
- @ConditionalOnClass 应指向算法库/实现类，而非本模块配置类；@ConditionalOnProperty 的 havingValue 与 matchIfMissing 组合需写明。
- 补 @AutoConfigureAfter / @AutoConfigureOrder 与 ObjectProvider 聚合策略（见 B-02）。
- starter 不得传递算法模块依赖；建议用测试断言依赖树（如 Maven Enforcer 或 dependency:tree 断言）。
- 补 spring-boot-configuration-processor 与元数据质量要求（description/default/deprecation），并可加元数据完整性测试。
- 如承诺 GraalVM native，需补 AOT hints 与 native 测试；否则在非目标中明确排除。

### H-05 Actuator（L226-234）

- 端点 id idGenerators（L229）与 Spring Boot 的端点命名/暴露约定不一致：Boot 的 EndpointId 要求以字母开头、仅字母数字（比较时忽略大小写与符号），实际 URL 与 management.endpoints.web.exposure.include 的取值需要明确写出；当前文档未给 URL 和暴露配置，用户无法正确启用。
- 未提 @ConditionalOnAvailableEndpoint（避免未启用 Actuator 时的启动副作用）。
- 健康检查语义粗糙：WorkerId 失效、时钟回拨是否直接 DOWN？瞬时回拨是否会导致实例被摘流量？UNKNOWN 在什么条件下返回？建议区分瞬时/持续状态，提供抖动与恢复策略，并限制健康检查开销（不得每次做昂贵操作）。
- 端点响应结构（字段、排序、是否包含 WorkerId 等敏感信息）未定义，建议给 DTO 与契约测试。

### H-06 指标（L362-368）

- 指标命名不符合 Micrometer 约定：应为小写点分命名、明确类型与单位；duration 应使用 Timer（自带 count），而不是再单独造 count；错误建议用 Timer 的 outcome/异常标签或独立 Counter，而不是三套指标。
- 建议前缀 knitid.（避免 id.* 与业务/框架冲突）：knitid.id.generation.duration（Timer，tags type、name）、knitid.id.generation.errors（Counter，tags type、name、reason）、knitid.id.clock.backwards（Counter，tags name、strategy）、knitid.id.worker.id（Gauge，tags name）、knitid.id.batch.size（DistributionSummary）。
- 标签基数需文档化：name 来自配置，数量有界但需说明；禁止把生成的 ID 值作为标签。
- 明确指标在热路径的开销（预注册、避免每次查注册表）。

### H-07 SPI 契约（L325-340）

每个 SPI 需补：方法签名、线程安全、是否阻塞、失败语义、幂等、生命周期、默认实现、示例。

- WorkerIdAssigner：assign() 同步/异步？release()/renew() 是否需要？租约失败时生成器如何处理？
- SequenceProvider：next(scope) / next(scope, n)、批量原子性、超时、Redis 不可用时 fail-fast。
- Clock：与 java.time.Clock 的关系？建议直接复用 JDK Clock 或明确包装，并提供 MutableClock 供测试（这也是测试所需，级别应为 P0）。
- IdFormatter / IdValidator：调用顺序（生成 → 校验 → 格式化？）、异常映射、性能影响。
- RandomProvider：nextBytes / nextInt、线程安全、是否要求密码学安全、默认值。
- IdGeneratorCustomizer：执行阶段、排序、幂等性（见 B-10）。

### H-08 异常体系（L379-389）

- 缺公共基类（建议 IdException extends RuntimeException）与错误码；参数错误（size <= 0、非法 context）用 IllegalArgumentException 还是 IdConfigurationException 需划界。
- 异常信息格式未定义（建议含 generator name/type、配置 key、可操作建议）；是否包含原始异常 cause；是否携带数据（ID 片段需脱敏）。
- 异常到指标/健康检查的映射未定义。
- 建议补错误码表（code、场景、处理建议）与 i18n 策略（可后置）。

### H-09 测试策略（L393-409）

现有测试类型方向正确，但缺关键项：

1. **性质测试**（jqwik）：UUID 版本/变体位、ULID 长度/字符集/字典序、雪花位段范围/正数、业务 ID 模板与校验位。
2. **规范测试向量**：RFC 9562 附录 A 的 UUIDv7 示例、ULID 规范向量；并与参考实现交叉验证（若包装第三方库则直接复用其向量）。
3. **多进程/多 JVM 唯一性**测试（不能只测多线程）。
4. **时钟故障注入**：回拨/前跳/重启同毫秒，依赖可注入 Clock（对应 H-07）。
5. **自动配置矩阵测试**：ApplicationContextRunner 覆盖"模块有无 × default-type 有无 × 用户 Bean 覆盖 × 非法配置"，断言启动成功/失败与提示信息。
6. **架构约束**：ArchUnit 断言 core 不依赖 Spring、autoconfigure 不编译依赖算法模块。
7. **依赖树断言**：starter 不传递算法模块。
8. **契约测试**：Actuator 端点 JSON、健康检查状态映射。
9. **性能基准**：JMH 工程化（固定硬件/预热/报告入库），CI 只做回归门槛或独立 runner，避免噪声误报。
10. **fuzz**：模板解析器、IdParser 随机输入不崩溃。
11. **Redis/外部依赖**：Testcontainers；故障（超时/断连）行为测试。
12. **兼容矩阵 CI**：Boot 4.0.x / 4.1.x。

### H-10 发布与工程化（L344-376、L413-427）

- com.example 占位 groupId（L154/L159 等）必须尽早确定（影响包名、坐标、发布）；建议写入 v0.2 的决策清单。
- 缺：许可证、SCM、开发者信息、Maven Central 发布要求、签名、sources/javadoc、SBOM、可复现构建。
- 缺版本策略：SemVer、0.x → 1.0、预发布（-M1/-RC1）、破坏性变更定义（配置属性、枚举值、SPI 方法）、弃用策略与支持周期。
- 缺文档交付定义：Javadoc 站点、配置参考、迁移指南（迁移来源需明确：从 uuid-creator/cosid/mybatis-plus 迁移的映射表，否则"迁移指南"无对象）。
- 缺 CI/质量门禁：编译矩阵、测试、静态检查、依赖漏洞扫描、覆盖率门槛（建议）、发布流水线。

---

## 3. 中优先级问题（Medium，14 项）

- **M-01 文档元信息**：缺作者、评审人、状态（草案/评审中/已批准）、最后更新日期、变更记录、关联 ADR/issue。当前只有"文档版本 v0.1"。
- **M-02 术语表**：UUIDv7/v8、ULID Monotonic、Crockford Base32、WorkerId/DatacenterId、epoch、时钟回拨、BORROW_FUTURE、变体位、序列位等。
- **M-03 用户故事与选型指南**：谁在什么场景用哪种 ID；为什么选 UUID7 而非 ULID/雪花（数据库排序、存储大小、可读性、跨系统性）；建议给"选型决策树"。
- **M-04 竞品/替代方案**：与 uuid-creator、ulid-creator、cosid、MyBatis-Plus IdentifierGenerator、各类 Snowflake 实现对比，说明 KnitID 的差异化（统一 Starter + 多类型 + 可观测 + SPI），否则"为什么需要这个库"不成立。
- **M-05 非目标**：现有第 2.2 节过于简略；建议明确不做：分布式强一致、控制台、非 Spring、全类型解析、ID 反查、跨语言 SDK 等。
- **M-06 可追踪矩阵**：FR → 设计 → 测试用例 → 验收结论的映射，保证 P0 全覆盖。
- **M-07 部署与运维约束**：NTP 要求、容器/StatefulSet 的 WorkerId 规划、Redis 高可用与故障降级、多环境配置样例（dev/test/prod）、K8s 探针与健康检查的关系。
- **M-08 安全与隐私威胁模型**：WorkerId 伪造/重复、UUID7/ULID 时间可推断导致的信息泄露、业务 ID 序列可枚举、随机段可预测性、日志脱敏配置项；NFR-05 只写了"默认 SecureRandom + 不记录完整 ID"。
- **M-09 配置参考与错误示例**：全量属性表 + 最小/完整/错误配置示例；元数据 description 的质量要求。
- **M-10 示例工程要求**：示例应可一键启动、覆盖各类型、同时作为集成测试；明确示例是否随发布（knitid-samples 在模块表标"否"，但交付物又列出）。
- **M-11 需求模板统一**：FR 标题内嵌优先级与另起一行两种写法并存；建议统一为"ID / 优先级 / 描述 / 配置项 / 边界条件 / 验收（Given-When-Then）/ 测试映射"。
- **M-12 模块表与交付物一致性**：knitid-samples 是否必须、是否发布需明确；BOM 的 import 用法示例缺失。
- **M-13 里程碑细化**：M1-M3 无日期、负责人、退出准则、依赖关系；建议每阶段给出"可演示成果 + 验收命令"。
- **M-14 风险表补充**：补 Owner、触发信号、应对动作；新增风险：SecureRandom 性能、配置前缀冲突、自动配置顺序、Boot 4 用户迁移、WorkerId 租约失败、ID 可预测性、外部序列依赖不可用、文档与实现漂移。

---

## 4. 低优先级/文字问题（Low，12 项）

- **L-01**（L229）/actuator/idGenerators：端点 id 与 URL/暴露配置需按 Boot 约定统一（见 H-05），建议文档中同时给出 URL 与 management.endpoints.web.exposure.include 示例。
- **L-02**（L236）健康检查的 UNKNOWN：需说明与 UP/DOWN 的判定条件（"默认生成器是否存在""WorkerId 是否有效""时钟是否回拨"分别映射到什么状态）。
- **L-03**（L193）epoch: 2024-01-01T00:00:00Z：明确类型与格式（ISO-8601 Instant），并说明修改 epoch 的兼容性影响。
- **L-04**（L200）示例模板 {prefix}-{yyyyMMdd}-{seq:6}-{check}：需与 6.5 节占位符定义、校验位算法对齐，并给出一个真实产出样例。
- **L-05**（L303）"序列号来源 SPI"与第 7 章 SequenceProvider 命名不统一，建议全文统一为 SequenceProvider。
- **L-06**（L327）IdGeneratorCustomizer 用途"生成器后置定制"过于模糊，建议给示例（如统一包装监控/限流）。
- **L-07**（L364-368）指标伪代码语法不规范（type,name），建议改为 Micrometer 的 name + tags 表。
- **L-08**（L350）NFR 中使用 "P1" 与 FR 优先级体系混用，建议 NFR 单独用"必须/应该/可选"。
- **L-09**（L104）"字符串与数值转换不丢失精度"需明确边界：long → String 无损；String → long 需范围校验与非数字处理。
- **L-10**（L164）"明确启动提示"需定义载体与文案（FailureAnalyzer + 日志级别）。
- **L-11**（L443）结论段重复正文且与 P0/里程碑冲突（见 B-09），建议改为"评审结论 + 开放问题索引"。
- **L-12**（L425-427）M1-M3 缺退出准则；且 "1.0 发布" 未与版本策略（H-10）关联。

---

## 5. 建议新增的文档章节

| # | 章节 | 目的 |
| --- | --- | --- |
| 1 | 文档信息与变更记录 | 版本、作者、评审状态、变更历史 |
| 2 | 术语表 | 统一 UUID/ULID/雪花/业务 ID 术语 |
| 3 | 用户故事与选型指南 | 说明"为谁解决什么问题"、如何选型 |
| 4 | 目标与非目标 | 明确边界，防止范围蔓延 |
| 5 | 竞品与替代方案 | 说明差异化价值 |
| 6 | 生成器能力矩阵 | 唯一性/单调性/长度/字符集/可解析/依赖（见 6.1） |
| 7 | 唯一性与碰撞概率 | 明确保证边界与概率数量级 |
| 8 | 生命周期与装配顺序 | 启动行为矩阵 + 自动配置顺序（见 6.2） |
| 9 | 全量配置参考 | 属性/类型/默认值/取值范围/示例 |
| 10 | 故障模式与降级 | Redis 故障、WorkerId 不可用、时钟异常 |
| 11 | 安全与隐私 | 威胁模型、随机源策略、日志脱敏 |
| 12 | 兼容性矩阵与 CI | Boot/Java/依赖矩阵 |
| 13 | 发布、版本与弃用策略 | SemVer、0.x→1.0、支持周期 |
| 14 | 需求可追踪矩阵 | FR → 设计 → 测试 → 验收 |
| 15 | 待决问题（Open Questions）与 ADR 索引 | 决策记录与跟踪 |
| 16 | 里程碑（含退出准则） | 可执行计划 |

---

## 6. 可直接采用的补充材料

### 6.1 生成器能力矩阵（建议新增）

| 类型 | 字符串长度 | 字符集 | 时间精度 | 单调性 | 唯一性范围 | 可解析 | 默认随机源 | 典型用途 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UUIDv7 | 36（含连字符） | hex + 连字符 | 1 ms | 同毫秒可单调（需选 RFC 9562 `6.2 方法） | 全局概率唯一 | 是（version=7） | SecureRandom | 数据库主键、跨系统 |
| UUIDv8 | 36 | hex + 连字符 | 可自定义 | 取决于布局 | 全局概率唯一 | 是（version=8） | SecureRandom | 内部自定义布局（不跨系统） |
| ULID | 26 | Crockford Base32 | 1 ms | 单调模式（随机段递增） | 全局概率唯一 | 是 | 可配置（性能优先可非密码学） | 可排序 ID、日志/事件 |
| Snowflake | 19（十进制 long） | 数字 | 1 ms | 严格递增（同节点） | 依赖 WorkerId 全局唯一 | 是（需位分配配置） | 无 | 高并发、短 ID、数值主键 |
| BIZ | 模板决定 | 模板决定 | 取决于占位符 | 取决于序列 | 取决于序列来源（单机/全局） | 需模板 | 取决于占位符 | 可读业务单号 |

### 6.2 启动行为矩阵（建议新增，解决 B-01）

| 场景 | 已引入算法模块 | 配置 default-type | 启动结果 | 默认 Bean | 健康检查 |
| --- | --- | --- | --- | --- | --- |
| A | 无 | 未配置 | 成功 | 无 | UNKNOWN（附"未引入生成器模块"说明） |
| B | 无 | 已配置 | **失败** | — | —（FailureAnalyzer 提示缺失 artifactId） |
| C | 1 个 | 未配置 | 成功 | 该生成器 | UP |
| D | ≥2 个 | 未配置 | **失败**（或按确定性优先级，二选一并写明） | — | —（提示显式配置 default-type） |
| E | ≥1 个 | 已配置 | 成功 | 对应生成器 | UP / WorkerId 异常时 DOWN |
| F | — | generators.<name> 重复 | **失败** | — | —（名称冲突提示） |
| G | — | 非法类型/未知属性 | **失败** | — | —（配置校验异常，含 key 与合法值） |

### 6.3 唯一性与碰撞边界（建议新增）

- 概率型（UUID7/UUID8/ULID）：给出随机位宽与生日碰撞数量级（如 74/80 随机位在 10^9 量级 ID 下的碰撞概率），声明"概率唯一，不保证绝对唯一"。
- 雪花：声明"依赖 WorkerId 全局唯一 + 时钟不倒退（或按策略处理）"；给出 WorkerId 位数对应的最大节点数。
- 业务 ID：逐序列来源声明唯一性范围（内存=单 JVM；Redis=集群内；DB=全局），并声明故障时不降级（避免重复）。
- 明确"单次批量内唯一"与"跨批次唯一"的保证。

### 6.4 验收标准改写示例（Given/When/Then）

**示例 1（FR-SNOW-01 片段）**

> **Given** 配置 timestamp=41、datacenter=5、worker=5、sequence=12，epoch 为 2024-01-01T00:00:00Z，WorkerId 固定为 1；
> **When** 在单线程内连续生成 1,000,000 个 ID；
> **Then** 所有 ID 为正 long；相邻 ID 严格递增；解析出的 timestamp 落在 [开始时间, 结束时间] 内；序列号在每毫秒内不超过 4095；压测吞吐 ≥ 目标值（以 JMH 报告为准，报告需记录硬件与 JVM 参数）。

> **Given** 位分配总和为 62 或 sequence=0 的配置；**When** 应用启动；**Then** 启动失败，异常信息包含非法项、合法范围与配置 key。

**示例 2（FR-BIZ-01 片段）**

> **Given** 模板 {prefix}-{yyyyMMdd}-{seq:6}-{check}、prefix=ORD、时区 Asia/Shanghai、序列来源为内存；
> **When** 单实例生成 100,000 个 ID；
> **Then** 格式匹配 ORD-8位日期-6位序号-1位校验位；同一自然日内序号单调且不重复；校验位按声明算法可验证；超过 999999 时按声明的溢出策略处理（抛错/进位），且行为有测试覆盖。

### 6.5 指标清单（建议新增，替换 L362-368）

| 指标名 | 类型 | 标签 | 单位 | 说明 |
| --- | --- | --- | --- | --- |
| knitid.id.generation.duration | Timer | type、name | seconds（Micrometer 基准） | 生成耗时与次数；P50/P95/P99 |
| knitid.id.generation.errors | Counter | type、name、reason | — | reason：CLOCK_BACKWARDS、WORKER_UNAVAILABLE、SEQ_EXHAUSTED、CONFIG、INTERNAL |
| knitid.id.clock.backwards | Counter | name、strategy | — | 回拨次数与幅度（幅度可用 DistributionSummary） |
| knitid.id.worker.id | Gauge | name | — | 当前 WorkerId；仅展示，不作为标签 |
| knitid.id.batch.size | DistributionSummary | type、name | — | 批量大小分布 |
| knitid.id.parser.duration | Timer | type | seconds | 可选，解析性能 |

### 6.6 配置参考骨架（建议新增）

```yaml
knitid:
  id:
    enabled: true                 # 默认 true
    default-type: UUID7           # 无默认值；多生成器时必填；类型名不区分大小写
    generators:
      order-id:
        enabled: true
        type: SNOWFLAKE
        snowflake:
          worker-id: 1            # 必填（或由 WorkerIdAssigner 提供）
          datacenter-id: 1
          epoch: 2024-01-01T00:00:00Z
          clock-backwards-strategy: WAIT   # WAIT/THROW/BORROW_FUTURE（后者默认禁用）
      user-id:
        type: ULID
        ulid:
          monotonic: true
          random-source: PERFORMANCE  # SECURE/PERFORMANCE
      biz-id:
        type: BIZ
        biz:
          template: "{prefix}-{yyyyMMdd}-{seq:6}-{check}"
          prefix: ORD
          zone-id: Asia/Shanghai
          sequence-provider: memory
          check-algorithm: MOD11_2       # NONE/LUHN/MOD11_2/MOD37_36
```

---

## 7. 待决问题（Open Questions，需项目负责人拍板）

| # | 问题 | 选项 | 建议 |
| --- | --- | --- | --- |
| Q1 | groupId/包名 | 私有/公共坐标 | 立即确定（如 com.unitseek.knitid），同步 package 与发布配置 |
| Q2 | 配置前缀 | app.id / knitid / knitid.id | knitid（避免与业务冲突） |
| Q3 | 自研 vs 包装第三方库 | 逐模块决策 | 核心算法自研（可控、可测、无传递依赖）；风险表同步更新 |
| Q4 | Spring Boot 支持范围 | 仅 4.1 / 4.0+4.1 / 3.5+4.x | 4.0.x + 4.1.x 矩阵；明确不承诺 3.x |
| Q5 | Java 基线 | 17 / 21 | 若无需 Java 21 特性，建议 17 扩大用户面 |
| Q6 | UUID8 布局 | 固定默认 + 少量预设 / 完全可配 | 固定默认布局 + 预设，完全可配会放大兼容与测试成本 |
| Q7 | 默认随机源 | SecureRandom / ThreadLocalRandom（按类型） | UUIDv7 用 SecureRandom，ULID 可配置并默认性能模式（需性能验证） |
| Q8 | 业务 ID 唯一性范围与日切时区 | 单机 / 集群 / 全局 | 逐来源声明；时区显式必填 |
| Q9 | WorkerId 首期支持范围 | 固定/环境变量/Redis/ZK/K8s | M1 固定+环境变量，M2 Redis 示例 |
| Q10 | GraalVM native | 支持/不支持 | 明确排除或补 AOT hints 与测试 |
| Q11 | 非 Spring 静态门面 | 提供/不提供 | core 零 Spring 依赖 + 可选静态门面（低成本） |
| Q12 | 1.0 前是否冻结 API | 是/否 | 建议 M2 起冻结公共 API，M3 只修 bug |

---

## 8. 建议推进步骤

1. **评审会（0.5 天）**：逐项确认第 7 节 Q1-Q12，记录为 ADR。
2. **v0.2 修订（2-3 天）**：解决 10 个 Blocker；补第 5 节第 1-9 章；为每个 P0 补 Given/When/Then 验收。
3. **ADR（与 v0.2 并行）**：接口设计、雪花位分配与回拨、业务 ID 模板与序列、WorkerId 分配、可观测性命名、版本与兼容策略。
4. **骨架先行（1 周）**：Maven 多模块 + 契约测试（测试向量、属性测试、自动配置矩阵）+ CI 矩阵，先跑通"失败测试"再实现。
5. **M1 实现**：core API/SPI/异常 → UUID7 + Snowflake（固定 WorkerId）→ starter/autoconfigure → 示例；完成 M1 退出准则与文档。
6. **M2**：ULID、UUID8、业务 ID、Redis WorkerId/序列示例、SPI 示例。
7. **M3**：Actuator、指标、基准报告、兼容性验证、1.0 发布；冻结 API 并确认弃用策略。

---

## 9. 参考依据

- RFC 9562（UUIDv7 `5.7、UUIDv8 `5.8、单调性方法 `6.2）：https://www.rfc-editor.org/rfc/rfc9562
- ULID 规范：https://github.com/ulid/spec
- Spring Boot 4.x 系统要求（Boot 4 最低 Java 17；当前稳定线 4.1.x，最新 4.1.2）：https://docs.spring.io/spring-boot/4.0/system-requirements.html
- Spring Boot 自动配置与 AutoConfiguration.imports：https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html
- Spring Boot Actuator 端点与暴露配置：https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot EndpointId（字母数字、比较时忽略大小写与符号）：https://docs.spring.io/spring-boot/docs/3.3.0-M2/api/org/springframework/boot/actuate/endpoint/EndpointId.html
- Micrometer 指标命名约定：https://micrometer.io/docs/concepts#_naming_meters

---

> 附注：本评审仅基于当前工作区中的 FRD v0.1 文本，未包含代码/原型/示例的验证；文中行号均指该版本。建议作者在 v0.2 中同步更新结论段与优先级，避免评审意见与正文再次漂移。
