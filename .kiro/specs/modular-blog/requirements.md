# Requirements Document

## Introduction

Este documento especifica os requisitos do sistema **Personal Blog**, implementado como um **monolito modular** utilizando Java 21 e Spring Boot. A arquitetura escolhida permite que o sistema cresça de forma organizada, adicionando novos módulos independentes sem comprometer a coesão do código existente. O módulo inicial é o **Blog**, responsável por gerenciar posts, categorias, tags e comentários. O banco de dados único é PostgreSQL, com todas as tabelas no schema `public`, isoladas por convenção de prefixo por módulo (ex.: `blog_posts`, `blog_categories`).

---

## Glossary

- **Sistema**: A aplicação Personal Blog como um todo (monolito modular).
- **Módulo**: Unidade de negócio coesa e isolada dentro do monolito (ex.: `blog`, `auth`, `analytics`).
- **Blog_Module**: O módulo responsável pelo domínio de blog (posts, categorias, tags, comentários).
- **Post**: Artigo publicado no blog, com título, conteúdo, slug, status e metadados.
- **Slug**: Identificador textual único e amigável para URL de um post (ex.: `meu-primeiro-post`).
- **Author**: Usuário autorizado a criar e publicar posts.
- **Reader**: Usuário anônimo ou autenticado que consome o conteúdo do blog.
- **Category**: Agrupamento temático de posts.
- **Tag**: Marcador livre associado a posts para facilitar a descoberta de conteúdo.
- **Comment**: Resposta textual de um Reader a um Post.
- **Draft**: Post em estado de rascunho, não visível publicamente.
- **Published**: Post em estado publicado, visível publicamente.
- **Module_Boundary**: Limite arquitetural que impede acesso direto entre módulos sem uso de interfaces explícitas.
- **Internal_Event**: Evento publicado dentro do processo JVM para comunicação assíncrona entre módulos.
- **API_Contract**: Interface pública de um módulo exposta para outros módulos ou para o exterior.
- **Module_Facade**: Implementação concreta da `API_Contract` de um módulo, anotada com `@Component` do Spring, que serve como único ponto de entrada para outros módulos acessarem funcionalidades do módulo proprietário. Outros módulos nunca chamam serviços internos, repositórios ou entidades diretamente — apenas a `Module_Facade`.
- **Repository_Interface**: Interface Java que define o contrato de persistência de uma entidade, sem depender de tecnologia específica (sem importações JDBC ou Spring Data nas interfaces do pacote `domain`).
- **Domain_Service_Interface**: Interface Java que define o contrato de um serviço de domínio, permitindo substituição por mocks em testes unitários sem necessidade de contexto Spring.
- **Validator**: Interface de abstração para validação de entidades de domínio, permitindo trocar a implementação (Jakarta Bean Validation, validação customizada) sem alterar o código de domínio.
- **Notification**: Objeto acumulador de erros de validação que segue o Notification Pattern (Martin Fowler), permitindo coletar todos os erros de validação de uma entidade antes de lançar uma exceção, resultando em melhor experiência do usuário ao apresentar todos os problemas de uma vez.
- **H2_Database**: Banco de dados SQL em memória compatível com PostgreSQL, utilizado em testes de repositório para validar queries SQL reais com JdbcTemplate sem necessidade de banco externo.
- **Table_Prefix**: Convenção de nomenclatura de tabelas no schema `public` do PostgreSQL que identifica o módulo proprietário por meio de um prefixo no nome da tabela (ex.: `blog_posts`, `blog_categories`, `blog_comments`, `blog_tags`).
- **Slug_Generator**: Componente responsável por gerar slugs a partir de títulos de posts.
- **Pagination**: Mecanismo de divisão de resultados em páginas com tamanho configurável.
- **MessageSource**: Componente Spring para externalização de mensagens i18n, permitindo que mensagens de erro, validação e resposta sejam traduzidas conforme o idioma do cliente.
- **HATEOAS**: Hypermedia as the Engine of Application State, nível 3 do Richardson Maturity Model, onde respostas REST incluem links hipermídia para guiar navegação pela API.
- **HAL**: Hypertext Application Language, formato JSON padronizado para respostas hipermídia contendo seção `_links` com URIs e relações.
- **Accept_Language_Header**: Header HTTP padrão usado pelo cliente para indicar o idioma preferido para a resposta da API.
- **Idempotent_Migration**: Script de migração Flyway que pode ser executado múltiplas vezes sem causar efeitos colaterais indesejados, utilizando cláusulas condicionais como `IF NOT EXISTS` e `IF EXISTS` no SQL.

