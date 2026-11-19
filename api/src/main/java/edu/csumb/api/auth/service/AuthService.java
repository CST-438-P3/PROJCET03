package edu.csumb.api.auth.service;

import edu.csumb.api.user.Identity;
import edu.csumb.api.user.IdentityRepository;
import edu.csumb.api.user.Provider;
import edu.csumb.api.user.User;
import edu.csumb.api.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
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
    private final IdentityRepository identities;
    private final TransactionOperations tx;
    private final JwtEncoder jwtEncoder;
    private final Duration jwtTtl;

    public AuthService(UserRepository users, IdentityRepository identities, TransactionOperations tx,
            JwtEncoder jwtEncoder, @Value("${app.jwt-ttl}") Duration jwtTtl) {
        this.users = users;
        this.identities = identities;
        this.tx = tx;
        this.jwtEncoder = jwtEncoder;
        this.jwtTtl = jwtTtl;
    }

    /**
     * Only the insert is transactional: a failed insert must not poison the transaction
     * that the lost-race lookup runs in.
     */
    public SignedIn signIn(Provider provider, String providerUserId, String email, String name) {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = findUser(provider, providerUserId)
                .orElseGet(() -> create(provider, providerUserId, cleanEmail, name));
        return new SignedIn(issueToken(user), user);
    }

    private Optional<User> findUser(Provider provider, String providerUserId) {
        return identities.findByProviderAndProviderUserId(provider, providerUserId).map(Identity::getUser);
    }

    private User create(Provider provider, String providerUserId, String email, String name) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw emailTaken(existing);
        });
        try {
            return tx.execute(status -> {
                User user = users.saveAndFlush(new User(email, displayName(name, email)));
                identities.saveAndFlush(new Identity(user, provider, providerUserId));
                return user;
            });
        } catch (DataIntegrityViolationException lostRace) {
            return findUser(provider, providerUserId)
                    .orElseThrow(() -> users.findByEmailIgnoreCase(email)
                            .<RuntimeException>map(this::emailTaken)
                            .orElse(lostRace));
        }
    }

    private ResponseStatusException emailTaken(User existing) {
        String where = identities.findFirstByUserOrderById(existing)
                .map(identity -> identity.getProvider().label())
                .orElse("another account");
        return new ResponseStatusException(HttpStatus.CONFLICT, "This email is registered with " + where);
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
