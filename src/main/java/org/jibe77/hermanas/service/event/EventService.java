package org.jibe77.hermanas.service.event;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Generic event sourcing service. Records timestamped business + auth events
 * to the {@code Event} table, and serves them back paginated for the
 * Journalisation page.
 *
 * <p>Business events (door / light / fan / music / resident / system lifecycle)
 * are surfaced to every visitor of the dashboard; auth events (login success,
 * login failure, logout) are admin-only — the controller chooses which subset
 * to expose via {@link #BUSINESS_TYPES} or {@link #AUTH_TYPES}.</p>
 */
@Component
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    /** Truncate long details strings to fit the {@code Event.details} column. */
    private static final int MAX_DETAILS_LENGTH = 500;

    /**
     * Event types that are safe to expose publicly on the Journalisation page.
     * Everything *except* auth events.
     */
    public static final List<EventType> BUSINESS_TYPES = Arrays.asList(
            EventType.STARTUP,
            EventType.SHUTDOWN,
            EventType.SHUTDOWN_REQUESTED,
            EventType.REBOOT_REQUESTED,
            EventType.DOOR_OPENED,
            EventType.DOOR_CLOSED,
            EventType.DOOR_OPEN_FAILED,
            EventType.DOOR_CLOSE_FAILED,
            EventType.DOOR_POSITION_UNKNOWN,
            EventType.LIGHT_ON,
            EventType.LIGHT_OFF,
            EventType.FAN_ON,
            EventType.FAN_OFF,
            EventType.MUSIC_STARTED,
            EventType.MUSIC_STOPPED,
            EventType.COCORICO,
            EventType.RESIDENT_CREATED,
            EventType.RESIDENT_DELETED,
            EventType.RESIDENT_UPDATED,
            EventType.RESIDENT_PHOTO_UPLOADED,
            EventType.RESIDENT_PHOTO_DELETED,
            EventType.USER_CREATED,
            EventType.USER_UPDATED,
            EventType.USER_DELETED,
            EventType.USER_SELF_UPDATED,
            EventType.CONFIG_CHANGED,
            EventType.EMAIL_TEST_SENT,
            EventType.PICTURE_TAKEN
    );

    /**
     * Event types restricted to administrators. Exposing failed login attempts
     * to anonymous users would leak whether a given login exists.
     */
    public static final List<EventType> AUTH_TYPES = Arrays.asList(
            EventType.LOGIN_SUCCESS,
            EventType.LOGIN_FAILED,
            EventType.LOGOUT
    );

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Records a manually-triggered event, attributing it to the currently
     * authenticated user. {@code triggeredBy} stays {@code null} for anonymous
     * callers — the journal endpoint then treats the entry like an automatic
     * one for display.
     */
    public void record(EventType type, String details) {
        recordInternal(type, details, currentUsername());
    }

    public void record(EventType type) {
        record(type, null);
    }

    /**
     * Records an automatic (scheduler-driven) event. Forces {@code triggeredBy}
     * to {@code null} regardless of any thread-local SecurityContext leakage.
     */
    public void recordAuto(EventType type, String details) {
        recordInternal(type, details, null);
    }

    public void recordAuto(EventType type) {
        recordAuto(type, null);
    }

    private void recordInternal(EventType type, String details, String triggeredBy) {
        Event event = new Event();
        event.setEventType(type);
        event.setDateTime(LocalDateTime.now());
        event.setDetails(truncate(details));
        event.setTriggeredBy(triggeredBy);
        eventRepository.save(event);
        logger.debug("Recorded event {} (details={}, by={})", type, details, triggeredBy);
    }

    /**
     * Returns the login of the currently authenticated user, or {@code null}
     * when the call is anonymous or runs off a non-HTTP thread (scheduler).
     * Anonymous Spring authentications carry the principal name
     * {@code "anonymousUser"} which we map to {@code null} so the journal
     * cleanly distinguishes "logged-in user" from "no user".
     */
    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        if (name == null || name.isEmpty() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    /**
     * Returns the most recent events matching the given types within the time
     * range, capped at {@code limit} rows.
     */
    public List<Event> findRecent(List<EventType> types, LocalDateTime from, LocalDateTime to, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 1000));
        return eventRepository.findByEventTypeInAndDateTimeBetweenOrderByDateTimeDesc(
                types, from, to, PageRequest.of(0, cappedLimit));
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_DETAILS_LENGTH ? s : s.substring(0, MAX_DETAILS_LENGTH);
    }
}
