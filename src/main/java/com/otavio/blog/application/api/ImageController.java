package com.otavio.blog.application.api;

import com.otavio.blog.application.api.dto.ImageResponse;
import com.otavio.blog.application.service.ImageService;
import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.shared.domain.Id;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

/**
 * Task 04: Upload de imagem
 * POST /api/blog/images
 */
@RestController
@RequestMapping("/api/blog/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "altText", required = false) String altText,
            HttpServletRequest httpRequest) {

        log.info("POST /api/blog/images - file: {}, size: {}b",
            file.getOriginalFilename(), file.getSize());

        // TODO: pegar do JWT
        Id<Author> authorId = Id.of("00000000-0000-0000-0000-000000000001");

        Image image = imageService.upload(file, altText, authorId);

        String baseUrl = getBaseUrl(httpRequest);
        ImageResponse response = ImageResponse.from(image, baseUrl);

        URI location = URI.create(baseUrl + "/api/blog/images/" + image.getId().asString());

        return ResponseEntity.created(location).body(response);
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
