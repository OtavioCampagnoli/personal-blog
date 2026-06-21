# Task 02: Criar Endpoint GET /api/blog/posts/{slug} para Buscar Post

## User Story
Como um **Leitor ou Autor**  
Eu quero **visualizar os detalhes completos de um post pelo slug**  
Para que **eu possa ler o conteúdo do post com todos os seus tópicos em ordem**

## Business Context
Leitores precisam acessar posts publicados através de URLs amigáveis (slug). Autores precisam visualizar seus drafts para continuar editando. O endpoint deve retornar o post completo com todos os tópicos ordenados e metadados das imagens.

## Acceptance Criteria

### Happy Path
1. Given existe um post PUBLISHED com slug "java-spring-boot-tutorial"  
   When eu faço GET para `/api/blog/posts/java-spring-boot-tutorial`  
   Then eu recebo HTTP 200 OK  
   And a resposta contém todos os detalhes do post  
   And os tópicos estão ordenados por `order` (ascending)  
   And cada tópico contém o conteúdo Markdown e suas imagens

2. Given eu sou o autor de um post DRAFT  
   When eu faço GET para `/api/blog/posts/{slug}` do meu draft  
   Then eu recebo HTTP 200 OK  
   And posso visualizar o post completo

3. Given o post contém 5 tópicos com diferentes ordens  
   When eu busco o post  
   Then os tópicos aparecem ordenados: [order: 1, 2, 3, 4, 5]  
   And cada tópico contém suas imagens ordenadas por `position`

### Validation Rules
- `slug`: Deve ser uma string não vazia, formato kebab-case

### Error Scenarios
1. Given não existe post com slug "post-inexistente"  
   When eu faço GET para `/api/blog/posts/post-inexistente`  
   Then eu recebo HTTP 404 Not Found  
   And a resposta inclui mensagem: "Post não encontrado com slug: post-inexistente"

2. Given existe um post DRAFT de outro autor  
   When eu (não sendo o autor) faço GET para esse post  
   Then eu recebo HTTP 403 Forbidden  
   And a resposta inclui mensagem: "Você não tem permissão para visualizar este draft"

3. Given existe um post com deletedAt != null (soft deleted)  
   When eu faço GET para `/api/blog/posts/{slug}`  
   Then eu recebo HTTP 404 Not Found  
   And o post não é retornado (tratado como inexistente)

4. Given o slug contém caracteres inválidos (espaços, maiúsculas)  
   When eu faço GET para `/api/blog/posts/Post Inválido`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui erro: "Slug inválido. Use formato kebab-case (ex: meu-post-titulo)"

### Edge Cases
1. Given o post não tem nenhum tópico  
   When eu faço GET para `/api/blog/posts/{slug}`  
   Then eu recebo HTTP 200 OK  
   And `topics` é um array vazio  
   And `topicCount` é 0

2. Given um tópico não tem imagens  
   When eu busco o post  
   Then o tópico aparece com `images: []`  
   And não causa erro

3. Given o post tem tags vazias ou null  
   When eu busco o post  
   Then `tags` retorna array vazio `[]`

### Non-Functional Requirements
- **Performance**: 
  - <100ms para post sem tópicos
  - <300ms para post com até 20 tópicos
  - <500ms para post com mais de 20 tópicos
- **Security**: 
  - Posts PUBLISHED são públicos (sem autenticação)
  - Posts DRAFT requerem autenticação e autorização (ser o autor)
- **Caching**: 
  - Posts PUBLISHED devem ter cache HTTP com `Cache-Control: public, max-age=3600`
  - Posts DRAFT não devem ter cache
- **HATEOAS**: Incluir links para: `self`, `edit`, `delete`, `publish`, `add-topic`, `category`, `topics`

## Request Example
```http
GET /api/blog/posts/como-construir-uma-api-restful-em-java
Accept: application/json
Accept-Language: pt-BR
```

## Response Example (Success - 200 OK)
```json
HTTP/1.1 200 OK
Content-Type: application/hal+json
Cache-Control: public, max-age=3600
ETag: "abc123def456"

{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Como Construir uma API RESTful em Java",
  "slug": "como-construir-uma-api-restful-em-java",
  "description": "Aprenda os conceitos fundamentais para criar APIs REST robustas e escaláveis",
  "status": "PUBLISHED",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "categoryName": "Backend Development",
  "tags": ["java", "spring-boot", "rest-api", "backend"],
  "topicCount": 3,
  "topics": [
    {
      "id": "topic-001",
      "order": 1,
      "title": "Introdução ao REST",
      "content": "# REST - Representational State Transfer\n\nREST é um estilo arquitetural...",
      "contentPreview": "REST é um estilo arquitetural...",
      "images": [
        {
          "id": "img-001",
          "url": "https://s3.amazonaws.com/bucket/images/rest-diagram.png",
          "altText": "Diagrama mostrando arquitetura REST",
          "position": 1,
          "width": 800,
          "height": 600
        }
      ],
      "_links": {
        "self": { "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-001" }
      }
    },
    {
      "id": "topic-002",
      "order": 2,
      "title": "Configurando Spring Boot",
      "content": "## Setup do Projeto\n\n```bash\nmvn archetype:generate...\n```",
      "contentPreview": "Setup do Projeto...",
      "images": [],
      "_links": {
        "self": { "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-002" }
      }
    },
    {
      "id": "topic-003",
      "order": 3,
      "title": "Criando seu Primeiro Endpoint",
      "content": "```java\n@RestController\npublic class HelloController {...}\n```",
      "contentPreview": "@RestController...",
      "images": [
        {
          "id": "img-002",
          "url": "https://s3.amazonaws.com/bucket/images/controller-code.png",
          "altText": "Código do controlador REST",
          "position": 1,
          "width": 1024,
          "height": 768
        },
        {
          "id": "img-003",
          "url": "https://s3.amazonaws.com/bucket/images/postman-test.png",
          "altText": "Teste no Postman",
          "position": 2,
          "width": 1024,
          "height": 768
        }
      ],
      "_links": {
        "self": { "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003" }
      }
    }
  ],
  "author": {
    "id": "author-001",
    "name": "Otávio Silva",
    "avatarUrl": "https://s3.amazonaws.com/bucket/avatars/otavio.jpg"
  },
  "createdAt": "2026-06-10T10:30:00Z",
  "updatedAt": "2026-06-15T14:20:00Z",
  "publishedAt": "2026-06-15T15:00:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "edit": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "delete": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "add-topic": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics"
    },
    "category": {
      "href": "/api/blog/categories/550e8400-e29b-41d4-a716-446655440000"
    },
    "author": {
      "href": "/api/blog/authors/author-001"
    }
  }
}
```

