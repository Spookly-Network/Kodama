# Build-Stage
FROM node:22-alpine AS build
WORKDIR /app

COPY webpanel/package*.json ./
RUN pnpm install

COPY webpanel/. .
RUN pnpm run build

# Runtime-Stage
FROM node:22-alpine
WORKDIR /app

ENV NITRO_PORT=3000
ENV HOST=0.0.0.0

COPY --from=build /app/.output ./.output

CMD ["node", ".output/server/index.mjs"]
