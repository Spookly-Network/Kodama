# Panel Authentication

## Purpose
Describe how the webpanel authenticates with the Brain API and protects routes.

## What changed
- Added a Brain API composable that injects the access token.
- Added route middleware to require authentication for protected pages.
- Login now calls the Brain `/api/auth/login` endpoint and stores the session.

## How to use / impact
- Set `NUXT_PUBLIC_BRAIN_BASE_URL` when the webpanel is not reverse-proxied to the Brain.
- Successful logins store a `kodama.session` cookie that includes the JWT and expiry.
- Protected routes redirect to `/login?redirect=<path>` when the session is missing or expired.

## Edge cases / risks
- An empty or incorrect `NUXT_PUBLIC_BRAIN_BASE_URL` will break login and API calls.
- Expired sessions are treated as unauthenticated; users must re-authenticate.
- `401 Unauthorized` responses clear the session; the next navigation redirects to login.

## Links
- Auth store: `webpanel/app/store/auth.ts`
- Brain API plugin: `webpanel/app/plugins/api.ts`
- Auth middleware: `webpanel/app/middleware/auth.global.ts`
- Login page: `webpanel/app/pages/login.vue`
