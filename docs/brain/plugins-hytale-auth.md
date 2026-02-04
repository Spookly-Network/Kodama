# Hytale Auth Plugin (Brain)

## Purpose
Provide Hytale authentication tokens during instance preparation without baking Hytale logic into the Brain.

## What changed
- Added a Brain plugin in `plugins/KodamaHytaleAuthenticator` that mints session tokens and injects them as instance variables.

## How to use / impact
- Build the plugin JAR from `plugins/` and copy it into the Brain working directory `./plugins`.
- Enable it by setting `PLUGINS_ENABLED=hytale-auth` for the Brain process.
- Configure the required environment variables below.

Required environment variables:
- `HYTALE_AUTH_REFRESH_TOKEN`
- `HYTALE_AUTH_TOKEN_URL`
- `HYTALE_AUTH_PROFILES_URL`
- `HYTALE_AUTH_SESSION_URL`

Optional environment variables:
- `HYTALE_AUTH_CLIENT_ID` (defaults to `hytale-server`)
- `HYTALE_AUTH_SCOPES` (defaults to `openid offline auth:server`)
- `HYTALE_AUTH_PROFILE_UUID`
- `HYTALE_AUTH_PROFILE_USERNAME`
- `HYTALE_AUTH_TIMEOUT_SECONDS` (defaults to `10`)

Injected instance variables:
- `HYTALE_SERVER_SESSION_TOKEN`
- `HYTALE_SERVER_IDENTITY_TOKEN`

## Edge cases / risks
- Tokens are minted at prepare time. If instances remain prepared for long periods, tokens may expire before start.
- If Hytale endpoints are unavailable, instance preparation will fail.
- If multiple profiles exist and no selector is configured, the first profile is used.

## Links
- `plugins/KodamaHytaleAuthenticator/src/main/java/net/spookly/kodama/plugins/hytale/HytaleAuthPlugin.java`
- `backend/brain/src/main/java/net/spookly/kodama/brain/plugin/KodamaBrainPlugin.java`
