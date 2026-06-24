# Mobile API Requests Guide

This guide explains how to send HTTP requests to the Minecraft HTTP Commands Plugin from mobile applications, web applications, or any HTTP client.

## Prerequisites

1. The plugin must be installed on your Minecraft server
2. You need the server's IP/hostname and port (default: 8080)
3. You need a valid ****** (set in `config.yml`)

## Basic Request Format

All requests use JSON format and require authentication via ******

### Request Headers

```
Authorization: ******
Content-Type: application/json
```

Replace `YOUR_TOKEN_HERE` with the token from your `config.yml` file.

## Endpoints

### 1. Execute Command

Execute a command on the Minecraft server.

**Endpoint:** `POST /execute-command`

**Request Body:**
```json
{
  "command": "say Hello from mobile!"
}
```

**Example Response (Success):**
```json
{
  "success": true,
  "message": "Command queued for execution"
}
```

**Example Response (Error):**
```json
{
  "success": false,
  "error": "Unauthorized: Invalid or missing ******"
}
```

---

### 2. Get Server Info

Get basic information about the Minecraft server.

**Endpoint:** `GET /info`

**Example Response:**
```json
{
  "success": true,
  "serverName": "My Awesome Server",
  "motd": "Welcome to my server!",
  "onlinePlayers": 5,
  "maxPlayers": 20
}
```

---

### 3. Get Players List

Get a list of all online players.

**Endpoint:** `GET /players`

**Example Response:**
```json
{
  "success": true,
  "players": [
    "Steve",
    "Alex",
    "NotchTheCreator"
  ]
}
```

---

### 4. Get Player Info

Get information about a specific player.

**Endpoint:** `GET /player/{username}`

Replace `{username}` with the player's username.

**Example Response:**
```json
{
  "success": true,
  "username": "Steve",
  "isOnline": true,
  "health": 20,
  "hunger": 20,
  "experience": 5000,
  "level": 10,
  "gameMode": "SURVIVAL"
}
```

---

### 5. Register Command for Offline Player

Register a command to be executed when a player joins the server.

**Endpoint:** `POST /register-command`

**Request Body:**
```json
{
  "username": "Steve",
  "command": "give @p diamond 64"
}
```

**Example Response:**
```json
{
  "success": true,
  "message": "Command registered for player Steve"
}
```

---

### 6. Validate Registration

Check if a pending command was executed.

**Endpoint:** `POST /validate-registration`

**Request Body:**
```json
{
  "username": "Steve"
}
```

**Example Response (Command Executed):**
```json
{
  "success": true,
  "status": "EXECUTED",
  "message": "Command has been executed"
}
```

**Example Response (Command Pending):**
```json
{
  "success": true,
  "status": "PENDING",
  "message": "Command is waiting for player to join"
}
```

---

## Platform-Specific Examples

### cURL (Command Line)

**Execute Command:**
```bash
curl -X POST http://your-server-ip:8080/execute-command \
  -H "Authorization: ******" \
  -H "Content-Type: application/json" \
  -d '{"command": "say Hello from cURL!"}'
```

**Get Server Info:**
```bash
curl -X GET http://your-server-ip:8080/info \
  -H "Authorization: ******"
```

**Get Players List:**
```bash
curl -X GET http://your-server-ip:8080/players \
  -H "Authorization: ******"
```

**Get Player Info:**
```bash
curl -X GET http://your-server-ip:8080/player/Steve \
  -H "Authorization: ******"
```

---

### JavaScript/Web

**Fetch API - Execute Command:**
```javascript
const executeCommand = async (command) => {
  const response = await fetch('http://your-server-ip:8080/execute-command', {
    method: 'POST',
    headers: {
      'Authorization': '******',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      command: command
    })
  });
  
  const data = await response.json();
  console.log(data);
  return data;
};

// Usage
executeCommand('say Hello from web!');
```

**Fetch API - Get Server Info:**
```javascript
const getServerInfo = async () => {
  const response = await fetch('http://your-server-ip:8080/info', {
    method: 'GET',
    headers: {
      'Authorization': '******'
    }
  });
  
  const data = await response.json();
  console.log(data);
  return data;
};

// Usage
getServerInfo();
```

**Fetch API - Get Players:**
```javascript
const getPlayers = async () => {
  const response = await fetch('http://your-server-ip:8080/players', {
    method: 'GET',
    headers: {
      'Authorization': '******'
    }
  });
  
  const data = await response.json();
  console.log(data);
  return data;
};

// Usage
getPlayers();
```

---

### Python

**Execute Command:**
```python
import requests
import json

def execute_command(server_ip, token, command):
    url = f"http://{server_ip}:8080/execute-command"
    headers = {
        "Authorization": f"******",
        "Content-Type": "application/json"
    }
    payload = {
        "command": command
    }
    
    response = requests.post(url, headers=headers, json=payload)
    print(response.json())

# Usage
execute_command("your-server-ip", "your-token-here", "say Hello from Python!")
```

**Get Server Info:**
```python
import requests

def get_server_info(server_ip, token):
    url = f"http://{server_ip}:8080/info"
    headers = {
        "Authorization": f"******"
    }
    
    response = requests.get(url, headers=headers)
    print(response.json())

# Usage
get_server_info("your-server-ip", "your-token-here")
```

**Get Players:**
```python
import requests

def get_players(server_ip, token):
    url = f"http://{server_ip}:8080/players"
    headers = {
        "Authorization": f"******"
    }
    
    response = requests.get(url, headers=headers)
    print(response.json())

# Usage
get_players("your-server-ip", "your-token-here")
```

