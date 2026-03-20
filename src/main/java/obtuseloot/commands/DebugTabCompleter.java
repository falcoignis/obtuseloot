package obtuseloot.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebugTabCompleter {
    private static final List<String> TOP = List.of("inspect", "rep", "evolve", "drift", "awaken", "converge", "lore", "reset", "save", "reload", "help", "instability", "archetype", "path", "simulate", "seed", "ability", "memory", "persistence", "ecosystem", "lineage", "genome", "projection", "subscriptions", "artifact");
    private static final List<String> REP_ACTIONS = List.of("set", "add", "reset");
    private static final List<String> STATS = List.of("precision", "brutality", "survival", "mobility", "chaos", "consistency", "kills", "bossKills", "recentKillChain", "survivalStreak");
    private static final List<String> ARCHETYPES = List.of("unformed", "vanguard", "deadeye", "ravager", "strider", "harbinger", "warden", "paragon");
    private static final List<String> SIMULATE = List.of("hit", "move", "lowhp", "kill", "multikill", "bosses", "chaos", "cycle", "resetcontext", "help", "path");
    private static final List<String> SIM_PATHS = List.of("precision", "brutality", "mobility", "survival", "chaos", "boss", "hybrid", "awaken", "drift");
    private static final List<String> SEED_ACTIONS = List.of("show", "reroll", "set", "export", "import");
    private static final List<String> ABILITY_ACTIONS = List.of("show", "refresh", "explain", "tree");
    private static final List<String> PERSISTENCE_ACTIONS = List.of("backend", "test", "migrate");
    private static final List<String> ECOSYSTEM_ACTIONS = List.of("bias", "balance");
    private static final List<String> GENOME_ACTIONS = List.of("interactions");
    private static final List<String> PROJECTION_ACTIONS = List.of("cache", "stats");
    private static final List<String> SUBSCRIPTION_ACTIONS = List.of("stats");
    private static final List<String> ARTIFACT_ACTIONS = List.of("storage", "resolve", "cache", "stats");

    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(TOP, args[1]);
        }
        if (args.length == 3 && "rep".equalsIgnoreCase(args[1])) {
            return filter(REP_ACTIONS, args[2]);
        }
        if (args.length == 4 && "rep".equalsIgnoreCase(args[1])
                && ("set".equalsIgnoreCase(args[2]) || "add".equalsIgnoreCase(args[2]))) {
            return filter(STATS, args[3]);
        }
        if (args.length == 3 && "instability".equalsIgnoreCase(args[1])) {
            return filter(List.of("clear"), args[2]);
        }
        if (args.length == 3 && "archetype".equalsIgnoreCase(args[1])) {
            return filter(List.of("set"), args[2]);
        }
        if (args.length == 4 && "archetype".equalsIgnoreCase(args[1]) && "set".equalsIgnoreCase(args[2])) {
            return filter(ARCHETYPES, args[3]);
        }
        if (args.length == 3 && "path".equalsIgnoreCase(args[1])) {
            return filter(List.of("set"), args[2]);
        }

        if (args.length == 3 && "simulate".equalsIgnoreCase(args[1])) {
            return filter(SIMULATE, args[2]);
        }
        if (args.length == 4 && "simulate".equalsIgnoreCase(args[1]) && "path".equalsIgnoreCase(args[2])) {
            return filter(SIM_PATHS, args[3]);
        }
        if (args.length == 3 && "seed".equalsIgnoreCase(args[1])) {
            return filter(SEED_ACTIONS, args[2]);
        }
        if (args.length == 3 && "ability".equalsIgnoreCase(args[1])) {
            return filter(ABILITY_ACTIONS, args[2]);
        }
        if (args.length == 3 && "ecosystem".equalsIgnoreCase(args[1])) {
            return filter(ECOSYSTEM_ACTIONS, args[2]);
        }
        if (args.length == 3 && "persistence".equalsIgnoreCase(args[1])) {
            return filter(PERSISTENCE_ACTIONS, args[2]);
        }

        if (args.length == 3 && "genome".equalsIgnoreCase(args[1])) {
            return filter(GENOME_ACTIONS, args[2]);
        }
        if (args.length == 3 && "projection".equalsIgnoreCase(args[1])) {
            return filter(PROJECTION_ACTIONS, args[2]);
        }
        if (args.length == 3 && "subscriptions".equalsIgnoreCase(args[1])) {
            return filter(SUBSCRIPTION_ACTIONS, args[2]);
        }
        if (args.length == 3 && "artifact".equalsIgnoreCase(args[1])) {
            return filter(ARTIFACT_ACTIONS, args[2]);
        }

        if (args.length == 4 && "persistence".equalsIgnoreCase(args[1]) && "migrate".equalsIgnoreCase(args[2])) {
            return filter(List.of("yaml-to-sqlite", "yaml-to-mysql"), args[3]);
        }

        if (expectsPlayer(args)) {
            return filter(onlinePlayers(), args[args.length - 1]);
        }

        return List.of();
    }

    private boolean expectsPlayer(String[] args) {
        if (args.length < 3) {
            return false;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        boolean base = (args.length == 3 && List.of("inspect", "evolve", "drift", "awaken", "converge", "lore", "reset", "save").contains(sub))
                || (args.length == 4 && "rep".equals(sub) && "reset".equalsIgnoreCase(args[2]))
                || (args.length == 5 && "rep".equals(sub) && ("set".equalsIgnoreCase(args[2]) || "add".equalsIgnoreCase(args[2])))
                || (args.length == 4 && "instability".equals(sub) && "clear".equalsIgnoreCase(args[2]))
                || (args.length == 5 && "archetype".equals(sub) && "set".equalsIgnoreCase(args[2]))
                || (args.length == 5 && "path".equals(sub) && "set".equalsIgnoreCase(args[2]))
                || (args.length == 4 && "ability".equals(sub) && List.of("show", "refresh", "explain", "tree").contains(args[2].toLowerCase(Locale.ROOT)))
                || (args.length == 3 && "memory".equals(sub))
                || (args.length == 3 && "lineage".equals(sub))
                || (args.length == 3 && "subscriptions".equals(sub))
                || (args.length == 4 && "artifact".equals(sub) && List.of("storage", "resolve", "cache", "stats").contains(args[2].toLowerCase(Locale.ROOT)));

        boolean seed = "seed".equals(sub)
                && ((args.length == 4 && List.of("show", "reroll", "export").contains(args[2].toLowerCase(Locale.ROOT)))
                || (args.length == 5 && List.of("set", "import").contains(args[2].toLowerCase(Locale.ROOT))));

        boolean simulate = "simulate".equals(sub)
                && ((args.length == 4 && List.of("lowhp", "kill", "chaos", "cycle", "resetcontext").contains(args[2].toLowerCase(Locale.ROOT)))
                || (args.length == 5 && List.of("hit", "move", "multikill", "bosses", "path").contains(args[2].toLowerCase(Locale.ROOT))));

        return base || seed || simulate;
    }

    private List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filter(List<String> values, String prefix) {
        if (prefix == null || prefix.isBlank()) return values;
        String lowered = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                out.add(value);
            }
        }
        return out;
    }
}
