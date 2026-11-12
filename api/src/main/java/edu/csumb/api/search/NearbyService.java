package edu.csumb.api.search;

import edu.csumb.api.post.Species;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NearbyService {
    private static final double EARTH_RADIUS_MILES = 3958.8;

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

        ZipCoordinateService.Coordinate origin = zipCoordinates.lookup(zip);
        return provider.findAvailablePets().stream()
                .filter(pet -> species == null || pet.species() == species)
                .map(pet -> new NearbyPet(pet, milesBetween(origin.latitude(), origin.longitude(),
                        pet.latitude(), pet.longitude())))
                .filter(pet -> pet.distanceMiles() <= distanceMiles)
                .sorted(Comparator.comparingDouble(NearbyPet::distanceMiles))
                .toList();
    }

    private static double milesBetween(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return EARTH_RADIUS_MILES * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record NearbyPet(PetListing pet, double distanceMiles) { }
}
