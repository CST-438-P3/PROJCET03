package edu.csumb.api.favorite;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.csumb.api.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Tests for GET /favorites, PUT /favorites/{postId} and DELETE /favorites/{postId} (S8).
class FavoriteControllerTest extends ApiIntegrationTest {

    private long adopterId;
    private long otherAdopterId;
    private String adopter;
    private String otherAdopter;
    private long luna;
    private long max;

    // Adds a shelter with two pets, and two adopters who can each sign in.
    @BeforeEach
    void addPetsAndAdopters() {
        addZip("93933", 36.6536, -121.7968);
        long shelterId = addShelter("Monterey Shelter", "93933");
        luna = addPost(shelterId, "Luna", "CAT");
        max = addPost(shelterId, "Max", "DOG");

        adopterId = addUser("adopter@example.com");
        otherAdopterId = addUser("other@example.com");
        adopter = tokenFor(adopterId);
        otherAdopter = tokenFor(otherAdopterId);
    }

    // How many favorites rows this user has in the database.
    private int favoriteCount(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM favorites WHERE user_id = ?", Integer.class, userId);
    }

    @Test
    void savedPostShowsInTheListAndOnThePost() {
        given().auth().oauth2(adopter)
                .when().put("/favorites/" + luna)
                .then().statusCode(204);

        given().auth().oauth2(adopter)
                .when().get("/favorites")
                .then().statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(1))
                .body("totalPages", equalTo(1))
                .body("content.name", contains("Luna"))
                .body("content[0].favorite", equalTo(true))
                .body("content[0].shelter.name", equalTo("Monterey Shelter"));

        // The post itself now says it's a favorite, so the heart shows filled in
        given().auth().oauth2(adopter)
                .when().get("/posts/" + luna)
                .then().statusCode(200)
                .body("favorite", equalTo(true));

        // A post they didn't save says false
        given().auth().oauth2(adopter)
                .when().get("/posts/" + max)
                .then().statusCode(200)
                .body("favorite", equalTo(false));
    }

    @Test
    void savingTwiceKeepsOneFavorite() {
        given().auth().oauth2(adopter).when().put("/favorites/" + luna).then().statusCode(204);
        given().auth().oauth2(adopter).when().put("/favorites/" + luna).then().statusCode(204);

        assertEquals(1, favoriteCount(adopterId));
    }

    @Test
    void removingTakesItOutOfTheList() {
        addFavorite(adopterId, luna);

        given().auth().oauth2(adopter)
                .when().delete("/favorites/" + luna)
                .then().statusCode(204);

        given().auth().oauth2(adopter)
                .when().get("/favorites")
                .then().statusCode(200)
                .body("totalElements", equalTo(0))
                .body("content", empty());

        given().auth().oauth2(adopter)
                .when().get("/posts/" + luna)
                .then().statusCode(200)
                .body("favorite", equalTo(false));
    }

    @Test
    void removingAPostThatIsNotSavedIsFine() {
        given().auth().oauth2(adopter)
                .when().delete("/favorites/" + luna)
                .then().statusCode(204);
    }

    @Test
    void newestSaveComesFirstAndPagesWork() {
        addFavorite(adopterId, luna);
        addFavorite(adopterId, max);

        given().auth().oauth2(adopter)
                .when().get("/favorites?page=0&size=1")
                .then().statusCode(200)
                .body("totalElements", equalTo(2))
                .body("totalPages", equalTo(2))
                .body("content.name", contains("Max"));

        given().auth().oauth2(adopter)
                .when().get("/favorites?page=1&size=1")
                .then().statusCode(200)
                .body("content.name", contains("Luna"));
    }

    // Ownership test: each user only ever sees and changes their own favorites.
    @Test
    void usersOnlySeeAndChangeTheirOwnFavorites() {
        addFavorite(adopterId, luna);
        addFavorite(otherAdopterId, max);

        given().auth().oauth2(adopter)
                .when().get("/favorites")
                .then().statusCode(200)
                .body("content.name", contains("Luna"));

        given().auth().oauth2(otherAdopter)
                .when().get("/favorites")
                .then().statusCode(200)
                .body("content.name", contains("Max"));

        // The other user "removing" Luna only touches their own favorites, which don't have Luna
        given().auth().oauth2(otherAdopter)
                .when().delete("/favorites/" + luna)
                .then().statusCode(204);
        assertEquals(1, favoriteCount(adopterId));

        // And Luna still shows as saved for the adopter, but not for the other user
        given().auth().oauth2(otherAdopter)
                .when().get("/posts/" + luna)
                .then().statusCode(200)
                .body("favorite", equalTo(false));
    }

    @Test
    void missingPostIs404() {
        given().auth().oauth2(adopter)
                .when().put("/favorites/999")
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("detail", equalTo("This post is no longer available"));

        given().auth().oauth2(adopter)
                .when().delete("/favorites/999")
                .then().statusCode(404);
    }

    @Test
    void badPageAndSizeAre400() {
        given().auth().oauth2(adopter)
                .when().get("/favorites?page=-1&size=51")
                .then().statusCode(400)
                .body("detail", equalTo("Page must be 0 or more; Size must be between 1 and 50"));
    }

    @Test
    void noTokenIs401() {
        given().when().get("/favorites").then().statusCode(401);
        given().when().put("/favorites/" + luna).then().statusCode(401);
        given().when().delete("/favorites/" + luna).then().statusCode(401);

        assertEquals(0, favoriteCount(adopterId));
    }
}