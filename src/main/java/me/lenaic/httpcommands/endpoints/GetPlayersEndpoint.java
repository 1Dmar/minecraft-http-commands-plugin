package me.lenaic.httpcommands.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lenaic.httpcommands.Endpoint;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoint for getting the list of online players via GET /players
 *
 * Response (JSON):
 * {
 *   "success": true,
 *   "players": ["player1", "player2"],
 *   "count": 2,
 *   "maxPlayers": 100
 * }
 */
public class GetPlayersEndpoint implements Endpoint {

    public GetPlayersEndpoint() {
    }

    @Override
    public String getPath() {
        return "/players";
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

        // Get online players
        List<String> playerNames = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> playerNames.add(player.getName()));
        int maxPlayers = Bukkit.getMaxPlayers();

        // Build JSON response
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", true);

        JsonArray playersArray = new JsonArray();
        for (String playerName : playerNames) {
            playersArray.add(playerName);
        }
        jsonObject.add("players", playersArray);
        jsonObject.addProperty("count", playerNames.size());
        jsonObject.addProperty("maxPlayers", maxPlayers);

        sendJsonResponse(exchange, 200, jsonObject);
    }
}
