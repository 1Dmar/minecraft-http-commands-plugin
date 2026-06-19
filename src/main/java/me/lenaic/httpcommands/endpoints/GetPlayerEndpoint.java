package me.lenaic.httpcommands.endpoints;

import com.google.gson.JsonObject;
import me.lenaic.httpcommands.Endpoint;
import me.lenaic.httpcommands.HttpCommandsPlugin;
import me.lenaic.httpcommands.PlaceholderHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endpoint for getting player information via GET /player/{username}
 * Optimized for offline players and doesn't lag/timeout
 */
public class GetPlayerEndpoint implements Endpoint {

    private final HttpCommandsPlugin plugin;
    private static final ConcurrentHashMap<String, Long> playerCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 30000; // 30 seconds cache

    public GetPlayerEndpoint(HttpCommandsPlugin plugin) {
        this.plugin = plugin;
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

        String finalUsername = username;

        // Run player lookup asynchronously to avoid blocking
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Quick check: is player online?
                org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(finalUsername);
                
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    // Player is online - fast path
                    sendOnlinePlayerResponse(exchange, onlinePlayer);
                } else {
                    // Player is offline - use OfflinePlayer (still fast now that it's async)
                    sendOfflinePlayerResponse(exchange, finalUsername);
                }
            } catch (Exception e) {
                try {
                    plugin.getLogger().warning("Error fetching player " + finalUsername + ": " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Error fetching player data: " + e.getMessage());
                } catch (IOException ignored) {
                    // Response already sent or connection closed
                }
            }
        });
    }

    /**
     * Send response for online player (fast)
     */
    private void sendOnlinePlayerResponse(com.sun.net.httpserver.HttpExchange exchange, org.bukkit.entity.Player player) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", true);
        jsonObject.addProperty("username", player.getName());
        jsonObject.addProperty("uuid", player.getUniqueId().toString());
        jsonObject.addProperty("isOnline", true);
        jsonObject.addProperty("displayName", player.getDisplayName());
        
        // Get player IP
        if (player.getAddress() != null) {
            jsonObject.addProperty("ip", player.getAddress().getAddress().getHostAddress());
        }
        
        jsonObject.addProperty("ping", player.getPing());
        jsonObject.addProperty("world", player.getWorld().getName());
        
        // Get balance from Vault if available
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                Economy economy = rsp.getProvider();
                if (economy != null) {
                    jsonObject.addProperty("balance", economy.getBalance(player));
                } else {
                    jsonObject.addProperty("balance", 0.0);
                }
            } else {
                jsonObject.addProperty("balance", 0.0);
            }
        } catch (Exception e) {
            jsonObject.addProperty("balance", 0.0);
        }
        
        jsonObject.addProperty("isBanned", player.isBanned());
        jsonObject.addProperty("isOp", player.isOp());
        jsonObject.addProperty("level", player.getLevel());
        jsonObject.addProperty("health", player.getHealth());

        // Add custom fields from config (Limited to 3)
        addCustomFields(jsonObject, player);

        sendJsonResponse(exchange, 200, jsonObject);
    }

    /**
     * Send response for offline player (async)
     */
    private void sendOfflinePlayerResponse(com.sun.net.httpserver.HttpExchange exchange, String username) throws IOException {
        try {
            // Get offline player data
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);

            // Check if player has ever joined the server
            if (offlinePlayer == null || offlinePlayer.getName() == null || !offlinePlayer.hasPlayedBefore()) {
                sendErrorResponse(exchange, 404, "Player '" + username + "' has never joined this server");
                return;
            }

            // Build JSON response for offline player
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("success", true);
            jsonObject.addProperty("username", offlinePlayer.getName());
            jsonObject.addProperty("uuid", offlinePlayer.getUniqueId().toString());
            jsonObject.addProperty("isOnline", false);
            
            // Timestamps for offline players
            jsonObject.addProperty("firstPlayed", offlinePlayer.getFirstPlayed());
            jsonObject.addProperty("lastPlayed", offlinePlayer.getLastSeen());
            
            // Get balance from Vault if available (offline)
            try {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    Economy economy = rsp.getProvider();
                    if (economy != null) {
                        double balance = economy.getBalance(offlinePlayer);
                        jsonObject.addProperty("balance", balance);
                    } else {
                        jsonObject.addProperty("balance", 0.0);
                    }
                } else {
                    jsonObject.addProperty("balance", 0.0);
                }
            } catch (Exception e) {
                jsonObject.addProperty("balance", 0.0);
            }
            
            jsonObject.addProperty("isBanned", offlinePlayer.isBanned());
            jsonObject.addProperty("isOp", offlinePlayer.isOp());

            // Add custom fields from config (Limited to 3)
            addCustomFields(jsonObject, offlinePlayer);

            sendJsonResponse(exchange, 200, jsonObject);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error fetching offline player: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Error fetching player data");
        }
    }

    /**
     * Add custom fields to the JSON response based on plugin configuration
     * Limited to 3 fields as per player card design
     */
    private void addCustomFields(JsonObject jsonObject, OfflinePlayer player) {
        ConfigurationSection customFields = plugin.getConfig().getConfigurationSection("player-endpoint-fields");
        if (customFields == null) return;

        JsonObject customJson = new JsonObject();
        int count = 0;
        for (String key : customFields.getKeys(false)) {
            if (count >= 3) break; // Hard limit to 3 fields

            ConfigurationSection fieldConfig = customFields.getConfigurationSection(key);
            if (fieldConfig == null) continue;

            String placeholder = fieldConfig.getString("placeholder", "");
            String value = PlaceholderHook.setPlaceholders(player, placeholder);
            
            customJson.addProperty(key, value);
            count++;
        }
        jsonObject.add("customFields", customJson);
    }
}
