---
inclusion: auto
---

# REST API Standards

## Context
All REST endpoints must follow Level 3 REST maturity (HATEOAS), support i18n, and provide excellent UX.

## API Structure

### Endpoint Naming
- Module prefix pattern: `/api/<module>/` (e.g., `/api/blog/`)
- Use plural nouns for collections: `/api/blog/posts`
- Use singular for specific resources: `/api/blog/posts/{slug}`
- Nested resources when relationship is clear: `/api/blog/posts/{slug}/comments`

### HTTP Methods & Status Codes
- `GET`: Retrieve resources (200 OK, 404 Not Found)
- `POST`: Create resources (201 Created with Location header)
- `PUT`: Full update (200 OK or 204 No Content)
- `PATCH`: Partial update (200 OK or 204 No Content)
- `DELETE`: Remove resources (204 No Content)
- Validation errors: 400 Bad Request
- Business rule violations: 422 Unprocessable Entity
- Internal errors: 500 Internal Server Error

## Internationalization (i18n)

### Supported Languages
- `en-US` (English - US) - DEFAULT
- `pt-BR` (Portuguese - Brazil)
- `es-ES` (Spanish - Spain)

### MessageSource Configuration
- All messages MUST come from MessageSource
- NEVER hardcode strings in Java code
- Location: `src/main/resources/i18n/messages_{locale}.properties`
- Required files:
  - `messages_en_US.properties`
  - `messages_pt_BR.properties`
  - `messages_es_ES.properties`

### Language Detection
1. Read `Accept-Language` header from request
2. If supported locale (`en-US`, `pt-BR`, `es-ES`): use it
3. If unsupported or missing: fallback to `en-US`
4. Return all messages in selected language

### Message Key Convention
Use structured, hierarchical keys:
```properties
# Validation errors
validation.title.required=Title is required
validation.title.tooShort=Title must be at least {0} characters (provided: {1})
validation.email.invalid=Invalid email format. Example: user@example.com

# Business errors
business.category.inUse=Cannot delete category "{0}" because it has {1} associated posts
business.post.notFound=Post with slug "{0}" not found
business.post.alreadyPublished=Post is already published

# Success messages
success.post.created=Post created successfully
success.post.published=Post "{0}" published successfully at {1}
```

### Error Response Format
All error responses MUST include:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_TITLE_TOO_SHORT",
  "message": "O título deve ter pelo menos 10 caracteres (fornecido: 5)",
  "details": {
    "field": "title",
    "providedValue": "Test",
    "expectedValue": "min 10 characters",
    "example": "My First Blog Post"
  },
  "_links": {
    "docs": { "href": "/api/blog/docs" }
  }
}
```

### UX-Focused Messages
Messages MUST be:
1. **Friendly**: Use natural language, avoid jargon
2. **Direct**: Get to the point quickly
3. **Detailed**: Explain what's wrong AND how to fix it
4. **Actionable**: Include examples of valid values
5. **Consistent**: Same structure across all 3 languages

**Bad Message:**
```
"Invalid input"
```

**Good Message (en-US):**
```
"Title must be between 10 and 200 characters. You provided 5 characters. Example: 'My First Blog Post'"
```

**Good Message (pt-BR):**
```
"O título deve ter entre 10 e 200 caracteres. Você forneceu 5 caracteres. Exemplo: 'Meu Primeiro Post'"
```

### Parametrized Messages
Use MessageSource placeholders for dynamic values:
```properties
# English
validation.title.length=Title must be between {0} and {1} characters (provided: {2})

# Portuguese
validation.title.length=O título deve ter entre {0} e {1} caracteres (fornecido: {2})

