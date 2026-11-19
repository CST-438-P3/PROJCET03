package edu.csumb.api.post;

import edu.csumb.api.post.service.PostService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /posts and GET, PUT, PATCH, DELETE /posts/{postId} from openapi.yaml. */
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    /** Creates the controller with the service that does the work. */
    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** Creates a post for a shelter the signed-in user is staff for (S7). */
    @PostMapping
    public ResponseEntity<PostResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody PostRequest request) {
        PostResponse post = postService.create(userId(jwt), request);
        return ResponseEntity.created(URI.create("/posts/" + post.id())).body(post);
    }

    /** Returns one post (S6). */
    @GetMapping("/{postId}")
    public PostResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId) {
        return postService.get(userId(jwt), postId);
    }

    /** Replaces a post, for staff of the post's shelter only (S7). */
    @PutMapping("/{postId}")
    public PostResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId,
                               @RequestBody PostUpdateRequest request) {
        return postService.update(userId(jwt), postId, request);
    }

    /** Changes a post's status, e.g. marks it adopted, for staff of the post's shelter only (S7). */
    @PatchMapping("/{postId}")
    public PostResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId,
                                     @RequestBody PostStatusRequest request) {
        return postService.changeStatus(userId(jwt), postId, request);
    }

    /** Deletes a post, for staff of the post's shelter only (S7). */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long postId) {
        postService.delete(userId(jwt), postId);
        return ResponseEntity.noContent().build();
    }

    /** The signed-in user's id, which the API puts in the token's subject. */
    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}