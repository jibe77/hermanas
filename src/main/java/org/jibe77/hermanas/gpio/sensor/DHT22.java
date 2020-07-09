package org.jibe77.hermanas.gpio.sensor;

import org.jibe77.hermanas.gpio.GpioHermanasController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implements the DHT22 / AM2302 reading in Java using Pi4J.
 *
 * See sensor specification sheet for details.
 *
 * @author Doug Culnane
 */
@Component
public class DHT22 {

    GpioHermanasController gpioHermanasController;

    Logger logger = LoggerFactory.getLogger(DHT22.class);

    /**
     * PI4J Pin number.
     */
    @Value("${sensor.gpio.address}")
    private int pinNumber;

    @Value("${sensor.gpio.timeout.in.ms}")
    private int timeoutInMilliseconds;

    @Value("${sensor.gpio.parity.check}")
    private boolean checkParity;

    private static final int maxTimings = 85;
    private final int[] dht22_dat = {0, 0, 0, 0, 0};
    private float temperature = 9999;
    private float humidity = 9999;


    public DHT22(GpioHermanasController gpioHermanasController) {
        this.gpioHermanasController = gpioHermanasController;
    }

    private int pollDHT22() {
        return gpioHermanasController.fetchData(pinNumber, dht22_dat);
    }

    public void refreshData() {
        int pollDataCheck = pollDHT22();
        if (pollDataCheck >= 40 && checkParity()) {

            final float newHumidity = (float) ((dht22_dat[0] << 8) + dht22_dat[1]) / 10;
            final float newTemperature = (float) (((dht22_dat[2] & 0x7F) << 8) + dht22_dat[3]) / 10;

            if (humidity == 9999 || ((newHumidity < humidity + 40) && (newHumidity > humidity - 40))) {
                humidity = newHumidity;
                if (humidity > 100) {
                    humidity = dht22_dat[0]; // for DHT22
                }
            }

            if (temperature == 9999 || ((newTemperature < temperature + 40) && (newTemperature > temperature - 40))) {
                temperature = (float) (((dht22_dat[2] & 0x7F) << 8) + dht22_dat[3]) / 10;
                if (temperature > 125) {
                    temperature = dht22_dat[2]; // for DHT22
                }
                if ((dht22_dat[2] & 0x80) != 0) {
                    temperature = -temperature;
                }
            }
        }
    }


    public double getHumidity() {
        if (humidity == 9999) {
            return 0;
        }
        logger.info("returning humidity {}.", humidity);
        return humidity;
    }

    @SuppressWarnings("unused")
    public double getTemperature() {
        if (temperature == 9999) {
            return 0;
        }
        logger.info("returning temperature {}.", temperature);
        return temperature;
    }

    private boolean checkParity() {
        logger.info("Check parity on data array, " +
                "index 0 is {}," +
                "index 1 is {}," +
                "index 2 is {}," +
                "index 3 is {}," +
                "index 4 is {}", dht22_dat[0], dht22_dat[1], dht22_dat[2], dht22_dat[3], dht22_dat[4]);
        return dht22_dat[4] == (dht22_dat[0] + dht22_dat[1] + dht22_dat[2] + dht22_dat[3] & 0xFF);
    }


}