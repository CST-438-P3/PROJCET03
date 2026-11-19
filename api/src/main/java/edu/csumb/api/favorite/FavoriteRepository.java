package edu.csumb.api.favorite;

import edu.csumb.api.post.AdoptionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Reads and writes the favorites table. One row means "this user saved this post".
 * The table's primary key is (user_id, post_id), so the same post can't be saved twice.
 */
public interface FavoriteRepository extends Repository<AdoptionPost, Long> {

    // The posts this user saved, newest save first.
    @Query(value = "SELECT p.* FROM adoption_posts p JOIN favorites f ON f.post_id = p.id "
            + "WHERE f.user_id = :userId ORDER BY f.created_at DESC, p.id DESC",
            countQuery = "SELECT count(*) FROM favorites WHERE user_id = :userId",
            nativeQuery = true)
    Page<AdoptionPost> findFavoritePosts(@Param("userId") long userId, Pageable pageable);

    // Says whether this user saved this specific post.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM favorites WHERE user_id = :userId AND post_id = :postId)",
            nativeQuery = true)
    boolean isFavorite(@Param("userId") long userId, @Param("postId") long postId);

    // Saves the post for the user. Does nothing if they already saved it.
    @Modifying
    @Query(value = "INSERT INTO favorites (user_id, post_id) VALUES (:userId, :postId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void addFavorite(@Param("userId") long userId, @Param("postId") long postId);

    // Removes the post from the user's favorites.Does nothing if it wasn't saved.
    @Modifying
    @Query(value = "DELETE FROM favorites WHERE user_id = :userId AND post_id = :postId", nativeQuery = true)
    void removeFavorite(@Param("userId") long userId, @Param("postId") long postId);
}