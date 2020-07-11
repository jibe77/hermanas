package org.jibe77.hermanas.gpio.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Implements the DHT22 / AM2302 reading in Java using Pi4J.
 *
 * See sensor specification sheet for details.
 *
 * @author Doug Culnane
 */
@Component
public class DHT22 {

    Logger logger = LoggerFactory.getLogger(DHT22.class);

    @Value("${sensor.python.command}")
    private String pythonCommand;

    /**
     * PI4J Pin number.
     */
    @Value("${sensor.python.script}")
    private String pythonScript;

    @Value("${sensor.python.arg1}")
    private String scriptArg1;

    @Value("${sensor.python.arg2}")
    private String scriptArg2;

    private double humidity = 0;
    private double temperature = 0;

    public DHT22() {
    }

    @Cacheable(value = {"sensor"})
    public void refreshData() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, pythonScript, scriptArg1, scriptArg2);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String returnValue = in.readLine();
                logger.info("python {} with native command {} {} {} has returned {}.", pythonCommand, pythonScript, scriptArg1, scriptArg2, returnValue);
                String[] temperatureAndHumidity = returnValue.split(" ");
                this.temperature = Double.valueOf(temperatureAndHumidity[0].substring(6, 10));
                this.humidity = Double.valueOf(temperatureAndHumidity[2].substring(9, 13));
                logger.info("temperature {} and humidity {}", temperature, humidity);
            }

            int exitValue = p.waitFor();
            logger.info("exit value {}.", exitValue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double getHumidity() {
        try {
            refreshData();
        } catch (IOException e) {
            logger.error("Can't refresh humidity.");
            return -1;
        }
        return humidity;
    }

    public double getTemperature() {
        try {
            refreshData();
        } catch (IOException e) {
            logger.error("Can't refresh temperature.");
            return -1;
        }
        return temperature;
    }

    @Scheduled(fixedDelayString = "${sensor.cache.delay}")
    @CacheEvict(value = {"sensor"})
    public void evictSensorCache() {
        logger.debug("evict sensor cache.");
    }
}