---
inclusion: auto
---

# Validation Notification Pattern

## Context
Use the Notification Pattern (Martin Fowler) for domain validation to accumulate ALL validation errors before throwing an exception, rather than failing on the first error. This provides a much better user experience.

## Why Notification Pattern?

### Traditional Approach (BAD ❌)
```java
public Post(String title, String content) {
    if (title == null || title.isBlank()) {
        throw new ValidationException("Title is required");  // Stops here!
    }
    if (title.length() < 10) {
        throw new ValidationException("Title too short");
    }
    if (content == null) {
        throw new ValidationException("Content is required");
    }
    // User only sees ONE error at a time - poor UX!
}
```

### Notification Pattern (GOOD ✅)
```java
public Post(String title, String content, Notification notification) {
    if (title == null || title.isBlank()) {
        notification.addError("title", "Title is required");
    } else if (title.length() < 10) {
        notification.addError("title", "Title must be at least 10 characters");
    }
    
    if (content == null || content.isBlank()) {
        notification.addError("content", "Content is required");
    }
    
    if (notification.hasErrors()) {
        throw new DomainValidationException(notification);
    }
    // User sees ALL errors at once - excellent UX!
}
```

## Notification Implementation

### Core Notification Class
```java
package com.blog.blog.domain.validation;

import java.util.*;

/**
 * Accumulates validation errors instead of throwing on first failure.
 * Based on Martin Fowler's Notification Pattern.
 */
public class Notification {
    
    private final Map<String, List<String>> errors = new LinkedHashMap<>();
    
    /**
     * Add a field-specific error message
     */
    public void addError(String field, String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }
    
    /**
     * Add a field-specific error with formatted message
     */
    public void addError(String field, String messageFormat, Object... args) {
        addError(field, String.format(messageFormat, args));
    }
    
    /**
     * Add a global error (not tied to specific field)
     */
    public void addGlobalError(String message) {
        addError("_global", message);
    }
    
    /**
     * Check if any errors have been accumulated
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get all errors as a flat map (field -> first error message)
     */
    public Map<String, String> getErrors() {
        Map<String, String> flatErrors = new LinkedHashMap<>();
        errors.forEach((field, messages) -> 
            flatErrors.put(field, messages.get(0))
        );
        return Collections.unmodifiableMap(flatErrors);
    }
    
    /**
     * Get all errors including multiple errors per field
     */
    public Map<String, List<String>> getAllErrors() {
        return Collections.unmodifiableMap(errors);
    }
    
    /**
     * Get error count
     */
    public int errorCount() {
        return errors.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Check if a specific field has errors
     */
    public boolean hasError(String field) {
        return errors.containsKey(field);
    }
    
    /**
     * Get errors for a specific field
     */
    public List<String> getErrorsFor(String field) {
        return errors.getOrDefault(field, Collections.emptyList());
    }
    
    /**
     * Merge another notification into this one
     */
    public void merge(Notification other) {
        other.errors.forEach((field, messages) -> 
            messages.forEach(msg -> addError(field, msg))
        );
    }
    
    /**
     * Create a new empty notification
     */
    public static Notification create() {
        return new Notification();
    }
    
    /**
     * Throw DomainValidationException if errors exist
     */
    public void throwIfHasErrors() {
        if (hasErrors()) {
            throw new DomainValidationException(this);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Validation errors:\n");
        errors.forEach((field, messages) -> {
            sb.append("  ").append(field).append(":\n");
            messages.forEach(msg -> sb.append("    - ").append(msg).append("\n"));
        });
        return sb.toString();
    }
}
```

