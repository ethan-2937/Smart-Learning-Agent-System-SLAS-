# 第5章 核心模块设计与实现

draft_status: skeleton

状态：骨架。

## 5.1 教材解析、切片与知识点

## 5.2 向量入库与语义检索

## 5.3 习题生成智能体

`QuestionGenerationServiceImpl` 负责生成任务生命周期，不再先完成生成再补写智能体标签。它规范化题型、难度和模式后，将 `WorkflowRequest` 交给 `AgentRunServiceImpl`；只有编排运行成功后才保存最终候选题。`QuestionGenerationAgent` 每次执行都会创建新的 `QuestionRecord`。RAG 模式从检索命中构造 `QuestionSourceRef`，直接模式则写入分数为 0、切片 ID 为空且带人工确认说明的来源标记。两种情况均保持 `PENDING_REVIEW`。

默认 mock 提供者走确定性规则生成，不调用网络。启用远程提供者时，模型调用仍通过 `LlmClient` 边界，调用本身被记录为真实工具调用；非法结构化响应会导致生成角色失败，而不会被记录成成功步骤。

## 5.4 质量审校、去重与难度智能体

`QualityReviewAgent` 不复用生成角色的结论，而是接收候选题、检索证据和请求重新检查。缺少题干或答案、单选题正确选项不唯一或与答案不一致时给出 `REJECT`；证据缺失、来源切片无法对应或直接模式的未检索来源需要 `HUMAN_REVIEW`；缺少解析时给出 `REVISE`；满足证据、答案唯一性、格式与可审核性条件时给出 `PASS`。这些分支由 `QualityReviewAgentTest` 的确定性样例覆盖。

`DedupDifficultyAgent` 通过 `QuestionRepository` 读取已有题目，按规范化题干检查已有题与批内重复，移除审校拒绝项和重复项，并用确定性规则评估难度。`TeachingComposerAgent` 最后确保候选题仍为待审核状态，并把修订、人工复核和去重信息整理成教师建议。`AgentRunServiceImplTest` 验证六个角色的调用顺序以及后一步输入确实包含前一步输出；失败测试验证链路不会继续伪造后续成功状态。

## 5.5 教师审核与题目发布

## 5.6 学生练习与学习反馈

## 5.7 持久化、身份认证与角色权限

`AgentRunRecord` 新增运行模式、失败角色、错误摘要、起止时间和观测指标；每个 `AgentStepReport` 保存结构化输入/输出 JSON、独立状态、时间和尝试次数；每个 `AgentToolCall` 保存调用 ID、所属角色、起止时间、成功标记和失败摘要。memory 与 MyBatis 实现使用同一领域对象，`MyBatisAgentRunRepositoryTest` 验证扩展字段的 JSON/PO 往返映射。旧 MySQL 数据库需一次性应用版本化升级脚本 `2026-07-13-agent-run-execution-trace.sql`。

写作规则：每节至少关联一个代码边界和一个测试或实验，不用大量代码截图充当解释。
