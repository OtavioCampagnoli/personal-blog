# Task 01: Criar Endpoint POST /api/blog/posts para Criar Post Draft

## User Story
Como um **Autor**  
Eu quero **criar um novo post em status de rascunho**  
Para que **eu possa começar a escrever meu conteúdo sem publicá-lo imediatamente**

## Business Context
Autores precisam criar posts incrementalmente, salvando o trabalho sem torná-lo público. O post começa vazio (sem tópicos) e será construído gradualmente através da adição de tópicos com texto Markdown e imagens.

## Acceptance Criteria

### Happy Path
1. Given eu sou um Autor autenticado  
   When eu faço POST para `/api/blog/posts` com título e categoryId válidos  
   Then um novo Post é criado com status DRAFT  
   And um slug único é gerado a partir do título  
   And eu recebo HTTP 201 Created com os detalhes do post  
   And o header Location contém a URL do post criado  
   And o post não contém tópicos inicialmente (lista vazia)

2. Given eu crio um post com título "Meu Primeiro Post"  
   When o slug "meu-primeiro-post" já existe  
   Then o sistema gera "meu-primeiro-post-2" automaticamente  
   And o post é criado com sucesso

### Validation Rules
- `title`: Obrigatório, 10-200 caracteres, não pode ser blank
- `description`: Opcional, máximo 500 caracteres (meta description para SEO)
- `categoryId`: Obrigatório, deve referenciar categoria existente
- `tags`: Opcional, array de strings, máximo 10 tags, cada uma 2-50 caracteres

### Error Scenarios
1. Given eu não estou autenticado  
   When eu faço POST para `/api/blog/posts`  
   Then eu recebo HTTP 401 Unauthorized  
   And a resposta inclui mensagem de erro no idioma requisitado

2. Given eu forneço um título com apenas 5 caracteres  
   When eu faço POST para `/api/blog/posts`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui erro de validação: "O título deve ter entre 10 e 200 caracteres (fornecido: 5)"

3. Given eu forneço um categoryId inexistente  
   When eu faço POST para `/api/blog/posts`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui erro: "Categoria não encontrada com ID: {categoryId}"

4. Given o título está vazio ou null  
   When eu faço POST para `/api/blog/posts`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui erro: "Título é obrigatório"

### Edge Cases
1. Given eu forneço tags duplicadas  
   When eu faço POST para `/api/blog/posts`  
   Then duplicatas são removidas automaticamente  
   And o post é criado apenas com tags únicas

2. Given eu forneço uma tag com espaços nas pontas  
   When eu faço POST para `/api/blog/posts`  
   Then espaços são removidos e a tag é normalizada para lowercase  
   And o post é criado com sucesso

3. Given eu forneço 15 tags (excede o limite de 10)  
   When eu faço POST para `/api/blog/posts`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui erro: "Máximo de 10 tags permitidas (fornecido: 15)"

### Non-Functional Requirements
- **Performance**: Endpoint deve responder em <200ms
- **Security**: Requer token JWT válido
- **i18n**: Mensagens de erro retornadas no idioma do header Accept-Language (en-US, pt-BR, es-ES), padrão en-US
- **HATEOAS**: Resposta inclui `_links` com relações: `self`, `add-topic`, `publish`, `edit`, `delete`, `category`
- **Idempotency**: Requisições duplicadas com mesmo conteúdo em 5 minutos retornam o post existente (409 Conflict) com Location header

## Request Example
```json
POST /api/blog/posts
Content-Type: application/json
Accept-Language: pt-BR
Authorization: Bearer {token}

{
  "title": "Como Construir uma API RESTful em Java",
  "description": "Aprenda os conceitos fundamentais para criar APIs REST robustas e escaláveis",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "tags": ["java", "spring-boot", "rest-api", "backend"]
}
```

