package edu.csumb.api.post.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PostRulesTest {

    /** Runs checkPost and returns the message of the error it throws. */
    private static String errorFor(String name, String species, Integer ageMonths, String description) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> PostRules.checkPost(name, species, ageMonths, description));
        return e.getMessage();
    }

    @Test
    void validPostPasses() {
        assertDoesNotThrow(() -> PostRules.checkPost("Mochi", "CAT", 24, "Calm and friendly."));
    }

    @Test
    void ageAndDescriptionAreOptional() {
        assertDoesNotThrow(() -> PostRules.checkPost("Max", "DOG", null, null));
    }

    @Test
    void edgeValuesPass() {
        assertDoesNotThrow(() -> PostRules.checkPost("a".repeat(60), "DOG", 0, "b".repeat(2000)));
        assertDoesNotThrow(() -> PostRules.checkPost("Old Boy", "DOG", 360, ""));
    }

    @Test
    void missingOrBlankNameIsRejected() {
        assertEquals("Name is required", errorFor(null, "CAT", 12, null));
        assertEquals("Name is required", errorFor("   ", "CAT", 12, null));
    }

    @Test
    void longNameIsRejected() {
        assertEquals("Name must be 60 characters or less", errorFor("a".repeat(61), "CAT", 12, null));
    }

    @Test
    void speciesOtherThanCatOrDogIsRejected() {
        assertEquals("Species must be CAT or DOG", errorFor("Tweety", "BIRD", 12, null));
        assertEquals("Species must be CAT or DOG", errorFor("Mochi", null, 12, null));
        assertEquals("Species must be CAT or DOG", errorFor("Mochi", "cat", 12, null));
    }

    @Test
    void ageOutOfRangeIsRejected() {
        assertEquals("Age must be between 0 and 360 months", errorFor("Mochi", "CAT", -1, null));
        assertEquals("Age must be between 0 and 360 months", errorFor("Mochi", "CAT", 361, null));
    }

    @Test
    void longDescriptionIsRejected() {
        assertEquals("Description must be 2000 characters or less",
            errorFor("Mochi", "CAT", 12, "b".repeat(2001)));
    }

    @Test
    void staffCanEditPostsOfTheirShelter() {
        assertTrue(PostRules.canEdit(Set.of(1L, 2L), 2L));
    }

    @Test
    void staffCannotEditPostsOfAnotherShelter() {
        assertFalse(PostRules.canEdit(Set.of(1L), 2L));
    }

    @Test
    void userWithNoShelterCannotEdit() {
        assertFalse(PostRules.canEdit(Set.of(), 1L));
        assertFalse(PostRules.canEdit(null, 1L));
    }

    @Test
    void onlyAvailablePostsShowInSearch() {
        assertTrue(PostRules.showInSearch("AVAILABLE"));
        assertFalse(PostRules.showInSearch("ADOPTED"));
        assertFalse(PostRules.showInSearch(null));
    }
}
