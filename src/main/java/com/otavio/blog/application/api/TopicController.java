package com.otavio.blog.application.api;

import com.otavio.blog.application.api.dto.AddImageToTopicRequest;
import com.otavio.blog.application.api.dto.AddTopicRequest;
import com.otavio.blog.application.api.dto.TopicImageResponse;
import com.otavio.blog.application.api.dto.TopicResponse;
import com.otavio.blog.application.service.TopicImageService;
import com.otavio.blog.application.service.TopicService;
import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicImage;
import com.otavio.blog.domain.image.ImageRepository;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.NotFoundException;
import com.otavio.blog.shared.domain.Slug;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/blog/posts/{slug}/topics")
public class TopicController {

    private static final Logger log = LoggerFactory.getLogger(TopicController.class);

    private final TopicService      topicService;
    private final TopicImageService topicImageService;
    private final ImageRepository   imageRepository;

    public TopicController(TopicService topicService,
                           TopicImageService topicImageService,
                           ImageRepository imageRepository) {
        this.topicService      = topicService;
        this.topicImageService = topicImageService;
        this.imageRepository   = imageRepository;
    }

    /** POST /api/blog/posts/{slug}/topics — Adicionar tópico */
    @PostMapping
    public ResponseEntity<TopicResponse> addTopic(
            @PathVariable String slug,
            @Valid @RequestBody AddTopicRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/blog/posts/{}/topics - title: {}", slug, request.title());

        Id<Author> authorId = Id.of("00000000-0000-0000-0000-000000000001");

        Topic topic = topicService.addTopic(
            Slug.of(slug), request.title(), request.content(), request.order(), authorId);

        String baseUrl = getBaseUrl(httpRequest);
        URI location = URI.create(baseUrl + "/api/blog/posts/" + slug + "/topics/" + topic.getId().asString());

        return ResponseEntity.created(location).body(TopicResponse.from(topic, slug, baseUrl));
    }

    /** GET /api/blog/posts/{slug}/topics — Listar tópicos */
    @GetMapping
    public ResponseEntity<List<TopicResponse>> listTopics(
            @PathVariable String slug,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        List<TopicResponse> topics = topicService.listByPost(Slug.of(slug))
            .stream().map(t -> TopicResponse.from(t, slug, baseUrl)).toList();

        return ResponseEntity.ok(topics);
    }

    /**
     * POST /api/blog/posts/{slug}/topics/{topicId}/images — Task 05: Associar imagem
     */
    @PostMapping("/{topicId}/images")
    public ResponseEntity<TopicImageResponse> addImageToTopic(
            @PathVariable String slug,
            @PathVariable String topicId,
            @Valid @RequestBody AddImageToTopicRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/blog/posts/{}/topics/{}/images - imageId: {}",
            slug, topicId, request.imageId());

        Id<Author> authorId = Id.of("00000000-0000-0000-0000-000000000001");

        TopicImage topicImage = topicImageService.addImage(
            Slug.of(slug),
            Id.of(topicId),
            Id.of(request.imageId()),
            request.position(),
            request.caption(),
            authorId
        );

        Image image = imageRepository.findById(topicImage.getImageId())
            .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND", "Image not found"));

        String baseUrl = getBaseUrl(httpRequest);
        TopicImageResponse response = TopicImageResponse.from(topicImage, image, slug, baseUrl);

        URI location = URI.create(baseUrl + "/api/blog/posts/" + slug
            + "/topics/" + topicId + "/images/" + topicImage.getId().asString());

        return ResponseEntity.created(location).body(response);
    }

    /** GET /api/blog/posts/{slug}/topics/{topicId}/images — Listar imagens do tópico */
    @GetMapping("/{topicId}/images")
    public ResponseEntity<List<TopicImageResponse>> listImages(
            @PathVariable String slug,
            @PathVariable String topicId,
            HttpServletRequest httpRequest) {

        String baseUrl = getBaseUrl(httpRequest);
        List<TopicImageResponse> images = topicImageService.listByTopic(Id.of(topicId))
            .stream()
            .map(ti -> {
                Image image = imageRepository.findById(ti.getImageId())
                    .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND", "Image not found"));
                return TopicImageResponse.from(ti, image, slug, baseUrl);
            })
            .toList();

        return ResponseEntity.ok(images);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        String suffix = ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443))
            ? ":" + port : "";
        return scheme + "://" + host + suffix;
    }
}
