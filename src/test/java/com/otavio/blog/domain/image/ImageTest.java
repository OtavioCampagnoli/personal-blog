package com.otavio.blog.domain.image;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageTest {

    private Image validImage() {
        return Image.create(Id.generate(), "images/author/uuid.jpg",
            "http://cdn/uuid.jpg", "Alt text", "image/jpeg",
            1_048_576L, 800, 600);
    }

    @Test
    void shouldCreateValidImage() {
        Image img = validImage();
        assertNotNull(img.getId());
        assertEquals("image/jpeg", img.getContentType());
        assertEquals(800, img.getWidth());
        assertEquals(600, img.getHeight());
        assertNotNull(img.getUploadedAt());
    }

    @Test
    void shouldPassValidation() {
        Notification n = Notification.create();
        validImage().validate(n);
        assertFalse(n.hasErrors());
    }

    @Test
    void shouldFailForUnsupportedContentType() {
        Image img = Image.create(Id.generate(), "k", "u", null, "application/pdf",
            1_000L, 800, 600);
        Notification n = Notification.create();
        img.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("UNSUPPORTED_FILE_TYPE", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenFileTooLarge() {
        long sixMB = 6L * 1024 * 1024;
        Image img = Image.create(Id.generate(), "k", "u", null, "image/jpeg",
            sixMB, 800, 600);
        Notification n = Notification.create();
        img.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("IMAGE_TOO_LARGE", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenDimensionsTooSmall() {
        Image img = Image.create(Id.generate(), "k", "u", null, "image/jpeg",
            1_000L, 50, 50);
        Notification n = Notification.create();
        img.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("DIMENSIONS_TOO_SMALL", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenDimensionsTooLarge() {
        Image img = Image.create(Id.generate(), "k", "u", null, "image/jpeg",
            1_000L, 5000, 5000);
        Notification n = Notification.create();
        img.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("DIMENSIONS_TOO_LARGE", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenAltTextTooLong() {
        String longAlt = "A".repeat(201);
        Image img = Image.create(Id.generate(), "k", "u", longAlt, "image/jpeg",
            1_000L, 800, 600);
        Notification n = Notification.create();
        img.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("ALT_TEXT_TOO_LONG", n.getErrors().get(0).code());
    }

    @Test
    void shouldAllowWebpPngGif() {
        for (String type : new String[]{"image/png", "image/webp", "image/gif"}) {
            Image img = Image.create(Id.generate(), "k", "u", null, type,
                1_000L, 800, 600);
            Notification n = Notification.create();
            img.validate(n);
            assertFalse(n.hasErrors(), "Should be valid for: " + type);
        }
    }

    @Test
    void shouldRecognizeAuthorOwnership() {
        Id<Author> authorId = Id.generate();
        Image img = Image.create(authorId, "k", "u", null, "image/jpeg",
            1_000L, 800, 600);

        assertTrue(img.belongsToAuthor(authorId));
        assertFalse(img.belongsToAuthor(Id.generate()));
    }
}
