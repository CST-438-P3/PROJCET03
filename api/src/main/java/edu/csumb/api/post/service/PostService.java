package edu.csumb.api.post.service;

import edu.csumb.api.error.ForbiddenException;
import edu.csumb.api.error.NotFoundException;
import edu.csumb.api.post.AdoptionPost;
import edu.csumb.api.post.PostRepository;
import edu.csumb.api.post.PostRequest;
import edu.csumb.api.post.PostResponse;
import edu.csumb.api.post.PostStatusRequest;
import edu.csumb.api.post.PostUpdateRequest;
import edu.csumb.api.shelter.ShelterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates, reads, edits and deletes adoption posts (S6, S7). */
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
        return PostResponse.from(find(postId));
    }

    /**
     * Replaces every field of a post and returns it (S7). Like create, who may edit is checked before the fields,
     * so a user gets 403 for any post whose shelter they are not staff for, even if the fields are wrong.
     */
    @Transactional
    public PostResponse update(long userId, long postId, PostUpdateRequest request) {
        AdoptionPost post = findForStaff(userId, postId, "You can't edit this post");
        PostRules.CheckedPost checked = PostRules.checkUpdate(request);
        post.replace(
            checked.name(),
            checked.species(),
            checked.breed(),
            checked.sex(),
            checked.size(),
            checked.ageMonths(),
            checked.description(),
            checked.photoUrl(),
            request.status());
        return PostResponse.from(post);
    }

    /** Changes only the status of a post, e.g. marks it adopted, and returns it (S7). */
    @Transactional
    public PostResponse changeStatus(long userId, long postId, PostStatusRequest request) {
        AdoptionPost post = findForStaff(userId, postId, "You can't edit this post");
        post.changeStatus(PostRules.checkStatus(request.status()));
        return PostResponse.from(post);
    }

    /** Deletes a post; the database also deletes its favorites and inquiries (S7). */
    @Transactional
    public void delete(long userId, long postId) {
        posts.delete(findForStaff(userId, postId, "You can't delete this post"));
    }

    /** Returns the post, or throws NotFoundException if there is no post with this id. */
    private AdoptionPost find(long postId) {
        return posts.findById(postId)
            .orElseThrow(() -> new NotFoundException("This post is no longer available"));
    }

    /**
     * Returns the post if the user is staff for its shelter. Throws NotFoundException if there is no such post,
     * or ForbiddenException with this message if the user is not staff for its shelter.
     */
    private AdoptionPost findForStaff(long userId, long postId, String forbiddenMessage) {
        AdoptionPost post = find(postId);
        if (!PostRules.canEdit(shelters.findShelterIdsForStaff(userId), post.getShelter().getId())) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return post;
    }
}
