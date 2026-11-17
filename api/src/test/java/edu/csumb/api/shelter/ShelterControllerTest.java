package edu.csumb.api.shelter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import edu.csumb.api.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShelterControllerTest extends ApiIntegrationTest {

    private String user;
    private long monterey;

    /** Adds three shelters, not in name order, and a signed-in user. */
    @BeforeEach
    void addShelters() {
        addZip("93933", 36.6536, -121.7968);
        addShelter("Salinas Animal Services", "93933");
        monterey = addShelter("Monterey Shelter", "93933");
        addShelter("Carmel Pet Rescue", "93933");
        user = tokenFor(addUser("adopter@example.com"));
    }

    @Test
    void listsSheltersSortedByName() {
        given().auth().oauth2(user)
            .when().get("/shelters")
            .then().statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(20))
            .body("totalElements", equalTo(3))
            .body("totalPages", equalTo(1))
            .body("content.name", contains("Carmel Pet Rescue", "Monterey Shelter", "Salinas Animal Services"))
            .body("content[1].zip", equalTo("93933"))
            .body("content[1].phone", equalTo("831-555-0100"));
    }

    @Test
    void secondPageHasTheRest() {
        given().auth().oauth2(user)
            .when().get("/shelters?page=1&size=2")
            .then().statusCode(200)
            .body("totalPages", equalTo(2))
            .body("content.name", contains("Salinas Animal Services"));
    }

    @Test
    void badPageAndSizeAre400() {
        given().auth().oauth2(user)
            .when().get("/shelters?page=-1&size=51")
            .then().statusCode(400)
            .body("detail", equalTo("Page must be 0 or more; Size must be between 1 and 50"));
    }

    @Test
    void getsOneShelter() {
        given().auth().oauth2(user)
            .when().get("/shelters/" + monterey)
            .then().statusCode(200)
            .body("id", equalTo((int) monterey))
            .body("name", equalTo("Monterey Shelter"))
            .body("address", equalTo("100 Main St"));
    }

    @Test
    void missingShelterIs404() {
        given().auth().oauth2(user)
            .when().get("/shelters/999")
            .then().statusCode(404)
            .body("detail", equalTo("Shelter not found"));
    }

    @Test
    void noTokenIs401() {
        given().when().get("/shelters").then().statusCode(401);
        given().when().get("/shelters/" + monterey).then().statusCode(401);
    }
}
