package me.lenaic.httpcommands;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Router that manages endpoint registration and request routing.
 * Follows the Open/Closed Principle - new endpoints can be added without modifying this class.
 *
 * To add a new endpoint:
 * 1. Create a new class implementing the Endpoint interface
 * 2. Register it with router.registerEndpoint(new YourEndpoint())
 * 3. No need to modify Router or any existing endpoint code
 */
public class Router implements HttpHandler {

    private final Map<String, Endpoint> endpoints = new LinkedHashMap<>();
    private final Map<Pattern, Endpoint> patternEndpoints = new LinkedHashMap<>();
    private final ConfigManager configManager;
    private final Logger logger;

    public Router(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * Register an endpoint with the router
     * @param endpoint the endpoint to register
     */
    public void registerEndpoint(Endpoint endpoint) {
        String path = endpoint.getPath();
        
        // Check if path contains path parameters (e.g., /player/{username})
        if (path.contains("{") && path.contains("}")) {
            // Convert {param} to regex pattern with capture group
            String regexPath = path.replaceAll("\\{([^}]+)\\}", "([^/]+)");
            Pattern pattern = Pattern.compile("^" + regexPath + "$");
            patternEndpoints.put(pattern, endpoint);
        } else {
            String key = endpoint.getMethod().toUpperCase() + ":" + path;
            endpoints.put(key, endpoint);
        }
    }

    /**
     * Find endpoint for the given path (supports path parameters)
     * @param method the HTTP method
     * @param path the request path
     * @return the endpoint and extracted path parameters, or null if not found
     */
    private Map.Entry<Endpoint, Map<String, String>> findEndpoint(String method, String path) {
        // First, try exact match
        String key = method.toUpperCase() + ":" + path;
        Endpoint endpoint = endpoints.get(key);
        if (endpoint != null) {
            return new AbstractMap.SimpleEntry<>(endpoint, Collections.emptyMap());
        }
        
        // Then try pattern matching
        for (Map.Entry<Pattern, Endpoint> entry : patternEndpoints.entrySet()) {
            java.util.regex.Matcher matcher = entry.getKey().matcher(path);
            if (matcher.matches()) {
                // Get the endpoint from the entry
                Endpoint matchedEndpoint = entry.getValue();
                
                // Extract path parameters
                Map<String, String> params = new HashMap<>();
                
                // Extract parameter names from pattern like /player/{username}
                java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
                java.util.regex.Matcher paramMatcher = paramPattern.matcher(matchedEndpoint.getPath());
                
                int groupIndex = 1;
                while (paramMatcher.find()) {
                    String paramName = paramMatcher.group(1);
                    params.put(paramName, matcher.group(groupIndex));
                    groupIndex++;
                }
                
                return new AbstractMap.SimpleEntry<>(matchedEndpoint, params);
            }
        }
        
        return null;
    }

    /**
     * Get all registered endpoints (for documentation/debugging)
     * @return list of endpoint descriptions
     */
    public List<String> getRegisteredEndpoints() {
        List<String> result = new ArrayList<>();
        for (Endpoint endpoint : endpoints.values()) {
            result.add(endpoint.getMethod() + " " + endpoint.getPath());
        }
        for (Endpoint endpoint : patternEndpoints.values()) {
            result.add(endpoint.getMethod() + " " + endpoint.getPath());
        }
        return result;
    }

    /**
     * Handle incoming HTTP requests - routes to the appropriate endpoint
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Check if HTTPS is required
        if (!checkHttps(exchange)) {
            sendErrorResponse(exchange, 400, "HTTPS is required. Please use a reverse proxy with HTTPS.");
            return;
        }

        // Find the matching endpoint (supports path parameters)
        Map.Entry<Endpoint, Map<String, String>> matchedEndpoint = findEndpoint(method, path);
        
        if (matchedEndpoint == null) {
            sendErrorResponse(exchange, 404, "Endpoint not found: " + method + " " + path);
            return;
        }

        Endpoint endpoint = matchedEndpoint.getKey();
        Map<String, String> pathParams = matchedEndpoint.getValue();
        
        // Store path parameters in exchange attributes for endpoints to access
        if (!pathParams.isEmpty()) {
            exchange.setAttribute("pathParams", pathParams);
        }

        // Check authentication if required
        if (endpoint.requiresAuth()) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (!isValidAuth(authHeader)) {
                sendErrorResponse(exchange, 401, "Unauthorized: Invalid or missing Bearer token");
                return;
            }
        }

        // Delegate to the endpoint handler
        try {
            endpoint.handle(exchange);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling request for " + path, e);
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Check if the request was made over HTTPS
     */
    private boolean checkHttps(HttpExchange exchange) {
        if (!configManager.isRequireHttps()) {
            return true;
        }

        String forwardedProto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        boolean isHttps = "https".equalsIgnoreCase(forwardedProto);

        if (!isHttps) {
            String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
            logger.severe("SECURITY WARNING: Non-HTTPS request received from " + remoteAddr);
        }

        return isHttps;
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
     * Send an error response
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
        jsonObject.addProperty("success", false);
        jsonObject.addProperty("error", message);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonObject.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
