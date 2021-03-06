package org.jibe77.hermanas.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties(value = { "id" })
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double temperature;

    private Double externalTemperature;

    private Double humidity;

    private Double externalHumidity;

    private LocalDateTime dateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Double getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(Double externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public Double getExternalHumidity() {
        return externalHumidity;
    }

    public void setExternalHumidity(Double externalHumidity) {
        this.externalHumidity = externalHumidity;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "id=" + id +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", dateTime=" + dateTime +
                '}';
    }
}
