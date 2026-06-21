package com.otavio.blog.domain.image;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.shared.domain.BaseEntity;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;

import java.time.Instant;
import java.util.Set;

public class Image extends BaseEntity<Image> {

    private static final Set<String> ALLOWED_TYPES =
        Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int MIN_DIMENSION   = 100;
    private static final int MAX_DIMENSION   = 4000;

    private Id<Author> authorId;
    private String s3Key;
    private String url;
    private String altText;
    private String contentType;
    private long   sizeBytes;
    private int    width;
    private int    height;
    private Instant uploadedAt;

    public Image() { super(); }
    public Image(Id<Image> id) { super(id); }

    public static Image create(Id<Author> authorId, String s3Key, String url,
                               String altText, String contentType,
                               long sizeBytes, int width, int height) {
        Image img = new Image();
        img.authorId    = authorId;
        img.s3Key       = s3Key;
        img.url         = url;
        img.altText     = altText;
        img.contentType = contentType;
        img.sizeBytes   = sizeBytes;
        img.width       = width;
        img.height      = height;
        img.uploadedAt  = Instant.now();
        return img;
    }

    @Override
    public void validate(Notification notification) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            notification.addError("contentType", "UNSUPPORTED_FILE_TYPE",
                "Unsupported file type. Use: JPEG, PNG, WebP or GIF",
                contentType, "image/jpeg");
        }
        if (sizeBytes > MAX_SIZE_BYTES) {
            notification.addError("sizeBytes", "IMAGE_TOO_LARGE",
                String.format("Max allowed size: 5MB (provided: %.1fMB)", sizeBytes / 1_048_576.0));
        }
        if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
            notification.addError("dimensions", "DIMENSIONS_TOO_SMALL",
                String.format("Minimum dimensions: %dx%dpx (provided: %dx%dpx)",
                    MIN_DIMENSION, MIN_DIMENSION, width, height));
        }
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            notification.addError("dimensions", "DIMENSIONS_TOO_LARGE",
                String.format("Maximum dimensions: %dx%dpx (provided: %dx%dpx)",
                    MAX_DIMENSION, MAX_DIMENSION, width, height));
        }
        if (altText != null && altText.length() > 200) {
            notification.addError("altText", "ALT_TEXT_TOO_LONG",
                "Alt text cannot exceed 200 characters");
        }
    }

    public boolean belongsToAuthor(Id<Author> userId) {
        return this.authorId.equals(userId);
    }

    // Getters
    public Id<Author> getAuthorId()  { return authorId; }
    public String     getS3Key()     { return s3Key; }
    public String     getUrl()       { return url; }
    public String     getAltText()   { return altText; }
    public String     getContentType() { return contentType; }
    public long       getSizeBytes() { return sizeBytes; }
    public int        getWidth()     { return width; }
    public int        getHeight()    { return height; }
    public Instant    getUploadedAt(){ return uploadedAt; }

    // Setters para reconstrução do banco
    public void setAuthorId(Id<Author> authorId)   { this.authorId = authorId; }
    public void setS3Key(String s3Key)             { this.s3Key = s3Key; }
    public void setUrl(String url)                 { this.url = url; }
    public void setAltText(String altText)         { this.altText = altText; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setSizeBytes(long sizeBytes)       { this.sizeBytes = sizeBytes; }
    public void setWidth(int width)                { this.width = width; }
    public void setHeight(int height)              { this.height = height; }
    public void setUploadedAt(Instant uploadedAt)  { this.uploadedAt = uploadedAt; }
}
