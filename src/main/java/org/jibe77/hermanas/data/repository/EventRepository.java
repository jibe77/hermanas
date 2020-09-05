package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Long> {

    Event findTopByEventTypeInOrderByDateTimeDesc(EventType ... eventTypes);
}
