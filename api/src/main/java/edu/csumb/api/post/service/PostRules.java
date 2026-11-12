package edu.csumb.api.post.service;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.post.PostStatus;
import edu.csumb.api.post.Species;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/** Rules for adoption posts from PRD stories S5 and S7, kept apart from controllers so they are easy to test. */
public final class PostRules {

    /** Longest pet name, the same as maxLength in openapi.yaml. */
    public static final int NAME_MAX_LENGTH = 60;

    /** Oldest age in months, the same as maximum in openapi.yaml. */
    public static final int AGE_MAX_MONTHS = 360;

    /** Longest description, the same as maxLength in openapi.yaml. */
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    /** Longest photo URL, the same as maxLength in openapi.yaml. */
    public static final int PHOTO_URL_MAX_LENGTH = 500;

    /** The fields of a post after checkPost, cleaned and ready to save; save these values, not the raw ones. */
    public record CheckedPost(String name, Species species, Integer ageMonths, String description, String photoUrl) {
    }

    private PostRules() {
    }

    /** Checks the fields of a new or edited post and returns them cleaned, or throws BadRequestException. */
    public static CheckedPost checkPost(
            String name, Species species, Integer ageMonths, String description, String photoUrl) {
        String cleanName = clean(name);
        if (cleanName == null || cleanName.isEmpty()) {
            throw new BadRequestException("Name is required");
        }
        if (length(cleanName) > NAME_MAX_LENGTH) {
            throw new BadRequestException("Name must be " + NAME_MAX_LENGTH + " characters or less");
        }
        if (species == null) {
            throw new BadRequestException("Species is required");
        }
        if (ageMonths != null && (ageMonths < 0 || ageMonths > AGE_MAX_MONTHS)) {
            throw new BadRequestException("Age must be between 0 and " + AGE_MAX_MONTHS + " months");
        }
        String cleanDescription = emptyToNull(clean(description));
        if (cleanDescription != null && length(cleanDescription) > DESCRIPTION_MAX_LENGTH) {
            throw new BadRequestException(
                "Description must be " + DESCRIPTION_MAX_LENGTH + " characters or less");
        }
        String cleanPhotoUrl = emptyToNull(clean(photoUrl));
        if (cleanPhotoUrl != null) {
            if (length(cleanPhotoUrl) > PHOTO_URL_MAX_LENGTH) {
                throw new BadRequestException(
                    "Photo URL must be " + PHOTO_URL_MAX_LENGTH + " characters or less");
            }
            if (!isWebLink(cleanPhotoUrl)) {
                throw new BadRequestException("Photo URL must be an http or https link");
            }
        }
        return new CheckedPost(cleanName, species, ageMonths, cleanDescription, cleanPhotoUrl);
    }

    /** Says whether a user who is staff at the given shelters may edit or delete a post of that shelter (S7). */
    public static boolean canEdit(Set<Long> staffShelterIds, long postShelterId) {
        return staffShelterIds != null && staffShelterIds.contains(postShelterId);
    }

    /** Says whether a post with this status should show up in nearby search (S5, S7). */
    public static boolean showInSearch(PostStatus status) {
        return status == PostStatus.AVAILABLE;
    }

    /** Removes spaces at the start and end of a text field. */
    private static String clean(String text) {
        return text == null ? null : text.strip();
    }

    /** Turns an empty text into null, so an optional field left empty is saved as missing. */
    private static String emptyToNull(String text) {
        return text == null || text.isEmpty() ? null : text;
    }

    /** Says whether the text is a full http or https link with a host. */
    private static boolean isWebLink(String text) {
        try {
            URI uri = new URI(text);
            String scheme = uri.getScheme();
            boolean web = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return web && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** Counts characters the way JSON Schema and Postgres do, so an emoji counts as one. */
    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
