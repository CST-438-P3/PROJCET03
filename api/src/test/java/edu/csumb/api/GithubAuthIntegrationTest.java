package edu.csumb.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.csumb.api.user.Identity;
import edu.csumb.api.user.IdentityRepository;
import edu.csumb.api.user.Provider;
import edu.csumb.api.user.User;
import edu.csumb.api.user.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** POST /auth/github against a real database, with GitHub replaced by a local HTTP stub. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.jwt-secret=test-secret-at-least-32-bytes-long-xx",
            "app.github.client-id=test-client-id",
            "app.github.client-secret=test-client-secret"
        })
class GithubAuthIntegrationTest {

    private static final String GITHUB_ID = "4242";
    private static final String EMAIL = "adopter@example.com";
    private static final String TOKEN_OK = "{\"access_token\":\"gh-token\",\"token_type\":\"bearer\"}";
    private static final String USER_OK = "{\"id\":4242,\"login\":\"ada\",\"name\":\"Ada Adopter\"}";
    private static final String EMAILS_OK = """
            [{"email":"old@example.com","primary":false,"verified":true},
             {"email":"adopter@example.com","primary":true,"verified":true}]""";

    private static final HttpServer GITHUB = startGithub();

    private static volatile int tokenStatus;
    private static volatile String tokenBody;
    private static volatile int userStatus;
    private static volatile String userBody;
    private static volatile String emailsBody;
    private static volatile String lastTokenRequest;
    private static volatile String lastAuthorization;
    private static volatile String lastUserAgent;

    @DynamicPropertySource
    static void githubUris(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + GITHUB.getAddress().getPort();
        registry.add("app.github.token-uri", () -> base + "/token");
        registry.add("app.github.api-uri", () -> base);
    }

    @AfterAll
    static void stopGithub() {
        GITHUB.stop(0);
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    IdentityRepository identities;

    @Autowired
    JwtDecoder apiJwtDecoder;

    @BeforeEach
    void reset() {
        identities.deleteAll();
        users.deleteAll();
        tokenStatus = 200;
        tokenBody = TOKEN_OK;
        userStatus = 200;
        userBody = USER_OK;
        emailsBody = EMAILS_OK;
        lastTokenRequest = null;
        lastAuthorization = null;
        lastUserAgent = null;
    }

    @Test
    void verifiedAccountGetsJwtAndUserRow() {
        var body = signIn().statusCode(200)
                .body("token", notNullValue())
                .body("user.email", equalTo(EMAIL))
                .body("user.displayName", equalTo("Ada Adopter"))
                .body("user.role", equalTo("USER"))
                .extract().jsonPath();

        User saved = identities.findByProviderAndProviderUserId(Provider.GITHUB, GITHUB_ID).orElseThrow().getUser();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(body.getLong("user.id")).isEqualTo(saved.getId());

        Jwt token = apiJwtDecoder.decode(body.getString("token"));
        assertThat(token.getSubject()).isEqualTo(saved.getId().toString());
        assertThat(token.getClaimAsStringList("roles")).containsExactly("USER");

        assertThat(lastTokenRequest)
                .contains("code=auth-code", "code_verifier=verifier", "client_secret=test-client-secret");
        assertThat(lastAuthorization).isEqualTo("Bearer gh-token");
        assertThat(lastUserAgent).isNotBlank();
    }

    @Test
    void signingInAgainKeepsOneRow() {
        signIn().statusCode(200);
        signIn().statusCode(200);
        assertThat(users.count()).isEqualTo(1);
        assertThat(identities.count()).isEqualTo(1);
    }

    @Test
    void missingNameFallsBackToLogin() {
        userBody = "{\"id\":4242,\"login\":\"ada\",\"name\":null}";
        signIn().statusCode(200).body("user.displayName", equalTo("ada"));
    }

    @Test
    void emailRegisteredWithGoogleIsRejected() {
        identities.save(new Identity(users.save(new User(EMAIL, "Ada")), Provider.GOOGLE, "google-sub-1"));
        signIn().statusCode(409).body("detail", equalTo("This email is registered with Google"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void unverifiedPrimaryEmailIsRejected() {
        emailsBody = """
                [{"email":"old@example.com","primary":false,"verified":true},
                 {"email":"adopter@example.com","primary":true,"verified":false}]""";
        signIn().statusCode(403).body("detail", equalTo("Your GitHub email is not verified"));
        assertThat(users.count()).isZero();
    }

    @Test
    void badCodeReportedWith200IsUnauthorized() {
        tokenBody = "{\"error\":\"bad_verification_code\",\"error_description\":\"The code passed is incorrect or expired.\"}";
        assertRejected(401);
    }

    @Test
    void codeRejectedWith4xxIsUnauthorized() {
        tokenStatus = 400;
        tokenBody = "{\"error\":\"invalid_request\"}";
        assertRejected(401);
    }

    @Test
    void revokedTokenIsUnauthorized() {
        userStatus = 401;
        userBody = "{\"message\":\"Bad credentials\"}";
        assertRejected(401);
    }

    @Test
    void githubErrorIsBadGateway() {
        tokenStatus = 500;
        tokenBody = "{}";
        assertRejected(502);
    }

    @Test
    void responseWithoutAccessTokenIsBadGateway() {
        tokenBody = "{}";
        assertRejected(502);
    }

    @Test
    void blankCodeIsBadRequest() {
        given().port(port).contentType(ContentType.JSON)
                .body(Map.of("code", " ", "codeVerifier", "verifier", "redirectUri", "http://localhost:8081"))
                .post("/auth/github")
                .then().statusCode(400);
        assertThat(lastTokenRequest).isNull();
    }

    private void assertRejected(int status) {
        signIn().statusCode(status);
        assertThat(users.count()).isZero();
    }

    private ValidatableResponse signIn() {
        return given().port(port).contentType(ContentType.JSON)
                .body(Map.of("code", "auth-code", "codeVerifier", "verifier", "redirectUri", "http://localhost:8081"))
                .post("/auth/github")
                .then();
    }

    private static HttpServer startGithub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/token", exchange -> {
                lastTokenRequest = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                respond(exchange, tokenStatus, tokenBody);
            });
            // "/user" also matches "/user/emails" by prefix, so route on the exact path.
            server.createContext("/user", exchange -> {
                lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                lastUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
                if (exchange.getRequestURI().getPath().equals("/user/emails")) {
                    respond(exchange, 200, emailsBody);
                } else {
                    respond(exchange, userStatus, userBody);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
