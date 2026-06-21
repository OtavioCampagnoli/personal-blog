package com.otavio.blog.shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void shouldStartWithNoErrors() {
        Notification n = Notification.create();
        assertFalse(n.hasErrors());
        assertEquals(0, n.errorCount());
    }

    @Test
    void shouldAccumulateMultipleErrors() {
        Notification n = Notification.create();
        n.addError("title", "TITLE_REQUIRED", "Title is required");
        n.addError("content", "CONTENT_TOO_SHORT", "Content too short");

        assertTrue(n.hasErrors());
        assertEquals(2, n.errorCount());
    }

    @Test
    void shouldThrowDomainValidationExceptionWhenHasErrors() {
        Notification n = Notification.create();
        n.addError("field", "CODE", "message");

        DomainValidationException ex = assertThrows(
            DomainValidationException.class,
            n::throwIfHasErrors
        );

        assertEquals(1, ex.getErrors().size());
        assertEquals("field", ex.getErrors().get(0).field());
        assertEquals("CODE", ex.getErrors().get(0).code());
    }

    @Test
    void shouldNotThrowWhenNoErrors() {
        Notification n = Notification.create();
        assertDoesNotThrow(n::throwIfHasErrors);
    }

    @Test
    void shouldStoreProvidedValueAndExample() {
        Notification n = Notification.create();
        n.addError("title", "TOO_SHORT", "Too short", "Hi", "Hello World");

        Notification.Error error = n.getErrors().get(0);
        assertEquals("Hi", error.providedValue());
        assertEquals("Hello World", error.example());
    }
}
