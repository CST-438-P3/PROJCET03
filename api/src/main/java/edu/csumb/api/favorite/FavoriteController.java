package edu.csumb.api.favorite;

import edu.csumb.api.favorite.service.FavoriteService;
import edu.csumb.api.post.PostPage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /favorites, PUT /favorites/{postId} and DELETE /favorites/{postId} from openapi.yaml. */
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    // Creates the controller with the service that does the work.
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // Returns one page of the signed-in user's favorite posts
    @GetMapping
    public PostPage list(@AuthenticationPrincipal Jwt jwt,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return favoriteService.list(userId(jwt), page, size);
    }

    // Saves a post to the signed-in user's favorites.
    @PutMapping("/{postId}")
    public ResponseEntity<Void> save(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId) {
        favoriteService.save(userId(jwt), postId);
        return ResponseEntity.noContent().build();
    }

    // Removes a post from the signed-in user's favorites.
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId) {
        favoriteService.remove(userId(jwt), postId);
        return ResponseEntity.noContent().build();
    }

    // The signed-in user's id, which the API puts in the token's subject.
    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}