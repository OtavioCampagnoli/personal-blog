---
inclusion: auto
---

# Modular Architecture Standards

## Context
This project follows a **modular monolith** architecture pattern. All code must respect module boundaries and isolation rules defined here.

## Module Structure Rules

### Package Organization
- **Shared Module**: `com.blog.shared` - Contains shared code used by all modules (BaseEntity, Id<T>, common utilities)
- **Business Modules**: `com.blog.<module>` (e.g., `com.blog.blog`, `com.blog.auth`)
- Module names MUST be lowercase, single-word identifiers without separators
- Never create cross-module package dependencies (except Shared Module)

### Dependency Graph
```
Business Modules (blog, auth, etc.)
         ↓
   Shared Module
         ↓
External Libraries
```

**Rules:**
- Business modules CAN depend on Shared Module
- Shared Module CANNOT depend on any business module
- Business modules CANNOT depend on other business modules directly
- Use Module Facades for inter-module communication

### Layer Organization Within Modules
Each module MUST follow this layering structure:
```
com.blog.<module>/
├── api/           # Module's public API Contract (facades, DTOs, events)
├── application/   # Application services, use cases
├── domain/        # Domain entities, interfaces, business logic
└── infrastructure/ # Framework implementations, persistence, external integrations
```

**Dependency Rules:**
- `api` → `application` → `domain` → `infrastructure`
- Never reverse dependencies
- Domain MUST NOT depend on infrastructure
- Infrastructure implements interfaces defined in domain

## Module Boundaries

### Inter-Module Communication - CRITICAL RULES

✅ **ALLOWED:**
- Call other modules via their `Module_Facade` (defined in `api` package)
- Publish `Internal_Event` via Spring's `ApplicationEventPublisher`
- Listen to events from other modules via `@EventListener`

❌ **FORBIDDEN:**
- Direct access to another module's `Repository_Interface`
- Direct calls to another module's services
- Direct access to another module's domain entities
- Direct database access to tables owned by other modules
- Any import from `com.blog.<other_module>.application`, `.domain`, or `.infrastructure`

### Module Facade Pattern
- Each module MUST expose exactly one `Module_Facade` implementing an `API_Contract` interface
- The facade is the ONLY entry point for synchronous inter-module calls
- Facade MUST be annotated with `@Component` and live in the `api` package
- Facade methods MUST return DTOs, never domain entities

### Event-Driven Communication
- Use `Internal_Event` (records) for asynchronous module notifications
- Events MUST be immutable records in the `api` package
- Event payload MUST contain only primitives, DTOs, or value objects
- NEVER include domain entities in event payloads

## Database Isolation

### Table Naming Convention
- All tables MUST use module prefix: `<module>_<table_name>`
- Example: `blog_posts`, `blog_categories`, `auth_users`
- Single PostgreSQL database, single `public` schema
- NO separate schemas or databases per module

### Cross-Module Data Access
- NEVER query tables owned by other modules directly
- Use the owning module's `Module_Facade` for synchronous reads
- Subscribe to the owning module's events for notifications

## Architectural Testing

### ArchUnit Rules
All builds MUST pass ArchUnit tests enforcing:
1. No direct repository access across modules
2. Layer dependency rules respected
3. Domain layer has no infrastructure imports
4. Module boundaries not violated

If ArchUnit tests fail, the violation MUST be fixed immediately.

## Adding New Modules

When adding a new module:
1. Create package structure: `com.blog.<newmodule>/api/application/domain/infrastructure`
2. Define `API_Contract` interface in `api` package
3. Implement `Module_Facade` in `api` package
4. Create table prefix convention: `<newmodule>_*`
5. Add Flyway migration scripts in `db/migration/<newmodule>/`
6. Update bootstrapping configuration only (no changes to existing modules)

## Single Deployable Artifact
- The system MUST produce a single executable JAR
- No microservices orchestration
- No multiple processes required for production deployment


## Shared Module (`com.blog.shared`)

### Purpose
The Shared Module contains infrastructure code used by all business modules:
- Domain base classes (`BaseEntity<T>`, `Id<T>`)
- Common value objects
- Shared utilities
- Framework-independent abstractions

### What Belongs in Shared Module
✅ **ALLOWED:**
- `BaseEntity<T>` - Base class for all domain entities
- `Id<T>` - Type-safe identifier wrapper
- Common value objects (Money, Email, etc.)
- Generic utilities (StringUtils, DateUtils)
- Marker interfaces
- Domain exceptions base classes

