# Superset + Spring Boot + React (Vite)

This project embeds Apache Superset dashboards into a React frontend using the Superset Embedded SDK.
A Spring Boot backend securely authenticates to Superset and issues guest tokens, so Superset credentials are never exposed to the browser.

This workspace is pinned to Superset 5.x for a simpler local embedding workflow.

## Quick Start (Run Everything)

Use 3 terminals and start services in this order: Superset -> Backend -> Frontend.

### Prerequisites (Windows)

1. Python 3.11 (required by Superset)
2. Node.js 18+
3. Java 17 JDK
4. Microsoft C++ Build Tools (for native Python package builds on Windows)

### Terminal 1 - Superset (port 8088)

```powershell
cd C:\Users\admin\Desktop\IBM\superset

# Create once (if missing)
if (-not (Test-Path ".venv311\\Scripts\\python.exe")) {
  & "$env:LocalAppData\\Programs\\Python\\Python311\\python.exe" -m venv .venv311
}

.\\.venv311\\Scripts\\Activate.ps1
$env:SUPERSET_CONFIG_PATH="C:\Users\admin\Desktop\IBM\superset\superset-config\superset_config.py"

# First-time setup only
superset db upgrade
superset fab create-admin --username admin --firstname Superset --lastname Admin --email admin@example.com --password admin
superset init

# Start
superset run -p 8088 --with-threads --reload --debugger
```

Superset URL: http://localhost:8088

### Terminal 2 - Backend (port 8080)

```powershell
cd C:\Users\admin\Desktop\IBM\superset\backend

# Required if Java is not already on PATH in this terminal
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:Path="$env:JAVA_HOME\\bin;$env:Path"

# Run with local Maven bundled in this repo
..\\.tools\\apache-maven-3.9.9\\bin\\mvn.cmd spring-boot:run
```

Backend URL: http://localhost:8080

### Terminal 3 - Frontend (port 5173)

```powershell
cd C:\Users\admin\Desktop\IBM\superset\frontend
npm install
npm run dev
```

Frontend URL: http://localhost:5173

### Smoke test

1. Open http://localhost:5173
2. Dashboard list should load from backend `/dashboards`
3. Click a dashboard card and verify the embedded dashboard renders

## 1. Folder Structure

```text
superset/
  backend/
    pom.xml
    .env.example
    src/main/java/com/example/superset/
      SupersetBackendApplication.java
      config/
        CorsConfig.java
        WebClientConfig.java
      controller/
        GlobalExceptionHandler.java
        SupersetController.java
      dto/
        DashboardSummaryDto.java
        GuestTokenRequest.java
        GuestTokenResponse.java
        SupersetGuestTokenApiRequest.java
        SupersetGuestTokenApiResponse.java
        SupersetLoginRequest.java
        SupersetLoginResponse.java
      service/
        SupersetAuthService.java
        SupersetServiceException.java
    src/main/resources/application.properties
  frontend/
    package.json
    .env.example
    index.html
    vite.config.js
    src/
      App.jsx
      App.css
      main.jsx
      components/
        DashboardList.jsx
        DashboardView.jsx
      services/
        api.js
  superset-config/
    superset_config.py
```

## 2. Step 1 - Apache Superset Setup (Python venv)

### Commands (Windows PowerShell)

```powershell
cd C:\Users\admin\Desktop\IBM\superset
& "$env:LocalAppData\Programs\Python\Python311\python.exe" -m venv .venv311
.\.venv311\Scripts\Activate.ps1
python -m pip install --upgrade pip setuptools wheel
pip install "apache-superset==5.0.0"
```

If `pip install apache-superset` fails on Windows with a C++ build error, install build tools and retry:

```powershell
winget install --id Microsoft.VisualStudio.2022.BuildTools -e --override "--quiet --wait --norestart --nocache --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
```

Set Superset config path:

```powershell
$env:SUPERSET_CONFIG_PATH="C:\Users\admin\Desktop\IBM\superset\superset-config\superset_config.py"
```

Initialize Superset:

```powershell
superset db upgrade
superset fab create-admin --username admin --firstname Superset --lastname Admin --email admin@example.com --password admin
superset init
```

Run Superset:

```powershell
superset run -p 8088 --with-threads --reload --debugger
```

Open http://localhost:8088 and sign in with the admin user.

### Enable embedding and allowed domains

1. Ensure `FEATURE_FLAGS = { "EMBEDDED_SUPERSET": True }` is active in `superset_config.py`.
2. In Superset UI, open a dashboard.
3. Click Share -> Embed dashboard.
4. Add allowed domains, for example:
   - `http://localhost:5173`
