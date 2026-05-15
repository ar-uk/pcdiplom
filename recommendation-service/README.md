# recommendation-service

Spring Boot microservice for turning a user prompt into a PC build recommendation.

## What it does

- Accepts a prompt such as "Build me a 1440p gaming PC under 1500"
- Calls a local **Ollama** HTTP endpoint (default **DeepSeek R1**) to extract structured intent; compatibility stays in Java
- Returns a build response grounded in catalog and compatibility logic

## LLM settings (not in the database)

Configure only via `src/main/resources/application.properties` or environment variables (`LLM_ENABLED`, `LLM_MODEL`, `LLM_URL`). Nothing is stored in Postgres for LLM host or model name.

Example `.env` next to Compose (optional):

```bash
LLM_ENABLED=true
LLM_MODEL=deepseek-r1:14b
LLM_URL=http://127.0.0.1:11434/v1/chat/completions
```

If `recommendation-service` runs **inside Docker** and Ollama runs on the **host**, use `LLM_URL=http://host.docker.internal:11434/v1/chat/completions` (see root `docker-compose.yml`).

## Endpoints

- `POST /api/recommendation/build`
- `POST /api/recommendation/chat/{sessionId}`

## Run

From the service root:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```
