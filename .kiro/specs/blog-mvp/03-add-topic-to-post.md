# Task 03: Criar Endpoint POST /api/blog/posts/{slug}/topics para Adicionar Tópico

## User Story
Como um **Autor**  
Eu quero **adicionar um novo tópico ao meu post**  
Para que **eu possa construir o conteúdo do post incrementalmente com seções organizadas**

## Business Context
Posts são compostos por múltiplos tópicos. Cada tópico representa uma seção com título e conteúdo em Markdown. Autores constroem seus posts adicionando tópicos um a um. A ordem dos tópicos pode ser especificada ou gerada automaticamente.

## Acceptance Criteria

### Happy Path
1. Given eu sou o autor de um post DRAFT  
   When eu faço POST para `/api/blog/posts/{slug}/topics` com título e conteúdo válidos  
   Then um novo Topic é criado e associado ao post  
   And o tópico recebe automaticamente `order = max(existing_orders) + 1`  
   And eu recebo HTTP 201 Created com os detalhes do tópico  
   And o header Location contém a URL do tópico criado

2. Given meu post tem 3 tópicos (orders: 1, 2, 3)  
   When eu adiciono um novo tópico sem especificar ordem  
   Then o novo tópico recebe `order = 4`  
   And é adicionado ao final da lista

3. Given eu quero adicionar um tópico em posição específica  
   When eu forneço `order: 2` no request  
   Then o novo tópico recebe `order = 2`  
   And tópicos existentes com order >= 2 são incrementados automaticamente  
   And a sequência fica: [1, novo_2, antigo_2→3, antigo_3→4]

4. Given meu post está vazio (0 tópicos)  
   When eu adiciono o primeiro tópico  
   Then ele recebe `order = 1`  
   And é criado com sucesso

### Validation Rules
- `title`: Obrigatório, 3-100 caracteres, não pode ser blank
- `content`: Obrigatório, mínimo 10 caracteres, formato Markdown válido
- `order`: Opcional, deve ser >= 1, se fornecido
- Post deve estar em status DRAFT ou PUBLISHED (não pode adicionar em ARCHIVED)

### Error Scenarios
1. Given eu não sou o autor do post  
   When eu tento adicionar um tópico  
   Then eu recebo HTTP 403 Forbidden  
   And a resposta inclui: "Você não tem permissão para editar este post"

2. Given o post não existe  
   When eu faço POST para `/api/blog/posts/inexistente/topics`  
   Then eu recebo HTTP 404 Not Found  
   And a resposta inclui: "Post não encontrado com slug: inexistente"

3. Given eu forneço um título com apenas 2 caracteres  
   When eu adiciono o tópico  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "O título do tópico deve ter entre 3 e 100 caracteres (fornecido: 2)"

4. Given eu forneço conteúdo com apenas 5 caracteres  
   When eu adiciono o tópico  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "O conteúdo deve ter no mínimo 10 caracteres (fornecido: 5)"

5. Given eu forneço `order: 0` ou negativo  
   When eu adiciono o tópico  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "A ordem deve ser >= 1"

6. Given o post está ARCHIVED  
   When eu tento adicionar um tópico  
   Then eu recebo HTTP 409 Conflict  
   And a resposta inclui: "Não é possível adicionar tópicos a posts arquivados"

7. Given eu não estou autenticado  
   When eu tento adicionar um tópico  
   Then eu recebo HTTP 401 Unauthorized

### Edge Cases
1. Given eu adiciono um tópico com `order: 999` (maior que quantidade atual)  
   When o post tem apenas 3 tópicos  
   Then o tópico recebe `order = 4` (ajustado automaticamente)  
   And é adicionado ao final

2. Given eu forneço conteúdo Markdown inválido (tags HTML malformadas)  
   When eu adiciono o tópico  
   Then o conteúdo é salvo como está (sem validação estrita de Markdown)  
   And o tópico é criado (responsabilidade do frontend sanitizar HTML)

3. Given o título contém caracteres especiais ou emojis  
   When eu adiciono o tópico  
   Then caracteres são preservados e salvos normalmente  
   And o tópico é criado

