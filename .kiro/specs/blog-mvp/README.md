# Blog MVP - Tasks Overview

## Objetivo
Construir um sistema de blog com suporte a posts compostos por tópicos ordenados, cada um contendo texto em Markdown e imagens posicionadas.

## Princípios de Design
- **Domain-Driven Design**: Agregados bem definidos (Post, Topic, Image)
- **Clean Architecture**: Separação clara entre domain, application e infrastructure
- **Notification Pattern**: Acumulação de erros de validação
- **HATEOAS**: Hypermedia links para navegação da API
- **Serverless**: S3 para storage de imagens, preparado para Lambda
- **Soft Delete**: Dados nunca são removidos permanentemente
- **i18n**: Suporte a en-US, pt-BR, es-ES

## Fases de Implementação

### Fase 1: CRUD de Posts ✅
Posts básicos com metadados (título, slug, status, tags, categoria)

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| [01-create-post-draft.md](./01-create-post-draft.md) | 📋 To Do | 2 dias | POST /api/blog/posts - Criar post em draft |
| [02-get-post.md](./02-get-post.md) | 📋 To Do | 1.5 dias | GET /api/blog/posts/{slug} - Buscar post completo |

### Fase 2: Tópicos ✅
Adicionar, editar e reordenar tópicos dentro de um post

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| [03-add-topic-to-post.md](./03-add-topic-to-post.md) | 📋 To Do | 2 dias | POST /api/blog/posts/{slug}/topics - Adicionar tópico |
| 06-update-topic.md | ⏳ Pendente | 1 dia | PUT /api/blog/posts/{slug}/topics/{id} - Editar tópico |
| 07-reorder-topics.md | ⏳ Pendente | 1 dia | PUT /api/blog/posts/{slug}/topics/reorder - Reordenar |
| 08-delete-topic.md | ⏳ Pendente | 0.5 dia | DELETE /api/blog/posts/{slug}/topics/{id} - Remover |

### Fase 3: Imagens ✅
Upload para S3 e associação com tópicos

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| [04-upload-image-to-s3.md](./04-upload-image-to-s3.md) | 📋 To Do | 2.5 dias | POST /api/blog/images - Upload para S3 |
| [05-add-image-to-topic.md](./05-add-image-to-topic.md) | 📋 To Do | 1.5 dias | POST /topics/{id}/images - Associar imagem |
| 09-reorder-images.md | ⏳ Pendente | 0.5 dia | PUT /topics/{id}/images/reorder - Reordenar |
| 10-delete-image.md | ⏳ Pendente | 1 dia | DELETE /api/blog/images/{id} - Remover do S3 |

### Fase 4: Publicação
Workflow de draft → published

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| 11-publish-post.md | ⏳ Pendente | 1 dia | POST /api/blog/posts/{slug}/publish - Publicar |
| 12-unpublish-post.md | ⏳ Pendente | 0.5 dia | POST /api/blog/posts/{slug}/unpublish - Despublicar |

### Fase 5: Listagem e Busca
Endpoints para descoberta de posts

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| 13-list-posts.md | ⏳ Pendente | 1.5 dias | GET /api/blog/posts - Listar com filtros |
| 14-search-posts.md | ⏳ Pendente | 2 dias | GET /api/blog/posts/search - Busca full-text |

### Fase 6: Infraestrutura e Shared
Componentes compartilhados necessários para todas as tasks

| Task | Status | Estimativa | Descrição |
|------|--------|------------|-----------|
| 00-shared-module.md | ⏳ Pendente | 3 dias | BaseEntity, Id<T>, Notification, Exceptions |
| 15-category-entity.md | ⏳ Pendente | 1 dia | Category entity e repository |
| 16-author-entity.md | ⏳ Pendente | 1 dia | Author entity e repository |
| 17-authentication.md | ⏳ Pendente | 2 dias | JWT authentication middleware |

## Estimativas Totais

- **Fase 1**: 3.5 dias
- **Fase 2**: 4.5 dias
- **Fase 3**: 4.5 dias
- **Fase 4**: 1.5 dias
- **Fase 5**: 3.5 dias
- **Fase 6**: 7 dias (infraestrutura)

**Total MVP**: ~24.5 dias (~5 semanas)