**Get Player Info:**
```python
import requests

def get_player_info(server_ip, token, username):
    url = f"http://{server_ip}:8080/player/{username}"
    headers = {
        "Authorization": f"******"
    }
    
    response = requests.get(url, headers=headers)
    print(response.json())

# Usage
get_player_info("your-server-ip", "your-token-here", "Steve")
```

---

### Java/Android

**Execute Command (using HttpURLConnection):**
```java
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public void executeCommand(String serverIp, String token, String command) throws Exception {
    URL url = new URL("http://" + serverIp + ":8080/execute-command");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    
    // Set request method and headers
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);
    
    // Send request body
    String jsonBody = "{\"command\": \"" + command + "\"}";
    try (OutputStream os = connection.getOutputStream()) {
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }
    
    // Read response
    int responseCode = connection.getResponseCode();
    System.out.println("Response Code: " + responseCode);
}

// Usage
try {
    executeCommand("your-server-ip", "your-token-here", "say Hello from Android!");
} catch (Exception e) {
    e.printStackTrace();
}
```

**Get Server Info (using HttpURLConnection):**
```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public void getServerInfo(String serverIp, String token) throws Exception {
    URL url = new URL("http://" + serverIp + ":8080/info");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    
    // Set request method and headers
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + token);
    
    // Read response
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream())
    );
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
    reader.close();
}

// Usage
try {
    getServerInfo("your-server-ip", "your-token-here");
} catch (Exception e) {
    e.printStackTrace();
}
```

---

### Kotlin/Android (Retrofit)

**Using Retrofit:**
```kotlin
import retrofit2.http.*

interface MinecraftApiService {
    @POST("/execute-command")
    suspend fun executeCommand(
        @Header("Authorization") token: String,
        @Body request: ExecuteCommandRequest
    ): ApiResponse<Unit>
    
    @GET("/info")
    suspend fun getServerInfo(
        @Header("Authorization") token: String
    ): ApiResponse<ServerInfo>
    
    @GET("/players")
    suspend fun getPlayers(
        @Header("Authorization") token: String
    ): ApiResponse<List<String>>
    
    @GET("/player/{username}")
    suspend fun getPlayerInfo(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): ApiResponse<PlayerInfo>
}

// Data classes
data class ExecuteCommandRequest(val command: String)
data class ServerInfo(val serverName: String, val onlinePlayers: Int, val maxPlayers: Int)
data class PlayerInfo(val username: String, val isOnline: Boolean, val level: Int)
data class ApiResponse<T>(val success: Boolean, val data: T?, val error: String?)

// Usage
class MinecraftManager(private val api: MinecraftApiService, private val token: String) {
    suspend fun executeCommand(command: String) {
        try {
            val response = api.executeCommand(
                "******",
                ExecuteCommandRequest(command)
            )
            if (response.success) {
                println("Command executed successfully")
            } else {
                println("Error: ${response.error}")
            }
        } catch (e: Exception) {
            println("Network error: ${e.message}")
        }
    }
    
    suspend fun getServerInfo() {
        try {
            val response = api.getServerInfo("******")
            if (response.success && response.data != null) {
                println("Server: ${response.data.serverName}")
                println("Players: ${response.data.onlinePlayers}/${response.data.maxPlayers}")
            }
        } catch (e: Exception) {
            println("Network error: ${e.message}")
        }
    }
}
```

---

## Configuration

### Using HTTP (Direct Connection)

1. Open `plugins/http-commands/config.yml`
2. Set `require-https: false` (already default)
3. Set your server IP/port
4. Change the bearer token to something secure

Example config:
```yaml
port: 8080
host: "0.0.0.0"
require-https: false
bearer-token: "your-secure-token-here"
```

Then make requests like:
```
http://your-server-ip:8080/info
```

### Using HTTPS (Reverse Proxy)

For production, use a reverse proxy like Nginx with SSL/TLS:

1. Set `require-https: true` in config.yml
2. Configure your reverse proxy to forward requests to `127.0.0.1:8080`
3. Set the `X-Forwarded-Proto: https` header in your reverse proxy

Then make requests like:
```
https://your-domain.com/info
```

---

## Troubleshooting

### "HTTPS is required" Error

**Solution 1: Use HTTP (Development Only)**
- Set `require-https: false` in `config.yml`
- Restart the server
- Make requests over HTTP

**Solution 2: Use a Reverse Proxy (Production)**
- Set up Nginx, Caddy, or similar
- Configure it to forward to `127.0.0.1:8080`
- Ensure the proxy sets `X-Forwarded-Proto: https`

### "Unauthorized" Error

- Check that you're sending the `Authorization: ****** header
- Verify the token matches the one in `config.yml`
- Token is case-sensitive

### Connection Refused

- Verify the server IP and port are correct
- Check that the Minecraft server is running
- Check that no firewall is blocking the port
- Verify the plugin is installed: `/http-commands reload`

### Timeout Errors

- The server might be overloaded
- Try again with a longer timeout
- Check server logs with `/http-commands reload`

---

## Security Best Practices

1. **Change the bearer token** - Use a strong, random token in production
2. **Use HTTPS** - Always use a reverse proxy with SSL/TLS in production
3. **Firewall** - Only allow access from trusted IPs if possible
4. **Commands** - Only expose commands that are safe
5. **Logging** - Monitor server logs for suspicious activity

---

## API Documentation

For complete API documentation with interactive examples, visit:
https://minecraft-http-commands-plugin-api-doc.lenaic.me

Or generate it locally:
```bash
./gradlew openApiGenerate
```

Open `docs/index.html` in your browser.
