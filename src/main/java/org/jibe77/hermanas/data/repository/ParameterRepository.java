package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Parameter;
import org.springframework.data.repository.CrudRepository;

public interface ParameterRepository extends CrudRepository<Parameter, Long> {

    Parameter findByEntryKey(String entryKey);
}
