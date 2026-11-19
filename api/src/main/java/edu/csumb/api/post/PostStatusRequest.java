package edu.csumb.api.post;

/** The body of PATCH /posts/{postId} in openapi.yaml, e.g. {"status": "ADOPTED"}. */
public record PostStatusRequest(PostStatus status) {
}
