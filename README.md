# KitTrack

Sistema web para controle da montagem, entrega e devolução de kits de equipamentos utilizados nas aulas de transmissão.

**Status:** em desenvolvimento.

## Objetivo

O KitTrack busca organizar a distribuição de equipamentos aos transmissores responsáveis por cada aula, facilitar o acompanhamento das devoluções e controlar a disponibilidade dos itens em estoque, reduzindo conferências manuais e retrabalho.

Um kit pode conter câmeras, microfones, passadores e pincéis, conforme as quantidades informadas no cadastro da aula.

## Tecnologias utilizadas

- **Linguagem:** Java 21
- **Framework:** Spring Boot 4.1.1
- **Interface:** Thymeleaf, HTML e CSS
- **Banco de dados:** PostgreSQL 18
- **Acesso ao banco:** Spring JDBC (`JdbcTemplate`)
- **Migrações:** Flyway
- **Build:** Maven Wrapper
- **Ambiente local:** Docker Compose

## Funcionalidades implementadas

- Cadastro de transmissores.
- Cadastro de aulas com data, horários, sala e transmissor responsável.
- Criação de kits vinculados às aulas, com quantidades de itens definidas no cadastro.
- Consulta e filtragem de aulas por data, sala, transmissor e status do kit.
- Registro da entrega de kits pendentes.
- Registro da devolução de kits entregues.
- Atualização do estoque na entrega e na devolução.
- Bloqueio de entrega quando o estoque disponível é insuficiente.
- Persistência dos dados no PostgreSQL.
- Versionamento do esquema do banco por meio do Flyway.

O projeto continua em desenvolvimento; funcionalidades adicionais podem ser incorporadas nas próximas etapas.

## Pré-requisitos

Antes de executar o projeto, instale:

- JDK 21.
- Docker Desktop com Docker Compose.
- Git, caso deseje clonar o repositório.

O Maven Wrapper está incluído no projeto, portanto não é necessário instalar o Maven separadamente.

## Configuração do ambiente

### 1. Obter o projeto

Clone o repositório e entre na pasta do projeto:

```powershell
git clone https://github.com/engvxtvictor-lgtm/KitTrack.git
cd KitTrack
```

A branch de desenvolvimento utilizada atualmente é `feat/base-spring`.

### 2. Configurar as variáveis de ambiente

Na raiz do projeto, crie um arquivo `.env` com as seguintes variáveis:

```dotenv
POSTGRES_DB=nome_do_banco
POSTGRES_USER=usuario_local
POSTGRES_PASSWORD=defina_uma_senha_local
```

Substitua os valores de exemplo pelos valores que deseja utilizar no ambiente local.

**Importante:** não versione o arquivo `.env`, pois ele contém credenciais. O arquivo já está listado no `.gitignore`.

### 3. Iniciar o PostgreSQL

Na raiz do projeto, execute:

```powershell
docker compose up -d db
```

O PostgreSQL será disponibilizado localmente na porta `5433`, encaminhada para a porta `5432` do contêiner.

Para verificar o contêiner:

```powershell
docker compose ps
```

### 4. Executar a aplicação

Entre na pasta `backend`:

```powershell
cd backend
```

Inicie a aplicação na porta `8083`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083"
```

Mantenha o terminal aberto enquanto estiver utilizando a aplicação.

Acesse pelo navegador:

**http://localhost:8083**

Na inicialização, o Spring Boot utiliza as configurações do banco e o Flyway para aplicar as migrações pendentes.

> A configuração atual do banco utiliza `localhost:5433`, adequada para executar a aplicação diretamente no computador e o banco em Docker Compose.

## Estrutura do projeto

```text
KitTrack/
├── .github/
├── .vscode/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   │           ├── db/
│   │           │   └── migration/
│   │           ├── templates/
│   │           └── application.properties
│   └── pom.xml
├── docs/
│   ├── diagramas/
│   ├── KitTrack_Modelo_Dados_v2.md
│   └── LEIA-ME_Modelagem_v2.txt
├── sql/
├── .env
├── .gitignore
├── docker-compose.yml
└── README.md
```

O arquivo `.env` é local e não deve ser enviado ao repositório.

## Banco de dados e migrações

As migrações do esquema ficam em:

`backend/src/main/resources/db/migration/`

A migração inicial é:

`V1__cria_tabelas_iniciais.sql`

O Flyway controla a aplicação e o histórico das migrações. Novas alterações estruturais devem ser adicionadas em arquivos de migração versionados, evitando modificar migrações já aplicadas em ambientes compartilhados.

## Documentação

Os documentos e diagramas de modelagem estão na pasta `docs/`, incluindo:

- Modelo de dados.
- Diagramas de entidades e relacionamentos.
- Fluxo operacional.
- Documentação do planejamento e das regras do sistema.

## Autor

**@dev.vxt**
