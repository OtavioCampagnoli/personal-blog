# Task 05: Criar Endpoint POST /api/blog/posts/{slug}/topics/{topicId}/images para Associar Imagem ao Tópico

## User Story
Como um **Autor**  
Eu quero **associar uma imagem já enviada ao S3 a um tópico específico**  
Para que **a imagem apareça no local correto dentro do conteúdo do post**

## Business Context
Após fazer upload de imagens para o S3 (Task 04), autores precisam associá-las aos tópicos. Cada imagem tem uma posição específica dentro do tópico, permitindo controlar onde ela aparece em relação ao texto Markdown.

## Acceptance Criteria

### Happy Path
1. Given eu sou o autor do post  
   And existe uma imagem que eu fiz upload  
   And existe um tópico no meu post  
   When eu faço POST para `/api/blog/posts/{slug}/topics/{topicId}/images` com o imageId  
   Then a imagem é associada ao tópico  
   And a imagem recebe automaticamente `position = max(existing_positions) + 1`  
   And eu recebo HTTP 201 Created com os detalhes da associação

2. Given o tópico já tem 2 imagens (positions: 1, 2)  
   When eu adiciono uma nova imagem sem especificar posição  
   Then a nova imagem recebe `position = 3`  
   And é adicionada ao final

3. Given eu quero adicionar uma imagem em posição específica  
   When eu forneço `position: 2` no request  
   Then a nova imagem recebe `position = 2`  
   And imagens existentes com position >= 2 são incrementadas automaticamente  
   And a sequência fica: [1, nova_2, antiga_2→3]

4. Given o tópico está vazio (0 imagens)  
   When eu adiciono a primeira imagem  
   Then ela recebe `position = 1`

### Validation Rules
- `imageId`: Obrigatório, deve referenciar imagem existente
- `position`: Opcional, deve ser >= 1 se fornecido
- `caption`: Opcional, máximo 300 caracteres
- Imagem deve pertencer ao autor do post (segurança)
- Tópico deve pertencer ao post especificado
- Limite: máximo 20 imagens por tópico

### Error Scenarios
1. Given eu não sou o autor do post  
   When eu tento associar uma imagem  
   Then eu recebo HTTP 403 Forbidden  
   And a resposta inclui: "Você não tem permissão para editar este post"

2. Given o imageId não existe  
   When eu faço a requisição  
   Then eu recebo HTTP 404 Not Found  
   And a resposta inclui: "Imagem não encontrada com ID: {imageId}"

3. Given a imagem pertence a outro autor  
   When eu tento associá-la ao meu tópico  
   Then eu recebo HTTP 403 Forbidden  
   And a resposta inclui: "Você não pode usar imagens de outros autores"

4. Given o topicId não existe  
   When eu faço a requisição  
   Then eu recebo HTTP 404 Not Found  
   And a resposta inclui: "Tópico não encontrado com ID: {topicId}"

5. Given o tópico não pertence ao post especificado  
   When eu faço POST para `/api/blog/posts/post-a/topics/topic-from-post-b/images`  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Tópico não pertence ao post especificado"

6. Given o tópico já tem 20 imagens  
   When eu tento adicionar a 21ª imagem  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Máximo de 20 imagens permitidas por tópico"

7. Given eu forneço `position: 0` ou negativo  
   When eu faço a requisição  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "A posição deve ser >= 1"

8. Given a imagem já está associada a este tópico  
   When eu tento associá-la novamente  
   Then eu recebo HTTP 409 Conflict  
   And a resposta inclui: "Esta imagem já está associada a este tópico"

### Edge Cases
1. Given eu adiciono uma imagem com `position: 999` (maior que quantidade atual)  
   When o tópico tem apenas 3 imagens  
   Then a imagem recebe `position = 4` (ajustado automaticamente)  
   And é adicionada ao final

2. Given eu forneço caption com 350 caracteres  
   When eu faço a requisição  
   Then eu recebo HTTP 400 Bad Request  
   And a resposta inclui: "Caption não pode exceder 300 caracteres"

3. Given eu não forneço caption  
   When eu adiciono a imagem  
   Then caption fica null  
   And o altText da imagem é usado como fallback no frontend

4. Given a mesma imagem já está em outro tópico do mesmo post  
   When eu tento adicioná-la a um novo tópico  
   Then a associação é criada com sucesso (imagem pode estar em múltiplos tópicos)

