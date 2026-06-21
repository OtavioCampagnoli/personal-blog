package com.otavio.blog.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddImageToTopicRequest(

    @NotBlank(message = "Image ID is required")
    String imageId,

    Integer position,

    @Size(max = 300, message = "Caption cannot exceed 300 characters")
    String caption
) {}
