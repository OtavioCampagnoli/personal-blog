# Blog MVP - Resumo Executivo

## 🎯 O Que Foi Criado

Foram criadas **5 tasks detalhadas** seguindo os princípios de Senior Product Owner, cobrindo as funcionalidades base do blog MVP.

## 📋 Tasks Criadas

### ✅ Task 01: Create Post Draft
**Endpoint**: `POST /api/blog/posts`  
**Objetivo**: Criar posts em rascunho com título, descrição, categoria e tags  
**Estimativa**: 2 dias  
**Key Features**:
- Geração automática de slug único
- Status DRAFT por padrão
- Validações completas (título 10-200 chars, máx 10 tags)
- Post criado vazio (sem tópicos inicialmente)

### ✅ Task 02: Get Post
**Endpoint**: `GET /api/blog/posts/{slug}`  
**Objetivo**: Buscar post completo com todos os tópicos e imagens ordenados  
**Estimativa**: 1.5 dias  
**Key Features**:
- Tópicos ordenados por `order`
- Imagens ordenadas por `position` dentro de cada tópico
- Cache para posts publicados
- Autorização para drafts (apenas autor)
- Soft delete filtrado

### ✅ Task 03: Add Topic to Post
**Endpoint**: `POST /api/blog/posts/{slug}/topics`  
**Objetivo**: Adicionar tópicos com conteúdo Markdown aos posts  
**Estimativa**: 2 dias  
**Key Features**:
- Ordem automática (adiciona no final) ou manual (reordena existentes)
- Conteúdo em Markdown (mín 10 chars, título 3-100 chars)
- Limite de 50 tópicos por post
- Apenas autor pode adicionar tópicos

### ✅ Task 04: Upload Image to S3
**Endpoint**: `POST /api/blog/images`  
**Objetivo**: Fazer upload de imagens para S3 com validações  
**Estimativa**: 2.5 dias  
**Key Features**:
- Validação de tipo (JPEG, PNG, WebP, GIF) via magic bytes
- Validação de tamanho (máx 5MB)
- Validação de dimensões (100x100 até 4000x4000)
- Storage no S3 com CloudFront CDN
- Extração automática de metadados (width, height)
- Correção de orientação EXIF

### ✅ Task 05: Add Image to Topic
**Endpoint**: `POST /api/blog/posts/{slug}/topics/{topicId}/images`  
**Objetivo**: Associar imagens já enviadas ao S3 com tópicos específicos  
**Estimativa**: 1.5 dias  
**Key Features**:
- Posicionamento automático ou manual dentro do tópico
- Caption opcional (máx 300 chars)
- Validação de ownership (apenas imagens do próprio autor)
- Limite de 20 imagens por tópico
- Mesma imagem pode estar em múltiplos tópicos

## 🏗️ Arquitetura

### Modelo de Domínio
```
Post (Agregado Raiz)
  └── Topics (Entidades ordenadas)
        └── TopicImages (Associações ordenadas)
              └── Image (Entidade independente)
```

### Stack Tecnológico
- **Backend**: Java 21 + Spring Boot 3.x
- **Cloud**: AWS S3 + CloudFront (serverless-ready)
- **Database**: PostgreSQL com JdbcTemplate
- **Testing**: JUnit 5 + TestContainers

### Padrões de Design
- **DDD**: Agregados bem definidos, linguagem ubíqua
- **Clean Architecture**: Domain → Application → Infrastructure
- **Notification Pattern**: Acumulação de erros de validação
- **HATEOAS**: Hypermedia links em todas as respostas
- **Soft Delete**: Dados nunca são removidos fisicamente

## 📊 Estimativas

| Fase | Tasks | Dias | Status |
|------|-------|------|--------|
| CRUD Posts | 01-02 | 3.5 | 📋 Especificado |
| Tópicos | 03, 06-08 | 4.5 | 📋 Parcial |
| Imagens | 04-05, 09-10 | 4.5 | 📋 Parcial |
| Publicação | 11-12 | 1.5 | ⏳ Pendente |
| Listagem | 13-14 | 3.5 | ⏳ Pendente |
| Infraestrutura | 00, 15-17 | 7.0 | ⏳ Pendente |
| **TOTAL MVP** | **17 tasks** | **24.5 dias** | **~5 semanas** |

## 🎨 Qualidade das Tasks

Cada task inclui:

### ✅ Estrutura Completa
- User Story com persona, capability e business value
- Business Context explicando o "por quê"
- Acceptance Criteria em formato Given-When-Then

