# Smart Learning Agent

基于 RAG 与多智能体协同的通用学习内容生成系统。

## Codex / Harness 开发入口

在 Codex 应用中请直接打开 `D:\BussinessEnglish`。智能体从 `AGENTS.md` 获取仓库地图，人类学习与协作流程见 `docs/HARNESS_GUIDE.md`，统一质量门执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
```

## 定位

系统不只面向商务英语，也可以用于英语学习、计算机课程、通识课程或自定义教材。当前第一版实现了：

- 教材上传：PDF、DOCX、TXT、Markdown、Excel。
- 教材解析与切片：把教材转成可检索片段。
- 向量检索：默认内存检索，已预留 Qdrant 配置和适配类。
- 多智能体出题链路：教材理解、检索规划、习题生成、质量审校、去重难度、教学编排。
- 题库审核：生成题目默认待审核，可通过或退回。
- 学生练习接口：第一版支持答案提交和简单判分。
- 持久化架构：已抽象 repository，支持 memory 与 MyBatis/MySQL 两种模式。

## 目录

```text
D:\BussinessEnglish
├─ backend   # Spring Boot 后端
├─ frontend  # Vue3 + Element Plus 前端
├─ deploy    # Nginx 配置
├─ docker-compose.yml
├─ 毕业设计_系统设计方案.md
└─ 毕业设计_开题报告.md
```

## 后端启动：内存模式

内存模式适合快速开发，不需要 MySQL，重启后数据会丢失。

```powershell
cd D:\BussinessEnglish\backend
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' spring-boot:run
```

默认地址：

```text
http://localhost:18082
```

## 后端启动：MySQL 持久化模式

先启动 MySQL，再运行后端 `mysql` profile。

```powershell
cd D:\BussinessEnglish
# Docker Desktop 未启动时，这条命令会失败；先打开 Docker Desktop。
docker compose up -d mysql

cd D:\BussinessEnglish\backend
$env:MYSQL_URL="jdbc:mysql://localhost:3306/smart_learning?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

`application-mysql.yml` 会设置：

```yaml
app:
  repository:
    provider: mybatis
```

对应表结构：`backend/src/main/resources/db/schema-mysql.sql`。

## 前端启动

```powershell
cd D:\BussinessEnglish\frontend
npm install
npm run dev
```

默认地址：

```text
http://localhost:5174
```

前端会通过 Vite proxy 转发 `/api` 到 `http://localhost:18082`。

## Docker 部署

先构建后端 jar：

```powershell
cd D:\BussinessEnglish\backend
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' package
```

再启动全量容器：

```powershell
cd D:\BussinessEnglish
docker compose up -d --build
```

访问：

```text
http://localhost:8080
```

容器说明：

```text
frontend  -> Nginx + Vue dist，端口 8080
backend   -> Spring Boot，端口 18082
mysql     -> MySQL 8，端口 3306
qdrant    -> Qdrant，端口 6333
```

注意：当前 `QdrantVectorStore` 已预留连接和 collection 初始化，但真实 upsert/search 仍使用内存检索兜底，方便第一阶段稳定演示。下一步会补齐真正的 Qdrant 写入和检索。

## 构建验证

```powershell
cd D:\BussinessEnglish\backend
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' test
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' package

cd D:\BussinessEnglish\frontend
npm run build
```

## 关键接口

```text
GET  /api/health
POST /api/materials/upload
POST /api/materials/{materialId}/parse
GET  /api/materials
GET  /api/materials/{materialId}/chunks
POST /api/courses
GET  /api/courses
POST /api/courses/{courseId}/chapters
GET  /api/courses/{courseId}/chapters
POST /api/materials/{materialId}/course-binding
POST /api/materials/{materialId}/knowledge-points/refresh
GET  /api/knowledge-points
POST /api/rag/retrieve
POST /api/questions/generation-tasks
GET  /api/questions
PUT  /api/questions/{questionId}
POST /api/questions/{questionId}/approve
POST /api/questions/{questionId}/reject
POST /api/questions/batch-status
GET  /api/questions/export
GET  /api/agents/workflow-template
GET  /api/agents/runs
GET  /api/agents/runs/{runId}
POST /api/practice/submit
GET  /api/runtime/status
```

## 后续开发重点

1. 增加课程、课时、班级和学生账号。
2. 增加学生练习记录、错题本和知识点掌握度统计。
3. 增加题目发布到课时、练习卷组卷和练习结果分析。
4. 增加基于教材来源的题目质量评估指标。
5. 接入真实 Embedding 模型，提升 Qdrant 语义检索效果。

## 题库审核能力

当前题库模块已经支持教师二次加工：

- 单题编辑：修改题干、选项、答案、解析和难度。
- 批量审核：勾选多道题后批量通过或退回。
- Excel 导出：可按当前教材和审核状态导出题库，便于交给教师复核或放入论文演示材料。

## 课程与知识点能力

系统已经从“教材工具”扩展为课程型学习平台：

- 课程管理：创建通用学习、英语、商务英语、计算机或自定义课程。
- 章节管理：在课程下维护章节，教材上传时可绑定课程和章节。
- 知识点抽取：教材解析后会根据切片关键词生成候选知识点，教师可据此组织题库和练习。
- 检索联动：知识点保留教材来源片段，便于答辩时说明“题目和知识点都可追溯到教材证据”。


## AI / Vector Provider Configuration

The system now supports pluggable AI and vector providers:

1. `AI_PROVIDER=mock`: offline demo mode, no API key required.
2. `AI_PROVIDER=deepseek`: DeepSeek/OpenAI-compatible chat API for better question stems, answers, and explanations.
3. `EMBEDDING_PROVIDER=openai-compatible`: OpenAI-compatible `/v1/embeddings` API for stronger semantic retrieval with Qdrant.

Recommended demo configuration:

```env
AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
AI_CHAT_PATH=/chat/completions
AI_MODEL=deepseek-v4-flash
AI_THINKING_ENABLED=false
AI_REASONING_EFFORT=medium
EMBEDDING_PROVIDER=mock
APP_VECTOR_DIMENSION=64
```

If you enable a real embedding model, set `APP_VECTOR_DIMENSION` to the model's real vector size. Otherwise Qdrant will reject vector writes because collection dimensions must match. See `Docker_Deployment.md` for details.
