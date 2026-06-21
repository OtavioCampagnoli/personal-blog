# Personal Blog - SDD Specification

## Objetivo

Este documento consolida a especificacao do sistema a partir do material existente em `.kiro/` e do codigo atualmente implementado no repositório.

O foco aqui e dupla:

1. Definir o que o sistema e hoje.
2. Fixar os limites arquiteturais que nao devem ser quebrados durante novas entregas.

## Fontes de Verdade

- Diretrizes arquiteturais e de qualidade: `.kiro/steering/`
- Especificacao funcional por entrega: `.kiro/specs/blog-mvp/`
- Implementacao atual: `src/main/java/com/otavio/blog/`
- Testes atuais: `src/test/java/com/otavio/blog/`
- Schema atual: `src/main/resources/db/migration/V1__create_initial_schema.sql`

## Visao Geral Do Sistema

O sistema e um monolito Spring Boot com um dominio de blog composto por:

- `Post`
- `Topic`
- `TopicImage`
- `Image`
- `Author`
- `Category`

O modelo atual implementa:

- criacao de posts em draft
- leitura de post por slug
- adicao e listagem de topicos
- upload de imagem
- associacao de imagem a topico
- erros de dominio e validacao com acumulacao de mensagens
- resposta HTTP com links hipermidia em campos `_links`

## Estado Atual E Decisoes Ja Tomadas

- O build e unico, com um artefato Spring Boot.
- O codigo esta organizado por camadas de pacote, nao por multiplos modulos Maven.
- O dominio usa `JdbcTemplate` via interfaces de repositorio no pacote `domain`.
- O acesso externo e feito por controllers HTTP na camada `application.api`.
- O fluxo de negocio centraliza regras em `application.service`.
- O schema atual usa tabelas `authors`, `categories`, `posts`, `post_tags`, `topics`, `images`, `topic_images`.
- O ambiente padrao de desenvolvimento usa H2, com Flyway habilitado.
- A especificacao atual do codigo usa HATEOAS manual com mapa `_links`, nao `RepresentationModel` do Spring HATEOAS.
- A seguranca esta permissiva por enquanto; a configuracao atual libera todas as rotas.

## Limites Arquiteturais

### 1. Shared Domain

Pacote: `com.otavio.blog.shared.domain`

Pode conter apenas:

- `BaseEntity`
- `Id<T>`
- `Slug`
- `Notification`
- excecoes de dominio reutilizaveis

Nao pode conter:

- Spring MVC
- JDBC
- persistencia
- regras de negocio especificas de um agregado

### 2. Domain

Pacotes:

- `com.otavio.blog.domain.post`
- `com.otavio.blog.domain.topic`
- `com.otavio.blog.domain.image`
- `com.otavio.blog.domain.author`
- `com.otavio.blog.domain.category`

Pode conter:

- entidades
- value objects
- contratos de repositorio
- regras de estado do dominio
- validacao de invariantes

Nao pode conter:

- `@Service`
- `@RestController`
- `JdbcTemplate`
- classes de web
- detalhes de storage
- acesso direto a tabelas

### 3. Application

Pacotes:

- `com.otavio.blog.application.service`
- `com.otavio.blog.application.api`
- `com.otavio.blog.application.api.dto`

Pode conter:

- orquestracao de casos de uso
- limites transacionais
- validacao de fluxo
- autorizacao de negocio
- mapeamento para DTOs
- adaptacao HTTP

Nao pode conter:

- SQL
- mapeamento direto de `ResultSet`
- regras de armazenamento
- logica de dominio pesada que pertença a entidades

### 4. Infrastructure

Pacotes:

- `com.otavio.blog.infrastructure.persistence`
- `com.otavio.blog.infrastructure.storage`
- `com.otavio.blog.infrastructure.config`

Pode conter:

- implementacao JDBC dos repositorios
- integracao com storage local ou cloud
- configuracao Spring
- adaptadores de seguranca
- mapeamento de persistencia

Nao pode conter:

- regras de negocio do dominio
- validacoes de input do usuario
- decisao sobre permissao de autor

## Regras De Dependencia

### Dependencias permitidas

- `domain` pode depender de `shared.domain` e JDK
- `application` pode depender de `domain`, `shared.domain` e Spring de orquestracao
- `infrastructure` pode depender de `domain`, `shared.domain`, `application` e bibliotecas externas
- `application.api` pode depender de `application`, `domain`, `shared.domain` e Spring Web

### Dependencias proibidas

- `domain` nao pode depender de `application`, `infrastructure` ou `application.api`
- `shared.domain` nao pode depender de nenhum pacote de negocio
- nenhum repositorio de dominio pode importar `JdbcTemplate`
- controllers nao devem chamar classes concretas de infraestrutura diretamente
- entidades nao devem depender de framework web

## Modelo De Dominio

### Post

Responsabilidades:

- representar um artigo do blog
- manter `title`, `slug`, `description`, `status`, `authorId`, `categoryId`, `tags`
- controlar transicao de estado: draft, published, archived
- garantir regras como:
  - slug valido
  - publicacao somente em estado permitido
  - autorizacao por autor dono

### Topic

Responsabilidades:

- representar uma secao ordenada do post
- manter `postId`, `title`, `content`, `orderNumber`
- garantir ordem e limites de conteudo

### TopicImage

Responsabilidades:

- representar associacao entre topico e imagem
- manter `topicId`, `imageId`, `position`, `caption`, `addedAt`

### Image

Responsabilidades:

- representar imagem armazenada
- manter metadados tecnicos e ownership por autor

### Author

Responsabilidades:

