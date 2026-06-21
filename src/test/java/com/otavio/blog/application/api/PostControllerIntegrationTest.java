package com.otavio.blog.application.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PostController — uses real H2 DB + full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    static final String CAT_ID = "11111111-1111-1111-1111-111111111111";

    // ── POST /api/blog/posts ───────────────────────────────────────────────

    @Test
    void createPost_shouldReturn201WithSlugAndLinks() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Meu Post de Integração Aqui",
                        "description": "Descrição do post",
                        "categoryId": "%s",
                        "tags": ["java", "test"]
                    }
                    """.formatted(CAT_ID)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Meu Post de Integração Aqui"))
            .andExpect(jsonPath("$.slug").value("meu-post-de-integracao-aqui"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.tags").isArray())
            .andExpect(jsonPath("$.topicCount").value(0))
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.add-topic").exists())
            .andExpect(jsonPath("$._links.publish").exists());
    }

    @Test
    void createPost_shouldGenerateUniqueSuffixForDuplicateSlug() throws Exception {
        String body = """
            {"title": "Título Duplicado Para Teste", "categoryId": "%s"}
            """.formatted(CAT_ID);

        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("titulo-duplicado-para-teste"));

        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("titulo-duplicado-para-teste-2"));
    }

    @Test
    void createPost_shouldReturn400WhenTitleTooShort() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Curto", "categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERRORS"))
            .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void createPost_shouldReturn400WhenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERRORS"));
    }

    @Test
    void createPost_shouldReturn400WhenCategoryNotFound() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Título Válido Aqui Teste", "categoryId": "00000000-0000-0000-0000-000000000099"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void createPost_shouldReturn400WhenTooManyTags() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Título Válido Aqui Teste",
                        "categoryId": "%s",
                        "tags": ["t1","t2","t3","t4","t5","t6","t7","t8","t9","t10","t11"]
                    }
                    """.formatted(CAT_ID)))
            .andExpect(status().isBadRequest());
    }

    // ── GET /api/blog/posts/{slug} ─────────────────────────────────────────

    @Test
    void getPost_shouldReturn200WithTopics() throws Exception {
        // Create post first
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Post Para Buscar Aqui", "categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isCreated());

        // Fetch it
        mockMvc.perform(get("/api/blog/posts/post-para-buscar-aqui"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("post-para-buscar-aqui"))
            .andExpect(jsonPath("$.topics").isArray())
            .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void getPost_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/blog/posts/esse-post-nao-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }
}
