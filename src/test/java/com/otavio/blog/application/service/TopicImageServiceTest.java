package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.image.ImageRepository;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicImage;
import com.otavio.blog.domain.topic.TopicImageRepository;
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
class TopicImageServiceTest {

    @Mock PostRepository       postRepository;
    @Mock TopicRepository      topicRepository;
    @Mock ImageRepository      imageRepository;
    @Mock TopicImageRepository topicImageRepository;

    TopicImageService service;

    static final Id<Author>   AUTHOR_ID = Id.of("00000000-0000-0000-0000-000000000001");
    static final Id<Category> CAT_ID    = Id.of("11111111-1111-1111-1111-111111111111");

    Post  post;
    Topic topic;
    Image image;
    Slug  postSlug;

    @BeforeEach
    void setUp() {
        service = new TopicImageService(postRepository, topicRepository,
            imageRepository, topicImageRepository);

        post     = Post.create("Título Válido Para Post", null, AUTHOR_ID, CAT_ID, null);
        postSlug = post.getSlug();
        topic    = Topic.create(post.getId(), "Tópico", "Conteúdo suficiente aqui", 1);
        image    = Image.create(AUTHOR_ID, "key", "url", "alt", "image/jpeg",
            1_000L, 800, 600);
    }

    @Test
    void addImage_shouldAddToEndByDefault() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        when(topicImageRepository.existsByTopicIdAndImageId(topic.getId(), image.getId()))
            .thenReturn(false);
        when(topicImageRepository.countByTopicId(topic.getId())).thenReturn(1);
        when(topicImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicImage result = service.addImage(
            postSlug, topic.getId(), image.getId(), null, "Caption", AUTHOR_ID);

        assertEquals(2, result.getPosition()); // count + 1
        assertEquals("Caption", result.getCaption());
        verify(topicImageRepository, never()).incrementPositionsFrom(any(), anyInt());
    }

    @Test
    void addImage_shouldReorderOnSpecificPosition() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        when(topicImageRepository.existsByTopicIdAndImageId(any(), any())).thenReturn(false);
        when(topicImageRepository.countByTopicId(topic.getId())).thenReturn(3);
        when(topicImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicImage result = service.addImage(
            postSlug, topic.getId(), image.getId(), 1, null, AUTHOR_ID);

        assertEquals(1, result.getPosition());
        verify(topicImageRepository).incrementPositionsFrom(topic.getId(), 1);
    }

    @Test
    void addImage_shouldThrowWhenImageAlreadyInTopic() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        when(topicImageRepository.existsByTopicIdAndImageId(topic.getId(), image.getId()))
            .thenReturn(true);

        DomainException ex = assertThrows(DomainException.class, () ->
            service.addImage(postSlug, topic.getId(), image.getId(), null, null, AUTHOR_ID));

        assertEquals("IMAGE_ALREADY_IN_TOPIC", ex.getCode());
    }

    @Test
    void addImage_shouldThrowWhenImageBelongsToAnotherAuthor() {
        Image otherImage = Image.create(Id.generate(), "k", "u", null, "image/jpeg",
            1_000L, 800, 600);

        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(imageRepository.findById(otherImage.getId())).thenReturn(Optional.of(otherImage));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
            service.addImage(postSlug, topic.getId(), otherImage.getId(), null, null, AUTHOR_ID));

        assertEquals("IMAGE_OWNERSHIP_VIOLATION", ex.getCode());
    }

    @Test
    void addImage_shouldThrowWhenTopicNotInPost() {
        Topic otherTopic = Topic.create(Id.generate(), "Outro", "Conteúdo suficiente", 1);

        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(otherTopic.getId())).thenReturn(Optional.of(otherTopic));

        DomainException ex = assertThrows(DomainException.class, () ->
            service.addImage(postSlug, otherTopic.getId(), image.getId(), null, null, AUTHOR_ID));

        assertEquals("TOPIC_NOT_IN_POST", ex.getCode());
    }

    @Test
    void addImage_shouldThrowWhenMaxImagesExceeded() {
        when(postRepository.findBySlug(postSlug)).thenReturn(Optional.of(post));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(imageRepository.findById(image.getId())).thenReturn(Optional.of(image));
        when(topicImageRepository.existsByTopicIdAndImageId(any(), any())).thenReturn(false);
        when(topicImageRepository.countByTopicId(topic.getId())).thenReturn(20);

        DomainException ex = assertThrows(DomainException.class, () ->
            service.addImage(postSlug, topic.getId(), image.getId(), null, null, AUTHOR_ID));

        assertEquals("MAX_IMAGES_EXCEEDED", ex.getCode());
    }
}
