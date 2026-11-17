package org.jibe77.hermanas.security.ratelimit;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.service.event.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Progressive account lockout (AUTH-VULN-03).
 *
 * <p>The administrator account absorbed 165 failed attempts during the engagement and was
 * never locked, never challenged, never slowed independently of the source address. These
 * tests pin the account-side behaviour that replaces that.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginAttemptServiceTest {

    @Mock
    private HermanasUserRepository repository;
    @Mock
    private EventService eventService;

    private LoginAttemptService service;
    private HermanasUser user;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(repository, eventService);
        ReflectionTestUtils.setField(service, "failureThreshold", 5);
        ReflectionTestUtils.setField(service, "initialLockout", Duration.ofMinutes(1));
        ReflectionTestUtils.setField(service, "maxLockout", Duration.ofHours(1));
        user = new HermanasUser("marguerite", "x", null, "ADMIN", true);
        when(repository.findByLogin(anyString())).thenReturn(Optional.of(user));
    }

    @Test
    void noLockoutBeforeTheThreshold() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("marguerite");
        }
        assertEquals(4, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
        assertFalse(service.isLocked("marguerite"));
    }

    @Test
    void thresholdTripsTheFirstOneMinuteLockoutAndAlerts() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("marguerite");
        }
        assertEquals(5, user.getFailedAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(service.isLocked("marguerite"));
        // The burst must be visible to the operator, on the admin-only journal.
        verify(eventService).recordAuto(eq(EventType.LOGIN_LOCKED_OUT), anyString());
    }

    @Test
    void backoffDoublesOnTheSecondTrip() {
        for (int i = 0; i < 10; i++) {
            service.recordFailure("marguerite");
        }
        // #5 is the first trip (1 min), #10 the second (2 min).
        Duration remaining = service.remainingLockout("marguerite");
        assertNotNull(remaining);
        assertTrue(remaining.toSeconds() > 60 && remaining.toSeconds() <= 120,
                "expected roughly a two-minute lock, got " + remaining);
    }

    @Test
    void backoffStopsAtTheCeiling() {
        ReflectionTestUtils.setField(service, "maxLockout", Duration.ofMinutes(2));
        for (int i = 0; i < 40; i++) {
            service.recordFailure("marguerite");
        }
        Duration remaining = service.remainingLockout("marguerite");
        assertNotNull(remaining);
        assertTrue(remaining.toMinutes() <= 2, "lockout exceeded the ceiling: " + remaining);
    }

    @Test
    void successClearsTheCounterAndTheLock() {
        for (int i = 0; i < 6; i++) {
            service.recordFailure("marguerite");
        }
        assertTrue(service.isLocked("marguerite"));

        service.recordSuccess("marguerite");

        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
        assertFalse(service.isLocked("marguerite"));
    }

    @Test
    void unknownLoginIsSilentlyIgnoredAndNeverReportsLocked() {
        when(repository.findByLogin(anyString())).thenReturn(Optional.empty());

        service.recordFailure("does-not-exist");
        service.recordSuccess("does-not-exist");

        assertFalse(service.isLocked("does-not-exist"));
        verify(repository, never()).save(any(HermanasUser.class));
    }
}