## Response Example (Not Found - 404)
```json
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "POST_NOT_FOUND",
  "message": "Post não encontrado com slug: post-inexistente",
  "details": {
    "slug": "post-inexistente"
  },
  "_links": {
    "search": {
      "href": "/api/blog/posts"
    },
    "docs": {
      "href": "/api/blog/docs"
    }
  }
}
```

## Response Example (Forbidden - 403)
```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "code": "DRAFT_ACCESS_DENIED",
  "message": "Você não tem permissão para visualizar este draft",
  "details": {
    "slug": "meu-draft-privado",
    "status": "DRAFT",
    "reason": "Only the author can view draft posts"
  },
  "_links": {
    "docs": {
      "href": "/api/blog/docs"
    }
  }
}
```

## Technical Notes

### Query Strategy
Usar **join fetch** para evitar N+1 queries:
```java
@Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.topics t
    LEFT JOIN FETCH t.images i
    WHERE p.slug = :slug
    AND p.deletedAt IS NULL
    ORDER BY t.order ASC, i.position ASC
""")
Optional<Post> findBySlugWithTopics(@Param("slug") String slug);
```

### Authorization Logic
```java
public Post getPost(Slug slug, User currentUser) {
    Post post = repository.findBySlugWithTopics(slug)
        .orElseThrow(() -> new PostNotFoundException(slug));
    
    if (post.isDraft() && !post.isAuthor(currentUser)) {
        throw new ForbiddenException("DRAFT_ACCESS_DENIED");
    }
    
    return post;
}
```

### Content Preview
Gerar preview de 150 caracteres do conteúdo Markdown (sem formatação):
```java
public String generatePreview(String markdownContent) {
    String plainText = markdownContent
        .replaceAll("#+ ", "")  // Remove headers
        .replaceAll("\\*\\*|\\*|__|_", "")  // Remove bold/italic
        .replaceAll("```[\\s\\S]*?```", "")  // Remove code blocks
        .replaceAll("`[^`]+`", "");  // Remove inline code
    
    return plainText.substring(0, Math.min(150, plainText.length())) + "...";
}
```

### Cache Strategy
```java
@Cacheable(value = "posts", key = "#slug", condition = "#result.isPublished()")
public Post getPost(Slug slug) {
    // ...
}
```

## Definition of Done
- [ ] GET /api/blog/posts/{slug} endpoint implementado em PostController
- [ ] PostService.getBySlug() implementado com lógica de autorização
- [ ] PostRepository.findBySlugWithTopics() usando join fetch
- [ ] Tópicos retornados ordenados por `order`
- [ ] Imagens dentro de tópicos ordenadas por `position`
- [ ] Content preview gerado para cada tópico
- [ ] Validação de slug (formato kebab-case)
- [ ] Soft delete filtrado (posts deletados retornam 404)
- [ ] Autorização para drafts (apenas autor pode ver)
- [ ] Cache HTTP configurado para posts publicados
- [ ] ETag gerado para suporte a cache condicional
- [ ] Unit tests para PostService
- [ ] Integration tests para GET endpoint
- [ ] Teste de performance (<300ms para 20 tópicos)
- [ ] HATEOAS links incluídos
- [ ] Mensagens i18n (en-US, pt-BR, es-ES)
- [ ] OpenAPI documentation atualizada
- [ ] Code review aprovado
- [ ] Deploy em staging

## Dependencies
- [ ] Task 01 (Create Post) deve estar completa
- [ ] Topic entity e TopicRepository implementados
- [ ] Image entity e ImageRepository implementados
- [ ] Author entity para incluir informações do autor
- [ ] Cache provider configurado (Redis ou in-memory)

## Questions / Clarifications Needed
- [ ] Devemos incluir contagem de visualizações no response?
- [ ] Precisamos de versionamento de conteúdo (histórico de edições)?
- [ ] Como lidar com posts muito grandes (>50 tópicos) - paginação de tópicos?
- [ ] Devemos incluir posts relacionados (related posts) na resposta?

## Estimated Effort
**1.5 dias**