---

## Requirements

---

### Requirement 1: Estrutura do Monolito Modular

**User Story:** Como arquiteto de software, quero que o sistema siga uma estrutura de monolito modular clara, para que cada módulo seja coeso, isolado e independentemente testável sem comprometer a entrega de um único artefato deployável.

#### Acceptance Criteria

1. THE Sistema SHALL organizar o código-fonte em pacotes raiz separados por módulo, seguindo a convenção `com.blog.<modulo>`, onde `<modulo>` é um identificador em letras minúsculas sem separadores (ex.: `com.blog.blog`, `com.blog.auth`).
2. THE Sistema SHALL utilizar um único banco de dados PostgreSQL compartilhado por todos os módulos, isolando as tabelas de cada módulo no schema `public` utilizando o `Table_Prefix` no formato `<modulo>_` (ex.: prefixo `blog_` para o módulo blog: `blog_posts`, `blog_categories`), sem utilizar schemas PostgreSQL separados ou múltiplas instâncias de banco de dados.
3. WHEN um módulo precisa de dados de outro módulo de forma síncrona, THE Sistema SHALL acessar esses dados exclusivamente por meio da `Module_Facade` do módulo proprietário; acesso direto a `Repository_Interface`s, entidades de domínio ou tabelas de outros módulos é estritamente proibido.
4. WHEN um módulo precisa notificar outro módulo de forma assíncrona, THE Sistema SHALL publicar `Internal_Event`s via `ApplicationEventPublisher` do Spring; o módulo consumidor é proibido de realizar chamadas diretas a qualquer tipo dentro dos pacotes internos de `com.blog.<modulo_produtor>`.
5. IF um módulo tenta acessar o `Repository_Interface` ou serviços internos de outro módulo, THEN THE Sistema SHALL falhar durante a execução dos testes ArchUnit, impedindo o acoplamento direto.
6. THE Sistema SHALL ser empacotado como um único artefato JAR executável, sem necessidade de orquestração de múltiplos processos para o ambiente de produção.
7. WHERE um novo módulo for adicionado ao Sistema, THE Sistema SHALL acomodar o novo módulo sem alterações no código dos módulos existentes, exceto em arquivos de bootstrapping — definidos como arquivos cuja única responsabilidade é registrar ou compor módulos na inicialização da aplicação.

---

### Requirement 2: Gerenciamento de Posts

**User Story:** Como Author, quero criar, editar, publicar e remover posts, para que eu possa gerenciar o conteúdo do meu blog de forma completa.

#### Acceptance Criteria

1. WHEN um Author submete um novo post com título, conteúdo e categoria, THE Blog_Module SHALL persistir o post com status `Draft` e gerar automaticamente um `Slug` único a partir do título.
2. WHEN o título de um post já resulta em um `Slug` existente, THE Slug_Generator SHALL adicionar um sufixo numérico incremental ao `Slug` para garantir unicidade (ex.: `meu-post-2`).
3. WHEN um Author solicita a publicação de um post em status `Draft`, THE Blog_Module SHALL alterar o status do post para `Published` e registrar o `published_at` com o timestamp atual.
4. WHEN um Author solicita a publicação de um post sem título ou sem conteúdo, THE Blog_Module SHALL rejeitar a operação e retornar uma mensagem de erro descritiva indicando os campos ausentes.
5. WHEN um Author atualiza o conteúdo de um post `Published`, THE Blog_Module SHALL manter o `Slug` original inalterado para preservar URLs existentes.
6. WHEN um Author solicita a exclusão de um post, THE Blog_Module SHALL realizar exclusão lógica, marcando o post como excluído sem remover o registro do banco de dados.
7. THE Blog_Module SHALL associar cada post a exatamente uma `Category` e a zero ou mais `Tag`s no momento da criação ou edição.
8. WHEN um post é publicado, THE Blog_Module SHALL publicar um `Internal_Event` do tipo `PostPublishedEvent` contendo o identificador e o `Slug` do post.

