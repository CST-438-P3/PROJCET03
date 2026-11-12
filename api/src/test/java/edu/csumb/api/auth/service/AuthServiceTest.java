package edu.csumb.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.csumb.api.user.Identity;
import edu.csumb.api.user.IdentityRepository;
import edu.csumb.api.user.Provider;
import edu.csumb.api.user.User;
import edu.csumb.api.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.server.ResponseStatusException;

/** The double-tap race on a first sign-in, which an integration test can't hit reliably. */
class AuthServiceTest {

    private static final String EMAIL = "a@example.com";

    private final UserRepository users = mock(UserRepository.class);
    private final IdentityRepository identities = mock(IdentityRepository.class);
    private final JwtEncoder encoder = mock(JwtEncoder.class);
    private final AuthService service = new AuthService(
            users, identities, TransactionOperations.withoutTransaction(), encoder, Duration.ofMinutes(15));

    @BeforeEach
    void loseTheInsertRace() {
        when(identities.findByProviderAndProviderUserId(Provider.GOOGLE, "sub")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("jwt")
                .header("alg", "HS256")
                .subject("1")
                .issuedAt(Instant.now())
                .build());
    }

    @Test
    void sameAccountWinningTheRaceSignsIn() {
        User winner = new User(EMAIL, "A");
        when(identities.findByProviderAndProviderUserId(Provider.GOOGLE, "sub"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Identity(winner, Provider.GOOGLE, "sub")));

        assertThat(service.signIn(Provider.GOOGLE, "sub", EMAIL, "A").user()).isSameAs(winner);
    }

    @Test
    void otherProviderWinningTheRaceConflicts() {
        User other = new User(EMAIL, "A");
        when(users.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(other));
        when(identities.findFirstByUserOrderById(other))
                .thenReturn(Optional.of(new Identity(other, Provider.GITHUB, "gh")));

        assertThatThrownBy(() -> service.signIn(Provider.GOOGLE, "sub", EMAIL, "A"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> {
                            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                            assertThat(e.getReason()).isEqualTo("This email is registered with GitHub");
                        });
    }
}