4. Given eu adiciono 50 tópicos ao mesmo post  
   When eu faço a 51ª requisição  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Máximo de 50 tópicos permitidos por post"

### Non-Functional Requirements
- **Performance**: Endpoint deve responder em <150ms
- **Security**: Requer autenticação JWT e autorização (ser autor do post)
- **Transaction**: Inserção do tópico e atualização de ordens deve ser atômica
- **Validation**: Usar Notification pattern para acumular erros
- **Event**: Publicar `TopicAddedEvent` após sucesso
- **HATEOAS**: Incluir links para: `self`, `post`, `reorder`, `edit`, `delete`, `add-image`

## Request Example (Sem especificar ordem)
```json
POST /api/blog/posts/como-construir-uma-api-restful-em-java/topics
Content-Type: application/json
Accept-Language: pt-BR
Authorization: Bearer {token}

{
  "title": "Configurando o Banco de Dados",
  "content": "## PostgreSQL Setup\n\nPara configurar o PostgreSQL no seu projeto Spring Boot:\n\n```yaml\nspring:\n  datasource:\n    url: jdbc:postgresql://localhost:5432/blog\n```\n\nEste tópico explica passo a passo..."
}
```

## Request Example (Com ordem específica)
```json
POST /api/blog/posts/como-construir-uma-api-restful-em-java/topics
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Requisitos do Sistema",
  "content": "Antes de começar, certifique-se de ter:\n\n- Java 21+\n- Maven 3.8+\n- IDE (IntelliJ ou Eclipse)",
  "order": 1
}
```

## Response Example (Success - 201 Created)
```json
HTTP/1.1 201 Created
Location: /api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-004
Content-Type: application/hal+json

{
  "id": "topic-004",
  "postId": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Configurando o Banco de Dados",
  "content": "## PostgreSQL Setup\n\nPara configurar o PostgreSQL no seu projeto Spring Boot:\n\n```yaml\nspring:\n  datasource:\n    url: jdbc:postgresql://localhost:5432/blog\n```\n\nEste tópico explica passo a passo...",
  "contentPreview": "PostgreSQL Setup - Para configurar o PostgreSQL no seu projeto Spring Boot...",
  "order": 4,
  "imageCount": 0,
  "createdAt": "2026-06-18T11:00:00Z",
  "updatedAt": "2026-06-18T11:00:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-004"
    },
    "post": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "reorder": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/reorder"
    },
    "edit": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-004"
    },
    "delete": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-004"
    },
    "add-image": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-004/images"
    }
  }
}
```

## Response Example (Validation Error - pt-BR)
```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T11:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERRORS",
  "message": "Erro de validação nos campos fornecidos",
  "errors": [
    {
      "field": "title",
      "code": "TITLE_TOO_SHORT",
      "message": "O título do tópico deve ter entre 3 e 100 caracteres (fornecido: 2)",
      "providedValue": "DB",
      "example": "Configurando o Banco de Dados"
    },
    {
      "field": "content",
      "code": "CONTENT_TOO_SHORT",
      "message": "O conteúdo deve ter no mínimo 10 caracteres (fornecido: 5)",
      "providedValue": "Setup",
      "example": "## PostgreSQL Setup\n\nPara configurar..."
    }
  ],
  "_links": {
    "docs": {
      "href": "/api/blog/docs"
    }
  }
}
```

## Technical Notes

### Domain Model
```java
public class Topic extends BaseEntity<Topic> {
    private Id<Topic> id;
    private Id<Post> postId;
    private String title;
    private String content; // Markdown
    private int order;
    private List<Id<Image>> imageIds;
    private Instant createdAt;
    private Instant updatedAt;
    
