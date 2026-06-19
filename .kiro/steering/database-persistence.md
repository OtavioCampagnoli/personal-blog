---
inclusion: auto
---

# Database & Persistence Standards

## Context
Single PostgreSQL database with explicit SQL using JdbcTemplate. No ORM complexity.

## Database Strategy

### Single Database Architecture
- **ONE** PostgreSQL database for all modules
- **ONE** schema: `public` (default PostgreSQL schema)
- Table isolation via naming convention (prefixes)
- No multiple databases or schemas per module

### Table Naming Convention
Format: `<module>_<table_name>`

**Examples:**
- Blog module: `blog_posts`, `blog_categories`, `blog_tags`, `blog_comments`
- Auth module: `auth_users`, `auth_roles`, `auth_permissions`
- Analytics module: `analytics_events`, `analytics_sessions`

**Rules:**
- Table name MUST start with module prefix
- Never create tables with prefixes belonging to other modules
- Use snake_case for table and column names
- Be descriptive and clear

### Primary Keys
- Use **UUID** for all entity primary keys
- Generate UUIDs in application layer (before persistence)
- Column name: `id` (type: `UUID`)

```sql
CREATE TABLE IF NOT EXISTS blog_posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE NOT NULL,
    -- ...
);
```

## JdbcTemplate Persistence

### Why JdbcTemplate?
1. **Explicit SQL**: Full control over queries
2. **No ORM magic**: Predictable behavior
3. **Fast tests**: H2 in-memory for repository tests
4. **Clean architecture**: Domain independent of persistence technology
5. **Performance**: No hidden N+1 queries or lazy loading issues

### Repository Implementation Pattern
```java
// Domain package - Interface
public interface PostRepository {
    Post save(Post post);
    Optional<Post> findBySlug(String slug);
    List<Post> findPublished(Pagination pagination);
}

// Infrastructure package - Implementation
@Repository
public class JdbcPostRepository implements PostRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Post> postRowMapper;
    
    @Override
    public Post save(Post post) {
        String sql = """
            INSERT INTO blog_posts (id, title, slug, content, status, category_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                content = EXCLUDED.content,
                status = EXCLUDED.status
            """;
        
        jdbcTemplate.update(sql, 
            post.getId(),
            post.getTitle(),
            post.getSlug(),
            post.getContent(),
            post.getStatus().name(),
            post.getCategoryId(),
            post.getCreatedAt()
        );
        
        return post;
    }
    
    @Override
    public Optional<Post> findBySlug(String slug) {
        String sql = "SELECT * FROM blog_posts WHERE slug = ? AND deleted_at IS NULL";
        try {
            Post post = jdbcTemplate.queryForObject(sql, postRowMapper, slug);
            return Optional.of(post);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
```

### RowMapper Pattern
Create dedicated `RowMapper` implementations for complex mappings:
```java
@Component
public class PostRowMapper implements RowMapper<Post> {
    @Override
    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Post.builder()
            .id(UUID.fromString(rs.getString("id")))
            .title(rs.getString("title"))
            .slug(rs.getString("slug"))
            .content(rs.getString("content"))
            .status(PostStatus.valueOf(rs.getString("status")))
            .categoryId(UUID.fromString(rs.getString("category_id")))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .publishedAt(rs.getTimestamp("published_at") != null 
                ? rs.getTimestamp("published_at").toInstant() 
                : null)
            .build();
    }
}
```

## Database Migrations with Flyway

### Migration Location
- Module-specific directory: `src/main/resources/db/migration/<module>/`
- Example: `db/migration/blog/`, `db/migration/auth/`

### Naming Convention
**Versioned Migrations:**
- Pattern: `V<version>__<description>.sql`
- Examples:
  - `V001__create_blog_posts_table.sql`
  - `V002__add_published_at_to_posts.sql`
  - `V003__create_blog_categories_table.sql`

**Repeatable Migrations:**
- Pattern: `R__<description>.sql`
- Examples:
  - `R__create_blog_views.sql`
  - `R__create_blog_functions.sql`

### Idempotent Migrations (REQUIRED)
All migration scripts MUST be idempotent using PostgreSQL conditional clauses:

**Creating Tables:**
```sql
CREATE TABLE IF NOT EXISTS blog_posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    deleted_at TIMESTAMP
);
```

**Adding Columns:**
```sql
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'blog_posts' AND column_name = 'view_count'
    ) THEN
        ALTER TABLE blog_posts ADD COLUMN view_count INTEGER DEFAULT 0;
    END IF;
END $$;
```

**Creating Indexes:**
```sql
CREATE INDEX IF NOT EXISTS idx_blog_posts_slug ON blog_posts(slug);
CREATE INDEX IF NOT EXISTS idx_blog_posts_published_at ON blog_posts(published_at) 
    WHERE deleted_at IS NULL;
```

**Dropping Tables:**
```sql
DROP TABLE IF EXISTS blog_legacy_table CASCADE;
```

