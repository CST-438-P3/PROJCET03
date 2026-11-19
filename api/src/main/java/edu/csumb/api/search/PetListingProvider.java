package edu.csumb.api.search;

import edu.csumb.api.post.Species;
import java.util.List;

/**
 * Boundary for the external pet-listing service. The real provider can be
 * added later without changing the HTTP contract or search rules.
 */
public interface PetListingProvider {
    List<PetListing> findAvailablePets();

    default List<PetDistance> search(String zip, int distanceMiles, Species species,
                                     ZipCoordinateService zipCoordinates) {
        ZipCoordinateService.Coordinate origin = zipCoordinates.lookup(zip);
        return findAvailablePets().stream()
                .filter(pet -> species == null || pet.species() == species)
                .map(pet -> new PetDistance(pet, milesBetween(origin.latitude(), origin.longitude(),
                        pet.latitude(), pet.longitude())))
                .filter(pet -> pet.distanceMiles() <= distanceMiles)
                .toList();
    }

    private static double milesBetween(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 3958.8 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    record PetDistance(PetListing pet, double distanceMiles) { }
}
