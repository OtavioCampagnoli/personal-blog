package com.otavio.blog.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.topic.TopicImage;

import java.time.Instant;
import java.util.Map;

public record TopicImageResponse(
    String  id,
    String  topicId,
    int     position,
    String  caption,
    Instant addedAt,
    ImageResponse image,

    @JsonProperty("_links")
    Map<String, PostResponse.Link> links
) {
    public static TopicImageResponse from(TopicImage ti, Image image,
                                          String postSlug, String baseUrl) {
        String self = baseUrl + "/api/blog/posts/" + postSlug
            + "/topics/" + ti.getTopicId().asString()
            + "/images/" + ti.getId().asString();

        return new TopicImageResponse(
            ti.getId().asString(),
            ti.getTopicId().asString(),
            ti.getPosition(),
            ti.getCaption(),
            ti.getAddedAt(),
            ImageResponse.from(image, baseUrl),
            Map.of(
                "self",   new PostResponse.Link(self),
                "delete", new PostResponse.Link(self),
                "topic",  new PostResponse.Link(baseUrl + "/api/blog/posts/" + postSlug
                    + "/topics/" + ti.getTopicId().asString()),
                "image",  new PostResponse.Link(baseUrl + "/api/blog/images/"
                    + image.getId().asString())
            )
        );
    }
}
