# 论文优先 Harness

本目录把毕业设计的系统实现、实验和正式文档放在同一条可追溯链路中。正式题目为“基于RAG与多智能体协同的外贸英语智能习题生成系统设计与实现”。

## 当前状态

- 学生、学院、专业、班级、学号和指导教师信息已确认。
- 开题报告日期和论文起止时间尚未确认，保持为空。
- 学校理工科论文模板与开题报告版式参考已经复制并记录哈希。
- 现有 `毕业设计_开题报告.md` 暂时是开题报告内容源。
- 论文只有章节与证据骨架，还没有生成正式 DOCX。

## 工作流

```text
功能任务 -> 代码与测试 -> 证据矩阵 -> 实验原始结果 -> 图表/章节 -> 模板化 DOCX -> 渲染审查
```

日常检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-thesis.ps1
```

最终提交检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-thesis.ps1 -Release
```

`-Release` 目前应当失败，这是有意设计：日期、完整实验和最终文档尚未就绪时，不允许把草稿误认为提交版本。
