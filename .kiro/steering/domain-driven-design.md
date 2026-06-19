---
inclusion: auto
---

# Domain-Driven Design Standards

## Context
All domain logic must be clean, testable, and framework-independent following DDD principles.

## Domain Layer Rules

### Entity Validation
Every domain entity MUST:
1. Have a `validate()` method that checks all invariants
2. Call `validate()` in the constructor before completing object creation
3. Use an injected `Validator` interface (defined in domain, implemented in infrastructure)
4. Throw `DomainValidationException` with detailed field errors when validation fails

**Example Pattern:**
```java
// In domain package
public interface Validator {
    void validate(Object entity) throws DomainValidationException;
}

// In domain entity
public class Post {
    public Post(String title, String content, Category category, Validator validator) {
        this.title = title;
        this.content = content;
        this.category = category;
        validator.validate(this); // Called before construction completes
    }
}
```

### No Invalid State
- Domain entities MUST NEVER exist in an invalid state
- All validation MUST happen before persistence
- Constructors and setters MUST validate immediately

### Repository Interfaces in Domain
- Define `Repository_Interface` in domain package (e.g., `PostRepository`)
- Interfaces MUST NOT import JDBC, Spring Data, or any infrastructure classes
- Repository methods use domain entities and value objects only
- Implementation lives in infrastructure package

**Example:**
```java
// In domain package - GOOD ✅
package com.blog.blog.domain;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findBySlug(String slug);
    List<Post> findPublished(Pagination pagination);
}

// In domain package - BAD ❌
package com.blog.blog.domain;
import org.springframework.jdbc.core.JdbcTemplate; // NEVER!

public interface PostRepository { ... }
```

### Service Interfaces in Domain
- Define `Domain_Service_Interface` for all business logic services
- Keep implementations in application or infrastructure layer
- Allows mocking in unit tests without Spring context

**Example:**
```java
// In domain package
public interface PostService {
    Post createDraft(String title, String content, UUID categoryId);
    Post publish(UUID postId);
}

// In application package
@Service
public class PostServiceImpl implements PostService { ... }
```

## Testing Strategy

### Unit Tests (Domain Logic)
- Mock all `Repository_Interface` using Mockito
- NO `@SpringBootTest`
- NO `ApplicationContext`
- NO database required
- Target: < 100ms total execution time
- Focus: business rules, validation, entity behavior

### Repository Tests
- Use `H2_Database` in-memory
- Use `@JdbcTest` (NOT `@DataJpaTest`)
- Validate actual SQL queries against PostgreSQL-compatible schema
- Target: < 2 seconds execution time
- Focus: SQL correctness, mapping, constraints

### Integration Tests (End-to-End)
- Use TestContainers with real PostgreSQL
- Test complete flows through all layers
- Target: < 30 seconds including container startup
- Focus: full system behavior, actual database interactions

## Value Objects and Records

### Event Payloads
- Use immutable `record` types for all events
- Define records in `api` package
- Include only primitives, DTOs, or value objects

**Example:**
```java
// In api package
public record PostPublishedEvent(UUID postId, String slug, Instant publishedAt) {}
```

### DTOs for API Boundaries
- Use DTOs (records or POJOs) for Module Facade responses
- NEVER return domain entities from facades
- Keep DTOs in `api` package

## Domain Independence

### Forbidden Dependencies in Domain Package
Domain classes MUST NOT import:
- `java.sql.*`
- `org.springframework.jdbc.*`
- `org.springframework.data.*`
- `jakarta.persistence.*`
- Any framework-specific annotations (except for value injection in interfaces)

### Validator Abstraction
- Domain defines `Validator` interface
- Infrastructure provides implementation (Jakarta Bean Validation or custom)
- Swappable validation strategy without touching domain code

## Persistence Strategy

### JdbcTemplate for Everything
- All repositories use JdbcTemplate
- NO JPA, NO Hibernate, NO Spring Data JPA
- Write explicit SQL queries
- Full control over SQL and performance

### Why JdbcTemplate?
1. Explicit SQL control
2. Faster tests (H2 in-memory for repository tests)
3. No ORM complexity
4. Clean separation: domain defines contract, infrastructure implements
5. Easier to reason about database interactions

## Cobertura de Testes
- Minimum 80% line coverage per module
- Focus on critical business logic and edge cases
- Use mutation testing where applicable for high-risk code
