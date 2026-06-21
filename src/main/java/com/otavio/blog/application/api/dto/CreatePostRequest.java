package com.otavio.blog.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO para requisição de criação de post
 */
public record CreatePostRequest(
    
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    String title,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,
    
    @NotNull(message = "Category ID is required")
    String categoryId,
    
    @Size(max = 10, message = "Maximum 10 tags allowed")
    List<@Size(min = 2, max = 50, message = "Each tag must be between 2 and 50 characters") String> tags
) {
    public CreatePostRequest {
        // Normalize tags if not null
        if (tags != null) {
            tags = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .toList();
        }
    }
}
