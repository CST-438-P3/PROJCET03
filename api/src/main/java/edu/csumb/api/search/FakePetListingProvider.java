package edu.csumb.api.search;

import edu.csumb.api.post.Species;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Seed data used until the external API credentials are available. */
@Component
@Profile("!pet-provider")
public class FakePetListingProvider implements PetListingProvider {
    @Override
    public List<PetListing> findAvailablePets() {
        return List.of(
                new PetListing(1, 101, "Monterey County Animal Services", "93933", "Luna", Species.CAT,
                        24, "A gentle lap cat who loves sunny windows.", null, 36.7068, -121.6555),
                new PetListing(2, 102, "Salinas Valley Shelter", "93906", "Max", Species.DOG,
                        36, "A playful dog looking for an active home.", null, 36.6777, -121.6550),
                new PetListing(3, 103, "Santa Cruz Animal Care", "95060", "Bella", Species.DOG,
                        18, "A friendly dog who enjoys walks.", null, 37.0511, -122.0839));
    }
}
