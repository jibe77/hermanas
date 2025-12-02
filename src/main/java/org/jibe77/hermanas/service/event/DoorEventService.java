package org.jibe77.hermanas.controller.event;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.jibe77.hermanas.service.door.model.DoorStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for recording and querying door state change events.
 *
 * <p>Provides event sourcing capabilities for door operations, maintaining
 * a complete audit trail of all door state changes.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Records all door open/close operations</li>
 *   <li>Tracks failed operations for troubleshooting</li>
 *   <li>Provides historical event queries with time filtering</li>
 *   <li>Supports event sourcing pattern for state reconstruction</li>
 * </ul>
 *
 * @see Event
 * @see EventType
 * @see EventRepository
 */
@Component
public class DoorEventService {

    private static final Logger logger = LoggerFactory.getLogger(DoorEventService.class);

    private final EventRepository eventRepository;

    // Door-related event types for queries
    private static final List<EventType> DOOR_EVENT_TYPES = Arrays.asList(
            EventType.DOOR_OPENED,
            EventType.DOOR_CLOSED,
            EventType.DOOR_OPEN_FAILED,
            EventType.DOOR_CLOSE_FAILED,
            EventType.DOOR_POSITION_UNKNOWN
    );

    public DoorEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Records a door opened event.
     */
    public void recordDoorOpened() {
        recordEvent(EventType.DOOR_OPENED);
        logger.info("Recorded DOOR_OPENED event");
    }

    /**
     * Records a door closed event.
     */
    public void recordDoorClosed() {
        recordEvent(EventType.DOOR_CLOSED);
        logger.info("Recorded DOOR_CLOSED event");
    }

    /**
     * Records a door open failure event.
     */
    public void recordDoorOpenFailed() {
        recordEvent(EventType.DOOR_OPEN_FAILED);
        logger.warn("Recorded DOOR_OPEN_FAILED event");
    }

    /**
     * Records a door close failure event.
     */
    public void recordDoorCloseFailed() {
        recordEvent(EventType.DOOR_CLOSE_FAILED);
        logger.warn("Recorded DOOR_CLOSE_FAILED event");
    }

    /**
     * Records a door position unknown event.
     */
    public void recordDoorPositionUnknown() {
        recordEvent(EventType.DOOR_POSITION_UNKNOWN);
        logger.warn("Recorded DOOR_POSITION_UNKNOWN event");
    }

    /**
     * Records an event based on door status.
     *
     * @param status the door status to record
     */
    public void recordDoorStatusEvent(DoorStatusEnum status) {
        EventType eventType;
        switch (status) {
            case OPENED:
                eventType = EventType.DOOR_OPENED;
                break;
            case CLOSED:
                eventType = EventType.DOOR_CLOSED;
                break;
            case SEEMS_OPENED:
            case SEEMS_CLOSED:
            case UNDEFINED:
            default:
                eventType = EventType.DOOR_POSITION_UNKNOWN;
                break;
        }
        recordEvent(eventType);
        logger.info("Recorded {} event from status: {}", eventType, status);
    }

    /**
     * Gets all door events, newest first.
     *
     * @return list of all door events
     */
    public List<Event> getAllDoorEvents() {
        return eventRepository.findByEventTypeInOrderByDateTimeDesc(
                DOOR_EVENT_TYPES.toArray(new EventType[0])
        );
    }

    /**
     * Gets door events within a time range.
     *
     * @param start start of time range (inclusive)
     * @param end end of time range (inclusive)
     * @return list of door events in time range, newest first
     */
    public List<Event> getDoorEventsBetween(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByEventTypeInAndDateTimeBetweenOrderByDateTimeDesc(
                DOOR_EVENT_TYPES, start, end
        );
    }

    /**
     * Gets the most recent door event.
     *
     * @return most recent door event, or null if none found
     */
    public Event getLatestDoorEvent() {
        return eventRepository.findTopByEventTypeInOrderByDateTimeDesc(
                DOOR_EVENT_TYPES.toArray(new EventType[0])
        );
    }

    /**
     * Generic event recording helper.
     *
     * @param eventType the type of event to record
     */
    private void recordEvent(EventType eventType) {
        Event event = new Event();
        event.setEventType(eventType);
        event.setDateTime(LocalDateTime.now());
        eventRepository.save(event);
    }
}
