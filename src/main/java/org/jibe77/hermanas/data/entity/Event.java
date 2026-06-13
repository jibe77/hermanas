package org.jibe77.hermanas.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Stored as ordinal (Hibernate default) to stay compatible with existing
    // rows on the production Pi DB. Any new EventType MUST be appended at the
    // end of the enum to avoid renumbering historical events.
    private EventType eventType;

    private LocalDateTime dateTime;

    @Column(length = 500)
    private String details;

    /**
     * Login of the authenticated user who triggered the event, or {@code null}
     * for scheduler-driven events. Exposed by the journal endpoint only to
     * authenticated callers — anonymous visitors see {@code null}.
     */
    @Column(length = 64)
    private String triggeredBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", dateTime=" + dateTime +
                ", details=" + details +
                ", triggeredBy=" + triggeredBy +
                '}';
    }
}
