package org.jibe77.hermanas.controller.sensor;

import org.jibe77.hermanas.data.entity.Sensor;
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
import java.time.LocalDateTime;

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

    public DHT22() {
        // default constructor
    }

    @Cacheable(value = {"sensor"})
    public Sensor refreshData() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, pythonScript, scriptArg1, scriptArg2);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))){
            String returnValue = in.readLine();
            logger.info("python {} with native command {} {} {} has returned {}.", pythonCommand, pythonScript, scriptArg1, scriptArg2, returnValue);
            Sensor sensor = parseSensorReturnedValue(returnValue);
            logger.info("temperature {} and humidity {}", sensor.getTemperature(), sensor.getHumidity());
            int exitValue = p.waitFor();
            logger.info("exit value {}.", exitValue);
            return sensor;
        } catch (InterruptedException e) {
            logger.error("interrupted while refreshing data.", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            return createDefaultSensor();
        }
    }

    protected Sensor parseSensorReturnedValue(String returnValue) {
        Sensor sensor = createDefaultSensor();
        String[] returnedString = returnValue.split(" ");
        for (String returnedValue : returnedString) {
            if (returnedValue.startsWith("Temp=")) {
                sensor.setTemperature(
                        Double.valueOf(
                                returnedValue.substring(5).replace('*', ' ')));
            } else if (returnedValue.startsWith("Humidity=")) {
                sensor.setHumidity(
                        Double.valueOf(
                                returnedValue.substring(9).replace('%', ' ')));
            }
        }
        return sensor;
    }

    private Sensor createDefaultSensor() {
        Sensor sensor = new Sensor();
        sensor.setDateTime(LocalDateTime.now());
        return sensor;
    }


    @Scheduled(fixedDelayString = "${sensor.cache.delay}")
    @CacheEvict(value = {"sensor"})
    public void evictSensorCache() {
        logger.debug("evict sensor cache.");
    }
}