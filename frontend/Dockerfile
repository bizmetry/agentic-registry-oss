# =========================
# STAGE 1 - Build
# =========================
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


# =========================
# STAGE 2 - Runtime
# =========================
FROM nginx:alpine

# Limpiar config default
RUN rm /etc/nginx/conf.d/default.conf

# Copiar nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar dist
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar entrypoint
COPY entrypoint.sh /entrypoint.sh

# ✅ Blindaje: permisos + CRLF->LF + verificación rápida
RUN set -eux; \
    sed -i 's/\r$//' /entrypoint.sh; \
    chmod +x /entrypoint.sh; \
    head -n 1 /entrypoint.sh | cat; \
    ls -la /entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]