**Adding Constraints:**
```sql
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_blog_posts_category'
    ) THEN
        ALTER TABLE blog_posts 
        ADD CONSTRAINT fk_blog_posts_category 
        FOREIGN KEY (category_id) REFERENCES blog_categories(id);
    END IF;
END $$;
```

### Why Idempotent Migrations?
- Safe to run multiple times
- Easier rollback and recovery
- Support for blue-green deployments
- Development environment flexibility

## Referential Integrity

### Foreign Key Constraints
ALWAYS define foreign key constraints in the database:
```sql
ALTER TABLE blog_posts 
ADD CONSTRAINT fk_blog_posts_category 
FOREIGN KEY (category_id) REFERENCES blog_categories(id) 
ON DELETE RESTRICT;

ALTER TABLE blog_comments 
ADD CONSTRAINT fk_blog_comments_post 
FOREIGN KEY (post_id) REFERENCES blog_posts(id) 
ON DELETE CASCADE;
```

### Constraint Naming Convention
- Pattern: `fk_<table>_<referenced_table>`
- Example: `fk_blog_posts_category`, `fk_blog_comments_post`

## Soft Deletes

### Standard Pattern
Use `deleted_at` timestamp column for logical deletion:
```sql
CREATE TABLE blog_posts (
    id UUID PRIMARY KEY,
    -- ... other columns ...
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Soft delete
UPDATE blog_posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?;

-- Query only non-deleted
SELECT * FROM blog_posts WHERE deleted_at IS NULL;
```

### Indexing Soft Deletes
Create partial indexes to exclude deleted records:
```sql
CREATE INDEX idx_blog_posts_slug ON blog_posts(slug) 
WHERE deleted_at IS NULL;

CREATE INDEX idx_blog_posts_published ON blog_posts(published_at) 
WHERE deleted_at IS NULL AND status = 'PUBLISHED';
```

## Performance Indexes

### Indexing Strategy
Create indexes for:
1. **Unique constraints**: slug, email, username
2. **Foreign keys**: category_id, post_id, author_id
3. **Frequent queries**: published_at, status, created_at
4. **Filters**: WHERE clause columns used in common queries

**Examples:**
```sql
-- Unique business keys
CREATE UNIQUE INDEX IF NOT EXISTS idx_blog_posts_slug 
ON blog_posts(slug) WHERE deleted_at IS NULL;

-- Foreign keys
CREATE INDEX IF NOT EXISTS idx_blog_comments_post_id 
ON blog_comments(post_id) WHERE deleted_at IS NULL;

-- Query optimization
CREATE INDEX IF NOT EXISTS idx_blog_posts_published_at 
ON blog_posts(published_at DESC) 
WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- Composite indexes for complex queries
CREATE INDEX IF NOT EXISTS idx_blog_posts_category_published 
ON blog_posts(category_id, published_at DESC) 
WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
```

## Error Handling

### SQLException Translation
Catch and translate database exceptions to domain exceptions:
```java
try {
    jdbcTemplate.update(sql, params);
} catch (DataIntegrityViolationException e) {
    if (e.getMessage().contains("duplicate key")) {
        throw new DuplicateSlugException("Post with slug already exists");
    }
    throw new PersistenceException("Failed to save post", e);
}
```

### Transaction Management
- Use Spring's `@Transactional` on service methods
- Define transaction boundaries at application layer, not infrastructure
- Default propagation: `REQUIRED`
- Default isolation: `READ_COMMITTED`

```java
@Service
public class PostServiceImpl implements PostService {
    
    @Transactional
    public Post publish(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        
        post.publish(); // Domain logic
        postRepository.save(post);
        eventPublisher.publishEvent(new PostPublishedEvent(post.getId(), post.getSlug()));
        
        return post;
    }
}
```

## Testing Strategy

### Unit Tests (Service Layer)
- Mock repository interfaces
- NO database required
- Fast execution (< 100ms total)

### Repository Tests
- Use H2 in-memory database
- Use `@JdbcTest` annotation
- Load module-specific schema from Flyway migrations
- Test SQL correctness and mapping logic
- Target: < 2 seconds execution

```java
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {JdbcPostRepository.class, PostRowMapper.class})
@Sql(scripts = "/db/migration/blog/V001__create_blog_posts_table.sql")
class JdbcPostRepositoryTest {
    
    @Autowired
    private PostRepository repository;
    
    @Test
    void shouldSaveAndFindPost() {
        Post post = createTestPost();
        repository.save(post);
        
        Optional<Post> found = repository.findBySlug(post.getSlug());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo(post.getTitle());
    }
}
```

### Integration Tests (End-to-End)
- Use TestContainers with real PostgreSQL
- Test complete flows including database
- Target: < 30 seconds including container startup

```java
@SpringBootTest
@Testcontainers
class BlogIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void shouldPublishPost() {
        // Test complete flow
    }
}
```
