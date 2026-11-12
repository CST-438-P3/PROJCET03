package edu.csumb.api.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.csumb.api.ApiIntegrationTest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DatabaseSchemaTest extends ApiIntegrationTest {

    /** Adds the 93933 ZIP code and a shelter there, and returns the shelter id. */
    private long addMontereyShelter() {
        addZip("93933", 36.6536, -121.7968);
        return addShelter("Monterey Shelter", "93933");
    }

    /** Counts rows in a table. */
    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    /** Reads when the post was last changed. */
    private OffsetDateTime updatedAt(long postId) {
        return jdbc.queryForObject("SELECT updated_at FROM adoption_posts WHERE id = ?", OffsetDateTime.class, postId);
    }

    @Test
    void newPostIsAvailableWithUnknownSexByDefault() {
        long post = addPost(addMontereyShelter(), "Mochi", "CAT");
        assertEquals("AVAILABLE",
            jdbc.queryForObject("SELECT status FROM adoption_posts WHERE id = ?", String.class, post));
        assertEquals("UNKNOWN",
            jdbc.queryForObject("SELECT sex FROM adoption_posts WHERE id = ?", String.class, post));
    }

    @Test
    void postRejectsUnknownSpeciesAndBlankName() {
        long shelter = addMontereyShelter();
        assertThrows(DataIntegrityViolationException.class, () -> addPost(shelter, "Foxy", "FOX"));
        assertThrows(DataIntegrityViolationException.class, () -> addPost(shelter, "   ", "CAT"));
    }

    @Test
    void postKeepsBreedSexAndSizeAndRejectsUnknownValues() {
        long post = addPost(addMontereyShelter(), "Max", "DOG");
        jdbc.update("UPDATE adoption_posts SET breed = 'Beagle', sex = 'MALE', size = 'MEDIUM' WHERE id = ?", post);
        assertEquals("Beagle MALE MEDIUM", jdbc.queryForObject(
            "SELECT breed || ' ' || sex || ' ' || size FROM adoption_posts WHERE id = ?", String.class, post));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE adoption_posts SET sex = 'BOY' WHERE id = ?", post));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE adoption_posts SET size = 'HUGE' WHERE id = ?", post));
    }

    @Test
    void shelterNeedsAKnownZipCode() {
        assertThrows(DataIntegrityViolationException.class, () -> addShelter("Nowhere Shelter", "00000"));
    }

    @Test
    void shelterNeedsAPhoneOrAnEmail() {
        addZip("93933", 36.6536, -121.7968);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO shelters (name, address, zip_code) VALUES ('Quiet Shelter', '1 Main St', '93933')"));
        jdbc.update("INSERT INTO shelters (name, address, zip_code, email) "
            + "VALUES ('Email Shelter', '1 Main St', '93933', 'hi@shelter.org')");
        assertEquals(1, count("shelters"));
    }

    @Test
    void emailIsUniqueIgnoringCase() {
        addUser("mochi@example.com");
        assertThrows(DataIntegrityViolationException.class, () -> addUser("Mochi@Example.com"));
    }

    @Test
    void userWithoutADisplayNameCanSignUp() {
        jdbc.update("INSERT INTO users (email) VALUES ('noname@example.com')");
        assertEquals(1, count("users"));
    }

    @Test
    void oneProviderAccountBelongsToOnlyOneUser() {
        long first = addUser("a@example.com");
        long second = addUser("b@example.com");
        addIdentity(first, "GOOGLE", "google-sub-1");
        assertThrows(DataIntegrityViolationException.class, () -> addIdentity(second, "GOOGLE", "google-sub-1"));
    }

    @Test
    void oneUserCanHaveBothProvidersForLinkingLater() {
        long user = addUser("a@example.com");
        addIdentity(user, "GOOGLE", "google-sub-1");
        assertEquals(2, jdbc.queryForObject(
            "SELECT count(*) FROM identities WHERE user_id = ?", Integer.class, user));
    }

    @Test
    void userCanSaveAPostOnlyOnce() {
        long post = addPost(addMontereyShelter(), "Mochi", "CAT");
        long user = addUser("a@example.com");
        addFavorite(user, post);
        assertThrows(DataIntegrityViolationException.class, () -> addFavorite(user, post));
    }

    @Test
    void editingAPostUpdatesUpdatedAt() {
        long post = addPost(addMontereyShelter(), "Mochi", "CAT");
        OffsetDateTime before = updatedAt(post);

        jdbc.update("UPDATE adoption_posts SET status = 'ADOPTED' WHERE id = ?", post);

        assertTrue(updatedAt(post).isAfter(before));
    }

    @Test
    void deletingAShelterDeletesItsPostsAndWhatPointsAtThem() {
        long shelter = addMontereyShelter();
        long post = addPost(shelter, "Mochi", "CAT");
        long user = addUser("a@example.com");
        addStaff(user, shelter);
        addFavorite(user, post);
        addInquiry(user, post, "Is she good with kids?");

        jdbc.update("DELETE FROM shelters WHERE id = ?", shelter);

        assertEquals(0, count("adoption_posts"));
        assertEquals(0, count("favorites"));
        assertEquals(0, count("inquiries"));
        assertEquals(0, count("shelter_staff"));
        assertEquals(1, count("users"));
    }

    @Test
    void deletingAUserDeletesTheirDataButNotPosts() {
        long shelter = addMontereyShelter();
        long post = addPost(shelter, "Mochi", "CAT");
        long user = addUser("a@example.com");
        addStaff(user, shelter);
        addFavorite(user, post);
        addInquiry(user, post, "Hi");

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertEquals(0, count("identities"));
        assertEquals(0, count("favorites"));
        assertEquals(0, count("inquiries"));
        assertEquals(0, count("shelter_staff"));
        assertEquals(1, count("adoption_posts"));
    }
}
