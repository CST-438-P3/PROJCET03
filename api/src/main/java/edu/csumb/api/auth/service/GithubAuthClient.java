package edu.csumb.api.auth.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Exchanges a GitHub authorization code and reads the user's id and primary verified email. */
@Component
public class GithubAuthClient {

    public record GithubProfile(String id, String email, String name) {
    }

    private record GithubUser(Long id, String login, String name) {
    }

    private record GithubEmail(String email, boolean primary, boolean verified) {
    }

    private static final Logger log = LoggerFactory.getLogger(GithubAuthClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient http;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private final String apiUri;

    public GithubAuthClient(
            @Value("${app.github.client-id}") String clientId,
            @Value("${app.github.client-secret}") String clientSecret,
            @Value("${app.github.token-uri}") String tokenUri,
            @Value("${app.github.api-uri}") String apiUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.apiUri = apiUri;
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT);
        // GitHub's API refuses requests without a User-Agent.
        this.http = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "api")
                .build();
    }

    public GithubProfile fetchProfile(String code, String codeVerifier, String redirectUri) {
        String accessToken = exchange(code, codeVerifier, redirectUri);
        GithubUser user = call(() -> get(accessToken, "/user").body(GithubUser.class));
        List<GithubEmail> emails = call(() -> get(accessToken, "/user/emails")
                .body(new ParameterizedTypeReference<List<GithubEmail>>() { }));
        if (user == null || user.id() == null || emails == null) {
            log.warn("GitHub returned an empty profile");
            throw unavailable();
        }
        String email = emails.stream()
                .filter(e -> e.primary() && e.verified() && StringUtils.hasText(e.email()))
                .map(GithubEmail::email)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Your GitHub email is not verified"));
        String name = StringUtils.hasText(user.name()) ? user.name() : user.login();
        return new GithubProfile(String.valueOf(user.id()), email, name);
    }

    private String exchange(String code, String codeVerifier, String redirectUri) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        Map<?, ?> response = call(() -> http.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(Map.class));
        // GitHub answers a bad or expired code with 200 and an "error" field.
        if (response != null && response.get("error") != null) {
            log.warn("GitHub rejected the code exchange: error={}", response.get("error"));
            throw invalidCode();
        }
        if (response == null || !(response.get("access_token") instanceof String token) || token.isBlank()) {
            log.warn("GitHub code exchange returned no access_token");
            throw unavailable();
        }
        return token;
    }

    private RestClient.ResponseSpec get(String accessToken, String path) {
        return http.get()
                .uri(apiUri + path)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.parseMediaType("application/vnd.github+json"))
                .retrieve();
    }

    /** 4xx means GitHub rejected our code or token (401); anything else means GitHub is unavailable (502). */
    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException e) {
            log.warn("GitHub rejected the request: status={}", e.getStatusCode());
            throw invalidCode();
        } catch (RestClientException e) {
            log.warn("GitHub request failed: {}", e.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired authorization code");
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub sign-in is unavailable");
    }
}
