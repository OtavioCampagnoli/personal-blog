package com.otavio.blog.application.service;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.category.CategoryRepository;
import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.post.PostRepository;
import com.otavio.blog.domain.post.PostStatus;
import com.otavio.blog.shared.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository     postRepository;
    @Mock CategoryRepository categoryRepository;

    PostService service;

    static final Id<Author> AUTHOR_ID   = Id.of("00000000-0000-0000-0000-000000000001");
    static final Id<com.otavio.blog.domain.category.Category> CAT_ID =
        Id.of("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        service = new PostService(postRepository, categoryRepository);
    }

    // ── createDraft ───────────────────────────────────────────────────────────

    @Test
    void createDraft_shouldSaveAndReturnPost() {
        when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
        when(postRepository.existsBySlug(any())).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Post result = service.createDraft(
            "Título Válido Para Post", "descrição", AUTHOR_ID, CAT_ID, List.of("java"));

        assertEquals("Título Válido Para Post", result.getTitle());
        assertEquals(PostStatus.DRAFT, result.getStatus());
        assertEquals("titulo-valido-para-post", result.getSlug().getValue());
        verify(postRepository).save(any());
    }

    @Test
    void createDraft_shouldGenerateUniqueSlugWithSuffix() {
        when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
        when(postRepository.existsBySlug(Slug.from("Meu Post"))).thenReturn(true);
        when(postRepository.findNextSlugSuffix("meu-post")).thenReturn(2);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Post result = service.createDraft("Meu Post", null, AUTHOR_ID, CAT_ID, null);

        assertEquals("meu-post-2", result.getSlug().getValue());
    }

    @Test
    void createDraft_shouldThrowWhenCategoryNotFound() {
        when(categoryRepository.existsById(CAT_ID)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
            service.createDraft("Título Válido Aqui", null, AUTHOR_ID, CAT_ID, null));

        assertEquals("CATEGORY_NOT_FOUND", ex.getCode());
    }

    @Test
    void createDraft_shouldThrowWhenTitleTooShort() {
        when(categoryRepository.existsById(CAT_ID)).thenReturn(true);

        assertThrows(DomainValidationException.class, () ->
            service.createDraft("Curto", null, AUTHOR_ID, CAT_ID, null));
    }

    @Test
    void createDraft_shouldNormalizeTags() {
        when(categoryRepository.existsById(CAT_ID)).thenReturn(true);
        when(postRepository.existsBySlug(any())).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Post result = service.createDraft("Título Válido Para Post", null, AUTHOR_ID, CAT_ID,
            List.of("  JAVA  ", "Java", "spring"));

        assertEquals(2, result.getTags().size()); // Java deduplicated
        assertTrue(result.getTags().contains("java"));
        assertTrue(result.getTags().contains("spring"));
    }

    // ── getBySlug ─────────────────────────────────────────────────────────────

    @Test
    void getBySlug_shouldReturnPublishedPost() {
        Post post = Post.create("Título Válido Para Post", null, AUTHOR_ID, CAT_ID, null);
        post.publish();

        when(postRepository.findBySlug(Slug.from("Título Válido Para Post")))
            .thenReturn(Optional.of(post));

        Post result = service.getBySlug(Slug.from("Título Válido Para Post"), Id.generate());
        assertTrue(result.isPublished());
    }

    @Test
    void getBySlug_shouldReturnDraftForAuthor() {
        Post post = Post.create("Título Válido Para Post", null, AUTHOR_ID, CAT_ID, null);

        when(postRepository.findBySlug(any())).thenReturn(Optional.of(post));

        Post result = service.getBySlug(Slug.from("titulo-valido-para-post"), AUTHOR_ID);
        assertTrue(result.isDraft());
    }

    @Test
    void getBySlug_shouldForbidDraftForOtherUser() {
        Post post = Post.create("Título Válido Para Post", null, AUTHOR_ID, CAT_ID, null);

        when(postRepository.findBySlug(any())).thenReturn(Optional.of(post));

        assertThrows(ForbiddenException.class, () ->
            service.getBySlug(Slug.from("titulo-valido-para-post"), Id.generate()));
    }

    @Test
    void getBySlug_shouldThrowWhenNotFound() {
        when(postRepository.findBySlug(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            service.getBySlug(Slug.of("nao-existe"), AUTHOR_ID));
    }
}
