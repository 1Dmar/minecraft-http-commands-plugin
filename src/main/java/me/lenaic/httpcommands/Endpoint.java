package me.lenaic.httpcommands;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Interface for HTTP endpoint handlers.
 * Following SOLID principles:
 * - Single Responsibility: Each endpoint handles one route
 * - Open/Closed: New endpoints can be added without modifying existing code
 * - Liskov Substitution: All endpoints are interchangeable
 * - Interface Segregation: Minimal interface with focused methods
 * - Dependency Inversion: Depends on abstraction, not concrete implementations
 */
public interface Endpoint extends HttpHandler {

    /**
     * Get the HTTP path this endpoint handles (e.g., "/players", "/execute")
     * @return the path pattern
     */
    String getPath();

    /**
     * Get the HTTP method this endpoint handles (e.g., "GET", "POST")
     * @return the HTTP method
     */
    String getMethod();

    /**
     * Check if this endpoint requires authentication
     * @return true if authentication is required
     */
    boolean requiresAuth();

    /**
     * Handle the HTTP request
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    @Override
    void handle(HttpExchange exchange) throws IOException;

    /**
     * Helper method to send a JSON response
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param data the JSON object to send
     * @throws IOException if an I/O error occurs
     */
    default void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Helper method to send an error response
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param message the error message
     * @throws IOException if an I/O error occurs
     */
    default void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", false);
        jsonObject.addProperty("error", message);
        sendJsonResponse(exchange, statusCode, jsonObject);
    }

    /**
     * Helper method to send a success response
     * @param exchange the HTTP exchange
     * @param statusCode the HTTP status code
     * @param message the success message
     * @throws IOException if an I/O error occurs
     */
    default void sendSuccessResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", true);
        jsonObject.addProperty("message", message);
        sendJsonResponse(exchange, statusCode, jsonObject);
    }
}
