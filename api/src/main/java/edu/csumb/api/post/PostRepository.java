package edu.csumb.api.post;

import org.springframework.data.jpa.repository.JpaRepository;

/** Reads and saves rows of adoption_posts. */
public interface PostRepository extends JpaRepository<AdoptionPost, Long> {
}