---

### Requirement 3: Listagem e Leitura de Posts

**User Story:** Como Reader, quero listar e ler posts publicados, para que eu possa consumir o conteúdo do blog de forma organizada e eficiente.

#### Acceptance Criteria

1. WHEN um Reader solicita a listagem de posts, THE Blog_Module SHALL retornar apenas posts com status `Published`, ordenados por `published_at` decrescente, com suporte a `Pagination`.
2. THE Blog_Module SHALL suportar `Pagination` com tamanho de página padrão de 10 itens e tamanho máximo de 50 itens por página.
3. WHEN um Reader solicita um tamanho de página superior a 50, THE Blog_Module SHALL retornar os resultados com o tamanho máximo de 50 itens e indicar o limite aplicado na resposta.
4. WHEN um Reader solicita um post por `Slug`, THE Blog_Module SHALL retornar os dados completos do post, incluindo título, conteúdo, autor, categoria, tags e data de publicação.
5. WHEN um Reader solicita um post cujo `Slug` não existe ou cujo post está em status diferente de `Published`, THE Blog_Module SHALL retornar um erro com código HTTP 404.
6. WHEN um Reader filtra posts por `Category`, THE Blog_Module SHALL retornar apenas posts `Published` associados àquela `Category`, com suporte a `Pagination`.
7. WHEN um Reader filtra posts por `Tag`, THE Blog_Module SHALL retornar apenas posts `Published` que contenham aquela `Tag`, com suporte a `Pagination`.
8. WHILE o Blog_Module processa uma solicitação de listagem, THE Blog_Module SHALL retornar a resposta em no máximo 500ms para conjuntos de até 10.000 posts publicados.

---

### Requirement 4: Gerenciamento de Categorias e Tags

**User Story:** Como Author, quero gerenciar categorias e tags, para que eu possa organizar o conteúdo do blog de forma taxonômica e consistente.

#### Acceptance Criteria

1. THE Blog_Module SHALL manter um conjunto de `Category`s criadas pelo Author, cada uma com nome único e descrição opcional.
2. WHEN um Author cria uma `Category` com um nome já existente, THE Blog_Module SHALL rejeitar a operação e retornar uma mensagem de erro indicando a duplicidade.
3. WHEN um Author remove uma `Category` que possui posts associados, THE Blog_Module SHALL rejeitar a operação e retornar uma mensagem de erro indicando que a categoria está em uso.
4. THE Blog_Module SHALL aceitar `Tag`s como strings livres associadas a posts, sem necessidade de cadastro prévio.
5. WHEN um Author associa uma `Tag` a um post, THE Blog_Module SHALL normalizar a tag para letras minúsculas e sem espaços nas extremidades antes de persistir.
6. THE Blog_Module SHALL retornar a lista de todas as `Tag`s distintas em uso nos posts `Published`, ordenadas alfabeticamente.

---

### Requirement 5: Gerenciamento de Comentários

**User Story:** Como Reader, quero comentar em posts publicados, para que eu possa interagir com o conteúdo e com o Author do blog.

#### Acceptance Criteria

