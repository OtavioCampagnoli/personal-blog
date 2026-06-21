---
inclusion: auto
---

# Testing Strategy

## Context
Comprehensive testing approach covering unit, integration, and repository layers with 80% minimum coverage.

## Test Pyramid

```
        /\
       /  \
      / E2E \       <- Few, slow, high confidence
     /-------\
    /  Integration \ <- Some, medium speed
   /---------------\
  /   Unit Tests    \ <- Many, fast, focused
 /-------------------\
```

## Unit Tests (Domain & Application Layer)

### Characteristics
- **Fast**: < 100ms total execution time
- **Isolated**: No Spring context, no database
- **Focused**: One class, one responsibility
- **Mocked**: All dependencies mocked with Mockito

### What to Test
- Domain entity validation logic
- Business rules and invariants
- Service orchestration
- Edge cases and error scenarios

### Unit Test Template
```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceTest {
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private SlugGenerator slugGenerator;
    
    @Mock
    private Validator validator;
    
    private PostService postService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        postService = new PostServiceImpl(
            postRepository,
            categoryRepository,
            slugGenerator,
            validator
        );
    }
    
    @Test
    @DisplayName("Should create post with generated slug")
    void shouldCreatePostWithGeneratedSlug() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Java", "Java posts");
        
        when(categoryRepository.findById(categoryId))
            .thenReturn(Optional.of(category));
        when(slugGenerator.generate("My Post Title"))
            .thenReturn("my-post-title");
        
        // Act
        Post result = postService.createPost(
            "My Post Title", 
            "Post content here", 
            categoryId
        );
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("my-post-title");
        assertThat(result.getStatus()).isEqualTo(PostStatus.DRAFT);
        
        verify(postRepository, times(1)).save(any(Post.class));
        verify(slugGenerator, times(1)).generate("My Post Title");
    }
    
    @Test
    @DisplayName("Should throw exception when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            postService.createPost("Title", "Content", categoryId)
        )
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessageContaining(categoryId.toString());
        
        verify(postRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should publish draft post and set published timestamp")
    void shouldPublishDraftPost() {
        // Arrange
        UUID postId = UUID.randomUUID();
        Post draftPost = Post.builder()
            .id(postId)
            .title("Draft Post")
            .content("Content")
            .status(PostStatus.DRAFT)
            .build();
        
        when(postRepository.findById(postId))
            .thenReturn(Optional.of(draftPost));
        
        // Act
        Post result = postService.publish(postId);
        
        // Assert
        assertThat(result.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(result.getPublishedAt()).isNotNull();
        
        verify(postRepository).save(argThat(post -> 
            post.getStatus() == PostStatus.PUBLISHED &&
            post.getPublishedAt() != null
        ));
    }
}
```

### Domain Entity Validation Tests
```java
class PostTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        validator = mock(Validator.class);
    }
    
    @Test
    @DisplayName("Should reject post with empty title")
    void shouldRejectEmptyTitle() {
        // Arrange
        doThrow(new DomainValidationException(Map.of("title", "Title is required")))
            .when(validator).validate(any());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            new Post("", "Content", category, validator)
        )
            .isInstanceOf(DomainValidationException.class)
            .extracting("fieldErrors")
            .satisfies(errors -> {
                Map<String, String> errorMap = (Map<String, String>) errors;
                assertThat(errorMap).containsKey("title");
            });
    }
    
    @Test
    @DisplayName("Should generate slug on post creation")
    void shouldGenerateSlugOnCreation() {
        // Arrange & Act
        Post post = Post.builder()
            .title("My Amazing Post")
            .slug("my-amazing-post")
            .content("Content")
            .build();
        
        // Assert
        assertThat(post.getSlug()).isEqualTo("my-amazing-post");
    }
}
```

## Repository Tests (Infrastructure Layer)

### Characteristics
- **Medium speed**: < 2 seconds execution time
- **In-memory H2**: PostgreSQL-compatible mode
- **Real SQL**: Tests actual queries and mappings
- **@JdbcTest**: Minimal Spring context for JDBC

### What to Test
- SQL query correctness
- Entity-to-row mapping (RowMapper)
- Database constraints (unique, foreign keys)
- Pagination and sorting
- Complex queries (joins, filters)

### Repository Test Template
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@Import({JdbcPostRepository.class, PostRowMapper.class})
@Sql(scripts = {
    "/db/migration/blog/V001__create_blog_posts_table.sql",
    "/db/migration/blog/V002__create_blog_categories_table.sql"
})
class JdbcPostRepositoryTest {
    
