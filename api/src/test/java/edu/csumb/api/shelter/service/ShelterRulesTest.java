package edu.csumb.api.shelter.service;

import static edu.csumb.api.shelter.service.ShelterRules.ADDRESS_MAX_LENGTH;
import static edu.csumb.api.shelter.service.ShelterRules.EMAIL_MAX_LENGTH;
import static edu.csumb.api.shelter.service.ShelterRules.NAME_MAX_LENGTH;
import static edu.csumb.api.shelter.service.ShelterRules.PHONE_MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.shelter.ShelterRequest;
import org.junit.jupiter.api.Test;

class ShelterRulesTest {

    /** Runs the check and returns the message of the error it throws. */
    private static String errorFor(String name, String address, String zip, String phone, String email) {
        BadRequestException e = assertThrows(BadRequestException.class,
            () -> ShelterRules.check(new ShelterRequest(name, address, zip, phone, email)));
        return e.getMessage();
    }

    @Test
    void validShelterIsCleaned() {
        ShelterRules.CheckedShelter checked = ShelterRules.check(
            new ShelterRequest("  Monterey Shelter ", " 1 Ocean Ave ", " 93940 ", " ", " hi@monterey.org "));
        assertEquals("Monterey Shelter", checked.name());
        assertEquals("1 Ocean Ave", checked.address());
        assertEquals("93940", checked.zip());
        assertNull(checked.phone());
        assertEquals("hi@monterey.org", checked.email());
    }

    @Test
    void phoneAloneIsEnough() {
        assertDoesNotThrow(() -> ShelterRules.check(
            new ShelterRequest("Shelter", "1 Ocean Ave", "93940", "831-555-0100", null)));
    }

    @Test
    void everythingMissingListsEveryProblem() {
        assertEquals("Name is required; Address is required; ZIP code is required; Phone or email is required",
            errorFor(null, "  ", null, "", null));
    }

    @Test
    void zipMustBeFiveDigits() {
        assertEquals("ZIP code must be 5 digits", errorFor("Shelter", "1 Ocean Ave", "9394a", "831", null));
        assertEquals("ZIP code must be 5 digits", errorFor("Shelter", "1 Ocean Ave", "939400", "831", null));
    }

    @Test
    void emailMustLookLikeAnEmail() {
        assertEquals("Email must look like name@example.com",
            errorFor("Shelter", "1 Ocean Ave", "93940", null, "monterey.org"));
        assertEquals("Email must look like name@example.com",
            errorFor("Shelter", "1 Ocean Ave", "93940", null, "hi@monterey"));
    }

    @Test
    void longestValuesPassAndOneMoreFails() {
        String name = "n".repeat(NAME_MAX_LENGTH);
        String address = "a".repeat(ADDRESS_MAX_LENGTH);
        String phone = "1".repeat(PHONE_MAX_LENGTH);
        String email = "e".repeat(EMAIL_MAX_LENGTH - "@x.org".length()) + "@x.org";
        assertDoesNotThrow(() -> ShelterRules.check(new ShelterRequest(name, address, "93940", phone, email)));

        assertEquals("Name must be 100 characters or less; Address must be 200 characters or less; "
                + "Phone must be 30 characters or less; Email must be 320 characters or less",
            errorFor(name + "n", address + "a", "93940", phone + "1", "e" + email));
    }
}