1. WHEN um Reader submete um comentário em um post `Published`, THE Blog_Module SHALL persistir o comentário associado ao post, com nome do autor, e-mail e conteúdo do comentário.
2. WHEN um Reader submete um comentário com conteúdo vazio, nome vazio ou e-mail inválido, THE Blog_Module SHALL rejeitar a operação e retornar uma mensagem de erro descritiva por campo inválido.
3. WHEN um Reader tenta comentar em um post com status diferente de `Published`, THE Blog_Module SHALL rejeitar a operação e retornar um erro com código HTTP 404.
4. THE Blog_Module SHALL exibir comentários de um post ordenados por data de criação crescente.
5. WHEN um Author solicita a remoção de um comentário, THE Blog_Module SHALL realizar exclusão lógica do comentário, marcando-o como removido sem apagar o registro.
6. WHERE a moderação de comentários estiver habilitada, THE Blog_Module SHALL manter novos comentários em status `Pending` até que um Author os aprove, antes de tornarem visíveis para Readers.

---

### Requirement 6: Regras Arquiteturais do Módulo

**User Story:** Como desenvolvedor, quero que o módulo de Blog siga regras arquiteturais claras, para que o código permaneça manutenível, testável e preparado para extensão com novos módulos.

#### Acceptance Criteria

1. THE Blog_Module SHALL organizar suas classes nas seguintes camadas internas, sem violação de dependência entre elas: `api` → `application` → `domain` → `infrastructure`.
2. THE Blog_Module SHALL expor funcionalidades para outros módulos exclusivamente por meio de uma `Module_Facade` que implementa uma `API_Contract` definida no pacote `com.blog.blog.api`, nunca expondo classes de domínio internas, entidades de domínio ou `Repository_Interface`s diretamente.
3. THE Blog_Module SHALL definir todos os seus serviços de domínio como `Domain_Service_Interface`s, com implementações concretas injetadas pelo Spring, de forma que qualquer serviço possa ser substituído por um mock em testes unitários sem necessidade de contexto Spring.
4. THE Blog_Module SHALL definir todos os seus repositórios como `Repository_Interface`s no pacote `domain`, com implementações usando JdbcTemplate para produção (PostgreSQL) e `H2_Database` em memória para testes de repositório.
5. WHEN todas as dependências entre camadas do Blog_Module são declaradas, THE Blog_Module SHALL referenciar exclusivamente interfaces (`API_Contract`, `Domain_Service_Interface`, `Repository_Interface`), nunca implementações concretas de outras camadas.
6. WHEN o Blog_Module precisa enviar dados a outro módulo via evento, THE Blog_Module SHALL usar classes de record imutáveis como payload do `Internal_Event`, sem incluir entidades de domínio no payload.
7. THE Blog_Module SHALL garantir que toda entidade de domínio utilize o `Notification` pattern para acumular todos os erros de validação antes de lançar exceção, permitindo que o usuário receba feedback sobre todos os problemas de uma vez.
8. WHEN uma entidade de domínio é construída ou atualizada, THE Blog_Module SHALL validar todos os campos e regras de negócio, acumulando erros no objeto `Notification` antes de invocar `throwIfHasErrors()`.
9. WHEN o objeto `Notification` contém erros após validação completa, THE Blog_Module SHALL lançar `DomainValidationException` contendo um mapa de todos os campos inválidos com suas respectivas mensagens de erro, impedindo que a entidade exista em estado inválido.
10. THE Blog_Module SHALL definir a classe `Notification` no pacote `domain.validation` com métodos para adicionar erros por campo (`addError(field, message)`), verificar existência de erros (`hasErrors()`), e obter todos os erros acumulados (`getErrors()`, `getAllErrors()`).
11. THE Blog_Module SHALL utilizar `Notification` para validações simples de campo (obrigatoriedade, tamanho, formato) e para validações complexas de regras de negócio que envolvem múltiplos campos ou estado da entidade.
10. THE Blog_Module SHALL implementar testes unitários de lógica de domínio utilizando Mockito para mockar `Repository_Interface`s, testes de repositório com `H2_Database` em memória utilizando JdbcTemplate, e testes de integração end-to-end com TestContainers executando PostgreSQL real, com cobertura mínima de 80% das linhas do módulo.
11. IF uma dependência externa (biblioteca de terceiros) for necessária exclusivamente por um módulo, THEN THE Sistema SHALL declarar essa dependência de forma que ela seja claramente associada ao módulo, utilizando comentários ou separação em sub-módulo de build quando aplicável.
12. THE Blog_Module SHALL utilizar o mecanismo de migração de banco de dados Flyway, com scripts de migração localizados em `db/migration/blog/`, versionados separadamente dos demais módulos.

