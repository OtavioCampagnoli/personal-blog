---
inclusion: auto
---

# Java & Spring Boot Standards

## Context
Java 21 with Spring Boot, following modern Java practices and Spring best practices.

## Java Version & Features

### Java 21
Use modern Java 21 features:
- **Records**: For immutable DTOs, events, value objects
- **Sealed classes**: For restricted type hierarchies
- **Pattern matching**: For instanceof and switch
- **Text blocks**: For SQL queries and JSON/XML templates
- **Virtual threads**: Consider for high-concurrency scenarios

**Example - Records for DTOs:**
```java
public record PostDTO(
    UUID id,
    String title,
    String slug,
    String content,
    String status,
    Instant publishedAt
) {}
```

**Example - Text blocks for SQL:**
```java
String sql = """
    SELECT p.*, c.name as category_name
    FROM blog_posts p
    INNER JOIN blog_categories c ON p.category_id = c.id
    WHERE p.deleted_at IS NULL
    AND p.status = ?
    ORDER BY p.published_at DESC
    """;
```

**Example - Pattern matching:**
```java
if (result instanceof PostDTO post && post.status().equals("PUBLISHED")) {
    // Use post directly
}
```

## Package Structure per Module

```
com.blog.<module>/
├── api/
│   ├── facade/              # Module facades (public API)
│   ├── dto/                 # Data transfer objects
│   └── events/              # Domain events
├── application/
│   ├── service/             # Application services
│   └── usecase/             # Use case implementations
├── domain/
│   ├── model/               # Domain entities
│   ├── repository/          # Repository interfaces
│   ├── service/             # Domain service interfaces
│   ├── exception/           # Domain exceptions
│   └── validation/          # Validator interface
└── infrastructure/
    ├── persistence/         # JdbcTemplate repositories
    │   ├── repository/      # Repository implementations
    │   └── mapper/          # RowMappers
    ├── config/              # Spring configuration
    └── validation/          # Validator implementation
```

## Dependency Injection

### Constructor Injection (REQUIRED)
ALWAYS use constructor injection, never field injection:

**Good ✅:**
```java
@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final SlugGenerator slugGenerator;
    
    public PostServiceImpl(
        PostRepository postRepository,
        CategoryRepository categoryRepository,
        SlugGenerator slugGenerator
    ) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.slugGenerator = slugGenerator;
    }
}
```

**Bad ❌:**
```java
@Service
public class PostServiceImpl implements PostService {
    @Autowired  // NEVER use field injection!
    private PostRepository postRepository;
}
```

### Why Constructor Injection?
1. Immutable dependencies (final fields)
2. Easier to test (no reflection needed)
3. Explicit dependencies
4. Fails fast if dependencies are missing
5. No need for @Autowired annotation (Spring auto-detects)

## Spring Annotations

### Service Layer
```java
@Service  // Application services
public class PostServiceImpl implements PostService { }

@Component  // Generic components, facades
public class BlogModuleFacade implements BlogApi { }
```

### Repository Layer
```java
@Repository  // Repository implementations
public class JdbcPostRepository implements PostRepository { }
```

### Configuration
```java
@Configuration
public class BlogModuleConfig {
    
    @Bean
    public Validator domainValidator() {
        return new JakartaBeanValidator();
    }
    
    @Bean
    public SlugGenerator slugGenerator() {
        return new DefaultSlugGenerator();
    }
}
```

### REST Controllers
```java
@RestController
@RequestMapping("/api/blog/posts")
public class PostController {
    
    private final PostService postService;
    
    public PostController(PostService postService) {
        this.postService = postService;
    }
    
    @GetMapping("/{slug}")
    public ResponseEntity<EntityModel<PostDTO>> getPost(@PathVariable String slug) {
        // Implementation
    }
}
```

## Exception Handling

### Domain Exceptions
Create specific domain exceptions:
```java
// Base domain exception
public class BlogDomainException extends RuntimeException {
    public BlogDomainException(String message) {
        super(message);
    }
    
    public BlogDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Specific exceptions
public class PostNotFoundException extends BlogDomainException {
    public PostNotFoundException(String slug) {
        super("Post not found: " + slug);
    }
}

public class DuplicateSlugException extends BlogDomainException {
    public DuplicateSlugException(String slug) {
        super("Post with slug already exists: " + slug);
    }
}

public class DomainValidationException extends BlogDomainException {
    private final Map<String, String> fieldErrors;
    
    public DomainValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
```

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final MessageSource messageSource;
    
    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        DomainValidationException ex,
        Locale locale
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .code("VALIDATION_ERROR")
            .message(messageSource.getMessage("error.validation", null, locale))
            .details(ex.getFieldErrors())
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        PostNotFoundException ex,
        Locale locale
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .code("POST_NOT_FOUND")
            .message(messageSource.getMessage("error.post.notFound", 
                new Object[]{ex.getMessage()}, locale))
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

## Validation

### Jakarta Bean Validation
Use for simple validations on DTOs:
```java
public record CreatePostRequest(
    @NotBlank(message = "{validation.title.required}")
    @Size(min = 10, max = 200, message = "{validation.title.size}")
    String title,
    
    @NotBlank(message = "{validation.content.required}")
    String content,
    
    @NotNull(message = "{validation.category.required}")
    UUID categoryId
) {}
```

