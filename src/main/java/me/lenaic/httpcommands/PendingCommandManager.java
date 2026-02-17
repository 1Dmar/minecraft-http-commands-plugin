package me.lenaic.httpcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages pending commands that wait for player to join
 * Persists pending commands to a JSON file for server restart survival
 */
public class PendingCommandManager {

    private final JavaPlugin plugin;
    private final Gson gson;
    private final File pendingFile;
    
    // Map of player name -> list of pending commands
    private Map<String, List<PendingCommand>> pendingCommands;

    public PendingCommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pendingFile = new File(plugin.getDataFolder(), "pending_commands.json");
        this.pendingCommands = new HashMap<>();
        
        loadPendingCommands();
    }

    /**
     * Load pending commands from file
     */
    @SuppressWarnings("unchecked")
    private void loadPendingCommands() {
        if (!pendingFile.exists()) {
            pendingCommands = new HashMap<>();
            return;
        }

        try {
            String content = new String(Files.readAllBytes(pendingFile.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                pendingCommands = new HashMap<>();
                return;
            }
            
            Type mapType = new TypeToken<Map<String, List<PendingCommand>>>(){}.getType();
            Map<String, List<PendingCommand>> loaded = gson.fromJson(content, mapType);
            
            if (loaded != null) {
                pendingCommands = loaded;
                plugin.getLogger().info("Loaded " + getTotalPendingCount() + " pending commands from file");
            } else {
                pendingCommands = new HashMap<>();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load pending commands file", e);
            pendingCommands = new HashMap<>();
        }
    }

    /**
     * Save pending commands to file
     */
    private void savePendingCommands() {
        try {
            // Ensure directory exists
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String json = gson.toJson(pendingCommands);
            
            try (FileWriter writer = new FileWriter(pendingFile, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pending commands", e);
        }
    }

    /**
     * Add a pending command for a player
     */
    public void addPendingCommand(String playerName, List<String> commands) {
        String normalizedName = playerName.toLowerCase();
        
        PendingCommand pendingCommand = new PendingCommand(commands, System.currentTimeMillis());
        
        pendingCommands.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(pendingCommand);
        
        savePendingCommands();
        plugin.getLogger().info("Added " + commands.size() + " pending command(s) for player: " + playerName);
    }

    /**
     * Get and remove pending commands for a player
     * Returns null if no pending commands exist for the player
     */
    public List<List<String>> getAndRemovePendingCommands(String playerName) {
        String normalizedName = playerName.toLowerCase();
        
        List<PendingCommand> commands = pendingCommands.remove(normalizedName);
        
        if (commands != null && !commands.isEmpty()) {
            savePendingCommands();
            plugin.getLogger().info("Executing " + commands.size() + " pending command(s) for player: " + playerName);
            
            List<List<String>> result = new ArrayList<>();
            for (PendingCommand cmd : commands) {
                result.add(cmd.getCommands());
            }
            return result;
        }
        
        return null;
    }

    /**
     * Check if a player has pending commands
     */
    public boolean hasPendingCommands(String playerName) {
        String normalizedName = playerName.toLowerCase();
        List<PendingCommand> commands = pendingCommands.get(normalizedName);
        return commands != null && !commands.isEmpty();
    }

    /**
     * Get total count of pending commands across all players
     */
    public int getTotalPendingCount() {
        int count = 0;
        for (List<PendingCommand> commands : pendingCommands.values()) {
            count += commands.size();
        }
        return count;
    }

    /**
     * Clear all pending commands (for cleanup)
     */
    public void clearAll() {
        pendingCommands.clear();
        savePendingCommands();
    }

    /**
     * Inner class representing a pending command with timestamp
     */
    public static class PendingCommand {
        private List<String> commands;
        private long timestamp;

        public PendingCommand(List<String> commands, long timestamp) {
            this.commands = commands;
            this.timestamp = timestamp;
        }

        public List<String> getCommands() {
            return commands;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