---

### Requirement 7: API REST do Módulo de Blog

**User Story:** Como desenvolvedor frontend ou consumidor de API, quero que o módulo de Blog exponha uma API REST bem definida com suporte a internacionalização e hipermídia, para que eu possa integrar o conteúdo do blog em qualquer interface de forma previsível, navegar pela API sem conhecimento prévio de URIs e receber mensagens no idioma apropriado.

#### Acceptance Criteria

1. THE Blog_Module SHALL expor endpoints REST sob o prefixo de path `/api/blog/`, com versionamento implícito na estrutura de path quando necessário.
2. WHEN uma requisição bem-sucedida é processada, THE Blog_Module SHALL retornar respostas no formato JSON com código HTTP adequado (200, 201, 204).
3. WHEN uma requisição contém dados inválidos, THE Blog_Module SHALL retornar HTTP 400 com um corpo JSON descrevendo os erros de validação por campo, com mensagens obtidas do MessageSource traduzidas conforme o `Accept_Language_Header`.
4. WHEN ocorre um erro interno não tratado, THE Blog_Module SHALL retornar HTTP 500 com uma mensagem genérica obtida do MessageSource traduzida conforme o `Accept_Language_Header`, sem expor detalhes de implementação ou stack traces na resposta ao cliente.
5. THE Blog_Module SHALL documentar todos os endpoints via OpenAPI 3.0, acessível em `/api/blog/docs` no ambiente de desenvolvimento.
6. WHEN um Reader solicita a listagem de posts, THE Blog_Module SHALL incluir na resposta metadados de paginação: `page`, `size`, `totalElements` e `totalPages`.
7. THE Blog_Module SHALL aceitar e retornar datas no formato ISO 8601 (ex.: `2024-01-15T10:30:00Z`) em todos os endpoints.
8. THE Blog_Module SHALL integrar com o MessageSource do Spring para todas as mensagens de erro, validação e resposta, delegando a internacionalização ao componente configurado no Requirement 10.
9. THE Blog_Module SHALL implementar HATEOAS conforme especificado no Requirement 11, retornando respostas hipermídia no formato HAL para todas as entidades e coleções.
10. WHEN uma requisição é processada, THE Blog_Module SHALL garantir que tanto as mensagens textuais quanto os links hipermídia sejam incluídos na resposta JSON, fornecendo uma experiência completa de navegação e compreensão da API.

---

### Requirement 8: Persistência e Integridade dos Dados

**User Story:** Como operador do sistema, quero que os dados do blog sejam persistidos de forma confiável e íntegra no PostgreSQL, para que não haja perda ou corrupção de conteúdo.

#### Acceptance Criteria

