package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Sensor information.
 * Used to expose sensor data through the REST API without exposing the JPA entity.
 */
@Schema(description = "Sensor data including internal and external temperature and humidity readings")
public class SensorDTO {

    @Schema(description = "Internal temperature in degrees Celsius", example = "7.2")
    private Double temperature;

    @Schema(description = "External temperature from weather service in degrees Celsius", example = "5.0")
    private Double externalTemperature;

    @Schema(description = "Internal humidity percentage", example = "99.9")
    private Double humidity;

    @Schema(description = "External humidity from weather service percentage", example = "93.0")
    private Double externalHumidity;

    @Schema(description = "Timestamp of the sensor reading", example = "2021-01-30T15:49:47")
    private LocalDateTime dateTime;

    public SensorDTO() {
    }

    public SensorDTO(Double temperature, Double externalTemperature, Double humidity, Double externalHumidity, LocalDateTime dateTime) {
        this.temperature = temperature;
        this.externalTemperature = externalTemperature;
        this.humidity = humidity;
        this.externalHumidity = externalHumidity;
        this.dateTime = dateTime;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(Double externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getExternalHumidity() {
        return externalHumidity;
    }

    public void setExternalHumidity(Double externalHumidity) {
        this.externalHumidity = externalHumidity;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public String toString() {
        return "SensorDTO{" +
                "temperature=" + temperature +
                ", externalTemperature=" + externalTemperature +
                ", humidity=" + humidity +
                ", externalHumidity=" + externalHumidity +
                ", dateTime=" + dateTime +
                '}';
    }
}
