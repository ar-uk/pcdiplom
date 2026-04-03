# recommendation-service

Spring Boot microservice for turning a user prompt into a PC build recommendation.

## What it does
- Accepts a prompt such as "Build me a 1440p gaming PC under 1500"
- Uses OpenAI to extract structured intent
- Returns a starter build response that can later be replaced with real catalog lookups from `partService`
- Supports a follow-up chat endpoint for future build edits

## Endpoints
- `POST /api/recommendation/build`
- `POST /api/recommendation/chat/{sessionId}`

## Environment variables
Set your OpenAI key before starting the service:

```bash
OPENAI_API_KEY=your_key_here
```

## Run
From the service root:

```bash
./gradlew bootRun
```

If you are on Windows:

```powershell
gradlew.bat bootRun
```

## Notes
This project currently returns a scaffolded build response. The next step is connecting it to `partService` so the selected parts come from your actual database instead of sample data.
