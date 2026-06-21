package com.otavio.blog.domain.category;

import com.otavio.blog.shared.domain.BaseEntity;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;
import com.otavio.blog.shared.domain.Slug;

/**
 * Entidade Category
 */
public class Category extends BaseEntity<Category> {
    
    private String name;
    private Slug slug;
    private String description;

    public Category() {
        super();
    }

    public Category(Id<Category> id) {
        super(id);
    }

    public static Category create(String name, String description) {
        Category category = new Category();
        category.name = name;
        category.slug = Slug.from(name);
        category.description = description;
        return category;
    }

    @Override
    public void validate(Notification notification) {
        if (name == null || name.isBlank()) {
            notification.addError("name", "NAME_REQUIRED", "Name is required");
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Slug getSlug() {
        return slug;
    }

    public void setSlug(Slug slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
