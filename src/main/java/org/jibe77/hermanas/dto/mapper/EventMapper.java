package org.jibe77.hermanas.dto.mapper;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.dto.EventDTO;
import org.jibe77.hermanas.service.event.EventService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    /**
     * Public mapper that strips {@code triggeredBy} — used when the journal is
     * rendered for an anonymous visitor. Login names belong to operators, not
     * the showcase dashboard.
     */
    public EventDTO toDTO(Event event) {
        return toDTO(event, false);
    }

    /**
     * Variant used by authenticated endpoints — preserves the {@code triggeredBy}
     * column so operators can see who turned the light on at 3am.
     */
    public EventDTO toDTO(Event event, boolean includeTriggeredBy) {
        if (event == null) {
            return null;
        }
        return new EventDTO(
                event.getId(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getDateTime(),
                event.getDetails(),
                includeTriggeredBy ? event.getTriggeredBy() : null
        );
    }

    public List<EventDTO> toDTOList(List<Event> events) {
        return toDTOList(events, false);
    }

    /**
     * Chooses the mapping mode dynamically: if a Spring {@code SecurityContext}
     * shows an authenticated user, {@code triggeredBy} is included; otherwise
     * it is stripped.
     */
    public List<EventDTO> toDTOListForCurrentCaller(List<Event> events) {
        return toDTOList(events, EventService.currentUsername() != null);
    }

    public List<EventDTO> toDTOList(List<Event> events, boolean includeTriggeredBy) {
        if (events == null) {
            return null;
        }
        return events.stream().map(e -> toDTO(e, includeTriggeredBy)).collect(Collectors.toList());
    }
}
