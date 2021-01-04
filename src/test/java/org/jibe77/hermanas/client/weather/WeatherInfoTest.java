package org.jibe77.hermanas.client.weather;

import org.jibe77.hermanas.controller.energy.WifiController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {WeatherClient.class})
class WeatherInfoTest {

    @Value("${suntime.latitude}")
    public double latitude;

    @Value("${suntime.longitude}")
    public double longitude;

    @Autowired
    WeatherClient weatherClient;

    @MockBean
    RestTemplateBuilder restTemplateBuilder;

    @MockBean
    RestTemplate restTemplate;

    @MockBean
    WifiController wifiController;

    @Test
    void testWeather() {
        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setValues(0d, 0d);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForObject(
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyDouble(),
                Mockito.anyDouble(),
                Mockito.anyString())).thenReturn(weatherInfo);
        weatherClient.setWeatherInfoEnabled(true);
        weatherInfo = weatherClient.getInfo();
        assertEquals(0d, weatherInfo.getTemp());
        assertEquals(0d, weatherInfo.getHumidity());
    }

    @Test
    void testWeatherIfDisabled() {
        weatherClient.setWeatherInfoEnabled(false);
        WeatherInfo weatherInfo = weatherClient.getInfo();
        assertEquals(WeatherClient.DEFAULT_VALUE_IF_DISABLED, weatherInfo.getMain().getTemp());
        assertEquals(WeatherClient.DEFAULT_VALUE_IF_DISABLED, weatherInfo.getMain().getHumidity());
    }
}
