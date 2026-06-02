package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    List<Resident> findAllByOrderByArrivalDateDescIdDesc();
}
