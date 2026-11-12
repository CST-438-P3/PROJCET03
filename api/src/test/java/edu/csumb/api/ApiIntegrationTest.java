package edu.csumb.api;

import io.restassured.RestAssured;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Base class for API integration tests: a real Postgres in Docker, the app on a random port,
 * REST Assured pointed at it, empty tables before each test, and helpers to add rows and make a
 * signed-in user's token. Extend it instead of repeating this setup in every test.
 * Every table is emptied, so a table added in a later migration needs no change here; zip_codes is
 * reference data (it may be loaded by a migration), so it is never emptied.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.jwt-secret=test-secret-at-least-32-bytes-long-xx")
public abstract class ApiIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private JwtEncoder jwtEncoder;

    /** Points REST Assured at this test's server and empties every table except the reference ones. */
    @BeforeEach
    void setUpApiTest() {
        RestAssured.port = port;
        List<String> tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                + "AND tablename NOT IN ('zip_codes', 'flyway_schema_history')",
            String.class);
        jdbc.execute("TRUNCATE " + String.join(", ", tables) + " RESTART IDENTITY");
    }

    /** Makes the Bearer token value the API accepts for this user, with roles such as "ADMIN". */
    protected String tokenFor(long userId, String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(String.valueOf(userId))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .claim("roles", List.of(roles))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Adds a ZIP code with its location, or does nothing if it is already there. */
    protected void addZip(String zip, double latitude, double longitude) {
        jdbc.update("INSERT INTO zip_codes (zip, latitude, longitude) VALUES (?, ?, ?) ON CONFLICT (zip) DO NOTHING",
            zip, latitude, longitude);
    }

    /** Adds a user who signs in with GitHub, using this email, and returns the new user id. */
    protected long addUser(String email) {
        long userId = jdbc.queryForObject(
            "INSERT INTO users (email, display_name) VALUES (?, ?) RETURNING id", Long.class, email, email);
        addIdentity(userId, "GITHUB", "github-" + email);
        return userId;
    }

    /** Adds a way for the user to sign in, like ("GOOGLE", the Google sub). */
    protected void addIdentity(long userId, String provider, String providerUserId) {
        jdbc.update("INSERT INTO identities (user_id, provider, provider_user_id) VALUES (?, ?, ?)",
            userId, provider, providerUserId);
    }

    /** Adds a shelter in a ZIP code that already exists and returns the new shelter id. */
    protected long addShelter(String name, String zip) {
        return jdbc.queryForObject(
            "INSERT INTO shelters (name, address, zip_code, phone) VALUES (?, '100 Main St', ?, '831-555-0100') "
                + "RETURNING id",
            Long.class, name, zip);
    }

    /** Makes the user staff for the shelter. */
    protected void addStaff(long userId, long shelterId) {
        jdbc.update("INSERT INTO shelter_staff (user_id, shelter_id) VALUES (?, ?)", userId, shelterId);
    }

    /** Saves the post to the user's favorites. */
    protected void addFavorite(long userId, long postId) {
        jdbc.update("INSERT INTO favorites (user_id, post_id) VALUES (?, ?)", userId, postId);
    }

    /** Adds an inquiry from the user about the post and returns the new inquiry id. */
    protected long addInquiry(long userId, long postId, String message) {
        return jdbc.queryForObject(
            "INSERT INTO inquiries (user_id, post_id, message) VALUES (?, ?, ?) RETURNING id",
            Long.class, userId, postId, message);
    }

    /** Adds an AVAILABLE post for the shelter and returns the new post id. */
    protected long addPost(long shelterId, String name, String species) {
        return jdbc.queryForObject(
            "INSERT INTO adoption_posts (shelter_id, name, species) VALUES (?, ?, ?) RETURNING id",
            Long.class, shelterId, name, species);
    }
}
