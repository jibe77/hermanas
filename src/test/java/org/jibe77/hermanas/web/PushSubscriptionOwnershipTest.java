package org.jibe77.hermanas.web;

import org.jibe77.hermanas.data.entity.PushSubscription;
import org.jibe77.hermanas.data.repository.PushSubscriptionRepository;
import org.jibe77.hermanas.service.push.PushNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AUTHZ-VULN-04 : un abonnement Web Push a un proprietaire, et ce proprietaire ne
 * change pas parce qu'un autre compte authentifie presente le meme endpoint.
 *
 * <p>Test unitaire direct (pas de contexte Spring) : le controleur ne depend que du
 * service push et du repository, et l'authentification se pose dans le
 * SecurityContextHolder.
 */
class PushSubscriptionOwnershipTest {

    private static final String ENDPOINT =
            "https://push.example.org/wpush/v1/0123456789abcdef";

    private PushNotificationService pushService;
    private PushSubscriptionRepository repository;
    private PushRestController controller;

    @BeforeEach
    void setUp() {
        pushService = mock(PushNotificationService.class);
        repository = mock(PushSubscriptionRepository.class);
        controller = new PushRestController(pushService, repository);
        when(pushService.endpointRefusalReason(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE" + role))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, authorities));
    }

    private PushSubscription ownedBy(String username) {
        PushSubscription sub = new PushSubscription(ENDPOINT, "old-p256dh", "old-auth", username);
        sub.setId(42L);
        return sub;
    }

    private PushRestController.SubscribeRequest request() {
        PushRestController.SubscribeRequest req = new PushRestController.SubscribeRequest();
        req.endpoint = ENDPOINT;
        req.p256dh = "new-p256dh";
        req.auth = "new-auth";
        return req;
    }

    @Test
    void firstSubscriptionStoresTheCallerAsOwner() {
        authenticate("marguerite", "USER");
        when(repository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.subscribe(request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<PushSubscription> saved = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("marguerite");
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void aSecondUserRefreshingTheSameEndpointDoesNotStealIt() {
        authenticate("jean", "USER");
        PushSubscription existing = ownedBy("marguerite");
        when(repository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(existing));

        ResponseEntity<Map<String, Object>> response = controller.subscribe(request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Les cles tournent (le service de push les fait pivoter cote navigateur)...
        assertThat(existing.getP256dh()).isEqualTo("new-p256dh");
        assertThat(existing.getAuth()).isEqualTo("new-auth");
        // ... mais la propriete reste a marguerite, et c'est ce que l'on renvoie.
        assertThat(existing.getUsername()).isEqualTo("marguerite");
        assertThat(response.getBody()).containsEntry("username", "marguerite");
        verify(repository).save(existing);
    }

    @Test
    void anUnownedRowIsClaimedByTheFirstCaller() {
        authenticate("jean", "USER");
        when(repository.findByEndpoint(ENDPOINT)).thenReturn(Optional.of(ownedBy(null)));

        controller.subscribe(request());

        ArgumentCaptor<PushSubscription> saved = ArgumentCaptor.forClass(PushSubscription.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("jean");
    }

    @Test
    void anonymousCallerIsRefused() {
        ResponseEntity<Map<String, Object>> response = controller.subscribe(request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(repository, never()).save(any());
    }

    @Test
    void unsubscribeOnlyRemovesTheCallersOwnSubscription() {
        authenticate("jean", "USER");
        PushRestController.UnsubscribeRequest req = new PushRestController.UnsubscribeRequest();
        req.endpoint = ENDPOINT;

        ResponseEntity<Void> response = controller.unsubscribe(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(repository).deleteByUsernameAndEndpoint("jean", ENDPOINT);
        verify(repository, never()).deleteByEndpoint(any());
    }

    @Test
    void adminCanPruneAnySubscription() {
        authenticate("marguerite", "ADMIN");
        PushRestController.UnsubscribeRequest req = new PushRestController.UnsubscribeRequest();
        req.endpoint = ENDPOINT;

        controller.unsubscribe(req);

        verify(repository).deleteByEndpoint(ENDPOINT);
        verify(repository, never()).deleteByUsernameAndEndpoint(any(), any());
    }
}
