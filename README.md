# HTTP Commands Plugin

A Minecraft Paper plugin that allows executing server commands via HTTP requests. Perfect for integrating external tools, web dashboards, or automation scripts with your Minecraft server.

## Features

- Execute Minecraft commands via HTTP API
- RESTful endpoints for server information
- Bearer token authentication
- Command queuing for offline players
- Support for waiting for players to join before executing commands
- Auto-generated interactive API documentation

## Requirements

- Minecraft Server running [Paper](https://papermc.io/) or a Paper fork (Spigot, Bukkit may work but not tested)
- Java 17 or higher

## Installation

### From Release

1. Download the latest release from the [Releases](https://github.com/lenaic/http-commands-plugin/releases) page
2. Drop the `http-commands-*.jar` file into your server's `plugins` folder
3. Restart or reload your server

### From Source

```bash
# Clone the repository
git clone https://github.com/lenaic/http-commands-plugin.git
cd http-commands-plugin

# Build the plugin
./gradlew build

# The JAR file will be in build/libs/
cp build/libs/http-commands-*.jar /path/to/server/plugins/
```

## Configuration

After first launch, the plugin creates `plugins/http-commands/config.yml`:

```yaml
# HTTP Commands Plugin Configuration

# Port for the HTTP server to listen on
port: 8080

# Require HTTPS for all requests
# When enabled, the plugin will log an error if a request is made over HTTP
# This is intended to encourage the use of a reverse proxy with HTTPS
# Set to false to allow direct HTTP access (not recommended for production)
require-https: true

# Bearer token for authentication
# WARNING: Change this to a secure token in production!
bearer-token: "change-me-to-a-secure-token"
```

### Security Recommendations

1. **Change the bearer token** - Use a strong, random string
2. **Use HTTPS** - Always use a reverse proxy with TLS/SSL
3. **Firewall** - Only allow access from trusted IPs if possible

### Using with a Reverse Proxy (Recommended)

For production, use a reverse proxy like Nginx or Caddy with HTTPS:

**Nginx example:**
```nginx
server {
    listen 443 ssl;
    server_name mc-api.example.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## API Documentation

Interactive API documentation is generated from the OpenAPI specification.

### Generating Documentation

```bash
# Generate HTML API documentation
./gradlew openApiGenerate

# Output will be in docs/index.html
```

### Viewing Documentation

Open `docs/index.html` in a web browser to view the interactive Swagger UI.

The documentation includes:
- All available endpoints
- Request/response schemas
- Example requests
- Try-it-out functionality

## Commands

### /http-commands reload

Reloads the plugin configuration and restarts the HTTP server.

```bash
/http-commands reload
```

Requires permission: `httpcommands.reload`

## Building

```bash
# Build the plugin
./gradlew build

# Build and generate API docs
./gradlew build openApiGenerate
```

The output JAR will be in `build/libs/`.

## License

[MIT License](LICENSE)
