package com.pv.posts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {

    private UUID postId;
    private UUID authorId;
    private String caption;
    private String mediaUrl;
    private Instant createdAt;
}

