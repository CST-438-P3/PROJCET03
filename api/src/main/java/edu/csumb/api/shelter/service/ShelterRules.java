package edu.csumb.api.shelter.service;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.shelter.ShelterRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Rules for shelters from PRD story S10, kept apart from controllers so they are easy to test. */
public final class ShelterRules {

    /** Longest shelter name, the same as maxLength in openapi.yaml. */
    public static final int NAME_MAX_LENGTH = 100;

    /** Longest address, the same as maxLength in openapi.yaml. */
    public static final int ADDRESS_MAX_LENGTH = 200;

    /** Longest phone number, the same as maxLength in openapi.yaml. */
    public static final int PHONE_MAX_LENGTH = 30;

    /** Longest email, the same as the shelters.email column. */
    public static final int EMAIL_MAX_LENGTH = 320;

    private static final Pattern ZIP = Pattern.compile("[0-9]{5}");

    /** Something, one @, then a domain with a dot. Enough to catch typos; the shelter's inbox is the real test. */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** The fields of a shelter after a check, cleaned and ready to save; save these values, not the raw ones. */
    public record CheckedShelter(String name, String address, String zip, String phone, String email) {
    }

    private ShelterRules() {
    }

    /**
     * Checks a shelter for create and update. Returns the fields cleaned, or throws BadRequestException listing
     * every problem at once, separated by "; ". Whether the ZIP code is known needs the database, so
     * ShelterService checks that after this.
     */
    public static CheckedShelter check(ShelterRequest request) {
        String name = clean(request.name());
        String address = clean(request.address());
        String zip = clean(request.zip());
        String phone = clean(request.phone());
        String email = clean(request.email());
        List<String> errors = new ArrayList<>();

        if (name == null) {
            errors.add("Name is required");
        } else if (length(name) > NAME_MAX_LENGTH) {
            errors.add("Name must be " + NAME_MAX_LENGTH + " characters or less");
        }
        if (address == null) {
            errors.add("Address is required");
        } else if (length(address) > ADDRESS_MAX_LENGTH) {
            errors.add("Address must be " + ADDRESS_MAX_LENGTH + " characters or less");
        }
        if (zip == null) {
            errors.add("ZIP code is required");
        } else if (!ZIP.matcher(zip).matches()) {
            errors.add("ZIP code must be 5 digits");
        }
        if (phone == null && email == null) {
            errors.add("Phone or email is required");
        }
        if (phone != null && length(phone) > PHONE_MAX_LENGTH) {
            errors.add("Phone must be " + PHONE_MAX_LENGTH + " characters or less");
        }
        if (email != null && length(email) > EMAIL_MAX_LENGTH) {
            errors.add("Email must be " + EMAIL_MAX_LENGTH + " characters or less");
        } else if (email != null && !EMAIL.matcher(email).matches()) {
            errors.add("Email must look like name@example.com");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
        return new CheckedShelter(name, address, zip, phone, email);
    }

    /** Removes spaces at the start and end, and turns an empty text into null. */
    private static String clean(String text) {
        if (text == null) {
            return null;
        }
        String stripped = text.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Counts characters the way JSON Schema and Postgres do, so an emoji counts as one. */
    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
