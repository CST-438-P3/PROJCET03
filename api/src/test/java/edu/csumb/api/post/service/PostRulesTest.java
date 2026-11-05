package edu.csumb.api.post.service;

import static edu.csumb.api.post.service.PostRules.AGE_MAX_MONTHS;
import static edu.csumb.api.post.service.PostRules.DESCRIPTION_MAX_LENGTH;
import static edu.csumb.api.post.service.PostRules.NAME_MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.csumb.api.post.PostStatus;
import edu.csumb.api.post.Species;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PostRulesTest {

    private static final String CAT_EMOJI = "🐱";

    /** Runs checkPost and returns the message of the error it throws. */
    private static String errorFor(String name, Species species, Integer ageMonths, String description) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> PostRules.checkPost(name, species, ageMonths, description));
        return e.getMessage();
    }

    @Test
    void validPostPasses() {
        assertDoesNotThrow(() -> PostRules.checkPost("Mochi", Species.CAT, 24, "Calm and friendly."));
    }

    @Test
    void ageAndDescriptionAreOptional() {
        assertDoesNotThrow(() -> PostRules.checkPost("Max", Species.DOG, null, null));
    }

    @Test
    void edgeValuesPass() {
        assertDoesNotThrow(() -> PostRules.checkPost(
            "a".repeat(NAME_MAX_LENGTH), Species.DOG, 0, "b".repeat(DESCRIPTION_MAX_LENGTH)));
        assertDoesNotThrow(() -> PostRules.checkPost("Old Boy", Species.DOG, AGE_MAX_MONTHS, ""));
    }

    @Test
    void emojiCountsAsOneCharacter() {
        assertDoesNotThrow(() -> PostRules.checkPost(CAT_EMOJI.repeat(NAME_MAX_LENGTH), Species.CAT, 12, null));
        assertEquals("Name must be 60 characters or less",
            errorFor(CAT_EMOJI.repeat(NAME_MAX_LENGTH + 1), Species.CAT, 12, null));
    }

    @Test
    void spacesAroundTheNameAreNotCounted() {
        assertDoesNotThrow(() -> PostRules.checkPost(
            "  " + "a".repeat(NAME_MAX_LENGTH) + "  ", Species.CAT, 12, null));
    }

    @Test
    void cleanRemovesSpacesAtTheEnds() {
        assertEquals("Mochi", PostRules.clean("  Mochi  "));
        assertEquals("Mochi Bear", PostRules.clean("Mochi Bear"));
        assertNull(PostRules.clean(null));
    }

    @Test
    void missingOrBlankNameIsRejected() {
        assertEquals("Name is required", errorFor(null, Species.CAT, 12, null));
        assertEquals("Name is required", errorFor("", Species.CAT, 12, null));
        assertEquals("Name is required", errorFor("   ", Species.CAT, 12, null));
    }

    @Test
    void longNameIsRejected() {
        assertEquals("Name must be 60 characters or less",
            errorFor("a".repeat(NAME_MAX_LENGTH + 1), Species.CAT, 12, null));
    }

    @Test
    void missingSpeciesIsRejected() {
        assertEquals("Species is required", errorFor("Mochi", null, 12, null));
    }

    @Test
    void ageOutOfRangeIsRejected() {
        assertEquals("Age must be between 0 and 360 months", errorFor("Mochi", Species.CAT, -1, null));
        assertEquals("Age must be between 0 and 360 months",
            errorFor("Mochi", Species.CAT, AGE_MAX_MONTHS + 1, null));
    }

    @Test
    void longDescriptionIsRejected() {
        assertEquals("Description must be 2000 characters or less",
            errorFor("Mochi", Species.CAT, 12, "b".repeat(DESCRIPTION_MAX_LENGTH + 1)));
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
        assertTrue(PostRules.showInSearch(PostStatus.AVAILABLE));
        assertFalse(PostRules.showInSearch(PostStatus.ADOPTED));
        assertFalse(PostRules.showInSearch(null));
    }
}
