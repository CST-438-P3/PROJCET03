package edu.csumb.api.search;

import edu.csumb.api.post.Species;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NearbyService {
    private final PetListingProvider provider;
    private final ZipCoordinateService zipCoordinates;

    public NearbyService(PetListingProvider provider, ZipCoordinateService zipCoordinates) {
        this.provider = provider;
        this.zipCoordinates = zipCoordinates;
    }

    public List<NearbyPet> search(String zip, int distanceMiles, Species species) {
        if (zip == null || !zip.matches("[0-9]{5}")) {
            throw new IllegalArgumentException("ZIP code must be five digits");
        }
        if (distanceMiles < 1 || distanceMiles > 100) {
            throw new IllegalArgumentException("Distance must be between 1 and 100 miles");
        }

        return provider.search(zip, distanceMiles, species, zipCoordinates).stream()
                .map(result -> new NearbyPet(result.pet(), result.distanceMiles()))
                .sorted(Comparator.comparingDouble(NearbyPet::distanceMiles))
                .toList();
    }

    public record NearbyPet(PetListing pet, double distanceMiles) { }
}