### ✅ Cenários Cobertos
- **Happy Path**: Fluxos principais
- **Validation Rules**: Regras de negócio
- **Error Scenarios**: Todos os erros possíveis com códigos específicos
- **Edge Cases**: Casos limites e boundary conditions
- **Non-Functional Requirements**: Performance, segurança, cache

### ✅ Exemplos Práticos
- Request examples (JSON completo)
- Response examples (sucesso e erros)
- Exemplos em múltiplos idiomas (pt-BR)
- Códigos de erro específicos

### ✅ Detalhes Técnicos
- Domain model com código Java
- Repository interfaces
- SQL queries quando necessário
- Lógica de negócio detalhada

### ✅ Definition of Done
- Checklist completo de 15-20 itens
- Testes unitários e de integração
- Performance targets
- Documentação OpenAPI
- Code review e deploy

## 🔗 Dependências

### Antes de Task 01:
- [ ] **Task 00**: Shared Module (BaseEntity, Id<T>, Notification)
- [ ] **Task 15**: Category entity e repository
- [ ] **Task 16**: Author entity e repository
- [ ] **Task 17**: JWT Authentication middleware
- [ ] Database schema inicial

### Ordem Recomendada:
1. Task 00 (Shared Module) ← **COMEÇAR AQUI**
2. Tasks 15-17 (Category, Author, Auth)
3. Tasks 01-02 (Posts)
4. Task 03 (Topics)
5. Tasks 04-05 (Images)

## 🚀 Próximos Passos

### Imediato:
1. Criar **Task 00 (Shared Module)** com detalhes de:
   - BaseEntity<T> abstrato
   - Id<T> value object
   - Notification pattern
   - DomainValidationException
   - Slug value object
   
2. Criar **Tasks de infraestrutura** (15-17):
   - Category CRUD
   - Author management
   - JWT authentication

### Depois:
3. Implementar tasks na ordem especificada
4. Criar tasks faltantes das Fases 2-5:
   - Update/Delete Topic
   - Reorder Topics/Images
   - Publish/Unpublish Post
   - List/Search Posts

## 💡 Diferenciais das Tasks

### 1. Completude
Cada task é **self-contained** - um desenvolvedor pode implementá-la sem precisar fazer perguntas.

### 2. Testabilidade
Todos os cenários têm critérios **pass/fail claros** com exemplos concretos.

### 3. i18n First
Mensagens de erro em **3 idiomas** (en-US, pt-BR, es-ES) desde o início.

### 4. Segurança
- Validação de ownership em operações sensíveis
- Magic bytes para validar tipos de arquivo
- Soft delete para auditoria

### 5. Performance
- Targets específicos (<200ms, <300ms, <3s)
- Cache HTTP configurado
- Join fetch para evitar N+1 queries

### 6. HATEOAS
Todas as respostas incluem **hypermedia links** para navegação da API.

## 📝 Observações Importantes

### Sobre Tópicos
- Posts começam **vazios** (sem tópicos)
- Autor constrói o post **incrementalmente**
- Tópicos podem ser reordenados livremente
- Cada tópico é uma **seção independente** com título e Markdown

### Sobre Imagens
- **Upload separado** da associação (2 etapas)
- Imagens ficam no S3 (preparado para serverless)
- Mesma imagem pode aparecer em **múltiplos tópicos**
- Posicionamento preciso dentro do tópico via `position`

### Sobre Ordem
- Tanto tópicos quanto imagens usam campo `order`/`position`
- Sistema reordena automaticamente quando inserido no meio
- SQL incrementa orders existentes de forma atômica

## 🎓 Lições de DDD Aplicadas

### Agregados
- **Post** é o agregado raiz
- **Topic** e **Image** são entidades dentro do agregado
- **TopicImage** é objeto de valor (associação)

### Invariantes
- Post não pode ter > 50 tópicos
- Tópico não pode ter > 20 imagens
- Imagem deve pertencer ao autor do post

### Linguagem Ubíqua
- Draft (rascunho), não "unpublished"
- Topic (tópico), não "section" ou "paragraph"
- Slug (url amigável), não "permalink"

## 📚 Recursos

- **Specs completas**: `.kiro/specs/blog-mvp/`
- **Overview**: [README.md](./.kiro/specs/blog-mvp/README.md)
- **API Docs**: OpenAPI será gerado automaticamente
- **Diagramas**: (TODO: adicionar diagramas de domínio)

---

**Criado em**: 18 de junho de 2026  
**Metodologia**: Senior PO Task Creation (baseado no skill)  
**Status**: ✅ 5 tasks criadas | ⏳ 12 tasks pendentes
