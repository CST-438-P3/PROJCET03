package edu.csumb.api.post;

import edu.csumb.api.post.service.PostService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /posts and GET /posts/{postId} from openapi.yaml. */
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
        PostResponse post = postService.create(Long.parseLong(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/posts/" + post.id())).body(post);
    }

    /** Returns one post (S6). */
    @GetMapping("/{postId}")
    public PostResponse get(@PathVariable long postId) {
        return postService.get(postId);
    }
}
