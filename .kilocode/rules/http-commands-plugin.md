# HTTP Commands Plugin - Project Rules

## API Documentation

**CRITICAL**: When modifying HTTP API endpoints in [`RequestHandler.java`](src/main/java/me/lenaic/httpcommands/RequestHandler.java), you MUST update [`src/main/api/openapi.yaml`](src/main/api/openapi.yaml) to reflect the changes.

This includes:
- Adding new endpoints (GET, POST, etc.)
- Modifying request/response schemas
- Adding or removing parameters
- Changing authentication requirements

After making API changes, regenerate docs with:
```bash
./gradlew openApiGenerate
```

## Platform Requirements

This is a Minecraft **Paper** plugin:
- Target: Paper 1.21+ (check [`build.gradle`](build.gradle) for exact version)
- API version: 1.21 (defined in [`plugin.yml`](src/main/resources/plugin.yml))
- Requires Java 21+
- **Do NOT add Spring Boot or other web frameworks**

When adding dependencies:
- Use `compileOnly` for Paper API (not included in runtime)
- Check https://papermc.io/ for latest versions

## Current Endpoints

All endpoints (except root) require Bearer token authentication:
- `GET /info` - Server information (player count, max players, version)
- `GET /players` - List online players
- `POST /execute` - Execute commands

## Response Format

- Always use JSON
- Include `success` boolean field
- Use appropriate HTTP status codes

## Security

- HTTPS is required by default (configurable in `config.yml`)
- Bearer token authentication for all protected endpoints
- Validate all inputs

## Build Commands

Always Build + Generate API docs after you ended all modifications:
./gradlew shadowJar openApiGenerate
