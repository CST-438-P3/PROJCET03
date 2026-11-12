package edu.csumb.api.post.service;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.post.PostRequest;
import edu.csumb.api.post.PostStatus;
import edu.csumb.api.post.Sex;
import edu.csumb.api.post.Size;
import edu.csumb.api.post.Species;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rules for adoption posts from PRD stories S5 and S7, kept apart from controllers so they are easy to test. */
public final class PostRules {

    /** Longest pet name, the same as maxLength in openapi.yaml. */
    public static final int NAME_MAX_LENGTH = 60;

    /** Longest breed, the same as maxLength in openapi.yaml. */
    public static final int BREED_MAX_LENGTH = 60;

    /** Oldest age in months, the same as maximum in openapi.yaml. */
    public static final int AGE_MAX_MONTHS = 360;

    /** Longest description, the same as maxLength in openapi.yaml. */
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    /** Longest photo URL, the same as maxLength in openapi.yaml. */
    public static final int PHOTO_URL_MAX_LENGTH = 500;

    /** The fields of a post after a check, cleaned and ready to save; save these values, not the raw ones. */
    public record CheckedPost(String name, Species species, String breed, Sex sex, Size size,
            Integer ageMonths, String description, String photoUrl) {
    }

    private PostRules() {
    }

    /**
     * Checks a new post (POST /posts): the shelter it is for, then every post field. Returns the fields cleaned,
     * or throws BadRequestException listing every problem at once, separated by "; ".
     */
    public static CheckedPost checkCreate(PostRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.shelterId() == null) {
            errors.add("Shelter is required");
        }
        return checkFields(errors, request.name(), request.species(), request.breed(), request.sex(),
            request.size(), request.ageMonths(), request.description(), request.photoUrl());
    }

    /**
     * Checks the fields every post has, for create and for edit (which has no shelter). Returns them cleaned,
     * or throws BadRequestException listing every problem at once, separated by "; ".
     */
    public static CheckedPost checkFields(String name, Species species, String breed, Sex sex, Size size,
            Integer ageMonths, String description, String photoUrl) {
        return checkFields(new ArrayList<>(), name, species, breed, sex, size, ageMonths, description, photoUrl);
    }

    /** Adds every field problem to the errors already found, then throws if there are any or returns the fields. */
    private static CheckedPost checkFields(List<String> errors, String rawName, Species species, String rawBreed,
            Sex sex, Size size, Integer ageMonths, String rawDescription, String rawPhotoUrl) {
        String name = clean(rawName);
        String breed = emptyToNull(clean(rawBreed));
        String description = emptyToNull(clean(rawDescription));
        String photoUrl = emptyToNull(clean(rawPhotoUrl));

        if (name == null || name.isEmpty()) {
            errors.add("Name is required");
        } else if (length(name) > NAME_MAX_LENGTH) {
            errors.add("Name must be " + NAME_MAX_LENGTH + " characters or less");
        }
        if (species == null) {
            errors.add("Species is required");
        }
        if (breed != null && length(breed) > BREED_MAX_LENGTH) {
            errors.add("Breed must be " + BREED_MAX_LENGTH + " characters or less");
        }
        if (ageMonths != null && (ageMonths < 0 || ageMonths > AGE_MAX_MONTHS)) {
            errors.add("Age must be between 0 and " + AGE_MAX_MONTHS + " months");
        }
        if (description != null && length(description) > DESCRIPTION_MAX_LENGTH) {
            errors.add("Description must be " + DESCRIPTION_MAX_LENGTH + " characters or less");
        }
        if (photoUrl != null && length(photoUrl) > PHOTO_URL_MAX_LENGTH) {
            errors.add("Photo URL must be " + PHOTO_URL_MAX_LENGTH + " characters or less");
        } else if (photoUrl != null && !isWebLink(photoUrl)) {
            errors.add("Photo URL must be an http or https link");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
        return new CheckedPost(name, species, breed, sex == null ? Sex.UNKNOWN : sex, size,
            ageMonths, description, photoUrl);
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
