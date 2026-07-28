#!/bin/bash
set -e

echo "=========================================="
echo "Iniciando aprovisionamiento del servidor..."
echo "=========================================="

echo "Actualizando paquetes del sistema..."
apt-get update -y
apt-get upgrade -y

echo "Instalando dependencias necesarias..."
apt-get install -y ca-certificates curl

echo "Instalando Docker Engine y Docker Compose..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "Habilitando y reiniciando el servicio de Docker..."
systemctl enable docker
systemctl start docker

echo "Configurando Firewall (UFW)..."
# Políticas por defecto
ufw default deny incoming
ufw default allow outgoing

# Puertos permitidos para la aplicación y administración
ufw allow 22/tcp      # SSH
ufw allow 80/tcp      # HTTP estandar (Opcional, si se usa reverse proxy a futuro)
ufw allow 5173/tcp    # Frontend
ufw allow 8080/tcp    # Backend
ufw allow 9080/tcp    # Keycloak

# Asegurar explícitamente que la base de datos no es accesible desde el exterior
ufw deny 5432/tcp

echo "Activando UFW..."
ufw --force enable

echo "Creando directorios para la aplicación..."
APP_DIR="/opt/inventory-app"
mkdir -p ${APP_DIR}
# Asignamos permisos adecuados (si el script lo corre root y usamos root para desplegar, está bien así)
chmod 750 ${APP_DIR}

echo "=========================================="
echo "Aprovisionamiento completado exitosamente."
echo "=========================================="
