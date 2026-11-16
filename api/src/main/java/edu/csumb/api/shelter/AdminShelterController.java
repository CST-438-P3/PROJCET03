package edu.csumb.api.shelter;

import edu.csumb.api.shelter.service.ShelterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The /admin/shelters endpoints from openapi.yaml (S10). SecurityConfig lets only users with the ADMIN role in
 * here: no token is 401, a token without ADMIN is 403.
 */
@RestController
@RequestMapping("/admin/shelters")
public class AdminShelterController {

    private final ShelterService shelterService;

    /** Creates the controller with the service that does the work. */
    public AdminShelterController(ShelterService shelterService) {
        this.shelterService = shelterService;
    }

    /** Creates a shelter. No Location header yet, because GET /shelters/{shelterId} comes with the admin screen. */
    @PostMapping
    public ResponseEntity<ShelterResponse> create(@RequestBody ShelterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shelterService.create(request));
    }

    /** Replaces a shelter. */
    @PutMapping("/{shelterId}")
    public ShelterResponse update(@PathVariable long shelterId, @RequestBody ShelterRequest request) {
        return shelterService.update(shelterId, request);
    }

    /** Deletes a shelter with its posts and staff. */
    @DeleteMapping("/{shelterId}")
    public ResponseEntity<Void> delete(@PathVariable long shelterId) {
        shelterService.delete(shelterId);
        return ResponseEntity.noContent().build();
    }

    /** Makes the user staff for the shelter, so they can post for it. */
    @PutMapping("/{shelterId}/staff/{userId}")
    public ResponseEntity<Void> addStaff(@PathVariable long shelterId, @PathVariable long userId) {
        shelterService.addStaff(shelterId, userId);
        return ResponseEntity.noContent().build();
    }

    /** Removes the user from the shelter's staff. */
    @DeleteMapping("/{shelterId}/staff/{userId}")
    public ResponseEntity<Void> removeStaff(@PathVariable long shelterId, @PathVariable long userId) {
        shelterService.removeStaff(shelterId, userId);
        return ResponseEntity.noContent().build();
    }
}
