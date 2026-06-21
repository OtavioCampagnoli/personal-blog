package com.otavio.blog.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.otavio.blog.domain.topic.Topic;

import java.time.Instant;
import java.util.Map;

public record TopicResponse(
    String id,
    String postId,
    String title,
    String content,
    String contentPreview,
    int order,
    Instant createdAt,
    Instant updatedAt,

    @JsonProperty("_links")
    Map<String, PostResponse.Link> links
) {

    public static TopicResponse from(Topic topic, String postSlug, String baseUrl) {
        String topicUrl = baseUrl + "/api/blog/posts/" + postSlug + "/topics/" + topic.getId().asString();
        String postUrl  = baseUrl + "/api/blog/posts/" + postSlug;

        return new TopicResponse(
            topic.getId().asString(),
            topic.getPostId().asString(),
            topic.getTitle(),
            topic.getContent(),
            preview(topic.getContent()),
            topic.getOrderNumber(),
            topic.getCreatedAt(),
            topic.getUpdatedAt(),
            Map.of(
                "self",    new PostResponse.Link(topicUrl),
                "post",    new PostResponse.Link(postUrl),
                "edit",    new PostResponse.Link(topicUrl),
                "delete",  new PostResponse.Link(topicUrl),
                "reorder", new PostResponse.Link(postUrl + "/topics/reorder")
            )
        );
    }

    private static String preview(String content) {
        if (content == null) return "";
        // Strip basic markdown symbols and truncate
        String plain = content
            .replaceAll("```[\\s\\S]*?```", "")
            .replaceAll("`[^`]+`", "")
            .replaceAll("#{1,6}\\s*", "")
            .replaceAll("[*_]{1,2}", "")
            .replaceAll("\\n+", " ")
            .trim();
        return plain.length() > 150 ? plain.substring(0, 150) + "..." : plain;
    }
}
