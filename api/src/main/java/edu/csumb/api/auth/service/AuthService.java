package edu.csumb.api.auth.service;

import edu.csumb.api.user.Provider;
import edu.csumb.api.user.User;
import edu.csumb.api.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Turns an identity already verified by a provider into a local user and an API JWT. */
@Service
public class AuthService {

    /** Longest display name, the same as maxLength in openapi.yaml. */
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;

    public record SignedIn(String token, User user) {
    }

    private final UserRepository users;
    private final JwtEncoder jwtEncoder;
    private final Duration jwtTtl;

    public AuthService(UserRepository users, JwtEncoder jwtEncoder, @Value("${app.jwt-ttl}") Duration jwtTtl) {
        this.users = users;
        this.jwtEncoder = jwtEncoder;
        this.jwtTtl = jwtTtl;
    }

    /**
     * Not transactional on purpose: a failed insert must not poison the transaction
     * that the lost-race lookup runs in.
     */
    public SignedIn signIn(Provider provider, String providerUserId, String email, String name) {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> create(provider, providerUserId, cleanEmail, name));
        return new SignedIn(issueToken(user), user);
    }

    private User create(Provider provider, String providerUserId, String email, String name) {
        users.findByEmail(email).ifPresent(existing -> {
            throw emailTaken(existing);
        });
        try {
            return users.saveAndFlush(new User(email, displayName(name, email), provider, providerUserId));
        } catch (DataIntegrityViolationException lostRace) {
            return users.findByProviderAndProviderUserId(provider, providerUserId)
                    .orElseThrow(() -> users.findByEmail(email)
                            .<RuntimeException>map(AuthService::emailTaken)
                            .orElse(lostRace));
        }
    }

    private static ResponseStatusException emailTaken(User existing) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "This email is registered with " + existing.getProvider().label());
    }

    private static String displayName(String name, String email) {
        String chosen = StringUtils.hasText(name) ? name.trim() : email.substring(0, email.indexOf('@'));
        return chosen.codePointCount(0, chosen.length()) <= DISPLAY_NAME_MAX_LENGTH
                ? chosen
                : chosen.substring(0, chosen.offsetByCodePoints(0, DISPLAY_NAME_MAX_LENGTH));
    }

    private String issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(jwtTtl))
                .claim("roles", List.of(user.getRole().name()))
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
