package edu.csumb.api.post.service;

import static edu.csumb.api.post.service.PostRules.AGE_MAX_MONTHS;
import static edu.csumb.api.post.service.PostRules.BREED_MAX_LENGTH;
import static edu.csumb.api.post.service.PostRules.DESCRIPTION_MAX_LENGTH;
import static edu.csumb.api.post.service.PostRules.NAME_MAX_LENGTH;
import static edu.csumb.api.post.service.PostRules.PHOTO_URL_MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.post.PostRequest;
import edu.csumb.api.post.PostStatus;
import edu.csumb.api.post.Sex;
import edu.csumb.api.post.Size;
import edu.csumb.api.post.Species;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PostRulesTest {

    private static final String CAT_EMOJI = "🐱";

    /** Runs checkFields with only these fields set. */
    private static PostRules.CheckedPost check(
            String name, Species species, Integer ageMonths, String description, String photoUrl) {
        return PostRules.checkFields(name, species, null, null, null, ageMonths, description, photoUrl);
    }

    /** Runs checkFields and returns the message of the error it throws. */
    private static String errorFor(String name, Species species, Integer ageMonths, String description) {
        return errorFor(name, species, ageMonths, description, null);
    }

    /** Runs checkFields with a photo URL and returns the message of the error it throws. */
    private static String errorFor(
            String name, Species species, Integer ageMonths, String description, String photoUrl) {
        BadRequestException e = assertThrows(BadRequestException.class,
            () -> check(name, species, ageMonths, description, photoUrl));
        return e.getMessage();
    }

    @Test
    void validPostPasses() {
        assertDoesNotThrow(() -> check(
            "Mochi", Species.CAT, 24, "Calm and friendly.", "https://example.com/mochi.jpg"));
    }

    @Test
    void ageAndDescriptionAreOptional() {
        assertDoesNotThrow(() -> check("Max", Species.DOG, null, null, null));
    }

    @Test
    void edgeValuesPass() {
        assertDoesNotThrow(() -> check(
            "a".repeat(NAME_MAX_LENGTH), Species.DOG, 0, "b".repeat(DESCRIPTION_MAX_LENGTH), null));
        assertDoesNotThrow(() -> check("Old Boy", Species.DOG, AGE_MAX_MONTHS, "", null));
    }

    @Test
    void emojiCountsAsOneCharacter() {
        assertDoesNotThrow(() -> check(CAT_EMOJI.repeat(NAME_MAX_LENGTH), Species.CAT, 12, null, null));
        assertEquals("Name must be 60 characters or less",
            errorFor(CAT_EMOJI.repeat(NAME_MAX_LENGTH + 1), Species.CAT, 12, null));
    }

    @Test
    void checkedNameIsTheTrimmedOneThatGetsSaved() {
        String name = "a".repeat(NAME_MAX_LENGTH);
        PostRules.CheckedPost post = check("  " + name + "  ", Species.CAT, 12, null, null);
        assertEquals(name, post.name());
        assertEquals(NAME_MAX_LENGTH, post.name().length());
    }

    @Test
    void checkFieldsReturnsCleanedValues() {
        PostRules.CheckedPost post = check(
            "  Mochi Bear  ", Species.CAT, 24, "  Calm.  ", "  https://example.com/mochi.jpg  ");
        assertEquals("Mochi Bear", post.name());
        assertEquals(Species.CAT, post.species());
        assertEquals(24, post.ageMonths());
        assertEquals("Calm.", post.description());
        assertEquals("https://example.com/mochi.jpg", post.photoUrl());
    }

    @Test
    void emptyOptionalFieldsBecomeNull() {
        PostRules.CheckedPost post = check("Max", Species.DOG, null, "   ", "  ");
        assertNull(post.description());
        assertNull(post.photoUrl());
        assertNull(check("Max", Species.DOG, null, null, null).description());
    }

    @Test
    void httpAndHttpsPhotoUrlsPass() {
        assertDoesNotThrow(() -> check("Mochi", Species.CAT, 12, null, "http://example.com/a.png"));
        assertDoesNotThrow(() -> check("Mochi", Species.CAT, 12, null, "HTTPS://example.com/a.png"));
    }

    @Test
    void badPhotoUrlIsRejected() {
        String message = "Photo URL must be an http or https link";
        assertEquals(message, errorFor("Mochi", Species.CAT, 12, null, "ftp://example.com/a.png"));
        assertEquals(message, errorFor("Mochi", Species.CAT, 12, null, "not a link"));
        assertEquals(message, errorFor("Mochi", Species.CAT, 12, null, "https://"));
        assertEquals(message, errorFor("Mochi", Species.CAT, 12, null, "mochi.jpg"));
    }

    @Test
    void longPhotoUrlIsRejected() {
        String url = "https://example.com/" + "a".repeat(PHOTO_URL_MAX_LENGTH);
        assertEquals("Photo URL must be 500 characters or less", errorFor("Mochi", Species.CAT, 12, null, url));
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

    @Test
    void everyProblemIsListedAtOnce() {
        assertEquals("Name is required; Age must be between 0 and 360 months; Photo URL must be an http or https link",
            errorFor("  ", Species.CAT, -1, null, "mochi.jpg"));
    }

    @Test
    void createNeedsAShelter() {
        BadRequestException e = assertThrows(BadRequestException.class, () -> PostRules.checkCreate(
            new PostRequest(null, "Mochi", Species.CAT, null, null, null, null, null, null)));
        assertEquals("Shelter is required", e.getMessage());
    }

    @Test
    void createListsAMissingShelterTogetherWithFieldProblems() {
        BadRequestException e = assertThrows(BadRequestException.class, () -> PostRules.checkCreate(
            new PostRequest(null, "", Species.CAT, null, null, null, -1, null, null)));
        assertEquals("Shelter is required; Name is required; Age must be between 0 and 360 months", e.getMessage());
    }

    @Test
    void createReturnsTheCleanedFields() {
        PostRules.CheckedPost post = PostRules.checkCreate(
            new PostRequest(7L, "  Max  ", Species.DOG, null, null, null, null, null, null));
        assertEquals("Max", post.name());
    }

    @Test
    void breedIsTrimmedAndChecked() {
        PostRules.CheckedPost post = PostRules.checkFields(
            "Max", Species.DOG, "  Beagle  ", null, null, null, null, null);
        assertEquals("Beagle", post.breed());
        BadRequestException e = assertThrows(BadRequestException.class, () -> PostRules.checkFields(
            "Max", Species.DOG, "b".repeat(BREED_MAX_LENGTH + 1), null, null, null, null, null));
        assertEquals("Breed must be 60 characters or less", e.getMessage());
    }

    @Test
    void sexIsUnknownWhenMissingAndSizeIsKept() {
        PostRules.CheckedPost missing = check("Max", Species.DOG, null, null, null);
        assertEquals(Sex.UNKNOWN, missing.sex());
        assertNull(missing.size());
        PostRules.CheckedPost given = PostRules.checkFields(
            "Max", Species.DOG, null, Sex.MALE, Size.LARGE, null, null, null);
        assertEquals(Sex.MALE, given.sex());
        assertEquals(Size.LARGE, given.size());
    }
}
