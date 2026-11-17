package org.jibe77.hermanas.dto.mapper;

import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.dto.EventDTO;
import org.jibe77.hermanas.service.event.EventService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for the anonymous journal feed (AUTH-VULN-02).
 *
 * <p>The public {@code GET /api/v1/events/business} endpoint is reachable without a
 * session and copies the free-text {@code details} column verbatim. That is how every
 * operator login and role was harvested in 33 requests: the {@code USER_*} rows carry
 * {@code login=<user> role=<role>} and had been added to the public type list without
 * re-checking the masking assumption. This test is the guard that stops the same class of
 * regression coming back — if a future event type puts an identity or a secret in
 * {@code details}, it fails here.</p>
 */
class EventMapperAnonymousTest {

    /** Anything that must never appear with a real value in an anonymous payload. */
    private static final Pattern FORBIDDEN =
            Pattern.compile("\\b(login|role|email|password|token|secret|api-key|key|appid)=[^\\s*]");

    private final EventMapper mapper = new EventMapper();

    private static Event event(EventType type, String details, String triggeredBy) {
        Event event = new Event();
        event.setId(1L);
        event.setEventType(type);
        event.setDateTime(LocalDateTime.now());
        event.setDetails(details);
        event.setTriggeredBy(triggeredBy);
        return event;
    }

    @Test
    void anonymousFeedNeverCarriesAnIdentityOrASecret() {
        List<Event> rows = List.of(
                event(EventType.USER_CREATED, "login=marguerite role=ADMIN", "marguerite"),
                event(EventType.USER_UPDATED, "login=jb fields=[password]", "marguerite"),
                event(EventType.USER_DELETED, "login=batman role=USER", "marguerite"),
                event(EventType.CONFIG_CHANGED,
                        "key=spring.mail.password old=hunter2 new=s3cr3t", "marguerite"),
                event(EventType.CONFIG_CHANGED,
                        "key=weather.info.key old=<unset> new=ABCDEF123456", "marguerite"),
                event(EventType.DOOR_OPENED, "by hand", "marguerite")
        );

        List<EventDTO> anonymous = mapper.toDTOList(rows, false);

        for (EventDTO dto : anonymous) {
            assertNull(dto.getTriggeredBy(),
                    "triggeredBy must be stripped for an anonymous caller: " + dto.getDetails());
            if (dto.getDetails() != null) {
                assertFalse(FORBIDDEN.matcher(dto.getDetails()).find(),
                        "anonymous details leak: " + dto.getDetails());
            }
        }
    }

    @Test
    void scrubbingKeepsTheHarmlessPartOfTheDetails() {
        EventDTO dto = mapper.toDTO(
                event(EventType.USER_UPDATED, "login=marguerite fields=[email,role=USER]", null),
                false);

        assertEquals("login=*** fields=[email,role=***]", dto.getDetails());
    }

    @Test
    void authenticatedCallerStillSeesTheFullDetails() {
        Event row = event(EventType.USER_UPDATED, "login=marguerite role=ADMIN", "marguerite");

        EventDTO dto = mapper.toDTO(row, true);

        assertEquals("login=marguerite role=ADMIN", dto.getDetails());
        assertEquals("marguerite", dto.getTriggeredBy());
    }

    /**
     * Ce qui relève de l'opérateur ne doit pas être sur la liste publique : ni les comptes
     * (AUTH-VULN-02), ni l'historique de configuration (CRYPTO-007).
     *
     * <p>Le caviardage du mapper ne suffit pas pour {@code CONFIG_CHANGED} : sa colonne
     * {@code details} est {@code key=<nom> old=<ancien> new=<nouveau>}, et si le mapper
     * masque bien {@code key=}, la valeur utile à un attaquant est derrière {@code new=},
     * qu'aucune règle ne masque — et pour cause, elle n'a pas de nom de clé reconnaissable.
     * C'est la raison pour laquelle cette catégorie passe derrière {@code hasRole(ADMIN)},
     * et non un raffinement du motif.</p>
     */
    @Test
    void operatorTelemetryIsNotOnThePublicTypeList() {
        List<EventType> publicTypes = EventService.BUSINESS_TYPES;

        for (EventType type : List.of(EventType.USER_CREATED, EventType.USER_UPDATED,
                EventType.USER_DELETED, EventType.USER_SELF_UPDATED,
                EventType.CONFIG_CHANGED)) {
            assertFalse(publicTypes.contains(type),
                    type + " discloses operator telemetry and must not be public");
            assertTrue(EventService.AUTH_TYPES.contains(type),
                    type + " should still be visible on the admin-only journal");
        }
    }
}