### Domain Validation Exception
```java
package com.blog.blog.domain.exception;

import com.blog.blog.domain.validation.Notification;
import java.util.Map;
import java.util.List;

public class DomainValidationException extends BlogDomainException {
    
    private final Map<String, String> errors;
    private final Map<String, List<String>> allErrors;
    
    public DomainValidationException(Notification notification) {
        super("Domain validation failed with " + notification.errorCount() + " error(s)");
        this.errors = notification.getErrors();
        this.allErrors = notification.getAllErrors();
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
    
    public Map<String, List<String>> getAllErrors() {
        return allErrors;
    }
    
    public boolean hasError(String field) {
        return errors.containsKey(field);
    }
    
    public String getError(String field) {
        return errors.get(field);
    }
}
```

## Using Notification in Domain Entities

### Entity with Notification Validation
```java
package com.blog.blog.domain.model;

import com.blog.blog.domain.validation.Notification;
import java.util.UUID;
import java.time.Instant;

public class Post {
    
    private UUID id;
    private String title;
    private String slug;
    private String content;
    private PostStatus status;
    private UUID categoryId;
    private Instant createdAt;
    private Instant publishedAt;
    
    // Constructor with validation
    public Post(String title, String content, UUID categoryId, String slug) {
        Notification notification = Notification.create();
        
        // Validate all fields, accumulating errors
        this.title = validateTitle(title, notification);
        this.content = validateContent(content, notification);
        this.categoryId = validateCategoryId(categoryId, notification);
        this.slug = validateSlug(slug, notification);
        
        // Throw only if there are errors
        notification.throwIfHasErrors();
        
        // Set remaining fields
        this.id = UUID.randomUUID();
        this.status = PostStatus.DRAFT;
        this.createdAt = Instant.now();
    }
    
    private String validateTitle(String title, Notification notification) {
        if (title == null || title.isBlank()) {
            notification.addError("title", "Title is required");
            return null;
        }
        
        String trimmed = title.trim();
        
        if (trimmed.length() < 10) {
            notification.addError("title", 
                "Title must be at least 10 characters (provided: %d)", 
                trimmed.length());
        }
        
        if (trimmed.length() > 200) {
            notification.addError("title", 
                "Title must not exceed 200 characters (provided: %d)", 
                trimmed.length());
        }
        
        return trimmed;
    }
    
    private String validateContent(String content, Notification notification) {
        if (content == null || content.isBlank()) {
            notification.addError("content", "Content is required");
            return null;
        }
        
        String trimmed = content.trim();
        
        if (trimmed.length() < 50) {
            notification.addError("content", 
                "Content must be at least 50 characters (provided: %d)", 
                trimmed.length());
        }
        
        return trimmed;
    }
    
    private UUID validateCategoryId(UUID categoryId, Notification notification) {
        if (categoryId == null) {
            notification.addError("categoryId", "Category is required");
        }
        return categoryId;
    }
    
    private String validateSlug(String slug, Notification notification) {
        if (slug == null || slug.isBlank()) {
            notification.addError("slug", "Slug is required");
            return null;
        }
        
        String trimmed = slug.trim();
        
        // Slug format validation
        if (!trimmed.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            notification.addError("slug", 
                "Slug must contain only lowercase letters, numbers, and hyphens (provided: '%s')", 
                trimmed);
        }
        
        return trimmed;
    }
    
    // Business method with validation
    public void publish() {
        Notification notification = Notification.create();
        
        if (this.status == PostStatus.PUBLISHED) {
            notification.addGlobalError("Post is already published");
        }
        
        if (this.title == null || this.title.isBlank()) {
            notification.addError("title", "Cannot publish post without title");
        }
        
        if (this.content == null || this.content.isBlank()) {
            notification.addError("content", "Cannot publish post without content");
        }
        
        notification.throwIfHasErrors();
        
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
    
    // Update method with validation
    public void update(String newTitle, String newContent) {
        Notification notification = Notification.create();
        
        String validatedTitle = validateTitle(newTitle, notification);
        String validatedContent = validateContent(newContent, notification);
        
        notification.throwIfHasErrors();
        
        this.title = validatedTitle;
        this.content = validatedContent;
    }
}
```

