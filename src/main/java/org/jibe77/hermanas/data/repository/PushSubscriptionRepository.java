package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.PushSubscription;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends CrudRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUsername(String username);

    void deleteByEndpoint(String endpoint);
}
