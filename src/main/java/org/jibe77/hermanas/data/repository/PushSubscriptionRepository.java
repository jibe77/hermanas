package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.PushSubscription;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PushSubscriptionRepository extends CrudRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);

    /**
     * La propriété d'un abonnement se juge par (utilisateur, endpoint), jamais par l'endpoint
     * seul : deux comptes ne doivent pas pouvoir se disputer le même abonnement.
     */
    Optional<PushSubscription> findByUsernameAndEndpoint(String username, String endpoint);

    void deleteByUsernameAndEndpoint(String username, String endpoint);
}
