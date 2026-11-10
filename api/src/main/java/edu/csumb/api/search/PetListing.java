package edu.csumb.api.search;

import edu.csumb.api.post.Species;

/** Normalized data returned by either the fake or third-party provider. */
public record PetListing(
        long id,
        String shelterName,
        String shelterZip,
        String name,
        Species species,
        Integer ageMonths,
        String description,
        String photoUrl,
        double latitude,
        double longitude) {
}
