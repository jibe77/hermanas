package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Sensor;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorRepository extends CrudRepository<Sensor, Long> {

    List<Sensor> findByDateTimeGreaterThan(LocalDateTime minusDays);
}