### Non-Functional Requirements
- **Performance**: Endpoint deve responder em <100ms
- **Security**: Requer autenticação JWT e autorização (ser autor do post)
- **Transaction**: Associação e atualização de posições deve ser atômica
- **HATEOAS**: Incluir links para: `self`, `topic`, `image`, `reorder`, `delete`
- **Event**: Publicar `ImageAddedToTopicEvent` após sucesso

## Request Example (Sem especificar posição)
```json
POST /api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images
Content-Type: application/json
Authorization: Bearer {token}

{
  "imageId": "img-550e8400-e29b-41d4-a716-446655440000",
  "caption": "Exemplo de código mostrando a estrutura do controlador REST com anotações Spring"
}
```

## Request Example (Com posição específica)
```json
POST /api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-001/images
Content-Type: application/json
Authorization: Bearer {token}

{
  "imageId": "img-123e4567-e89b-12d3-a456-426614174000",
  "position": 1,
  "caption": "Arquitetura geral do sistema antes de começarmos"
}
```

## Response Example (Success - 201 Created)
```json
HTTP/1.1 201 Created
Location: /api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images/img-550e8400
Content-Type: application/hal+json

{
  "id": "topic-image-001",
  "topicId": "topic-003",
  "postSlug": "como-construir-uma-api-restful-em-java",
  "imageId": "img-550e8400-e29b-41d4-a716-446655440000",
  "position": 3,
  "caption": "Exemplo de código mostrando a estrutura do controlador REST com anotações Spring",
  "image": {
    "id": "img-550e8400-e29b-41d4-a716-446655440000",
    "url": "https://d1234567890.cloudfront.net/images/author-123/550e8400.png",
    "altText": "Código do controlador REST",
    "contentType": "image/png",
    "width": 1920,
    "height": 1080
  },
  "addedAt": "2026-06-18T13:00:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images/img-550e8400"
    },
    "topic": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003"
    },
    "image": {
      "href": "/api/blog/images/img-550e8400-e29b-41d4-a716-446655440000"
    },
    "reorder": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images/reorder"
    },
    "delete": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images/img-550e8400"
    }
  }
}
```

## Response Example (Image Already Associated)
```json
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T13:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "IMAGE_ALREADY_IN_TOPIC",
  "message": "Esta imagem já está associada a este tópico",
  "details": {
    "imageId": "img-550e8400-e29b-41d4-a716-446655440000",
    "topicId": "topic-003",
    "existingPosition": 2
  },
  "_links": {
    "existing": {
      "href": "/api/blog/posts/como-construir-uma-api-restful-em-java/topics/topic-003/images/img-550e8400"
    }
  }
}
```

## Response Example (Image Belongs to Different Author)
```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "timestamp": "2026-06-18T13:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "code": "IMAGE_OWNERSHIP_VIOLATION",
  "message": "Você não pode usar imagens de outros autores",
  "details": {
    "imageId": "img-550e8400-e29b-41d4-a716-446655440000",
    "imageAuthorId": "author-999",
    "yourAuthorId": "author-123"
  },
  "_links": {
    "upload-your-own": {
      "href": "/api/blog/images"
    }
  }
}
```

## Technical Notes

### Domain Model
```java
public class TopicImage extends BaseEntity<TopicImage> {
    private Id<TopicImage> id;
    private Id<Topic> topicId;
    private Id<Image> imageId;
    private int position;
    private String caption; // Nullable
    private Instant addedAt;
    
    public void validate(Notification notification) {
        if (caption != null && caption.length() > 300) {
            notification.addError("caption", "CAPTION_TOO_LONG", 
                "Caption cannot exceed 300 characters");
        }
        if (position < 1) {
            notification.addError("position", "POSITION_INVALID", 
                "Position must be >= 1");
        }
    }
}
```