5. Save. Superset will only allow embedding from these domains.

### Create dashboards

1. Create/connect a database in Superset.
2. Create a dataset from a table.
3. Build charts and save them.
4. Create a dashboard and add charts.
5. Publish dashboard and note the dashboard ID from URL or dashboard settings.

## 3. Step 2 - Backend (Spring Boot)

### Features implemented

- `POST /get-guest-token`: receives dashboard ID and returns guest token.
- `GET /dashboards`: lists available dashboards from Superset.
- Secure Superset login flow in service layer.
- CORS enabled for frontend origin.
- DTO-based request/response handling.
- Centralized error handling via `@RestControllerAdvice`.

### Config

`backend/src/main/resources/application.properties` uses env variables:

- `SUPERSET_BASE_URL` (default `http://localhost:8088`)
- `SUPERSET_USERNAME`
- `SUPERSET_PASSWORD`
- `SUPERSET_PROVIDER` (default `db`)
- `FRONTEND_ORIGIN` (default `http://localhost:5173`)

### Run backend

```powershell
cd C:\Users\admin\Desktop\IBM\superset\backend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

If Maven is installed globally and on PATH, you can also use:

```powershell
mvn spring-boot:run
```

Backend runs at http://localhost:8080.

## 4. Step 3 - Frontend (React + Vite)

If you want to recreate manually:

```powershell
npm create vite@latest frontend -- --template react
cd frontend
npm install
npm install @superset-ui/embedded-sdk@^0.3.0 axios
```

In this repository, frontend files are already provided.

### Run frontend

```powershell
cd C:\Users\admin\Desktop\IBM\superset\frontend
npm install
npm run dev
```

Frontend runs at http://localhost:5173.

### How embed works

- Frontend loads dashboard list from backend `GET /dashboards`.
- User clicks dashboard card.
- Frontend calls backend `POST /get-guest-token` with dashboard UUID.
- Backend logs into Superset and exchanges for guest token.
- Frontend calls `embedDashboard(...)` from Superset Embedded SDK.

## 5. Step 4 - Integration Ports and CORS

Use these service URLs:

- Superset: `http://localhost:8088`
- Spring Boot: `http://localhost:8080`
- React Vite: `http://localhost:5173`

CORS checklist:

1. Backend CORS is configured in `CorsConfig.java` for frontend origin.
2. Superset CORS and CSP `frame-ancestors` include `http://localhost:5173`.
3. Dashboard embed settings in Superset include `http://localhost:5173` as allowed domain.

## 6. API Contract

### `GET /dashboards`

Returns:

```json
[
  {
    "id": "3",
    "title": "Revenue Overview",
    "slug": "revenue-overview"
  }
]
```

### `POST /get-guest-token`

Request:

```json
{
  "dashboardId": "3"
}
```

Response:

```json
{
  "token": "<guest-jwt-token>"
}
```

## 7. Common Errors and Fixes

1. `403 Forbidden` on embed iframe:
   - Ensure dashboard has embed enabled and `http://localhost:5173` is in allowed domains.
   - Verify `EMBEDDED_SUPERSET` feature flag is true.

2. `401` or `422` from `/api/v1/security/login`:
   - Check `SUPERSET_USERNAME`, `SUPERSET_PASSWORD`, and `SUPERSET_PROVIDER=db`.

3. `Superset guest token request failed`:
   - Confirm user has permission to generate guest tokens.

- Confirm dashboard UUID exists and the dashboard is published.
- Confirm the role configured by `GUEST_ROLE_NAME` has:
  - `can read on CurrentUserRestApi`
  - `can log on Superset`
  - `can set embedded on Dashboard`

4. Browser CORS errors:
   - Check backend `FRONTEND_ORIGIN` matches frontend URL exactly.
   - Restart Superset after changing `superset_config.py`.

5. `Refusing to start due to insecure SECRET_KEY`:

- Set `SECRET_KEY` in `superset-config/superset_config.py`.
- Re-run `superset db upgrade` and `superset init`.

6. Blank embedded area:
   - Confirm `VITE_SUPERSET_DOMAIN` points to active Superset URL.
   - Open devtools network tab and verify guest token API returns a token.

## 8. Production Hardening Notes

1. Replace admin credentials with a dedicated technical Superset user.
2. Store secrets in a vault or environment-specific secret manager.
3. Add auth in Spring Boot so `/get-guest-token` is protected.
4. Add caching of Superset login token to reduce repeated logins.
5. Add HTTPS and strict domain allow-lists in Superset and backend.
#   s u p e r s e t  
 