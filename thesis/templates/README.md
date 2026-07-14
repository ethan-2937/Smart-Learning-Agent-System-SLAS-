# 模板管理

这里的两个 DOCX 是只读参考原件。`manifest.json` 记录了来源、大小、SHA-256 和使用限制。

- `hlju-thesis-engineering-template.docx`：黑龙江大学理工科毕业论文官方模板。
- `hlju-opening-report-layout-reference.docx`：已填写开题报告的版式参考，正文属于旧项目，不能复用其研究结论。

当前只完成了结构读取和哈希固定。由于现有 Codex 文档运行时未找到 LibreOffice，尚未完成模板逐页渲染与完整 `artifact.md` 提炼。正式生成 Word 文件前必须先完成模板提炼，再从原件副本进行局部替换并逐页检查。
