package com.otavio.blog.domain.topic;

import com.otavio.blog.domain.post.Post;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicTest {

    @Test
    void shouldCreateValidTopic() {
        Id<Post> postId = Id.generate();
        Topic topic = Topic.create(postId, "Introdução ao Spring Boot",
            "## Introdução\n\nSpring Boot é um framework que facilita...", 1);

        assertNotNull(topic.getId());
        assertEquals(postId, topic.getPostId());
        assertEquals("Introdução ao Spring Boot", topic.getTitle());
        assertEquals(1, topic.getOrderNumber());
    }

    @Test
    void shouldPassValidationForValidTopic() {
        Topic topic = Topic.create(Id.generate(), "Título Válido",
            "Conteúdo com mais de dez caracteres", 1);

        Notification n = Notification.create();
        topic.validate(n);
        assertFalse(n.hasErrors());
    }

    @Test
    void shouldFailWhenTitleIsBlank() {
        Topic topic = Topic.create(Id.generate(), "  ", "Conteúdo suficiente aqui", 1);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("TITLE_REQUIRED", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenTitleTooShort() {
        Topic topic = Topic.create(Id.generate(), "AB", "Conteúdo suficiente aqui", 1);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("TITLE_TOO_SHORT", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenTitleTooLong() {
        String longTitle = "A".repeat(101);
        Topic topic = Topic.create(Id.generate(), longTitle, "Conteúdo suficiente aqui", 1);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("TITLE_TOO_LONG", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenContentTooShort() {
        Topic topic = Topic.create(Id.generate(), "Título Válido", "Curto", 1);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("CONTENT_TOO_SHORT", n.getErrors().get(0).code());
    }

    @Test
    void shouldFailWhenOrderLessThanOne() {
        Topic topic = Topic.create(Id.generate(), "Título Válido",
            "Conteúdo suficiente aqui", 0);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.hasErrors());
        assertEquals("ORDER_INVALID", n.getErrors().get(0).code());
    }

    @Test
    void shouldAccumulateMultipleValidationErrors() {
        Topic topic = Topic.create(Id.generate(), "", "x", 0);
        Notification n = Notification.create();
        topic.validate(n);

        assertTrue(n.errorCount() >= 2); // title + content + order
    }

    @Test
    void shouldRecognizeCorrectPostOwnership() {
        Id<Post> postId = Id.generate();
        Topic topic = Topic.create(postId, "Título", "Conteúdo suficiente aqui", 1);

        assertTrue(topic.belongsToPost(postId));
        assertFalse(topic.belongsToPost(Id.generate()));
    }
}
