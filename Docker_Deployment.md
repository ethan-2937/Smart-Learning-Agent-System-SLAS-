# Docker Deployment Notes

## Local startup

The backend image copies `backend/target/*.jar`, so build the backend jar before the first compose run:

```powershell
cd D:\BussinessEnglish\backend
& 'C:\Users\23885\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd' package
```

Start all containers:

```powershell
cd D:\BussinessEnglish
docker compose up -d --build
```

Useful URLs:

```text
Frontend:       http://localhost:8080
Backend health: http://localhost:18082/api/health
Runtime status: http://localhost:8080/api/runtime/status
Qdrant UI:      http://localhost:6333/dashboard
```

## Docker Hub EOF workaround

If Docker Hub fails with EOF, run compose through the local Clash proxy:

```powershell
$env:HTTP_PROXY="http://127.0.0.1:7897"
$env:HTTPS_PROXY="http://127.0.0.1:7897"
docker compose up -d --build
```

You can also set the same proxy in Docker Desktop: `Settings -> Resources -> Proxies`.

## Enable DeepSeek V4 Flash

Copy `.env.example` to `.env` and edit the API key:

```env
AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
AI_CHAT_PATH=/chat/completions
AI_API_KEY=your_deepseek_api_key
AI_MODEL=deepseek-v4-flash
AI_THINKING_ENABLED=false
AI_REASONING_EFFORT=medium
```

DeepSeek is suitable for question generation, answer explanation, and agent reasoning. Vector search still needs an embedding model. The default `EMBEDDING_PROVIDER=mock` is enough for offline demos. For stronger semantic retrieval, configure an OpenAI-compatible embedding API and set `APP_VECTOR_DIMENSION` to the real embedding dimension.

## Common commands

```powershell
docker compose ps
docker compose logs -f backend
docker compose restart backend
docker compose down
```