    @Autowired
    private PostRepository repository;
    
    @Test
    @DisplayName("Should save and retrieve post by slug")
    void shouldSaveAndRetrievePostBySlug() {
        // Arrange
        Post post = Post.builder()
            .id(UUID.randomUUID())
            .title("Test Post")
            .slug("test-post")
            .content("Test content")
            .status(PostStatus.DRAFT)
            .categoryId(UUID.randomUUID())
            .createdAt(Instant.now())
            .build();
        
        // Act
        repository.save(post);
        Optional<Post> found = repository.findBySlug("test-post");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Post");
        assertThat(found.get().getStatus()).isEqualTo(PostStatus.DRAFT);
    }
    
    @Test
    @DisplayName("Should return published posts only")
    void shouldReturnPublishedPostsOnly() {
        // Arrange
        saveDraftPost("draft-1");
        savePublishedPost("published-1");
        savePublishedPost("published-2");
        
        // Act
        List<Post> published = repository.findPublished(
            Pagination.of(0, 10)
        );
        
        // Assert
        assertThat(published).hasSize(2);
        assertThat(published)
            .allMatch(p -> p.getStatus() == PostStatus.PUBLISHED);
    }
    
    @Test
    @DisplayName("Should enforce unique slug constraint")
    void shouldEnforceUniqueSlugConstraint() {
        // Arrange
        Post post1 = createPost("duplicate-slug");
        repository.save(post1);
        
        Post post2 = createPost("duplicate-slug");
        
        // Act & Assert
        assertThatThrownBy(() -> repository.save(post2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("duplicate key");
    }
    
    @Test
    @DisplayName("Should paginate results correctly")
    void shouldPaginateResultsCorrectly() {
        // Arrange
        for (int i = 0; i < 25; i++) {
            savePublishedPost("post-" + i);
        }
        
        // Act
        List<Post> page1 = repository.findPublished(Pagination.of(0, 10));
        List<Post> page2 = repository.findPublished(Pagination.of(1, 10));
        List<Post> page3 = repository.findPublished(Pagination.of(2, 10));
        
        // Assert
        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(10);
        assertThat(page3).hasSize(5);
    }
    
    @Test
    @DisplayName("Should soft delete post")
    void shouldSoftDeletePost() {
        // Arrange
        Post post = createPost("to-delete");
        repository.save(post);
        
        // Act
        repository.delete(post.getId());
        Optional<Post> found = repository.findBySlug("to-delete");
        
        // Assert
        assertThat(found).isEmpty();
        
        // Verify soft delete (deleted_at is set)
        Optional<Post> foundWithDeleted = repository.findByIdIncludingDeleted(post.getId());
        assertThat(foundWithDeleted).isPresent();
        assertThat(foundWithDeleted.get().getDeletedAt()).isNotNull();
    }
}
```

## Integration Tests (End-to-End)

### Characteristics
- **Slow**: < 30 seconds including container startup
- **Real PostgreSQL**: TestContainers
- **Full Spring context**: All layers loaded
- **Complete flows**: From HTTP request to database

### What to Test
- Complete user journeys
- API contracts (request/response)
- Transaction boundaries
- Event publishing and handling
- Error handling across layers

### Integration Test Template
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
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
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Should create, publish and retrieve post")
    void shouldCreatePublishAndRetrievePost() throws Exception {
        // 1. Create category
        String categoryRequest = """
            {
                "name": "Java",
                "description": "Java programming"
            }
            """;
        
        String categoryResponse = mockMvc.perform(post("/api/blog/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Java"))
            .andReturn().getResponse().getContentAsString();
        
        String categoryId = JsonPath.read(categoryResponse, "$.id");
        
        // 2. Create draft post
        String postRequest = """
            {
                "title": "My First Java Post",
                "content": "This is the content of my first post",
                "categoryId": "%s"
            }
            """.formatted(categoryId);
        
        String postResponse = mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("my-first-java-post"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$._links.self.href").exists())
            .andExpect(jsonPath("$._links.publish.href").exists())
            .andReturn().getResponse().getContentAsString();
        
        String slug = JsonPath.read(postResponse, "$.slug");
        
        // 3. Publish post
        mockMvc.perform(post("/api/blog/posts/{slug}/publish", slug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.publishedAt").exists())
            .andExpect(jsonPath("$._links.unpublish.href").exists());
        
        // 4. Retrieve published post
        mockMvc.perform(get("/api/blog/posts/{slug}", slug)
                .header("Accept-Language", "en-US"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("My First Java Post"))
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$._links.self.href").value(containsString(slug)))
            .andExpect(jsonPath("$._links.category.href").exists());
    }
    
    @Test
    @DisplayName("Should return validation errors in Portuguese")
    void shouldReturnValidationErrorsInPortuguese() throws Exception {
        String invalidRequest = """
            {
                "title": "Short",
                "content": "",
                "categoryId": null
            }
            """;
        
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "pt-BR")
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value(containsString("validação")))
            .andExpect(jsonPath("$.details.title").exists())
            .andExpect(jsonPath("$.details.content").exists());
    }
    
    @Test
    @DisplayName("Should return HATEOAS links for paginated results")
    void shouldReturnHateoasLinksForPaginatedResults() throws Exception {
        // Create 15 published posts
        for (int i = 0; i < 15; i++) {
            createAndPublishPost("Post " + i);
        }
        
        // Request page 1 (second page)
        mockMvc.perform(get("/api/blog/posts")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.posts").isArray())
            .andExpect(jsonPath("$._embedded.posts.length()").value(5))
            .andExpect(jsonPath("$.page.size").value(10))
            .andExpect(jsonPath("$.page.totalElements").value(15))
            .andExpect(jsonPath("$.page.totalPages").value(2))
            .andExpect(jsonPath("$._links.self.href").exists())
            .andExpect(jsonPath("$._links.first.href").exists())
            .andExpect(jsonPath("$._links.prev.href").exists())
            .andExpect(jsonPath("$._links.last.href").exists());
    }
}
```

## Test Coverage

### Minimum Requirements
- **Overall**: 80% line coverage per module
- **Domain layer**: 90%+ (critical business logic)
- **Application layer**: 85%+
- **Infrastructure layer**: 75%+
- **API layer**: 80%+

### Coverage Tools
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>coverage-check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Test Naming

### Class Names
- Unit: `<ClassUnderTest>Test`
- Repository: `Jdbc<Entity>RepositoryTest`
- Integration: `<Feature>IntegrationTest`

### Method Names
Use descriptive names with `@DisplayName`:
```java
@Test
@DisplayName("Should create post with generated slug when title is provided")
void shouldCreatePostWithGeneratedSlugWhenTitleProvided() { }

@Test
@DisplayName("Should throw CategoryNotFoundException when category does not exist")
void shouldThrowCategoryNotFoundExceptionWhenCategoryDoesNotExist() { }
```

## Test Data Builders

### Use Builder Pattern
```java
class PostTestBuilder {
    private UUID id = UUID.randomUUID();
    private String title = "Default Title";
    private String slug = "default-slug";
    private String content = "Default content";
    private PostStatus status = PostStatus.DRAFT;
    private UUID categoryId = UUID.randomUUID();
    
    public static PostTestBuilder aPost() {
        return new PostTestBuilder();
    }
    
    public PostTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }
    
    public PostTestBuilder withSlug(String slug) {
        this.slug = slug;
        return this;
    }
    
    public PostTestBuilder published() {
        this.status = PostStatus.PUBLISHED;
        return this;
    }
    
    public Post build() {
        return Post.builder()
            .id(id)
            .title(title)
            .slug(slug)
            .content(content)
            .status(status)
            .categoryId(categoryId)
            .build();
    }
}

// Usage
Post post = PostTestBuilder.aPost()
    .withTitle("Custom Title")
    .published()
    .build();
```

## Assertion Libraries

### AssertJ (Recommended)
```java
import static org.assertj.core.api.Assertions.*;

assertThat(post.getTitle()).isEqualTo("Expected Title");
assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
assertThat(post.getPublishedAt()).isNotNull();

assertThat(posts)
    .hasSize(3)
    .extracting(Post::getStatus)
    .containsOnly(PostStatus.PUBLISHED);

assertThatThrownBy(() -> postService.publish(invalidId))
    .isInstanceOf(PostNotFoundException.class)
    .hasMessageContaining("not found");
```

## Running Tests

### Maven Commands
```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dgroups=unit

# Run only integration tests
mvn test -Dgroups=integration

# Run with coverage
mvn clean verify

# Skip tests
mvn clean install -DskipTests
```

### Test Execution Time Targets
- Unit tests: < 100ms total
- Repository tests: < 2 seconds per module
- Integration tests: < 30 seconds per module
- Full test suite: < 5 minutes
