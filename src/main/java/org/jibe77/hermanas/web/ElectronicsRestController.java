package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of the GPIO wiring. Values come from application.properties and
 * cannot be changed at runtime — they reflect the physical wiring of the chicken
 * coop, which is set once when the box is assembled. A wiring change requires a
 * restart anyway because Pi4j provisions each pin at startup and never reopens it.
 */
@RestController
@RequestMapping("/api/v1/electronics")
@Tag(name = "Electronics", description = "Read-only view of the GPIO wiring (BCM numbering)")
public class ElectronicsRestController {

    @Value("${door.button.bottom.gpio.address}")
    private int doorButtonBottomPin;

    @Value("${door.button.up.gpio.address}")
    private int doorButtonUpPin;

    @Value("${door.servo.gpio.address}")
    private int doorServoPin;

    @Value("${birdhouse.button.gpio.address}")
    private int birdhouseButtonPin;

    @Value("${light.relay.gpio.address}")
    private int lightRelayPin;

    @Value("${fan.relay.gpio.address}")
    private int fanRelayPin;

    @Value("${sensor.python.arg2}")
    private int sensorPin;

    @Operation(summary = "List the GPIO pins used by every connected component")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pin layout")
    })
    @GetMapping("/gpio")
    public List<Map<String, Object>> listGpioPins() {
        List<Map<String, Object>> pins = new ArrayList<>();
        pins.add(entry("door.servo", "Door servomotor", "Servomoteur de la porte",
                "output (PWM)", "servo", doorServoPin));
        pins.add(entry("door.button.up", "Door upper end-stop button", "Bouton de fin de course haut de la porte",
                "input", "button", doorButtonUpPin));
        pins.add(entry("door.button.bottom", "Door bottom end-stop button", "Bouton de fin de course bas de la porte",
                "input", "button", doorButtonBottomPin));
        pins.add(entry("birdhouse.button", "Birdhouse button", "Bouton de la maisonnette",
                "input", "button", birdhouseButtonPin));
        pins.add(entry("light.relay", "Light relay", "Relais de l'éclairage",
                "output", "light", lightRelayPin));
        pins.add(entry("fan.relay", "Fan relay", "Relais du ventilateur",
                "output", "fan", fanRelayPin));
        pins.add(entry("sensor.dht22", "Temperature & humidity sensor (DHT22)", "Capteur de température et d'humidité (DHT22)",
                "input (1-wire, via python)", "sensor", sensorPin));
        return pins;
    }

    private Map<String, Object> entry(String key, String labelEn, String labelFr,
                                      String direction, String kind, int bcmPin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", labelEn);
        m.put("labelFr", labelFr);
        m.put("direction", direction);
        m.put("kind", kind);
        m.put("pin", bcmPin);
        m.put("boardPin", bcmToBoardPin(bcmPin));
        return m;
    }

    /**
     * BCM (GPIO) number → physical pin number on the Raspberry Pi 40-pin header.
     * Source: pinout.xyz. Only the BCM numbers actually wired on the chicken coop
     * need to be covered, but the full map is kept here so adding a new component
     * does not silently return "?" if its pin is in the table.
     */
    private static String bcmToBoardPin(int bcm) {
        switch (bcm) {
            case 2:  return "3";
            case 3:  return "5";
            case 4:  return "7";
            case 14: return "8";
            case 15: return "10";
            case 17: return "11";
            case 18: return "12";
            case 27: return "13";
            case 22: return "15";
            case 23: return "16";
            case 24: return "18";
            case 10: return "19";
            case 9:  return "21";
            case 25: return "22";
            case 11: return "23";
            case 8:  return "24";
            case 7:  return "26";
            case 0:  return "27";
            case 1:  return "28";
            case 5:  return "29";
            case 6:  return "31";
            case 12: return "32";
            case 13: return "33";
            case 19: return "35";
            case 16: return "36";
            case 26: return "37";
            case 20: return "38";
            case 21: return "40";
            default: return "?";
        }
    }
}
