package com.otavio.blog.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.otavio.blog.domain.image.Image;

import java.time.Instant;
import java.util.Map;

public record ImageResponse(
    String id,
    String url,
    String s3Key,
    String altText,
    String contentType,
    long   sizeBytes,
    int    width,
    int    height,
    String authorId,
    Instant uploadedAt,

    @JsonProperty("_links")
    Map<String, PostResponse.Link> links
) {
    public static ImageResponse from(Image image, String baseUrl) {
        String self = baseUrl + "/api/blog/images/" + image.getId().asString();
        return new ImageResponse(
            image.getId().asString(),
            image.getUrl(),
            image.getS3Key(),
            image.getAltText(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getWidth(),
            image.getHeight(),
            image.getAuthorId().asString(),
            image.getUploadedAt(),
            Map.of(
                "self",   new PostResponse.Link(self),
                "delete", new PostResponse.Link(self)
            )
        );
    }
}
