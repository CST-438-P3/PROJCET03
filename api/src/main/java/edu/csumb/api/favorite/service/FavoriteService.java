package edu.csumb.api.favorite.service;

import edu.csumb.api.error.BadRequestException;
import edu.csumb.api.error.NotFoundException;
import edu.csumb.api.favorite.FavoriteRepository;
import edu.csumb.api.post.AdoptionPost;
import edu.csumb.api.post.PostPage;
import edu.csumb.api.post.PostRepository;
import edu.csumb.api.post.PostResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves, removes and lists a user's favorite posts (S8).
 * Every method takes the signed-in user's id from their token, so a user can only ever
 * read or change their own favorites.
 */
@Service
public class FavoriteService {

    // Most posts on one page, the same as the Size parameter in openapi.yaml
    public static final int PAGE_SIZE_MAX = 50;

    private final FavoriteRepository favorites;
    private final PostRepository posts;

    // Creates the service with the repositories it reads and writes.
    public FavoriteService(FavoriteRepository favorites, PostRepository posts) {
        this.favorites = favorites;
        this.posts = posts;
    }

    // Returns one page of the user's favorite posts, newest save first.
    @Transactional(readOnly = true)
    public PostPage list(long userId, int page, int size) {
        checkPage(page, size);
        Page<AdoptionPost> found = favorites.findFavoritePosts(userId, PageRequest.of(page, size));

        // Every post in this list is a favorite. favorite is always true here
        List<PostResponse> savedPosts = found.getContent().stream()
                .map(post -> PostResponse.from(post).withFavorite(true))
                .toList();
        return new PostPage(page, size, found.getTotalElements(), found.getTotalPages(), savedPosts);
    }

    // Saves the post to the user's favorites. Saving it again is fine and changes nothing. */
    @Transactional
    public void save(long userId, long postId) {
        checkPostExists(postId);
        favorites.addFavorite(userId, postId);
    }

    // Removes the post from the user's favorites.
    @Transactional
    public void remove(long userId, long postId) {
        checkPostExists(postId);
        favorites.removeFavorite(userId, postId);
    }

    // Throws NotFoundException if there is no post with this id, so the API should answer with 404.
    private void checkPostExists(long postId) {
        if (!posts.existsById(postId)) {
            throw new NotFoundException("This post is no longer available");
        }
    }

    // Checks the page and size of GET /favorites, or throws a BadRequestException */
    static void checkPage(int page, int size) {
        List<String> errors = new ArrayList<>();
        if (page < 0) {
            errors.add("Page must be 0 or more");
        }
        if (size < 1 || size > PAGE_SIZE_MAX) {
            errors.add("Size must be between 1 and " + PAGE_SIZE_MAX);
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
    }
}