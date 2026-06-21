package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.CategoryRepository;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.shared.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {
    
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public PostService(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Cria um novo post em status DRAFT
     */
    @Transactional
    public Post createDraft(String title, String description, Id<Author> authorId, 
                           Id<com.otavio.blog.domain.category.Category> categoryId, 
                           List<String> tags) {
        
        log.debug("Creating draft post with title: {}", title);
        
        // Validar se categoria existe
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", 
                "Category not found with ID: " + categoryId);
        }

        // Criar post
        Post post = Post.create(title, description, authorId, categoryId, tags);

        // Validar post
        Notification notification = Notification.create();
        post.validate(notification);
        notification.throwIfHasErrors();

        // Gerar slug único
        Slug uniqueSlug = generateUniqueSlug(post.getSlug());
        if (!uniqueSlug.equals(post.getSlug())) {
            post.updateSlug(uniqueSlug);
        }

        // Salvar
        Post saved = postRepository.save(post);
        
        log.info("Post created successfully: id={}, slug={}", saved.getId(), saved.getSlug());
        
        return saved;
    }

    /**
     * Gera um slug único, adicionando sufixo se necessário
     */
    private Slug generateUniqueSlug(Slug baseSlug) {
        if (!postRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        // Slug já existe, encontrar próximo sufixo disponível
        int suffix = postRepository.findNextSlugSuffix(baseSlug.getValue());
        Slug uniqueSlug = baseSlug.withSuffix(suffix);
        
        log.debug("Slug {} already exists, using {}", baseSlug, uniqueSlug);
        
        return uniqueSlug;
    }

    /**
     * Busca post por slug
     */
    @Transactional(readOnly = true)
    public Post getBySlug(Slug slug, Id<Author> currentUserId) {
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", 
                "Post not found with slug: " + slug));

        // Se é draft, apenas o autor pode visualizar
        if (post.isDraft() && !post.isAuthor(currentUserId)) {
            throw new ForbiddenException("DRAFT_ACCESS_DENIED", 
                "You don't have permission to view this draft");
        }

        return post;
    }

    /**
     * Busca post por slug (público - apenas posts publicados)
     */
    @Transactional(readOnly = true)
    public Post getPublishedBySlug(Slug slug) {
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", 
                "Post not found with slug: " + slug));

        if (!post.isPublished()) {
            throw new NotFoundException("POST_NOT_FOUND", 
                "Post not found with slug: " + slug);
        }

        return post;
    }
}
