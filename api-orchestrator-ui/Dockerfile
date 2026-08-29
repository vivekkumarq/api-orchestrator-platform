# syntax=docker/dockerfile:1

# ---- build ------------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /build

# Install from the lockfile first so a source edit does not re-resolve deps.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# The API base URL is baked in at build time: Vite substitutes import.meta.env
# during the bundle, so this cannot be changed by an environment variable at
# container start. Override it with --build-arg when building for a deployment.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- runtime ----------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://localhost/ || exit 1
