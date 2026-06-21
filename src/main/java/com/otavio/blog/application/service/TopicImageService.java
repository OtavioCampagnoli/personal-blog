package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.image.ImageRepository;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicImage;
import com.otavio.blog.domain.topic.TopicImageRepository;
import com.otavio.blog.domain.topic.TopicRepository;
import com.otavio.blog.shared.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicImageService {

    private static final Logger log = LoggerFactory.getLogger(TopicImageService.class);
    private static final int MAX_IMAGES_PER_TOPIC = 20;

    private final PostRepository       postRepository;
    private final TopicRepository      topicRepository;
    private final ImageRepository      imageRepository;
    private final TopicImageRepository topicImageRepository;

    public TopicImageService(PostRepository postRepository,
                             TopicRepository topicRepository,
                             ImageRepository imageRepository,
                             TopicImageRepository topicImageRepository) {
        this.postRepository       = postRepository;
        this.topicRepository      = topicRepository;
        this.imageRepository      = imageRepository;
        this.topicImageRepository = topicImageRepository;
    }

    @Transactional
    public TopicImage addImage(Slug postSlug, Id<Topic> topicId,
                               Id<Image> imageId, Integer requestedPosition,
                               String caption, Id<Author> authorId) {

        // Validar post e autorização
        Post post = postRepository.findBySlug(postSlug)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND",
                "Post not found with slug: " + postSlug));
        post.ensureAuthor(authorId);

        // Validar tópico pertence ao post
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND",
                "Topic not found with id: " + topicId));
        if (!topic.belongsToPost(post.getId())) {
            throw new DomainException("TOPIC_NOT_IN_POST",
                "Topic does not belong to the specified post");
        }

        // Validar imagem existe e pertence ao autor
        Image image = imageRepository.findById(imageId)
            .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND",
                "Image not found with id: " + imageId));
        if (!image.belongsToAuthor(authorId)) {
            throw new ForbiddenException("IMAGE_OWNERSHIP_VIOLATION",
                "You cannot use images from other authors");
        }

        // Verificar duplicação
        if (topicImageRepository.existsByTopicIdAndImageId(topicId, imageId)) {
            throw new DomainException("IMAGE_ALREADY_IN_TOPIC",
                "This image is already associated with this topic");
        }

        // Verificar limite
        int currentCount = topicImageRepository.countByTopicId(topicId);
        if (currentCount >= MAX_IMAGES_PER_TOPIC) {
            throw new DomainException("MAX_IMAGES_EXCEEDED",
                "Maximum " + MAX_IMAGES_PER_TOPIC + " images per topic allowed");
        }

        // Calcular posição
        int targetPosition;
        if (requestedPosition == null) {
            targetPosition = currentCount + 1;
        } else {
            targetPosition = Math.min(Math.max(requestedPosition, 1), currentCount + 1);
            if (targetPosition <= currentCount) {
                topicImageRepository.incrementPositionsFrom(topicId, targetPosition);
            }
        }

        TopicImage topicImage = TopicImage.create(topicId, imageId, targetPosition, caption);

        Notification notification = Notification.create();
        topicImage.validate(notification);
        notification.throwIfHasErrors();

        TopicImage saved = topicImageRepository.save(topicImage);

        log.info("Image added to topic: topicId={}, imageId={}, position={}",
            topicId, imageId, targetPosition);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<TopicImage> listByTopic(Id<Topic> topicId) {
        return topicImageRepository.findByTopicIdOrderByPosition(topicId);
    }
}
