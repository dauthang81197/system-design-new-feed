package com.pv.posts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Post response payload")
public class PostResponse {

    @Schema(description = "Post unique identifier (UUID v4)")
    private UUID id;

    @Schema(description = "Author user ID (UUID v4)")
    private UUID authorId;

    @Schema(description = "Post caption")
    private String caption;

    @Schema(description = "Media URL")
    private String mediaUrl;

    @Schema(description = "Creation timestamp in ISO-8601")
    private Instant createdAt;
}

