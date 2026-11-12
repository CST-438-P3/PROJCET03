package edu.csumb.api.search;

import java.util.List;

/**
 * Boundary for the external pet-listing service. The real provider can be
 * added later without changing the HTTP contract or search rules.
 */
public interface PetListingProvider {
    List<PetListing> findAvailablePets();
}
