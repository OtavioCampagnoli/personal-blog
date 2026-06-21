package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicRepository;
import com.otavio.blog.shared.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock TopicRepository topicRepository;
    @Mock PostRepository  postRepository;

    TopicService service;

    static final Id<Author>   AUTHOR_ID = Id.of("00000000-0000-0000-0000-000000000001");
    static final Id<Category> CAT_ID    = Id.of("11111111-1111-1111-1111-111111111111");

    Post post;
    Slug postSlug;

    @BeforeEach
    void setUp() {
        service  = new TopicService(topicRepository, postRepository);
        post     = Post.create("Título Válido Para Post", null, AUTHOR_ID, CAT_ID, null);
        postSlug = post.getSlug();
    }

    @Test
    void addTopic_shouldAddToEndByDefault() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.countByPostId(post.getId())).thenReturn(2);
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Topic result = service.addTopic(postSlug, "Meu Tópico",
            "Conteúdo suficiente aqui", null, AUTHOR_ID);

        assertEquals(3, result.getOrderNumber()); // count + 1
        verify(topicRepository, never()).incrementOrdersFrom(any(), anyInt());
    }

    @Test
    void addTopic_shouldRespectSpecificOrder() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.countByPostId(post.getId())).thenReturn(3);
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Topic result = service.addTopic(postSlug, "Meu Tópico",
            "Conteúdo suficiente aqui", 2, AUTHOR_ID);

        assertEquals(2, result.getOrderNumber());
        verify(topicRepository).incrementOrdersFrom(post.getId(), 2);
    }

    @Test
    void addTopic_shouldClampOrderBeyondCount() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.countByPostId(post.getId())).thenReturn(2);
        when(topicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // order=999 with only 2 topics → should land at 3 (end)
        Topic result = service.addTopic(postSlug, "Meu Tópico",
            "Conteúdo suficiente aqui", 999, AUTHOR_ID);

        assertEquals(3, result.getOrderNumber());
        verify(topicRepository, never()).incrementOrdersFrom(any(), anyInt());
    }

    @Test
    void addTopic_shouldThrowWhenPostNotFound() {
        when(postRepository.findBySlug(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            service.addTopic(postSlug, "Título", "Conteúdo suficiente aqui", null, AUTHOR_ID));
    }

    @Test
    void addTopic_shouldThrowWhenNotAuthor() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));

        // TODO: Add JWT authentication so this uses the real user from the token
        assertThrows(ForbiddenException.class, () ->
            service.addTopic(postSlug, "Título", "Conteúdo suficiente aqui", null, Id.generate()));
    }

    @Test
    void addTopic_shouldThrowWhenMaxTopicsReached() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.countByPostId(post.getId())).thenReturn(50);

        assertThrows(DomainException.class, () ->
            service.addTopic(postSlug, "Título", "Conteúdo suficiente aqui", null, AUTHOR_ID));
    }

    @Test
    void addTopic_shouldThrowWhenArchivedPost() {
        post.archive();
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));

        assertThrows(DomainException.class, () ->
            service.addTopic(postSlug, "Título", "Conteúdo suficiente aqui", null, AUTHOR_ID));
    }

    @Test
    void addTopic_shouldThrowWhenContentTooShort() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.countByPostId(post.getId())).thenReturn(0);

        assertThrows(DomainValidationException.class, () ->
            service.addTopic(postSlug, "Título Válido", "Curto", null, AUTHOR_ID));
    }
}