### Domain Validation
Complex business rules in domain entities:
```java
public class Post {
    
    private final Validator validator;
    
    public Post(String title, String content, Category category, Validator validator) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.validator = validator;
        
        validate(); // Always validate in constructor
    }
    
    private void validate() {
        Map<String, String> errors = new HashMap<>();
        
        if (title == null || title.isBlank()) {
            errors.put("title", "Title is required");
        } else if (title.length() < 10) {
            errors.put("title", "Title must be at least 10 characters");
        }
        
        if (content == null || content.isBlank()) {
            errors.put("content", "Content is required");
        }
        
        if (category == null) {
            errors.put("category", "Category is required");
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }
    }
}
```

## Transaction Management

### Service Layer Transactions
```java
@Service
public class PostServiceImpl implements PostService {
    
    @Transactional  // Default: propagation=REQUIRED, isolation=READ_COMMITTED
    public Post createPost(String title, String content, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        
        String slug = slugGenerator.generate(title);
        Post post = new Post(title, content, category, slug, validator);
        
        return postRepository.save(post);
    }
    
    @Transactional(readOnly = true)  // Optimize read-only operations
    public Optional<Post> findBySlug(String slug) {
        return postRepository.findBySlug(slug);
    }
}
```

### Transaction Boundaries
- Define at **service layer**, not repository or controller
- Use `@Transactional(readOnly = true)` for read operations
- Keep transactions short and focused
- Avoid long-running operations inside transactions

## Event Publishing

### Domain Events
```java
// Event definition (in api package)
public record PostPublishedEvent(
    UUID postId,
    String slug,
    Instant publishedAt
) {}

// Publishing (in service)
@Service
public class PostServiceImpl implements PostService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Post publish(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        
        post.publish();
        postRepository.save(post);
        
        // Publish event AFTER successful persistence
        eventPublisher.publishEvent(
            new PostPublishedEvent(post.getId(), post.getSlug(), Instant.now())
        );
        
        return post;
    }
}

// Listening (in another module)
@Component
public class AnalyticsEventListener {
    
    @EventListener
    @Async  // Process asynchronously
    public void onPostPublished(PostPublishedEvent event) {
        // Handle event in analytics module
    }
}
```

## Configuration Properties

### Type-Safe Configuration
```java
@ConfigurationProperties(prefix = "blog")
public record BlogProperties(
    int maxPostsPerPage,
    int defaultPageSize,
    List<String> supportedLanguages,
    SlugConfig slug
) {
    public record SlugConfig(
        int maxLength,
        String separator
    ) {}
}

// Enable in config class
@Configuration
@EnableConfigurationProperties(BlogProperties.class)
public class BlogModuleConfig { }

// Usage
@Service
public class PostServiceImpl implements PostService {
    
    private final BlogProperties properties;
    
    public List<Post> findPosts(Pagination pagination) {
        int pageSize = Math.min(
            pagination.size(), 
            properties.maxPostsPerPage()
        );
        // ...
    }
}
```

## Testing

### Unit Tests (No Spring Context)
```java
class PostServiceTest {
    
    private PostRepository postRepository;
    private CategoryRepository categoryRepository;
    private SlugGenerator slugGenerator;
    private Validator validator;
    private PostService postService;
    
    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        slugGenerator = mock(SlugGenerator.class);
        validator = mock(Validator.class);
        
        postService = new PostServiceImpl(
            postRepository,
            categoryRepository,
            slugGenerator,
            validator
        );
    }
    
    @Test
    void shouldCreatePost() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Java");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(slugGenerator.generate("My Post")).thenReturn("my-post");
        
        // Act
        Post post = postService.createPost("My Post", "Content", categoryId);
        
        // Assert
        assertThat(post.getSlug()).isEqualTo("my-post");
        verify(postRepository).save(any(Post.class));
    }
}
```

### Integration Tests (With Spring Context)
```java
@SpringBootTest
@Testcontainers
class PostControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateAndPublishPost() throws Exception {
        String requestBody = """
            {
                "title": "My First Post",
                "content": "This is the content",
                "categoryId": "%s"
            }
            """.formatted(categoryId);
        
        mockMvc.perform(post("/api/blog/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("my-first-post"))
            .andExpect(jsonPath("$._links.self.href").exists());
    }
}
```

## Logging

### SLF4J with Logback
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PostServiceImpl implements PostService {
    
    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    
    @Transactional
    public Post publish(UUID postId) {
        log.debug("Publishing post: {}", postId);
        
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> {
                log.warn("Post not found for publishing: {}", postId);
                return new PostNotFoundException(postId);
            });
        
        post.publish();
        postRepository.save(post);
        
        log.info("Post published successfully: slug={}, publishedAt={}", 
            post.getSlug(), post.getPublishedAt());
        
        return post;
    }
}
```

### Log Levels
- **ERROR**: Application errors, exceptions
- **WARN**: Potential issues, deprecated usage
- **INFO**: Important business events (post published, user registered)
- **DEBUG**: Detailed flow information (development)
- **TRACE**: Very detailed information (rarely used)

## Code Style

### Naming Conventions
- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

### Method Naming
- Queries: `find*`, `get*`, `list*`
- Commands: `create*`, `update*`, `delete*`, `publish*`
- Boolean checks: `is*`, `has*`, `can*`

### Keep Methods Small
- One responsibility per method
- Max 20-30 lines per method
- Extract complex logic to private methods
- Use meaningful method names (self-documenting)
