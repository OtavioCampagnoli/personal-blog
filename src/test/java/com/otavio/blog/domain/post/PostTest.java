package com.otavio.blog.domain.post;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;
import com.otavio.blog.shared.domain.Slug;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostTest {

    @Test
    void shouldCreateValidPost() {
        // Given
        String title = "My First Blog Post About Java";
        String description = "Learn Java fundamentals";
        Id<Author> authorId = Id.generate();
        Id<Category> categoryId = Id.generate();
        List<String> tags = List.of("java", "programming");

        // When
        Post post = Post.create(title, description, authorId, categoryId, tags);

        // Then
        assertNotNull(post.getId());
        assertEquals(title, post.getTitle());
        assertEquals(description, post.getDescription());
        assertEquals(PostStatus.DRAFT, post.getStatus());
        assertEquals(authorId, post.getAuthorId());
        assertEquals(categoryId, post.getCategoryId());
        assertEquals(2, post.getTags().size());
        assertEquals("java", post.getTags().get(0));
        assertEquals("programming", post.getTags().get(1));
        assertTrue(post.isDraft());
        assertFalse(post.isPublished());
    }

    @Test
    void shouldGenerateSlugFromTitle() {
        // Given
        String title = "Como Construir uma API RESTful em Java";
        Post post = Post.create(title, null, Id.generate(), Id.generate(), null);

        // Then
        assertEquals("como-construir-uma-api-restful-em-java", post.getSlug().getValue());
    }

    @Test
    void shouldNormalizeTags() {
        // Given - tags com espaços, duplicadas e em uppercase
        List<String> tags = List.of("  Java  ", "SPRING", "spring", "boot");
        Post post = Post.create("Valid Title Here", null, Id.generate(), Id.generate(), tags);

        // Then - tags devem estar lowercase, sem espaços e sem duplicatas
        assertEquals(3, post.getTags().size());
        assertTrue(post.getTags().contains("java"));
        assertTrue(post.getTags().contains("spring"));
        assertTrue(post.getTags().contains("boot"));
    }

    @Test
    void shouldFailValidationWhenTitleTooShort() {
        // Given
        Post post = Post.create("Short", null, Id.generate(), Id.generate(), null);
        Notification notification = Notification.create();

        // When
        post.validate(notification);

        // Then
        assertTrue(notification.hasErrors());
        assertEquals(1, notification.errorCount());
        assertEquals("title", notification.getErrors().get(0).field());
        assertEquals("TITLE_TOO_SHORT", notification.getErrors().get(0).code());
    }

    @Test
    void shouldFailValidationWhenTooManyTags() {
        // Given - 11 tags (máximo é 10)
        List<String> tags = List.of("tag1", "tag2", "tag3", "tag4", "tag5", 
                                    "tag6", "tag7", "tag8", "tag9", "tag10", "tag11");
        Post post = Post.create("Valid Title Here", null, Id.generate(), Id.generate(), tags);
        Notification notification = Notification.create();

        // When
        post.validate(notification);

        // Then
        assertTrue(notification.hasErrors());
        assertTrue(notification.getErrors().stream()
            .anyMatch(e -> e.code().equals("TOO_MANY_TAGS")));
    }

    @Test
    void shouldPublishPost() {
        // Given
        Post post = Post.create("Valid Title Here", null, Id.generate(), Id.generate(), null);
        assertTrue(post.isDraft());

        // When
        post.publish();

        // Then
        assertEquals(PostStatus.PUBLISHED, post.getStatus());
        assertTrue(post.isPublished());
        assertNotNull(post.getPublishedAt());
    }

    @Test
    void shouldUnpublishPost() {
        // Given
        Post post = Post.create("Valid Title Here", null, Id.generate(), Id.generate(), null);
        post.publish();
        assertTrue(post.isPublished());

        // When
        post.unpublish();

        // Then
        assertEquals(PostStatus.DRAFT, post.getStatus());
        assertTrue(post.isDraft());
        assertNull(post.getPublishedAt());
    }

    @Test
    void shouldVerifyAuthor() {
        // Given
        Id<Author> authorId = Id.generate();
        Post post = Post.create("Valid Title Here", null, authorId, Id.generate(), null);

        // Then
        assertTrue(post.isAuthor(authorId));
        assertFalse(post.isAuthor(Id.generate())); // Different author
    }
}
