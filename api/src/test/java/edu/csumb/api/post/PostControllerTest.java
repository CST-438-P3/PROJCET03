package edu.csumb.api.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import edu.csumb.api.ApiIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostControllerTest extends ApiIntegrationTest {

    private long shelterA;
    private long shelterB;
    private String staffOfA;

    /** Adds Shelter A and Shelter B in 93933, and a user who is staff for Shelter A only. */
    @BeforeEach
    void addSheltersAndStaff() {
        addZip("93933", 36.6536, -121.7968);
        shelterA = addShelter("Shelter A", "93933");
        shelterB = addShelter("Shelter B", "93933");
        long staff = addUser("staff@shelter-a.org");
        addStaff(staff, shelterA);
        staffOfA = tokenFor(staff);
    }

    /** A valid create body for this shelter. */
    private static String body(long shelterId) {
        return """
            {"shelterId": %d, "name": "  Mochi  ", "species": "CAT", "breed": "Ragdoll",
             "ageMonths": 24, "description": "Calm and friendly.", "photoUrl": "https://example.com/mochi.jpg"}
            """.formatted(shelterId);
    }

    /** Counts rows in adoption_posts. */
    private int postCount() {
        return jdbc.queryForObject("SELECT count(*) FROM adoption_posts", Integer.class);
    }

    @Test
    void staffCreatesAPostForTheirShelterAndAnyoneSignedInCanOpenIt() {
        var created = given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body(body(shelterA))
            .when().post("/posts")
            .then().statusCode(201)
            .body("name", equalTo("Mochi"))
            .body("shelter.id", equalTo((int) shelterA))
            .body("shelter.name", equalTo("Shelter A"))
            .body("species", equalTo("CAT"))
            .body("breed", equalTo("Ragdoll"))
            .body("sex", equalTo("UNKNOWN"))
            .body("status", equalTo("AVAILABLE"))
            .body("createdAt", notNullValue())
            .body("$", not(hasKey("size")))
            .extract();
        int id = created.path("id");
        assertEquals("/posts/" + id, created.header("Location"));

        String adopter = tokenFor(addUser("adopter@example.com"));
        given().auth().oauth2(adopter)
            .when().get("/posts/" + id)
            .then().statusCode(200)
            .body("name", equalTo("Mochi"))
            .body("ageMonths", equalTo(24))
            .body("createdAt", equalTo(created.path("createdAt")));
    }

    @Test
    void staffCannotPostForAnotherShelter() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body(body(shelterB))
            .when().post("/posts")
            .then().statusCode(403)
            .contentType("application/problem+json")
            .body("detail", equalTo("You can't post for this shelter"));
        assertEquals(0, postCount());
    }

    @Test
    void userWhoIsNotStaffCannotPost() {
        String adopter = tokenFor(addUser("adopter@example.com"));
        given().auth().oauth2(adopter).contentType(ContentType.JSON).body(body(shelterA))
            .when().post("/posts")
            .then().statusCode(403);
        assertEquals(0, postCount());
    }

    @Test
    void userWhoIsNotStaffGets403EvenWithBadFields() {
        String adopter = tokenFor(addUser("adopter@example.com"));
        given().auth().oauth2(adopter).contentType(ContentType.JSON)
            .body("{\"shelterId\": %d, \"name\": \"   \", \"species\": \"CAT\"}".formatted(shelterA))
            .when().post("/posts")
            .then().statusCode(403);
    }

    @Test
    void missingShelterIs400() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON)
            .body("{\"name\": \"Mochi\", \"species\": \"CAT\"}")
            .when().post("/posts")
            .then().statusCode(400)
            .body("detail", equalTo("Shelter is required"));
    }

    @Test
    void badFieldsAre400WithEveryProblemListed() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON)
            .body("{\"shelterId\": %d, \"name\": \"   \", \"species\": \"CAT\", \"ageMonths\": 400}".formatted(shelterA))
            .when().post("/posts")
            .then().statusCode(400)
            .contentType("application/problem+json")
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Name is required; Age must be between 0 and 360 months"));
        assertEquals(0, postCount());
    }

    @Test
    void unknownSpeciesIs400() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON)
            .body("{\"shelterId\": %d, \"name\": \"Tweety\", \"species\": \"BIRD\"}".formatted(shelterA))
            .when().post("/posts")
            .then().statusCode(400);
        assertEquals(0, postCount());
    }

    @Test
    void createWithoutTokenIs401() {
        given().contentType(ContentType.JSON).body(body(shelterA))
            .when().post("/posts")
            .then().statusCode(401);
        assertEquals(0, postCount());
    }

    @Test
    void getWithoutTokenIs401() {
        given().when().get("/posts/1").then().statusCode(401);
    }

    @Test
    void missingPostIs404() {
        given().auth().oauth2(staffOfA)
            .when().get("/posts/999")
            .then().statusCode(404)
            .contentType("application/problem+json")
            .body("detail", equalTo("This post is no longer available"));
    }

    /** A valid edit body with this status. */
    private static String editBody(String status) {
        return """
            {"name": "  Mochi Bear  ", "species": "CAT", "breed": "  ", "sex": "FEMALE", "size": "SMALL",
             "ageMonths": 30, "description": "Still calm.", "status": "%s"}
            """.formatted(status);
    }

    /** Reads one column of a post straight from the database. */
    private String column(long postId, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM adoption_posts WHERE id = ?", String.class, postId);
    }

    /** Counts rows of a table that belong to this post. */
    private int rowsFor(String table, long postId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE post_id = ?", Integer.class, postId);
    }

    @Test
    void staffEditsAPostOfTheirShelter() {
        long post = addPost(shelterA, "Mochi", "CAT");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body(editBody("AVAILABLE"))
            .when().put("/posts/" + post)
            .then().statusCode(200)
            .body("id", equalTo((int) post))
            .body("shelter.id", equalTo((int) shelterA))
            .body("name", equalTo("Mochi Bear"))
            .body("sex", equalTo("FEMALE"))
            .body("size", equalTo("SMALL"))
            .body("ageMonths", equalTo(30))
            .body("$", not(hasKey("breed")))
            .body("$", not(hasKey("photoUrl")));

        assertEquals("Mochi Bear", column(post, "name"));
        assertNull(column(post, "breed"));
        assertEquals("Still calm.", column(post, "description"));
    }

    @Test
    void staffCannotEditAPostOfAnotherShelter() {
        long post = addPost(shelterB, "Rex", "DOG");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body(editBody("ADOPTED"))
            .when().put("/posts/" + post)
            .then().statusCode(403)
            .contentType("application/problem+json")
            .body("detail", equalTo("You can't edit this post"));

        assertEquals("Rex", column(post, "name"));
        assertEquals("AVAILABLE", column(post, "status"));
    }

    @Test
    void userWhoIsNotStaffGets403OnEditEvenWithBadFields() {
        long post = addPost(shelterA, "Mochi", "CAT");
        String adopter = tokenFor(addUser("adopter@example.com"));

        given().auth().oauth2(adopter).contentType(ContentType.JSON).body("{\"name\": \"  \"}")
            .when().put("/posts/" + post)
            .then().statusCode(403);
    }

    @Test
    void badEditIs400WithEveryProblemListedAndChangesNothing() {
        long post = addPost(shelterA, "Mochi", "CAT");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON)
            .body("{\"name\": \"   \", \"species\": \"CAT\", \"ageMonths\": -1}")
            .when().put("/posts/" + post)
            .then().statusCode(400)
            .body("detail", equalTo("Status is required; Name is required; Age must be between 0 and 360 months"));

        assertEquals("Mochi", column(post, "name"));
    }

    @Test
    void editOfMissingPostIs404() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body(editBody("AVAILABLE"))
            .when().put("/posts/999")
            .then().statusCode(404)
            .body("detail", equalTo("This post is no longer available"));
    }

    @Test
    void editWithoutTokenIs401() {
        long post = addPost(shelterA, "Mochi", "CAT");
        given().contentType(ContentType.JSON).body(editBody("AVAILABLE"))
            .when().put("/posts/" + post)
            .then().statusCode(401);
        assertEquals("Mochi", column(post, "name"));
    }

    @Test
    void staffMarksAPostAdoptedAndOtherFieldsStay() {
        long post = addPost(shelterA, "Mochi", "CAT");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body("{\"status\": \"ADOPTED\"}")
            .when().patch("/posts/" + post)
            .then().statusCode(200)
            .body("status", equalTo("ADOPTED"))
            .body("name", equalTo("Mochi"));

        assertEquals("ADOPTED", column(post, "status"));
    }

    @Test
    void staffCannotMarkAPostOfAnotherShelterAdopted() {
        long post = addPost(shelterB, "Rex", "DOG");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body("{\"status\": \"ADOPTED\"}")
            .when().patch("/posts/" + post)
            .then().statusCode(403)
            .body("detail", equalTo("You can't edit this post"));

        assertEquals("AVAILABLE", column(post, "status"));
    }

    @Test
    void missingStatusIs400() {
        long post = addPost(shelterA, "Mochi", "CAT");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body("{}")
            .when().patch("/posts/" + post)
            .then().statusCode(400)
            .body("detail", equalTo("Status is required"));
    }

    @Test
    void unknownStatusIs400() {
        long post = addPost(shelterA, "Mochi", "CAT");

        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body("{\"status\": \"SOLD\"}")
            .when().patch("/posts/" + post)
            .then().statusCode(400);

        assertEquals("AVAILABLE", column(post, "status"));
    }

    @Test
    void statusChangeOfMissingPostIs404() {
        given().auth().oauth2(staffOfA).contentType(ContentType.JSON).body("{\"status\": \"ADOPTED\"}")
            .when().patch("/posts/999")
            .then().statusCode(404);
    }

    @Test
    void statusChangeWithoutTokenIs401() {
        long post = addPost(shelterA, "Mochi", "CAT");
        given().contentType(ContentType.JSON).body("{\"status\": \"ADOPTED\"}")
            .when().patch("/posts/" + post)
            .then().statusCode(401);
        assertEquals("AVAILABLE", column(post, "status"));
    }

    @Test
    void staffDeletesAPostAndItsFavoritesAndInquiries() {
        long post = addPost(shelterA, "Mochi", "CAT");
        long adopter = addUser("adopter@example.com");
        addFavorite(adopter, post);
        addInquiry(adopter, post, "Is she good with cats?");

        given().auth().oauth2(staffOfA)
            .when().delete("/posts/" + post)
            .then().statusCode(204);

        assertEquals(0, postCount());
        assertEquals(0, rowsFor("favorites", post));
        assertEquals(0, rowsFor("inquiries", post));
        given().auth().oauth2(staffOfA)
            .when().get("/posts/" + post)
            .then().statusCode(404);
    }

    @Test
    void staffCannotDeleteAPostOfAnotherShelter() {
        long post = addPost(shelterB, "Rex", "DOG");

        given().auth().oauth2(staffOfA)
            .when().delete("/posts/" + post)
            .then().statusCode(403)
            .body("detail", equalTo("You can't delete this post"));

        assertEquals(1, postCount());
    }

    @Test
    void deleteOfMissingPostIs404() {
        given().auth().oauth2(staffOfA)
            .when().delete("/posts/999")
            .then().statusCode(404);
    }

    @Test
    void deleteWithoutTokenIs401() {
        long post = addPost(shelterA, "Mochi", "CAT");
        given().when().delete("/posts/" + post).then().statusCode(401);
        assertEquals(1, postCount());
    }
}