## Dependências Críticas

### Antes de começar Task 01:
- [ ] Shared Module (BaseEntity, Id, Notification)
- [ ] Category entity
- [ ] Author entity
- [ ] Authentication middleware
- [ ] Database schema inicial

### Ordem recomendada:
1. **Shared Module** (Task 00) - Base para tudo
2. **Category + Author** (Tasks 15, 16) - Dependências do Post
3. **Authentication** (Task 17) - Segurança
4. **Posts** (Tasks 01-02) - CRUD básico
5. **Topics** (Tasks 03, 06-08) - Conteúdo estruturado
6. **Images** (Tasks 04-05, 09-10) - Mídia
7. **Publish** (Tasks 11-12) - Workflow
8. **List/Search** (Tasks 13-14) - Descoberta

## Modelo de Dados

### Post (Agregado Raiz)
```
Post
├── id: UUID
├── title: String
├── slug: String (unique)
├── description: String
├── status: DRAFT | PUBLISHED | ARCHIVED
├── categoryId: UUID
├── authorId: UUID
├── tags: String[]
├── topicIds: UUID[]
└── timestamps
```

### Topic (Entidade)
```
Topic
├── id: UUID
├── postId: UUID (FK)
├── title: String
├── content: String (Markdown)
├── order: int
├── imageIds: UUID[]
└── timestamps
```

### TopicImage (Objeto de Valor / Associação)
```
TopicImage
├── id: UUID
├── topicId: UUID (FK)
├── imageId: UUID (FK)
├── position: int
├── caption: String
└── addedAt: timestamp
```

### Image (Entidade)
```
Image
├── id: UUID
├── s3Key: String
├── url: String (CloudFront)
├── altText: String
├── contentType: String
├── sizeBytes: long
├── width: int
├── height: int
├── authorId: UUID (FK)
└── uploadedAt: timestamp
```

## API Endpoints Summary

### Posts
- `POST /api/blog/posts` - Criar draft
- `GET /api/blog/posts/{slug}` - Buscar post
- `GET /api/blog/posts` - Listar posts
- `PUT /api/blog/posts/{slug}` - Atualizar post
- `DELETE /api/blog/posts/{slug}` - Deletar (soft)
- `POST /api/blog/posts/{slug}/publish` - Publicar
- `POST /api/blog/posts/{slug}/unpublish` - Despublicar

### Topics
- `POST /api/blog/posts/{slug}/topics` - Adicionar tópico
- `PUT /api/blog/posts/{slug}/topics/{id}` - Atualizar tópico
- `PUT /api/blog/posts/{slug}/topics/reorder` - Reordenar tópicos
- `DELETE /api/blog/posts/{slug}/topics/{id}` - Remover tópico

### Images
- `POST /api/blog/images` - Upload para S3
- `POST /api/blog/posts/{slug}/topics/{id}/images` - Associar a tópico
- `PUT /api/blog/posts/{slug}/topics/{id}/images/reorder` - Reordenar imagens
- `DELETE /api/blog/images/{id}` - Remover do S3

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Cloud**: AWS (S3, CloudFront, Lambda)
- **Database**: PostgreSQL
- **Persistence**: JdbcTemplate
- **Testing**: JUnit 5, AssertJ, TestContainers
- **Build**: Maven
- **Serverless**: Spring Cloud Function

## Como Usar Este Guia

1. **Leia a task completa** antes de começar a implementar
2. **Revise as dependências** e garanta que estão prontas
3. **Siga o Definition of Done** como checklist
4. **Use os exemplos de request/response** como contrato da API
5. **Implemente os testes** conforme especificado
6. **Faça code review** antes de marcar como completo

## Perguntas em Aberto

- [ ] Devemos ter versionamento de conteúdo (histórico de edições)?
- [ ] Precisamos de sistema de comentários nos posts?
- [ ] Devemos gerar thumbnails automáticos das imagens?
- [ ] Como lidar com imagens órfãs (uploaded mas nunca usadas)?
- [ ] Precisamos de analytics (visualizações, tempo de leitura)?
- [ ] Devemos ter drafts colaborativos (múltiplos autores)?
- [ ] Sistema de tags deve ter taxonomia/hierarquia?
