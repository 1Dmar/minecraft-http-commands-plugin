package me.lenaic.httpcommands;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Manages plugin configuration for port and bearer token
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    private int port;
    private String bearerToken;
    private boolean requireHttps;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        // Ensure plugin data folder exists
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Create config file if it doesn't exist
        if (configFile == null) {
            configFile = new File(dataFolder, "config.yml");
        }

        if (!configFile.exists()) {
            // Copy default config from resources
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create config file", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load values
        port = config.getInt("port", 8080);
        bearerToken = config.getString("bearer-token", "change-me");
        requireHttps = config.getBoolean("require-https", true);

        plugin.getLogger().info("Configuration loaded - Port: " + port + ", Require HTTPS: " + requireHttps);
    }

    /**
     * Reload configuration from file
     */
    public void reloadConfig() {
        loadConfig();
    }

    public int getPort() {
        return port;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
