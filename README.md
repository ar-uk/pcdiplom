# PC Builder Services (Standalone)

This workspace now uses **4 standalone Spring Boot services**:

- `partService` (`part-service`, port `8081`)
- `auth-service` (`auth-service`, port `8082`)
- `community-service` (`community-service`, port `8083`)
- `api-gateway` (`api-gateway`, port `8080`)

## Version alignment

All services are aligned to:

- Spring Boot `3.3.5`
- Spring Cloud BOM `2023.0.4`
- Java `17`
- Gradle wrapper `9.3.1`

## JWT flow

- `auth-service` issues and validates JWT (`/auth/token`, `/auth/validate`).
- `api-gateway` validates JWT on protected routes and forwards requests.
- `/auth/**` is public on gateway; other routes require `Authorization: Bearer <token>`.

## Eureka

All services are configured as Eureka clients with:

- `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`

Start your Eureka server first.

## Gateway routes

Configured in `api-gateway/src/main/resources/application.properties`:

- `/api/**` -> `lb://part-service`
- `/community/**` -> `lb://community-service`
- `/auth/**` -> `lb://auth-service`

## Verify tests

Run in each service folder:

```powershell
.\gradlew.bat test --no-daemon --console=plain
```

## Start services (suggested order)

1. Eureka server
2. `partService`
3. `auth-service`
4. `community-service`
5. `api-gateway`

Example:

```powershell
Set-Location "c:\Users\User\Desktop\pc\partService"
.\gradlew.bat bootRun
```

Use the same pattern for `auth-service`, `community-service`, and `api-gateway`.

## Quick API checks

1. Get token from auth:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/token" -ContentType "application/json" -Body '{"username":"demo"}'
```

2. Use returned token for protected route:

```powershell
$token = "<paste-token>"
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/community/ping" -Headers @{ Authorization = "Bearer $token" }
```

## Docker quick start (teammate-friendly)

From the workspace root:

```powershell
docker compose up --build
```

Then open:

- Frontend: `http://localhost:5173`
- API gateway: `http://localhost:8080`
- Eureka dashboard: `http://localhost:8761`

Stop all containers:

```powershell
docker compose down
```

Reset DB data (fresh state):

```powershell
docker compose down -v
```
