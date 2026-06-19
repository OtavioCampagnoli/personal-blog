---
inclusion: auto
---

# Modular Architecture Standards

## Context
This project follows a **modular monolith** architecture pattern. All code must respect module boundaries and isolation rules defined here.

## Module Structure Rules

### Package Organization
- Each module MUST be organized under `com.blog.<module>` (e.g., `com.blog.blog`, `com.blog.auth`)
- Module names MUST be lowercase, single-word identifiers without separators
- Never create cross-module package dependencies

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
