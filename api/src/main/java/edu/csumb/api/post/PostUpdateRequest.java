package edu.csumb.api.post;

/** The body of PUT /posts/{postId}, the PostUpdate schema in openapi.yaml. PostRules checks it before it is saved. */
public record PostUpdateRequest(
        String name,
        Species species,
        String breed,
        Sex sex,
        Size size,
        Integer ageMonths,
        String description,
        String photoUrl,
        PostStatus status) {
}
