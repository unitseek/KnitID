# Governance

## 角色

| 角色 | 职责 |
| --- | --- |
| **Contributor** | 提交 Issue/PR、文档、示例 |
| **Committer** | 审核 PR、triage Issue、维护文档 |
| **Maintainer** | 合并权限、版本发布、安全响应、兼容性决策 |

当前维护者见 [MAINTAINERS.md](MAINTAINERS.md)。

## 决策方式

- **日常变更**：PR + 至少 1 名 Committer/Maintainer 审核；
- **公共 API/配置/输出格式变更**：需要 ADR，并由 Maintainer 达成共识；
- **争议**：由 Maintainer 讨论；无法达成共识时由项目负责人决定并记录理由；
- **安全**：按 [SECURITY.md](SECURITY.md) 私密处理，修复发布前不公开细节。

## Lazy Consensus

非破坏性变更在 Issue/PR 中公示至少 72 小时，无实质反对即视为通过；破坏性变更需要明确批准，不适用 lazy consensus。

## 发布权限

- 只有 Maintainer 可以触发 release workflow；
- 版本号遵循 [SemVer](https://semver.org/)；
- 1.0 后公共 API/SPI/配置键/ID 输出格式冻结，破坏性变更进入下一个 MAJOR；
- 发布前必须通过 `mvn verify`、`mvn -Prelease package` 与发布检查清单。

## 成为 Committer / Maintainer

- 持续贡献（代码/文档/Issue triage）并被现有维护者认可；
- 理解项目兼容性、安全与发布流程；
- 由现有 Maintainer 提名并达成共识；
- 没有固定贡献数量门槛，质量、判断力和协作更重要。

## 变更治理

修改本文档需要 PR + 现有 Maintainer 多数同意；重大治理变更应附带 ADR 并公示。

## 行为准则

所有参与者必须遵守 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)；违规由 Maintainer 按准则处理。
