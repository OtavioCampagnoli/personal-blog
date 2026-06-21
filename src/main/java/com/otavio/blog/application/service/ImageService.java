package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.image.ImageRepository;
import com.otavio.blog.infrastructure.storage.StorageService;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    // Magic bytes para validar o tipo real do arquivo
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", "jpg",
        "image/png",  "png",
        "image/gif",  "gif",
        "image/webp", "webp"
    );

    private final ImageRepository    imageRepository;
    private final StorageService     storageService;

    public ImageService(ImageRepository imageRepository, StorageService storageService) {
        this.imageRepository = imageRepository;
        this.storageService  = storageService;
    }

    @Transactional
    public Image upload(MultipartFile file, String altText, Id<Author> authorId) {
        // 1. Validar tipo via magic bytes
        String contentType = resolveContentType(file);

        // 2. Ler metadados da imagem (dimensões)
        BufferedImage buffered = readImage(file);

        // 3. Montar entidade e validar regras de domínio
        String ext = EXTENSIONS.getOrDefault(contentType, "bin");
        String key = "images/" + authorId.asString() + "/" + UUID.randomUUID() + "." + ext;

        Image image = Image.create(
            authorId,
            key,
            storageService.resolveUrl(key),
            altText,
            contentType,
            file.getSize(),
            buffered.getWidth(),
            buffered.getHeight()
        );

        Notification notification = Notification.create();
        image.validate(notification);
        notification.throwIfHasErrors();

        // 4. Fazer upload para o storage
        storageService.upload(file, key);

        // 5. Persistir metadados
        Image saved = imageRepository.save(image);

        log.info("Image uploaded: id={}, key={}, size={}b, {}x{}",
            saved.getId(), key, file.getSize(), buffered.getWidth(), buffered.getHeight());

        return saved;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolveContentType(MultipartFile file) {
        String declared = file.getContentType();

        try {
            byte[] header = file.getBytes();
            for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
                byte[] magic = entry.getValue();
                if (header.length >= magic.length) {
                    boolean match = true;
                    for (int i = 0; i < magic.length; i++) {
                        if (header[i] != magic[i]) { match = false; break; }
                    }
                    if (match) return entry.getKey();
                }
            }
        } catch (IOException e) {
            log.warn("Could not read file bytes for magic check", e);
        }

        // Se não bateu magic bytes, rejeitar
        throw new com.otavio.blog.shared.domain.DomainValidationException(
            buildUnsupportedTypeNotification(declared));
    }

    private com.otavio.blog.shared.domain.Notification buildUnsupportedTypeNotification(String provided) {
        Notification n = Notification.create();
        n.addError("contentType", "UNSUPPORTED_FILE_TYPE",
            "Unsupported file type. Use: JPEG, PNG, WebP or GIF",
            provided, "image/jpeg");
        return n;
    }

    private BufferedImage readImage(MultipartFile file) {
        try {
            BufferedImage img = ImageIO.read(file.getInputStream());
            if (img == null) {
                Notification n = Notification.create();
                n.addError("file", "INVALID_IMAGE", "Cannot read image data");
                throw new com.otavio.blog.shared.domain.DomainValidationException(n);
            }
            return img;
        } catch (IOException e) {
            Notification n = Notification.create();
            n.addError("file", "IMAGE_READ_ERROR", "Error reading image file");
            throw new com.otavio.blog.shared.domain.DomainValidationException(n);
        }
    }
}
