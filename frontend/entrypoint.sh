#!/bin/sh
set -e

echo "🚀 Starting frontend container..."

# (Opcional) mostrar variables de entorno útiles
echo "ENV:"
env | grep -i api || true

# (Opcional) si querés reemplazar variables dentro del build
# envsubst < /usr/share/nginx/html/config.template.js > /usr/share/nginx/html/config.js

# Verificar nginx config
nginx -t

# Arrancar nginx en foreground
exec nginx -g "daemon off;"

