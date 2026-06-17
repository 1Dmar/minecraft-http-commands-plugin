package me.lenaic.httpcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.util.List;

/**
 * Main plugin class for ProMcScure plugin
 * Provides HTTP endpoint to execute Minecraft console commands
 */
public final class HttpCommandsPlugin extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private HttpServerManager httpServerManager;
    private PendingCommandManager pendingCommandManager;

    @Override
    public void onEnable() {
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize pending command manager (loads from file)
        pendingCommandManager = new PendingCommandManager(this);

        // Initialize and start HTTP server
        httpServerManager = new HttpServerManager(this, configManager, pendingCommandManager);
        httpServerManager.start();

        int pluginId = 29591;
        new Metrics(this, pluginId);

        // Register event listener for player join
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("ProMcScure plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Stop HTTP server
        if (httpServerManager != null) {
            httpServerManager.stop();
        }

        getLogger().info("ProMcScure plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("http-commands")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("httpcommands.reload")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }

                // Reload configuration
                configManager.reloadConfig();

                // Restart HTTP server with new config
                httpServerManager.stop();
                httpServerManager = new HttpServerManager(this, configManager, pendingCommandManager);
                httpServerManager.start();

                sender.sendMessage("§aConfiguration reloaded and HTTP server restarted!");
                return true;
            }

            sender.sendMessage("§cUsage: /http-commands reload");
            return true;
        }

        return false;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PendingCommandManager getPendingCommandManager() {
        return pendingCommandManager;
    }

    /**
     * Handle player join event - execute pending commands for the player
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        
        if (pendingCommandManager.hasPendingCommands(playerName)) {
            getLogger().info("Player " + playerName + " joined, executing pending commands");
            
            // Execute pending commands asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                List<List<String>> pendingCommands = pendingCommandManager.getAndRemovePendingCommands(playerName);
                
                if (pendingCommands != null) {
                    for (List<String> commands : pendingCommands) {
                        executeCommands(commands);
                    }
                }
            });
        }
    }

    /**
     * Execute a list of commands synchronously on the main server thread
     */
    private void executeCommands(List<String> commands) {
        Bukkit.getScheduler().runTask(this, () -> {
            for (String command : commands) {
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                if (success) {
                    getLogger().info("Executed pending command: " + command);
                } else {
                    getLogger().warning("Failed to execute pending command: " + command);
                }
            }
        });
    }
}
