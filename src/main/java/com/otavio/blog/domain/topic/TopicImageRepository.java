package com.otavio.blog.domain.topic;

import com.otavio.blog.domain.image.Image;
import com.otavio.blog.shared.domain.Id;

import java.util.List;

public interface TopicImageRepository {
    TopicImage save(TopicImage topicImage);
    List<TopicImage> findByTopicIdOrderByPosition(Id<Topic> topicId);
    int countByTopicId(Id<Topic> topicId);
    boolean existsByTopicIdAndImageId(Id<Topic> topicId, Id<Image> imageId);
    void incrementPositionsFrom(Id<Topic> topicId, int fromPosition);
    void delete(Id<TopicImage> id);
}
