package edu.csumb.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.csumb.api.user.Provider;
import edu.csumb.api.user.User;
import edu.csumb.api.user.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

/** POST /auth/google against a real database, with Google replaced by a local HTTP stub. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.jwt-secret=test-secret-at-least-32-bytes-long-xx",
            "app.google.client-id=" + GoogleAuthIntegrationTest.CLIENT_ID,
            "app.google.client-secret=test-client-secret"
        })
class GoogleAuthIntegrationTest {

    static final String CLIENT_ID = "test-client-id";
    private static final String KEY_ID = "test-key";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String SUBJECT = "google-sub-1";
    private static final String EMAIL = "adopter@example.com";

    private static final RSAKey GOOGLE_KEY = rsaKey(KEY_ID);
    private static final RSAKey ATTACKER_KEY = rsaKey(KEY_ID);
    private static final HttpServer GOOGLE = startGoogle();

    private static volatile int tokenStatus;
    private static volatile String tokenBody;
    private static volatile int jwksStatus;
    private static volatile String lastTokenRequest;

    @DynamicPropertySource
    static void googleUris(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + GOOGLE.getAddress().getPort();
        registry.add("app.google.token-uri", () -> base + "/token");
        registry.add("app.google.jwks-uri", () -> base + "/jwks");
    }

    @AfterAll
    static void stopGoogle() {
        GOOGLE.stop(0);
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    JwtDecoder apiJwtDecoder;

    @BeforeEach
    void reset() {
        users.deleteAll();
        tokenStatus = 200;
        jwksStatus = 200;
        lastTokenRequest = null;
        googleReturns(idToken(GOOGLE_KEY, claims -> { }));
    }

    @Test
    void verifiedAccountGetsJwtAndUserRow() {
        var body = signIn().statusCode(200)
                .body("token", notNullValue())
                .body("user.email", equalTo(EMAIL))
                .body("user.displayName", equalTo("Ada Adopter"))
                .body("user.role", equalTo("USER"))
                .extract().jsonPath();

        User saved = users.findByEmail(EMAIL).orElseThrow();
        assertThat(saved.getProvider()).isEqualTo(Provider.GOOGLE);
        assertThat(saved.getProviderUserId()).isEqualTo(SUBJECT);
        assertThat(body.getLong("user.id")).isEqualTo(saved.getId());

        Jwt token = apiJwtDecoder.decode(body.getString("token"));
        assertThat(token.getSubject()).isEqualTo(saved.getId().toString());
        assertThat(token.getClaimAsStringList("roles")).containsExactly("USER");

        assertThat(lastTokenRequest)
                .contains("code_verifier=verifier", "client_secret=test-client-secret", "grant_type=authorization_code");
    }

    @Test
    void signingInAgainKeepsOneRow() {
        signIn().statusCode(200);
        signIn().statusCode(200);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void shortIssuerFormIsAccepted() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.issuer("accounts.google.com")));
        signIn().statusCode(200);
    }

    @Test
    void missingNameFallsBackToEmailName() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.claim("name", null)));
        signIn().statusCode(200).body("user.displayName", equalTo("adopter"));
    }

    @Test
    void emailRegisteredWithGitHubIsRejected() {
        users.save(new User(EMAIL, "Ada", Provider.GITHUB, "gh-1"));
        signIn().statusCode(409).body("detail", equalTo("This email is registered with GitHub"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void emailMatchIgnoresCase() {
        users.save(new User(EMAIL, "Ada", Provider.GITHUB, "gh-1"));
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.claim("email", "Adopter@Example.COM")));
        signIn().statusCode(409);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void wrongAudienceIsRejected() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.audience("someone-else")));
        assertRejected(401);
    }

    @Test
    void wrongIssuerIsRejected() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.issuer("https://evil.example")));
        assertRejected(401);
    }

    @Test
    void expiredTokenIsRejected() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(3600)))));
        assertRejected(401);
    }

    @Test
    void badSignatureIsRejected() {
        googleReturns(idToken(ATTACKER_KEY, claims -> { }));
        assertRejected(401);
    }

    @Test
    void unverifiedEmailIsRejected() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.claim("email_verified", false)));
        signIn().statusCode(403).body("detail", equalTo("Your Google email is not verified"));
        assertThat(users.count()).isZero();
    }

    @Test
    void missingEmailVerifiedClaimIsRejected() {
        googleReturns(idToken(GOOGLE_KEY, claims -> claims.claim("email_verified", null)));
        assertRejected(403);
    }

    @Test
    void codeRejectedByGoogleIsUnauthorized() {
        tokenStatus = 400;
        tokenBody = "{\"error\":\"invalid_grant\"}";
        assertRejected(401);
    }

    @Test
    void googleErrorIsBadGateway() {
        tokenStatus = 500;
        tokenBody = "{}";
        assertRejected(502);
    }

    @Test
    void responseWithoutIdTokenIsBadGateway() {
        tokenBody = "{}";
        assertRejected(502);
    }

    @Test
    void unreachableKeysAreBadGateway() {
        // An unknown key ID forces a fetch even when the keys are already cached.
        jwksStatus = 500;
        googleReturns(idToken(rsaKey("rotated-key"), claims -> { }));
        assertRejected(502);
    }

    @Test
    void blankCodeIsBadRequest() {
        given().port(port).contentType(ContentType.JSON)
                .body(Map.of("code", " ", "codeVerifier", "verifier", "redirectUri", "http://localhost:8081"))
                .post("/auth/google")
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
                .post("/auth/google")
                .then();
    }

    private static void googleReturns(String idToken) {
        tokenBody = "{\"id_token\":\"" + idToken + "\"}";
    }

    private static String idToken(RSAKey signer, Consumer<JWTClaimsSet.Builder> change) {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CLIENT_ID))
                .subject(SUBJECT)
                .claim("email", EMAIL)
                .claim("email_verified", true)
                .claim("name", "Ada Adopter")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
        change.accept(claims);
        try {
            var jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.getKeyID()).build(), claims.build());
            jwt.sign(new RSASSASigner(signer));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey rsaKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpServer startGoogle() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/token", exchange -> {
                lastTokenRequest = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                respond(exchange, tokenStatus, tokenBody);
            });
            server.createContext("/jwks", exchange ->
                    respond(exchange, jwksStatus, new JWKSet(GOOGLE_KEY.toPublicJWK()).toString()));
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
