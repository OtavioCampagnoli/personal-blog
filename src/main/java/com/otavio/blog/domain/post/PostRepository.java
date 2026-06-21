package com.otavio.blog.domain.post;

import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Slug;

import java.util.Optional;

/**
 * Repository interface para Post
 */
public interface PostRepository {
    
    /**
     * Salva um post (insert ou update)
     */
    Post save(Post post);

    /**
     * Busca post por ID
     */
    Optional<Post> findById(Id<Post> id);

    /**
     * Busca post por slug (ignora soft deleted)
     */
    Optional<Post> findBySlug(Slug slug);

    /**
     * Verifica se existe post com o slug (ignora soft deleted)
     */
    boolean existsBySlug(Slug slug);

    /**
     * Busca próximo sufixo disponível para slug
     */
    int findNextSlugSuffix(String baseSlug);

    /**
     * Deleta post (soft delete)
     */
    void delete(Id<Post> id);
}
