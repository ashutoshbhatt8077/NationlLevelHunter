# DevPilot

DevPilot is an AI-powered GitHub codebase assistant. It lets an authenticated GitHub user browse repositories, index a repository's source files into a PostgreSQL/pgvector store, and ask questions about the indexed code through a streaming chat interface with file citations.

The project is organized as a Next.js frontend and a Spring Boot backend:

```text
NationlLevelHunter/
├── backend/                 # Spring Boot API, OAuth, indexing, RAG chat
├── client/                  # Next.js web application
├── docker-compose.yml       # Local PostgreSQL + pgvector
└── docker/postgres/         # PostgreSQL extension initialization
```

## Features

- GitHub OAuth login and session-based authentication.
- Lists repositories available to the signed-in GitHub user.
- Repository synchronization through the GitHub REST API.
- Asynchronous source indexing with progress tracking.
- Code chunking and vector embeddings stored in PostgreSQL with pgvector.
- Retrieval-augmented chat grounded in the selected repository.
- Server-sent events (SSE) for token-by-token assistant responses.
- Chat history, sessions, and source citations.
- Repository index states: `PENDING`, `INDEXING`, `READY`, and `FAILED`.
- Responsive dashboard and chat UI built with Next.js, React, and Tailwind CSS.

## Technology stack

### Frontend

- Next.js `16.2.12`
- React `19.2.4`
- TypeScript
- TanStack Query
- Tailwind CSS
- shadcn/ui and Radix-style UI primitives

### Backend

- Java 21
- Spring Boot `4.1.0`
- Spring Web MVC
- Spring Security OAuth2 Client
- Spring Data JPA
- Spring AI `2.0.0`
- OpenAI model integration
- PostgreSQL and Spring AI pgvector store
- Maven

### Local infrastructure

- PostgreSQL 16 with the `pgvector` extension
- Docker Compose

## Prerequisites

Install the following before starting development:

- Git
- Java 21 or newer
- Node.js compatible with the installed Next.js version
- npm
- Docker Desktop with Docker Compose
- A GitHub OAuth application
- An OpenAI API key

## Quick start

### 1. Clone the repository

```bash
git clone https://github.com/ashutoshbhatt8077/NationlLevelHunter.git
cd NationlLevelHunter
```

### 2. Start PostgreSQL and pgvector

From the repository root:

```bash
docker compose up -d postgres
```

The database is exposed on host port `5433` and uses the following local development defaults:

| Setting | Value |
|---|---|
| Database | `devpilot` |
| Username | `postgres` |
| Password | `postgres` |
| Host port | `5433` |
| Container port | `5432` |

The initialization script enables the PostgreSQL extensions required by the vector store. The named Docker volume `devpilot_pg_data` persists data between container restarts.

### 3. Configure the backend

Create the backend configuration file expected by Spring Boot:

```text
backend/src/main/resources/application.properties
```

Do not commit real credentials. A suitable local configuration is:

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5433/devpilot
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Spring AI / OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}

# Spring AI pgvector store
spring.ai.vectorstore.pgvector.initialize-schema=true

# GitHub OAuth
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
spring.security.oauth2.client.registration.github.scope=read:user,user:email,repo

# Frontend and CORS
app.frontend-url=http://localhost:3000
app.cors.allowed-origins=http://localhost:3000
```

Set the referenced secrets in the shell used to start the backend:

PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
$env:GITHUB_CLIENT_ID="your-github-oauth-client-id"
$env:GITHUB_CLIENT_SECRET="your-github-oauth-client-secret"
```

Bash:

```bash
export OPENAI_API_KEY="your-openai-api-key"
export GITHUB_CLIENT_ID="your-github-oauth-client-id"
export GITHUB_CLIENT_SECRET="your-github-oauth-client-secret"
```

### 4. Configure GitHub OAuth

Create a GitHub OAuth App under **GitHub → Settings → Developer settings → OAuth Apps**.

For local development, use:

| Field | Value |
|---|---|
| Homepage URL | `http://localhost:3000` |
| Authorization callback URL | `http://localhost:8080/login/oauth2/code/github` |

The `repo` scope is needed to read private repositories that the authenticated user can access. Request only the scopes required by your deployment and review GitHub's OAuth documentation before production use.

### 5. Start the backend

From `backend`:

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Bash:

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### 6. Start the frontend

From `client`:

```bash
npm ci
npm run dev
```

The web application is available at `http://localhost:3000`.

The frontend uses `http://localhost:8080` as its default API base URL. To override it, create `client/.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Open `http://localhost:3000`, sign in with GitHub, select a repository, start indexing, and open a chat after the index reaches `READY`.

## Development commands

### Frontend

```bash
cd client
npm ci
npm run dev       # Development server
npm run lint      # ESLint
npm run build     # Production build
npm run start     # Serve the production build
```

### Backend

