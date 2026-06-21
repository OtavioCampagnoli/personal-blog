package com.otavio.blog.domain.post;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.Category;
import com.otavio.blog.shared.domain.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raiz Post
 */
public class Post extends BaseEntity<Post> {
    
    private String title;
    private Slug slug;
    private String description;
    private PostStatus status;
    private Id<Author> authorId;
    private Id<Category> categoryId;
    private List<String> tags;
    private Instant publishedAt;

    // Constructor para criar novo post
    private Post() {
        super();
        this.status = PostStatus.DRAFT;
        this.tags = new ArrayList<>();
    }

    // Constructor para reconstruir do banco
    public Post(Id<Post> id) {
        super(id);
        this.tags = new ArrayList<>();
    }

    /**
     * Factory method para criar um novo post
     */
    public static Post create(String title, String description, Id<Author> authorId, Id<Category> categoryId, List<String> tags) {
        Post post = new Post();
        post.title = title;
        post.slug = Slug.from(title);
        post.description = description;
        post.authorId = authorId;
        post.categoryId = categoryId;
        post.tags = normalizeTags(tags);
        return post;
    }

    @Override
    public void validate(Notification notification) {
        // Title validation
        if (title == null || title.isBlank()) {
            notification.addError("title", "TITLE_REQUIRED", "Title is required");
        } else if (title.length() < 10) {
            notification.addError("title", "TITLE_TOO_SHORT", 
                String.format("Title must be at least 10 characters (provided: %d)", title.length()),
                title, "My First Blog Post About Java");
        } else if (title.length() > 200) {
            notification.addError("title", "TITLE_TOO_LONG", 
                String.format("Title must not exceed 200 characters (provided: %d)", title.length()),
                title, null);
        }

        // Description validation
        if (description != null && description.length() > 500) {
            notification.addError("description", "DESCRIPTION_TOO_LONG", 
                String.format("Description must not exceed 500 characters (provided: %d)", description.length()));
        }

        // Author validation
        if (authorId == null) {
            notification.addError("authorId", "AUTHOR_REQUIRED", "Author is required");
        }

        // Category validation
        if (categoryId == null) {
            notification.addError("categoryId", "CATEGORY_REQUIRED", "Category is required");
        }

        // Tags validation
        if (tags != null && tags.size() > 10) {
            notification.addError("tags", "TOO_MANY_TAGS", 
                String.format("Maximum 10 tags allowed (provided: %d)", tags.size()));
        }
    }

    /**
     * Normaliza tags: trim, lowercase, remove duplicatas
     */
    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    /**
     * Atualiza o slug quando o título muda
     */
    public void updateSlug(Slug newSlug) {
        this.slug = newSlug;
        markAsUpdated();
    }

    /**
     * Publica o post
     */
    public void publish() {
        if (status == PostStatus.ARCHIVED) {
            throw new DomainException("CANNOT_PUBLISH_ARCHIVED", "Cannot publish an archived post");
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        markAsUpdated();
    }

    /**
     * Despublica o post (volta para draft)
     */
    public void unpublish() {
        this.status = PostStatus.DRAFT;
        this.publishedAt = null;
        markAsUpdated();
    }

    /**
     * Arquiva o post
     */
    public void archive() {
        this.status = PostStatus.ARCHIVED;
        markAsUpdated();
    }

    /**
     * Verifica se o post pode ser editado
     */
    public void ensureCanBeEdited() {
        if (status == PostStatus.ARCHIVED) {
            throw new DomainException("CANNOT_EDIT_ARCHIVED", "Cannot edit an archived post");
        }
    }

    /**
     * Verifica se o usuário é o autor do post
     */
    public void ensureAuthor(Id<Author> userId) {
        if (!this.authorId.equals(userId)) {
            throw ForbiddenException.notAuthor();
        }
    }

    /**
     * Verifica se o post é um draft
     */
    public boolean isDraft() {
        return status == PostStatus.DRAFT;
    }

    /**
     * Verifica se o post está publicado
     */
    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    /**
     * Verifica se o post está arquivado
     */
    public boolean isArchived() {
        return status == PostStatus.ARCHIVED;
    }

    /**
     * Verifica se o usuário é o autor
     */
    public boolean isAuthor(Id<Author> userId) {
        return this.authorId.equals(userId);
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public Slug getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Id<Author> getAuthorId() {
        return authorId;
    }

    public Id<Category> getCategoryId() {
        return categoryId;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    // Setters para reconstrução do banco
    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(Slug slug) {
        this.slug = slug;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public void setAuthorId(Id<Author> authorId) {
        this.authorId = authorId;
    }

    public void setCategoryId(Id<Category> categoryId) {
        this.categoryId = categoryId;
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", slug=" + slug +
                ", status=" + status +
                ", authorId=" + authorId +
                ", createdAt=" + createdAt +
                '}';
    }
}
