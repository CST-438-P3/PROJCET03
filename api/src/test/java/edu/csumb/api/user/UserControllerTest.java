package edu.csumb.api.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import edu.csumb.api.ApiIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class UserControllerTest extends ApiIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void meReturnsOnlyTheTokenOwner() {
        addUser("other@example.com");
        long me = addUser("me@example.com");
        given().auth().oauth2(tokenFor(me))
            .when().get("/users/me")
            .then().statusCode(200)
            .body("id", equalTo((int) me))
            .body("email", equalTo("me@example.com"))
            .body("displayName", equalTo("me@example.com"))
            .body("role", equalTo("USER"));
    }

    @Test
    void noTokenIs401() {
        given().when().get("/users/me").then().statusCode(401);
    }

    @Test
    void expiredTokenIs401() {
        long me = addUser("me@example.com");
        // Past the decoder's 60 s clock skew.
        Instant past = Instant.now().minusSeconds(600);
        given().auth().oauth2(sign(JwtClaimsSet.builder().subject(String.valueOf(me))
                .issuedAt(past).expiresAt(past.plusSeconds(300)).build()))
            .when().get("/users/me")
            .then().statusCode(401);
    }

    @Test
    void tamperedTokenIs401OnAnyProtectedPath() {
        long me = addUser("me@example.com");
        String[] mine = tokenFor(me).split("\\.");
        String[] admin = tokenFor(me, "ADMIN").split("\\.");
        // My signature on a payload that claims ADMIN.
        String tampered = mine[0] + "." + admin[1] + "." + mine[2];
        given().auth().oauth2(tampered).when().get("/users/me").then().statusCode(401);
        given().auth().oauth2(tampered).when().get("/admin/shelters/1/staff").then().statusCode(401);
        given().auth().oauth2("not-a-jwt").when().get("/shelters").then().statusCode(401);
    }

    @Test
    void tokenForDeletedUserIs401() {
        given().auth().oauth2(tokenFor(999L)).when().get("/users/me").then().statusCode(401);
    }

    @Test
    void healthAndAuthNeedNoToken() {
        given().when().get("/actuator/health").then().statusCode(200);
        // Reaches the controller (400 for the empty body), not the 401 from the filter.
        given().contentType("application/json").body("{}")
            .when().post("/auth/github").then().statusCode(400);
    }

    private String sign(JwtClaimsSet claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