❌ **FORBIDDEN:**
- Business logic specific to any module
- References to business modules (`com.blog.blog`, `com.blog.auth`, etc.)
- Framework-specific implementations (keep them generic)

### BaseEntity<T> Pattern

All domain entities MUST extend `BaseEntity<T>`:

```java
// In Shared Module (com.blog.shared.domain)
public abstract class BaseEntity<T> {
    private final Id<T> id;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    
    protected BaseEntity(Id<T> id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.deletedAt = null;
    }
    
    public Id<T> getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    
    protected void markAsUpdated() {
        this.updatedAt = Instant.now();
    }
    
    public void softDelete() {
        this.deletedAt = Instant.now();
    }
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

**Usage in business modules:**
```java
// In Blog Module (com.blog.blog.domain)
public class Post extends BaseEntity<Post> {
    private String title;
    private String slug;
    private String content;
    
    public Post(Id<Post> id, String title, String slug, String content) {
        super(id); // Initializes id, createdAt, updatedAt, deletedAt
        this.title = title;
        this.slug = slug;
        this.content = content;
    }
    
    public void updateContent(String newContent) {
        this.content = newContent;
        markAsUpdated(); // Updates updatedAt automatically
    }
}
```

### Id<T> Pattern

Type-safe identifier wrapper prevents mixing IDs between different entities:

```java
// In Shared Module (com.blog.shared.domain)
public final class Id<T> {
    private final UUID value;
    
    private Id(UUID value) {
        this.value = value;
    }
    
    public static <T> Id<T> generate() {
        return new Id<>(UUID.randomUUID());
    }
    
    public static <T> Id<T> of(UUID uuid) {
        return new Id<>(uuid);
    }
    
    public UUID value() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Id)) return false;
        Id<?> id = (Id<?>) o;
        return Objects.equals(value, id.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
```

**Benefits:**
```java
// Type safety prevents mistakes at compile time
Id<Post> postId = Id.generate();
Id<Category> categoryId = Id.generate();

// This compiles ✅
Post post = postRepository.findById(postId);

// This won't compile ❌
Post post = postRepository.findById(categoryId); // Compile error!
```

**Usage in repositories:**
```java
public interface PostRepository {
    Optional<Post> findById(Id<Post> id);  // Type-safe!
    Post save(Post post);
    void deleteById(Id<Post> id);
}
```

**Persistence layer conversion:**
```java
@Repository
public class JdbcPostRepository implements PostRepository {
    
    @Override
    public Optional<Post> findById(Id<Post> id) {
        String sql = "SELECT * FROM blog_posts WHERE id = ? AND deleted_at IS NULL";
        // Convert Id<Post> to UUID for database
        UUID uuid = id.value();
        // ... execute query ...
        // Convert UUID back to Id<Post> when reconstructing entity
        return Optional.of(new Post(Id.of(resultUuid), title, slug, content));
    }
}
```

### Audit Fields Pattern

All entities automatically get audit fields from `BaseEntity<T>`:

**Database Schema:**
```sql
CREATE TABLE blog_posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    
    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP  -- NULL = active, NOT NULL = soft deleted
);

-- Index for filtering active records
CREATE INDEX idx_blog_posts_deleted_at ON blog_posts(deleted_at) 
WHERE deleted_at IS NULL;
```

**Soft Delete Pattern:**
```java
// Delete entity (soft delete)
post.softDelete();
postRepository.save(post);

// Check if deleted
if (post.isDeleted()) {
    // Entity is soft deleted
}

// Repository query excludes deleted by default
String sql = """
    SELECT * FROM blog_posts 
    WHERE deleted_at IS NULL
    AND status = 'PUBLISHED'
    ORDER BY published_at DESC
    """;

// Special method to include deleted entities
public Optional<Post> findByIdIncludingDeleted(Id<Post> id) {
    String sql = "SELECT * FROM blog_posts WHERE id = ?";
    // No deleted_at filter
}
```

### ArchUnit Rules for Shared Module

```java
@Test
void sharedModuleShouldNotDependOnBusinessModules() {
    classes()
        .that().resideInAPackage("com.blog.shared..")
        .should().onlyDependOnClassesThat(
            resideInAnyPackage(
                "com.blog.shared..",
                "java..",
                "org.springframework..",
                "jakarta.."
            )
        )
        .check(importedClasses);
}

@Test
void allEntitiesShouldExtendBaseEntity() {
    classes()
        .that().resideInAPackage("..domain..")
        .and().haveSimpleNameEndingWith("Entity")
        .or().areAnnotatedWith(Entity.class)
        .should().beAssignableTo(BaseEntity.class)
        .check(importedClasses);
}
```
