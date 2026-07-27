# Migration `poupou` (Pi Zero) → `pru` (Pi Zero 2 W)

Checklist opérationnelle. Stratégie all-in-one : hardware + OS 64 bits (Trixie arm64) + **Java 25** + Spring Boot 4 + Jakarta EE + **pi4j 4.x avec FFM** dans une seule bascule. Camera + player audio traités en dernier.

**Décision GPIO stack (2026-07-04)** : passage direct à `pi4j-plugin-ffm` (Foreign Function & Memory API de Java 22+, requiert Java 25 LTS). Élimine la dette technique `pigpio` (archivé 2021, retiré de Trixie) **et** la dette technique intermédiaire `libgpiod`/`gpiod` (déprécié dans pi4j 4.1). Voie officielle définitive de pi4j.

**Downtime coop attendu : ~1 h**, à planifier en milieu de journée porte ouverte.

**Alias réseau conservé** : `poupou` (côté `shannen`) = IP fixe `192.168.1.35`, restera valide après bascule DHCP. Hostname interne de la nouvelle machine = `pru` (marque la différence avec l'ancien `poulailler`).

---

## 📚 Sommaire

- [Phase 0 — Préparation `pru` headless (hors ligne)](#phase-0--préparation-pru-headless-hors-ligne)
- [Phase 1 — Migration code Java 25 / Spring Boot 4 / Jakarta EE / pi4j 4.x](#phase-1--migration-code-java-25--spring-boot-4--jakarta-ee--pi4j-4x)
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

**Nouvelle version de `Hermanas.sh` pour `pru`** (déjà adaptée Java 25 / SB4 / pi4j 4.x FFM / Zero 2 W 512 Mo — voir tableau des changements sous le script) :

```sh
#!/bin/sh
SERVICE_NAME=Hermanas
PATH_TO_JAR=/var/lib/hermanas/hermanas.jar
PID_PATH_NAME=/run/hermanas/hermanas.pid
CONFIG_LOCATION=file:/var/lib/hermanas/application.properties

# Chemin absolu vers Java 25 (openjdk-25-jdk apt) — vérifier avec `readlink -f $(which java)`
JAVA_BIN=/usr/lib/jvm/java-25-openjdk-arm64/bin/java

# --enable-native-access=ALL-UNNAMED : requis pour pi4j-plugin-ffm (FFM API restricted)
# -Djava.security.egd : sans cette option la JVM lit /dev/random, qui bloque
# quand le pool d'entropie est vide — cas courant sur un Pi headless. Constaté
# le 2026-07-27 : entropy_avail à 256, démarrage de plus de 5 minutes avec un
# CPU quasi inactif (la JVM attendait, elle ne calculait pas). Le "/./" est
# indispensable, sans lui l'option est silencieusement ignorée.
# Empreinte mémoire plafonnée poste par poste. Le heap seul ne suffit pas :
# mesuré sur pru, la JVM occupait 324 Mo (133 en RAM + 191 swappés) alors que
# -Xmx valait 192m — les ~130 Mo restants sont hors heap (metaspace, code JIT,
# piles de threads). Sur 415 Mo partagés avec MariaDB, chaque poste doit être
# borné, sans quoi le noyau swappe sur la carte SD et l'use prématurément.
#
#  -Xms = -Xmx    : heap alloué d'emblée à sa taille finale. Sinon il grandit
#                   par paliers, et chaque agrandissement recopie des pages.
#  MaxMetaspace   : Spring Boot 4 charge beaucoup de classes ; sans plafond ce
#                   poste devient le deuxième plus lourd après le heap.
#                   ⚠️ 96m était TROP BAS : OutOfMemoryError: Metaspace en boucle
#                   au démarrage, systématiquement à l'initialisation GPIO quand
#                   pi4j et ses providers s'ajoutent aux classes Spring. 192m
#                   laisse de la marge — l'objectif est de borner la croissance,
#                   pas de comprimer au maximum. Mesurer le besoin réel avec
#                   `jcmd <pid> VM.native_memory summary` avant de resserrer.
#  ReservedCode   : le défaut réserve 240 Mo d'espace d'adressage pour le JIT.
#  Xss512k        : 512 Ko par thread au lieu de 1 Mo (~40 threads Tomcat).
#  ExitOnOOM      : arrêt net + redémarrage systemd, plutôt qu'une JVM qui
#                   agonise en swappant.
JVM_OPTS="-Xmx160m -Xms160m \
    -XX:+UseSerialGC \
    -XX:MaxMetaspaceSize=192m \
    -XX:ReservedCodeCacheSize=48m \
    -XX:MaxDirectMemorySize=16m \
    -Xss512k \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.net.preferIPv4Stack=true \
    -Djava.security.egd=file:/dev/./urandom \
    --enable-native-access=ALL-UNNAMED"

# JMX restreint à la boucle locale via jmxremote.host — accès par tunnel SSH :
#   ssh -p 5722 -L 9010:localhost:9010 jean-baptisterenaux@pru.local
# NE PAS utiliser local.only=true : incompatible avec un port déclaré, la JVM
# refuse alors de démarrer le connecteur (cf. correction du 2026-07-27).
JMX_OPTS="-Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.host=127.0.0.1 \
    -Djava.rmi.server.hostname=127.0.0.1"

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup $JAVA_BIN $JVM_OPTS $JMX_OPTS \
                -jar $PATH_TO_JAR \
                --spring.config.additional-location=$CONFIG_LOCATION \
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
                -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005 \
                -jar $PATH_TO_JAR \
                --spring.config.additional-location=$CONFIG_LOCATION \
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
| Pas de `-Xmx` | `-Xmx256m -Xms128m` | Plafonner la heap : SB4 est plus lourd que SB2, la RAM Zero 2 W reste à 512 Mo |
| Pas de GC choisi | `-XX:+UseSerialGC` | GC single-thread, économe en mémoire, adapté aux petits heaps sur ARM |
| Pas de FFM autorisation | `--enable-native-access=ALL-UNNAMED` | Requis pour `pi4j-plugin-ffm` — sans ça, warning restricted method à chaque appel GPIO |
| `java.rmi.server.hostname=10.0.0.20` | `=127.0.0.1` | L'IP `10.0.0.20` était un vestige non fonctionnel (LAN Freebox = 192.168.1.0/24) |
| `local.only=false` | `jmxremote.host=127.0.0.1` | JMX exposé sans auth ni TLS = trou de sécu. `local.only=true` avait d'abord été retenu, mais ce paramètre est **incompatible avec un port déclaré** et empêche la JVM de démarrer le connecteur. `jmxremote.host` fait ce qu'on attendait : le connecteur n'écoute que sur la boucle locale, l'accès passe par tunnel SSH |
| Pas de `--spring.config.location` | `--spring.config.additional-location=file:/var/lib/hermanas/application.properties` | Ne dépend plus du CWD au démarrage. ⚠️ **`additional-location` et non `location`** : `location` *remplace* la configuration embarquée dans le JAR, `additional-location` la *complète*. Le fichier externe hérité de `poupou` ne contient qu'une soixantaine de propriétés sur les 165 du JAR — avec `location`, tout le reste disparaît et l'application échoue au démarrage sur `Could not resolve placeholder` (constaté le 2026-07-27 avec `light.security.timer.delay.eco`). |
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
# ⚠️ i2c et spi sont indispensables même si le projet n'utilise ni l'un ni l'autre :
# le plugin pi4j FFM initialise TOUS ses providers d'un bloc, et FFMI2CProviderImpl
# vérifie l'appartenance au groupe i2c. L'échec de ce contrôle fait tomber le plugin
# entier — les providers ffm-digital-* ne sont alors jamais enregistrés, et toute
# création de sortie GPIO échoue sur ProviderNotFoundException (constaté 2026-07-27).
sudo usermod -aG i2c   hermanas     # exigé par FFMI2CProviderImpl
sudo usermod -aG spi   hermanas     # idem côté SPI
sudo usermod -aG audio hermanas     # accès à /dev/snd/* pour cvlc/amixer
sudo usermod -aG dialout hermanas   # accès aux périphériques série éventuels (utile pour DHT22 via UART si besoin un jour)
```

Vérifier :
```bash
id hermanas
# attendu : uid=... groups=...(hermanas),20(dialout),29(audio),997(gpio),998(i2c),999(spi)
# (numéros indicatifs — ce sont les NOMS de groupes qui comptent)
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
# multi-user.target est atteint quand tous les services système sont lancés.
# Sans cette contrainte, Hermanas démarre en concurrence avec le reste du boot
# et la JVM réclame ses ~300 Mo au pire moment — d'où le pic de swap constaté
# (450 Mo écrits sur la carte SD, cf. 3.2bis).
After=network.target mariadb.service multi-user.target
Requires=mariadb.service

[Service]
Type=forking
User=hermanas
Group=hermanas
SupplementaryGroups=gpio i2c spi audio dialout

# Laisse le système se stabiliser avant de réclamer la mémoire : les services de
# boot terminent leur initialisation et libèrent leurs allocations temporaires.
# Sur une machine où le démarrage prend déjà 8 minutes, 45 s de plus ne coûtent
# rien face à l'usure de carte SD évitée.
ExecStartPre=/bin/sleep 45

# Priorité basse : Hermanas cède le CPU et les I/O aux services système pendant
# qu'ils démarrent. Sans effet une fois le système au repos, l'application étant
# alors seule à travailler.
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=6

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
  # attendu : User=hermanas, Group=hermanas, SupplementaryGroups=gpio i2c spi audio dialout
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

### 0.7bis — ⚠️ Script Python du capteur DHT22 (oubli de la roadmap initiale)

> **Trou identifié le 2026-07-27.** `application.properties` pilote le capteur de
> température/humidité par un script Python externe :
> ```
> sensor.python.command = /usr/bin/python
> sensor.python.script  = /home/pi/AdafruitDHT.py
> ```
> Ce script n'apparaissait **nulle part** dans la roadmap, et la note de projet le
> rangeait même parmi les « anciens scripts Python de prototypage » à ne pas migrer,
> au même titre que `servo_door_*.py` ou `button.py`. C'est une erreur : ceux-là sont
> effectivement obsolètes, mais **`AdafruitDHT.py` est toujours utilisé en production**.
> Le laisser derrière casse la lecture température/humidité.

**Constaté sur `poupou` (2026-07-27) :**

```
/usr/bin/python -> python2                 # le script tourne en Python 2
Adafruit_DHT : présent en python2 uniquement
sudo /usr/bin/python AdafruitDHT.py 22 4 → Temp=26.7*  Humidity=99.9%
sans sudo                                → Failed to get reading
```

Deux blocages pour `pru` :
1. **Python 2 est absent de Trixie.** Le script ne peut pas être repris tel quel.
2. **`Adafruit_DHT` exige root** — elle accède à `/dev/mem`. Or Hermanas tourne sous
   le user `hermanas`, non privilégié : le script échouerait silencieusement.

**Solution retenue (décision utilisateur) : réécriture en Python 3 avec
`adafruit-circuitpython-dht`.** Cette bibliothèque parle au chardev
`/dev/gpiochip0` via libgpiod plutôt qu'à `/dev/mem` — l'appartenance au groupe
`gpio` suffit, ce que `hermanas` a déjà. C'est le même mécanisme que
`pi4j-plugin-ffm`, donc cohérent avec le reste de la migration.

- [x] Script réécrit : **`scripts/read_dht22.py`** (versionné dans le dépôt).
  Le format de sortie est **conservé à l'identique** — `Temp=26.7*  Humidity=99.9%` —
  car `SensorService.parseSensorReturnedValue()` découpe cette chaîne sur les espaces
  et cherche les préfixes `Temp=` et `Humidity=`. Aucun code Java à modifier.
  Il reproduit aussi le comportement de `read_retry()` (15 tentatives espacées de 2 s),
  le DHT22 échouant fréquemment sur une lecture isolée.

- [ ] Installer la dépendance sur `pru` :
  ```bash
  sudo apt install -y python3-pip libgpiod3 python3-libgpiod
  pip3 install --break-system-packages adafruit-circuitpython-dht
  python3 -c "import adafruit_dht, board; print('modules OK')"
  ```
  ⚠️ **`libgpiod3` et non `libgpiod2`** : Trixie est passé à libgpiod 2.x et a
  renommé le paquet. `apt-cache search libgpiod` pour vérifier si le nom change
  encore. `python3-libgpiod` fournit les bindings Python en paquet Debian, ce qui
  évite de les faire compiler par pip.
- [ ] Déposer le script :
  ```bash
  # depuis le Mac
  scp -P 5722 scripts/read_dht22.py jean-baptisterenaux@pru.local:/tmp/
  # sur pru
  sudo mv /tmp/read_dht22.py /var/lib/hermanas/
  sudo chown hermanas:hermanas /var/lib/hermanas/read_dht22.py
  sudo chmod 750 /var/lib/hermanas/read_dht22.py
  ```
- [ ] Adapter `/var/lib/hermanas/application.properties` :
  ```properties
  sensor.python.command = /usr/bin/python3
  sensor.python.script  = /var/lib/hermanas/read_dht22.py
  ```
  ⚠️ **`python3` et non `python`** : Trixie ne fournit pas `/usr/bin/python`.
- [ ] Tester **sous le user `hermanas`**, pour valider que root n'est plus nécessaire :
  ```bash
  sudo -u hermanas /usr/bin/python3 /var/lib/hermanas/read_dht22.py 22 4
  # attendu : Temp=XX.X*  Humidity=XX.X%
  ```
  Ce test ne peut réussir qu'une fois le capteur physiquement branché (Phase 5).
- [ ] *Alternative si la bibliothèque pose problème* : le noyau expose le DHT22 via
  `dtoverlay=dht11,gpiopin=4` et le sysfs `/sys/bus/iio/devices/`, ce qui supprimerait
  toute dépendance Python. Demande d'adapter `SensorService` — à évaluer hors bascule.

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

## Phase 1 — Migration code Java 25 / Spring Boot 4 / Jakarta EE / pi4j 4.x

Objectif : le repo compile et passe les tests avec Java 25 + **Spring Boot 4** + Jakarta EE + pi4j 4.x FFM. Sur le Mac uniquement. Branche dédiée. Aucun impact sur `poupou` ni `pru`.

> **Décision utilisateur 2026-07-27 — Spring Boot 4 au lieu de 3.5.x.** La roadmap
> initiale visait SB 3.5.x. La migration a d'abord été menée jusqu'à un
> `BUILD SUCCESS` sur **SB 3.5.3**, puis poussée jusqu'à **SB 4.1.0** (GA du
> 2026-06-10) pour partir sur les dernières versions plutôt que de devoir
> refaire le chantier dans quelques mois. Les deux paliers sont documentés
> ci-dessous : ce qui a été fait pour SB3 reste valable, SB4 ajoute une
> couche supplémentaire de renommages liés à sa modularisation.

### ✅ Phase 1 terminée (2026-07-27)

`mvn clean package` → **BUILD SUCCESS**, **66/66 tests**, JAR **102 Mo** avec la SPA
bundlée (`fr-FR`, `en-US`, `ro-RO`). Commit **`e1df962`** sur `feat/pi-zero-2-migration`
(67 fichiers, +692 / −292).

Tout ce qui était réalisable sur le Mac est fait. Les 7 items encore décochés plus bas
ne sont **pas des oublis** — ils dépendent tous d'une machine qui tourne ou du matériel :

| Item | Reporté en | Pourquoi |
|---|---|---|
| Bloc `picam` dans `GpioHermanasRpiService` (3 items) | **Phase 7** | Dette assumée dès la conception de la roadmap : le chantier caméra est traité en dernier |
| Warnings properties `spring.*` | **Phase 3** | Visibles seulement au démarrage de l'application |
| `spring.quartz.*` | **Phase 3** | idem |
| Renommages Micrometer sur Grafana | **Phase 5** | Nécessite les métriques réelles remontées en production |
| Recâblage servo broche 22 → 32 | **Phase 5.1** | Manipulation physique, au montage sur le coop |
| « Ne pas merger sur master » | **Phase 5** | Garde-fou volontaire jusqu'à validation runtime |

**Seule action encore possible ici :** `git push` de la branche (cf. 1.8).

➡️ **La suite est la Phase 2**, qui ouvre la fenêtre de downtime du poulailler.

### 1.1 — Branche + snapshot

```bash
git checkout -b feat/pi-zero-2-migration
git status    # doit être clean sauf éventuels WIP à commit avant
```

### 1.2 — `pom.xml` : bumps de version

- [x] `<java.version>11</java.version>` → `<java.version>25</java.version>`.
- [x] `<maven.compiler.source>` et `<maven.compiler.target>` → `25` si présents. *(Non présents dans le pom, hérités du parent SB.)*
- [x] `spring-boot-starter-parent` : `2.7.18` → `3.5.3` → **`4.1.0`** (GA du 2026-06-10).
- [x] `<pi4j.version>2.4.0</pi4j.version>` → **`4.0.2`** (release 2026-06-08, dernière 4.x stable).
- [x] **Retirer** les artefacts `pi4j-plugin-raspberrypi` et `pi4j-plugin-pigpio` du bloc `<dependencies>`.
- [x] **Ajouter** la nouvelle dépendance :
  ```xml
  <dependency>
      <groupId>com.pi4j</groupId>
      <artifactId>pi4j-plugin-ffm</artifactId>
      <version>${pi4j.version}</version>
  </dependency>
  ```
- [x] Laisser `<picam.version>` inchangé (retiré en Phase 7).
- [x] **Fixer manuellement `org.apache.httpcomponents:httpclient` en 4.5.14** — Spring Boot 3 a retiré `httpclient` (v4) de son BOM (migration vers httpclient5). `web-push` 5.1.2 tire encore `httpasyncclient` 4.1.5, on garde donc la génération v4 pour compat API (`HttpResponse` exposé par `PushService.send`).
- [x] `mvn versions:display-dependency-updates` exécuté au 2026-07-26. Bumps mineurs appliqués :
  - `mariadb-java-client` 3.5.8 → **3.5.9**
  - `commons-io` 2.20.0 → **2.22.0**
  - `resilience4j-spring-boot2` 1.7.1 → **`resilience4j-spring-boot4` 2.4.0**.
    ⚠️ L'artifactId encode la ligne Spring Boot ciblée, et le module **refuse de
    démarrer** si elle ne correspond pas : `SpringBoot3Verifier` lève
    « Module ... is only compatible with Spring Boot 3.x » au lancement. Une étape
    intermédiaire en `-spring-boot3` compilait sans erreur mais échouait au runtime
    (constaté 2026-07-27).
  - `h2` : inchangé, déjà géré par le BOM SB

**Bumps supplémentaires imposés par SB 4 :**

- [x] **`spring-boot-starter-aop` → `spring-boot-starter-aspectj`** — le starter a été renommé.
- [x] **`spring-boot-starter-restclient` ajouté** — `RestTemplateBuilder` / `RestClient.Builder` ne sont plus fournis par `spring-boot-starter-web`.
- [x] **`spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test` ajoutés** — l'infra de test est modularisée (`@DataJpaTest`, `@WebMvcTest` ne sont plus dans `spring-boot-test-autoconfigure`). Ces starters tirent `spring-boot-starter-test` transitivement.
- [x] **`spring-retry` : version fixée manuellement à `2.0.9`** — retiré du BOM SB 4, la déclaration sans version ne résout plus.
- [x] **`jacoco-maven-plugin` 0.8.12 → `0.8.15`** — les versions ≤ 0.8.12 ne savent pas instrumenter le bytecode Java 25 (`IllegalClassFormatException` sur chaque classe, tests inexploitables).
- [x] **`springdoc-openapi-ui` 1.8.0 → `springdoc-openapi-starter-webmvc-ui` 3.0.3** — la ligne 1.x est SB 2, la 2.x est SB 3, la **3.x est la ligne SB 4**. Aucun changement de code applicatif nécessaire (annotations `io.swagger.v3.*` inchangées).
- [x] **`<fork>true</fork>` sur `maven-compiler-plugin`** — voir 1.6, contourne un crash de la compilation in-process.

**Pas fait volontairement :**
- jquery 4 (held back, risque de casse visuelle — cf. CLAUDE.md)
- **Doublon Jackson 2 / Jackson 3 accepté** (réexaminé 2026-07-27). SB 4 est sur
  Jackson 3 (`tools.jackson.*`), mais `swagger-core-jakarta` — dépendance amont de
  springdoc — reste sur Jackson 2 (`com.fasterxml.jackson.*`).

  **Aucune version ne corrige cela** : il n'existe pas de ligne 3.x de Swagger, et
  la plus récente (2.2.52) dépend toujours de `com.fasterxml.jackson.core:jackson-databind`.
  Bumper depuis la 2.2.47 tirée par springdoc n'apporterait donc rien sur ce point.

  **Ce n'est pas un conflit** : groupId, packages racine et modules JPMS sont distincts,
  aucune classe n'est homonyme. C'est précisément la cohabitation que le renommage de
  groupId de Jackson 3 visait à permettre. Coût : ~4 Mo de JAR et quelques Mo de metaspace.

  Alternatives écartées : retirer springdoc (perte de la doc API interactive et des
  annotations OpenAPI de 46 endpoints), ou forcer une override de version (risque
  d'incompatibilité pour un gain nul). À réévaluer quand Swagger migrera.

  ⚠️ **Le code applicatif, lui, est intégralement sur Jackson 3.** `DetectionParser`
  utilisait `com.fasterxml.jackson.databind.ObjectMapper`, disponible seulement par
  transitivité via Swagger — migré vers `tools.jackson.databind.ObjectMapper` :
  - `JsonProcessingException` → `JacksonException` (renommée, et devenue *unchecked*)
  - `getOriginalMessage()` → `getMessage()` (supprimée en Jackson 3)

  Les `@JsonIgnoreProperties` de `Sensor` et `WeatherInfo` **restent en
  `com.fasterxml.jackson.annotation`** et c'est correct : Jackson 3 dépend lui-même de
  `com.fasterxml.jackson.core:jackson-annotations`, le module d'annotations n'ayant pas
  été renommé. Ne pas chercher à les « migrer ».

**Bump version applicative** : `0.8.11` → **`0.9.1`** (jalon migration all-in-one).

### 1.3 — Refactor `javax.*` → `jakarta.*` (37 fichiers)

Commande one-shot exécutée depuis la racine du projet :

```bash
grep -rl -E "javax\.(persistence|annotation|servlet|validation|mail)" src/ | \
  xargs sed -i '' -E 's|javax\.(persistence\|annotation\|servlet\|validation\|mail)|jakarta.\1|g'
```

- [x] 37 fichiers refactorés en une passe.
- [x] **NE PAS toucher** `javax.imageio.*` (JDK standard, pas Jakarta) — respecté par le regex `-E`.
- [x] Vérifier avec `grep -rn "javax\." src/main/java` — restent uniquement `javax.sql.DataSource` (JDK) et `javax.imageio.ImageIO` (JDK). ✅
- [x] Résultat : de **100 → 12 erreurs de compilation**.

### 1.4 — SecurityConfig (Spring Security 6 puis 7)

- [x] `@EnableGlobalMethodSecurity(prePostEnabled = true)` → **`@EnableMethodSecurity`**.
- [x] Vérifier que `SecurityFilterChain` (déjà en place) compile. ✅
- [x] **`antMatchers(...)` → `requestMatchers(...)`** (18 occurrences) — supprimé dans Spring Security 6 :
  ```bash
  sed -i '' 's|\.antMatchers(|.requestMatchers(|g' \
    src/main/java/org/jibe77/hermanas/security/SecurityConfig.java
  ```
- [x] **`AntPathRequestMatcher` → `PathPatternRequestMatcher.pathPattern(...)`** — supprimé en Security 7, nouveau package `org.springframework.security.web.servlet.util.matcher`.
- [x] **`new DaoAuthenticationProvider()` + `setUserDetailsService(uds)` → `new DaoAuthenticationProvider(uds)`** — le constructeur no-arg et le setter ont disparu en Security 7.
- [x] **Toute la chaîne fluide convertie en DSL lambda** — en Security 7 les surcharges no-arg (`headers()`, `csrf()`, `formLogin()`, `rememberMe()`, `logout()`, `exceptionHandling()`) et les `.and()` sont supprimées. Chaque bloc devient un `Customizer` :
  ```java
  // Avant (Security 5/6)
  .headers().frameOptions().disable()
  .and()
  .csrf().csrfTokenRepository(...)
  .and()
  .authorizeRequests()
      .requestMatchers(...).permitAll()
      .anyRequest().permitAll()
  .and()

  // Après (Security 7)
  .headers(h -> h.frameOptions(fo -> fo.disable()))
  .csrf(csrf -> csrf.csrfTokenRepository(...))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers(...).permitAll()
      .anyRequest().permitAll())
  ```
- [x] **`authorizeRequests()` → `authorizeHttpRequests()`**.

### 1.4bis — Durcissement `GlobalExceptionHandler` (effet de bord utile)

Découvert en corrigeant les tests : `AuthenticationCredentialsNotFoundException` et
`AccessDeniedException` tombaient sur le handler générique `@ExceptionHandler(Exception.class)`
et ressortaient en **HTTP 500**. En production la chaîne de filtres Security les intercepte
avant, ce qui masquait le problème — mais dès que la chaîne est absente (slice `@WebMvcTest`)
ou que l'exception remonte de plus profond, un défaut d'authentification se présentait comme
une erreur serveur.

- [x] Handler `AuthenticationCredentialsNotFoundException` → **401 Unauthorized**.
- [x] Handler `AccessDeniedException` → **403 Forbidden**.

### 1.5 — Autres points SB 2 → 3 → 4

**Packages déplacés par la modularisation SB 4** (le gros du travail) :

| Avant | Après |
|---|---|
| `org.springframework.boot.web.client.RestTemplateBuilder` | `org.springframework.boot.restclient.RestTemplateBuilder` |
| `org.springframework.boot.web.servlet.error.ErrorController` | `org.springframework.boot.webmvc.error.ErrorController` |
| `org.springframework.boot.actuate.health.*` (`Health`, `HealthIndicator`, `Status`) | `org.springframework.boot.health.contributor.*` |
| `org.springframework.boot.actuate.system.DiskSpaceHealthIndicator` | `org.springframework.boot.health.application.DiskSpaceHealthIndicator` |
| `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `org.springframework.boot.test.mock.mockito.MockBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |

**Méthodes renommées :**
- [x] `RestTemplateBuilder.setConnectTimeout(...)` → **`connectTimeout(...)`**, `setReadTimeout(...)` → **`readTimeout(...)`**.
  ⚠️ Piège : un `sed` global sur `setConnectTimeout` touche aussi `HttpURLConnection` dans
  `CameraRestController`, dont les setters n'ont **pas** changé. Vérifier le diff après coup.
- [x] `HealthIndicator.getHealth(boolean)` → **`health(boolean)`**.

**Restant à valider au runtime (Phase 3) :**
- [ ] Properties `spring.*` : lire les warnings au démarrage.
- [ ] Actuator : renommages Micrometer à surveiller sur Grafana en Phase 5.
- [ ] Quartz : vérifier `spring.quartz.*`.
- [x] **`<executable>true</executable>` retiré du `spring-boot-maven-plugin`** — option
  supprimée en SB 4 (`Parameter 'executable' is unknown` au build). Le JAR n'est plus
  directement exécutable (`./hermanas.jar`), sans impact puisque `Hermanas.sh` lance `java -jar`.

### 1.5bis — Refactor GPIO : pi4j 2.x pigpio → pi4j 4.x FFM

Fichiers touchés : `GpioHermanasRpiService.java`, `DefaultGpioPinDigitalOutput.java`, `DefaultGpioPinDigitalInput.java`, `DefaultGpioPwm.java`.

**Suppressions dans `GpioHermanasRpiService.java`** (retirées en Phase 7, gardées en dette pour le moment) :
- [ ] Import `uk.co.caprica.picam.*` (Phase 7).
- [ ] Bloc `System.load(picamJniImplementation)` + try/catch `UnsatisfiedLinkError` (Phase 7).
- [ ] Annotation `@Value("${camera.picam.jni.implementation}")` et field `picamJniImplementation` (Phase 7).

**Adaptations API pi4j 2.x → 4.x — signatures réelles vérifiées via `javap` sur `pi4j-core-4.0.2.jar`** :

- [x] `Pi4J.newAutoContext()` : gardé tel quel (auto-détection choisira FFM comme seul plugin présent).
- [x] **`.address(int)` → `.bcm(int)`** sur `DigitalInputConfigBuilder`, `DigitalOutputConfigBuilder`, `PwmConfigBuilder`. `address(int)` a été renommé pour clarifier le mode de numérotation.
- [x] **Providers `pigpio-*` → `ffm-*`** :
  - `"pigpio-digital-input"` → `"ffm-digital-input"`
  - `"pigpio-digital-output"` → `"ffm-digital-output"`
  - `"pigpio-pwm"` → `"ffm-pwm"`
- [x] **`event.source().getAddress()` → `event.source().bcm()`** dans le listener de `provisionOutput`.
- [x] **`Lifecycle.shutdown(Context)` → `shutdownInternal(Context)`** avec type de retour concret (plus `Object`) — impacte les 3 stubs `Default*`.
- [x] **`Pwm.getDutyCycle()` retourne `Integer`** (pas `float` ni `double` comme l'Explore l'avait dit initialement) — validé par `javap`.
- [x] **`Pwm.setDutyCycle(Integer)`** (pas `Number`) — idem.
- [x] **`IO.close()`** ajouté (nouvelle méthode de l'interface).
- [x] **`ListenableOnOffRead.addConsumer/removeConsumer(Consumer<Boolean>)`** ajoutés dans les stubs Digital (nouvelle méthode 4.x).

**Point critique — PWM du servo porte : PWM logiciel NON supporté par FFM ⚠️** :

Confirmé par l'inspection du code source `pi4j-v2/plugins/pi4j-plugin-ffm/.../FFMPwmProviderImpl.java` : le plugin FFM lève une `IOException` si `PwmType.SOFTWARE` est demandé. Il ne parle qu'à l'API PWM matérielle du kernel Linux via chardev.

- [x] `.pwmType(PwmType.SOFTWARE)` → `.pwmType(PwmType.HARDWARE)` dans `GpioHermanasRpiService.provisionPwm()`.
**Choix de la nouvelle broche (décision utilisateur 2026-07-27) :**

La roadmap prévoyait GPIO 18, mais **GPIO 18 est déjà occupé par `door.button.up`**.
Les broches PWM matériel du Pi Zero 2 W sont GPIO **12, 13, 18, 19** ; parmi elles,
12, 13 et 19 sont libres. Retenu : **GPIO 12 (broche physique 32, canal PWM0)** —
un seul fil à déplacer, le bouton haut de porte ne bouge pas.

- [x] **`application.properties` adapté** : `door.servo.gpio.address` **25 → 12**
  (dans `src/main/resources/` et `src/test/resources/`).
- [ ] ⚠️ **Recâblage physique à faire au moment du montage sur le coop (Phase 5.1)** :
  déplacer le fil de signal du servo de la **broche 22** (GPIO 25) vers la **broche 32** (GPIO 12).
  L'alimentation et la masse du servo ne bougent pas.

**Plan GPIO après migration :**

| GPIO | Broche physique | Usage | Remarque |
|---|---|---|---|
| 12 | 32 | `door.servo` | ⚠️ **nouveau** — PWM0 matériel |
| 14 | 8 | `light.relay` | inchangé |
| 15 | 10 | `door.button.bottom` | inchangé |
| 18 | 12 | `door.button.up` | inchangé |
| 23 | 16 | `fan.relay` | inchangé |
| 24 | 18 | `birdhouse.button` | inchangé |
| ~~25~~ | ~~22~~ | — | **libéré** (ancien servo) |

**Résultat de la compilation** : **BUILD SUCCESS** ✅ (0 erreur).

### 1.6 — Compilation itérative

> ⚠️ **Piège Maven / zsh** : le profil `with-frontend` est actif par défaut. Pour le
> désactiver il faut **quoter le `!`**, sinon zsh l'interprète comme une history expansion :
> `-P'!with-frontend'` (et non `-P!with-frontend`, ni `-Pfrontend` qui n'existe pas).

```bash
# Itérer sans re-builder le frontend (npm ci + build Angular coûtent ~1 min)
mvn clean compile -P'!with-frontend' 2>&1 | tail -40

# Build complet une fois tout OK
mvn clean package    # tests + fat JAR (déclenche npm ci + Angular build)
```

#### ⚠️ Blocage majeur rencontré : `Cannot load from object array because "this.hashes" is null`

Symptôme : `mvn compile` échoue immédiatement avec ce message, **sans jamais lister
d'erreur applicative**. Le build s'arrête avant d'avoir analysé le code.

Fausses pistes explorées (~1 h perdue) :
- ❌ Ce n'est **pas** un bug de `javac` Temurin 25 : `javac --release 25` en ligne de
  commande directe compile parfaitement et affiche les vraies erreurs.
- ❌ Ce n'est **pas** lié à Maven 3 vs 4 : reproduit à l'identique sur Maven 3.9.11 et 4.0.0-rc-5.
- ❌ Ce n'est **pas** lié à `maven-compiler-plugin` 3.15.0 vs 3.14.0, ni à `plexus-utils` 4.0.1 vs 3.6.0.
- ❌ Ce n'est **pas** spécifique à Java 25 : le bug est rapporté dès Java 17.

Cause réelle : bug de **`plexus-compiler-javac`**, la couche qui invoque `javac`
*in-process* via l'API `javax.tools`. Rien à voir avec le code du projet.

**Fix** — forcer la compilation dans un process séparé :
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <fork>true</fork>
    </configuration>
</plugin>
```
Références : [maven-compiler-plugin#554](https://github.com/apache/maven-compiler-plugin/issues/554), [plexus-compiler#66](https://github.com/codehaus-plexus/plexus-compiler/issues/66).

**Astuce de diagnostic** : quand Maven masque les erreurs, court-circuiter le plugin pour
obtenir la vraie liste :
```bash
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -P'!with-frontend' -q
find src/main/java -name "*.java" > /tmp/sources.txt
javac --release 25 -cp "$(cat /tmp/cp.txt)" -d /tmp/out @/tmp/sources.txt 2>&1 | head -40
```

#### Progression des erreurs de compilation

| Étape | Erreurs restantes |
|---|---|
| Bump `pom.xml` (SB 4 + Java 25 + pi4j 4) | 100 |
| Après refactor `javax.*` → `jakarta.*` | 12 |
| Après `antMatchers` → `requestMatchers` + pi4j 4.x FFM | 66 *(SB 3 → 4)* |
| Après packages déplacés SB 4 (health, restclient, webmvc) | 8 |
| Après API Security 7 (DSL lambda, DaoAuthenticationProvider, PathPatternRequestMatcher) | **0** ✅ |

- [x] `mvn clean compile -P'!with-frontend'` → **BUILD SUCCESS**
- [x] `mvn clean package` complet (frontend inclus) → **BUILD SUCCESS**, JAR **102 Mo**
  (SPA bundlée : 3284 fichiers statiques, locales `fr-FR` / `en-US` / `ro-RO`).
  *(L'erreur `ENOTEMPTY` sur `frontend/node_modules/@fortawesome/*` rencontrée une fois s'est
  résolue d'elle-même au build suivant ; en cas de récidive :
  `rm -rf frontend/node_modules && cd frontend && npm ci`.)*

### 1.7 — Tests

- [x] `mvn test` : **66/66 tests backend passent** ✅
- [x] Frontend inchangé (npm build tel quel).

**Corrections nécessaires côté tests (SB 4) :**

| Problème | Cause | Fix |
|---|---|---|
| `@MockBean` introuvable (39 occurrences, 9 fichiers) | Supprimé en SB 4 (déprécié depuis 3.4) | → `@MockitoBean` |
| `@DataJpaTest` / `@WebMvcTest` introuvables | Infra de test modularisée | Starters + packages dédiés (cf. 1.2 et 1.5) |
| `PictureRepositoryTest` : `NoSuchBeanDefinitionException: CacheManager` | Slices SB 4 plus strictes ; `@EnableCaching` exige un `CacheManager` que `@DataJpaTest` ne charge pas | `@ImportAutoConfiguration(CacheAutoConfiguration.class)` |
| `IllegalClassFormatException` sur **toutes** les classes | JaCoCo 0.8.12 ne lit pas le bytecode Java 25 | JaCoCo → 0.8.15 |
| `ApplicationStatusListenerTest` : `sendMail` jamais invoqué | Mockito 5.23 a durci le matching des **varargs** : `any()` ne matche plus un appel `EventType...` | → `any(EventType[].class)` |
| `ConfigRestControllerTest` : 401 attendu, 200 puis 500 obtenu | `@WebMvcTest` ne charge plus `SecurityConfig` → les `@PreAuthorize` du contrôleur sont inertes | Config imbriquée `@EnableMethodSecurity` importée dans le test + handlers 401/403 (cf. 1.4bis) |

### 1.8 — Commit sur la branche

- [x] **Commit `e1df962`** sur `feat/pi-zero-2-migration` (67 fichiers, +692 / −292).
- [x] Artefacts de test ajoutés au `.gitignore` (`LOG_FILE_IS_UNDEFINED`, `audit.txt`,
      `audit_config.txt` — logs applicatifs écrits dans le CWD pendant `mvn test`).
- [x] **Push** de la branche (`ed7db4f..7782041`) :
      ```bash
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

### 2.2 — Dump MariaDB (défensif, cross-version 10.3 → 11.x)

> ⚠️ **Trois pièges dans cette commande :**
> 1. **`--set-gtid-purged=OFF` est une option MySQL, pas MariaDB.** Elle fait
>    échouer `mysqldump` avec `unknown variable`. Retirée ci-dessous.
> 2. **Quoter le mot de passe** : il contient des `/`, à protéger du shell —
>    `-p'...'` collé au flag, sans espace.
> 3. **Ne pas copier la commande à partir de `mysqldump`** : le `"` fermant
>    appartient au `ssh`. Sans son `"` ouvrant, le shell reste bloqué à
>    attendre la fin de la chaîne (prompt `>`). Faire `Ctrl+C` et repartir de
>    la commande entière.

**Depuis le Mac ou `pru`** (commande complète avec le `ssh`) :

```bash
ssh -p 5722 pi@poupou "mysqldump \
  --single-transaction \
  --routines --triggers --events \
  --skip-lock-tables \
  --no-tablespaces \
  --default-character-set=utf8mb4 \
  -u pi -p'<password>' hermanas" > /tmp/hermanas-$(date +%F).sql
```

**Ou directement depuis une session SSH ouverte sur `poupou`** (sans le `ssh`) :

```bash
mysqldump \
  --single-transaction \
  --routines --triggers --events \
  --skip-lock-tables \
  --no-tablespaces \
  --default-character-set=utf8mb4 \
  -u pi -p'<password>' hermanas \
  > /tmp/hermanas-$(date +%F).sql
```

- [ ] Vérifier que le dump n'est pas vide et se termine proprement :
  ```bash
  ls -lh /tmp/hermanas-*.sql
  tail -1 /tmp/hermanas-*.sql    # attendu : "-- Dump completed on ..."
  grep -c "CREATE TABLE" /tmp/hermanas-*.sql   # attendu : au moins 4
  ```

Nettoyer les DEFINER absents sur `pru` :

```bash
# macOS (BSD sed) — le '' après -i est obligatoire
sed -i '' 's/DEFINER=`[^`]*`@`[^`]*` //g' /tmp/hermanas-*.sql

# Linux (GNU sed), si le dump est déjà sur poupou ou pru
sed -i 's/DEFINER=`[^`]*`@`[^`]*` //g' /tmp/hermanas-*.sql
```

### 2.3 — Restauration

Si le dump a été généré **depuis une session ouverte sur `poupou`**, il s'y trouve
déjà : le plus simple est de le tirer directement depuis `pru`, sans détour par le Mac.

**Sur `pru`** :

```bash
scp -P 5722 pi@poupou:/tmp/hermanas-$(date +%F).sql /tmp/

# Nettoyer les DEFINER absents sur pru (GNU sed : pas de '' après -i)
sed -i 's/DEFINER=`[^`]*`@`[^`]*` //g' /tmp/hermanas-$(date +%F).sql

mysql -u pi -p'<password>' hermanas < /tmp/hermanas-$(date +%F).sql
```

*(Variante si le dump est passé par le Mac : `scp -P 5722 /tmp/hermanas-*.sql
jean-baptisterenaux@pru.local:/tmp/` puis la même restauration en SSH.)*

### 2.4 — Vérification counts

> ⚠️ **Les deux machines n'ont pas le même user Linux** : `pi` sur `poupou`,
> `jean-baptisterenaux` sur `pru`. En revanche le **user MariaDB reste `pi`**
> des deux côtés (user applicatif, indépendant du user Linux — cf. Phase 0.5),
> donc la requête ci-dessous est rigoureusement identique sur les deux machines.

**À lancer localement sur chaque machine** (plus simple que d'imbriquer les
guillemets dans un `ssh`) :

```bash
mysql -u pi -p'<password>' hermanas -e "
  SELECT 'parameter' AS t, COUNT(*) AS n FROM parameter UNION ALL
  SELECT 'sensor',         COUNT(*)      FROM sensor    UNION ALL
  SELECT 'event',          COUNT(*)      FROM event     UNION ALL
  SELECT 'picture',        COUNT(*)      FROM picture;"
```

*Variante en une passe depuis le Mac, si les deux machines sont joignables —
noter le user Linux qui diffère selon l'hôte :*

```bash
ssh -p 5722 pi@poupou "mysql -u pi -p'<password>' hermanas -e \"SELECT COUNT(*) FROM event;\""
ssh -p 5722 jean-baptisterenaux@pru.local "mysql -u pi -p'<password>' hermanas -e \"SELECT COUNT(*) FROM event;\""
```

- [x] Les 4 counts sont **identiques** entre `poupou` et `pru` (vérifié 2026-07-27 : parameter 26, sensor 27999, event 22075, picture 37329).

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

> ✅ **Décision utilisateur 2026-07-27 : seul `application.properties` est migré.**
> Les trois autres fichiers de la liste d'origine sont devenus inutiles — vérifié
> dans le code avant de les écarter :
>
> | Fichier | Pourquoi il est inutile |
> |---|---|
> | `users.properties` | L'authentification est passée en base. `DbUserDetailsService` ne lit ce fichier que si la table `hermanas_user` est **vide**, en mécanisme de reprise pour les anciennes installations. La base restaurée depuis `poupou` contient les users, le fichier n'est jamais ouvert. |
> | `keystore.p12` | Aucune propriété `server.ssl.*` dans `application.properties` : l'application sert en HTTP simple. Le TLS est terminé par le reverse-proxy sur `shannen`. |
> | `email` | Plus aucune référence dans la configuration ni dans le code. |

**À lancer sur `pru`** (transfert direct `poupou` → `pru`, sans détour par le Mac) :

```bash
for f in application.properties users.properties keystore.p12 email; do
  scp -P 5722 pi@poupou:/home/pi/$f /tmp/
done

sudo mv /tmp/application.properties /tmp/users.properties /tmp/keystore.p12 /tmp/email /var/lib/hermanas/
sudo chown hermanas:hermanas /var/lib/hermanas/{application.properties,users.properties,keystore.p12,email}
sudo chmod 640 /var/lib/hermanas/{application.properties,users.properties,keystore.p12,email}
```

*(Variante depuis le Mac, si `poupou` et `pru` ne se voient pas directement :
`scp -3 -P 5722 pi@poupou:/home/pi/$f jean-baptisterenaux@pru.local:/tmp/` — l'option
`-3` fait transiter le flux par la machine locale.)*

`640` (owner rw, group r) : Hermanas peut lire, aucun autre user.

- [x] **Ajuster les chemins dans `application.properties`** pour pointer vers `/var/lib/hermanas/` au lieu de `/home/pi/` :
  ```bash
  ssh -p 5722 jean-baptisterenaux@pru.local
  sudo sed -i 's|/home/pi/|/var/lib/hermanas/|g' /var/lib/hermanas/application.properties
  # Vérifier :
  sudo grep -E "camera\.path|music\.path|hermanas\.security\.users-file" /var/lib/hermanas/application.properties
  ```
  Attention : cette commande remplace TOUS les `/home/pi/` du fichier. À valider avant, il peut y avoir des cas de bord (chemins qui doivent rester en `/home/pi/` par erreur historique).
- [x] ✅ **`door.servo.gpio.address` : rien à corriger ici** (vérifié 2026-07-27).
  La clé est **absente** de l'`application.properties` externe hérité de `poupou`.
  C'est donc la valeur embarquée dans le JAR qui s'applique, et elle vaut déjà **12**
  (corrigée en Phase 1.5bis). Elle est injectée par `@Value` dans `ServoMotorService`
  et `ElectronicsRestController` — aucune valeur codée en dur dans le code.
  ```bash
  # Contrôle : ne doit RIEN renvoyer
  sudo grep "door.servo.gpio.address" /var/lib/hermanas/application.properties
  ```
  ⚠️ Si cette clé venait à être ajoutée au fichier externe, elle **surchargerait** le
  JAR : il faudrait alors y écrire `12`, jamais `25`.
  *(Ne pas confondre avec `SERVO_OPENING_MAX_POSITION = 25` dans `ServoMotorService` :
  c'est une position angulaire du servo, pas un numéro de broche.)*
- [x] `keystore.p12` **non migré** — décision confirmée par la configuration :
  `application.properties` fixe `server.port = 8080` et ne déclare aucune propriété
  `server.ssl.*`. Hermanas sert donc en **HTTP simple**, le TLS étant terminé par
  `shannen`. Le basculement redouté comme « chantier séparé » était en réalité déjà
  effectif.

### 2.7 — Config MariaDB custom (optionnel)

```bash
ls /etc/mysql/mariadb.conf.d/
# S'il y a des fichiers autres que les 50-*.cnf par défaut :
scp -P 5722 pi@poupou:/etc/mysql/mariadb.conf.d/<file>.cnf /tmp/    # depuis pru
# Diff avant d'écraser sur pru
```

- [x] ✅ **Rien à migrer** (vérifié 2026-07-27). `poupou` ne contient que les quatre
  fichiers livrés par le paquet Debian — `50-client.cnf`, `50-mysql-clients.cnf`,
  `50-mysqld_safe.cnf`, `50-server.cnf` — sans aucun ajout custom.
- [x] *Contrôle complémentaire* : `50-server.cnf` de `poupou` inspecté (2026-07-27).
  Deux directives s'écartent du défaut Debian, **aucune des deux n'est à reproduire** :

  | Directive sur `poupou` | Décision pour `pru` | Raison |
  |---|---|---|
  | `bind-address = 0.0.0.0` | ✅ **à reproduire** (décision utilisateur 2026-07-27) | La base est administrée via **Adminer** depuis le LAN. Un tunnel SSH serait contraignant pour un usage d'admin régulier. |
  | `query_cache_size = 16M` | ❌ **ne pas reproduire** | Le query cache est déprécié depuis MariaDB 10.1 et **supprimé en 10.6+**. Sur MariaDB 11.x la directive est ignorée, voire refusée au démarrage. |

  Le reste (`expire_logs_days`, `character-set-server = utf8mb4`, `collation-server`)
  est standard et déjà couvert par les défauts de MariaDB 11.

  ```bash
  # Sur pru : ouvrir MariaDB au LAN pour Adminer
  sudo sed -i 's|^bind-address.*|bind-address            = 0.0.0.0|' \
    /etc/mysql/mariadb.conf.d/50-server.cnf
  sudo systemctl restart mariadb
  ss -tlnp | grep 3306    # attendu : 0.0.0.0:3306
  ```

- [x] ⚠️ **User MariaDB pour l'accès distant.** `bind-address = 0.0.0.0` ne suffit pas :
  le user créé en Phase 0.5 est `'pi'@'localhost'` et refuse toute connexion venant
  d'une autre machine. `poupou` déclare en plus un **`pi@192.168.1.%`** (constaté
  2026-07-27) — c'est lui qu'utilise Adminer. À reproduire sur `pru` :
  ```bash
  sudo mysql <<'SQL'
  CREATE USER IF NOT EXISTS 'pi'@'192.168.1.%' IDENTIFIED BY '<password>';
  GRANT ALL PRIVILEGES ON hermanas.* TO 'pi'@'192.168.1.%';
  FLUSH PRIVILEGES;
  SQL
  ```
  Le masque `'192.168.1.%'` restreint au LAN — le conserver plutôt que d'élargir à `'%'`.

  **Comment le diagnostic a été mené** — sur `poupou`, `pi` n'a pas le privilège de lire
  `mysql.user` (`ERROR 1142`), et `sudo mysql` échoue en 10.3 qui n'active pas
  l'authentification `unix_socket` pour root (contrairement à MariaDB 11 sur `pru`).
  Contournement : se connecter depuis `pru` et demander au serveur quel compte a matché.
  ```bash
  # Sur pru
  mysql --skip-ssl -h poupou -u pi -p'<password>' -e "SELECT CURRENT_USER();"
  # → pi@192.168.1.%
  ```

- [x] ⚠️ **TLS : le client MariaDB 11 exige SSL par défaut.** Depuis la 11.4, une
  connexion vers un serveur sans TLS échoue avec
  `TLS/SSL error: SSL is required, but the server does not support it`.
  C'est une exigence **du client**, pas un refus d'authentification — d'où le
  `--skip-ssl` ci-dessus pour interroger `poupou` (MariaDB 10.3).
  **À anticiper pour Adminer → `pru`** : selon la version de son client, il faudra soit
  activer TLS sur `pru`, soit désactiver l'exigence côté Adminer.

- [x] Test croisé après configuration ✅ (2026-07-27, `poupou` → `pru` renvoie `pi@192.168.1.%`) :
  ```bash
  # Depuis poupou (client 10.3, pas d'exigence TLS)
  mysql -h pru.local -u pi -p'<password>' -e "SELECT CURRENT_USER();"
  # attendu : pi@192.168.1.%
  ```

  ℹ️ **Collation** : `poupou` est en `utf8mb4_general_ci`, MariaDB 11 utilise
  `utf8mb4_uca1400_ai_ci` par défaut. Le dump restauré conserve les collations
  d'origine table par table — pas de casse. Seule une table créée *ex nihilo* par
  Hibernate prendrait la collation moderne ; sans impact ici, le schéma étant figé.

---

## Phase 3 — Test à vide sur banc

Objectif : `pru` démarre le JAR Java 25 / SB4 / pi4j 4.x avec les vraies données, en profil `gpio-fake`, sans être branchée au coop.

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
  --spring.config.additional-location=file:/var/lib/hermanas/application.properties
```

**Attendus :**

- [ ] `Started HermanasApplication in XX seconds` — noter la valeur (baseline).
- [ ] `UnsatisfiedLinkError` sur `picam-2.0.1.so` : **normal** (catché par le service GPIO).
- [ ] `MusicService` peut throw (cvlc absent) : **normal**.
- [ ] Si `> 90 s` : envisager `arm_freq=800` au lieu de 600.

#### ⚠️ Écueil rencontré : entropie insuffisante → démarrage de 5 minutes

Symptôme trompeur : démarrage extrêmement lent alors que `htop` montre un **CPU
quasi inactif**. L'application n'est pas en train de calculer, elle *attend*.

```bash
cat /proc/sys/kernel/random/entropy_avail
# 256 le 2026-07-27 — très bas
```

La JVM et Tomcat réclament de l'aléatoire sécurisé au démarrage (`SecureRandom`),
et `/dev/random` bloque tant que le pool est vide. Un Pi headless, sans clavier ni
souris, en produit très peu.

- [ ] Alimenter le pool avec `haveged` :
  ```bash
  sudo apt install -y haveged
  sudo systemctl enable --now haveged
  cat /proc/sys/kernel/random/entropy_avail   # doit passer à plusieurs milliers
  ```
- [ ] Ajouter l'option JVM dans `Hermanas.sh` (cf. Phase 0.3) :
  ```
  -Djava.security.egd=file:/dev/./urandom
  ```
  ⚠️ Le `/./` est indispensable — sans lui la JVM ignore silencieusement l'option.

#### ⚠️ Écueil rencontré : resilience4j vérifie la version de Spring Boot au démarrage

```
APPLICATION FAILED TO START
Module 'io.github.resilience4j:resilience4j-spring-boot3' is only compatible
with Spring Boot 3.x
Action: Update your project to use 'io.github.resilience4j:resilience4j-spring-boot4'
```

L'artifactId de resilience4j encode la ligne Spring Boot ciblée, et le module
embarque un `SpringBoot3Verifier` qui **échoue au démarrage** si elle ne correspond
pas. Le projet compilait pourtant sans erreur avec `-spring-boot3` : rien ne se voit
avant l'exécution.

- [x] Corrigé : `resilience4j-spring-boot3` → **`resilience4j-spring-boot4`**, même
  version 2.4.0. Aucun changement de code — l'annotation `@CircuitBreaker` de
  `WeatherClient` conserve son package `io.github.resilience4j.circuitbreaker.annotation`.

#### ⚠️ Écueil rencontré : le PWM FFM exige chip/channel, pas `.bcm()`

```
IllegalArgumentException: PWM Chip and Channel are needed for hardware PWM
                          with the FFM I/O provider
```

Le provider PWM du plugin FFM **refuse `.bcm()`**. Il attend l'adressage sysfs du
kernel (`/sys/class/pwm/pwmchipN/pwmM`), soit un couple `chip` + `channel`, et non
une numérotation GPIO — contrairement aux providers digital input/output qui, eux,
acceptent `.bcm()`.

- [x] Corrigé dans `GpioHermanasRpiService.provisionPwm()` : une table
  `BCM_TO_PWM_CHANNEL` traduit la broche en canal. La signature de l'interface reste
  inchangée (`provisionPwm(id, name, gpioAddress)`), la traduction étant un détail
  d'implémentation du provider.

  | BCM | Canal | Remarque |
  |---|---|---|
  | 12 | PWM0 | **retenu pour le servo** (broche physique 32) |
  | 18 | PWM0 | occupé par `door.button.up` |
  | 13 | PWM1 | libre, repli possible |
  | 19 | PWM1 | libre, repli possible |

  Le Pi Zero 2 W n'expose qu'un seul contrôleur, donc `chip = 0`. Une broche hors de
  cette table lève désormais une `IllegalArgumentException` explicite plutôt qu'un
  message du plugin.

- [ ] ⚠️ **Activer le PWM matériel dans `/boot/firmware/config.txt`.** Sans overlay,
  `/sys/class/pwm/` est vide et aucun chip n'existe — le provider échouera même avec
  le bon couple chip/channel.
  ```bash
  # Vérifier d'abord
  ls -la /sys/class/pwm/
  grep -E "^dtoverlay=pwm" /boot/firmware/config.txt

  # Activer PWM0 sur GPIO 12 (func=4 = ALT0)
  echo "dtoverlay=pwm,pin=12,func=4" | sudo tee -a /boot/firmware/config.txt
  sudo reboot
  ```
  Après redémarrage, `/sys/class/pwm/pwmchip0/` doit exister.

#### ⚠️ Écueil rencontré : la caméra faisait tomber tout le GPIO

`GpioHermanasRpiService.initialiseGpioPins()` chargeait la librairie native picam
**et** initialisait le contexte pi4j dans le même bloc `try`. Le `.so` étant absent
sur `pru` (chemin `/home/pi/` hérité de `poupou`), l'`UnsatisfiedLinkError`
interrompait le bloc avant `Pi4J.newAutoContext()` — `pi4j` restait `null`, et le
premier service GPIO à démarrer échouait :

```
NullPointerException: Cannot invoke "com.pi4j.context.Context.create(...)"
  because "this.pi4j" is null
    at LightService.init(LightService.java:66)
```

Autrement dit : porte, lumière et ventilateur tombaient à cause d'un problème de
**caméra**, alors que celle-ci est explicitement acceptée KO jusqu'à la Phase 7.

- [x] Corrigé : les deux initialisations sont désormais dans des `try` séparés, et
  `tearDown()` vérifie que `pi4j` n'est pas `null` avant d'appeler `shutdown()`
  (la NPE masquait l'erreur d'origine dans les logs).
- [ ] Vérifier la propriété `camera.picam.jni.implementation` dans
  `/var/lib/hermanas/application.properties` : si elle pointe encore vers
  `/home/pi/`, la corriger ou la retirer. Le chemin n'existe pas sur `pru`.

#### ⚠️ Écueil rencontré : `config.location` vs `config.additional-location`

Premier démarrage soldé par un échec (2026-07-27) :

```
Could not resolve placeholder 'light.security.timer.delay.eco'
```

`Hermanas.sh` utilisait `--spring.config.location`, qui **remplace** la configuration
embarquée dans le JAR au lieu de la compléter. Or l'`application.properties` hérité de
`poupou` ne contient qu'une soixantaine de propriétés, contre **165** dans le JAR :
tout le reste disparaissait, et l'application échouait sur la première propriété
introuvable.

- [x] Corrigé en `--spring.config.additional-location` :
  ```bash
  sudo sed -i 's|--spring.config.location=|--spring.config.additional-location=|' \
    /usr/local/bin/Hermanas.sh
  ```

Le fichier externe ne doit contenir que ce qui diffère du JAR — identifiants de base,
chemins, coordonnées GPS, clés SMTP — et surcharge alors ponctuellement les défauts
embarqués.

- [ ] Contrôle après correction : le fichier externe est bien plus court que celui
  du JAR, c'est normal et voulu.
  ```bash
  sed -nE 's/^([a-zA-Z][a-zA-Z0-9._-]*) *=.*/\1/p' \
    /var/lib/hermanas/application.properties | sort -u | wc -l
  ```

#### ⚠️ Risque principal : `ddl-auto=update` avec Hibernate 7

`application.properties` (hérité de `poupou`) contient
`spring.jpa.hibernate.ddl-auto=update`. Au démarrage, Hibernate 7 compare le schéma
restauré — généré par **Hibernate 5.6** sous Spring Boot 2.7 — avec ce qu'il attend,
et **modifie la base** pour combler les écarts. Or la génération de types a changé
entre les deux versions.

C'est le risque le plus sérieux de ce premier démarrage : la base vient d'être
restaurée et contient 87 000 lignes de données réelles.

- [ ] **Surveiller les DDL émises** pendant le démarrage :
  ```bash
  sudo journalctl -u Hermanas.service -f | grep -iE "alter table|drop |create table|hbm2ddl"
  ```
- [ ] Un `alter table` isolé sur un type de colonne est acceptable. **Un `drop` ne l'est
  jamais** — couper immédiatement et repasser en `validate`.
- [ ] *Option prudente pour ce premier lancement* : forcer `validate` en ligne de
  commande, qui vérifie sans rien modifier. Si le schéma est conforme, remettre
  `update` ensuite.
  ```bash
  # ajouter à la commande de démarrage manuel
  --spring.jpa.hibernate.ddl-auto=validate
  ```
  ⚠️ En `validate`, l'application **refuse de démarrer** si le schéma diverge — c'est
  précisément l'intérêt : on voit l'écart au lieu de le subir.

**Autres corrections attendues** : properties SB2→SB4 renommées, dialecte Hibernate
pour MariaDB 11.

> ℹ️ **Propriétés vérifiées avant migration (2026-07-27)** — aucune ne casse en SB4 :
> `spring.mail.*`, `spring.datasource.*`, `spring.jpa.hibernate.ddl-auto`,
> `spring.mvc.servlet.load-on-startup`, `server.port`, `server.forward-headers-strategy`.
>
> À noter : **`server.port = 8080`** et aucune propriété `server.ssl.*` — l'application
> sert en **HTTP simple**, le TLS étant terminé par `shannen`. Les commandes de test
> doivent donc viser `http://…:8080`, pas `https://…:8443`.

### 3.2bis — Empreinte mémoire : préserver la carte SD

> **Contraintes du site (décision utilisateur 2026-07-27), par ordre de priorité :**
> 1. **Éviter le swap** — chaque écriture use la carte SD, dont le remplacement
>    impose un déplacement physique.
> 2. **Limiter la consommation** — la machine est alimentée par panneau solaire.
>    Le bridage `arm_freq=600` est donc **maintenu** : le remonter accélérerait le
>    démarrage mais consommerait davantage, à contre-emploi.
> 3. **La vitesse de démarrage est secondaire** — elle ne pèse qu'au redémarrage
>    hebdomadaire du mercredi.

**Diagnostic (2026-07-27).** `vmstat` pendant le démarrage montrait `wa` à 90-100 %
et `us` à 0 % : le CPU n'était pas sollicité, il attendait la carte SD. 538 Mo
swappés, dont 191 pour la seule JVM.

La cause est structurelle : Spring Boot 4 + Hibernate 7 + Tomcat 11 sont
sensiblement plus lourds que la pile SB 2.7 d'origine, sur une machine dont la RAM
n'a pas changé (415 Mo, partagés avec MariaDB).

#### a. MariaDB — le plus gros gisement

```bash
sudo tee /etc/mysql/mariadb.conf.d/60-low-memory.cnf > /dev/null <<'EOF'
[mysqld]
innodb_buffer_pool_size = 32M
key_buffer_size         = 8M
max_connections         = 20
table_open_cache        = 64
performance_schema      = OFF
EOF
sudo systemctl restart mariadb
```

`performance_schema = OFF` libère à lui seul plusieurs dizaines de Mo.
**Résultat mesuré : swap 526 Mo → 178 Mo au repos.**

#### b. JVM — plafonner chaque poste, pas seulement le heap

Le heap ne représente que la moitié de l'empreinte. Mesuré avec `-Xmx192m` :

```
VmRSS  : 133 Mo      VmSwap : 191 Mo      → 324 Mo au total
```

Les ~130 Mo hors heap se répartissent entre metaspace (Spring Boot 4 charge
énormément de classes), cache de code JIT, piles de threads et buffers directs.
Réduire le seul `-Xmx` les laisse intacts. Voir le bloc `JVM_OPTS` de la Phase 0.3
pour les plafonds retenus.

#### c. Beans lazy — mais sélectivement

- [x] **`spring.main.lazy-initialization=true` global : à ne PAS utiliser.**
  17 classes portent un `@PostConstruct`, dont `LightService`, `ServoMotorService`,
  les boutons de porte et `SunTimeUtils`. En lazy global, ces initialisations
  n'ont lieu qu'au premier accès HTTP — sans visiteur sur l'interface, **la porte
  ne s'ouvrirait jamais au lever du soleil**.
- [x] **`@Lazy` appliqué aux 23 contrôleurs REST**, aucun n'ayant de
  `@PostConstruct` : ils ne servent qu'aux requêtes HTTP. Les automatismes
  (porte, lumière, capteurs, scheduler solaire) restent initialisés au démarrage.
  Le premier appel à un endpoint est un peu plus lent, une seule fois.

#### d. vm.swappiness

```bash
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
sudo sysctl -p /etc/sysctl.d/99-swappiness.conf
```

Défaut : 60 — le noyau swappe volontiers. À 10, il ne le fait qu'en dernier recours.

#### e. Session graphique retirée

- [x] `sudo systemctl set-default multi-user.target` — l'image installée était une
  **Desktop** alors que la roadmap prévoyait une **Lite**. `labwc`, `wf-panel-pi`,
  `pcmanfm`, `gvfs` et `polkit` tournaient en permanence sur une machine headless.
  Gain modeste en RAM (~10 Mo résidents), mais surtout du cache disque récupéré.

#### g. Mémoire GPU réduite — +48 Mo de RAM

Le SoC Broadcom partage sa RAM entre le processeur ARM et le VideoCore. La part
allouée au GPU est réservée par le firmware **avant** que Linux ne démarre : elle
n'apparaît donc jamais dans `free -h`. C'est pourquoi le Pi Zero 2 W, annoncé à
512 Mo, n'en expose que 415.

```bash
vcgencmd get_mem gpu    # gpu=64M sur pru (2026-07-27)
```

64 Mo pour un GPU qu'une machine headless n'utilise pas.

- [x] Réduire au plancher firmware :
  ```bash
  echo "gpu_mem=16" | sudo tee -a /boot/firmware/config.txt
  sudo reboot
  vcgencmd get_mem gpu    # attendu : gpu=16M
  free -h                 # total : 415 → ~463 Mo
  ```

⚠️ **À rouvrir en Phase 7.** `rpicam-still` réclame typiquement 64 Mo de mémoire
GPU. Il faudra remonter `gpu_mem` quand la caméra sera remise en service — la
caméra étant de toute façon hors service jusque-là, ces 48 Mo sont un gain
immédiat, à rendre plus tard.

#### f. Swapfile SD supprimé — zram seul ✅

`swapon --show` liste deux espaces : `/dev/zram0` (415 Mo, compressé en RAM,
priorité 100) et `/var/swap` (850 Mo, **sur la carte SD**, priorité -2). Le noyau
remplit zram d'abord, puis déborde sur le fichier — c'est ce débordement qui use
le support.

**Mesure décisive (2026-07-27)** — `zramctl` révèle un taux de compression bien
meilleur qu'attendu sur le heap Java :

```
NAME       ALGORITHM DISKSIZE   DATA COMPR TOTAL
/dev/zram0 zstd        207.5M 162.2M 35.1M  38.5M     → ratio 4,6:1
```

162 Mo de pages swappées n'occupaient que 38 Mo de RAM réelle. Mais zram était
**saturé** (204,4 Mo utilisés sur 207,5) et sa taille mal calibrée — d'où le
débordement sur `/var/swap`, où 209 Mo étaient écrits sur la carte.

- [x] **Agrandir zram à 2× la RAM.** Avec le ratio constaté, 830 Mo de pages ne
  coûtent qu'environ 180 Mo de mémoire réelle au pire.
  ```bash
  sudo tee /etc/systemd/zram-generator.conf > /dev/null <<'EOF'
  [zram0]
  zram-size = ram * 2
  compression-algorithm = zstd
  EOF
  sudo systemctl daemon-reload
  sudo reboot
  ```
- [x] **Supprimer le swapfile disque** :
  ```bash
  sudo swapoff /var/swap
  sudo systemctl disable --now dphys-swapfile
  sudo rm -f /var/swap
  sudo sed -i '/\/var\/swap/d' /etc/fstab   # sinon il revient au boot
  swapon --show                              # ne doit lister que /dev/zram0
  ```
  ⚠️ **Ordre important** : agrandir zram **avant** de retirer le swapfile, sinon il
  ne reste que 207 Mo de swap total.
  ⚠️ `swapoff` échoue avec `Cannot allocate memory` s'il n'y a pas assez de RAM libre
  pour rapatrier le contenu du fichier — arrêter Hermanas d'abord, ou simplement
  redémarrer (le reboot purge le swapfile).

**Résultat : `/dev/zram0` 830 Mo, aucun swap disque. Plus une seule écriture de swap
sur la carte SD.**

⚠️ Sans filet disque, un dépassement mémoire déclenche un `OOM kill` au lieu d'un
ralentissement. Combiné à `-XX:+ExitOnOutOfMemoryError` et au `Restart=on-failure`
du unit, cela donne un redémarrage propre plutôt qu'une usure continue — c'est le
compromis voulu.

### 3.3 — Sanity check applicatif

> ⚠️ **HTTP sur le port 8080**, pas HTTPS sur 8443 : `application.properties` fixe
> `server.port = 8080` et ne déclare aucune propriété `server.ssl.*` — le TLS est
> terminé par `shannen`.

```bash
curl http://pru.local:8080/actuator/health
curl -c /tmp/cookies.txt -X POST http://pru.local:8080/api/v1/auth/login \
  -d "username=<user>&password=<password>"
curl -b /tmp/cookies.txt http://pru.local:8080/api/v1/sensor/info
```

- [ ] `/actuator/health` : `UP` (porte `DOWN` accepté en `gpio-fake`).
- [ ] Login OK, session valide.
- [ ] `/sensor/info` renvoie l'historique restauré.
- [ ] Frontend charge dans un navigateur (`http://pru.local:8080/`).

#### Vérification JMX

La configuration JMX de `Hermanas.sh` a été corrigée en Phase 0.3 mais **jamais
exécutée** — `local.only=true` empêchait le connecteur de démarrer, remplacé par
`jmxremote.host=127.0.0.1`. Premier test réel ici.

- [ ] Le connecteur écoute, et **seulement sur la boucle locale** :
  ```bash
  ss -tlnp | grep 9010
  # attendu : 127.0.0.1:9010 — surtout PAS 0.0.0.0:9010
  ```
- [ ] Aucune erreur JMX au démarrage :
  ```bash
  sudo journalctl -u Hermanas.service | grep -iE "jmx|rmi|9010"
  ```
- [ ] Connexion depuis le Mac via tunnel SSH :
  ```bash
  ssh -p 5722 -L 9010:localhost:9010 jean-baptisterenaux@pru.local
  # puis, dans une autre session : jconsole localhost:9010
  ```

> ℹ️ **`PrivateTmp=yes` dans le unit systemd** isole le `/tmp` du service. Le fichier
> de découverte JVM (`/tmp/hsperfdata_hermanas/`) n'est donc pas visible depuis ta
> session SSH : `jcmd -l` et `jps` ne listeront pas le process, et l'attachement local
> de `jconsole` ne fonctionnera pas. **La connexion par le port 9010 reste opérationnelle** —
> c'est la voie à utiliser.

> ℹ️ **`authenticate=false`** est acceptable *uniquement* parce que le connecteur est
> restreint à `127.0.0.1` : il faut déjà un accès SSH à la machine pour l'atteindre. À
> garder en tête : quiconque obtient un shell sur `pru` peut alors invoquer des MBeans,
> donc modifier la configuration à chaud voire arrêter l'application. Compromis assumé
> pour une machine mono-service en réseau domestique.

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
| **12** | **32** | **Servomoteur de la porte** | **output (PWM matériel)** |
| 18 | 12 | Bouton de fin de course haut de la porte | input |
| 15 | 10 | Bouton de fin de course bas de la porte | input |
| 24 | 18 | Bouton poussoir inversé de la lumière du nichoir | input |
| 14 | 8 | Relais de l'éclairage | output |
| 23 | 16 | Relais du ventilateur | output |
| 4 | 7 | Capteur de température et d'humidité (DHT22) | input (1-wire) |

#### 🔧 Recâblage : UN SEUL fil à déplacer

Tous les fils se rebranchent à l'identique **sauf un** : le fil de **signal du
servomoteur** (le fil de commande, généralement **orange** ou **jaune** sur un servo
type SG90/MG996R — pas le rouge ni le marron/noir).

| | Avant (`poupou`) | Après (`pru`) |
|---|---|---|
| **Fil concerné** | Signal servo (orange/jaune) | idem |
| **Broche physique** | **22** | **32** |
| **GPIO (BCM)** | 25 | 12 |
| **Repère visuel** | 11ᵉ broche, rangée intérieure | 16ᵉ broche, rangée extérieure |

**Les deux autres fils du servo ne bougent pas :**

| Fil servo | Broche | Rôle |
|---|---|---|
| Rouge | 2 ou 4 (5 V) | alimentation — **inchangé** |
| Marron / noir | 6, 9, 14, 20… (GND) | masse — **inchangé** |

**Repérage de la broche 32 sur le header 40 pins** — les broches paires sont sur la
rangée extérieure (côté bord de carte), les impaires sur la rangée intérieure. La
broche 32 est la **16ᵉ paire** en partant du connecteur d'alimentation :

```
        rangée intérieure (impaires)          rangée extérieure (paires)
                 ...                                    ...
        21 ─────────────────────                22 ────────  ← ANCIENNE (GPIO 25)
        23 ─────────────────────                24
        25 ─────────────────────                26
        27 ─────────────────────                28
        29 ─────────────────────                30
        31 ─────────────────────                32 ────────  ← NOUVELLE (GPIO 12)
        33 ─────────────────────                34
                 ...                                    ...
```

- [ ] **Débrancher** le fil de signal du servo de la broche 22.
- [ ] **Rebrancher** ce même fil sur la broche 32.
- [ ] Vérifier que rien d'autre n'a bougé (comparer avec la table de câblage ci-dessus).
- [ ] ⚠️ Faire ce déplacement **`pru` hors tension**, puis rebrancher l'alimentation.

> **Pourquoi ce changement ?** `pi4j-plugin-ffm` (pi4j 4.x) ne gère que le PWM
> **matériel** — le PWM logiciel de pigpio n'existe plus. Sur Pi Zero 2 W, seuls
> GPIO 12, 13, 18 et 19 exposent un canal PWM matériel ; GPIO 25 n'en fait pas
> partie. GPIO 18 étant déjà pris par le bouton haut de porte, le servo va sur
> GPIO 12. Cf. Phase 1.5bis.

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
      ⚠️ **Premier test du PWM matériel sur GPIO 12** — c'est le changement le plus
      risqué de la migration. Si le servo ne bouge pas ou vibre : vérifier que le fil
      est bien sur la broche 32, puis tester GPIO 13 (broche 33) ou GPIO 19 (broche 35),
      les deux autres canaux PWM matériel libres. Ajuster `door.servo.gpio.range` si la
      course est incorrecte — la résolution PWM matérielle diffère de l'ancien PWM logiciel.
- [ ] Lumière : `/api/v1/light/switch` → relais entendu, lumière visible.
- [ ] Ventilateur : `/api/v1/fan/switch`.
- [ ] Capteur T°/humidité : `/api/v1/sensor/info` → valeurs cohérentes.
- [ ] Boutons physiques : appuis testés, événements dans les logs.
- [ ] **Camera KO** (attendu) : `/api/v1/camera/*` renvoie 500.
- [ ] **Musique KO** (attendu) : `/api/v1/music/*` renvoie 500.

### 5.4 — Vérifications monitoring

**Deux sources de métriques distinctes**, à ne pas confondre :

| Source | Port | Couvre | Fourni par |
|---|---|---|---|
| `prometheus-node-exporter` | `:9100/metrics` | Système : CPU, RAM, disque, réseau, température SoC | Paquet apt (Phase 0.7) |
| `/actuator/prometheus` | `:8080` (Hermanas, HTTP) | Applicatif : `hermanas.door.*`, `hermanas.sensor.*`, JVM, HTTP | `micrometer-registry-prometheus` |

> ⚠️ **Bug latent corrigé pendant la migration.** `application.properties` exposait
> `prometheus` dans `management.endpoints.web.exposure.include`, mais la dépendance
> `micrometer-registry-prometheus` n'était **pas** déclarée dans le `pom.xml` :
> l'endpoint renvoyait donc **404**, et les métriques de `HermanasMetrics` (compteurs
> de porte, températures, sessions caméra) n'étaient scrapables par personne. Elles
> n'existaient qu'en JSON sur `/actuator/metrics/hermanas.*`, que Prometheus ne lit pas.
> La dépendance a été ajoutée au `pom.xml` (version gérée par le BOM Spring Boot).

- [ ] `https://www.hermanas.fr/` charge depuis Internet.
- [ ] Login public OK.
- [ ] **node-exporter** répond, depuis `shannen` :
  ```bash
  curl -s http://poupou:9100/metrics | head -3
  ```
- [ ] **Endpoint applicatif** répond au format Prometheus (texte, pas JSON) :
  ```bash
  # Depuis pru — HTTP sur 8080 (server.port=8080, pas de keystore : TLS terminé par shannen)
  curl -s -u '<admin>:<password>' http://localhost:8080/actuator/prometheus \
    | grep -E "^hermanas_" | head
  # attendu : hermanas_door_..., hermanas_sensor_... — et surtout PAS un 404
  ```
- [ ] **Prometheus scrape bien les deux cibles** — vérifier côté `shannen` que le job
  Hermanas existe dans `prometheus.yml`. Si seul le `:9100` (node-exporter) y figure,
  ajouter la cible applicative décrite ci-dessous.

  ⚠️ **L'endpoint est protégé** : `/actuator/**` exige `ROLE_ADMIN` hors `/health` et
  `/info` (cf. `SecurityConfig`) — le scrape recevra un **401** sans authentification.

  **Décision : ajouter `basic_auth` au job Prometheus, ne pas ouvrir l'endpoint.**
  Le fichier `prometheus.yml` est déjà sur `shannen` en accès root ; y placer un
  identifiant admin coûte moins que d'élargir la surface exposée par l'application.
  ```yaml
  - job_name: 'hermanas-app'
    metrics_path: '/actuator/prometheus'
    scheme: http          # server.port=8080, pas de TLS local
    basic_auth:
      username: '<admin>'
      password: '<password>'
    static_configs:
      - targets: ['poupou:8080']
  ```
  *(L'alternative — rendre `/actuator/prometheus` public — a été écartée. L'endpoint
  n'expose que des compteurs et jauges, sans secret, et le port 8080 n'est pas routé
  depuis Internet ; mais les noms d'URI renseignent sur la structure de l'API et les
  métriques JVM sur le dimensionnement de la machine. Aucune raison de s'en priver
  quand `basic_auth` ne coûte rien.)*
- [ ] Grafana (https://grafana.r3n4.uk) :
  - Dashboards **système** (CPU, RAM, disque, réseau) : points récents, pas de trou > 5 min.
  - Dashboards **Hermanas** (`hermanas.door.*`, `hermanas.sensor.*`) : points récents.
  - ⚠️ **Renommages Micrometer** : la migration saute Micrometer 1.9 → 1.17. Certaines
    métriques JVM et HTTP ont pu changer de nom (`http_server_requests_*`,
    `jvm_memory_*`). Si un panel se vide alors que l'endpoint répond, comparer les noms
    exposés avec ceux des requêtes PromQL du dashboard.

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

- [ ] `spring-boot-starter-parent` : minor bumps SB4.
- [ ] `pi4j` : dernière 2.x.
- [ ] `mariadb-java-client` : dernière compatible SB4.
- [ ] `micrometer-*` : dernière.
- [ ] `commons-io`, `commons-lang3`, `lib-sunrise-sunset` : dernières.

### 6.2 — Dépendances frontend

```bash
cd frontend
npm outdated
npm audit
```

> ℹ️ **État constaté le 2026-07-27.** Cette section a été réécrite : elle listait
> encore Angular 21 → 22 comme chantier à venir et `uuid` comme dépendance directe,
> alors que le projet est déjà en Angular 22 et que `uuid` n'apparaît plus dans
> `package.json`. Les points ci-dessous sont vérifiés.

**Versions en place** : Angular 22.0.2, CDK/Material 22.0.2, TypeScript 6.0.3,
zone.js 0.16.2, Vitest 4.1.8, ESLint 10.5.0, ng-bootstrap 21.0.0-rc.0.

#### A. Dépréciations signalées à chaque `npm ci`

- [ ] **`@angular-devkit/build-angular` → `@angular/build`** — le support Webpack
  d'Angular est déprécié. `angular.json` utilise encore le builder
  `@angular-devkit/build-angular:browser`, à remplacer par `@angular/build:application`.
  C'est le chantier frontend le plus lourd : nouveau système de build (esbuild + Vite),
  configuration à reprendre, et le `--localize` sur trois locales doit être revalidé.
  Gain attendu : build nettement plus rapide.
  Voir https://angular.dev/tools/cli/build-system-migration
- [x] ✅ **`@angular/platform-browser-dynamic` : code déjà migré, paquet à retirer.**
  Vérifié le 2026-07-27 : `src/main.ts` utilise déjà `bootstrapApplication` depuis
  `@angular/platform-browser`, et **aucun fichier du projet n'importe
  `platform-browser-dynamic`**. Le paquet n'apparaît plus que dans `package.json` —
  c'est un reliquat. Il suffit de l'y supprimer :
  ```bash
  npm uninstall @angular/platform-browser-dynamic
  ```
  ⚠️ Vérifier ensuite que `npm test` passe (Vitest peut s'en servir indirectement).
- [ ] **`@angular/animations` déprécié** — remplacé par `animate.enter` /
  `animate.leave` (nouvelle API Angular 22). **4 composants concernés**, tous dans
  `app-common` : `toast-container`, `offline-banner`, `loading-spinner`,
  `pwa-install-banner`. Ils utilisent `trigger`/`style`/`transition`/`animate`.
  Périmètre restreint, donc migration abordable — mais validation visuelle nécessaire.
- [ ] **`flag-icon-css@3.5.0` → `flag-icons`** — projet renommé. Utilisé dans
  `top-nav-lang.component.html` (drapeaux gb/fr/ro). Changement de nom de paquet et
  probablement de préfixe CSS (`flag-icon-*` → `fi-*`), donc retouche du template.

#### B. Sass : `@import` déprécié

- [ ] **33 fichiers `.scss`** utilisent `@import`, supprimé dans Dart Sass 3.0.
  Détail vérifié : **31** font `@import 'styles/variables.scss'` (cas trivial,
  mécanisable), les autres importent des partiels de `styles/` (`navigation/`,
  `layout/`). Aucun `@use` n'est encore employé dans le projet.
  Un migrateur automatique existe : https://sass-lang.com/d/import
  Attention : `@use` a une portée différente d'`@import` (pas de fuite globale des
  variables), donc à vérifier visuellement après conversion.

  ⚠️ **Ordre conseillé : traiter le point A avant celui-ci.** Ces avertissements sont
  émis par `sass-loader`, un composant de la chaîne Webpack. La migration vers
  `@angular/build` (esbuild/Vite) change de moteur Sass et fera probablement
  disparaître ce bruit — ou le présentera autrement. Migrer les 33 fichiers d'abord
  risquerait un double travail.

#### C. Vulnérabilités npm

`npm audit` remonte **24 vulnérabilités** (5 low, 7 moderate, 11 high, 1 critical).

- [ ] Les analyser avant d'agir : la chaîne principale est
  `webpack-dev-server → sockjs → uuid <11.1.1`, soit du **outillage de développement**,
  absent du bundle de production. Le risque réel est donc faible.
- [ ] `undici` est également signalé (Set-Cookie SameSite, cache partagé). Vérifié :
  il ne provient que de `@angular/cli` (via `pacote → node-gyp`) et de `jsdom`, donc
  **outillage uniquement** — jamais dans le bundle livré.
- [ ] ⚠️ **Ne pas lancer `npm audit fix --force`** : il applique des changements de
  version majeure sans discernement et casserait probablement le build Angular.
  Traiter au cas par cas, en vérifiant si le paquet finit dans le bundle livré.
- [ ] La migration vers `@angular/build` (point A) supprimerait `webpack-dev-server`
  et donc l'essentiel de ces alertes.

#### D. Points mineurs

- [ ] **Browserslist** : la configuration vit dans le fichier `frontend/browserslist`
  (et non dans `package.json`). Elle contient `> 0.5%`, `last 2 versions`,
  `Firefox ESR`, `not dead`, plus une ligne `not IE 9-11` devenue sans objet. C'est le
  `> 0.5%` qui fait entrer des navigateurs qu'Angular ne supporte plus (`kaios`,
  `op_mini`, `chrome 109`, `samsung 29`…), d'où le bruit à chaque build. Sans effet
  sur le résultat. Piste : remplacer par `defaults` ou resserrer à `> 1%`.
- [ ] **`@stomp/rx-stomp` en CommonJS** — provoque un « optimization bailout » signalé
  au build. Cosmétique tant que la taille du bundle reste acceptable.
- [ ] ⚠️ **`@ng-bootstrap/ng-bootstrap` en `21.0.0-rc.0` alors que la GA `21.0.0`
  est publiée** (vérifié le 2026-07-27). Une *release candidate* tourne donc en
  production sans raison. Bump simple, à faire en priorité dans ce lot :
  ```bash
  npm install @ng-bootstrap/ng-bootstrap@21.0.0
  ```
- [ ] **Node local** : `node.version` du `pom.xml` est `v24.17.0`, et il n'existe
  aucun `.nvmrc` dans `frontend/` (vérifié le 2026-07-27). Rien ne documente donc la
  version attendue, alors qu'Angular 22 exige au minimum v22.22.3 — un Node trop
  ancien fait échouer toute commande `npm` lancée à la main, sans que le build Maven
  ne le signale (il télécharge son propre Node).
  ```bash
  echo "24" > frontend/.nvmrc
  ```

Chaque bump majeur = branche dédiée + validation visuelle + Playwright e2e
(`npm run e2e`).

#### E. i18n — dette structurelle corrigée le 2026-07-27

`messages.xlf` n'avait pas été régénéré depuis six semaines : 101 unités
enregistrées contre 630 dans le code, d'où une trentaine de « No translation found »
à chaque build. Deux causes, toutes deux traitées :

1. Extraction jamais relancée après ajout de nouveaux textes → `npm run i18n` puis
   41 clés traduites en `fr-FR` et `ro-RO`.
2. `sunOffsetsWhyDelay` et `sunOffsetsForceNote` contenaient du HTML brut
   (`<strong>`, `<ul>`) dans leur `<source>` au lieu des placeholders
   `<x id="START_TAG_STRONG"/>` attendus par Angular. Le source ne correspondant pas
   à celui extrait du code, la traduction était **ignorée silencieusement**.

- [ ] **Pour éviter la récidive** : lancer `npm run i18n` après tout ajout de texte
  traduisible, et vérifier qu'aucun « No translation found » n'apparaît au build.
  Un contrôle en CI serait plus fiable :
  ```bash
  npm run i18n && git diff --exit-code src/locale/messages.xlf
  ```
  (échoue si l'extraction n'était pas à jour)
- [ ] **Ne jamais écrire de HTML brut** dans un `<source>` de fichier `.xlf` : toujours
  repartir de l'unité générée dans `messages.xlf`.

### 6.3 — Validation

- [ ] `mvn test` : **66 tests** OK (le chiffre « 71+ » de la roadmap initiale était
  erroné — la suite en compte 66 depuis la migration SB 4).
- [ ] `cd frontend && npm test && npm run e2e` : suites vertes.
- [ ] `mvn clean package` : **aucun** « No translation found », aucun avertissement.
- [ ] Redéploiement complet via `deploy.sh`.
- [ ] Cycle 24 h d'observation.

### 6.4 — Renommer le compte MariaDB `pi` → `hermanas_app`

> **Volontairement reporté après la bascule** (décision 2026-07-27). Le gain est
> cosmétique : `pi@localhost` fonctionne exactement comme `hermanas_app@localhost`.
> L'introduire pendant la Phase 2/3 aurait ajouté une variable inutile au chemin
> critique — en cas d'échec au démarrage, on veut suspecter Spring Boot 4, Hibernate 7
> ou pi4j FFM, pas un renommage de compte SQL.

**Pourquoi le faire** : le compte MariaDB s'appelle `pi` pour des raisons purement
historiques — il reprend le nom du user Linux de `poupou`. Or les deux notions sont
indépendantes, et sur `pru` le user Linux `pi` n'existe même plus (le service tourne
sous `hermanas`, l'humain est `jean-baptisterenaux`). Garder `pi` entretient une
homonymie trompeuse et traîne un vestige de l'ancienne machine.

- [ ] Renommer les deux comptes (l'applicatif et celui d'Adminer) :
  ```bash
  sudo mysql <<'SQL'
  RENAME USER 'pi'@'localhost'   TO 'hermanas_app'@'localhost';
  RENAME USER 'pi'@'192.168.1.%' TO 'hermanas_app'@'192.168.1.%';
  FLUSH PRIVILEGES;
  SQL
  ```
- [ ] Mettre à jour `spring.datasource.username` dans `/var/lib/hermanas/application.properties`.
- [ ] `sudo systemctl restart Hermanas.service` puis vérifier la connexion en base dans les logs.
- [ ] Mettre à jour la configuration d'Adminer.
- [ ] Vérifier :
  ```bash
  sudo mysql -e "SELECT User, Host FROM mysql.user WHERE User LIKE 'hermanas%' OR User='pi';"
  # attendu : hermanas_app@localhost et hermanas_app@192.168.1.%, plus aucun 'pi'
  ```

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
| ~~1. Migration code Java 25 + SB4 + pi4j 4.x FFM~~ | Mac | ~~6-10 h dev~~ **✅ fait (2026-07-27)** | 0 |
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
- **Runtime = Java 25 LTS + Spring Boot 4 + Jakarta EE + pi4j 4.x FFM** dès le départ. Le passage direct à FFM élimine les dettes techniques `pigpio` et `libgpiod`/`gpiod` en une bascule.
- **4 cœurs Zero 2 W conservés** (pas de `maxcpus=1`).
- **Wi-Fi conservé** (pas de `disable-wifi`).
- **CPU bridé à 600 MHz** (`arm_freq=600`) — à ajuster si démarrage Spring trop lent.
- **SSH par clé** depuis le Mac (Phase 0.2bis), plus de mot de passe à taper.
- **Hermanas tourne sous le user système `hermanas`** (non-root), membre des groupes `gpio`, `audio`, `dialout`. Rendu possible par le passage à pi4j FFM + chardev (`/dev/gpiochip0` accessible au groupe `gpio`). Bonus : élimine le hack VLC-en-root de `poupou`. Fichiers applicatifs dans `/var/lib/hermanas/` (owner `hermanas:hermanas`, mode 640/750). Unit systemd durci : `ProtectHome=yes`, `ProtectSystem=strict`, `NoNewPrivileges=yes`, `PrivateTmp=yes`.
- **Certificats TLS gérés par `shannen`** — plus de Let's Encrypt côté `pru`, et
  `keystore.p12` n'est même pas migré : Hermanas sert en HTTP simple sur `:8080`
  (`server.port=8080`, aucune propriété `server.ssl.*`), `shannen` termine le TLS.
- **Camera + player audio traités en dernier** (Phase 7), après stabilisation et update libs.
- **Photos migrées via Samba** (share temporaire sur `pru`, monté sur `poupou`, purgé après).
- **Prometheus + Grafana restent sur `shannen`** — juste `prometheus-node-exporter` à réinstaller sur `pru`.
- **Rollback conservé 1 semaine** post-bascule.
