package obtuseloot.commands;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.debug.ArtifactDebugger;
import obtuseloot.names.NamePoolManager;
import obtuseloot.reputation.ReputationManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime command handling for both player and console senders.
 */
public final class ObtuseLootCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION_HELP = "obtuseloot.help";
    private static final String PERMISSION_INFO = "obtuseloot.info";
    private static final String PERMISSION_INSPECT = "obtuseloot.inspect";
    private static final String PERMISSION_ADMIN = "obtuseloot.admin";
    private static final String PERMISSION_EDIT = "obtuseloot.edit";

    private final ObtuseLoot plugin;

    public ObtuseLootCommand(ObtuseLoot plugin) {
        this.plugin = plugin;
    }

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
            sender.sendMessage("§7/" + label + " refresh [player] §8- §fRegenerate a player's artifact profile §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " reset [player] §8- §fClear a player's tracked artifact and reputation state §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " reload §8- §fReload config-driven runtime settings and name pools §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " addname <pool> <value> §8- §fAdd a name entry to a pool (prefixes/suffixes/generic) §8["
                    + PERMISSION_EDIT + "]");
            sender.sendMessage("§7/" + label + " removename <pool> <value> §8- §fRemove a name entry from a pool §8["
                    + PERMISSION_EDIT + "]");
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

        if ("refresh".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_ADMIN)) {
                return true;
            }

            Player target = resolveTarget(sender, args, label, "refresh");
            if (target == null) {
                return true;
            }

            ArtifactManager.remove(target.getUniqueId());
            Artifact refreshed = ArtifactManager.getOrCreate(target.getUniqueId());

            sender.sendMessage("§aRefreshed artifact profile for §f" + target.getName() + "§a: §d" + refreshed.getName());
            if (!sender.equals(target)) {
                target.sendMessage("§dYour ObtuseLoot artifact profile was refreshed by an administrator.");
            }
            return true;
        }

        if ("reset".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_ADMIN)) {
                return true;
            }

            Player target = resolveTarget(sender, args, label, "reset");
            if (target == null) {
                return true;
            }

            ArtifactManager.remove(target.getUniqueId());
            ReputationManager.remove(target.getUniqueId());
            sender.sendMessage("§aCleared tracked ObtuseLoot state for §f" + target.getName() + "§a.");
            if (!sender.equals(target)) {
                target.sendMessage("§dYour ObtuseLoot progression state was reset by an administrator.");
            }
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_ADMIN)) {
                return true;
            }

            plugin.reloadConfig();
            RuntimeSettings.load(plugin.getConfig());
            NamePoolManager.initialize(plugin);
            sender.sendMessage("§aObtuseLoot config/runtime snapshots reloaded.");
            return true;
        }

        if ("addname".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /" + label + " addname <prefixes|suffixes|generic> <value>");
                return true;
            }

            return handleNameEdit(sender, true, args[1], joinFrom(args, 2));
        }

        if ("removename".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /" + label + " removename <prefixes|suffixes|generic> <value>");
                return true;
            }

            return handleNameEdit(sender, false, args[1], joinFrom(args, 2));
        }

        sender.sendMessage("§cUnknown subcommand. Try /" + label + " help");
        return true;
    }

    private boolean handleNameEdit(CommandSender sender, boolean add, String pool, String value) {
        String normalizedPool = NamePoolManager.normalizePool(pool);
        if (normalizedPool == null) {
            sender.sendMessage("§cUnknown pool '" + pool + "'. Valid pools: prefixes, suffixes, generic.");
            return true;
        }

        if (!hasEditPermission(sender, normalizedPool)) {
            return true;
        }

        try {
            if (add) {
                if (!NamePoolManager.addName(normalizedPool, value)) {
                    sender.sendMessage("§eNo change made. Entry may already exist or input was invalid.");
                    return true;
                }
                sender.sendMessage("§aAdded '§f" + value.trim() + "§a' to §f" + normalizedPool + "§a.");
            } else {
                if (!NamePoolManager.removeName(normalizedPool, value)) {
                    sender.sendMessage("§eNo change made. Entry may not exist, input was invalid, or removal would empty the pool.");
                    return true;
                }
                sender.sendMessage("§aRemoved '§f" + value.trim() + "§a' from §f" + normalizedPool + "§a.");
            }
        } catch (IOException exception) {
            sender.sendMessage("§cFailed to persist name pool update: " + exception.getMessage());
        }

        return true;
    }

    private boolean hasEditPermission(CommandSender sender, String pool) {
        String scopedPermission = PERMISSION_EDIT + "." + pool;
        if (sender.hasPermission(PERMISSION_EDIT) || sender.hasPermission(scopedPermission)) {
            return true;
        }

        sender.sendMessage("§cYou do not have permission: " + scopedPermission);
        return false;
    }

    private String joinFrom(String[] values, int start) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < values.length; index++) {
            if (index > start) {
                builder.append(' ');
            }
            builder.append(values[index]);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>();
            addIfPermitted(sender, candidates, "help", PERMISSION_HELP);
            addIfPermitted(sender, candidates, "info", PERMISSION_INFO);
            addIfPermitted(sender, candidates, "inspect", PERMISSION_INSPECT);
            addIfPermitted(sender, candidates, "refresh", PERMISSION_ADMIN);
            addIfPermitted(sender, candidates, "reset", PERMISSION_ADMIN);
            addIfPermitted(sender, candidates, "reload", PERMISSION_ADMIN);
            if (hasAnyEditPermission(sender)) {
                candidates.add("addname");
                candidates.add("removename");
            }
            return filterByPrefix(candidates, args[0]);
        }

        if (args.length == 2 && isPlayerTargetCommand(args[0])) {
            if ((("inspect".equalsIgnoreCase(args[0]) && !sender.hasPermission(PERMISSION_INSPECT))
                    || (("refresh".equalsIgnoreCase(args[0]) || "reset".equalsIgnoreCase(args[0]))
                    && !sender.hasPermission(PERMISSION_ADMIN)))) {
                return List.of();
            }

            List<String> names = new ArrayList<>();
            for (Player player : sender.getServer().getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filterByPrefix(names, args[1]);
        }

        if (args.length == 2 && ("addname".equalsIgnoreCase(args[0]) || "removename".equalsIgnoreCase(args[0]))) {
            List<String> editablePools = new ArrayList<>();
            for (String pool : NamePoolManager.allPoolNames()) {
                if (sender.hasPermission(PERMISSION_EDIT + "." + pool) || sender.hasPermission(PERMISSION_EDIT)) {
                    editablePools.add(pool);
                }
            }
            return filterByPrefix(editablePools, args[1]);
        }

        if (args.length >= 3 && "removename".equalsIgnoreCase(args[0])) {
            String pool = NamePoolManager.normalizePool(args[1]);
            if (pool == null || !hasAnyEditPermission(sender)
                    || (!sender.hasPermission(PERMISSION_EDIT) && !sender.hasPermission(PERMISSION_EDIT + "." + pool))) {
                return List.of();
            }

            return filterByPrefix(NamePoolManager.getPool(pool), args[args.length - 1]);
        }

        return List.of();
    }

    private boolean hasAnyEditPermission(CommandSender sender) {
        if (sender.hasPermission(PERMISSION_EDIT)) {
            return true;
        }

        for (String pool : NamePoolManager.allPoolNames()) {
            if (sender.hasPermission(PERMISSION_EDIT + "." + pool)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlayerTargetCommand(String subcommand) {
        return "inspect".equalsIgnoreCase(subcommand)
                || "refresh".equalsIgnoreCase(subcommand)
                || "reset".equalsIgnoreCase(subcommand);
    }

    private Player resolveTarget(CommandSender sender, String[] args, String label, String subcommand) {
        if (args.length >= 2) {
            Player target = sender.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return null;
            }
            return target;
        }

        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage("§cConsole must provide a player: /" + label + " " + subcommand + " <player>");
        return null;
    }

    private void addIfPermitted(CommandSender sender, List<String> candidates, String value, String permission) {
        if (sender.hasPermission(permission)) {
            candidates.add(value);
        }
    }

    private List<String> filterByPrefix(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return candidates;
        }

        String lowered = prefix.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(lowered)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou do not have permission: " + permission);
            return false;
        }

        return true;
    }
}
