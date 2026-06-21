package com.otavio.blog.domain.author;

import com.otavio.blog.shared.domain.BaseEntity;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;

/**
 * Entidade Author
 */
public class Author extends BaseEntity<Author> {
    
    private String name;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String bio;

    public Author() {
        super();
    }

    public Author(Id<Author> id) {
        super(id);
    }

    public static Author create(String name, String email, String passwordHash) {
        Author author = new Author();
        author.name = name;
        author.email = email;
        author.passwordHash = passwordHash;
        return author;
    }

    @Override
    public void validate(Notification notification) {
        if (name == null || name.isBlank()) {
            notification.addError("name", "NAME_REQUIRED", "Name is required");
        }
        if (email == null || email.isBlank()) {
            notification.addError("email", "EMAIL_REQUIRED", "Email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            notification.addError("passwordHash", "PASSWORD_REQUIRED", "Password is required");
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
