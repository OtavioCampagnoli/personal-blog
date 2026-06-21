package com.otavio.blog.domain.category;

import com.otavio.blog.shared.domain.Id;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Id<Category> id);
    boolean existsById(Id<Category> id);
}
