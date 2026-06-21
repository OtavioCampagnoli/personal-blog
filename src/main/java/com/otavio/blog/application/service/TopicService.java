package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicRepository;
import com.otavio.blog.shared.domain.DomainException;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.NotFoundException;
import com.otavio.blog.shared.domain.Notification;
import com.otavio.blog.shared.domain.Slug;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);
    private static final int MAX_TOPICS_PER_POST = 50;

    private final TopicRepository topicRepository;
    private final PostRepository postRepository;

    public TopicService(TopicRepository topicRepository, PostRepository postRepository) {
        this.topicRepository = topicRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Topic addTopic(Slug postSlug, String title, String content,
                          Integer requestedOrder, Id<Author> authorId) {

        Post post = postRepository.findBySlug(postSlug)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND",
                "Post not found with slug: " + postSlug));

        post.ensureAuthor(authorId);
        post.ensureCanBeEdited();

        int currentCount = topicRepository.countByPostId(post.getId());

        if (currentCount >= MAX_TOPICS_PER_POST) {
            throw new DomainException("MAX_TOPICS_EXCEEDED",
                "Maximum " + MAX_TOPICS_PER_POST + " topics per post allowed");
        }

        int targetOrder;
        if (requestedOrder == null) {
            targetOrder = currentCount + 1;
        } else {
            targetOrder = Math.min(requestedOrder, currentCount + 1);
            targetOrder = Math.max(targetOrder, 1);

            if (targetOrder <= currentCount) {
                topicRepository.incrementOrdersFrom(post.getId(), targetOrder);
            }
        }

        Topic topic = Topic.create(post.getId(), title, content, targetOrder);

        Notification notification = Notification.create();
        topic.validate(notification);
        notification.throwIfHasErrors();

        Topic saved = topicRepository.save(topic);

        log.info("Topic added: id={}, postSlug={}, order={}", saved.getId(), postSlug, targetOrder);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Topic> listByPost(Slug postSlug) {
        Post post = postRepository.findBySlug(postSlug)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND",
                "Post not found with slug: " + postSlug));

        return topicRepository.findByPostIdOrderByOrderNumber(post.getId());
    }
}
