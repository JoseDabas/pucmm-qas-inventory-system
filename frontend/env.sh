#!/bin/sh
# Recreate config file
rm -rf /usr/share/nginx/html/env-config.js
touch /usr/share/nginx/html/env-config.js

# Add assignment 
echo "window._env_ = {" >> /usr/share/nginx/html/env-config.js

# Add environment variables
echo "  VITE_KEYCLOAK_URL: \"$VITE_KEYCLOAK_URL\"," >> /usr/share/nginx/html/env-config.js
echo "  VITE_API_BASE_URL: \"$VITE_API_BASE_URL\"," >> /usr/share/nginx/html/env-config.js
echo "  VITE_KEYCLOAK_CLIENT_ID: \"$VITE_KEYCLOAK_CLIENT_ID\"," >> /usr/share/nginx/html/env-config.js
echo "  VITE_KEYCLOAK_CLIENT_SECRET: \"$VITE_KEYCLOAK_CLIENT_SECRET\"" >> /usr/share/nginx/html/env-config.js

echo "};" >> /usr/share/nginx/html/env-config.js

# Start nginx
exec nginx -g "daemon off;"
