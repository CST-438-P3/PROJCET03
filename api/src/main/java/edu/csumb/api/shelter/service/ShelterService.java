package edu.csumb.api.shelter.service;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.error.NotFoundException;
import edu.csumb.api.shelter.Shelter;
import edu.csumb.api.shelter.ShelterRepository;
import edu.csumb.api.shelter.ShelterRequest;
import edu.csumb.api.shelter.ShelterResponse;
import edu.csumb.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, edits and deletes shelters and chooses their staff, for admins (S10). */
@Service
public class ShelterService {

    private final ShelterRepository shelters;
    private final UserRepository users;

    /** Creates the service with the repositories it reads and writes. */
    public ShelterService(ShelterRepository shelters, UserRepository users) {
        this.shelters = shelters;
        this.users = users;
    }

    /** Saves a new shelter and returns it. */
    @Transactional
    public ShelterResponse create(ShelterRequest request) {
        ShelterRules.CheckedShelter checked = checkWithZip(request);
        Shelter shelter = shelters.save(new Shelter(
            checked.name(), checked.address(), checked.zip(), checked.phone(), checked.email()));
        return ShelterResponse.from(shelter);
    }

    /** Replaces every field of a shelter and returns it. */
    @Transactional
    public ShelterResponse update(long shelterId, ShelterRequest request) {
        Shelter shelter = find(shelterId);
        ShelterRules.CheckedShelter checked = checkWithZip(request);
        shelter.replace(checked.name(), checked.address(), checked.zip(), checked.phone(), checked.email());
        return ShelterResponse.from(shelter);
    }

    /** Deletes a shelter; the database also deletes its staff rows and its posts, with their favorites and inquiries. */
    @Transactional
    public void delete(long shelterId) {
        shelters.delete(find(shelterId));
    }

    /** Makes the user staff for the shelter. Doing it twice is fine. */
    @Transactional
    public void addStaff(long shelterId, long userId) {
        checkShelterAndUser(shelterId, userId);
        shelters.addStaff(shelterId, userId);
    }

    /** Removes the user from the shelter's staff. Doing it when they are not staff is fine. */
    @Transactional
    public void removeStaff(long shelterId, long userId) {
        checkShelterAndUser(shelterId, userId);
        shelters.removeStaff(shelterId, userId);
    }

    /** Runs ShelterRules, then checks that the ZIP code is one we know, since shelters.zip_code must point to it. */
    private ShelterRules.CheckedShelter checkWithZip(ShelterRequest request) {
        ShelterRules.CheckedShelter checked = ShelterRules.check(request);
        if (!shelters.zipExists(checked.zip())) {
            throw new BadRequestException("We don't know ZIP code " + checked.zip());
        }
        return checked;
    }

    /** Returns the shelter, or throws NotFoundException if there is no shelter with this id. */
    private Shelter find(long shelterId) {
        return shelters.findById(shelterId).orElseThrow(() -> new NotFoundException("Shelter not found"));
    }

    /** Throws NotFoundException if the shelter or the user does not exist. */
    private void checkShelterAndUser(long shelterId, long userId) {
        if (!shelters.existsById(shelterId)) {
            throw new NotFoundException("Shelter not found");
        }
        if (!users.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
    }
}
