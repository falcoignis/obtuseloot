package obtuseloot.commands;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.fusion.FusionEngine;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DebugCommand {
    private static final String PERMISSION_DEBUG = "obtuseloot.debug";
    private static final List<String> REP_STATS = List.of(
            "precision", "brutality", "survival", "mobility", "chaos", "consistency",
            "kills", "bosskills", "recentkillchain", "survivalstreak"
    );
    private static final List<String> ARCHETYPES = List.of(
            "unformed", "vanguard", "deadeye", "ravager", "strider", "harbinger", "warden", "paragon"
    );

    private final ObtuseLoot plugin;
    private final FusionEngine fusionEngine = new FusionEngine();

    public DebugCommand(ObtuseLoot plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasDebugPermission(sender)) {
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "inspect" -> inspect(sender, label, args);
            case "rep" -> rep(sender, label, args);
            case "evolve" -> evolve(sender, label, args);
            case "drift" -> drift(sender, label, args);
            case "awaken" -> awaken(sender, label, args);
            case "fuse" -> fuse(sender, label, args);
            case "lore" -> lore(sender, label, args);
            case "reset" -> reset(sender, label, args);
            case "save" -> save(sender, label, args);
            case "reload" -> reload(sender);
            case "instability" -> instability(sender, label, args);
            case "archetype" -> archetype(sender, label, args);
            case "path" -> path(sender, label, args);
            default -> {
                sender.sendMessage("§cUnknown debug subcommand. Try /" + label + " debug help");
                yield true;
            }
        };
    }

    private boolean inspect(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "inspect");
        if (target == null) {
            return true;
        }
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());

        sender.sendMessage("§d=== Obtuse Debug Inspect: " + target.getName() + " ===");
        sender.sendMessage("§7name=§f" + artifact.getGeneratedName() + " §7archetype=§f" + artifact.getArchetypePath()
                + " §7evolution=§f" + artifact.getEvolutionPath());
        sender.sendMessage("§7awakening=§f" + artifact.getAwakeningPath() + " §7fusion=§f" + artifact.getFusionPath());
        sender.sendMessage("§7driftLevel=§f" + artifact.getDriftLevel() + " §7totalDrifts=§f" + artifact.getTotalDrifts()
                + " §7driftAlignment=§f" + artifact.getDriftAlignment());
        sender.sendMessage("§7lineage=§f" + artifact.getLatentLineage() + " §7instability=§f" + artifact.getCurrentInstabilityState());
        sender.sendMessage("§7seed affinities: §f" + formatStatMap(artifact, "seed"));
        sender.sendMessage("§7drift bias: §f" + artifact.getDriftBiasAdjustments());
        sender.sendMessage("§7awakening bias: §f" + artifact.getAwakeningBiasAdjustments());
        sender.sendMessage("§7awakening multipliers: §f" + artifact.getAwakeningGainMultipliers());
        sender.sendMessage("§7awakening traits: §f" + artifact.getAwakeningTraits());
        sender.sendMessage("§7rep: P=" + rep.getPrecision() + " B=" + rep.getBrutality() + " S=" + rep.getSurvival()
                + " M=" + rep.getMobility() + " X=" + rep.getChaos() + " C=" + rep.getConsistency()
                + " K=" + rep.getKills() + " BK=" + rep.getBossKills()
                + " chain=" + rep.getRecentKillChain() + " streak=" + rep.getSurvivalStreak());
        sender.sendMessage("§7Recent lore history: §f" + tail(artifact.getLoreHistory(), 5));
        sender.sendMessage("§7Recent drift history: §f" + tail(artifact.getDriftHistory(), 5));
        return true;
    }

    private boolean rep(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + label + " debug rep <set|add|reset> ...");
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if ("reset".equals(mode)) {
            Player target = resolveTarget(sender, label, args, 2, "rep reset");
            if (target == null) return true;
            plugin.getReputationManager().reset(target.getUniqueId());
            refreshAndSave(target);
            sender.sendMessage("§aReset reputation for " + target.getName() + ".");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /" + label + " debug rep <set|add> <stat> <value> [player]");
            return true;
        }
        String stat = normalizeStat(args[2]);
        if (!REP_STATS.contains(stat)) {
            sender.sendMessage("§cInvalid stat. Allowed: " + REP_STATS);
            return true;
        }
        Integer value = parseInt(sender, args[3]);
        if (value == null) return true;
        Player target = resolveTarget(sender, label, args, 4, "rep " + mode + " " + args[2] + " " + args[3]);
        if (target == null) return true;

        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        int oldValue = getRepValue(rep, stat);
        if ("set".equals(mode)) {
            setRepValue(rep, stat, value);
        } else if ("add".equals(mode)) {
            setRepValue(rep, stat, Math.max(0, oldValue + value));
        } else {
            sender.sendMessage("§cUnknown mode " + mode + ". Use set/add/reset.");
            return true;
        }

        refreshAndSave(target);
        sender.sendMessage("§a" + mode + " " + target.getName() + " " + stat + ": " + oldValue + " -> " + getRepValue(rep, stat));
        return true;
    }

    private boolean evolve(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "evolve");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        String oldA = artifact.getArchetypePath();
        String oldE = artifact.getEvolutionPath();
        plugin.getEvolutionEngine().evaluate(target, artifact, rep);
        refreshAndSave(target);
        sender.sendMessage("§aEvolve " + target.getName() + ": archetype " + oldA + " -> " + artifact.getArchetypePath()
                + ", evolution " + oldE + " -> " + artifact.getEvolutionPath());
        return true;
    }

    private boolean drift(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "drift");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        var mutation = plugin.getDriftEngine().forceDrift(target, artifact, rep);
        plugin.getEvolutionEngine().evaluate(target, artifact, rep);
        refreshAndSave(target);
        sender.sendMessage("§aForced drift for " + target.getName() + ": " + mutation.profileId());
        return true;
    }

    private boolean awaken(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "awaken");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        String old = artifact.getAwakeningPath();
        boolean changed = plugin.getAwakeningEngine().forceAwakening(target, artifact, rep);
        refreshAndSave(target);
        if (!changed) {
            sender.sendMessage("§eAwakening already active: " + old + ".");
        } else {
            sender.sendMessage("§aAwakening applied for " + target.getName() + ": " + old + " -> " + artifact.getAwakeningPath());
        }
        return true;
    }

    private boolean fuse(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "fuse");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        String old = artifact.getFusionPath();
        boolean changed = fusionEngine.evaluate(target, artifact, rep);
        refreshAndSave(target);
        if (!changed) {
            sender.sendMessage("§eNo valid fusion recipe available for " + target.getName() + ".");
        } else {
            sender.sendMessage("§aFusion changed for " + target.getName() + ": " + old + " -> " + artifact.getFusionPath());
        }
        return true;
    }

    private boolean lore(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "lore");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        sender.sendMessage("§dActionBar: §f" + plugin.getLoreEngine().buildActionBarSummary(artifact, rep));
        for (String line : plugin.getLoreEngine().buildLoreLines(artifact, rep)) {
            sender.sendMessage("§7- §f" + line);
        }
        return true;
    }

    private boolean reset(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "reset");
        if (target == null) return true;
        plugin.getArtifactManager().recreate(target.getUniqueId());
        plugin.getReputationManager().reset(target.getUniqueId());
        plugin.getCombatContextManager().remove(target.getUniqueId());
        refreshAndSave(target);
        sender.sendMessage("§aReset artifact and reputation for " + target.getName() + ".");
        return true;
    }

    private boolean save(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "save");
        if (target == null) return true;
        plugin.getArtifactManager().save(target.getUniqueId());
        plugin.getReputationManager().save(target.getUniqueId());
        sender.sendMessage("§aSaved artifact and reputation for " + target.getName() + ".");
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadConfig();
        obtuseloot.config.RuntimeSettings.load(plugin.getConfig());
        obtuseloot.names.NamePoolManager.initialize(plugin);
        sender.sendMessage("§aObtuseLoot config reloaded.");
        return true;
    }

    private boolean instability(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || !"clear".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cUsage: /" + label + " debug instability clear [player]");
            return true;
        }
        Player target = resolveTarget(sender, label, args, 2, "instability clear");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        artifact.clearInstability();
        artifact.addLoreHistory("Instability cleared by debug command.");
        refreshAndSave(target);
        sender.sendMessage("§aCleared instability for " + target.getName() + ".");
        return true;
    }

    private boolean archetype(CommandSender sender, String label, String[] args) {
        if (args.length < 3 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cUsage: /" + label + " debug archetype set <archetype> [player]");
            return true;
        }
        String value = args[2].toLowerCase(Locale.ROOT);
        if (!ARCHETYPES.contains(value)) {
            sender.sendMessage("§cInvalid archetype. Allowed: " + ARCHETYPES);
            return true;
        }
        Player target = resolveTarget(sender, label, args, 3, "archetype set " + value);
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        String old = artifact.getArchetypePath();
        artifact.setArchetypePath(value);
        artifact.addLoreHistory("Archetype set by debug: " + old + " -> " + value);
        refreshAndSave(target);
        sender.sendMessage("§aSet " + target.getName() + " archetype: " + old + " -> " + value);
        return true;
    }

    private boolean path(CommandSender sender, String label, String[] args) {
        if (args.length < 3 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cUsage: /" + label + " debug path set <evolutionPath> [player]");
            return true;
        }
        String value = args[2];
        Player target = resolveTarget(sender, label, args, 3, "path set " + value);
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        String old = artifact.getEvolutionPath();
        artifact.setEvolutionPath(value);
        artifact.addLoreHistory("Evolution path set by debug: " + old + " -> " + value);
        refreshAndSave(target);
        sender.sendMessage("§aSet " + target.getName() + " evolution path: " + old + " -> " + value);
        return true;
    }

    private boolean hasDebugPermission(CommandSender sender) {
        if (sender instanceof Player player && !player.isOp() && !sender.hasPermission(PERMISSION_DEBUG)) {
            sender.sendMessage("§cYou do not have permission: " + PERMISSION_DEBUG);
            return false;
        }
        return true;
    }

    private void refreshAndSave(Player target) {
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        plugin.getLoreEngine().refreshLore(target, artifact, rep);
        plugin.getArtifactManager().save(target.getUniqueId());
        plugin.getReputationManager().save(target.getUniqueId());
    }

    private Player resolveTarget(CommandSender sender, String label, String[] args, int optionalPlayerIndex, String usageTail) {
        if (args.length > optionalPlayerIndex) {
            Player target = Bukkit.getPlayerExact(args[optionalPlayerIndex]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[optionalPlayerIndex]);
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage("§cConsole must provide a player: /" + label + " debug " + usageTail + " <player>");
        return null;
    }

    private Integer parseInt(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid integer: " + value);
            return null;
        }
    }

    private String normalizeStat(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "");
    }

    private int getRepValue(ArtifactReputation rep, String stat) {
        return switch (stat) {
            case "precision" -> rep.getPrecision();
            case "brutality" -> rep.getBrutality();
            case "survival" -> rep.getSurvival();
            case "mobility" -> rep.getMobility();
            case "chaos" -> rep.getChaos();
            case "consistency" -> rep.getConsistency();
            case "kills" -> rep.getKills();
            case "bosskills" -> rep.getBossKills();
            case "recentkillchain" -> rep.getRecentKillChain();
            case "survivalstreak" -> rep.getSurvivalStreak();
            default -> 0;
        };
    }

    private void setRepValue(ArtifactReputation rep, String stat, int value) {
        int safe = Math.max(0, value);
        switch (stat) {
            case "precision" -> rep.setPrecision(safe);
            case "brutality" -> rep.setBrutality(safe);
            case "survival" -> rep.setSurvival(safe);
            case "mobility" -> rep.setMobility(safe);
            case "chaos" -> rep.setChaos(safe);
            case "consistency" -> rep.setConsistency(safe);
            case "kills" -> rep.setKills(safe);
            case "bosskills" -> rep.setBossKills(safe);
            case "recentkillchain" -> rep.setRecentKillChain(safe);
            case "survivalstreak" -> rep.setSurvivalStreak(safe);
            default -> { }
        }
        rep.applySoftFloor();
    }

    private String tail(List<String> lines, int count) {
        if (lines.isEmpty()) {
            return "[]";
        }
        int from = Math.max(0, lines.size() - count);
        return lines.subList(from, lines.size()).toString();
    }

    private String formatStatMap(Artifact artifact, String ignored) {
        return "{precision=" + artifact.getSeedPrecisionAffinity()
                + ", brutality=" + artifact.getSeedBrutalityAffinity()
                + ", survival=" + artifact.getSeedSurvivalAffinity()
                + ", mobility=" + artifact.getSeedMobilityAffinity()
                + ", chaos=" + artifact.getSeedChaosAffinity()
                + ", consistency=" + artifact.getSeedConsistencyAffinity()
                + '}';
    }

    private void sendHelp(CommandSender sender, String label) {
        List<String> lines = Arrays.asList(
                "/" + label + " debug inspect [player]",
                "/" + label + " debug rep set <stat> <value> [player]",
                "/" + label + " debug rep add <stat> <value> [player]",
                "/" + label + " debug rep reset [player]",
                "/" + label + " debug evolve [player]",
                "/" + label + " debug drift [player]",
                "/" + label + " debug awaken [player]",
                "/" + label + " debug fuse [player]",
                "/" + label + " debug lore [player]",
                "/" + label + " debug reset [player]",
                "/" + label + " debug save [player]",
                "/" + label + " debug reload",
                "/" + label + " debug instability clear [player]",
                "/" + label + " debug archetype set <archetype> [player]",
                "/" + label + " debug path set <evolutionPath> [player]"
        );
        sender.sendMessage("§dObtuseLoot Debug Commands:");
        for (String line : lines) {
            sender.sendMessage("§7- §f" + line);
        }
    }
}
