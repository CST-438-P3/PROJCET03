package edu.csumb.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

/** Checks that ApiIntegrationTest's setup works: REST Assured reaches the app and tokenFor makes a valid token. */
class ApiIntegrationTestSetupTest extends ApiIntegrationTest {

    @Test
    void protectedPathWithoutTokenIs401() {
        given().when().get("/users/me").then().statusCode(401);
    }

    @Test
    void tokenFromTheHelperIsAccepted() {
        long userId = addUser("token-helper@example.com");
        given().header("Authorization", "Bearer " + tokenFor(userId))
            .when().get("/users/me")
            .then().statusCode(not(401));
    }
}
