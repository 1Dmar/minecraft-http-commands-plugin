package me.lenaic.httpcommands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles HTTP requests for command execution
 */
public class RequestHandler implements HttpHandler {

    private final HttpCommandsPlugin plugin;
    private final ConfigManager configManager;
    private final PendingCommandManager pendingCommandManager;

    public RequestHandler(HttpCommandsPlugin plugin, ConfigManager configManager, PendingCommandManager pendingCommandManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.pendingCommandManager = pendingCommandManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check if HTTPS is required
        if (!checkHttps(exchange)) {
            // Send error response to inform the client
            sendJsonResponse(exchange, 400, "success", false, "HTTPS is required. Please use a reverse proxy with HTTPS.");
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        
        // Handle GET /players
        if ("/players".equals(path)) {
            handleGetPlayers(exchange);
            return;
        }
        
        // Handle GET /info
        if ("/info".equals(path)) {
            handleGetInfo(exchange);
            return;
        }
        
        // Only allow POST requests for other endpoints
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "success", false, "Method not allowed");
            return;
        }

        // Validate Bearer token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (!isValidAuth(authHeader)) {
            sendJsonResponse(exchange, 401, "success", false, "Unauthorized: Invalid or missing Bearer token");
            return;
        }

        // Read request body
        String requestBody = readRequestBody(exchange);
        if (requestBody == null || requestBody.isEmpty()) {
            sendJsonResponse(exchange, 400, "success", false, "Empty request body");
            return;
        }

        // Parse commands and waitForPlayer from JSON
        RequestData requestData = parseRequestData(requestBody);
        if (requestData == null || requestData.commands == null || requestData.commands.isEmpty()) {
            sendJsonResponse(exchange, 400, "success", false, "Missing or invalid 'commands' array in JSON");
            return;
        }

        // Check if we need to wait for a player
        if (requestData.waitForPlayer != null && !requestData.waitForPlayer.isEmpty()) {
            handleWaitForPlayer(exchange, requestData);
        } else {
            // Execute commands immediately
            executeCommandsSequentially(exchange, requestData.commands);
        }
    }

    /**
     * Check if the request was made over HTTPS
     * Returns true if HTTPS is used or not required, false otherwise
     */
    private boolean checkHttps(HttpExchange exchange) {
        // Check if HTTPS is required
        if (!configManager.isRequireHttps()) {
            return true;
        }
        
        // Check X-Forwarded-Proto header (set by reverse proxies)
        String forwardedProto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        
        boolean isHttps = "https".equalsIgnoreCase(forwardedProto);
        
        if (!isHttps) {
            // Log error to server console
            String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
            plugin.getLogger().severe("=================================================");
            plugin.getLogger().severe("SECURITY WARNING: Non-HTTPS request received!");
            plugin.getLogger().severe("Request from: " + remoteAddr);
            plugin.getLogger().severe("This plugin does not support HTTPS directly.");
            plugin.getLogger().severe("Please configure a reverse proxy with HTTPS in front of this server.");
            plugin.getLogger().severe("You should not use HTTP access, as it is insecure.");
            plugin.getLogger().severe("To allow direct HTTP access, set 'require-https: false' in config.yml");
            plugin.getLogger().severe("=================================================");
        }
        
        return isHttps;
    }

    /**
     * Handle the waitForPlayer logic
     */
    private void handleWaitForPlayer(HttpExchange exchange, RequestData requestData) {
        String playerName = requestData.waitForPlayer;
        
        // Check if player is online
        OfflinePlayer player = Bukkit.getPlayer(playerName);
        
        if (player != null && player.isOnline()) {
            // Player is online, execute commands immediately
            plugin.getLogger().info("Player " + playerName + " is online, executing commands immediately");
            executeCommandsSequentially(exchange, requestData.commands);
        } else {
            // Player is offline, save commands as pending
            plugin.getLogger().info("Player " + playerName + " is offline, saving commands as pending");
            pendingCommandManager.addPendingCommand(playerName, requestData.commands);
            
            try {
                sendJsonResponse(exchange, 202, "success", true, "Commands saved and will execute when player '" + playerName + "' joins");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send response", e);
            }
        }
    }

    /**
     * Data class to hold parsed request data
     */
    private static class RequestData {
        List<String> commands;
        String waitForPlayer;
    }

    /**
     * Parse commands and waitForPlayer from JSON
     */
    private RequestData parseRequestData(String json) {
        RequestData data = new RequestData();
        
        try {
            JsonElement root = JsonParser.parseString(json);
            JsonObject rootObject = root.getAsJsonObject();
            
            // Parse commands array
            JsonArray commandsArray = rootObject.getAsJsonArray("commands");
            if (commandsArray != null) {
                data.commands = new ArrayList<>();
                for (JsonElement element : commandsArray) {
                    data.commands.add(element.getAsString());
                }
            }
            
            // Parse waitForPlayer (optional, defaults to null)
            JsonElement waitForPlayerElement = rootObject.get("waitForPlayer");
            if (waitForPlayerElement != null && !waitForPlayerElement.isJsonNull()) {
                data.waitForPlayer = waitForPlayerElement.getAsString();
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON", e);
            return null;
        }
        
        return data;
    }

    /**
     * Validate Bearer token from Authorization header
     */
    private boolean isValidAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return token.equals(configManager.getBearerToken());
    }

    /**
     * Handle GET /players - returns list of online players
     */
    private void handleGetPlayers(HttpExchange exchange) throws IOException {
        // Validate Bearer token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (!isValidAuth(authHeader)) {
            sendJsonResponse(exchange, 401, "success", false, "Unauthorized: Invalid or missing Bearer token");
            return;
        }

        // Get online players
        List<String> playerNames = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> playerNames.add(player.getName()));
        int maxPlayers = Bukkit.getMaxPlayers();

        // Build JSON response
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"players\":[");
        for (int i = 0; i < playerNames.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"");
            json.append(escapeJson(playerNames.get(i)));
            json.append("\"");
        }
        json.append("],\"count\":");
        json.append(playerNames.size());
        json.append(",\"maxPlayers\":");
        json.append(maxPlayers);
        json.append("}");

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Handle GET /info - returns server info
     */
    private void handleGetInfo(HttpExchange exchange) throws IOException {
        // Validate Bearer token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (!isValidAuth(authHeader)) {
            sendJsonResponse(exchange, 401, "success", false, "Unauthorized: Invalid or missing Bearer token");
            return;
        }

        // Get server info
        int playerCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        String serverVersion = Bukkit.getVersion();

        // Build JSON response
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"playerCount\":");
        json.append(playerCount);
        json.append(",\"maxPlayers\":");
        json.append(maxPlayers);
        json.append(",\"serverVersion\":\"");
        json.append(escapeJson(serverVersion));
        json.append("\"}");

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Read request body as string
     */
    private String readRequestBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read request body", e);
            return null;
        }
    }

    /**
     * Parse commands array from JSON using Gson
     * @deprecated Use parseRequestData instead
     */
    @Deprecated
    private List<String> parseCommandsFromJson(String json) {
        List<String> commands = new ArrayList<>();
        
        try {
            JsonElement root = JsonParser.parseString(json);
            JsonArray commandsArray = root.getAsJsonObject().getAsJsonArray("commands");
            
            if (commandsArray != null) {
                for (JsonElement element : commandsArray) {
                    commands.add(element.getAsString());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON", e);
        }
        
        return commands;
    }

    /**
     * Execute commands sequentially and send response
     */
    private void executeCommandsSequentially(HttpExchange exchange, List<String> commands) {
        // Use Bukkit scheduler to execute commands synchronously
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                List<String> statuses = new ArrayList<>();
                List<String> outputs = new ArrayList<>();
                
                // Execute each command sequentially
                for (String command : commands) {
                    CommandResult result = executeCommandWithOutput(command);
                    statuses.add(result.status);
                    outputs.add(result.output);
                }

                sendJsonWithStatusAndOutputs(exchange, 200, "success", true, statuses, outputs);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing commands", e);
                try {
                    sendJsonResponse(exchange, 500, "success", false, "Error: " + e.getMessage());
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to send error response", ex);
                }
            }
        });
    }

    /**
     * Result class for command execution
     */
    private static class CommandResult {
        String status;
        String output;
        
        CommandResult(String status, String output) {
            this.status = status;
            this.output = output;
        }
    }

    /**
     * Execute a command and capture its output using Paper's createCommandSender API
     */
    private CommandResult executeCommandWithOutput(String command) {
        StringBuilder outputBuilder = new StringBuilder();
        
        // Create a consumer that captures all feedback/output
        Consumer<Component> feedbackConsumer = component -> {
            // Convert the Adventure Component to JSON text
            String text = GsonComponentSerializer.gson().serialize(component);
            if (outputBuilder.length() > 0) {
                outputBuilder.append("\n");
            }
            outputBuilder.append(text);
        };
        
        // Create a custom command sender that captures output
        CommandSender customSender = Bukkit.createCommandSender(feedbackConsumer);
        
        // Dispatch the command
        boolean success = Bukkit.dispatchCommand(customSender, command);
        
        String output;
        String status;
        
        if (!success) {
            status = "failed";
            output = outputBuilder.length() > 0 ? outputBuilder.toString() : null;
        } else if (outputBuilder.length() == 0) {
            status = "passed";
            output = null;
        } else {
            status = "passed";
            output = outputBuilder.toString();
        }
        
        return new CommandResult(status, output);
    }

    /**
     * Send JSON response with status and outputs arrays
     */
    private void sendJsonWithStatusAndOutputs(HttpExchange exchange, int statusCode, String key, boolean value, List<String> statuses, List<String> outputs) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\"");
        json.append(key);
        json.append("\":");
        json.append(value);
        json.append(",\"statuses\":[");
        
        // Build statuses array
        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"");
            json.append(escapeJson(statuses.get(i)));
            json.append("\"");
        }
        
        json.append("],\"outputs\":[");
        
        // Build outputs array - keep JSON as objects, use null for empty
        for (int i = 0; i < outputs.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            String output = outputs.get(i);
            if (output == null) {
                json.append("null");
            } else {
                // Output is already JSON from Adventure, use it directly
                json.append(output);
            }
        }
        
        json.append("]}");
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String key, boolean value, String output) throws IOException {
        String json = String.format("{\"%s\":%b,\"output\":\"%s\"}", key, value, escapeJson(output));
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Send JSON response with array output
     */
    private void sendJsonArrayResponse(HttpExchange exchange, int statusCode, String key, boolean value, List<String> outputs) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\"");
        json.append(key);
        json.append("\":");
        json.append(value);
        json.append(",\"outputs\":[");
        
        for (int i = 0; i < outputs.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"");
            json.append(escapeJson(outputs.get(i)));
            json.append("\"");
        }
        
        json.append("]}");
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