## Response Example (Success - 201 Created)
```json
HTTP/1.1 201 Created
Location: /api/blog/posts/como-construir-uma-api-restful-em-java
Content-Type: application/hal+json

{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Como Construir uma API RESTful em Java",
  "slug": "como-construir-uma-api-restful-em-java",
  "description": "Aprenda os conceitos fundamentais para criar APIs REST robustas e escaláveis",
  "status": "DRAFT",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "tags": ["java", "spring-boot", "rest-api", "backend"],
  "topics": [],
  "topicCount": 0,
  "createdAt": "2026-06-18T10:30:00Z",
  "updatedAt": "2026-06-18T10:30:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "add-topic": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics"
    },
    "publish": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/publish"
    },
    "edit": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "delete": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java"
    },
    "category": {
      "href": "/api/blog/categories/550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

## Response Example (Validation Error - pt-BR)
```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERRORS",
  "message": "Erro de validação nos campos fornecidos",
  "errors": [
    {
      "field": "title",
      "code": "TITLE_TOO_SHORT",
      "message": "O título deve ter entre 10 e 200 caracteres (fornecido: 5)",
      "providedValue": "Test",
      "example": "Como Construir uma API RESTful em Java"
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
public class Post extends BaseEntity<Post> {
    private Id<Post> id;
    private String title;
    private Slug slug;
    private String description;
    private PostStatus status; // DRAFT, PUBLISHED, ARCHIVED
    private Id<Category> categoryId;
    private List<String> tags;
    private List<Id<Topic>> topicIds; // Referências aos tópicos
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt; // Soft delete
    
    // Business rules
    public void validate(Notification notification) {
        if (title == null || title.isBlank()) {
            notification.addError("title", "TITLE_REQUIRED", "Title is required");
        }
        if (title != null && (title.length() < 10 || title.length() > 200)) {
            notification.addError("title", "TITLE_LENGTH_INVALID", 
                "Title must be between 10 and 200 characters");
        }
        if (tags != null && tags.size() > 10) {
            notification.addError("tags", "TOO_MANY_TAGS", 
                "Maximum 10 tags allowed");
        }
    }
}
```

### Slug Generation
- Usar biblioteca `github-slugger` ou implementação própria
- Normalizar: lowercase, remover acentos, substituir espaços por hífens
- Remover caracteres especiais (manter apenas a-z, 0-9, hífens)
- Exemplo: "Como Construir uma API?" → "como-construir-uma-api"
- Se slug existe, adicionar sufixo numérico incremental

### Repository Pattern
```java
public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Id<Post> id);
    Optional<Post> findBySlug(Slug slug);
    boolean existsBySlug(Slug slug);
    List<Post> findAll(PostFilter filter, Pageable pageable);
}
```

### Notification Pattern
Acumular todos os erros de validação antes de lançar exceção:
```java
Notification notification = Notification.create();
post.validate(notification);
category.validate(notification);

if (notification.hasErrors()) {
    throw new DomainValidationException(notification);
}
```

### Event Publishing
Após criar o post, publicar evento assíncrono:
```java
eventPublisher.publish(new PostCreatedEvent(post.getId(), post.getTitle()));
```

## Definition of Done
- [ ] POST /api/blog/posts endpoint implementado em PostController
- [ ] CreatePostRequest DTO criado com anotações Jakarta Bean Validation
- [ ] PostService.createDraft() implementado com lógica de negócio
- [ ] Geração de slug com verificação de unicidade
- [ ] Validação de Post usando Notification pattern
- [ ] Post entity criada estendendo BaseEntity<Post>
- [ ] PostRepository com método save() e findBySlug()
- [ ] Integração com CategoryRepository para validar categoria
- [ ] Normalização de tags (trim, lowercase, deduplicação)
- [ ] Unit tests para PostService (repository mockado)
- [ ] Integration tests para POST endpoint (TestContainers)
- [ ] Resposta de erro segue RFC 7807 Problem Details
- [ ] Mensagens i18n adicionadas (en-US, pt-BR, es-ES)
- [ ] HATEOAS links incluídos na resposta
- [ ] OpenAPI documentation atualizada
- [ ] Performance testada (<200ms)
- [ ] Code review aprovado
- [ ] Deploy em staging

## Dependencies
- [ ] **Shared Module**: BaseEntity<T> e Id<T> devem estar implementados
- [ ] **Category Entity**: Category e CategoryRepository devem existir
- [ ] **SlugGenerator**: Componente para gerar slugs
- [ ] **Notification Pattern**: Classe Notification para acumular erros
- [ ] **Authentication**: Middleware de autenticação JWT configurado
- [ ] **Database Schema**: Tabela `posts` criada

## Questions / Clarifications Needed
- [ ] Devemos limitar o número de drafts por autor?
- [ ] Drafts antigos (>30 dias sem atualização) devem ser arquivados automaticamente?
- [ ] Autor pode ter múltiplos posts com mesmo título em DRAFT?
- [ ] Como lidar com concorrência (dois autores criando post com mesmo título simultaneamente)?

## Estimated Effort
**2 dias** (assumindo que shared module e dependências já existem)
