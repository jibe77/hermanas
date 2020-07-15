package org.jibe77.hermanas.client.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherInfo {

    public void setValues(String temperature, String humidity) {
        if (getMain() == null)
            setMain(new Main());
        getMain().setTemp(temperature);
        getMain().setHumidity(humidity);
    }

    class Main {

        public Main() {
        }

        private String temp;
        private String humidity;

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getHumidity() {
            return humidity;
        }

        public void setHumidity(String humidity) {
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


    /*
        {
          "coord": {
            "lon":6.14,"lat":49.37
          },
          "weather":[{
            "id":801,
            "main":"Clouds",
            "description":"few clouds",
            "icon":"02d"}],
          "base":"stations",
          "main":{"temp":286.43,"feels_like":283.83,"temp_min":285.93,"temp_max":287.15,"pressure":1024,"humidity":71},
          "visibility":10000,
          "wind":{"speed":3.1,"deg":20},
          "clouds":{"all":20},
          "dt":1594451423,
          "sys":{"type":1,"id":1601,"country":"FR","sunrise":1594438910,"sunset":1594496382},
          "timezone":7200,
          "id":2972811,
          "name":"Thionville",
          "cod":200
        }
     */

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
                "name='" + name + '\'' +
                ", main=" + main +
                '}';
    }
}
