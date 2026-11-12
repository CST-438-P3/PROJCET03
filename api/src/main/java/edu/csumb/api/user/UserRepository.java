package edu.csumb.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Matches the unique index on lower(email). */
    Optional<User> findByEmailIgnoreCase(String email);
}
