package com.otavio.blog.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.topic.Topic;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PostResponse(
    String id,
    String title,
    String slug,
    String description,
    String status,
    String categoryId,
    List<String> tags,
    int topicCount,
    List<TopicResponse> topics,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt,

    @JsonProperty("_links")
    Map<String, Link> links
) {

    public static PostResponse from(Post post, List<Topic> topics, String baseUrl) {
        String postUrl = baseUrl + "/api/blog/posts/" + post.getSlug().getValue();

        List<TopicResponse> topicResponses = topics.stream()
            .map(t -> TopicResponse.from(t, post.getSlug().getValue(), baseUrl))
            .toList();

        return new PostResponse(
            post.getId().asString(),
            post.getTitle(),
            post.getSlug().getValue(),
            post.getDescription(),
            post.getStatus().name(),
            post.getCategoryId().asString(),
            post.getTags(),
            topics.size(),
            topicResponses,
            post.getCreatedAt(),
            post.getUpdatedAt(),
            post.getPublishedAt(),
            Map.of(
                "self",      new Link(postUrl),
                "add-topic", new Link(postUrl + "/topics"),
                "publish",   new Link(postUrl + "/publish"),
                "edit",      new Link(postUrl),
                "delete",    new Link(postUrl),
                "category",  new Link(baseUrl + "/api/blog/categories/" + post.getCategoryId().asString())
            )
        );
    }

    public record Link(String href) {}
}
