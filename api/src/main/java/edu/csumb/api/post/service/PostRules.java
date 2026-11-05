package edu.csumb.api.post.service;

import edu.csumb.api.post.PostStatus;
import edu.csumb.api.post.Species;
import java.util.Set;

/** Rules for adoption posts from PRD stories S5 and S7, kept apart from controllers so they are easy to test. */
public final class PostRules {

    /** Longest pet name, the same as maxLength in openapi.yaml. */
    public static final int NAME_MAX_LENGTH = 60;

    /** Oldest age in months, the same as maximum in openapi.yaml. */
    public static final int AGE_MAX_MONTHS = 360;

    /** Longest description, the same as maxLength in openapi.yaml. */
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    private PostRules() {
    }

    /** Removes spaces at the start and end of a text field before it is checked and saved. */
    public static String clean(String text) {
        return text == null ? null : text.strip();
    }

    /** Checks the fields of a new or edited post and throws IllegalArgumentException if one breaks openapi.yaml. */
    public static void checkPost(String name, Species species, Integer ageMonths, String description) {
        String cleanName = clean(name);
        if (cleanName == null || cleanName.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (length(cleanName) > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Name must be " + NAME_MAX_LENGTH + " characters or less");
        }
        if (species == null) {
            throw new IllegalArgumentException("Species is required");
        }
        if (ageMonths != null && (ageMonths < 0 || ageMonths > AGE_MAX_MONTHS)) {
            throw new IllegalArgumentException("Age must be between 0 and " + AGE_MAX_MONTHS + " months");
        }
        String cleanDescription = clean(description);
        if (cleanDescription != null && length(cleanDescription) > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Description must be " + DESCRIPTION_MAX_LENGTH + " characters or less");
        }
    }

    /** Says whether a user who is staff at the given shelters may edit or delete a post of that shelter (S7). */
    public static boolean canEdit(Set<Long> staffShelterIds, long postShelterId) {
        return staffShelterIds != null && staffShelterIds.contains(postShelterId);
    }

    /** Says whether a post with this status should show up in nearby search (S5, S7). */
    public static boolean showInSearch(PostStatus status) {
        return status == PostStatus.AVAILABLE;
    }

    /** Counts characters the way JSON Schema and Postgres do, so an emoji counts as one. */
    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
