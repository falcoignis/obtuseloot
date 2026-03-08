package com.obtuseloot.commands;

import com.obtuseloot.debug.ArtifactDebugger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Minimal command wiring to ensure plugin.yml command metadata is backed by runtime behavior.
 */
public final class ObtuseLootCommand implements CommandExecutor {
    private static final String PERMISSION_HELP = "obtuseloot.help";
    private static final String PERMISSION_INFO = "obtuseloot.info";
    private static final String PERMISSION_INSPECT = "obtuseloot.inspect";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_HELP)) {
                return true;
            }

            sender.sendMessage("§dObtuseLoot command reference:");
            sender.sendMessage("§7/" + label + " help §8- §fShow this command list §8[" + PERMISSION_HELP + "]");
            sender.sendMessage("§7/" + label + " info §8- §fShow plugin runtime status §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " inspect [player] §8- §fInspect tracked artifact state for a player §8["
                    + PERMISSION_INSPECT + "]");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_INFO)) {
                return true;
            }

            sender.sendMessage("§dObtuseLoot is active. Progression hooks are wired for combat/kill events.");
            return true;
        }

        if ("inspect".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_INSPECT)) {
                return true;
            }

            Player target;
            if (args.length >= 2) {
                target = sender.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                sender.sendMessage("§cConsole must provide a player: /" + label + " inspect <player>");
                return true;
            }

            sender.sendMessage("§7" + ArtifactDebugger.describe(target.getUniqueId()));
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Try /" + label + " help");
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou do not have permission: " + permission);
            return false;
        }

        return true;
    }
}
