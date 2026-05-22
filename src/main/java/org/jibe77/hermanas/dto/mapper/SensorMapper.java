package org.jibe77.hermanas.dto.mapper;

import org.jibe77.hermanas.data.entity.Sensor;
import org.jibe77.hermanas.dto.SensorDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Mapper to convert between Sensor entity and SensorDTO.
 */
@Component
public class SensorMapper {

    /**
     * Converts a Sensor entity to a SensorDTO.
     *
     * @param sensor the Sensor entity
     * @return the SensorDTO
     */
    public SensorDTO toDTO(Sensor sensor) {
        if (sensor == null) {
            return null;
        }
        return new SensorDTO(
                sensor.getTemperature(),
                sensor.getExternalTemperature(),
                sensor.getHumidity(),
                sensor.getExternalHumidity(),
                sensor.getDateTime()
        );
    }

    /**
     * Converts a list of Sensor entities to a list of SensorDTOs.
     *
     * @param sensors the list of Sensor entities
     * @return the list of SensorDTOs
     */
    public List<SensorDTO> toDTOList(List<Sensor> sensors) {
        if (sensors == null) {
            return null;
        }
        return sensors.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts an Iterable of Sensor entities to a list of SensorDTOs.
     *
     * @param sensors the Iterable of Sensor entities
     * @return the list of SensorDTOs
     */
    public List<SensorDTO> toDTOList(Iterable<Sensor> sensors) {
        if (sensors == null) {
            return null;
        }

        return StreamSupport.stream(sensors.spliterator(), false)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts a SensorDTO to a Sensor entity.
     * Note: This is typically not used for API responses, but included for completeness.
     *
     * @param dto the SensorDTO
     * @return the Sensor entity
     */
    public Sensor toEntity(SensorDTO dto) {
        if (dto == null) {
            return null;
        }
        Sensor sensor = new Sensor();
        sensor.setTemperature(dto.getTemperature());
        sensor.setExternalTemperature(dto.getExternalTemperature());
        sensor.setHumidity(dto.getHumidity());
        sensor.setExternalHumidity(dto.getExternalHumidity());
        sensor.setDateTime(dto.getDateTime());
        return sensor;
    }
}