```bash
cd backend

# Compile and package
./mvnw clean package

# Run tests
./mvnw test

# Run locally
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Application flow

1. The user opens the Next.js client and starts GitHub OAuth.
2. Spring Security handles the OAuth callback and creates or updates the local user.
3. The backend redirects the user to the frontend callback route.
4. The client requests the user's repositories from `/api/repos`.
5. The user starts indexing a repository.
6. The backend reads eligible files from GitHub, chunks the source, creates embeddings, and stores vectors in pgvector.
7. The client polls the repository status endpoint until indexing completes.
8. A chat session is created for the repository.
9. A question is sent to the backend. The backend retrieves the most relevant code chunks, builds a grounded prompt, and streams the answer over SSE.
10. The response includes citations derived from the retrieved document metadata.

Indexing is asynchronous. A successful HTTP response from the index endpoint means that indexing has started, not that it has completed.

## API reference

All `/api/**` endpoints require an authenticated session unless noted otherwise. Requests from the frontend include credentials so the backend session cookie is sent.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/auth/login-url` | Returns the GitHub OAuth login path |
| `GET` | `/oauth2/authorization/github` | Starts GitHub OAuth |
| `GET` | `/api/auth/me` | Returns the current user |
| `POST` | `/api/auth/logout` | Ends the current session |

### Repositories and indexing

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/repos?refresh=true` | Synchronizes and lists the user's GitHub repositories |
| `GET` | `/api/repos?refresh=false` | Lists repositories stored locally |
| `GET` | `/api/repos/{id}` | Gets one owned repository |
| `POST` | `/api/repos/{id}/index` | Starts asynchronous indexing; returns `202 Accepted` |
| `GET` | `/api/repos/{id}/status` | Gets indexing progress and errors |

Example:

```bash
curl -i -X POST \
  http://localhost:8080/api/repos/<repository-id>/index \
  -H "Content-Type: application/json" \
  --cookie "<session-cookie>"
```

### Chat

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/chat/sessions` | Creates a chat session for a repository |
| `GET` | `/api/chat/sessions?repositoryId={id}` | Lists sessions for a repository |
| `GET` | `/api/chat/sessions/{id}` | Gets messages in a session |
| `POST` | `/api/chat/sessions/{id}/messages` | Streams an answer as `text/event-stream` |

Create a session:

```json
{
  "repositoryId": "repository-uuid",
  "title": "Understanding authentication"
}
```

Send a message:

```json
{
  "content": "How does authentication work in this repository?"
}
```

The message stream can contain these event types:

- `user_message`
- `token`
- `assistant_message`
- `done`

## RAG and indexing behavior

The backend:

- Retrieves the selected repository's default branch tree from GitHub.
- Filters out unsupported and oversized files.
- Reads eligible file contents through the GitHub Contents API.
- Splits files into code chunks.
- Stores embeddings and repository metadata in pgvector.
- Retrieves the top 8 chunks for each chat question.
- Restricts vector searches to the selected repository.
- Includes file path and line metadata for citations.
- Keeps an SSE response open for up to three minutes while the model responds.

Starting an index again replaces the existing vectors for that repository before adding the new index. If a file cannot be read, indexing logs the file and continues; a repository-level failure is reported through the `FAILED` status.

## Security and configuration notes

- Never commit `.env` files, OAuth secrets, database passwords, or API keys.
- Use a strong database password outside local development.
- Restrict `app.cors.allowed-origins` to trusted frontend origins.
- Use HTTPS for frontend, backend, OAuth callbacks, and cookies in production.
- Limit GitHub OAuth scopes to the minimum required by the deployment.
- Store application secrets in a secret manager or deployment platform configuration.
- Review access control before exposing repository indexing to multiple users.
- The backend disables CSRF because it uses API-style requests; production deployments should review their session, origin, and cookie configuration carefully.

## Troubleshooting

### The backend cannot connect to PostgreSQL

Confirm that the database is running and that the application uses port `5433`:

```bash
docker compose ps
docker compose logs postgres
```

If the container was created before the initialization script changed, recreate the local database only when it is safe to remove local data:

```bash
docker compose down
docker compose up -d postgres
```

### OAuth redirects to the wrong URL

Verify that:

- The GitHub OAuth callback URL exactly matches `http://localhost:8080/login/oauth2/code/github`.
- `app.frontend-url` is set to `http://localhost:3000`.
- The client is running on the same origin configured in `app.cors.allowed-origins`.

### The frontend returns `401 Unauthorized`

Make sure the backend is running, the browser accepts the backend session cookie, and the frontend API URL points to the backend:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Then sign out and sign in again.

### Indexing remains in `FAILED`

Inspect backend logs and check:

- The GitHub token has access to the repository.
- The `repo` OAuth scope is available for private repositories.
- GitHub API rate limits have not been exceeded.
- The OpenAI API key is valid.
- PostgreSQL has the pgvector extension enabled.

### Chat has no useful context

Wait until the repository status is `READY`. The assistant is intentionally instructed to answer only from retrieved repository context and may report uncertainty when no matching chunks are found.

## Production considerations

For a production deployment:

1. Run the frontend and backend behind HTTPS.
2. Use managed PostgreSQL with pgvector or a secured PostgreSQL deployment.
3. Replace local default credentials and externalize all secrets.
4. Configure a production GitHub OAuth callback.
5. Restrict CORS to the production frontend origin.
6. Add monitoring for API errors, GitHub rate limits, indexing failures, vector-store latency, and model usage.
7. Use backups and migrations rather than relying on `spring.jpa.hibernate.ddl-auto=update`.
8. Configure resource limits for indexing workers and model requests.
9. Review retention and deletion behavior for repository content, embeddings, chat messages, and OAuth tokens.

## Contributing

1. Create a feature branch from `main`.
2. Keep frontend and backend changes focused.
3. Run the relevant lint, build, and test commands before opening a pull request.
4. Do not commit secrets or generated build output.
5. Document new endpoints, environment variables, and user-visible behavior.

## License

No license file is currently included in the repository. Add an explicit license before distributing or accepting external contributions.
