package edu.csumb.api.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/** A post as the API returns it, the Post schema in openapi.yaml; empty optional fields are left out. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponse(
        long id,
        ShelterSummary shelter,
        String name,
        Species species,
        String breed,
        Sex sex,
        Size size,
        Integer ageMonths,
        String description,
        String photoUrl,
        PostStatus status,
        OffsetDateTime createdAt,
        Boolean favorite) {

    /** The shelter a post belongs to, the ShelterSummary schema in openapi.yaml. */
    public record ShelterSummary(long id, String name) {
    }

    /** response with the favorite field filled in for the signed-in user (S8). */
    public PostResponse withFavorite(boolean isFavorite) {
        return new PostResponse(id, shelter, name, species, breed, sex, size, ageMonths, description, photoUrl,
                status, createdAt, isFavorite);
    }

    /** Builds the response for a saved post, without the favorite field. */
    public static PostResponse from(AdoptionPost post) {
        return new PostResponse(
                post.getId(),
                new ShelterSummary(post.getShelter().getId(), post.getShelter().getName()),
                post.getName(),
                post.getSpecies(),
                post.getBreed(),
                post.getSex(),
                post.getSize(),
                post.getAgeMonths(),
                post.getDescription(),
                post.getPhotoUrl(),
                post.getStatus(),
                post.getCreatedAt(),
                null);
    }
}