### Complex Cross-Field Validation
```java
public class Comment {
    
    private UUID id;
    private UUID postId;
    private String authorName;
    private String authorEmail;
    private String content;
    private CommentStatus status;
    
    public Comment(UUID postId, String authorName, String authorEmail, String content) {
        Notification notification = Notification.create();
        
        // Individual field validations
        this.postId = validatePostId(postId, notification);
        this.authorName = validateAuthorName(authorName, notification);
        this.authorEmail = validateAuthorEmail(authorEmail, notification);
        this.content = validateContent(content, notification);
        
        // Cross-field validation (only if no errors so far)
        if (!notification.hasErrors()) {
            validateBusinessRules(notification);
        }
        
        notification.throwIfHasErrors();
        
        this.id = UUID.randomUUID();
        this.status = CommentStatus.PENDING;
    }
    
    private void validateBusinessRules(Notification notification) {
        // Example: Check if author name and email match some pattern
        if (authorName.equalsIgnoreCase("admin") && !authorEmail.endsWith("@blog.com")) {
            notification.addError("authorEmail", 
                "Admin users must use @blog.com email addresses");
        }
        
        // Example: Content length based on author status
        if (isNewAuthor() && content.length() < 100) {
            notification.addError("content", 
                "First-time commenters must provide at least 100 characters");
        }
    }
    
    private UUID validatePostId(UUID postId, Notification notification) {
        if (postId == null) {
            notification.addError("postId", "Post ID is required");
        }
        return postId;
    }
    
    private String validateAuthorName(String name, Notification notification) {
        if (name == null || name.isBlank()) {
            notification.addError("authorName", "Author name is required");
            return null;
        }
        
        String trimmed = name.trim();
        
        if (trimmed.length() < 2) {
            notification.addError("authorName", "Author name must be at least 2 characters");
        }
        
        if (trimmed.length() > 100) {
            notification.addError("authorName", "Author name must not exceed 100 characters");
        }
        
        return trimmed;
    }
    
    private String validateAuthorEmail(String email, Notification notification) {
        if (email == null || email.isBlank()) {
            notification.addError("authorEmail", "Author email is required");
            return null;
        }
        
        String trimmed = email.trim().toLowerCase();
        
        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            notification.addError("authorEmail", 
                "Invalid email format. Example: user@example.com");
        }
        
        return trimmed;
    }
    
    private String validateContent(String content, Notification notification) {
        if (content == null || content.isBlank()) {
            notification.addError("content", "Comment content is required");
            return null;
        }
        
        String trimmed = content.trim();
        
        if (trimmed.length() < 10) {
            notification.addError("content", 
                "Comment must be at least 10 characters (provided: %d)", 
                trimmed.length());
        }
        
        if (trimmed.length() > 2000) {
            notification.addError("content", 
                "Comment must not exceed 2000 characters (provided: %d)", 
                trimmed.length());
        }
        
        return trimmed;
    }
}
```

## API Response with All Errors

### Error Response DTO
```java
package com.blog.blog.api.dto;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    Map<String, String> fieldErrors,
    String path
) {
    public static ValidationErrorResponse from(
        DomainValidationException ex,
        String path,
        String message
    ) {
        return new ValidationErrorResponse(
            Instant.now(),
            400,
            "Bad Request",
            "VALIDATION_ERROR",
            message,
            ex.getErrors(),
            path
        );
    }
}
```

### Global Exception Handler
```java
package com.blog.blog.api.exception;

import com.blog.blog.domain.exception.DomainValidationException;
import com.blog.blog.api.dto.ValidationErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final MessageSource messageSource;
    
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
        DomainValidationException ex,
        HttpServletRequest request,
        Locale locale
    ) {
        String message = messageSource.getMessage(
            "error.validation.multiple",
            new Object[]{ex.getErrors().size()},
            "Validation failed with {0} error(s)",
            locale
        );
        
        ValidationErrorResponse response = ValidationErrorResponse.from(
            ex,
            request.getRequestURI(),
            message
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
}
```

