# Checklist de mise en service — `pru` (Pi Zero 2 W)

Première mise sous tension après recâblage du GPIO. À dérouler dans l'ordre :
chaque étape suppose la précédente validée.

> **Convention** — `[pru]` = à lancer en SSH sur le Pi, `[mac]` = depuis le poste
> de dev, `[nav]` = dans le navigateur.

---

## 0 — Avant de remettre sous tension

- [ ] Fil de **signal** du servo sur la **broche physique 32** (GPIO 12), pas 22.
      Repère : 16ᵉ paire, rangée **extérieure** (numéros pairs).
- [ ] Fil rouge (5 V) et marron/noir (GND) du servo **inchangés**.
- [ ] Aucun fil ne touche une broche voisine (le pas est de 2,54 mm).
- [ ] Porte de la coop en position **basse** (fermée) — état connu au démarrage.

### Câblage complet attendu

| Fonction | BCM | Broche physique | Sens |
|---|---|---|---|
| Servo porte | **12** | **32** | sortie (PWM0) |
| Bouton fin de course bas | 15 | 10 | entrée |
| Bouton fin de course haut | 18 | 12 | entrée |
| Bouton maisonnette | 24 | 18 | entrée |
| Relais lumière | 14 | 8 | sortie |
| Relais ventilateur | 23 | 16 | sortie |
| Capteur DHT22 | 4 | 7 | entrée (1-wire, python) |

Cette table est aussi servie en direct par l'API — voir l'étape 3.

---

## 1 — Système

```bash
# [pru]
hostnamectl --static          # pru
getent hosts pru              # résout sans délai
uptime
free -h                       # ~463 Mo total, swap = zram uniquement
swapon --show                 # /dev/zram0 seul — AUCUN /var/swap
vcgencmd get_throttled        # throttled=0x0
```

- [ ] `swapon --show` ne montre **que** `/dev/zram0`
- [ ] `get_throttled` vaut `0x0` (sinon : alimentation insuffisante — critique
      pour le servo, qui tire un pic au démarrage)

```bash
# [pru] les GPIO sont-ils accessibles sans root ?
groups hermanas               # doit contenir gpio, i2c, spi
ls -l /dev/gpiochip0
ls /sys/class/pwm/pwmchip0/   # doit exister — sinon dtoverlay pwm absent
```

- [ ] `/sys/class/pwm/pwmchip0/` présent (sinon `dtoverlay=pwm,pin=12,func=4`
      manque dans `/boot/firmware/config.txt` → PWM impossible)

---

## 2 — Démarrage de l'application

```bash
# [pru]
sudo systemctl start Hermanas
journalctl -u Hermanas -f
```

Compter **~265 s**. C'est normal, ne pas s'inquiéter du silence.

À surveiller dans les logs, dans l'ordre d'apparition :

- [ ] `Init pi4j context.` **sans** `Can't initialise pi4j context`
- [ ] `Provision PWM on BCM 12 -> chip 0, channel 0.`
- [ ] `Can't load picam native library` → **attendu**, caméra = Phase 7
- [ ] `Started HermanasApplication in ~265 seconds`
- [ ] **aucun** `Thread starvation`
- [ ] **aucune** boucle de redémarrage (`systemctl status Hermanas` → `active (running)`)

```bash
# [pru] confirmation
systemctl status Hermanas
curl -s localhost:8080/actuator/health
```

- [ ] `health` renvoie `UP` (ou `DOWN` avec seulement `door` en cause si la
      position est indéterminée — voir étape 4)

---

## 3 — GPIO : lecture seule d'abord

Rien ne bouge à cette étape. On vérifie que le logiciel *voit* le matériel.

```bash
# [pru] table de câblage vue par l'application
curl -s localhost:8080/api/v1/electronics/gpio | python3 -m json.tool
```

- [ ] `door.servo` → `"pin": 12`, `"boardPin": 32`
- [ ] les 7 composants sont listés

```bash
# [pru] état live des fins de course
curl -s localhost:8080/api/v1/buttons/status | python3 -m json.tool
```

- [ ] Porte fermée : `BOTTOM` = `pressed: true`, `UP` = `pressed: false`
- [ ] Appuyer **à la main** sur le fin de course haut, relancer la commande →
      `UP` passe à `true`. Idem pour `BIRDHOUSE`.

> Un bouton toujours `true` ou jamais déclenché = contact HS ou fil inversé.
> C'est le moment de le voir, avant de faire bouger la porte.

```bash
# [pru] capteur DHT22 — jamais validé sur pru jusqu'ici
curl -s localhost:8080/api/v1/sensor/info | python3 -m json.tool
```

- [ ] température et humidité plausibles (ni `null`, ni `0.0`)

