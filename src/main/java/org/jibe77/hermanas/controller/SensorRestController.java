package org.jibe77.hermanas.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.client.weather.WeatherClient;
import org.jibe77.hermanas.client.weather.WeatherInfo;
import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.controller.sensor.SensorService;
import org.jibe77.hermanas.data.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor")
@Tag(name = "Sensor", description = "Temperature and humidity sensor endpoints for real-time data and historical records")
public class SensorRestController {

    private static final Logger logger = LoggerFactory.getLogger(SensorRestController.class);

    SensorService sensorService;

    WeatherClient weatherClient;

    SensorRepository sensorRepository;

    public SensorRestController(SensorService sensorService, WeatherClient weatherClient, SensorRepository sensorRepository) {
        this.sensorService = sensorService;
        this.weatherClient = weatherClient;
        this.sensorRepository = sensorRepository;
    }

    @Operation(
            summary = "Get current sensor info",
            description = "Returns current temperature and humidity from internal sensor plus external weather data. Example: {\"temperature\":7.2,\"externalTemperature\":5.0,\"humidity\":99.9,\"externalHumidity\":93.0,\"dateTime\":\"2021-01-30T15:49:47.993426\"}"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sensor data retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Sensor.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Sensor read error or weather API error",
                    content = @Content
            )
    })
    @GetMapping(value = "/info")
    public Sensor getInfo() throws IOException {
        Sensor sensor = sensorService.refreshData();
        WeatherInfo weatherInfo = weatherClient.getInfo();
        sensor.setExternalHumidity(weatherInfo.getHumidity());
        sensor.setExternalTemperature(weatherInfo.getTemp());
        return sensor;
    }

    @Operation(
            summary = "Get sensor history for last 24 hours",
            description = "Returns all sensor readings from the last 24 hours"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            )
    })
    @GetMapping(value = "/history/today")
    public List<Sensor> getHistoryLastDay() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusDays(1));
    }

    @Operation(
            summary = "Get sensor history for last week",
            description = "Returns all sensor readings from the last 7 days"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            )
    })
    @GetMapping(value = "/history/week")
    public List<Sensor> getHistoryLastWeek() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusWeeks(1));
    }

    @Operation(
            summary = "Get sensor history for last month",
            description = "Returns all sensor readings from the last 30 days"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            )
    })
    @GetMapping(value = "/history/month")
    public List<Sensor> getTodayLastMonth() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusMonths(1));
    }

    @Operation(
            summary = "Get sensor history for last year",
            description = "Returns all sensor readings from the last 365 days"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            )
    })
    @GetMapping(value = "/history/year")
    public List<Sensor> getHistoryYear() {
        return sensorRepository.findByDateTimeGreaterThan(LocalDateTime.now().minusYears(1));
    }

    @Operation(
            summary = "Get sensor history for specific year",
            description = "Returns all sensor readings for a specific year (full calendar year Jan 1 - Dec 31)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid year format",
                    content = @Content
            )
    })
    @GetMapping(value = "/history/year/{year}")
    public List<Sensor> getHistoryYear(
            @Parameter(description = "Year (e.g., 2024)", example = "2024", required = true)
            @PathVariable(name="year") String year) {
        logger.info("fetching history with year parameter : {}.", year);
        LocalDateTime startDate =
                LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0)
                        .withSecond(0).withYear(Integer.parseInt(year));
        LocalDateTime endDate =
                LocalDateTime.now().withMonth(12).withDayOfMonth(31).withHour(23).withMinute(59)
                        .withSecond(59).withYear(Integer.parseInt(year));

        logger.info("start date is {} and end date parameter is {}.", startDate, endDate);
        return sensorRepository.findByDateTimeBetweenOrderByDateTimeDesc(startDate, endDate);
    }

    @Operation(
            summary = "Get sensor history for date range",
            description = "Returns all sensor readings between two dates in descending order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date format",
                    content = @Content
            )
    })
    @GetMapping(value = "/history/{from}/{to}")
    public List<Sensor> getHistory(
            @Parameter(description = "Start date in format yyyy-MM-dd-HH-mm", example = "2024-01-01-00-00", required = true)
            @PathVariable(name = "from") @DateTimeFormat(pattern = "yyyy-MM-dd-HH-mm") Date from,
            @Parameter(description = "End date in format yyyy-MM-dd-HH-mm", example = "2024-12-31-23-59", required = true)
            @PathVariable(name = "to") @DateTimeFormat(pattern = "yyyy-MM-dd-HH-mm") Date to) {
        logger.info("fetching history from {} to {}.", from, to);
        return sensorRepository.findByDateTimeBetweenOrderByDateTimeDesc(convertToLocalDateTimeViaInstant(from), convertToLocalDateTimeViaInstant(to));

    }

    @Operation(
            summary = "Get list of years with sensor data",
            description = "Returns a list of all years that have sensor data available"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Year list retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))
            )
    })
    @GetMapping(value = "/history/years")
    public List<String> getHistoryYearList() {
        return sensorRepository.getHistoryYearList();
    }

    @Operation(
            summary = "Get all sensor history",
            description = "Returns all sensor readings ever recorded (use with caution, can be large)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "All history retrieved successfully",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Sensor.class)))
            )
    })
    @GetMapping(value = "/history/all")
    public Iterable<Sensor> getHistoryAll() {
        return sensorRepository.findAll();
    }

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