### Example API Response (All Errors at Once)
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed with 4 error(s)",
  "fieldErrors": {
    "title": "Title must be at least 10 characters (provided: 5)",
    "content": "Content is required",
    "categoryId": "Category is required",
    "slug": "Slug must contain only lowercase letters, numbers, and hyphens (provided: 'My Post!')"
  },
  "path": "/api/blog/posts"
}
```

## Testing Notification Pattern

### Unit Tests
```java
class PostValidationTest {
    
    @Test
    @DisplayName("Should accumulate all validation errors")
    void shouldAccumulateAllValidationErrors() {
        // Arrange
        String shortTitle = "Short";  // Too short
        String emptyContent = "";     // Empty
        UUID nullCategory = null;     // Null
        String invalidSlug = "Invalid Slug!";  // Invalid format
        
        // Act & Assert
        assertThatThrownBy(() -> 
            new Post(shortTitle, emptyContent, nullCategory, invalidSlug)
        )
            .isInstanceOf(DomainValidationException.class)
            .satisfies(ex -> {
                DomainValidationException validationEx = (DomainValidationException) ex;
                Map<String, String> errors = validationEx.getErrors();
                
                // Assert all 4 errors are present
                assertThat(errors).hasSize(4);
                assertThat(errors).containsKey("title");
                assertThat(errors).containsKey("content");
                assertThat(errors).containsKey("categoryId");
                assertThat(errors).containsKey("slug");
                
                // Assert specific error messages
                assertThat(errors.get("title"))
                    .contains("at least 10 characters");
                assertThat(errors.get("content"))
                    .isEqualTo("Content is required");
                assertThat(errors.get("categoryId"))
                    .isEqualTo("Category is required");
                assertThat(errors.get("slug"))
                    .contains("lowercase letters, numbers, and hyphens");
            });
    }
    
    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void shouldPassValidationWhenAllFieldsValid() {
        // Arrange
        String validTitle = "My Amazing Blog Post Title";
        String validContent = "This is a valid content with more than 50 characters for sure.";
        UUID validCategory = UUID.randomUUID();
        String validSlug = "my-amazing-blog-post-title";
        
        // Act
        Post post = new Post(validTitle, validContent, validCategory, validSlug);
        
        // Assert
        assertThat(post).isNotNull();
        assertThat(post.getTitle()).isEqualTo(validTitle);
        assertThat(post.getContent()).isEqualTo(validContent);
    }
    
    @Test
    @DisplayName("Should accumulate errors from business rule validation")
    void shouldAccumulateErrorsFromBusinessRuleValidation() {
        // Arrange
        Post publishedPost = createPublishedPost();
        publishedPost.setTitle(null);  // Invalid for publishing
        publishedPost.setContent("");   // Invalid for publishing
        
        // Act & Assert
        assertThatThrownBy(() -> publishedPost.publish())
            .isInstanceOf(DomainValidationException.class)
            .satisfies(ex -> {
                DomainValidationException validationEx = (DomainValidationException) ex;
                Map<String, String> errors = validationEx.getErrors();
                
                assertThat(errors).hasSize(3);
                assertThat(errors).containsKey("_global");  // Already published
                assertThat(errors).containsKey("title");    // Missing title
                assertThat(errors).containsKey("content");  // Missing content
            });
    }
}
```

## Benefits

1. **Better UX**: Users see all validation errors at once, not one at a time
2. **Fewer roundtrips**: Fix all issues in one go instead of multiple submissions
3. **Clear feedback**: Each field gets specific, actionable error messages
4. **Testable**: Easy to assert on specific error conditions
5. **Maintainable**: Centralized validation logic in domain entities
6. **Flexible**: Easy to add new validations without breaking existing code

## When to Use

- ✅ Domain entity construction and updates
- ✅ Complex business rule validation
- ✅ Multi-field cross-validation
- ✅ User input validation (forms, APIs)
- ❌ Simple null checks (use Objects.requireNonNull)
- ❌ Programming errors (use assertions or exceptions immediately)
