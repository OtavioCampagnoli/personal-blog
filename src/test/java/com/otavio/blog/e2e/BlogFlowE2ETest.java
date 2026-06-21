package com.otavio.blog.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E: Cria post → adiciona tópicos → faz upload de imagem → associa imagem ao tópico
 * → verifica o post completo com todos os dados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BlogFlowE2ETest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    static final String CAT_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void fullFlow_createPostAddTopicsUploadImageAssociateToTopic() throws Exception {

        // ── STEP 1: Create post ────────────────────────────────────────────────
        MvcResult postResult = mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Guia Completo de Spring Boot E2E",
                        "description": "Post completo com tópicos e imagens",
                        "categoryId": "%s",
                        "tags": ["java", "spring-boot", "e2e"]
                    }
                    """.formatted(CAT_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("guia-completo-de-spring-boot-e2e"))
            .andExpect(jsonPath("$.topicCount").value(0))
            .andReturn();

        String postSlug = json(postResult).get("slug").asText();

        // ── STEP 2: Add topics ─────────────────────────────────────────────────
        MvcResult topic1Result = mockMvc.perform(post("/api/blog/posts/{s}/topics", postSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "O que é Spring Boot",
                        "content": "## Introdução\\n\\nSpring Boot é um framework Java que facilita o desenvolvimento."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order").value(1))
            .andReturn();

        String topic1Id = json(topic1Result).get("id").asText();

        mockMvc.perform(post("/api/blog/posts/{s}/topics", postSlug)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Configurando o Projeto",
                        "content": "## Setup\\n\\nPara criar o projeto acesse start.spring.io e configure."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.order").value(2));

        // ── STEP 3: Upload image ───────────────────────────────────────────────
        MockMultipartFile file = new MockMultipartFile(
            "file", "spring-diagram.jpg", "image/jpeg", createJpegImage());

        MvcResult imageResult = mockMvc.perform(multipart("/api/blog/images")
                .file(file)
                .param("altText", "Diagrama de arquitetura Spring Boot"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contentType").value("image/jpeg"))
            .andExpect(jsonPath("$.width").isNumber())
            .andExpect(jsonPath("$.height").isNumber())
            .andReturn();

        String imageId = json(imageResult).get("id").asText();
        assertNotNull(imageId);

        // ── STEP 4: Associate image to topic ──────────────────────────────────
        mockMvc.perform(post("/api/blog/posts/{s}/topics/{t}/images", postSlug, topic1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "imageId": "%s",
                        "caption": "Diagrama mostrando a arquitetura do Spring Boot"
                    }
                    """.formatted(imageId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.position").value(1))
            .andExpect(jsonPath("$.caption").value("Diagrama mostrando a arquitetura do Spring Boot"))
            .andExpect(jsonPath("$.image.id").value(imageId));

        // ── STEP 5: Verify full post response ─────────────────────────────────
        mockMvc.perform(get("/api/blog/posts/{s}", postSlug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(postSlug))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.topicCount").value(2))
            .andExpect(jsonPath("$.topics", hasSize(2)))
            .andExpect(jsonPath("$.topics[0].title").value("O que é Spring Boot"))
            .andExpect(jsonPath("$.topics[0].order").value(1))
            .andExpect(jsonPath("$.topics[1].title").value("Configurando o Projeto"))
            .andExpect(jsonPath("$.topics[1].order").value(2))
            .andExpect(jsonPath("$._links.publish").exists());
    }

    @Test
    void errorFlow_shouldReturnCorrectErrorCodes() throws Exception {
        // 404 on unknown post
        mockMvc.perform(get("/api/blog/posts/nao-existe"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));

        // 400 on missing title
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERRORS"));

        // 400 on invalid title length
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Curto", "categoryId": "%s"}
                    """.formatted(CAT_ID)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /**
     * Gera um JPEG válido de 200x200 em memória para testes.
     */
    private byte[] createJpegImage() throws Exception {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.WHITE);
        g.drawString("Test Image", 50, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}