- representar o autor que cria e publica conteudo
- servir como referencia de ownership e autenticacao futura

### Category

Responsabilidades:

- classificar posts
- fornecer slug amigavel e nome unico

## Repositorios

Os repositorios sao contratos de dominio.

Regras:

- interface no pacote `domain`
- implementacao no pacote `infrastructure.persistence`
- retorno com `Id<T>` tipado
- queries devem ignorar registros com `deleted_at` preenchido quando o caso de uso normal exigir apenas ativos
- operacoes de escrita devem respeitar transacao

### Contratos existentes

- `PostRepository`
- `TopicRepository`
- `TopicImageRepository`
- `ImageRepository`
- `CategoryRepository`

## Persistencia

### Regras Estruturais

- um unico banco relacional
- uma unica aplicacao
- schema unico
- tabelas com chave primaria UUID
- `created_at`, `updated_at` e `deleted_at` para entidades auditaveis
- soft delete como comportamento padrao

### Schema Atual

O schema implementado hoje usa:

- `authors`
- `categories`
- `posts`
- `post_tags`
- `topics`
- `images`
- `topic_images`

### Regras De Integridade

- `posts.author_id` referencia `authors.id`
- `posts.category_id` referencia `categories.id`
- `topics.post_id` referencia `posts.id`
- `images.author_id` referencia `authors.id`
- `topic_images.topic_id` referencia `topics.id`
- `topic_images.image_id` referencia `images.id`

## API

### Responsabilidades Da Camada HTTP

- receber requests
- validar payload estrutural
- converter DTOs para comandos de aplicacao
- converter dominio para response models
- devolver status HTTP e headers corretos
- centralizar tratamento de excecoes

### Endpoints Atuais

- `POST /api/blog/posts`
- `GET /api/blog/posts/{slug}`
- `POST /api/blog/posts/{slug}/topics`
- `GET /api/blog/posts/{slug}/topics`
- `POST /api/blog/posts/{slug}/topics/{topicId}/images`
- `GET /api/blog/posts/{slug}/topics/{topicId}/images`
- `POST /api/blog/images`

### Contrato De Resposta

- respostas de sucesso devem retornar JSON
- respostas de erro devem carregar `code`, mensagem legivel e lista de erros quando aplicavel
- links hipermidia devem ficar em `_links`
- datas devem ser serializadas em ISO 8601

## Validacao E Erros

### Regras

- validacao de dominio deve usar `Notification`
- regras acumuladas devem gerar `DomainValidationException`
- erros de negocio devem ser previsiveis e com codigo
- a API nao deve expor stack trace

### Formato Esperado

Cada erro deve idealmente informar:

- `code`
- `field` quando aplicavel
- mensagem compreensivel
- valor fornecido quando for seguro
- exemplo ou formato esperado quando ajudar o usuario

## HATEOAS

O sistema ja produz links em `_links` nas respostas de:

- post
- topico
- imagem

Regra arquitetural:

- o link gerado deve ser coerente com o estado do recurso
- a resposta nao deve incluir acoes impossiveis no contexto atual
- links devem apontar para recursos validos e acessiveis

## Testes

### Estrategia Obrigatoria

- testes unitarios para entidades e value objects
- testes de servico para casos de uso
- testes de repositorio para SQL e mapeamento
- testes de integracao fim a fim para fluxo de API

### Padriao Atual

- `JUnit 5`
- `Mockito`
- `Spring Boot Test`
- `MockMvc`
- `TestContainers`

### Regras De Qualidade

- regras arquiteturais devem ser cobrertas por testes de boundary
- mudancas de dominio devem manter cobertura de validacao
- repositorios nao devem ser testados apenas por mock

## Boas Praticas SDD Para Este Projeto

1. Especificar primeiro o comportamento, depois a implementacao.
2. Registrar explicitamente o que esta fora de escopo.
3. Manter um unico documento de verdade para arquitetura, com derivacoes menores por funcionalidade.
4. Amarrar cada decisao a um owner de camada.
5. Fazer o codigo obedecer o contrato, nao o contrario.
6. Usar testes para bloquear violações de arquitetura.
7. Evitar misturar roadmap, implementacao e regras de dominio no mesmo texto.

## Nao Objetivos Atuais

Este documento nao amplia o escopo para:

- comentarios
- analytics
- multi-tenancy
- microsservicos
- event bus distribuido
- versionamento de posts
- colaboracao em tempo real
- motor de busca full-text

## Divergencias Conhecidas Entre A Spec Antiga E O Codigo Atual

- A documentacao antiga fala em `com.blog.*`, mas o codigo esta em `com.otavio.blog.*`.
- A documentacao antiga sugere um monolito modular formal; hoje existe modularidade por pacote, nao por multiplos artefatos.
- A documentacao antiga fala em tabelas com prefixo `blog_`; o schema atual usa tabelas sem prefixo.
- A documentacao antiga sugere Spring HATEOAS completo; hoje o projeto usa links manuais em `_links`.
- A documentacao antiga sugere PostgreSQL em producao; o runtime atual esta configurado para H2 no ambiente local.
- A documentacao antiga antecipa auth e outros modulos; o codigo atual ainda trata autor como referencia de dominio e usa seguranca permissiva.

## Conclusao

O sistema ja tem uma base boa para SDD, mas a disciplina principal agora e estabilizar as fronteiras:

- dominio puro
- aplicacao orquestradora
- infraestrutura substituivel
- API sem regra de negocio
- contrato testavel por camadas

Se uma nova entrega nao encaixar nesses limites, a doc deve ser atualizada antes do codigo.
