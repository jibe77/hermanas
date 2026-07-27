#!/bin/bash

# Script de déploiement automatique pour Hermanas
#
# Usage:
#   ./deploy.sh              # déploie sur pru (Pi Zero 2 W, cible par défaut)
#   ./deploy.sh poupou       # déploie sur poupou (ancien Pi Zero, rollback)
#   ./deploy.sh --skip-build # réutilise le JAR déjà présent dans target/
#
# Le JAR transite par /tmp car /var/lib/hermanas/ appartient à hermanas:hermanas
# en 750 : l'utilisateur SSH ne peut pas y écrire directement.

set -euo pipefail

# ─── Configuration par cible ────────────────────────────────────────────────
TARGET="${1:-pru}"
SKIP_BUILD=false
for arg in "$@"; do
    [ "$arg" = "--skip-build" ] && SKIP_BUILD=true
done

case "$TARGET" in
    pru|--skip-build)
        TARGET="pru"
        REMOTE_HOST="pru.local"
        REMOTE_USER="jean-baptisterenaux"
        REMOTE_PATH="/var/lib/hermanas"
        SERVICE_USER="hermanas"
        ;;
    poupou)
        REMOTE_HOST="poupou"
        REMOTE_USER="pi"
        REMOTE_PATH="/home/pi"
        SERVICE_USER="root"
        ;;
    *)
        echo "Cible inconnue : $TARGET (attendu : pru ou poupou)" >&2
        exit 1
        ;;
esac

REMOTE_PORT="5722"
SERVICE_NAME="Hermanas.service"

# Couleurs pour les messages
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Déploiement de Hermanas sur ${TARGET} ===${NC}"
echo -e "${GREEN}    ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH} (service sous ${SERVICE_USER})${NC}"

# ─── Étape 1: Build Maven ───────────────────────────────────────────────────
if [ "$SKIP_BUILD" = true ]; then
    echo -e "${YELLOW}[1/6] Build ignoré (--skip-build)${NC}"
else
    echo -e "${YELLOW}[1/6] Compilation avec Maven...${NC}"
    mvn clean package -DskipTests
fi

# ─── Étape 2: Récupération de la version ────────────────────────────────────
echo -e "${YELLOW}[2/6] Détection de la version...${NC}"
JAR_FILE=$(ls target/hermanas-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Erreur: Aucun fichier JAR trouvé dans target/${NC}"
    exit 1
fi

VERSION=$(basename "$JAR_FILE")
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "${GREEN}Fichier détecté: $VERSION ($JAR_SIZE)${NC}"

# ─── Étape 3: Transfert vers /tmp ───────────────────────────────────────────
echo -e "${YELLOW}[3/6] Transfert du fichier vers ${REMOTE_HOST}...${NC}"
scp -P "$REMOTE_PORT" "$JAR_FILE" "${REMOTE_USER}@${REMOTE_HOST}:/tmp/"

# Contrôle de taille : un transfert interrompu passerait sinon inaperçu et le
# service échouerait au démarrage sur un JAR tronqué.
LOCAL_BYTES=$(wc -c < "$JAR_FILE" | tr -d ' ')
REMOTE_BYTES=$(ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
    "stat -c %s /tmp/${VERSION} 2>/dev/null || echo 0")
if [ "$LOCAL_BYTES" != "$REMOTE_BYTES" ]; then
    echo -e "${RED}Erreur: transfert incomplet (${REMOTE_BYTES} octets reçus sur ${LOCAL_BYTES}).${NC}"
    exit 1
fi
echo -e "${GREEN}Transfert vérifié: ${REMOTE_BYTES} octets.${NC}"

# Les étapes suivantes appellent sudo sur la machine distante. Il faut donc un
# TTY (ssh -t) pour que la demande de mot de passe s'affiche dans ce terminal.
#
# ⚠️ ssh -t et un heredoc sont incompatibles : le heredoc occupe stdin, que sudo
# utilise pour lire le mot de passe — la demande partirait dans le vide et le
# script resterait bloqué. Les commandes sont donc passées en argument.
#
# Une seule session SSH pour les étapes 4 à 6 : sudo met son authentification en
# cache quelques minutes, le mot de passe n'est demandé qu'une fois.

echo -e "${YELLOW}[4-6/6] Arrêt, installation et redémarrage du service...${NC}"
echo -e "${YELLOW}      (le mot de passe sudo de ${REMOTE_HOST} peut être demandé)${NC}"

ssh -t -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" "
set -e
echo '--- [4/6] Arrêt du service'
sudo systemctl stop ${SERVICE_NAME} || true

echo '--- [5/6] Installation du JAR et du lien symbolique'
sudo mv /tmp/${VERSION} ${REMOTE_PATH}/
sudo chown ${SERVICE_USER}:${SERVICE_USER} ${REMOTE_PATH}/${VERSION}
sudo chmod 640 ${REMOTE_PATH}/${VERSION}
sudo ln -sfn ${REMOTE_PATH}/${VERSION} ${REMOTE_PATH}/hermanas.jar
sudo chown -h ${SERVICE_USER}:${SERVICE_USER} ${REMOTE_PATH}/hermanas.jar

echo '--- [6/6] Redémarrage du service'
sudo systemctl start ${SERVICE_NAME}
echo 'Attente du démarrage...'
sleep 5
sudo systemctl status ${SERVICE_NAME} --no-pager || true
"

echo -e "${GREEN}=== Déploiement terminé ! ===${NC}"
echo -e "${GREEN}Version déployée: ${VERSION} sur ${TARGET}${NC}"
echo -e "${YELLOW}Suivre les logs : ssh -p ${REMOTE_PORT} ${REMOTE_USER}@${REMOTE_HOST} 'sudo journalctl -u ${SERVICE_NAME} -f'${NC}"