Si échec, **d'abord vérifier la configuration** — les valeurs par défaut du JAR
pointent encore vers `poupou` (`/usr/bin/python` et `/home/pi/AdafruitDHT.py`,
qui n'existent ni l'un ni l'autre sur Trixie). Elles doivent être surchargées
dans `/var/lib/hermanas/application.properties` :

```bash
# [pru]
grep sensor.python /var/lib/hermanas/application.properties
```

- [ ] `sensor.python.command` → `/usr/bin/python3` (**pas** `/usr/bin/python`)
- [ ] `sensor.python.script` → le chemin réel du script sur `pru`
      (`read_dht22.py`, réécrit en Phase 0.7bis — **pas** `AdafruitDHT.py`)

Si ces lignes sont absentes, les ajouter :

```bash
# [pru]
sudo tee -a /var/lib/hermanas/application.properties > /dev/null <<'EOF'

sensor.python.command = /usr/bin/python3
sensor.python.script = /var/lib/hermanas/scripts/read_dht22.py
EOF
sudo systemctl restart Hermanas
```

Ensuite seulement, tester le script seul pour isoler matériel vs logiciel
(adapter le chemin à celui configuré ci-dessus) :

```bash
# [pru]
/usr/bin/python3 /var/lib/hermanas/scripts/read_dht22.py 22 4
# attendu : Temp=21.3*  Humidity=54.2%
```

`Failed to get reading` + exit 1 = le script tourne mais ne lit rien → câblage
du capteur (data sur broche 7 / BCM 4, résistance de tirage 4,7-10 kΩ vers 3,3 V).

---

## 4 — Actionneurs, du moins risqué au plus risqué

### 4a. Lumière et ventilateur (aucun risque mécanique)

```bash
# [pru] dans un second terminal, suivre les commutations
journalctl -u Hermanas -f | grep -i "fan_relay\|light_relay\|FanService\|LightService"
```

```bash
# [pru] le paramètre `param` est OBLIGATOIRE
curl -s "localhost:8080/api/v1/fan/switch?param=true" ; echo
curl -s localhost:8080/api/v1/fan/status ; echo
curl -s "localhost:8080/api/v1/fan/switch?param=false" ; echo
```

Attendu dans les logs :

```
FanService : Switching on fan.
GpioHermanasRpiService : Event on fan_relay on address 23, state is now HIGH
```

- [ ] la ligne `Event on fan_relay ... HIGH` apparaît
- [ ] le relais **claque** de façon audible
- [ ] le ventilateur tourne, puis s'arrête seul (timer de sécurité)

> **Distinguer relais et actionneur.** Le log et le clac prouvent que le Pi
> commande bien le relais ; ils ne disent rien du circuit en aval.
>
> | Observation | Diagnostic |
> |---|---|
> | Log `HIGH` + clac + ça tourne | OK |
> | Log `HIGH` + clac, **immobile** | Circuit aval : alimentation, ou fil desserré au bornier du relais |
> | Log `HIGH`, **aucun clac** | Commande : câble BCM 23 / broche 16, GND commun, ou alim du module relais |
>
> Cas 2 — vérifier dans l'ordre : fils sur **COM** et **NO** (pas NC, qui
> inverserait le comportement) ; serrage du bornier à vis (panne la plus
> fréquente : un brin qui a glissé) ; alimentation de l'actionneur.

La lumière est en **POST** → nécessite une session. À tester depuis le front
(étape 5). Le log attendu est le même, sur `light_relay` / adresse 14.

### 4b. Servo — petit mouvement contrôlé

⚠️ **Garder une main près de l'alimentation.** Si le servo force contre une
butée, couper immédiatement.

```bash
# [pru] impulsion brève, 500 ms
curl -s "localhost:8080/api/v1/door/turnServo?dutyCycle=8&frequency=50&duration=500"
```

- [ ] le servo **bouge** (même légèrement)
- [ ] il ne vibre pas et ne chauffe pas une fois l'impulsion finie
- [ ] `journalctl` montre `turning servomotor ...` puis `... servomotor done !`

Si rien ne bouge : vérifier `/sys/class/pwm/pwmchip0/` (étape 1) et que le fil
de signal est bien sur la broche 32.

### 4c. Cycle de porte complet

Se fait **depuis le front** (étape 5) : `/door/open` et `/door/close` sont en
POST, donc authentifiés + CSRF — un curl nu renverra 401/403, ce qui n'est
**pas** une panne.

- [ ] Ouverture : la porte monte et **s'arrête au fin de course haut**
- [ ] `curl -s localhost:8080/api/v1/door/status` → `OPENED`
- [ ] Fermeture : la porte descend et **s'arrête au fin de course bas**
- [ ] `curl -s localhost:8080/api/v1/door/status` → `CLOSED`
- [ ] Aucun bruit de forçage en fin de course

