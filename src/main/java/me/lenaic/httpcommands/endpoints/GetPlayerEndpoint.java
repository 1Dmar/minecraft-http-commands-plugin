package me.lenaic.httpcommands.endpoints;

import com.google.gson.JsonObject;
import me.lenaic.httpcommands.Endpoint;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint for getting player information via GET /player/{username}
 *
 * Response (JSON):
 * {
 *   "success": true,
 *   "username": "Player1",
 *   "uuid": "...",
 *   "isOnline": true,
 *   "displayName": "Player1",
 *   "ip": "127.0.0.1",
 *   "ping": 20,
 *   "world": "world",
 *   "balance": 100.0,
 *   "firstPlayed": 1234567890,
 *   "lastPlayed": 1234567890,
 *   "isBanned": false,
 *   "isOp": false
 * }
 */
public class GetPlayerEndpoint implements Endpoint {

    public GetPlayerEndpoint() {
    }

    @Override
    public String getPath() {
        return "/player/{username}";
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        // Only allow GET method
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed. Use GET for this endpoint.");
            return;
        }

        // Get path parameters from the exchange attribute
        @SuppressWarnings("unchecked")
        Map<String, String> pathParams = (Map<String, String>) exchange.getAttribute("pathParams");
        
        String username = pathParams != null ? pathParams.get("username") : null;
        
        // Try to get username from URL path if not in pathParams
        if (username == null || username.isEmpty()) {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/player/")) {
                username = path.substring("/player/".length());
            }
        }

        // Validate username
        if (username == null || username.isEmpty()) {
            sendErrorResponse(exchange, 400, "Missing required parameter: username");
            return;
        }

        // Build response synchronously (same approach as other endpoints)
        JsonObject jsonObject = new JsonObject();
        
        try {
            // Try to find the player
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);

            if (offlinePlayer == null || offlinePlayer.getName() == null) {
                sendErrorResponse(exchange, 404, "Player not found: " + username);
                return;
            }

            // Build JSON response
            jsonObject.addProperty("success", true);
            jsonObject.addProperty("username", offlinePlayer.getName());
            
            UUID uuid = offlinePlayer.getUniqueId();
            jsonObject.addProperty("uuid", uuid != null ? uuid.toString() : null);
            jsonObject.addProperty("isOnline", offlinePlayer.isOnline());
            
            // First and last played timestamps
            try {
                jsonObject.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
                jsonObject.addProperty("lastPlayed", offlinePlayer.getLastSeen());
            } catch (Exception e) {
                // Ignore - player might be new
            }
            
            jsonObject.addProperty("isBanned", offlinePlayer.isBanned());
            jsonObject.addProperty("isOp", offlinePlayer.isOp());

            // Get player balance from Vault API
            try {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    Economy economy = rsp.getProvider();
                    if (economy != null && offlinePlayer.getName() != null) {
                        double balance = economy.getBalance(offlinePlayer);
                        jsonObject.addProperty("balance", balance);
                    } else {
                        jsonObject.addProperty("balance", 0.0);
                    }
                } else {
                    jsonObject.addProperty("balance", 0.0);
                }
            } catch (Exception e) {
                // Vault or economy plugin not available
                jsonObject.addProperty("balance", 0.0);
            }

            sendJsonResponse(exchange, 200, jsonObject);
            
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Error: " + e.getMessage());
        }
    }
}
