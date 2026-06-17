# ProMcScure Setup Guide

Welcome to ProMcScure! This guide will help you set up the plugin and connect it with your REST client in minutes.

## Quick Start (3 Steps)

### 1. Install the Plugin

1. Download the latest `promcscure-*.jar` from [Releases](https://github.com/1Dmar/minecraft-http-commands-plugin/releases)
2. Drop it into your server's `plugins/` folder
3. Restart your Minecraft server

### 2. Find Your ******

When the server starts, you'll see:

```
╔════════════════════════════════════════╗
║         ProMcScure Setup Guide         ║
╚════════════════════════════════════════╝

✓ AUTO-GENERATED BEARER TOKEN:
  token_abc123def456...
```

**Copy this token** - you'll need it for your REST client!

### 3. Configure Your REST Client

See the [REST Client Configuration](#rest-client-configuration) section below.

---

## Installation Options

### From Release (Recommended)

1. Visit [Releases](https://github.com/1Dmar/minecraft-http-commands-plugin/releases)
2. Download `promcscure-*.jar`
3. Place in `plugins/` folder
4. Restart server

### From Source

```bash
git clone -b copilot/main https://github.com/1Dmar/minecraft-http-commands-plugin.git
cd minecraft-http-commands-plugin
gradle build
cp build/libs/promcscure-*.jar /path/to/server/plugins/
```

---

## Configuration

### Auto-Generated Token

ProMcScure **automatically generates a secure token on first run**. No manual setup needed!

- **Location:** `plugins/promcscure/config.yml`
- **Field:** `bearer-token`

### Manual Token Management

To view your current token:

```bash
/promcscure token
```

To change your token, edit `plugins/promcscure/config.yml`:

```yaml
bearer-token: your-new-secure-token-here
```

Then reload:

```bash
/promcscure reload
```

### Configuration File

```yaml
# plugins/promcscure/config.yml

# Port for the API server
port: 8080

# ****** for authentication (auto-generated on first run)
bearer-token: token_abc123def456...

# Require HTTPS (set to false for HTTP-only)
require-https: false
```

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/promcscure reload` | `promcscure.admin` | Reload config and restart API |
| `/promcscure token` | `promcscure.admin` | Display current bearer token |
| `/promcscure status` | `promcscure.admin` | Show server status |
| `/register confirm <id>` | - | Confirm account registration |
| `/register deny <id>` | - | Deny account registration |

---

## REST Client Configuration

### Setting Up with Postman (Desktop)

#### Step 1: Create Environment

1. Click **Environments** → **Create**
2. Name: `ProMcScure`
3. Add variables:
   - `base_url`: `http://YOUR_SERVER_IP:8080`
   - `token`: `token_abc123...` (from /promcscure token)

#### Step 2: Create First Request

1. Click **Create** → **HTTP Request**
2. Change method to **GET**
3. URL: `{{base_url}}/info`
4. Go to **Auth** tab:
   - Type: ********
   - Token: `{{token}}`
5. Click **Send**

✓ You should see server info in the response!

#### Step 3: Save Request

1. Click **Save**
2. Name: `Get Server Info`
3. Collection: `Create New` → Name: `ProMcScure`

#### Step 4: Create More Requests

Duplicate the request and modify:
- `GET /players` - Get all players
- `GET /player/Steve` - Get player info
- `POST /execute` with body `{"commands": ["say Hello"]}`

---

### Setting Up with Thunder Client (VS Code)

#### Step 1: Install Extension

1. Open VS Code
2. Go to Extensions
3. Search "Thunder Client"
4. Click Install

#### Step 2: Create Collection

1. Click Thunder Client icon (left sidebar)
2. Click **New Collection**
3. Name: `ProMcScure`

#### Step 3: Create Request

1. Click **New Request**
2. Method: **GET**
3. URL: `http://YOUR_SERVER_IP:8080/info`
4. Headers tab:
   - Key: `Authorization`
   - Value: `******
5. Click **Send**

#### Step 4: Save Request

- Click **Save**
- Name: `Get Info`
- Collection: `ProMcScure`

---

### Setting Up with Insomnia

#### Step 1: Create Environment

1. Click **Environment** (left sidebar)
2. Click **+**
3. Name: `ProMcScure`
4. Add:
   ```json
   {
     "base_url": "http://YOUR_SERVER_IP:8080",
     "token": "token_abc123..."
   }
   ```

#### Step 2: Create Request

1. Click **Create** → **HTTP Request**
2. Method: **GET**
3. URL: `{{ base_url }}/info`
4. Auth tab:
   - Type: ********
   - Token: `{{ token }}`
5. Click **Send**

---

### Mobile REST Client Setup

#### Android - Postman App

1. Download Postman from Google Play Store
2. Create account or login
3. Create collection `ProMcScure`
4. Use instructions from [Desktop Postman Setup](#setting-up-with-postman-desktop)

#### iOS - REST Client

1. Download REST Client from App Store
2. Create new request
3. Method: GET
4. URL: `http://YOUR_SERVER_IP:8080/info`
5. Headers:
   - `Authorization: ******

#### Android - Thunder Client (Offline)

1. Install Thunder Client from Play Store
2. Same setup as VS Code version above

---

## API Endpoints

### 1. Get Server Info

```
GET /info
Authorization: ******
```

**Response:**
```json
{
  "success": true,
  "playerCount": 5,
  "maxPlayers": 20,
  "serverVersion": "Paper 1.21.3"
}
```

---

### 2. Get Players List

```
GET /players
Authorization: ******
```

**Response:**
```json
{
  "success": true,
  "players": ["Steve", "Alex"],
  "count": 2,
  "maxPlayers": 20
}
```

---

### 3. Get Player Info

```
GET /player/{username}
Authorization: ******
```

**Response:**
```json
{
  "success": true,
  "username": "Steve",
  "uuid": "...",
  "isOnline": true,
  "balance": 100.0,
  "level": 10,
  "isBanned": false,
  "isOp": false
}
```

---

### 4. Execute Commands

```
POST /execute
Authorization: ******
Content-Type: application/json

{
  "commands": ["say Hello!", "give @a diamond"],
  "waitForPlayer": "PlayerName" (optional)
}
```

**Response:**
```json
{
  "success": true,
  "statuses": ["passed", "passed"],
  "outputs": [null, null]
}
```

---

### 5. Validate Registration

```
POST /validate-registration
Authorization: ******
Content-Type: application/json

{
  "username": "PlayerName",
  "email": "player@example.com",
  "ip": "192.168.1.1",
  "callbackUrl": "https://yourwebsite.com/callback",
  "registrationId": "uuid-here"
}
```

---

## Troubleshooting

### "****** Invalid"

- ✓ Check token matches output of `/promcscure token`
- ✓ Make sure you're using `****** format
- ✓ Token is case-sensitive

### Connection Refused

- ✓ Server must be running
- ✓ Firewall may be blocking port 8080
- ✓ Check `/promcscure status` output

### "HTTPS is required" Error

This shouldn't happen with the latest version! If you see it:

- ✓ Make sure you have the latest JAR
- ✓ Check `require-https: false` in config.yml
- ✓ Run `/promcscure reload`

### Wrong Server Address

Find your server address:

**Local Network:**
- Ask your server admin for the internal IP
- Usually starts with `192.168.x.x` or `10.x.x.x`

**Internet:**
- Use your public IP or domain name
- Check `ifconfig` or `ipconfig`

---

## Security Best Practices

### 1. Change Default Token

The token is auto-generated but change it to something memorable:

```yaml
bearer-token: my-super-secret-token-12345
```

### 2. Use Strong Tokens

Good examples:
- `token_aB9xYz8qLm7nOp6kRsT5uV4`
- Mix uppercase, lowercase, numbers, symbols

### 3. Use HTTPS in Production

For servers accessible from the internet:

1. Set up reverse proxy (Nginx, Caddy)
2. Enable HTTPS with SSL certificate
3. Set `require-https: true` in config.yml

### 4. Firewall Rules

Only allow access from trusted IPs:

```
Allow: 192.168.1.50 → 8080
Deny:  * → 8080
```

### 5. Restrict Commands

Only allow safe commands via API:

```
✓ say, scoreboard, execute
✗ stop, reload, restart
```

---

## Performance Tips

### 1. Thread Pool

The server uses 4 threads by default. For high load:

Edit `HttpServerManager.java`:

```java
httpServer.setExecutor(Executors.newFixedThreadPool(8));
```

Then rebuild.

### 2. Rate Limiting

If needed, add rate limiting to your reverse proxy (Nginx, Caddy).

### 3. Command Queuing

Use `waitForPlayer` for heavy operations:

```json
{
  "commands": ["heavy command"],
  "waitForPlayer": "Steve"
}
```

The command executes when Steve joins, not immediately.

---

## More Information

- **Full API Documentation:** [MOBILE_REQUESTS.md](MOBILE_REQUESTS.md)
- **GitHub Repository:** https://github.com/1Dmar/minecraft-http-commands-plugin
- **Issues/Bugs:** https://github.com/1Dmar/minecraft-http-commands-plugin/issues

---

## FAQ

**Q: Does ProMcScure use a database?**
A: No, everything is stored in YAML files.

**Q: Can I run multiple servers with one token?**
A: Yes, generate a token on each server.

**Q: What if I lose my token?**
A: Edit `plugins/promcscure/config.yml` and change/regenerate it.

**Q: Can I use HTTP and HTTPS together?**
A: Yes! Set `require-https: false` to allow both.

**Q: How do I disable the token entirely?**
A: You can't - authorization is required for security.

**Q: Does it work on Windows?**
A: Yes! Paper runs on Windows, Linux, and macOS.

---

## Support

Need help?

1. Check this guide first
2. Check [GitHub Issues](https://github.com/1Dmar/minecraft-http-commands-plugin/issues)
3. Create a new issue with:
   - Server version
   - Plugin version
   - Error message
   - What you tried

---

## License

ProMcScure is licensed under the [MIT License](LICENSE).

Made with ❤️ for Minecraft server admins and developers!
