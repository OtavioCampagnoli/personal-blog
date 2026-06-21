package com.otavio.blog.domain.image;

import com.otavio.blog.shared.domain.Id;

import java.util.Optional;

public interface ImageRepository {
    Image save(Image image);
    Optional<Image> findById(Id<Image> id);
    void delete(Id<Image> id);
}
