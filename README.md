# Smart Learning Agent

基于 RAG 与多智能体协同的通用学习内容生成系统。

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
POST /api/rag/retrieve
POST /api/questions/generation-tasks
GET  /api/questions
POST /api/questions/{questionId}/approve
POST /api/questions/{questionId}/reject
GET  /api/agents/workflow-template
GET  /api/agents/runs
GET  /api/agents/runs/{runId}
POST /api/practice/submit
```

## 后续开发重点

1. 完善 `QdrantVectorStore` 的真实 upsert/search HTTP 调用。
2. 接入真实 LLM 和 Embedding API，同时保留 Mock 模式。
3. 增加课程、课时、学生账号、学习记录、错题本。
4. 增加教师编辑题目、批量审核、发布到课时。
5. 增加基于教材来源的题目质量评估指标。