    public void validate(Notification notification) {
        if (title == null || title.isBlank()) {
            notification.addError("title", "TITLE_REQUIRED", "Title is required");
        }
        if (title != null && (title.length() < 3 || title.length() > 100)) {
            notification.addError("title", "TITLE_LENGTH_INVALID", 
                "Title must be between 3 and 100 characters");
        }
        if (content == null || content.length() < 10) {
            notification.addError("content", "CONTENT_TOO_SHORT", 
                "Content must have at least 10 characters");
        }
        if (order < 1) {
            notification.addError("order", "ORDER_INVALID", 
                "Order must be >= 1");
        }
    }
}
```

### Order Management Logic
```java
public Topic addTopic(Slug postSlug, CreateTopicRequest request, User author) {
    Post post = postRepository.findBySlug(postSlug)
        .orElseThrow(() -> new PostNotFoundException(postSlug));
    
    post.ensureAuthor(author); // Throws ForbiddenException if not author
    post.ensureCanBeEdited(); // Throws ConflictException if ARCHIVED
    
    int topicCount = topicRepository.countByPostId(post.getId());
    if (topicCount >= 50) {
        throw new MaxTopicsExceededException("Maximum 50 topics per post");
    }
    
    int targetOrder = request.getOrder() != null 
        ? Math.min(request.getOrder(), topicCount + 1) 
        : topicCount + 1;
    
    // Se ordem especificada, incrementar tópicos >= targetOrder
    if (request.getOrder() != null && targetOrder <= topicCount) {
        topicRepository.incrementOrdersFrom(post.getId(), targetOrder);
    }
    
    Topic topic = Topic.create(
        post.getId(),
        request.getTitle(),
        request.getContent(),
        targetOrder
    );
    
    Notification notification = Notification.create();
    topic.validate(notification);
    notification.throwIfHasErrors();
    
    Topic saved = topicRepository.save(topic);
    eventPublisher.publish(new TopicAddedEvent(saved.getId(), post.getId()));
    
    return saved;
}
```

### Repository Methods
```java
public interface TopicRepository {
    Topic save(Topic topic);
    int countByPostId(Id<Post> postId);
    void incrementOrdersFrom(Id<Post> postId, int fromOrder);
    List<Topic> findByPostIdOrderByOrder(Id<Post> postId);
}
```

### SQL for Increment Orders
```sql
UPDATE topics 
SET "order" = "order" + 1,
    updated_at = NOW()
WHERE post_id = ? 
  AND "order" >= ?
```

## Definition of Done
- [ ] POST /api/blog/posts/{slug}/topics endpoint implementado
- [ ] AddTopicRequest DTO criado com validações
- [ ] TopicService.addTopic() implementado
- [ ] Topic entity criada com validações
- [ ] TopicRepository com save(), countByPostId(), incrementOrdersFrom()
- [ ] Lógica de ordem automática implementada
- [ ] Lógica de reordenação quando ordem específica é fornecida
- [ ] Autorização (apenas autor pode adicionar tópicos)
- [ ] Validação de limite de 50 tópicos
- [ ] Validação de status do post (não pode ser ARCHIVED)
- [ ] Transaction management para atomicidade
- [ ] Unit tests para TopicService
- [ ] Integration tests para POST endpoint
- [ ] Teste de concorrência (múltiplos tópicos adicionados simultaneamente)
- [ ] TopicAddedEvent publicado após sucesso
- [ ] HATEOAS links incluídos
- [ ] Mensagens i18n (en-US, pt-BR, es-ES)
- [ ] OpenAPI documentation atualizada
- [ ] Code review aprovado
- [ ] Deploy em staging

## Dependencies
- [ ] Task 01 (Create Post) deve estar completa
- [ ] Task 02 (Get Post) deve estar completa
- [ ] Post entity deve ter método ensureAuthor() e ensureCanBeEdited()
- [ ] Database schema: tabela `topics` criada com índice em (post_id, order)

## Questions / Clarifications Needed
- [ ] Devemos permitir adicionar tópicos a posts PUBLISHED (já publicados)?
- [ ] Como lidar com concorrência na reordenação (dois autores adicionando tópicos simultaneamente)?
- [ ] Devemos ter validação de Markdown (syntax check) ou deixar livre?
- [ ] Preview de conteúdo deve ser gerado no backend ou frontend?

## Estimated Effort
**2 dias**
