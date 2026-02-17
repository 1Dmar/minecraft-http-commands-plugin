package me.lenaic.httpcommands;

import me.lenaic.httpcommands.endpoints.ExecuteCommandEndpoint;
import me.lenaic.httpcommands.endpoints.GetInfoEndpoint;
import me.lenaic.httpcommands.endpoints.GetPlayersEndpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Manages the HTTP server lifecycle
 *
 * To add a new endpoint:
 * 1. Create a new class implementing the Endpoint interface
 * 2. Register it with router.registerEndpoint(new YourEndpoint())
 * 3. That's it! No need to modify this class for routing
 */
public class HttpServerManager {

    private final HttpCommandsPlugin plugin;
    private final ConfigManager configManager;
    private final PendingCommandManager pendingCommandManager;
    private com.sun.net.httpserver.HttpServer httpServer;
    private Router router;

    public HttpServerManager(HttpCommandsPlugin plugin, ConfigManager configManager, PendingCommandManager pendingCommandManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.pendingCommandManager = pendingCommandManager;
    }

    /**
     * Start the HTTP server
     */
    public void start() {
        try {
            int port = configManager.getPort();
            httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);

            // Create and configure the router
            router = new Router(configManager, plugin.getLogger());

            // Register all endpoints - NEW ENDPOINTS GO HERE
            router.registerEndpoint(new ExecuteCommandEndpoint(plugin, pendingCommandManager));
            router.registerEndpoint(new GetPlayersEndpoint());
            router.registerEndpoint(new GetInfoEndpoint());

            // Register the router as the handler for all requests
            httpServer.createContext("/", router);

            // Log registered endpoints
            plugin.getLogger().info("Registered endpoints: " + router.getRegisteredEndpoints());

            // Use a fixed thread pool for handling requests
            httpServer.setExecutor(Executors.newFixedThreadPool(4));
            httpServer.start();

            plugin.getLogger().info("HTTP server started on port " + port);
            plugin.getLogger().info("Endpoint: http://localhost:" + port + "/");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start HTTP server: " + e.getMessage());
        }
    }

    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            plugin.getLogger().info("HTTP server stopped");
            httpServer = null;
        }
    }
}
