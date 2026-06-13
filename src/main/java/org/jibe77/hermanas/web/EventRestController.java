package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.dto.EventDTO;
import org.jibe77.hermanas.dto.mapper.EventMapper;
import org.jibe77.hermanas.service.event.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal endpoints — surface the {@code Event} table to the Journalisation
 * page. Two flavours:
 *
 * <ul>
 *   <li><b>/business</b> — door / light / fan / music / resident / system
 *       lifecycle events. Public (visible to anyone hitting the dashboard).</li>
 *   <li><b>/auth</b> — login success/failure/logout. Restricted to
 *       administrators via the SecurityConfig URL rule
 *       ({@code /api/v1/events/auth/**}).</li>
 * </ul>
 *
 * <p>Both endpoints accept a {@code from}/{@code to} time window (ISO-8601
 * datetimes) and a {@code limit} (capped at 1000 in the service).</p>
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Journal", description = "Business and authentication event log (the Journalisation page)")
public class EventRestController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventRestController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @Operation(
            summary = "List business events",
            description = "Returns business events (door / light / fan / music / resident / system) within the given time window, newest first."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events returned",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventDTO.class)))
            )
    })
    @GetMapping("/business")
    public List<EventDTO> listBusinessEvents(
            @Parameter(description = "Window start (inclusive). Defaults to 7 days ago.")
            @RequestParam(value = "from", required = false) String from,
            @Parameter(description = "Window end (inclusive). Defaults to now.")
            @RequestParam(value = "to", required = false) String to,
            @Parameter(description = "Max rows to return (1-1000). Defaults to 200.")
            @RequestParam(value = "limit", required = false, defaultValue = "200") int limit) {
        LocalDateTime fromTs = parseOrDefault(from, LocalDateTime.now().minusDays(7));
        LocalDateTime toTs = parseOrDefault(to, LocalDateTime.now());
        // toDTOListForCurrentCaller masks `triggeredBy` for anonymous visitors
        // so the public showcase never exposes operator logins, while a logged-in
        // user sees who hit the open-door button.
        return eventMapper.toDTOListForCurrentCaller(
                eventService.findRecent(EventService.BUSINESS_TYPES, fromTs, toTs, limit));
    }

    @Operation(
            summary = "List authentication events",
            description = "Returns LOGIN_SUCCESS / LOGIN_FAILED / LOGOUT events. Admin-only."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events returned",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventDTO.class)))
            ),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    @GetMapping("/auth")
    public List<EventDTO> listAuthEvents(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "limit", required = false, defaultValue = "200") int limit) {
        LocalDateTime fromTs = parseOrDefault(from, LocalDateTime.now().minusDays(7));
        LocalDateTime toTs = parseOrDefault(to, LocalDateTime.now());
        // Admin-only endpoint — always expose triggeredBy.
        return eventMapper.toDTOList(
                eventService.findRecent(EventService.AUTH_TYPES, fromTs, toTs, limit),
                true);
    }

    private static LocalDateTime parseOrDefault(String value, LocalDateTime fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
