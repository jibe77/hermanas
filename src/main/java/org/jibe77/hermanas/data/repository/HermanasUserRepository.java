package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.HermanasUser;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HermanasUserRepository extends CrudRepository<HermanasUser, Long> {

    Optional<HermanasUser> findByLogin(String login);

    boolean existsByLogin(String login);

    List<HermanasUser> findByNotificationsEnabledTrue();

    List<HermanasUser> findByRole(String role);

    long countByRole(String role);

    void deleteByLogin(String login);
}