1. THE Blog_Module SHALL criar e manter todas as suas tabelas no schema `public` do PostgreSQL utilizando o `Table_Prefix` `blog_` (ex.: `blog_posts`, `blog_categories`, `blog_comments`, `blog_tags`), nunca criando tabelas com prefixos pertencentes a outros módulos.
2. THE Blog_Module SHALL garantir a integridade referencial entre posts, categorias e tags por meio de constraints de chave estrangeira definidas no banco de dados.
3. WHEN o Sistema é inicializado, THE Blog_Module SHALL executar as migrações Flyway pendentes antes de aceitar requisições, garantindo que o schema esteja na versão esperada.
4. THE Blog_Module SHALL criar todos os scripts de migração Flyway como `Idempotent_Migration`, utilizando cláusulas condicionais SQL (ex.: `CREATE TABLE IF NOT EXISTS`, `DROP TABLE IF EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`) para garantir que os scripts possam ser aplicados múltiplas vezes sem falha.
5. THE Blog_Module SHALL nomear scripts de migração Flyway seguindo o padrão `V<versão>__<descrição>.sql` para migrações versionadas e `R__<descrição>.sql` para migrações repetíveis (views, procedures, funções).
6. WHEN scripts versionados (`V*`) criam tabelas, colunas ou constraints, THE Blog_Module SHALL utilizar `IF NOT EXISTS` na cláusula SQL para evitar erros em caso de reaplicação.
7. WHEN scripts versionados (`V*`) removem tabelas, colunas ou constraints, THE Blog_Module SHALL utilizar `IF EXISTS` na cláusula SQL para evitar erros em caso de reaplicação.
8. THE Blog_Module SHALL utilizar UUIDs como identificadores primários das entidades `Post`, `Category`, `Comment`, gerados pela aplicação antes da persistência.
9. IF uma operação de escrita falhar por violação de constraint do banco de dados, THEN THE Blog_Module SHALL capturar a exceção SQLException, realizar rollback da transação e retornar uma mensagem de erro amigável ao chamador, sem propagar detalhes internos do driver JDBC para a camada de API.
10. IF uma entidade de domínio falhar na validação durante a criação ou atualização, THEN THE Blog_Module SHALL lançar `DomainValidationException` antes de qualquer tentativa de persistência, contendo mensagens descritivas dos campos inválidos.
11. THE Blog_Module SHALL indexar as colunas `slug` da tabela `blog_posts`, `name` da tabela `blog_categories` e `published_at` da tabela `blog_posts` para garantir performance nas consultas mais frequentes.

---

### Requirement 10: Internacionalização e Experiência do Usuário da API

**User Story:** Como consumidor da API (desenvolvedor frontend ou integrador), quero receber mensagens de erro, validação e resposta no meu idioma preferido, claras e detalhadas, para que eu possa entender rapidamente o problema e saber como corrigi-lo sem consultar documentação adicional.

#### Acceptance Criteria

