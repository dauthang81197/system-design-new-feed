package com.pv.posts.repository;

import com.pv.posts.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostsRepository extends JpaRepository<Post, UUID> {

    /**
     * Find posts by author with pagination (soft-delete filter applied by @SQLRestriction).
     */
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);

    /**
     * Find post by id including soft-deleted (used for ownership check before hard operations).
     */
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdIgnoreDelete(UUID id);
}

