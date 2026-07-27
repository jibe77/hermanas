#!/usr/bin/env python3
"""Lecture du capteur DHT22 (température / humidité).

Remplace l'ancien AdafruitDHT.py, qui dépendait de la bibliothèque
Adafruit_DHT — archivée depuis 2020, disponible uniquement en Python 2, et
absente de Debian Trixie.

Deux différences importantes avec l'ancien script :

* Il repose sur adafruit-circuitpython-dht, qui parle au chardev
  /dev/gpiochip0 via libgpiod au lieu d'accéder à /dev/mem. Il n'exige donc
  plus les privilèges root : l'appartenance au groupe `gpio` suffit, ce que le
  user `hermanas` a déjà. C'est le même mécanisme que pi4j-plugin-ffm.
* Le format de sortie est conservé à l'identique — "Temp=26.7*  Humidity=99.9%" —
  car SensorService.parseSensorReturnedValue() découpe cette chaîne sur les
  espaces et cherche les préfixes "Temp=" et "Humidity=".

Usage (mêmes arguments que l'ancien script) :
    read_dht22.py <type> <pin>
        type : 11 pour un DHT11, 22 pour un DHT22/AM2302
        pin  : numéro BCM de la broche de données (4 sur Hermanas)
"""
import sys
import time

try:
    import adafruit_dht
    import board
except ImportError:
    sys.stderr.write(
        "Missing dependency. Install with:\n"
        "  pip3 install --break-system-packages adafruit-circuitpython-dht\n")
    sys.exit(2)

# Le DHT22 est un capteur peu fiable : une lecture isolée échoue fréquemment
# (timing raté, checksum invalide) sans que rien ne soit anormal. L'ancien
# script s'appuyait sur Adafruit_DHT.read_retry() qui réessayait 15 fois par
# défaut ; on reproduit ce comportement.
MAX_ATTEMPTS = 15
RETRY_DELAY_SECONDS = 2.0


def resolve_pin(bcm_pin):
    """Convertit un numéro BCM en objet board (board.D4 pour 4)."""
    name = f"D{bcm_pin}"
    pin = getattr(board, name, None)
    if pin is None:
        sys.stderr.write(f"Unknown BCM pin: {bcm_pin}\n")
        sys.exit(2)
    return pin


def main():
    if len(sys.argv) != 3:
        sys.stderr.write(f"Usage: {sys.argv[0]} <11|22> <bcm_pin>\n")
        sys.exit(2)

    sensor_type, bcm_pin = sys.argv[1], sys.argv[2]

    if sensor_type not in ("11", "22"):
        sys.stderr.write(f"Unsupported sensor type: {sensor_type} (expected 11 or 22)\n")
        sys.exit(2)

    try:
        bcm_pin = int(bcm_pin)
    except ValueError:
        sys.stderr.write(f"Invalid pin: {bcm_pin}\n")
        sys.exit(2)

    pin = resolve_pin(bcm_pin)
    device = (adafruit_dht.DHT11(pin) if sensor_type == "11"
              else adafruit_dht.DHT22(pin))

    try:
        for attempt in range(MAX_ATTEMPTS):
            try:
                temperature = device.temperature
                humidity = device.humidity
                if temperature is not None and humidity is not None:
                    # Format attendu par SensorService — ne pas modifier.
                    print(f"Temp={temperature:.1f}*  Humidity={humidity:.1f}%")
                    return 0
            except RuntimeError:
                # Lecture ratée : normal sur ce capteur, on réessaie.
                pass

            if attempt < MAX_ATTEMPTS - 1:
                time.sleep(RETRY_DELAY_SECONDS)

        sys.stderr.write("Failed to get reading. Try again!\n")
        return 1
    finally:
        # Libère la broche, sinon la lecture suivante peut échouer.
        try:
            device.exit()
        except Exception:
            pass


if __name__ == "__main__":
    sys.exit(main())
