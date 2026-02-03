# Brain CORS Configuration

## Purpose
Allow the Web Panel (or other clients) to call the Brain API from a different origin.

## What changed
The Brain now exposes a configurable CORS policy driven by `brain.web.cors.*` properties.

## How to use / impact
Set the allowed origins to the Web Panel URL so browsers can call the API.
Defaults allow `http://localhost:3000` for local development.

Environment variables:
- `BRAIN_CORS_ALLOWED_ORIGINS` (comma-separated)
- `BRAIN_CORS_ALLOWED_METHODS` (comma-separated)
- `BRAIN_CORS_ALLOWED_HEADERS` (comma-separated)
- `BRAIN_CORS_EXPOSED_HEADERS` (comma-separated)
- `BRAIN_CORS_ALLOW_CREDENTIALS` (`true`/`false`)
- `BRAIN_CORS_MAX_AGE_SECONDS`

Example:
```
BRAIN_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://panel.example.com
```

## Edge cases / risks
- If `BRAIN_CORS_ALLOWED_ORIGINS` is empty, browsers will block cross-origin requests.
- Keep the allowlist tight in production.

## Links
- `backend/brain/src/main/java/net/spookly/kodama/brain/config/BrainCorsProperties.java`
- `backend/brain/src/main/java/net/spookly/kodama/brain/config/SecurityConfig.java`
