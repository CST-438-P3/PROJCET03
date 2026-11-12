package edu.csumb.api.post.service;

import edu.csumb.api.error.ForbiddenException;
import edu.csumb.api.error.NotFoundException;
import edu.csumb.api.post.AdoptionPost;
import edu.csumb.api.post.PostRepository;
import edu.csumb.api.post.PostRequest;
import edu.csumb.api.post.PostResponse;
import edu.csumb.api.shelter.ShelterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and reads adoption posts (S6, S7). */
@Service
public class PostService {

    private final PostRepository posts;
    private final ShelterRepository shelters;

    /** Creates the service with the repositories it reads and writes. */
    public PostService(PostRepository posts, ShelterRepository shelters) {
        this.posts = posts;
        this.shelters = shelters;
    }

    /**
     * Saves a new post for a shelter the user is staff for and returns it (S7). Who may post is checked before
     * the fields, so a user gets 403 for any shelter they are not staff for, even if the fields are wrong.
     */
    @Transactional
    public PostResponse create(long userId, PostRequest request) {
        Long shelterId = request.shelterId();
        if (shelterId != null && !PostRules.canEdit(shelters.findShelterIdsForStaff(userId), shelterId)) {
            throw new ForbiddenException("You can't post for this shelter");
        }
        PostRules.CheckedPost checked = PostRules.checkCreate(request);
        AdoptionPost post = posts.save(new AdoptionPost(
            shelters.getReferenceById(shelterId),
            checked.name(),
            checked.species(),
            checked.breed(),
            checked.sex(),
            checked.size(),
            checked.ageMonths(),
            checked.description(),
            checked.photoUrl()));
        return PostResponse.from(post);
    }

    /** Returns one post, or throws NotFoundException if there is no post with this id (S6). */
    @Transactional(readOnly = true)
    public PostResponse get(long postId) {
        return posts.findById(postId)
            .map(PostResponse::from)
            .orElseThrow(() -> new NotFoundException("This post is no longer available"));
    }
}
