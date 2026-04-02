package com.pv.posts.controller;

import com.pv.posts.dto.*;
import com.pv.posts.service.PostsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post lifecycle operations: create, read, delete")
@SecurityRequirement(name = "bearerAuth")
public class PostsController {

    private final PostsService postsService;

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UUID authorId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        log.info("[POST /posts] authorId={}", authorId);
        PostResponse response = postsService.createPost(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single post by ID")
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "Post UUID") @PathVariable UUID id
    ) {
        log.info("[GET /posts/{}]", id);
        return ResponseEntity.ok(postsService.getPostById(id));
    }

    @GetMapping
    @Operation(summary = "List posts by author (paginated)")
    public ResponseEntity<PagedResponse<PostResponse>> listPosts(
            @Parameter(description = "Author UUID to filter by") @RequestParam UUID authorId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[GET /posts] authorId={} page={} size={}", authorId, page, size);
        return ResponseEntity.ok(postsService.listPostsByAuthor(authorId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete own post")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UUID requesterId,
            @Parameter(description = "Post UUID") @PathVariable UUID id
    ) {
        log.info("[DELETE /posts/{}] requesterId={}", id, requesterId);
        postsService.deletePost(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}

