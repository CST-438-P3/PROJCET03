package edu.csumb.api.shelter;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Reads and writes shelters, the ZIP codes they use, and which users are staff for them. */
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    /** The ids of the shelters this user is staff for, from shelter_staff. */
    @Query(value = "SELECT shelter_id FROM shelter_staff WHERE user_id = :userId", nativeQuery = true)
    Set<Long> findShelterIdsForStaff(@Param("userId") long userId);

    /** Says whether this ZIP code is in zip_codes, which shelters.zip_code must point to. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM zip_codes WHERE zip = :zip)", nativeQuery = true)
    boolean zipExists(@Param("zip") String zip);

    /** Makes the user staff for the shelter; does nothing if they already are. */
    @Modifying
    @Query(value = "INSERT INTO shelter_staff (user_id, shelter_id) VALUES (:userId, :shelterId) ON CONFLICT DO NOTHING",
        nativeQuery = true)
    void addStaff(@Param("shelterId") long shelterId, @Param("userId") long userId);

    /** Removes the user from the shelter's staff; does nothing if they were not staff. */
    @Modifying
    @Query(value = "DELETE FROM shelter_staff WHERE user_id = :userId AND shelter_id = :shelterId", nativeQuery = true)
    void removeStaff(@Param("shelterId") long shelterId, @Param("userId") long userId);
}
