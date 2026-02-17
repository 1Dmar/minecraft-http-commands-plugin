package me.lenaic.httpcommands;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Manages the HTTP server lifecycle
 */
public class HttpServerManager {

    private final HttpCommandsPlugin plugin;
    private final ConfigManager configManager;
    private final PendingCommandManager pendingCommandManager;
    private HttpServer httpServer;

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

            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // Create context for POST /execute
            RequestHandler requestHandler = new RequestHandler(plugin, configManager, pendingCommandManager);
            httpServer.createContext("/execute", requestHandler);
            
            // Create context for GET /players
            httpServer.createContext("/players", requestHandler);

            // Use a fixed thread pool for handling requests
            httpServer.setExecutor(Executors.newFixedThreadPool(4));

            httpServer.start();

            plugin.getLogger().info("HTTP server started on port " + port);
            plugin.getLogger().info("Endpoint: http://localhost:" + port + "/execute");
            plugin.getLogger().info("Endpoint: http://localhost:" + port + "/players");
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
