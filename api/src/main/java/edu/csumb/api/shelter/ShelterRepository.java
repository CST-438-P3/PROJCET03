package edu.csumb.api.shelter;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads rows of shelters and which shelters a user is staff for. */
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    /** The ids of the shelters this user is staff for, from shelter_staff. */
    @Query(value = "SELECT shelter_id FROM shelter_staff WHERE user_id = :userId", nativeQuery = true)
    Set<Long> findShelterIdsForStaff(@Param("userId") long userId);
}
