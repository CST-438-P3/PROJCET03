package edu.csumb.api.auth;

import edu.csumb.api.auth.service.AuthService;
import edu.csumb.api.auth.service.GoogleAuthClient;
import edu.csumb.api.user.Provider;
import edu.csumb.api.user.Role;
import edu.csumb.api.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record CodeExchange(@NotBlank String code, @NotBlank String codeVerifier, @NotBlank String redirectUri) {
    }

    public record UserResponse(Long id, String email, String displayName, Role role) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
        }
    }

    public record SessionResponse(String token, UserResponse user) {
    }

    private final GoogleAuthClient google;
    private final AuthService authService;

    public AuthController(GoogleAuthClient google, AuthService authService) {
        this.google = google;
        this.authService = authService;
    }

    @PostMapping("/google")
    public SessionResponse google(@Valid @RequestBody CodeExchange body) {
        var profile = google.fetchProfile(body.code(), body.codeVerifier(), body.redirectUri());
        var signedIn = authService.signIn(Provider.GOOGLE, profile.subject(), profile.email(), profile.name());
        return new SessionResponse(signedIn.token(), UserResponse.from(signedIn.user()));
    }
}
