# Migration `poupou` (Pi Zero) → `pru` (Pi Zero 2 W)

Checklist opérationnelle. Stratégie all-in-one : hardware + OS 64 bits (Trixie arm64) + **Java 25** + Spring Boot 3 + Jakarta EE + **pi4j 4.x avec FFM** dans une seule bascule. Camera + player audio traités en dernier.

**Décision GPIO stack (2026-07-04)** : passage direct à `pi4j-plugin-ffm` (Foreign Function & Memory API de Java 22+, requiert Java 25 LTS). Élimine la dette technique `pigpio` (archivé 2021, retiré de Trixie) **et** la dette technique intermédiaire `libgpiod`/`gpiod` (déprécié dans pi4j 4.1). Voie officielle définitive de pi4j.

**Downtime coop attendu : ~1 h**, à planifier en milieu de journée porte ouverte.

**Alias réseau conservé** : `poupou` (côté `shannen`) = IP fixe `192.168.1.35`, restera valide après bascule DHCP. Hostname interne de la nouvelle machine = `pru` (marque la différence avec l'ancien `poulailler`).

---

## 📚 Sommaire

- [Phase 0 — Préparation `pru` headless (hors ligne)](#phase-0--préparation-pru-headless-hors-ligne)
- [Phase 1 — Migration code Java 25 / Spring Boot 3 / Jakarta EE / pi4j 4.x](#phase-1--migration-code-java-25--spring-boot-3--jakarta-ee--pi4j-4x)
- [Phase 2 — Bascule des données et fichiers](#phase-2--bascule-des-données-et-fichiers)
- [Phase 3 — Test à vide sur banc](#phase-3--test-à-vide-sur-banc)
- [Phase 4 — Bascule réseau](#phase-4--bascule-réseau)
- [Phase 5 — Bascule hardware sur le coop](#phase-5--bascule-hardware-sur-le-coop)
- [Phase 6 — Mise à jour des dépendances (Maven + npm)](#phase-6--mise-à-jour-des-dépendances-maven--npm)
- [Phase 7 — Chantier camera + player audio](#phase-7--chantier-camera--player-audio)
- [Phase 8 — Rollback plan](#phase-8--rollback-plan)

---

## Phase 0 — Préparation `pru` headless (hors ligne)

Objectif : SD prête, `pru` bootable en Wi-Fi, accessible en SSH sur port 5722, sans jamais brancher d'écran ni de clavier.

### 0.1 — Flashage headless de la SD

- [x] Ouvrir `rpi-imager` sur le Mac.
- [x] OS : **Raspberry Pi OS (64-bit) Lite** (Trixie arm64 via `rpi-imager` récent, pas la Desktop).
- [x] Cliquer ⚙️ (OS customization) et renseigner :
  - Hostname : `pru`
  - Enable SSH : password ou clés publiques
  - Username : `pi` + mot de passe fort
  - Wireless LAN : SSID + WPA2 + country FR
  - Locale : `Europe/Paris`, clavier `fr`
- [x] Flasher, éjecter, insérer dans `pru`, brancher l'alim.
- [x] Attendre 2-3 min (premier boot + resize + Wi-Fi).
- [x] Depuis le Mac : `ping pru.local` (mDNS), noter l'IP.
- [x] Premier SSH : `ssh jean-baptisterenaux@pru.local`.

### 0.2 — SSH sur port 5722

```bash
sudo sed -i 's/^#\?Port .*/Port 5722/' /etc/ssh/sshd_config
sudo systemctl restart ssh
```

- [x] Retester depuis le Mac : `ssh -p 5722 jean-baptisterenaux@pru.local`.
- [x] **À partir d'ici, toujours utiliser `-p 5722`.**

### 0.2bis — Clé SSH publique du Mac pour login sans mot de passe

Objectif : plus de mot de passe à taper, ni interactivement ni dans `deploy.sh`.

Sur le Mac, vérifier qu'une clé existe déjà :

```bash
ls -la ~/.ssh/id_ed25519.pub ~/.ssh/id_rsa.pub 2>/dev/null
```

- [x] Si aucune clé existante, en générer une (ed25519, moderne et court) :
  ```bash
  ssh-keygen -t ed25519 -C "jean-baptisterenaux@mbp"
  # Enter enter enter (accepter les défauts, passphrase à ta discrétion)
  ```

Copier la clé publique sur `pru` :

```bash
ssh-copy-id -p 5722 -i ~/.ssh/id_ed25519.pub jean-baptisterenaux@pru.local
# (dernière fois qu'on tape le mot de passe)
```

- [x] Test : `ssh -p 5722 jean-baptisterenaux@pru.local` → connexion sans prompt de mot de passe.
- [x] Vérifier sur `pru` que le contenu est correct :
  ```bash
  cat ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  chmod 700 ~/.ssh
  ```

**Optionnel — durcir SSH une fois la clé validée** (dans `/etc/ssh/sshd_config`) :
```
PasswordAuthentication no
PermitRootLogin no
```
Puis `sudo systemctl restart ssh`. À ne faire qu'après avoir **testé** la connexion par clé depuis le Mac, sinon tu te verrouilles dehors.

### 0.3 — Récupération des scripts custom depuis `poupou`

Transfert **direct `poupou` → `pru`** (pull depuis `pru`, pas de détour par le Mac). Les deux machines doivent être branchées et joignables sur le LAN pendant cette étape. `poupou` écoute sur le port SSH `5722` (cf. étapes ultérieures 0.5/2.1/2.2 qui utilisent le même port).

Sur `pru` (en tant que `jean-baptisterenaux`) :

```bash
# Wrapper de démarrage Hermanas (pour référence — sera remplacé par la version adaptée ci-dessous)
scp -P 5722 pi@poupou:/usr/local/bin/Hermanas.sh /tmp/Hermanas.sh.old

# Unit systemd (le sudo distant expose le contenu que systemctl cat lit dans /etc/systemd/system/)
ssh -p 5722 pi@poupou "sudo systemctl cat Hermanas.service" > /tmp/Hermanas.service

# Scripts USB
scp -P 5722 pi@poupou:/home/pi/usb_on.sh /tmp/
scp -P 5722 pi@poupou:/home/pi/usb_off.sh /tmp/
scp -P 5722 pi@poupou:/home/pi/usb_sleep_10.sh /tmp/
```

> ⚠️ Ces fichiers restent dans `/tmp/` sur `pru` pour le moment — ils seront déplacés à leur place définitive lors des étapes suivantes (`Hermanas.sh` en 0.6, scripts USB en 0.8), une fois le user `hermanas` et l'arborescence `/var/lib/hermanas/` créés (0.4bis).

**Nouvelle version de `Hermanas.sh` pour `pru`** (déjà adaptée Java 25 / SB3 / pi4j 4.x FFM / Zero 2 W 512 Mo — voir tableau des changements sous le script) :

```sh
#!/bin/sh
SERVICE_NAME=Hermanas
PATH_TO_JAR=/var/lib/hermanas/hermanas.jar
PID_PATH_NAME=/run/hermanas/hermanas.pid
CONFIG_LOCATION=file:/var/lib/hermanas/application.properties

# Chemin absolu vers Java 25 (openjdk-25-jdk apt) — vérifier avec `readlink -f $(which java)`
JAVA_BIN=/usr/lib/jvm/java-25-openjdk-arm64/bin/java

# --enable-native-access=ALL-UNNAMED : requis pour pi4j-plugin-ffm (FFM API restricted)
JVM_OPTS="-Xmx256m -Xms128m \
    -XX:+UseSerialGC \
    -Djava.net.preferIPv4Stack=true \
    --enable-native-access=ALL-UNNAMED"

# JMX localhost uniquement — accès via SSH tunnel :
# ssh -p 5722 -L 9010:localhost:9010 pi@poupou
JMX_OPTS="-Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.local.only=true \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=127.0.0.1"

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup $JAVA_BIN $JVM_OPTS $JMX_OPTS \
                -jar $PATH_TO_JAR \
                --spring.config.location=$CONFIG_LOCATION \
                & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    debug)
        echo "Starting $SERVICE_NAME (debug) ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup $JAVA_BIN $JVM_OPTS $JMX_OPTS \
                -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
                -jar $PATH_TO_JAR \
                --spring.config.location=$CONFIG_LOCATION \
                & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started (debug) ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME)
            echo "$SERVICE_NAME stopping ..."
            kill $PID
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac
```

**Changements par rapport à l'ancien script sur `poupou`** :

| Ancien | Nouveau | Pourquoi |
|---|---|---|
| `java` (via PATH) | `$JAVA_BIN` (chemin absolu apt vers Java 25) | Fiabilité au démarrage systemd (le PATH de root peut ne pas être ce que tu crois selon la session) |
| Pas de `-Xmx` | `-Xmx256m -Xms128m` | Plafonner la heap : SB3 est plus lourd que SB2, la RAM Zero 2 W reste à 512 Mo |
| Pas de GC choisi | `-XX:+UseSerialGC` | GC single-thread, économe en mémoire, adapté aux petits heaps sur ARM |
| Pas de FFM autorisation | `--enable-native-access=ALL-UNNAMED` | Requis pour `pi4j-plugin-ffm` — sans ça, warning restricted method à chaque appel GPIO |
| `java.rmi.server.hostname=10.0.0.20` | `=127.0.0.1` | L'IP `10.0.0.20` était un vestige non fonctionnel (LAN Freebox = 192.168.1.0/24) |
| `local.only=false` | `local.only=true` | JMX exposé sans auth ni TLS = trou de sécu. Désormais accessible via SSH tunnel uniquement |
| Pas de `--spring.config.location` | Explicit `file:/var/lib/hermanas/application.properties` | Ne dépend plus du CWD au moment du démarrage |
| `-Dcom.sun.management.jmxremote=true` (en `debug`) | Retiré | Redondant, l'activation JMX est déjà impliquée par la présence du port |

- [x] Créer le script sur `pru` avec ce contenu, puis :
  ```bash
  sudo chmod +x /usr/local/bin/Hermanas.sh
  ```
- [x] Adapter `Hermanas.service` : garder l'`ExecStart=/usr/local/bin/Hermanas.sh start` s'il était déjà comme ça sur `poupou`. Vérifier qu'il n'y a pas d'`Environment=JAVA_HOME=...` hardcodé qui pointerait vers l'ancien Java 11.

### 0.4 — Runtime : Java 25 + MariaDB + outils

**OpenJDK via apt** — voie simple, patches sécurité auto via `unattended-upgrades`, chemin stable pour systemd. FFM API requiert Java 22+, on vise le LTS courant (Java 25) mais on prend ce que Trixie propose de plus haut.

Vérifier ce qui est dispo :
```bash
apt-cache policy openjdk-25-jdk 2>/dev/null
apt-cache policy openjdk-24-jdk 2>/dev/null
apt-cache policy openjdk-23-jdk 2>/dev/null
```

Installer la version la plus haute dispo (25 si présent, sinon 24, sinon 23, minimum 22 pour FFM) :
```bash
sudo apt install -y openjdk-25-jdk    # ou -24-jdk / -23-jdk selon disponibilité
java -version                          # doit afficher la version installée
readlink -f $(which java)              # noter le chemin réel — sera JAVA_BIN dans Hermanas.sh
```

Attendu : chemin type `/usr/lib/jvm/java-25-openjdk-arm64/bin/java`. À reporter dans `Hermanas.sh` (variable `JAVA_BIN`).

Puis les paquets système (avec pi4j 4.x + FFM, **pas besoin de pigpio ni de libgpiod côté userspace** — FFM parle directement au kernel via chardev `/dev/gpiochip0`) :

```bash
sudo apt update
sudo apt install -y \
  mariadb-server \
  iw \
  git curl
```

Vérifications post-install :
- [x] `ls /dev/gpiochip*` doit lister au moins `gpiochip0`. C'est le device kernel qu'utilise FFM. Fourni par le kernel Linux depuis 4.8, présent par défaut sur Trixie.
- [x] Vérifier les permissions : `ls -la /dev/gpiochip0` doit afficher `crw-rw---- root gpio`. Le mode `rw` pour le groupe `gpio` permet aux users de ce groupe de piloter les GPIO sans être root.
- [x] Vérifier `mysql --version` : noter la version (attendu ~11.x sur Trixie) et comparer avec `poupou` (10.3).
- [x] Augmenter le swap à 512 Mo (Java 25 + MariaDB + Hermanas sur 512 Mo RAM) :
  ```bash
  sudo apt update                                                                                                                         
  sudo apt install -y dphys-swapfile                                                                                                      
  sudo sed -i 's/^#\?CONF_SWAPSIZE=.*/CONF_SWAPSIZE=512/' /etc/dphys-swapfile
  sudo dphys-swapfile setup                                                                                                               
  sudo dphys-swapfile swapon                                                                                                            
  ```
  Vérifie :                                                                                                                               
  free -h                                                                                                                               
  swapon --show
  

### 0.4bis — Créer le user système `hermanas`

Objectif : faire tourner l'application sous un user dédié non-root, cohérent avec le passage à FFM + chardev (plus besoin de `/dev/mem` root-only). Élimine aussi le hack VLC-en-root de `poupou`.

```bash
sudo useradd -r -s /usr/sbin/nologin -c "Hermanas service user" hermanas
sudo usermod -aG gpio  hermanas     # accès à /dev/gpiochip*
sudo usermod -aG audio hermanas     # accès à /dev/snd/* pour cvlc/amixer
sudo usermod -aG dialout hermanas   # accès aux périphériques série éventuels (utile pour DHT22 via UART si besoin un jour)
```

Vérifier :
```bash
id hermanas
# attendu : uid=... groups=...(hermanas),20(dialout),29(audio),997(gpio) (numéros indicatifs)
```

Créer le répertoire de travail applicatif (plus propre que `/home/jean-baptisterenaux/`) :

```bash
sudo mkdir -p /var/lib/hermanas
sudo chown hermanas:hermanas /var/lib/hermanas
sudo chmod 750 /var/lib/hermanas

# Sous-dossiers pour photos, music, residents-photos (migration Phase 2)
sudo -u hermanas mkdir -p /var/lib/hermanas/{photos,music,residents-photos,log}
```

**Note** : les fichiers migrés depuis `poupou` (Phase 2) atterriront désormais dans `/var/lib/hermanas/` au lieu de `/home/jean-baptisterenaux/`. Les chemins dans `application.properties` (`camera.path.root`, `music.path.*`, etc.) sont à ajuster en conséquence lors de la copie du fichier.

### 0.5 — Coquille MariaDB vide

Récupérer les creds JDBC de `poupou` :

```bash
ssh -p 5722 pi@poupou "grep -E 'datasource\.(url|username|password)' /home/pi/application.properties"
```

Créer la base + user sur `pru` (avec les mêmes creds) :

```bash
sudo mysql << EOF
CREATE DATABASE hermanas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER '<user>'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON hermanas.* TO '<user>'@'localhost';
FLUSH PRIVILEGES;
EOF
```

- [x] Test rapide de connexion : `mysql -u <user> -p<password> -e "SELECT 1;"`.

### 0.6 — Wrapper Hermanas.sh + unit systemd (user `hermanas`, pas root)

> ✅ **Hermanas tourne sous le user système `hermanas`** (non-root) créé en Phase 0.4bis. Rendu possible par le passage à **pi4j-plugin-ffm** qui accède au GPIO via chardev `/dev/gpiochip0` — permission `crw-rw---- root:gpio`, donc n'importe quel membre du groupe `gpio` y accède sans root. Le user `hermanas` est aussi membre du groupe `audio` (pour `cvlc`/`amixer`) et `dialout` (série). **Bonus** : le hack VLC-en-root (crontab quotidienne `sed 's/geteuid/getppid/g' /usr/bin/vlc`) devient inutile — VLC refuse root mais accepte n'importe quel autre user.

- [x] Copier le wrapper adapté :
  ```bash
  sudo cp /tmp/Hermanas.sh /usr/local/bin/Hermanas.sh
  sudo chmod +x /usr/local/bin/Hermanas.sh
  ```

- [x] Créer le répertoire de PID `run` avec le bon owner (systemd le nettoie au reboot, mais on le crée d'abord) :
  ```bash
  sudo mkdir -p /run/hermanas
  sudo chown hermanas:hermanas /run/hermanas
  sudo chmod 755 /run/hermanas
  ```

**Version du unit `Hermanas.service` pour `pru`** (user dédié, RuntimeDirectory géré par systemd, hardening capabilities) :

```ini
[Unit]
Description=Hermanas Service
After=network.target mariadb.service
Requires=mariadb.service

[Service]
Type=forking
User=hermanas
Group=hermanas
SupplementaryGroups=gpio audio dialout

# systemd crée /run/hermanas automatiquement à chaque démarrage,
# avec les bons owner/group, et le supprime à l'arrêt.
RuntimeDirectory=hermanas
RuntimeDirectoryMode=0755

WorkingDirectory=/var/lib/hermanas
PIDFile=/run/hermanas/hermanas.pid

ExecStart=/usr/local/bin/Hermanas.sh start
ExecStop=/usr/local/bin/Hermanas.sh stop
Restart=on-failure
RestartSec=10

# Hardening — le user n'a besoin ni de home, ni d'accès étendu au FS
ProtectHome=yes
ProtectSystem=strict
ReadWritePaths=/var/lib/hermanas /run/hermanas
NoNewPrivileges=yes
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
```

- [x] Installer le unit :
  ```bash
  sudo tee /etc/systemd/system/Hermanas.service > /dev/null << 'EOF'
  # ... coller le contenu ci-dessus ...
  EOF
  sudo chmod 644 /etc/systemd/system/Hermanas.service
  sudo chown root:root /etc/systemd/system/Hermanas.service
  sudo systemctl daemon-reload
  sudo systemctl enable Hermanas.service
  ```
- [x] Vérifier la syntaxe : `sudo systemd-analyze verify /etc/systemd/system/Hermanas.service` (attendu : aucune sortie).
- [x] Vérifier que `Hermanas.service` cible bien le user `hermanas` :
  ```bash
  sudo systemctl show Hermanas.service | grep -E "^(User|Group|SupplementaryGroups)="
  # attendu : User=hermanas, Group=hermanas, SupplementaryGroups=gpio audio dialout
  ```
- [x] **Ne pas démarrer** le service tout de suite (le JAR et `application.properties` sont installés en Phase 2/3).

**À propos du hardening** :
- `ProtectHome=yes` : Hermanas ne voit pas `/home/*`. Cohérent : tout est dans `/var/lib/hermanas/`.
- `ProtectSystem=strict` : `/`, `/usr`, `/etc`, `/boot` en lecture seule pour le process. Empêche toute modification système.
- `ReadWritePaths=` : les seules zones où Hermanas peut écrire. Si Hermanas écrit ailleurs (log dans `/var/log/*`, etc.), à ajouter ici.
- `NoNewPrivileges=yes` : Hermanas ne peut pas escalader via `sudo`/`setuid`.
- `PrivateTmp=yes` : `/tmp` isolé (Hermanas voit son propre `/tmp` privé, pas celui du système).

Si un endpoint échoue au premier démarrage à cause de `ProtectSystem=strict` (ex. besoin d'écrire dans `/var/log/`), regarder le journal et ajouter le chemin manquant à `ReadWritePaths=`.

### 0.7 — Monitoring : prometheus-node-exporter

```bash
sudo apt install -y prometheus-node-exporter
sudo systemctl enable --now prometheus-node-exporter
```

- [x] Vérifier : `curl -s http://localhost:9100/metrics | head -5`.
- [x] Récupérer la config custom de `poupou` si elle existe :
  ```bash
  scp -3 -P 5722 pi@poupou:/etc/default/prometheus-node-exporter /tmp/ne.poupou
  diff /tmp/ne.poupou /etc/default/prometheus-node-exporter
  # Si différences intentionnelles : sudo cp /tmp/ne.poupou /etc/default/prometheus-node-exporter && sudo systemctl restart prometheus-node-exporter
  ```

### 0.8 — Scripts USB

Les scripts `usb_*.sh` ont été récupérés dans `/tmp/` sur `pru` en 0.3. Ils utilisent le sysfs `buspower` pour couper/allumer l'alim USB globale — mécanisme spécifique au chipset Broadcom des Pi.

**⚠️ Adaptation nécessaire : adresse du contrôleur USB différente sur Pi Zero 2 W.**

Sur `poupou` (Pi Zero, BCM2835), l'adresse était `20980000.usb`. Sur `pru` (Pi Zero 2 W, BCM2710), c'est `3f980000.usb` (ou proche). Vérifier :

```bash
ls /sys/devices/platform/soc/ | grep usb
# attendu : 3f980000.usb (ou similaire)
```

Contenu original des 3 scripts (récupérés de `poupou`) :

```bash
# usb_on.sh
#!/bin/bash
echo 1 | sudo tee /sys/devices/platform/soc/20980000.usb/buspower > /dev/null

# usb_off.sh
#!/bin/bash
echo 0 | sudo tee /sys/devices/platform/soc/20980000.usb/buspower > /dev/null

# usb_sleep_10.sh
#!/bin/bash
echo 0 | sudo tee /sys/devices/platform/soc/20980000.usb/buspower > /dev/null
sleep 10
echo 1 | sudo tee /sys/devices/platform/soc/20980000.usb/buspower > /dev/null
```

**Remplacer l'adresse dans les 3 scripts en une commande, puis les installer dans `/usr/local/bin/`** (emplacement standard pour les scripts admin, appelés par la crontab root en 0.9) :

```bash
# Remplacer 20980000 → 3f980000 dans les 3 fichiers (adapter si ls a montré une autre adresse)
sed -i 's|20980000\.usb|3f980000.usb|g' /tmp/usb_on.sh /tmp/usb_off.sh /tmp/usb_sleep_10.sh

# Vérifier
grep buspower /tmp/usb_*.sh

# Installer
sudo mv /tmp/usb_on.sh /tmp/usb_off.sh /tmp/usb_sleep_10.sh /usr/local/bin/
sudo chown root:root /usr/local/bin/usb_*.sh
sudo chmod 755 /usr/local/bin/usb_*.sh
```

**Note sur le `sudo` interne** : les scripts contiennent `sudo tee ...` — inutile puisqu'ils sont eux-mêmes appelés en root (via crontab root ou `sudo`). On peut le laisser (ça marche), ou nettoyer :

```bash
sudo sed -i 's|| sudo tee || tee |g' /usr/local/bin/usb_*.sh
```

- [x] Test manuel : `sudo /usr/local/bin/usb_on.sh` — pas d'erreur, pas de sortie.
- [x] Vérifier l'effet : `cat /sys/devices/platform/soc/3f980000.usb/buspower` doit afficher `1`.
- [x] Test extinction : `sudo /usr/local/bin/usb_off.sh` puis re-cat → `0`.
- [x] Rallumer avant de continuer : `sudo /usr/local/bin/usb_on.sh`.

> ⚠️ **Cohérence avec 0.9** : la crontab root (0.9) référence désormais `/usr/local/bin/usb_on.sh` (déjà à jour).

### 0.9 — Crontab root (reproduite depuis `poupou`)

```bash
sudo crontab -e
```

Contenu (adaptations Trixie : `iwconfig` → `iw`, sans la ligne tzupdater qui devient obsolète avec Java 25) :

```cron
# active le port USB au démarrage
@reboot /usr/local/bin/usb_on.sh

# active la carte wifi au démarrage
@reboot /usr/sbin/rfkill unblock 0 && /usr/sbin/iw dev wlan0 set txpower auto && /bin/sleep 5 && /usr/bin/timedatectl

# redémarrage tous les mercredis à 16h05
00 16   *   *   3    /sbin/shutdown -r +5

# nettoyer des paquets d'installation temporaires
00 15   *   *   3    /usr/bin/apt-get clean

# NOTE : le hack VLC-en-root (`sed 's/geteuid/getppid/g' /usr/bin/vlc`) qui tournait
# quotidiennement sur poupou est retiré. Hermanas tourne désormais sous le user `hermanas`
# (Phase 0.4bis), pas root — VLC accepte sans problème un user standard.
```

### 0.10 — Économies d'énergie

Éditer `/boot/firmware/config.txt` :

```bash
sudo tee -a /boot/firmware/config.txt << 'EOF'

# === Économies d'énergie Hermanas ===
dtoverlay=disable-bt

dtparam=act_led_trigger=none
dtparam=act_led_activelow=off
dtparam=pwr_led_trigger=none
dtparam=pwr_led_activelow=off

hdmi_blanking=2

gpu_freq=250

arm_freq=600
EOF

sudo systemctl disable hciuart bluetooth
```

- [x] **NE PAS** ajouter `maxcpus=1` (les 4 cœurs sont conservés).
- [x] **NE PAS** ajouter `disable-wifi`.

### 0.11 — Nettoyage services inutiles

```bash
# Services legacy Buster non pertinents sur Bookworm
sudo systemctl disable dhcpcd5 || true
sudo systemctl disable rsync || true
sudo systemctl disable sshswitch || true
sudo systemctl disable triggerhappy || true
sudo systemctl disable rpi-display-backlight || true
sudo systemctl disable smartd smartmontools || true
```

- [x] **Conserver** : `fake-hwclock`, `unattended-upgrades`, `dphys-swapfile`.

### 0.11bis — Configuration `unattended-upgrades` (mises à jour complètes)

Par défaut, `unattended-upgrades` sur Debian n'applique que les **patches de sécurité**. Sur `pru`, on veut aussi les **updates normales** (bug fixes, versions mineures) pour éviter de laisser dériver les paquets. Décision utilisateur 2026-07-25 : Option A (étendre `unattended-upgrades`) plutôt qu'un cron `apt-get upgrade` manuel — plus propre, gère les locks apt et les prompts dpkg correctement.

- [x] Installer le paquet (⚠️ pas fourni par défaut sur Trixie Lite, contrairement à ce que suggère la ligne "conserver" en 0.11) :
  ```bash
  sudo apt install -y unattended-upgrades
  ```

- [x] Vérifier que le service est enabled après install :
  ```bash
  systemctl is-enabled unattended-upgrades
  # attendu : enabled
  ```

- [x] Étendre `unattended-upgrades` aux updates normales :
  ```bash
  sudo sed -i 's|^//\(\s*"origin=Debian,codename=\${distro_codename}-updates";\)|\1|' /etc/apt/apt.conf.d/50unattended-upgrades
  ```

- [x] Ajouter la config Hermanas (reboot auto, cleanup, périodicité). ⚠️ **Le marqueur `EOF` final doit être collé à gauche (colonne 0)**, sinon bash reste bloqué au prompt `>` en attendant la fin du heredoc :

```bash
sudo tee /etc/apt/apt.conf.d/51hermanas-upgrades > /dev/null << 'EOF'
Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-Time "04:00";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::AutocleanInterval "7";
EOF
```

- [x] Test à blanc (aucun changement appliqué) :
  ```bash
  sudo unattended-upgrades --dry-run --debug 2>&1 | tail -30
  ```
  Attendu : lignes `Checking...` sans erreur, et éventuellement des paquets listés comme "would be upgraded".

**Notes :**
- Reboot à 04:00 : les poules dorment, pas de porte à ouvrir/fermer. Aligné avec le reboot hebdo du mercredi 16h (crontab root, cf. 0.9) — celui-ci reste maître pour les kernels qui exigent un reboot.
- `Remove-Unused-Dependencies "true"` : équivalent d'un `apt autoremove` automatique.
- Les logs atterrissent dans `/var/log/unattended-upgrades/`.

### 0.12 — Alias shell pour le user `pi`

Reproduction des alias utiles de `poupou`, moins ceux qui pointent vers des scripts Python legacy (`servo_door_down.py`, `servo_door_up.py`) qu'on ne migre pas.

```bash
cat >> ~/.bashrc << 'EOF'

# === Alias Hermanas ===
alias restart='sudo systemctl restart Hermanas.service'
alias start='sudo systemctl start Hermanas.service'
alias stop='sudo systemctl stop Hermanas.service'
alias log='sudo journalctl -u Hermanas.service -f'

# Couleurs et confort
alias ls='ls --color=auto'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'
EOF

source ~/.bashrc
```

- [x] Test rapide : `alias | grep Hermanas` doit lister les 4 alias service.
- [x] `log` remplace le `tail -f /var/log/syslog` d'origine par `journalctl -u Hermanas.service -f` — plus ciblé, plus utile en debug (sur Bookworm systemd-journald ne remplit plus `/var/log/syslog` par défaut de toute façon).
- [x] `up`/`down` (scripts Python `servo_door_*.py`) : **non reproduits**, ces scripts appartiennent au bloat de `poupou`. Les endpoints REST `/api/v1/door/open` et `/api/v1/door/close` remplissent le même rôle.

### 0.13 — Reboot final + sanity check

```bash
sudo reboot
```

Après ~1 min, depuis le Mac :

```bash
ping pru.local
ssh -p 5722 jean-baptisterenaux@pru.local
```

Sur `pru` :

- [x] `vcgencmd measure_clock arm` → `600000000`.
- [x] `vcgencmd get_config int | grep -E "arm_freq|gpu_freq"` → 600 et 250.
- [x] `rfkill list wifi` → not blocked.
- [x] `iw dev wlan0 info` → txpower affiché.
- [x] `sudo crontab -l` → contenu attendu.
- [x] `systemctl is-enabled Hermanas.service prometheus-node-exporter mariadb` → tous enabled (pas de `pigpiod` : la lib est embarquée dans la JVM).
- [x] `java -version` → Java 25.
- [x] `mysql --version` → 10.11.x.

**Fin de Phase 0** : `pru` prête, bootable, headless, économe, en attente. Peut rester éteinte jusqu'à la Phase 2.

---

## Phase 1 — Migration code Java 25 / Spring Boot 3 / Jakarta EE / pi4j 4.x

Objectif : le repo compile et passe les tests avec Java 25 + Spring Boot 3 + Jakarta EE + pi4j 4.x FFM. Sur le Mac uniquement. Branche dédiée. Aucun impact sur `poupou` ni `pru`.

### 1.1 — Branche + snapshot

```bash
git checkout -b feat/pi-zero-2-migration
git status    # doit être clean sauf éventuels WIP à commit avant
```

### 1.2 — `pom.xml` : bumps de version

- [ ] `<java.version>11</java.version>` → `<java.version>25</java.version>`.
- [ ] `<maven.compiler.source>` et `<maven.compiler.target>` → `25` si présents.
- [ ] `spring-boot-starter-parent` : `2.7.x` → dernière `3.5.x+` compatible Java 25 (SB 3.4+ recommandé, vérifier Maven Central).
- [ ] `<pi4j.version>2.4.0</pi4j.version>` → `4.0.0` (ou dernière 4.x stable).
- [ ] **Retirer** les artefacts `pi4j-plugin-raspberrypi` et `pi4j-plugin-pigpio` du bloc `<dependencies>`.
- [ ] **Ajouter** la nouvelle dépendance :
  ```xml
  <dependency>
      <groupId>com.pi4j</groupId>
      <artifactId>pi4j-plugin-ffm</artifactId>
      <version>${pi4j.version}</version>
  </dependency>
  ```
- [ ] Laisser `<picam.version>` inchangé (retiré en Phase 7).
- [ ] `mvn versions:display-dependency-updates` pour voir les autres bumps.

### 1.3 — Refactor `javax.*` → `jakarta.*` (~70 imports)

```bash
find src -name "*.java" -exec sed -i '' \
  -e 's/javax\.persistence\./jakarta.persistence./g' \
  -e 's/javax\.annotation\.PostConstruct/jakarta.annotation.PostConstruct/g' \
  -e 's/javax\.annotation\.PreDestroy/jakarta.annotation.PreDestroy/g' \
  -e 's/javax\.servlet\./jakarta.servlet./g' \
  -e 's/javax\.validation\./jakarta.validation./g' \
  -e 's/javax\.mail\./jakarta.mail./g' \
  {} \;
```

- [ ] **NE PAS toucher** `javax.imageio.*` (JDK standard, pas Jakarta).
- [ ] Vérifier avec `grep -rn "javax\." src/main/java` que seul `javax.imageio` reste.

### 1.4 — SecurityConfig

- [ ] `@EnableGlobalMethodSecurity(prePostEnabled = true)` → `@EnableMethodSecurity`.
- [ ] Vérifier que `SecurityFilterChain` (déjà en place) compile avec Spring Security 6.

### 1.5 — Autres points SB 2 → 3

- [ ] Properties `spring.*` : lire les warnings au premier `mvn compile`.
- [ ] Actuator : renommages Micrometer 1.10 → 1.13 à surveiller sur Grafana en Phase 5.
- [ ] Quartz : compatible SB3, vérifier `spring.quartz.*`.
- [ ] `authorizeRequests()` → `authorizeHttpRequests()` si présent.

### 1.5bis — Refactor GPIO : pi4j 2.x pigpio → pi4j 4.x FFM

Fichiers principaux touchés : `GpioHermanasRpiService.java`, `DefaultGpioPinDigitalOutput.java`, `DefaultGpioPinDigitalInput.java`, `DefaultGpioPwm.java`, `BirdhouseButtonService.java`, `LightService.java`, `FanService.java`.

**Suppressions dans `GpioHermanasRpiService.java`** :
- [ ] Import `uk.co.caprica.picam.*` (déjà prévu Phase 7 — peut rester en dette temporaire ici).
- [ ] Bloc `System.load(picamJniImplementation)` + try/catch `UnsatisfiedLinkError` (Phase 7).
- [ ] Annotation `@Value("${camera.picam.jni.implementation}")` et field `picamJniImplementation` (Phase 7).

**Adaptations pi4j 2.x → 4.x** :
- [ ] `Pi4J.newAutoContext()` : garder tel quel (auto-détection choisira FFM comme seul plugin présent).
- [ ] Vérifier les signatures d'appel `DigitalOutputConfigBuilder`, `DigitalInputConfigBuilder` — API stabilisée mais des detail methods peuvent avoir changé.
- [ ] Callbacks événements bouton (`BirdhouseButtonService.onDigitalStateChangeEvent`) : signature `DigitalStateChangeEvent<T>` peut varier entre 2.x et 4.x.

**Point critique — PWM du servo porte (`DefaultGpioPwm.java`)** :
- [ ] Vérifier que le PWM logiciel est supporté par `pi4j-plugin-ffm` sur GPIO 25 (broche 22).
- [ ] Si **non supporté** : deux options :
  - **Option 1 (recommandée)** : basculer sur PWM matériel via GPIO 18 (broche 12). Nécessite un **recâblage physique du fil de servo** de broche 22 vers broche 12. Mettre à jour `door.servo.gpio.address=18` dans `application.properties` et le tableau GPIO de Phase 5.1.
  - **Option 2** : implémenter le PWM logiciel via `ScheduledExecutorService` en pur Java. Moins précis pour un servo, à éviter.
- [ ] Si **supporté** sur GPIO 25 : rien à changer, garder `door.servo.gpio.address=25`.

### 1.6 — Compilation itérative

```bash
mvn clean compile 2>&1 | tee /tmp/compile.log
# Corriger les erreurs une par une :
#   1. imports javax restants
#   2. APIs supprimées Spring Security 6
#   3. APIs supprimées Pi4j (si bump)
#   4. warnings config SB3
mvn clean package    # test + fat JAR final
```

### 1.7 — Tests

- [ ] `mvn test` : 71+ tests backend passent.
- [ ] Frontend inchangé (npm build tel quel).

### 1.8 — Commit sur la branche

```bash
git add -A
git commit -m "feat: migrate to Java 25 + Spring Boot 3 + Jakarta EE + pi4j 4.x FFM (arm64)"
git push -u origin feat/pi-zero-2-migration
```

- [ ] **Ne pas merger sur master** — attendre la validation runtime en Phase 5.

---

## Phase 2 — Bascule des données et fichiers

⚠️ **Début de la fenêtre de downtime coop.** Planifier en milieu de journée, porte ouverte.

### 2.1 — Arrêt Hermanas sur `poupou`

```bash
ssh -p 5722 pi@poupou "sudo systemctl stop Hermanas.service"
```

### 2.2 — Dump MariaDB (défensif, cross-version 10.3 → 10.11)

```bash
ssh -p 5722 pi@poupou "mysqldump \
  --single-transaction \
  --routines --triggers --events \
  --skip-lock-tables \
  --no-tablespaces \
  --default-character-set=utf8mb4 \
  --set-gtid-purged=OFF \
  -u <user> -p<password> hermanas" > /tmp/hermanas-$(date +%F).sql
```

Nettoyer les DEFINER absents sur `pru` :

```bash
sed -i '' 's/DEFINER=`[^`]*`@`[^`]*` //g' /tmp/hermanas-*.sql
```

### 2.3 — Restauration

```bash
scp -P 5722 /tmp/hermanas-*.sql jean-baptisterenaux@pru.local:/tmp/
ssh -p 5722 jean-baptisterenaux@pru.local "mysql -u <user> -p<password> hermanas < /tmp/hermanas-$(date +%F).sql"
```

### 2.4 — Vérification counts

```bash
for host in poupou pru.local; do
  echo "=== $host ==="
  ssh -p 5722 pi@$host "mysql -u <user> -p<password> hermanas -e \"
    SELECT 'parameter' AS t, COUNT(*) FROM parameter UNION ALL
    SELECT 'sensor',     COUNT(*) FROM sensor    UNION ALL
    SELECT 'event',      COUNT(*) FROM event     UNION ALL
    SELECT 'picture',    COUNT(*) FROM picture;\""
done
```

- [ ] Les 4 counts sont **identiques** entre `poupou` et `pru`.

### 2.5 — Migration photos + music via Samba

Sur `pru` :

```bash
sudo apt install -y samba
sudo smbpasswd -a jean-baptisterenaux     # mot de passe temporaire libre
```

Ajouter le user `jean-baptisterenaux` au groupe `hermanas` **temporairement** pour pouvoir écrire dans `/var/lib/hermanas/` via Samba :

```bash
sudo usermod -aG hermanas jean-baptisterenaux
# La session SSH courante doit être renouvelée pour appliquer :
exit  # puis se reconnecter
```

Ajouter dans `/etc/samba/smb.conf` :

```ini
[migration]
   path = /var/lib/hermanas
   valid users = jean-baptisterenaux
   read only = no
   browsable = yes
   guest ok = no
   create mask = 0664
   directory mask = 0775
   force group = hermanas
```

`force group = hermanas` garantit que tous les fichiers créés par Samba appartiennent au groupe `hermanas`, donc lisibles par le service.

```bash
sudo systemctl restart smbd
```

Sur `poupou` (avant bascule DHCP, utiliser l'IP LAN de `pru`) :

```bash
sudo apt install -y cifs-utils
sudo mkdir -p /mnt/pru-migration
IP_PROU="192.168.1.XX"    # IP actuelle de pru avant bascule

sudo mount -t cifs //${IP_PROU}/migration /mnt/pru-migration \
  -o username=jean-baptisterenaux,password=<smb_password>,uid=$(id -u pi),gid=$(id -g pi)

rsync -avh --progress /home/pi/photos/           /mnt/pru-migration/photos/
rsync -avh --progress /home/pi/music/            /mnt/pru-migration/music/
rsync -avh --progress /home/pi/residents-photos/ /mnt/pru-migration/residents-photos/

sudo umount /mnt/pru-migration
```

- [ ] Vérifier tailles + counts sur `pru` :
  ```bash
  ssh -p 5722 jean-baptisterenaux@pru.local "du -sh /var/lib/hermanas/photos /var/lib/hermanas/music /var/lib/hermanas/residents-photos"
  ```
- [ ] Corriger l'ownership (Samba avec `force group` a fait au mieux, mais on veut `hermanas:hermanas` sur tout) :
  ```bash
  sudo chown -R hermanas:hermanas /var/lib/hermanas/photos /var/lib/hermanas/music /var/lib/hermanas/residents-photos
  ```

Nettoyer Samba (surface d'attaque minimale) :

```bash
ssh -p 5722 jean-baptisterenaux@pru.local << 'EOF'
sudo systemctl stop smbd
sudo systemctl disable smbd nmbd
sudo apt purge -y samba samba-common
sudo gpasswd -d jean-baptisterenaux hermanas  # retire l'ajout temporaire au groupe
EOF
```

### 2.6 — Fichiers externalisés

**Note :** les certificats TLS sont désormais gérés par le reverse-proxy sur `shannen`. On ne migre plus `letsencrypt/`, on ne vérifie plus d'expiration, on ne remet plus en place le cron de renouvellement. Seuls les fichiers strictement applicatifs sont transportés.

Copie via `/tmp/` puis placement dans `/var/lib/hermanas/` avec l'ownership `hermanas:hermanas` :

```bash
for f in application.properties users.properties keystore.p12 email; do
  scp -3 -P 5722 pi@poupou:/home/pi/$f jean-baptisterenaux@pru.local:/tmp/
done

ssh -p 5722 jean-baptisterenaux@pru.local << 'EOF'
sudo mv /tmp/application.properties /tmp/users.properties /tmp/keystore.p12 /tmp/email /var/lib/hermanas/
sudo chown hermanas:hermanas /var/lib/hermanas/application.properties /var/lib/hermanas/users.properties /var/lib/hermanas/keystore.p12 /var/lib/hermanas/email
sudo chmod 640 /var/lib/hermanas/application.properties /var/lib/hermanas/users.properties /var/lib/hermanas/keystore.p12 /var/lib/hermanas/email
EOF
```

`640` (owner rw, group r) : Hermanas peut lire, aucun autre user.

- [ ] **Ajuster les chemins dans `application.properties`** pour pointer vers `/var/lib/hermanas/` au lieu de `/home/pi/` :
  ```bash
  ssh -p 5722 jean-baptisterenaux@pru.local
  sudo sed -i 's|/home/pi/|/var/lib/hermanas/|g' /var/lib/hermanas/application.properties
  # Vérifier :
  sudo grep -E "camera\.path|music\.path|hermanas\.security\.users-file" /var/lib/hermanas/application.properties
  ```
  Attention : cette commande remplace TOUS les `/home/pi/` du fichier. À valider avant, il peut y avoir des cas de bord (chemins qui doivent rester en `/home/pi/` par erreur historique).
- [ ] `keystore.p12` conservé tel quel : Hermanas continue de servir en HTTPS interne sur `:8443` pour l'instant. Basculer Hermanas en HTTP simple (et laisser `shannen` terminer TLS) est un chantier séparé, hors migration matérielle.

### 2.7 — Config MariaDB custom (optionnel)

```bash
ssh -p 5722 pi@poupou "ls /etc/mysql/mariadb.conf.d/"
# S'il y a des fichiers autres que 50-server.cnf par défaut :
scp -3 -P 5722 pi@poupou:/etc/mysql/mariadb.conf.d/<file>.cnf jean-baptisterenaux@pru.local:/tmp/
# Diff avant d'écraser sur pru
```

---

## Phase 3 — Test à vide sur banc

Objectif : `pru` démarre le JAR Java 25 / SB3 / pi4j 4.x avec les vraies données, en profil `gpio-fake`, sans être branchée au coop.

### 3.1 — Build + déploiement du JAR

```bash
# Sur le Mac
mvn clean package -DskipTests
scp -P 5722 target/hermanas-*.jar jean-baptisterenaux@pru.local:/tmp/

# Sur pru — placer le JAR dans /var/lib/hermanas/ avec l'ownership hermanas:hermanas
ssh -p 5722 jean-baptisterenaux@pru.local << 'EOF'
JAR=$(basename /tmp/hermanas-*.jar)
sudo mv /tmp/$JAR /var/lib/hermanas/$JAR
sudo chown hermanas:hermanas /var/lib/hermanas/$JAR
sudo chmod 640 /var/lib/hermanas/$JAR
sudo -u hermanas ln -sfn /var/lib/hermanas/$JAR /var/lib/hermanas/hermanas.jar
ls -la /var/lib/hermanas/hermanas.jar
EOF
```

### 3.2 — Démarrage manuel en `gpio-fake`

Le test à vide se fait **sous le user `hermanas`** via `sudo -u`, pour valider que les permissions FS et l'accès Java 25 fonctionnent comme le service systemd le fera.

```bash
ssh -p 5722 jean-baptisterenaux@pru.local

sudo -u hermanas /usr/lib/jvm/java-25-openjdk-arm64/bin/java \
  -Xmx256m -Xms128m -XX:+UseSerialGC \
  --enable-native-access=ALL-UNNAMED \
  -jar /var/lib/hermanas/hermanas.jar \
  --spring.profiles.active=gpio-fake \
  --spring.config.location=file:/var/lib/hermanas/application.properties
```

**Attendus :**

- [ ] `Started HermanasApplication in XX seconds` — noter la valeur (baseline).
- [ ] `UnsatisfiedLinkError` sur `picam-2.0.1.so` : **normal** (catché par le service GPIO).
- [ ] `MusicService` peut throw (cvlc absent) : **normal**.
- [ ] Si `> 90 s` : envisager `arm_freq=800` au lieu de 600.

**Corrections attendues** : properties SB2→SB3 renommées, Hibernate dialect MariaDB 10.11.

### 3.3 — Sanity check applicatif

```bash
curl -k https://pru.local:8443/actuator/health
curl -k -c /tmp/cookies.txt -X POST https://pru.local:8443/api/v1/auth/login \
  -d "username=<user>&password=<password>"
curl -k -b /tmp/cookies.txt https://pru.local:8443/api/v1/sensor/info
```

- [ ] `/actuator/health` : `UP` (porte `DOWN` accepté en `gpio-fake`).
- [ ] Login OK, session valide.
- [ ] `/sensor/info` renvoie l'historique restauré.
- [ ] Frontend charge dans un navigateur (`https://pru.local:8443/`).

### 3.4 — Arrêt propre

Ctrl+C. **Ne pas démarrer le service systemd encore.**

---

## Phase 4 — Bascule réseau

### 4.1 — Extinction de `poupou`

```bash
ssh -p 5722 pi@poupou "sudo shutdown -h now"
```

- [ ] Attendre extinction complète (LED verte fixe puis éteinte).
- [ ] Débrancher **le câble GPIO du coop** (à conserver pour rollback).
- [ ] Débrancher l'alim.

### 4.2 — Freebox : bail DHCP statique

Interface admin Freebox → **Paramètres → DHCP → Baux statiques** :

- [ ] Repérer le bail actuel sur `192.168.1.35` (MAC de `poupou`).
- [ ] Remplacer la MAC par celle de `pru` (`ip link show wlan0 | grep ether` sur `pru`).
- [ ] Sauvegarder.

Sur `pru` :

```bash
sudo reboot
```

Après reboot :

```bash
ip -4 addr show wlan0    # doit afficher 192.168.1.35
```

### 4.3 — Vérifications

Depuis n'importe quelle machine du LAN :

```bash
ping poupou           # doit répondre depuis 192.168.1.35 (donc pru)
ssh -p 5722 pi@poupou # doit se connecter à pru
```

- [ ] `/etc/hosts` de `shannen` n'a **pas** besoin d'être touché (résolution par IP fixe).

---

## Phase 5 — Bascule hardware sur le coop

### 5.1 — Branchement

- [ ] Alimentation `pru`.
- [ ] Nappe caméra CSI-2 mini (22 pins, 0.5 mm, 11.5 mm) : **la même nappe qu'avec `poupou` se rebranche telle quelle** sur le connecteur CSI du Pi Zero 2 W. Aucun adaptateur, aucune pièce à racheter. Orientation : contacts métalliques face au PCB.
- [ ] Nappe GPIO sur le header 40 broches (câblage identique Zero → Zero 2). Recâbler selon la table ci-dessous :

**Câblage GPIO Hermanas** (numéros BCM = numéros dans le code Java, numéros de broche physique = position sur le header 40 pins) :

| GPIO (BCM) | Broche physique | Composant | Direction |
|:---:|:---:|---|---|
| 25 | 22 | Servomoteur de la porte | output (PWM) |
| 18 | 12 | Bouton de fin de course haut de la porte | input |
| 15 | 10 | Bouton de fin de course bas de la porte | input |
| 24 | 18 | Bouton poussoir inversé de la lumière du nichoir | input |
| 14 | 8 | Relais de l'éclairage | output |
| 23 | 16 | Relais du ventilateur | output |
| 4 | 7 | Capteur de température et d'humidité (DHT22) | input (1-wire) |

- [ ] Attendre le boot complet (~1 min).
- [ ] `ping poupou` (= `pru`) répond.

### 5.2 — Démarrage Hermanas

```bash
ssh -p 5722 pi@poupou "sudo systemctl start Hermanas.service"
ssh -p 5722 pi@poupou "sudo journalctl -u Hermanas.service -f"
```

- [ ] `Started HermanasApplication in XX seconds` sans erreur bloquante.

### 5.3 — Tests hardware manuels

- [ ] Porte : `/api/v1/door/open` puis `/api/v1/door/close` → mouvement servo vérifié.
- [ ] Lumière : `/api/v1/light/switch` → relais entendu, lumière visible.
- [ ] Ventilateur : `/api/v1/fan/switch`.
- [ ] Capteur T°/humidité : `/api/v1/sensor/info` → valeurs cohérentes.
- [ ] Boutons physiques : appuis testés, événements dans les logs.
- [ ] **Camera KO** (attendu) : `/api/v1/camera/*` renvoie 500.
- [ ] **Musique KO** (attendu) : `/api/v1/music/*` renvoie 500.

### 5.4 — Vérifications monitoring

- [ ] `https://www.hermanas.fr/` charge depuis Internet.
- [ ] Login public OK.
- [ ] Grafana (https://grafana.r3n4.uk) :
  - Dashboards **système** (CPU, RAM, disque, réseau) : points récents, pas de trou > 5 min.
  - Dashboards **Hermanas** (`hermanas.door.*`, `hermanas.sensor.*`) : points récents.
- [ ] `curl -s http://poupou:9100/metrics | head -3` depuis `shannen` : node-exporter OK.

### 5.5 — Cycle complet observé

- [ ] Laisser tourner **24 h minimum**, avec un cycle sunrise + sunset complet.
- [ ] Vérifier automatismes : `SunRelatedJob` (60s), door-state verification 30 min après sunrise/sunset.
- [ ] `journalctl -u Hermanas.service --since "24 hours ago" -p err` : pas d'erreur récurrente.

### 5.6 — Merge de la branche

Après 24 h stables :

```bash
git checkout master
git merge feat/pi-zero-2-migration
git push
```

---

## Phase 6 — Mise à jour des dépendances (Maven + npm)

Objectif : rattraper les updates de libs différées, profiter que `pru` tourne stable.

### 6.1 — Dépendances backend

```bash
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

À passer en revue, une lib à la fois, `mvn test` entre chaque, commit dédié :

- [ ] `spring-boot-starter-parent` : minor bumps SB3.
- [ ] `pi4j` : dernière 2.x.
- [ ] `mariadb-java-client` : dernière compatible SB3.
- [ ] `micrometer-*` : dernière.
- [ ] `commons-io`, `commons-lang3`, `lib-sunrise-sunset` : dernières.

### 6.2 — Dépendances frontend

```bash
cd frontend
npm outdated
```

Points ouverts (cf. CLAUDE.md Phase 7) :

- [ ] `@angular/cdk` + `@angular/material` 21 → 22 (Material 3 tokens, casse potentielle thème).
- [ ] `uuid` 9 → 14 (vérifier si encore présent dans `package.json`).
- [ ] **Reporter Angular 21 → 22 à début 2027** (posture définie dans CLAUDE.md).

Chaque major bump = branche dédiée + tests visuels + Playwright e2e (`npm run e2e`).

### 6.3 — Validation

- [ ] `mvn test` : 71+ tests OK.
- [ ] `cd frontend && npm test && npm run e2e` : suites vertes.
- [ ] Redéploiement complet via `deploy.sh`.
- [ ] Cycle 24 h d'observation.

---

## Phase 7 — Chantier camera + player audio

Objectif : restaurer les endpoints `/api/v1/camera/*` et `/api/v1/music/*` avec des stacks modernes.

### 7.1 — Snapshot camera : `rpicam-still`

- [ ] `sudo apt install -y rpicam-apps` sur `pru`.
- [ ] Refactor Java :
  - Supprimer dépendance Maven `uk.co.caprica:picam` dans `pom.xml`.
  - Supprimer `src/main/resources/native/picam-2.0.1.so`.
  - Supprimer `CameraConfiguration.java`.
  - Dans `GpioHermanasRpiService.java` : virer `System.load()` + imports `uk.co.caprica.picam.*`, remplacer `takePicture()` par `ProcessBuilder("rpicam-still", "-o", tempFile, "--width", ..., "--height", ..., "--quality", ...)`.
- [ ] `application.properties` : remplacer `camera.picam.jni.implementation` par `camera.snapshot.command`.
- [ ] Test `/api/v1/camera/takePicture` → JPEG valide.

### 7.2 — Streaming camera : `rpicam-vid | ffmpeg`

- [ ] `sudo apt install -y ffmpeg`.
- [ ] Éditer `application.properties` :
  ```
  camera.streaming.command = /bin/sh -c "rpicam-vid -t 0 --codec mjpeg --width 480 --height 270 --framerate 8 -o - | ffmpeg -f mjpeg -i - -c copy -f mpjpeg -listen 1 http://0.0.0.0:8081/"
  ```
- [ ] Aucun code Java à toucher (`CameraRestController` proxifie tel quel).
- [ ] Test `/api/v1/camera/stream`.

### 7.3 — Player audio

- [ ] `sudo apt install -y vlc-bin vlc-plugin-base alsa-utils`.
- [ ] Vérifier chemins : `/usr/bin/cvlc` et `/usr/bin/amixer` (matchent `application.properties`).
- [ ] **Pas de hack VLC-en-root nécessaire** : Hermanas tourne sous le user `hermanas` (Phase 0.4bis), pas root. VLC accepte n'importe quel user standard.
- [ ] Test `/api/v1/music/switch` et `/api/v1/music/cocorico`.

### 7.4 — Optionnel : améliorations futures

- [ ] `go2rtc` pour WebRTC (chantier front + back non trivial).

---

## Phase 8 — Rollback plan

Fenêtre de conservation : **1 semaine post-Phase 5**.

### 8.1 — Éléments à conserver

- [ ] SD `poupou` intacte dans une pochette antistatique.
- [ ] Dump SQL `hermanas-YYYY-MM-DD.sql` sur le Mac.
- [ ] `application.properties`, `users.properties`, `keystore.p12` en backup local.
- [ ] Ancien JAR Java 11 (`hermanas-0.8.5.jar`) sur le Mac.

### 8.2 — Procédure rollback (~15 min)

- [ ] `sudo systemctl stop Hermanas.service` sur `pru`.
- [ ] Débrancher GPIO de `pru`, rebrancher sur `poupou`.
- [ ] Freebox : rebasculer bail DHCP MAC `pru` → MAC `poupou`.
- [ ] Démarrer `poupou` : `sudo systemctl start Hermanas.service`.
- [ ] Vérifier `https://www.hermanas.fr/`.

### 8.3 — Nettoyage définitif (après 1 semaine stable)

- [ ] Wiper la SD `poupou` ou la ranger comme spare.
- [ ] Supprimer le dump SQL local.
- [ ] Mettre à jour `deploy.sh` si tu renommes les variables `REMOTE_HOST` (`poupou` reste valide via DHCP donc pas obligatoire).

---

## 📊 Récapitulatif — timing

| Phase | Sur | Durée | Downtime coop |
|---|---|---|---|
| 0. Prépa `pru` headless | pru | 1-2 h | 0 |
| 1. Migration code Java 25 + SB3 + pi4j 4.x FFM | Mac | 6-10 h dev | 0 |
| 2. Bascule données + Samba photos | poupou→pru | 30-60 min | **✓ démarre** |
| 3. Test à vide sur banc | pru | 30 min | ✓ |
| 4. Bascule réseau (Freebox) | Freebox | 10 min | ✓ |
| 5. Bascule hardware sur coop | pru + coop | 15 min + 24 h obs | **✓ termine (~1 h)** |
| 6. Update libs Maven + npm | Mac | 1-2 jours | 0 |
| 7. Camera + player audio | pru + Mac | 4-8 h dev | 0 |
| 8. Attente rollback | – | 1 semaine | 0 |

**Downtime coop total : ~1 h**, à planifier porte ouverte.

---

## 🎯 Décisions clés figées

- **Hostname interne `pru`** (pas `poulailler`), pour marquer la différence avec l'ancien matériel.
- **Alias externe `poupou`** conservé, résolu par IP fixe `192.168.1.35` côté `shannen` — rien à toucher côté résolution de nom.
- **OS `pru` = Raspberry Pi OS Lite 64-bit Bookworm** (arm64).
- **Runtime = Java 25 LTS + Spring Boot 3 + Jakarta EE + pi4j 4.x FFM** dès le départ. Le passage direct à FFM élimine les dettes techniques `pigpio` et `libgpiod`/`gpiod` en une bascule.
- **4 cœurs Zero 2 W conservés** (pas de `maxcpus=1`).
- **Wi-Fi conservé** (pas de `disable-wifi`).
- **CPU bridé à 600 MHz** (`arm_freq=600`) — à ajuster si démarrage Spring trop lent.
- **SSH par clé** depuis le Mac (Phase 0.2bis), plus de mot de passe à taper.
- **Hermanas tourne sous le user système `hermanas`** (non-root), membre des groupes `gpio`, `audio`, `dialout`. Rendu possible par le passage à pi4j FFM + chardev (`/dev/gpiochip0` accessible au groupe `gpio`). Bonus : élimine le hack VLC-en-root de `poupou`. Fichiers applicatifs dans `/var/lib/hermanas/` (owner `hermanas:hermanas`, mode 640/750). Unit systemd durci : `ProtectHome=yes`, `ProtectSystem=strict`, `NoNewPrivileges=yes`, `PrivateTmp=yes`.
- **Certificats TLS gérés par `shannen`** — plus de Let's Encrypt côté `pru`. Le `keystore.p12` local est conservé tel quel pour l'HTTPS interne (`:8443`), sa gestion long terme est un chantier séparé.
- **Camera + player audio traités en dernier** (Phase 7), après stabilisation et update libs.
- **Photos migrées via Samba** (share temporaire sur `pru`, monté sur `poupou`, purgé après).
- **Prometheus + Grafana restent sur `shannen`** — juste `prometheus-node-exporter` à réinstaller sur `pru`.
- **Rollback conservé 1 semaine** post-bascule.
