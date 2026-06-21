package com.otavio.blog.application.api;

import com.otavio.blog.application.api.dto.AddTopicRequest;
import com.otavio.blog.application.api.dto.CreatePostRequest;
import com.otavio.blog.application.api.dto.PostResponse;
import com.otavio.blog.application.api.dto.TopicResponse;
import com.otavio.blog.application.service.PostService;
import com.otavio.blog.application.service.TopicService;
import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Slug;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/blog/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;
    private final TopicService topicService;

    public PostController(PostService postService, TopicService topicService) {
        this.postService = postService;
        this.topicService = topicService;
    }

    /**
     * POST /api/blog/posts — Criar novo post em draft
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/blog/posts - title: {}", request.title());

        Id<Author> authorId = getMockedAuthorId();
        Id<Category> categoryId = Id.of(request.categoryId());

        Post post = postService.createDraft(
            request.title(),
            request.description(),
            authorId,
            categoryId,
            request.tags() != null ? request.tags() : Collections.emptyList()
        );

        String baseUrl = getBaseUrl(httpRequest);
        PostResponse response = PostResponse.from(post, List.of(), baseUrl);

        URI location = URI.create(baseUrl + "/api/blog/posts/" + post.getSlug().getValue());

        log.info("Post created: id={}, slug={}", post.getId(), post.getSlug());

        return ResponseEntity.created(location).body(response);
    }

    /**
     * GET /api/blog/posts/{slug} — Buscar post com tópicos
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String slug,
            HttpServletRequest httpRequest) {

        log.info("GET /api/blog/posts/{}", slug);

        Id<Author> currentUserId = getMockedAuthorId();
        Post post = postService.getBySlug(Slug.of(slug), currentUserId);

        String baseUrl = getBaseUrl(httpRequest);
        List<Topic> topics = topicService.listByPost(Slug.of(slug));
        PostResponse response = PostResponse.from(post, topics, baseUrl);

        return ResponseEntity.ok(response);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        String suffix = ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443))
            ? ":" + port : "";
        return scheme + "://" + host + suffix;
    }

    private Id<Author> getMockedAuthorId() {
        return Id.of("00000000-0000-0000-0000-000000000001");
    }
}
