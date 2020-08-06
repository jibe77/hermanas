package org.jibe77.hermanas.client.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherInfo {

    void setValues(Double temperature, Double humidity) {
        if (getMain() == null) {
            setMain(new Main());
            setName("External temperature.");
        }
        getMain().setTemp(temperature);
        getMain().setHumidity(humidity);
    }

    public Double getHumidity() {
        return getMain().getHumidity();
    }

    public Double getTemp() { return getMain().getTemp();}

    class Main {

        public Main() {
        }

        private Double temp;
        private Double humidity;

        public Double getTemp() {
            return temp;
        }

        private void setTemp(Double temp) {
            this.temp = temp;
        }

        public Double getHumidity() {
            return humidity;
        }

        private void setHumidity(Double humidity) {
            this.humidity = humidity;
        }

        @Override
        public String toString() {
            return "main{" +
                    "temp='" + temp + '\'' +
                    ", humidity='" + humidity + '\'' +
                    '}';
        }
    }

    String name;
    Main main;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    @Override
    public String toString() {
        return "WeatherInfo{" +
                "name='" + getName() + '\'' +
                ", main=" + getMain() +
                '}';
    }
}
