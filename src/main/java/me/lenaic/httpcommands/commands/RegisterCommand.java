package me.lenaic.httpcommands.commands;

import me.lenaic.httpcommands.HttpCommandsPlugin;
import me.lenaic.httpcommands.endpoints.ValidateRegistrationEndpoint;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Command handler for /register confirm|deny <registrationId>
 * This command is triggered when a player clicks on the Confirm/Deny buttons in chat
 */
public class RegisterCommand implements CommandExecutor {

    private final ValidateRegistrationEndpoint endpoint;

    public RegisterCommand(HttpCommandsPlugin plugin, ValidateRegistrationEndpoint endpoint) {
        this.plugin = plugin;
        this.endpoint = endpoint;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /register <confirm|deny> <registrationId>");
            return true;
        }

        String action = args[0].toLowerCase();
        String registrationIdStr = args[1];

        UUID registrationId;
        try {
            registrationId = UUID.fromString(registrationIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid registration ID.");
            return true;
        }

        if (action.equals("confirm")) {
            player.sendMessage(net.kyori.adventure.text.Component.text("⏳ Processing your response... Please wait.", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            endpoint.sendResponseToWebsite(player, registrationId, true);
        } else if (action.equals("deny")) {
            player.sendMessage(net.kyori.adventure.text.Component.text("⏳ Processing your response... Please wait.", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            endpoint.sendResponseToWebsite(player, registrationId, false);
        } else {
            player.sendMessage("§cUsage: /register <confirm|deny> <registrationId>");
        }

        return true;
    }
}
