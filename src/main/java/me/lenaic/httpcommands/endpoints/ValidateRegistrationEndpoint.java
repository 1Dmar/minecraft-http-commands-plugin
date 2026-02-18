package me.lenaic.httpcommands.endpoints;

import com.google.gson.*;
import me.lenaic.httpcommands.Endpoint;
import me.lenaic.httpcommands.HttpCommandsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Endpoint for validating player registration via POST /validate-registration
 *
 * Request body (JSON):
 * {
 *   "username": "PlayerName",
 *   "email": "player@email.com",
 *   "ip": "192.168.1.1",
 *   "callbackUrl": "https://website.com/api/validate"
 * }
 *
 * This endpoint sends a chat message to the player asking for confirmation
 * to allow their account to be registered with the provided email and IP.
 * The player can click on Confirm/Deny buttons in the chat.
 */
public class ValidateRegistrationEndpoint implements Endpoint {

    private final HttpCommandsPlugin plugin;
    private final ConcurrentHashMap<UUID, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ValidateRegistrationEndpoint(HttpCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Store a pending registration for a player
     */
    public void addPendingRegistration(UUID playerId, PendingRegistration registration) {
        pendingRegistrations.put(playerId, registration);
        
        // Schedule expiry after 1 minute
        scheduler.schedule(() -> {
            PendingRegistration removed = pendingRegistrations.remove(playerId);
            if (removed != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text("Your registration request has expired. Please start again on the website.", NamedTextColor.RED));
                }
            }
        }, 1, TimeUnit.MINUTES);
    }

    /**
     * Get and remove a pending registration for a player
     */
    public PendingRegistration getPendingRegistration(UUID playerId) {
        return pendingRegistrations.remove(playerId);
    }

    @Override
    public String getPath() {
        return "/validate-registration";
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        // Only allow POST method
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed. Use POST for this endpoint.");
            return;
        }

        // Read request body
        String requestBody = readRequestBody(exchange);
        if (requestBody == null || requestBody.isEmpty()) {
            sendErrorResponse(exchange, 400, "Empty request body");
            return;
        }

        // Parse request data
        RequestData requestData = parseRequestData(requestBody);
        if (requestData == null) {
            sendErrorResponse(exchange, 400, "Invalid JSON format");
            return;
        }

        // Validate required fields
        if (requestData.username == null || requestData.username.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required field: username");
            return;
        }
        if (requestData.email == null || requestData.email.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required field: email");
            return;
        }
        if (requestData.ip == null || requestData.ip.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required field: ip");
            return;
        }
        if (requestData.callbackUrl == null || requestData.callbackUrl.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required field: callbackUrl");
            return;
        }
        if (requestData.registrationId == null || requestData.registrationId.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required field: registrationId");
            return;
        }

        // Send validation request to the player
        sendValidationMessage(exchange, requestData);
    }

    private void sendValidationMessage(com.sun.net.httpserver.HttpExchange exchange, RequestData requestData) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(requestData.username);

            if (player == null || !player.isOnline()) {
                try {
                    sendErrorResponse(exchange, 404, "Player '" + requestData.username + "' is not online");
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to send error response", e);
                }
                return;
            }

            // Use the registration ID from the request
            UUID registrationId = UUID.fromString(requestData.registrationId);
            
            // Store the pending registration
            PendingRegistration pending = new PendingRegistration(
                requestData.email, 
                requestData.ip, 
                requestData.callbackUrl,
                requestData.registrationId,
                System.currentTimeMillis()
            );
            addPendingRegistration(player.getUniqueId(), pending);

            // Build the validation message with clickable buttons
            Component confirmButton = Component.text()
                .content("[ CONFIRM ]")
                .color(TextColor.fromHexString("#22c55e"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to confirm registration", NamedTextColor.GREEN)))
                .clickEvent(ClickEvent.runCommand("/register confirm " + registrationId))
                .build();

            Component denyButton = Component.text()
                .content("[ DENY ]")
                .color(TextColor.fromHexString("#ef4444"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to deny registration", NamedTextColor.RED)))
                .clickEvent(ClickEvent.runCommand("/register deny " + registrationId))
                .build();

            Component message = Component.text()
                .append(Component.text("\n"))
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", NamedTextColor.GOLD))
                .append(Component.text("Account Registration Request\n", NamedTextColor.AQUA))
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n", NamedTextColor.GOLD))
                .append(Component.text("A website registration was attempted for your account.\n\n", NamedTextColor.WHITE))
                .append(Component.text("Email: ", NamedTextColor.GRAY))
                .append(Component.text(requestData.email + "\n", NamedTextColor.WHITE))
                .append(Component.text("IP Address: ", NamedTextColor.GRAY))
                .append(Component.text(requestData.ip + "\n\n", NamedTextColor.WHITE))
                .append(Component.text("Do you want to allow this registration?\n", NamedTextColor.WHITE))
                .append(Component.text("You have 1 minute to respond.\n\n", NamedTextColor.YELLOW))
                .append(confirmButton)
                .append(Component.text("  "))
                .append(denyButton)
                .append(Component.text("\n\n", NamedTextColor.WHITE))
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                .build();

            player.sendMessage(message);

            plugin.getLogger().info("Sent registration validation request to player: " + requestData.username);

            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("success", true);
                jsonObject.addProperty("message", "Validation request sent to player: " + requestData.username);
                jsonObject.addProperty("registrationId", requestData.registrationId);
                sendJsonResponse(exchange, 200, jsonObject);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send response", e);
            }
        });
    }

    /**
     * Send the player's response to the website
     */
    public void sendResponseToWebsite(Player player, UUID registrationId, boolean confirmed) {
        PendingRegistration registration = getPendingRegistration(player.getUniqueId());
        
        if (registration == null) {
            player.sendMessage(Component.text("This registration request has expired. Please start again on the website.", NamedTextColor.RED));
            return;
        }

        // Send POST request to callback URL asynchronously
        scheduler.execute(() -> {
            try {
                URL url = new URL(registration.getCallbackUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                JsonObject jsonBody = new JsonObject();
                jsonBody.addProperty("registrationId", registrationId.toString());
                jsonBody.addProperty("username", player.getName());
                jsonBody.addProperty("confirmed", confirmed);
                jsonBody.addProperty("email", registration.getEmail());
                jsonBody.addProperty("ip", registration.getIp());
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (responseCode == 410) {
                        // Expired
                        player.sendMessage(Component.text("This registration request has expired. Please start again on the website.", NamedTextColor.RED));
                    } else if (responseCode >= 200 && responseCode < 300) {
                        if (confirmed) {
                            player.sendMessage(Component.text("Registration confirmed! Your account has been linked.", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("Registration denied.", NamedTextColor.RED));
                        }
                    } else {
                        player.sendMessage(Component.text("Error communicating with the server. Please try again.", NamedTextColor.RED));
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send response to website", e);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Error communicating with the server. Please try again.", NamedTextColor.RED));
                });
            }
        });
    }

    private String readRequestBody(com.sun.net.httpserver.HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read request body", e);
            return null;
        }
    }

    private RequestData parseRequestData(String json) {
        RequestData data = new RequestData();
        try {
            JsonElement root = JsonParser.parseString(json);
            JsonObject rootObject = root.getAsJsonObject();

            JsonElement usernameElement = rootObject.get("username");
            if (usernameElement != null && !usernameElement.isJsonNull()) {
                data.username = usernameElement.getAsString();
            }

            JsonElement emailElement = rootObject.get("email");
            if (emailElement != null && !emailElement.isJsonNull()) {
                data.email = emailElement.getAsString();
            }

            JsonElement ipElement = rootObject.get("ip");
            if (ipElement != null && !ipElement.isJsonNull()) {
                data.ip = ipElement.getAsString();
            }
            
            JsonElement callbackUrlElement = rootObject.get("callbackUrl");
            if (callbackUrlElement != null && !callbackUrlElement.isJsonNull()) {
                data.callbackUrl = callbackUrlElement.getAsString();
            }
            
            JsonElement registrationIdElement = rootObject.get("registrationId");
            if (registrationIdElement != null && !registrationIdElement.isJsonNull()) {
                data.registrationId = registrationIdElement.getAsString();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse JSON", e);
            return null;
        }
        return data;
    }

    /**
     * Pending registration data
     */
    public static class PendingRegistration {
        private final String email;
        private final String ip;
        private final String callbackUrl;
        private final String registrationId;
        private final long timestamp;

        public PendingRegistration(String email, String ip, String callbackUrl, String registrationId, long timestamp) {
            this.email = email;
            this.ip = ip;
            this.callbackUrl = callbackUrl;
            this.registrationId = registrationId;
            this.timestamp = timestamp;
        }

        public String getEmail() {
            return email;
        }

        public String getIp() {
            return ip;
        }
        
        public String getCallbackUrl() {
            return callbackUrl;
        }
        
        public String getRegistrationId() {
            return registrationId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private static class RequestData {
        String username;
        String email;
        String ip;
        String callbackUrl;
        String registrationId;
    }
}
