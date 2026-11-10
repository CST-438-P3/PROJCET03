package edu.csumb.api.search;

import edu.csumb.api.post.Species;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class NearbyController {
    private final NearbyService nearbyService;

    public NearbyController(NearbyService nearbyService) {
        this.nearbyService = nearbyService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<NearbyResponse> nearby(
            @RequestParam String zip,
            @RequestParam(defaultValue = "10") int distance,
            @RequestParam(required = false) Species species) {
        List<NearbyService.NearbyPet> pets;
        try {
            pets = nearbyService.search(zip, distance, species);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        List<NearbyPost> content = pets.stream().map(result -> new NearbyPost(
                result.pet().id(),
                new ShelterSummary(result.pet().id(), result.pet().shelterName()),
                result.pet().name(),
                result.pet().species(),
                result.pet().ageMonths(),
                result.pet().description(),
                result.pet().photoUrl(),
                "AVAILABLE",
                Math.round(result.distanceMiles() * 10.0) / 10.0)).toList();
        int totalPages = content.isEmpty() ? 0 : 1;
        return ResponseEntity.ok(new NearbyResponse(0, content.size(), content.size(), totalPages, content));
    }

    public record NearbyResponse(int page, int size, long totalElements, int totalPages,
                                 List<NearbyPost> content) { }
    public record ShelterSummary(long id, String name) { }
    public record NearbyPost(long id, ShelterSummary shelter, String name, Species species,
                             Integer ageMonths, String description, String photoUrl,
                             String status, double distanceMiles) { }
}
