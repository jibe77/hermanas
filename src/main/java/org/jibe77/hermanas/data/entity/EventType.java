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

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║ Stored as ORDINAL in the production DB — see Event.eventType.       ║
    // ║ NEVER reorder existing values; NEVER insert in the middle; ALWAYS   ║
    // ║ append new types at the bottom.                                     ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // System events (legacy positions 0-1)
    STARTUP,
    SHUTDOWN,

    // Door events (legacy positions 2-6)
    DOOR_OPENED,
    DOOR_CLOSED,
    DOOR_OPEN_FAILED,
    DOOR_CLOSE_FAILED,
    DOOR_POSITION_UNKNOWN,

    // -- New types appended for the "Journalisation" feature --

    // System
    SHUTDOWN_REQUESTED,
    REBOOT_REQUESTED,

    // Light
    LIGHT_ON,
    LIGHT_OFF,

    // Fan
    FAN_ON,
    FAN_OFF,

    // Music
    MUSIC_STARTED,
    MUSIC_STOPPED,
    COCORICO,

    // Resident
    RESIDENT_CREATED,
    RESIDENT_DELETED,

    // Auth (admin-only visibility)
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT
}
