package me.lenaic.httpcommands.endpoints;

import com.google.gson.JsonObject;
import me.lenaic.httpcommands.Endpoint;
import org.bukkit.Bukkit;

import java.io.IOException;

/**
 * Endpoint for getting server information via GET /info
 *
 * Response (JSON):
 * {
 *   "success": true,
 *   "playerCount": 5,
 *   "maxPlayers": 100,
 *   "serverVersion": "Paper 1.21.3"
 * }
 */
public class GetInfoEndpoint implements Endpoint {

    public GetInfoEndpoint() {
    }

    @Override
    public String getPath() {
        return "/info";
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

        // Get server info
        int playerCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        String serverVersion = Bukkit.getVersion();

        // Build JSON response
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", true);
        jsonObject.addProperty("playerCount", playerCount);
        jsonObject.addProperty("maxPlayers", maxPlayers);
        jsonObject.addProperty("serverVersion", serverVersion);

        sendJsonResponse(exchange, 200, jsonObject);
    }
}
