package edu.csumb.api.shelter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.csumb.api.ApiIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminShelterControllerTest extends ApiIntegrationTest {

    /** A valid shelter body with spaces around the name. */
    private static final String SHELTER = """
        {"name": "  Monterey Shelter  ", "address": "1 Ocean Ave", "zip": "93940", "email": "hi@monterey.org"}
        """;

    private String admin;
    private long shelterA;
    private long user;

    /** Adds two ZIP codes, Shelter A, a plain user, and an admin token. */
    @BeforeEach
    void addShelterAndUsers() {
        addZip("93933", 36.6536, -121.7968);
        addZip("93940", 36.6002, -121.8947);
        shelterA = addShelter("Shelter A", "93933");
        user = addUser("staff@example.com");
        admin = tokenFor(addUser("admin@example.com"), "ADMIN");
    }

    /** A request with a JSON body. */
    private static RequestSpecification json(String body) {
        return given().contentType(ContentType.JSON).body(body);
    }

    /** Counts rows of a table. */
    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    /** Says whether the user is staff for the shelter. */
    private boolean isStaff(long userId, long shelterId) {
        return jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM shelter_staff WHERE user_id = ? AND shelter_id = ?)",
            Boolean.class, userId, shelterId);
    }

    /** Tries to create a post for the shelter as this user and returns the status code. */
    private int postAs(long userId, long shelterId) {
        return json("{\"shelterId\": %d, \"name\": \"Mochi\", \"species\": \"CAT\"}".formatted(shelterId))
            .auth().oauth2(tokenFor(userId))
            .when().post("/posts")
            .then().extract().statusCode();
    }

    @Test
    void adminCreatesAShelter() {
        int id = json(SHELTER).auth().oauth2(admin)
            .when().post("/admin/shelters")
            .then().statusCode(201)
            .body("name", equalTo("Monterey Shelter"))
            .body("address", equalTo("1 Ocean Ave"))
            .body("zip", equalTo("93940"))
            .body("email", equalTo("hi@monterey.org"))
            .body("$", not(hasKey("phone")))
            .extract().path("id");
        assertEquals("Monterey Shelter",
            jdbc.queryForObject("SELECT name FROM shelters WHERE id = ?", String.class, id));
    }

    @Test
    void badShelterIs400WithEveryProblemListed() {
        json("{\"name\": \"  \", \"address\": \"1 Ocean Ave\", \"zip\": \"9394\"}").auth().oauth2(admin)
            .when().post("/admin/shelters")
            .then().statusCode(400)
            .contentType("application/problem+json")
            .body("detail", equalTo("Name is required; ZIP code must be 5 digits; Phone or email is required"));
        assertEquals(1, count("shelters"));
    }

    @Test
    void unknownZipIs400() {
        json(SHELTER.replace("93940", "10001")).auth().oauth2(admin)
            .when().post("/admin/shelters")
            .then().statusCode(400)
            .body("detail", equalTo("We don't know ZIP code 10001"));
        assertEquals(1, count("shelters"));
    }

    @Test
    void adminUpdatesAShelter() {
        json(SHELTER).auth().oauth2(admin)
            .when().put("/admin/shelters/" + shelterA)
            .then().statusCode(200)
            .body("id", equalTo((int) shelterA))
            .body("name", equalTo("Monterey Shelter"))
            .body("$", not(hasKey("phone")));

        assertEquals("Monterey Shelter",
            jdbc.queryForObject("SELECT name FROM shelters WHERE id = ?", String.class, shelterA));
    }

    @Test
    void badUpdateChangesNothing() {
        json("{\"name\": \"New name\", \"address\": \"1 Ocean Ave\", \"zip\": \"93940\"}").auth().oauth2(admin)
            .when().put("/admin/shelters/" + shelterA)
            .then().statusCode(400)
            .body("detail", equalTo("Phone or email is required"));

        assertEquals("Shelter A", jdbc.queryForObject("SELECT name FROM shelters WHERE id = ?", String.class, shelterA));
    }

    @Test
    void updateOfMissingShelterIs404() {
        json(SHELTER).auth().oauth2(admin)
            .when().put("/admin/shelters/999")
            .then().statusCode(404)
            .body("detail", equalTo("Shelter not found"));
    }

    @Test
    void adminDeletesAShelterWithEverythingUnderIt() {
        addStaff(user, shelterA);
        long post = addPost(shelterA, "Mochi", "CAT");
        addFavorite(user, post);
        addInquiry(user, post, "Is she good with cats?");

        given().auth().oauth2(admin)
            .when().delete("/admin/shelters/" + shelterA)
            .then().statusCode(204);

        assertEquals(0, count("shelters"));
        assertEquals(0, count("shelter_staff"));
        assertEquals(0, count("adoption_posts"));
        assertEquals(0, count("favorites"));
        assertEquals(0, count("inquiries"));
    }

    @Test
    void deleteOfMissingShelterIs404() {
        given().auth().oauth2(admin)
            .when().delete("/admin/shelters/999")
            .then().statusCode(404);
    }

    @Test
    void grantedUserCanPostAndRevokedUserCannot() {
        assertEquals(403, postAs(user, shelterA));

        given().auth().oauth2(admin)
            .when().put("/admin/shelters/%d/staff/%d".formatted(shelterA, user))
            .then().statusCode(204);
        assertEquals(201, postAs(user, shelterA));

        given().auth().oauth2(admin)
            .when().delete("/admin/shelters/%d/staff/%d".formatted(shelterA, user))
            .then().statusCode(204);
        assertEquals(403, postAs(user, shelterA));
    }

    @Test
    void grantingTwiceAndRevokingANonStaffUserAreFine() {
        String staffPath = "/admin/shelters/%d/staff/%d".formatted(shelterA, user);
        given().auth().oauth2(admin).when().put(staffPath).then().statusCode(204);
        given().auth().oauth2(admin).when().put(staffPath).then().statusCode(204);
        assertTrue(isStaff(user, shelterA));

        given().auth().oauth2(admin).when().delete(staffPath).then().statusCode(204);
        given().auth().oauth2(admin).when().delete(staffPath).then().statusCode(204);
        assertFalse(isStaff(user, shelterA));
    }

    @Test
    void staffChangesForAMissingShelterOrUserAre404() {
        given().auth().oauth2(admin)
            .when().put("/admin/shelters/999/staff/" + user)
            .then().statusCode(404)
            .body("detail", equalTo("Shelter not found"));
        given().auth().oauth2(admin)
            .when().put("/admin/shelters/%d/staff/999".formatted(shelterA))
            .then().statusCode(404)
            .body("detail", equalTo("User not found"));
        given().auth().oauth2(admin)
            .when().delete("/admin/shelters/%d/staff/999".formatted(shelterA))
            .then().statusCode(404);
    }

    @Test
    void userWithoutAdminRoleGets403OnEveryAdminRoute() {
        String notAdmin = tokenFor(user);
        String staffPath = "/admin/shelters/%d/staff/%d".formatted(shelterA, user);

        json(SHELTER).auth().oauth2(notAdmin).when().post("/admin/shelters").then().statusCode(403);
        json(SHELTER).auth().oauth2(notAdmin).when().put("/admin/shelters/" + shelterA).then().statusCode(403);
        given().auth().oauth2(notAdmin).when().delete("/admin/shelters/" + shelterA).then().statusCode(403);
        given().auth().oauth2(notAdmin).when().put(staffPath).then().statusCode(403);
        given().auth().oauth2(notAdmin).when().delete(staffPath).then().statusCode(403);

        assertEquals(1, count("shelters"));
        assertFalse(isStaff(user, shelterA));
    }

    @Test
    void noTokenGets401OnEveryAdminRoute() {
        String staffPath = "/admin/shelters/%d/staff/%d".formatted(shelterA, user);

        json(SHELTER).when().post("/admin/shelters").then().statusCode(401);
        json(SHELTER).when().put("/admin/shelters/" + shelterA).then().statusCode(401);
        given().when().delete("/admin/shelters/" + shelterA).then().statusCode(401);
        given().when().put(staffPath).then().statusCode(401);
        given().when().delete(staffPath).then().statusCode(401);

        assertEquals(1, count("shelters"));
    }
}
