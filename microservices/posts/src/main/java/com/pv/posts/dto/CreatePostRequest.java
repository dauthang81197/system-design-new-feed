package com.pv.posts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to create a new post")
public class CreatePostRequest {

    @Size(max = 2200, message = "Caption must not exceed 2200 characters")
    @Schema(description = "Post caption text", example = "Hello world!", maxLength = 2200)
    private String caption;

    @Schema(description = "URL of the already-uploaded media file", example = "https://cdn.example.com/photo.jpg")
    private String mediaUrl;
}

