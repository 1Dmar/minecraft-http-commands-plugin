package me.lenaic.httpcommands;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Helper class for PlaceholderAPI integration
 */
public class PlaceholderHook {

    private static boolean isEnabled = false;

    static {
        isEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Set a placeholder for a player
     * @param player The player to set the placeholder for
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String setPlaceholders(OfflinePlayer player, String text) {
        if (!isEnabled || text == null) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static boolean isEnabled() {
        return isEnabled;
    }
}
