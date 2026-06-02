package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "A single journal entry (business or auth event)")
public class EventDTO {

    @Schema(example = "12345")
    private Long id;

    @Schema(example = "DOOR_OPENED")
    private String eventType;

    @Schema(example = "2026-05-30T18:42:11")
    private LocalDateTime dateTime;

    @Schema(description = "Optional human-readable context. Null when there is nothing extra to say.",
            example = "by jb")
    private String details;

    public EventDTO() {
    }

    public EventDTO(Long id, String eventType, LocalDateTime dateTime, String details) {
        this.id = id;
        this.eventType = eventType;
        this.dateTime = dateTime;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
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
}