1. THE Sistema SHALL suportar três idiomas para mensagens de API: `en-US` (inglês americano), `pt-BR` (português brasileiro) e `es-ES` (espanhol europeu).
2. THE Sistema SHALL utilizar o componente MessageSource do Spring Framework para externalizar todas as mensagens de erro, validação e resposta da API.
3. WHEN uma requisição contém o `Accept_Language_Header`, THE Sistema SHALL inspecionar o header e determinar o idioma da resposta com base no valor fornecido.
4. WHEN o `Accept_Language_Header` contém um idioma suportado (`en-US`, `pt-BR` ou `es-ES`), THE Sistema SHALL retornar todas as mensagens no idioma solicitado.
5. WHEN o `Accept_Language_Header` não é fornecido ou contém um idioma não suportado, THE Sistema SHALL utilizar `en-US` como idioma padrão para todas as mensagens.
6. THE Sistema SHALL obter todas as mensagens de erro, validação e resposta exclusivamente do MessageSource, nunca incluindo strings hardcoded no código Java.
7. THE Sistema SHALL organizar arquivos de mensagens i18n no formato `messages_{locale}.properties` em `src/main/resources/i18n/`, com três arquivos obrigatórios: `messages_en_US.properties`, `messages_pt_BR.properties` e `messages_es_ES.properties`.
8. WHEN uma mensagem de validação é gerada, THE Sistema SHALL incluir no corpo da resposta JSON: o nome do campo afetado, o valor fornecido pelo cliente (se seguro para exibição), o valor ou formato esperado, e um exemplo de valor válido.
9. WHEN uma mensagem de erro de negócio é gerada, THE Sistema SHALL incluir no corpo da resposta JSON: a descrição da regra de negócio violada e uma sugestão de ação corretiva que o consumidor pode tomar.
10. THE Sistema SHALL incluir um código de erro estruturado e padronizado (ex.: `VALIDATION_TITLE_TOO_SHORT`, `BUSINESS_CATEGORY_IN_USE`, `BUSINESS_POST_NOT_FOUND`) em todas as respostas de erro, além da mensagem legível, para facilitar tratamento programático pelos consumidores da API.
11. THE Sistema SHALL garantir que mensagens equivalentes nos três idiomas suportados mantenham consistência em estrutura, nível de detalhe e tom, sem perda de informação ou contexto durante a tradução.
12. WHEN o Sistema traduz uma mensagem de validação para qualquer dos três idiomas, THE Sistema SHALL preservar todos os elementos informativos: campo, valor fornecido, valor esperado e exemplo válido.
13. WHEN uma mensagem não está disponível no idioma solicitado, THE Sistema SHALL retornar a mensagem no idioma padrão (`en-US`) ao invés de falhar ou retornar a chave da mensagem.
14. THE Sistema SHALL projetar mensagens com foco em UX: amigáveis ao usuário final, diretas (sem jargão técnico desnecessário), e detalhadas o suficiente para explicar o problema e como corrigi-lo sem consultar documentação externa.
15. WHEN uma mensagem contém parâmetros dinâmicos (ex.: valores de campo, limites numéricos), THE Sistema SHALL utilizar placeholders parametrizados do MessageSource (ex.: `{0}`, `{1}`) para permitir injeção de valores específicos do contexto da requisição.

---

### Requirement 11: Maturidade REST com HATEOAS

**User Story:** Como consumidor da API, quero que as respostas incluam links hipermídia para ações relacionadas, para que eu possa navegar pela API de forma dinâmica sem precisar construir URIs manualmente ou conhecer a estrutura de URLs previamente.

#### Acceptance Criteria

1. THE Sistema SHALL implementar HATEOAS (REST Maturity Level 3 do Richardson Maturity Model) utilizando a biblioteca Spring HATEOAS para gerar links hipermídia em todas as respostas de entidades.
2. THE Sistema SHALL retornar respostas JSON no formato HAL (Hypertext Application Language), incluindo uma seção `_links` em cada resposta de entidade ou coleção.
3. WHEN uma seção `_links` é gerada, THE Sistema SHALL incluir para cada link: a relação (`rel`) e a URI (`href`), além de atributos opcionais como `type` (tipo de mídia aceito) e `title` (descrição legível do link).
4. WHEN o endpoint raiz `/api/blog` é acessado, THE Sistema SHALL retornar uma resposta contendo links para todos os recursos principais da API: posts, categories, tags, comments, com relação `rel` descritiva para cada recurso.
5. WHEN uma entidade Post é retornada em uma resposta, THE Sistema SHALL incluir links com as seguintes relações aplicáveis ao estado atual do Post: `self` (link para o próprio recurso), `edit` (link para edição), `delete` (link para exclusão), `publish` (se status Draft), `unpublish` (se status Published), `comments` (link para comentários do post), `category` (link para a categoria associada), `author` (link para o autor).
6. WHEN uma entidade Category é retornada em uma resposta, THE Sistema SHALL incluir links com as seguintes relações: `self`, `edit`, `delete`, `posts` (link para listar todos os posts da categoria).
7. WHEN uma entidade Comment é retornada em uma resposta, THE Sistema SHALL incluir links com as seguintes relações aplicáveis ao estado atual do Comment: `self`, `delete`, `approve` (se status Pending), `reject` (se status Pending).
8. WHEN uma resposta paginada é retornada, THE Sistema SHALL incluir na seção `_links` os seguintes links de navegação: `first` (primeira página), `last` (última página), `next` (próxima página, se existir), `prev` (página anterior, se existir), além do link `self` para a página atual.
9. WHILE o Sistema gera links hipermídia para uma entidade, THE Sistema SHALL incluir apenas links aplicáveis ao estado atual da entidade, removendo links que não fazem sentido no contexto atual (ex.: um Post com status Published não deve incluir link `publish`, apenas `unpublish`).
10. THE Sistema SHALL garantir que todos os links gerados na seção `_links` sejam URIs absolutas ou relativas válidas, apontando para recursos acessíveis pelos consumidores da API.
11. THE Sistema SHALL utilizar classes `RepresentationModel`, `EntityModel` e `CollectionModel` do Spring HATEOAS para construir respostas hipermídia, garantindo conformidade com o formato HAL.
12. THE Sistema SHALL permitir que consumidores da API descubram recursos e ações disponíveis dinamicamente através dos links hipermídia, sem necessidade de conhecimento prévio da estrutura de URIs.
13. WHEN um recurso possui paginação, THE Sistema SHALL incluir metadados de paginação tanto no corpo da resposta quanto como links hipermídia (`first`, `last`, `next`, `prev`), garantindo que consumidores possam navegar utilizando apenas os links fornecidos.
14. THE Sistema SHALL garantir que links hipermídia incluam URIs absolutas completas (incluindo protocolo, host e porta) ou URIs relativas válidas ao contexto da aplicação, sem URIs quebradas ou inválidas.
15. WHEN um erro de validação ou negócio ocorre, THE Sistema SHALL incluir links relevantes na resposta de erro quando aplicável, como link para documentação da API ou link para recurso relacionado ao erro.

