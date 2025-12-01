package org.waterwood.waterfunservicecore.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservicecore.entity.user.UserDatum;

import java.util.Optional;

public interface UserDatumRepo extends JpaRepository<UserDatum, Long> {
    Optional<UserDatum> findByEmailHash(String emailHash);
    Optional<UserDatum> findByPhoneHash(String phoneHash);
}
