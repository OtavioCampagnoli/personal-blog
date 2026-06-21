# Quick Start - Personal Blog

## Pré-requisitos

- Java 21
- Maven 3.8+
- (Opcional) IntelliJ IDEA ou VS Code

**Nota**: Usando H2 Database (in-memory), não precisa instalar banco de dados! 🎉

## Como Executar

### 1. Build do projeto

```bash
mvn clean install
```

### 2. Executar a aplicação

```bash
mvn spring-boot:run
```

A aplicação estará rodando em: **http://localhost:8080**

### 3. Acessar o console do H2 (opcional)

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:blogdb`
- Username: `sa`
- Password: (deixar em branco)

## Testando a API

### Criar um post (Task 01)

```bash
curl -X POST http://localhost:8080/api/blog/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Meu Primeiro Post sobre Java",
    "description": "Aprenda os conceitos fundamentais do Java",
    "categoryId": "11111111-1111-1111-1111-111111111111",
    "tags": ["java", "spring-boot", "backend"]
  }'
```

**Response esperado (201 Created):**
```json
{
  "id": "...",
  "title": "Meu Primeiro Post sobre Java",
  "slug": "meu-primeiro-post-sobre-java",
  "description": "Aprenda os conceitos fundamentais do Java",
  "status": "DRAFT",
  "categoryId": "11111111-1111-1111-1111-111111111111",
  "tags": ["java", "spring-boot", "backend"],
  "topicCount": 0,
  "createdAt": "2026-06-18T...",
  "updatedAt": "2026-06-18T...",
  "publishedAt": null,
  "_links": {
    "self": { "href": "http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java" },
    "add-topic": { "href": "http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java/topics" },
    "publish": { "href": "http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java/publish" },
    "edit": { "href": "http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java" },
    "delete": { "href": "http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java" },
    "category": { "href": "http://localhost:8080/api/blog/categories/11111111-1111-1111-1111-111111111111" }
  }
}
```

### Buscar o post criado (Task 02)

```bash
curl http://localhost:8080/api/blog/posts/meu-primeiro-post-sobre-java
```

### Testar validações (erro esperado)

```bash
# Título muito curto (< 10 caracteres)
curl -X POST http://localhost:8080/api/blog/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Java",
    "categoryId": "11111111-1111-1111-1111-111111111111"
  }'
```

**Response esperado (400 Bad Request):**
```json
{
  "timestamp": "2026-06-18T...",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERRORS",
  "message": "Validation errors in the provided fields",
  "errors": [
    {
      "field": "title",
      "code": "Size",
      "message": "Title must be between 10 and 200 characters",
      "providedValue": "Java",
      "example": null
    }
  ]
}
```

## Dados Iniciais

O banco vem com:

- **Author**: `otavio@example.com` (ID: `00000000-0000-0000-0000-000000000001`)
- **Category**: `Uncategorized` (ID: `11111111-1111-1111-1111-111111111111`)

## Estrutura da API

### Endpoints implementados (Task 01 & 02)

- `POST /api/blog/posts` - Criar post draft
- `GET /api/blog/posts/{slug}` - Buscar post

### Próximos endpoints (Tasks futuras)

- `POST /api/blog/posts/{slug}/topics` - Adicionar tópico (Task 03)
- `POST /api/blog/images` - Upload de imagem (Task 04)
- `POST /api/blog/posts/{slug}/topics/{topicId}/images` - Associar imagem (Task 05)
- `POST /api/blog/posts/{slug}/publish` - Publicar post
- `GET /api/blog/posts` - Listar posts

## Troubleshooting

### Erro de compilação Maven

```bash
# Limpar e recompilar
mvn clean install -U
```

### Verificar tabelas no H2

1. Acesse http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:blogdb`
3. Username: `sa`, Password: (vazio)
4. Execute: `SELECT * FROM posts;`

### Flyway migration error

O H2 recria o banco toda vez que reinicia a aplicação (in-memory).
Apenas reinicie a aplicação.

## Desenvolvimento

### Rodar testes

```bash
mvn test
```

### Build sem testes

```bash
mvn clean install -DskipTests
```

### Hot reload (DevTools)

O Spring Boot DevTools está configurado. Qualquer mudança em código Java será recarregada automaticamente.

## Tecnologias Usadas

- **Java 21** com features modernas (Records, Text Blocks)
- **Spring Boot 3.2.5** (Web, JDBC, Validation, Security)
- **H2 Database** (in-memory) - sem necessidade de instalação!
- **Flyway** para migrations
- **JdbcTemplate** para persistência
- **Maven** como build tool

## Próximos Passos

1. ✅ **Task 01** - Create Post Draft (DONE)
2. ✅ **Task 02** - Get Post (DONE - básico)
3. 🚧 **Task 03** - Add Topic to Post
4. 🚧 **Task 04** - Upload Image to S3
5. 🚧 **Task 05** - Add Image to Topic

Ver tasks completas em `.kiro/specs/blog-mvp/`
