# Hytale Auth Plugin (Brain)

## Purpose
Provide Hytale authentication tokens during instance preparation without baking Hytale logic into the Brain.

## What changed
- Added a Brain plugin in `plugins/KodamaHytaleAuthenticator` that mints session tokens and injects them as instance variables.

## How to use / impact
- Build the plugin JAR from `plugins/` and copy it into the Brain working directory `./plugins`.
- Enable it by setting `PLUGINS_ENABLED=hytale-auth` for the Brain process.
- Add a config file at `./plugins/hytale-auth.json` (or override with `HYTALE_AUTH_CONFIG_PATH`).

Example config (`plugins/hytale-auth.json`):

```json
{
  "refreshToken": "your-refresh-token",
  "tokenUrl": "https://auth.hytale.com/...",
  "profilesUrl": "https://api.hytale.com/my-account/get-profiles",
  "sessionUrl": "https://api.hytale.com/game-session/new",
  "clientId": "hytale-server",
  "scopes": "openid offline auth:server",
  "profileUuid": "00000000-0000-0000-0000-000000000000",
  "profileUsername": "optional",
  "timeoutSeconds": 10
}
```

Required config fields:
- `refreshToken`
- `tokenUrl`
- `profilesUrl`
- `sessionUrl`

Optional config fields:
- `clientId` (defaults to `hytale-server`)
- `scopes` (defaults to `openid offline auth:server`)
- `profileUuid`
- `profileUsername`
- `timeoutSeconds` (defaults to `10`)

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
