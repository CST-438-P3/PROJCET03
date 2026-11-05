package edu.csumb.api.post.service;

import java.util.Set;

/** Rules for adoption posts from PRD stories S5 and S7, kept apart from controllers so they are easy to test. */
public final class PostRules {

    private PostRules() {
    }

    /** Checks the fields of a new or edited post and throws IllegalArgumentException if one breaks openapi.yaml. */
    public static void checkPost(String name, String species, Integer ageMonths, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.length() > 60) {
            throw new IllegalArgumentException("Name must be 60 characters or less");
        }
        if (!"CAT".equals(species) && !"DOG".equals(species)) {
            throw new IllegalArgumentException("Species must be CAT or DOG");
        }
        if (ageMonths != null && (ageMonths < 0 || ageMonths > 360)) {
            throw new IllegalArgumentException("Age must be between 0 and 360 months");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description must be 2000 characters or less");
        }
    }

    /** Says whether a user who is staff at the given shelters may edit or delete a post of that shelter (S7). */
    public static boolean canEdit(Set<Long> staffShelterIds, long postShelterId) {
        return staffShelterIds != null && staffShelterIds.contains(postShelterId);
    }

    /** Says whether a post with this status should show up in nearby search (S5, S7). */
    public static boolean showInSearch(String status) {
        return "AVAILABLE".equals(status);
    }
}
