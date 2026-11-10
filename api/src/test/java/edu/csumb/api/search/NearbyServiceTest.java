package edu.csumb.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.csumb.api.post.Species;
import java.util.List;
import org.junit.jupiter.api.Test;

class NearbyServiceTest {
    private final ZipCoordinateService zipCoordinates = new ZipCoordinateService();
    private final PetListingProvider pets = () -> List.of(
            new PetListing(1, "Near Shelter", "93933", "Luna", Species.CAT, 12, "Cat", null, 36.7068, -121.6555),
            new PetListing(2, "Far Shelter", "95060", "Bella", Species.DOG, 18, "Dog", null, 37.0511, -122.0839));
    private final NearbyService service = new NearbyService(pets, zipCoordinates);

    @Test
    void filtersOutPetsBeyondRadiusAndSortsNearestFirst() {
        List<NearbyService.NearbyPet> result = service.search("93933", 10, null);
        assertEquals(List.of("Luna"), result.stream().map(p -> p.pet().name()).toList());
    }

    @Test
    void filtersBySpecies() {
        assertEquals(List.of("Luna"), service.search("93933", 100, Species.CAT)
                .stream().map(p -> p.pet().name()).toList());
    }

    @Test
    void rejectsUnknownZip() {
        assertEquals("We couldn't find that ZIP code",
                assertThrows(IllegalArgumentException.class, () -> service.search("00000", 10, null)).getMessage());
    }
}
