package com.otavio.blog.domain.topic;

import com.otavio.blog.domain.post.Post;
import com.otavio.blog.shared.domain.BaseEntity;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;

/**
 * Entidade Topic - representa uma seção de um post
 */
public class Topic extends BaseEntity<Topic> {

    private Id<Post> postId;
    private String title;
    private String content; // Markdown
    private int orderNumber;

    public Topic() {
        super();
    }

    public Topic(Id<Topic> id) {
        super(id);
    }

    public static Topic create(Id<Post> postId, String title, String content, int orderNumber) {
        Topic topic = new Topic();
        topic.postId = postId;
        topic.title = title;
        topic.content = content;
        topic.orderNumber = orderNumber;
        return topic;
    }

    @Override
    public void validate(Notification notification) {
        if (title == null || title.isBlank()) {
            notification.addError("title", "TITLE_REQUIRED", "Title is required");
        } else if (title.length() < 3) {
            notification.addError("title", "TITLE_TOO_SHORT",
                String.format("Title must be at least 3 characters (provided: %d)", title.length()),
                title, "Introdução ao Spring Boot");
        } else if (title.length() > 100) {
            notification.addError("title", "TITLE_TOO_LONG",
                String.format("Title must not exceed 100 characters (provided: %d)", title.length()));
        }

        if (content == null || content.isBlank()) {
            notification.addError("content", "CONTENT_REQUIRED", "Content is required");
        } else if (content.length() < 10) {
            notification.addError("content", "CONTENT_TOO_SHORT",
                String.format("Content must be at least 10 characters (provided: %d)", content.length()),
                content, "## Introdução\n\nNeste tópico vamos ver...");
        }

        if (orderNumber < 1) {
            notification.addError("order", "ORDER_INVALID", "Order must be >= 1");
        }
    }

    public boolean belongsToPost(Id<Post> postId) {
        return this.postId.equals(postId);
    }

    // Getters
    public Id<Post> getPostId() { return postId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getOrderNumber() { return orderNumber; }

    // Setters para reconstrução do banco
    public void setPostId(Id<Post> postId) { this.postId = postId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }
}