---

### Requirement 9: Estratégia de Persistência com JdbcTemplate

**User Story:** Como desenvolvedor, quero que a camada de domínio seja independente do banco de dados e que a persistência utilize JdbcTemplate, para que eu possa executar diferentes tipos de testes de forma eficiente e ter controle direto sobre queries SQL sem a complexidade do ORM.

#### Acceptance Criteria

1. THE Blog_Module SHALL definir todos os seus repositórios como `Repository_Interface`s Java no pacote `domain`, sem nenhuma importação de classes JDBC ou Spring Data nessas interfaces.
2. THE Blog_Module SHALL implementar todos os repositórios utilizando JdbcTemplate do Spring, escrevendo queries SQL explícitas para operações de persistência, sem utilizar JPA, Hibernate ou Spring Data JPA.
3. WHEN testes unitários de serviços de domínio são executados, THE Blog_Module SHALL utilizar Mockito para mockar as `Repository_Interface`s, sem carregar `ApplicationContext` Spring, sem uso de `@SpringBootTest` e sem necessidade de banco de dados.
4. WHEN testes de repositório são executados, THE Blog_Module SHALL utilizar `H2_Database` em memória com JdbcTemplate, validando queries SQL reais contra um schema compatível com PostgreSQL, utilizando `@JdbcTest` e evitando `@DataJpaTest`.
5. WHEN testes de integração end-to-end são executados, THE Blog_Module SHALL utilizar TestContainers para iniciar um container PostgreSQL real, garantindo que o comportamento em ambiente de teste espelha o comportamento em produção.
6. WHEN o Sistema é executado em produção ou desenvolvimento, THE Blog_Module SHALL utilizar as implementações JdbcTemplate conectadas a uma instância PostgreSQL configurada via propriedades Spring (`spring.datasource.*`).
7. IF uma classe de domínio importar diretamente qualquer classe dos pacotes `java.sql`, `org.springframework.jdbc` ou `org.springframework.data`, THEN o build SHALL falhar por violação de regra arquitetural detectada pelos testes ArchUnit.
8. THE Blog_Module SHALL garantir que testes unitários de lógica de domínio executem em menos de 100ms no total, testes de repositório com `H2_Database` executem em menos de 2 segundos, e testes de integração com TestContainers executem em menos de 30 segundos incluindo o tempo de inicialização do container.
