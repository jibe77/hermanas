package org.jibe77.hermanas.data.entity;

/**
 * Event types for event sourcing and audit trail.
 *
 * <p>System events:</p>
 * <ul>
 *   <li>STARTUP - Application started</li>
 *   <li>SHUTDOWN - Application shutting down</li>
 * </ul>
 *
 * <p>Door state change events:</p>
 * <ul>
 *   <li>DOOR_OPENED - Door successfully opened</li>
 *   <li>DOOR_CLOSED - Door successfully closed</li>
 *   <li>DOOR_OPEN_FAILED - Door failed to open</li>
 *   <li>DOOR_CLOSE_FAILED - Door failed to close</li>
 *   <li>DOOR_POSITION_UNKNOWN - Door position could not be determined</li>
 * </ul>
 */
public enum EventType {

    // System events
    STARTUP,
    SHUTDOWN,

    // Door events
    DOOR_OPENED,
    DOOR_CLOSED,
    DOOR_OPEN_FAILED,
    DOOR_CLOSE_FAILED,
    DOOR_POSITION_UNKNOWN
}
