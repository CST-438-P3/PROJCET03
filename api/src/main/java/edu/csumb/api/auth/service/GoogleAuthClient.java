package edu.csumb.api.auth.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Exchanges a Google authorization code and verifies the ID token that comes back. */
@Component
public class GoogleAuthClient {

    public record GoogleProfile(String subject, String email, String name) {
    }

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final RestClient http;
    private final JwtDecoder idTokenDecoder;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;

    public GoogleAuthClient(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret,
            @Value("${app.google.token-uri}") String tokenUri,
            @Value("${app.google.jwks-uri}") String jwksUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        this.http = RestClient.builder().requestFactory(requestFactory).build();
        // Kept out of the context: a second JwtDecoder bean would break the Resource Server setup.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(requestFactory))
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS, iss -> ISSUERS.contains(String.valueOf(iss))),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId))));
        this.idTokenDecoder = decoder;
    }

    public GoogleProfile fetchProfile(String code, String codeVerifier, String redirectUri) {
        Jwt idToken = verify(exchange(code, codeVerifier, redirectUri));
        String email = idToken.getClaimAsString("email");
        if (!Boolean.TRUE.equals(idToken.getClaimAsBoolean("email_verified")) || !StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your Google email is not verified");
        }
        return new GoogleProfile(idToken.getSubject(), email, idToken.getClaimAsString("name"));
    }

    private String exchange(String code, String codeVerifier, String redirectUri) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<?, ?> response;
        try {
            response = http.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException e) {
            // A wrong client secret also lands here, so leave a trace of it.
            log.warn("Google rejected the code exchange: status={}", e.getStatusCode());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired authorization code");
        } catch (RestClientException e) {
            log.warn("Google code exchange failed: {}", e.getClass().getSimpleName());
            throw unavailable();
        }
        if (response == null || !(response.get("id_token") instanceof String idToken)) {
            log.warn("Google code exchange returned no id_token");
            throw unavailable();
        }
        return idToken;
    }

    private Jwt verify(String idToken) {
        try {
            return idTokenDecoder.decode(idToken);
        } catch (BadJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
        } catch (JwtException e) {
            // Anything else means the keys could not be fetched.
            log.warn("Google ID token check failed: {}", e.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google sign-in is unavailable");
    }
}
