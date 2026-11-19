package edu.csumb.api.post;

/** The body of POST /posts, the PostCreate schema in openapi.yaml. PostRules checks it before it is saved. */
public record PostRequest(
        Long shelterId,
        String name,
        Species species,
        String breed,
        Sex sex,
        Size size,
        Integer ageMonths,
        String description,
        String photoUrl) {
}
