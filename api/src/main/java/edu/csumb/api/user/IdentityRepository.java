package edu.csumb.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, Long> {

    Optional<Identity> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    Optional<Identity> findFirstByUserOrderById(User user);
}