# Spanish
validation.title.length=El título debe tener entre {0} y {1} caracteres (proporcionado: {2})
```

```java
String message = messageSource.getMessage(
    "validation.title.length",
    new Object[]{10, 200, title.length()},
    locale
);
```

## HATEOAS (REST Level 3)

### Framework
- Use **Spring HATEOAS** library
- Return responses in **HAL** (Hypertext Application Language) format
- All entity responses MUST include `_links` section

### Link Relations (rel)
Standard relations for entities:

**Post Resource:**
- `self`: Link to the post itself
- `edit`: Link to update the post (only if user has permission)
- `delete`: Link to delete the post (only if user has permission)
- `publish`: Link to publish draft (only if status = Draft)
- `unpublish`: Link to unpublish (only if status = Published)
- `comments`: Link to post's comments
- `category`: Link to post's category
- `author`: Link to post's author

**Category Resource:**
- `self`: Link to the category
- `edit`: Link to update category
- `delete`: Link to delete category (only if no posts)
- `posts`: Link to all posts in this category

**Comment Resource:**
- `self`: Link to the comment
- `delete`: Link to delete comment
- `approve`: Link to approve (only if status = Pending)
- `reject`: Link to reject (only if status = Pending)

**Collection (Paginated):**
- `self`: Current page
- `first`: First page
- `last`: Last page
- `next`: Next page (only if exists)
- `prev`: Previous page (only if exists)

### HAL Response Structure
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "My Blog Post",
  "slug": "my-blog-post",
  "content": "...",
  "status": "PUBLISHED",
  "publishedAt": "2024-01-15T10:30:00Z",
  "_links": {
    "self": {
      "href": "/api/blog/posts/my-blog-post"
    },
    "edit": {
      "href": "/api/blog/posts/my-blog-post",
      "type": "application/json",
      "title": "Edit this post"
    },
    "delete": {
      "href": "/api/blog/posts/my-blog-post",
      "type": "application/json"
    },
    "unpublish": {
      "href": "/api/blog/posts/my-blog-post/unpublish",
      "type": "application/json"
    },
    "comments": {
      "href": "/api/blog/posts/my-blog-post/comments"
    },
    "category": {
      "href": "/api/blog/categories/java"
    }
  }
}
```

### State-Aware Links
Links MUST be conditional based on resource state:
- Draft post: include `publish` link, exclude `unpublish`
- Published post: include `unpublish` link, exclude `publish`
- Category with posts: exclude `delete` link
- Pending comment: include `approve` and `reject` links

### Implementation Classes
Use Spring HATEOAS classes:
- `RepresentationModel`: Base for custom representations
- `EntityModel<T>`: Wraps single entity with links
- `CollectionModel<T>`: Wraps collection with links
- `PagedModel<T>`: Wraps paginated collection with navigation links

```java
EntityModel<PostDTO> resource = EntityModel.of(postDTO);
resource.add(linkTo(methodOn(PostController.class).getPost(slug)).withSelfRel());
resource.add(linkTo(methodOn(PostController.class).updatePost(slug)).withRel("edit"));
```

### API Root Endpoint
`GET /api/blog` MUST return links to all main resources:
```json
{
  "_links": {
    "posts": { "href": "/api/blog/posts" },
    "categories": { "href": "/api/blog/categories" },
    "tags": { "href": "/api/blog/tags" },
    "comments": { "href": "/api/blog/comments" }
  }
}
```

## Pagination

### Query Parameters
- `page`: Page number (0-indexed, default: 0)
- `size`: Items per page (default: 10, max: 50)
- `sort`: Sort field and direction (e.g., `publishedAt,desc`)

### Response Metadata
Include pagination metadata in response:
```json
{
  "_embedded": {
    "posts": [ /* array of posts */ ]
  },
  "page": {
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "number": 0
  },
  "_links": {
    "self": { "href": "/api/blog/posts?page=0&size=10" },
    "first": { "href": "/api/blog/posts?page=0&size=10" },
    "next": { "href": "/api/blog/posts?page=1&size=10" },
    "last": { "href": "/api/blog/posts?page=4&size=10" }
  }
}
```

### Page Size Enforcement
- If requested size > 50: return size=50 and include warning in response
- Never exceed maximum page size for performance reasons

## Date Format
- Use **ISO 8601** format: `2024-01-15T10:30:00Z`
- Always include timezone (UTC preferred)
- Accept and return dates in consistent format across all endpoints

## Documentation
- Use **OpenAPI 3.0** for API documentation
- Auto-generate from annotations
- Available at `/api/blog/docs` in development
- Include examples for all request/response formats
- Document all error codes and messages
