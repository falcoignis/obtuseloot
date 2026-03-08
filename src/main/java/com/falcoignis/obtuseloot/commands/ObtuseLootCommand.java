package com.falcoignis.obtuseloot.commands;

import com.falcoignis.obtuseloot.debug.ArtifactDebugger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Minimal command wiring to ensure plugin.yml command metadata is backed by runtime behavior.
 */
public final class ObtuseLootCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§dObtuseLoot commands: /" + label + " info | /" + label + " inspect [player]");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§dObtuseLoot is active. Progression hooks are wired for combat/kill events.");
            return true;
        }

        if ("inspect".equalsIgnoreCase(args[0])) {
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
}
