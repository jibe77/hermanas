package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Event entity - provides event sourcing and audit trail capabilities.
 *
 * <p>Supports querying events by type and time range for historical analysis.</p>
 */
public interface EventRepository extends CrudRepository<Event, Long> {

    /**
     * Finds the most recent event matching any of the given event types.
     *
     * @param eventTypes variable number of event types to search for
     * @return the most recent matching event, or null if none found
     */
    Event findTopByEventTypeInOrderByDateTimeDesc(EventType... eventTypes);

    /**
     * Finds all events matching any of the given event types, ordered by time descending.
     *
     * @param eventTypes variable number of event types to search for
     * @return list of matching events, newest first
     */
    List<Event> findByEventTypeInOrderByDateTimeDesc(EventType... eventTypes);

    /**
     * Finds all events within a time range, ordered by time descending.
     *
     * @param start start of time range (inclusive)
     * @param end end of time range (inclusive)
     * @return list of events in time range, newest first
     */
    List<Event> findByDateTimeBetweenOrderByDateTimeDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Finds all events matching event types within a time range, ordered by time descending.
     *
     * @param eventTypes variable number of event types to search for
     * @param start start of time range (inclusive)
     * @param end end of time range (inclusive)
     * @return list of matching events in time range, newest first
     */
    List<Event> findByEventTypeInAndDateTimeBetweenOrderByDateTimeDesc(List<EventType> eventTypes, LocalDateTime start, LocalDateTime end);

    /**
     * Finds events matching the given types within a time range, paginated.
     * Used by the Journalisation page to cap the number of rows returned per call.
     *
     * @param eventTypes types to include
     * @param start start of time range (inclusive)
     * @param end end of time range (inclusive)
     * @param pageable size/sort hint (sort already enforced by method name)
     * @return page of events, newest first
     */
    List<Event> findByEventTypeInAndDateTimeBetweenOrderByDateTimeDesc(List<EventType> eventTypes, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
