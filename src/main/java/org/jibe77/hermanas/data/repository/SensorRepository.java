package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Sensor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorRepository extends CrudRepository<Sensor, Long> {

    List<Sensor> findByDateTimeGreaterThan(LocalDateTime minusDays);

    List<Sensor> findByDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query(value="select distinct date_format(date_time,'%Y') from sensor", nativeQuery=true)
    List<String> getHistoryYearList();
}
