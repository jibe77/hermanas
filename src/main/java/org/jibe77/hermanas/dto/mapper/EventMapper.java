package org.jibe77.hermanas.dto.mapper;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.dto.EventDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    public EventDTO toDTO(Event event) {
        if (event == null) {
            return null;
        }
        return new EventDTO(
                event.getId(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getDateTime(),
                event.getDetails()
        );
    }

    public List<EventDTO> toDTOList(List<Event> events) {
        if (events == null) {
            return null;
        }
        return events.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
