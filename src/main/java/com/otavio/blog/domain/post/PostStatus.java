package com.otavio.blog.domain.post;

/**
 * Status possíveis de um post
 */
public enum PostStatus {
    DRAFT,      // Rascunho - visível apenas para o autor
    PUBLISHED,  // Publicado - visível para todos
    ARCHIVED    // Arquivado - não pode ser editado
}
