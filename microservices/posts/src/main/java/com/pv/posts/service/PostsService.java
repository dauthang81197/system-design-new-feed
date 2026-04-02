package com.pv.posts.service;

import com.pv.posts.dto.*;
import com.pv.posts.entity.Post;
import com.pv.posts.exception.ForbiddenException;
import com.pv.posts.exception.PostNotFoundException;
import com.pv.posts.kafka.PostEventProducer;
import com.pv.posts.mapper.PostMapper;
import com.pv.posts.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostsService {

    private final PostsRepository postsRepository;
    private final PostEventProducer postEventProducer;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(UUID authorId, CreatePostRequest request) {
        log.info("[PostsService] Creating post for authorId={}", authorId);

        Post post = Post.builder()
                .authorId(authorId)
                .caption(request.getCaption())
                .mediaUrl(request.getMediaUrl())
                .build();

        Post saved = postsRepository.save(post);
        log.debug("[PostsService] Post saved with id={}", saved.getId());

        PostCreatedEvent event = PostCreatedEvent.builder()
                .postId(saved.getId())
                .authorId(saved.getAuthorId())
                .caption(saved.getCaption())
                .mediaUrl(saved.getMediaUrl())
                .createdAt(saved.getCreatedAt())
                .build();

        postEventProducer.publishPostCreated(event);

        return postMapper.toResponse(saved);
    }

    @Cacheable(value = "posts", key = "#postId")
    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId) {
        log.info("[PostsService] Fetching post id={}", postId);
        Post post = postsRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        return postMapper.toResponse(post);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> listPostsByAuthor(UUID authorId, int page, int size) {
        log.info("[PostsService] Listing posts for authorId={} page={} size={}", authorId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postsRepository.findByAuthorId(authorId, pageable);

        return PagedResponse.<PostResponse>builder()
                .content(postPage.getContent().stream().map(postMapper::toResponse).toList())
                .page(postPage.getNumber())
                .size(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .last(postPage.isLast())
                .build();
    }

    @CacheEvict(value = "posts", key = "#postId")
    @Transactional
    public void deletePost(UUID postId, UUID requesterId) {
        log.info("[PostsService] Deleting post id={} by requesterId={}", postId, requesterId);

        Post post = postsRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getAuthorId().equals(requesterId)) {
            throw new ForbiddenException("You are not allowed to delete this post");
        }

        post.setDeletedAt(Instant.now());
        postsRepository.save(post);
        log.info("[PostsService] Post id={} soft-deleted", postId);
    }
}

