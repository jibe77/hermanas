#!/bin/bash

# Script de déploiement automatique pour Hermanas
# Usage: ./deploy.sh

set -e  # Arrête le script en cas d'erreur

# Configuration
REMOTE_HOST="poupou"
REMOTE_PORT="5722"
REMOTE_USER="pi"
REMOTE_PATH="/home/pi"
SERVICE_NAME="Hermanas.service"

# Couleurs pour les messages
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Déploiement de Hermanas ===${NC}"

# Étape 1: Build Maven
echo -e "${YELLOW}[1/5] Compilation avec Maven...${NC}"
mvn clean package -DskipTests

# Étape 2: Récupération de la version
echo -e "${YELLOW}[2/5] Détection de la version...${NC}"
JAR_FILE=$(ls target/hermanas-*.jar | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Erreur: Aucun fichier JAR trouvé dans target/${NC}"
    exit 1
fi

VERSION=$(basename "$JAR_FILE")
echo -e "${GREEN}Fichier détecté: $VERSION${NC}"

# Étape 3: Transfert du fichier
echo -e "${YELLOW}[3/5] Transfert du fichier vers le serveur...${NC}"
scp -P "$REMOTE_PORT" "$JAR_FILE" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/"

# Étape 4: Arrêt du service, création du lien symbolique et redémarrage
echo -e "${YELLOW}[4/5] Arrêt du service...${NC}"
ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" "sudo systemctl stop $SERVICE_NAME"

echo -e "${YELLOW}[5/5] Mise à jour du lien symbolique et redémarrage...${NC}"
ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" << EOF
    cd ${REMOTE_PATH}
    sudo rm -f hermanas.jar
    sudo ln -s ${VERSION} hermanas.jar
    sudo systemctl start ${SERVICE_NAME}
    echo "Attente du démarrage du service..."
    sleep 2
    sudo systemctl status ${SERVICE_NAME} --no-pager
EOF

echo -e "${GREEN}=== Déploiement terminé avec succès! ===${NC}"
echo -e "${GREEN}Version déployée: ${VERSION}${NC}"
