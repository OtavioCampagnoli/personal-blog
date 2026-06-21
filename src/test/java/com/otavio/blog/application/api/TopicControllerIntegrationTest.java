package com.otavio.blog.application.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TopicControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    static final String CAT_ID   = "11111111-1111-1111-1111-111111111111";
    static final String POST_SLUG = "post-com-topicos-de-teste";

    @BeforeEach
    void createPost() throws Exception {
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Post com Tópicos de Teste", "categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isCreated());
    }

    // ── POST /api/blog/posts/{slug}/topics ────────────────────────────────

    @Test
    void addTopic_shouldReturn201WithOrderAndLinks() throws Exception {
        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Introdução ao Spring",
                        "content": "## Introdução\\n\\nSpring Boot é um framework que facilita a criação de apps Java."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Introdução ao Spring"))
            .andExpect(jsonPath("$.order").value(1))
            .andExpect(jsonPath("$._links.self").exists())
            .andExpect(jsonPath("$._links.post").exists());
    }

    @Test
    void addTopic_shouldAutoIncrementOrder() throws Exception {
        addTopicRequest("Primeiro Tópico", "Conteúdo do primeiro tópico aqui");
        addTopicRequest("Segundo Tópico", "Conteúdo do segundo tópico aqui");

        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Terceiro Tópico", "content": "Conteúdo do terceiro tópico aqui"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order").value(3));
    }

    @Test
    void addTopic_shouldInsertAtSpecificOrder() throws Exception {
        addTopicRequest("Tópico Original Um", "Conteúdo suficiente do primeiro");
        addTopicRequest("Tópico Original Dois", "Conteúdo suficiente do segundo");

        // Insert at position 1 — should push existing topics forward
        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Novo Primeiro Tópico", "content": "Inserido no início aqui", "order": 1}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order").value(1));

        // Verify list is correctly ordered
        mockMvc.perform(get("/api/blog/posts/{slug}/topics", POST_SLUG))
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].order").value(1))
            .andExpect(jsonPath("$[1].order").value(2))
            .andExpect(jsonPath("$[2].order").value(3));
    }

    @Test
    void addTopic_shouldReturn400WhenTitleTooShort() throws Exception {
        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "AB", "content": "Conteúdo suficiente aqui sim"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void addTopic_shouldReturn400WhenContentTooShort() throws Exception {
        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Título Válido", "content": "Curto"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    void addTopic_shouldReturn404WhenPostNotFound() throws Exception {
        mockMvc.perform(post("/api/blog/posts/inexistente/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Título Qualquer", "content": "Conteúdo suficiente aqui"}
                    """))
            .andExpect(status().isNotFound());
    }

    // ── GET /api/blog/posts/{slug}/topics ─────────────────────────────────

    @Test
    void listTopics_shouldReturnEmptyWhenNoTopics() throws Exception {
        mockMvc.perform(get("/api/blog/posts/{slug}/topics", POST_SLUG))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listTopics_shouldReturnOrderedTopics() throws Exception {
        addTopicRequest("Tópico A", "Conteúdo do tópico A aqui");
        addTopicRequest("Tópico B", "Conteúdo do tópico B aqui");
        addTopicRequest("Tópico C", "Conteúdo do tópico C aqui");

        mockMvc.perform(get("/api/blog/posts/{slug}/topics", POST_SLUG))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].title").value("Tópico A"))
            .andExpect(jsonPath("$[1].title").value("Tópico B"))
            .andExpect(jsonPath("$[2].title").value("Tópico C"));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void addTopicRequest(String title, String content) throws Exception {
        mockMvc.perform(post("/api/blog/posts/{slug}/topics", POST_SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "%s", "content": "%s"}
                    """.formatted(title, content)))
            .andExpect(status().isCreated());
    }
}
