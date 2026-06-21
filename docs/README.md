# Personal Blog - MVP

## Visão Geral

Sistema de blog pessoal com suporte a posts compostos por múltiplos tópicos, cada um podendo conter texto em Markdown e imagens.

## Arquitetura

- **Backend**: Serverless (AWS Lambda / API Gateway)
- **Storage**: S3 para imagens
- **Database**: DynamoDB ou PostgreSQL (definir)
- **Linguagem**: Java com Spring Boot

## Conceitos do Domínio

### Post
Um post é composto por:
- Metadados (título, slug, status, datas)
- Lista ordenada de **Tópicos**

### Tópico
Um tópico representa uma seção do post e contém:
- Ordem de exibição
- Conteúdo em Markdown
- Lista de imagens (opcional)
- Cada imagem tem uma posição específica no tópico

### Image
Representa uma imagem armazenada:
- URL no S3
- Metadados (alt text, dimensões, ordem)

## Funcionalidades MVP

### Fase 1 - CRUD Básico de Posts
- [x] Criar post em draft
- [x] Buscar post por ID/slug
- [x] Listar posts
- [x] Atualizar post
- [x] Deletar post (soft delete)

### Fase 2 - Tópicos
- [ ] Adicionar tópico a um post
- [ ] Reordenar tópicos
- [ ] Editar conteúdo Markdown de um tópico
- [ ] Remover tópico

### Fase 3 - Imagens
- [ ] Upload de imagem para S3
- [ ] Associar imagem a um tópico
- [ ] Posicionar imagem no tópico
- [ ] Remover imagem

### Fase 4 - Publicação
- [ ] Publicar post (DRAFT → PUBLISHED)
- [ ] Despublicar post

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── otavio/
│   │           └── blog/
│   │               ├── domain/
│   │               │   ├── post/
│   │               │   │   ├── Post.java
│   │               │   │   ├── PostStatus.java
│   │               │   │   ├── PostRepository.java
│   │               │   │   └── PostService.java
│   │               │   ├── topic/
│   │               │   │   ├── Topic.java
│   │               │   │   ├── TopicRepository.java
│   │               │   │   └── TopicService.java
│   │               │   └── image/
│   │               │       ├── Image.java
│   │               │       ├── ImageRepository.java
│   │               │       └── ImageService.java
│   │               ├── application/
│   │               │   └── api/
│   │               │       ├── PostController.java
│   │               │       └── dto/
│   │               └── infrastructure/
│   │                   ├── persistence/
│   │                   └── storage/
│   └── resources/
│       └── application.yml
└── test/
```

## Princípios de Design

1. **Domain-Driven Design**: Agregados bem definidos, linguagem ubíqua
2. **Clean Architecture**: Separação de camadas (domain, application, infrastructure)
3. **Notification Pattern**: Acumulação de erros de validação
4. **Value Objects**: Para IDs, Slugs, etc.
5. **Soft Delete**: Nunca deletar dados permanentemente
6. **HATEOAS**: Hypermedia links nas respostas da API
7. **i18n**: Suporte a múltiplos idiomas (en-US, pt-BR, es-ES)

## Stack Tecnológico

- Java 21
- Spring Boot 3.x
- Spring Cloud Function (para serverless)
- AWS SDK (S3, Lambda)
- JdbcTemplate (persistência)
- TestContainers (testes de integração)
- JUnit 5 + AssertJ (testes)

## Como Executar

```bash
# Build
./mvnw clean package

# Executar localmente
./mvnw spring-boot:run

# Deploy serverless
sam deploy
```

## API Endpoints

### Posts
- `POST /api/blog/posts` - Criar post draft
- `GET /api/blog/posts/{slug}` - Buscar post
- `GET /api/blog/posts` - Listar posts
- `PUT /api/blog/posts/{slug}` - Atualizar post
- `DELETE /api/blog/posts/{slug}` - Deletar post
- `POST /api/blog/posts/{slug}/publish` - Publicar post

### Tópicos
- `POST /api/blog/posts/{slug}/topics` - Adicionar tópico
- `PUT /api/blog/posts/{slug}/topics/{topicId}` - Atualizar tópico
- `PUT /api/blog/posts/{slug}/topics/reorder` - Reordenar tópicos
- `DELETE /api/blog/posts/{slug}/topics/{topicId}` - Remover tópico

### Imagens
- `POST /api/blog/images` - Upload de imagem
- `POST /api/blog/posts/{slug}/topics/{topicId}/images` - Associar imagem
- `DELETE /api/blog/images/{imageId}` - Remover imagem

## ✅ Status da Implementação

### Task 01: Create Post Draft - **COMPLETO**
- ✅ Post entity com validações DDD
- ✅ PostRepository (JdbcTemplate)
- ✅ PostService com lógica de slug único
- ✅ PostController com endpoint POST
- ✅ Global Exception Handler (RFC 7807)
- ✅ DTOs com validação Jakarta
- ✅ HATEOAS links na resposta
- ✅ Migration Flyway com schema completo

**Endpoint funcionando**: `POST /api/blog/posts`

Ver [QUICKSTART.md](./QUICKSTART.md) para testar!

## Tasks Criadas

Ver todas as tasks detalhadas em [`.kiro/specs/blog-mvp/`](./.kiro/specs/blog-mvp/)

### Próximas 5 Tasks Prioritárias:

1. **[Task 00: Shared Module](./. kiro/specs/blog-mvp/00-shared-module.md)** - BaseEntity, Id<T>, Notification Pattern (3 dias)
2. **[Task 01: Create Post Draft](./.kiro/specs/blog-mvp/01-create-post-draft.md)** - POST /api/blog/posts (2 dias)
3. **[Task 02: Get Post](./.kiro/specs/blog-mvp/02-get-post.md)** - GET /api/blog/posts/{slug} (1.5 dias)
4. **[Task 03: Add Topic](./.kiro/specs/blog-mvp/03-add-topic-to-post.md)** - POST /api/blog/posts/{slug}/topics (2 dias)
5. **[Task 04: Upload Image](./.kiro/specs/blog-mvp/04-upload-image-to-s3.md)** - POST /api/blog/images (2.5 dias)

**Total MVP**: ~24.5 dias de desenvolvimento

## Como Começar

1. **Setup inicial**:
   ```bash
   # Instalar dependências
   mvn clean install
   
   # Configurar banco de dados
   docker-compose up -d postgres
   
   # Configurar AWS credentials
   aws configure
   ```

2. **Seguir ordem de implementação**:
   - Começar pelo Shared Module (Task 00)
   - Implementar Category e Author entities
   - Configurar autenticação
   - Seguir tasks numeradas em ordem

3. **Para cada task**:
   - Ler a task completa
   - Verificar dependências
   - Implementar seguindo o Definition of Done
   - Escrever testes
   - Code review