> Le point critique est **l'arrêt sur fin de course**. Si la porte dépasse, le
> servo force en continu : couper l'alimentation et revoir l'étape 3.

---

## 5 — Front-end et authentification

```bash
# [pru] le SPA est-il servi ?
curl -sI localhost:8080/ | head -3
curl -sI localhost:8080/fr-FR/ | head -3
```

- [ ] `[nav]` ouvrir `http://pru:8080/` → redirection vers `/fr-FR/`
- [ ] La page de login s'affiche (formulaire Angular, **pas** Amplify)
- [ ] Connexion avec un compte de `users.properties` → arrivée sur le dashboard
- [ ] Le dashboard affiche température, humidité, état de la porte
- [ ] **Console navigateur (F12) : aucune 404 sur `/fr-FR/actuator/*`**
      → c'est la validation du correctif `ngsw-config.json`
- [ ] Déconnexion puis reconnexion fonctionnent

> **Si les 404 `/fr-FR/actuator/*` persistent** : c'est l'ancien service worker
> encore enregistré dans le navigateur. F12 → Application → Service Workers →
> *Unregister*, puis rechargement forcé (Cmd+Shift+R).

Puis, connecté :

- [ ] Bouton d'ouverture de la porte → la porte bouge (étape 4c)
- [ ] Bouton lumière → le relais claque
- [ ] Page Électronique : les 7 GPIO s'affichent, servo sur broche 32
- [ ] Page Logs (compte admin) accessible

---

## 6 — Automatismes

```bash
# [pru]
curl -s localhost:8080/api/v1/scheduler/nextEvents | python3 -m json.tool
curl -s localhost:8080/api/v1/scheduler/doorOpeningTime
curl -s localhost:8080/api/v1/scheduler/doorClosingTime
```

- [ ] Les heures correspondent au lever/coucher du soleil du jour
- [ ] `nextEvents` liste bien les prochains événements

---

## 7 — Arrêt propre (à ne pas sauter)

C'est ici qu'on valide les timeouts systemd posés en Phase 3.

```bash
# [pru]
time sudo systemctl stop Hermanas
journalctl -u Hermanas -n 40
```

- [ ] L'arrêt prend **moins de 180 s**
- [ ] `Shutdown gpio instance.` apparaît dans les logs
- [ ] **Aucun** `SIGKILL` / `Killing process` / `state 'stop-sigterm' timed out`
- [ ] Après l'arrêt, le servo n'est **plus alimenté** (ne force pas, ne chauffe pas)
- [ ] `SHUTDOWN` enregistré en base

Puis redémarrage final :

```bash
# [pru]
sudo systemctl start Hermanas
sudo systemctl is-enabled Hermanas    # doit être "enabled"
```

- [ ] `is-enabled` → `enabled` (sinon le service ne repartira pas après coupure
      de courant — critique sur alimentation solaire)

---

## 8 — Surveillance des premières 24 h

```bash
# [pru]
watch -n 60 'free -h; echo; vmstat 1 3 | tail -1'
```

- [ ] `wa` reste entre 0 et 1 % (pas de retour du thrashing SD)
- [ ] Aucune écriture sur un swap disque (`swapon --show` → zram seul)
- [ ] Ouverture automatique au lever du soleil **observée**
- [ ] Fermeture automatique au coucher **observée**
- [ ] Grafana (https://grafana.r3n4.uk) reçoit les métriques

---

## En cas de problème

| Symptôme | Piste |
|---|---|
| Servo immobile | `/sys/class/pwm/pwmchip0/` absent → `dtoverlay=pwm,pin=12,func=4` |
| `ProviderNotFoundException: ffm-*` | `usermod -aG gpio,i2c,spi hermanas` puis redémarrer |
| Porte ne s'arrête pas en fin de course | Couper l'alimentation. Bouton HS → étape 3 |
| 401/403 sur `/door/open` en curl | Normal : POST = session + CSRF. Passer par le front |
| 404 `/fr-FR/actuator/*` | Ancien service worker en cache → *Unregister* dans DevTools |
| DHT22 `Failed to get reading` | Câblage capteur : broche 7, tirage 4,7-10 kΩ vers 3,3 V |
| Démarrage > 15 min puis kill | `TimeoutStartSec` — vérifier `systemctl show Hermanas` |
| Relais claque mais rien ne tourne | Circuit aval : bornier COM/NO desserré, ou alimentation de l'actionneur |
| Caméra en erreur | **Attendu** — Phase 7 |

---

## Rollback

`poupou` reste intact et peut être remis en service : rebrancher son GPIO
(servo sur broche **22**, pas 32) et le rallumer. La base de `pru` aura divergé
— voir Phase 8 de `migration.md`.