### Association Logic
```java
public TopicImage addImageToTopic(
    Slug postSlug, 
    Id<Topic> topicId, 
    AddImageRequest request, 
    User author
) {
    Post post = postRepository.findBySlug(postSlug)
        .orElseThrow(() -> new PostNotFoundException(postSlug));
    
    post.ensureAuthor(author);
    
    Topic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new TopicNotFoundException(topicId));
    
    if (!topic.belongsToPost(post.getId())) {
        throw new InvalidTopicException("Topic does not belong to specified post");
    }
    
    Image image = imageRepository.findById(request.getImageId())
        .orElseThrow(() -> new ImageNotFoundException(request.getImageId()));
    
    if (!image.belongsToAuthor(author.getId())) {
        throw new ImageOwnershipException("Cannot use images from other authors");
    }
    
    boolean alreadyAssociated = topicImageRepository
        .existsByTopicIdAndImageId(topicId, image.getId());
    
    if (alreadyAssociated) {
        throw new ImageAlreadyAssociatedException("Image already in this topic");
    }
    
    int imageCount = topicImageRepository.countByTopicId(topicId);
    if (imageCount >= 20) {
        throw new MaxImagesExceededException("Maximum 20 images per topic");
    }
    
    int targetPosition = request.getPosition() != null 
        ? Math.min(request.getPosition(), imageCount + 1) 
        : imageCount + 1;
    
    if (request.getPosition() != null && targetPosition <= imageCount) {
        topicImageRepository.incrementPositionsFrom(topicId, targetPosition);
    }
    
    TopicImage topicImage = TopicImage.create(
        topicId,
        image.getId(),
        targetPosition,
        request.getCaption()
    );
    
    Notification notification = Notification.create();
    topicImage.validate(notification);
    notification.throwIfHasErrors();
    
    TopicImage saved = topicImageRepository.save(topicImage);
    eventPublisher.publish(new ImageAddedToTopicEvent(saved.getId(), topicId));
    
    return saved;
}
```

### Repository Methods
```java
public interface TopicImageRepository {
    TopicImage save(TopicImage topicImage);
    int countByTopicId(Id<Topic> topicId);
    boolean existsByTopicIdAndImageId(Id<Topic> topicId, Id<Image> imageId);
    void incrementPositionsFrom(Id<Topic> topicId, int fromPosition);
    List<TopicImage> findByTopicIdOrderByPosition(Id<Topic> topicId);
}
```

### SQL for Increment Positions
```sql
UPDATE topic_images 
SET position = position + 1,
    updated_at = NOW()
WHERE topic_id = ? 
  AND position >= ?
```

### Database Schema
```sql
CREATE TABLE topic_images (
    id UUID PRIMARY KEY,
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    image_id UUID NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    position INT NOT NULL CHECK (position >= 1),
    caption VARCHAR(300),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (topic_id, image_id),
    UNIQUE (topic_id, position)
);

CREATE INDEX idx_topic_images_topic_id ON topic_images(topic_id);
CREATE INDEX idx_topic_images_position ON topic_images(topic_id, position);
```

## Definition of Done
- [ ] POST /api/blog/posts/{slug}/topics/{topicId}/images endpoint implementado
- [ ] AddImageToTopicRequest DTO criado
- [ ] TopicImageService.addImage() implementado
- [ ] TopicImage entity criada com validações
- [ ] TopicImageRepository implementado
- [ ] Lógica de posição automática implementada
- [ ] Lógica de reordenação quando posição específica é fornecida
- [ ] Validação de ownership da imagem
- [ ] Validação de pertencimento do tópico ao post
- [ ] Validação de limite de 20 imagens por tópico
- [ ] Verificação de duplicação (mesma imagem no mesmo tópico)
- [ ] Transaction management para atomicidade
- [ ] Unit tests para TopicImageService
- [ ] Integration tests para POST endpoint
- [ ] ImageAddedToTopicEvent publicado após sucesso
- [ ] HATEOAS links incluídos
- [ ] Mensagens i18n (en-US, pt-BR, es-ES)
- [ ] OpenAPI documentation atualizada
- [ ] Code review aprovado
- [ ] Deploy em staging

## Dependencies
- [ ] Task 03 (Add Topic) deve estar completa
- [ ] Task 04 (Upload Image) deve estar completa
- [ ] Database schema: tabela `topic_images` criada
- [ ] Topic entity deve ter método belongsToPost()
- [ ] Image entity deve ter método belongsToAuthor()

## Questions / Clarifications Needed
- [ ] Caption suporta Markdown ou apenas texto puro?
- [ ] Devemos permitir associar imagens a posts PUBLISHED (já publicados)?
- [ ] Como lidar com remoção de imagem do S3 que ainda está associada a tópicos?
- [ ] Devemos ter preview/thumbnail específico para listagens?

## Estimated Effort
**1.5 dias**
