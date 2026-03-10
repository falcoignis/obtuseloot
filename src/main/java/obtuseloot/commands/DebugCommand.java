package obtuseloot.commands;

import obtuseloot.ObtuseLoot;
import obtuseloot.analytics.InteractionHeatmapExporter;
import obtuseloot.analytics.TraitInteractionAnalyzer;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.abilities.ArtifactTriggerBinding;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.abilities.PlayerArtifactTriggerMap;
import obtuseloot.abilities.ArtifactEvolutionStage;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.artifacts.Artifact;
import obtuseloot.combat.CombatContext;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.fusion.FusionEngine;
import obtuseloot.obtuseengine.ArtifactProcessor;
import obtuseloot.persistence.MySqlPersistenceProvider;
import obtuseloot.persistence.PersistenceMigrator;
import obtuseloot.persistence.SqlitePersistenceProvider;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DebugCommand {
    private static final String PERMISSION_DEBUG = "obtuseloot.debug";
    private static final List<String> REP_STATS = List.of(
            "precision", "brutality", "survival", "mobility", "chaos", "consistency",
            "kills", "bosskills", "recentkillchain", "survivalstreak"
    );
    private static final List<String> ARCHETYPES = List.of(
            "unformed", "vanguard", "deadeye", "ravager", "strider", "harbinger", "warden", "paragon"
    );
    private static final List<String> SIM_PATHS = List.of(
            "precision", "brutality", "mobility", "survival", "chaos", "boss", "hybrid", "awaken", "drift"
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
            case "simulate" -> simulate(sender, label, args);
            case "seed" -> seed(sender, label, args);
            case "ability" -> ability(sender, label, args);
            case "memory" -> memory(sender, label, args);
            case "persistence" -> persistence(sender, label, args);
            case "ecosystem" -> ecosystem(sender, args);
            case "lineage" -> lineage(sender, label, args);
            case "genome" -> genome(sender, label, args);
            case "projection" -> projection(sender, args);
            case "subscriptions" -> subscriptions(sender, label, args);
            case "artifact" -> artifactStorage(sender, label, args);
            default -> {
                sender.sendMessage("§cUnknown debug subcommand. Try /" + label + " debug help");
                yield true;
            }
        };
    }

    private boolean artifactStorage(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || "storage".equalsIgnoreCase(args[1])) {
            Player target = resolveTarget(sender, label, args, 2, "artifact storage");
            if (target == null) return true;
            Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
            String handDescription = plugin.getArtifactItemStorage().describeItemStorage(target.getInventory().getItemInMainHand());
            sender.sendMessage("§dArtifact storage for " + target.getName());
            sender.sendMessage("§7backend=§f" + plugin.getPersistenceManager().backendName() + " §7storageKey=§f" + artifact.getArtifactStorageKey());
            sender.sendMessage("§7owner=§f" + artifact.getOwnerId() + " §7mainHand=§f" + handDescription);
            return true;
        }
        if ("resolve".equalsIgnoreCase(args[1])) {
            Player target = resolveTarget(sender, label, args, 2, "artifact resolve");
            if (target == null) return true;
            Artifact resolved = plugin.getArtifactItemStorage().resolve(target.getInventory().getItemInMainHand(), target.getUniqueId());
            sender.sendMessage(resolved == null
                    ? "§cCould not resolve artifact from main-hand item."
                    : "§aResolved artifact: §f" + resolved.getGeneratedName() + " §7key=§f" + resolved.getArtifactStorageKey());
            return true;
        }
        sender.sendMessage("§cUsage: /" + label + " debug artifact [storage|resolve] [player]");
        return true;
    }

    private boolean inspect(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "inspect");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());

        sender.sendMessage("§d=== Obtuse Debug Inspect: " + target.getName() + " ===");
        sender.sendMessage("§7name=§f" + artifact.getGeneratedName() + " §7archetype=§f" + artifact.getArchetypePath()
                + " §7evolution=§f" + artifact.getEvolutionPath());
        sender.sendMessage("§7awakening=§f" + artifact.getAwakeningPath() + " §7fusion=§f" + artifact.getFusionPath());
        sender.sendMessage("§7driftLevel=§f" + artifact.getDriftLevel() + " §7totalDrifts=§f" + artifact.getTotalDrifts()
                + " §7driftAlignment=§f" + artifact.getDriftAlignment());
        sender.sendMessage("§7seed=§f" + artifact.getArtifactSeed() + " §7lineage=§f" + artifact.getLatentLineage() + " §7instability=§f" + artifact.getCurrentInstabilityState());
        sender.sendMessage("§7seed affinities: §f" + formatStatMap(artifact));
        sender.sendMessage("§7drift bias: §f" + artifact.getDriftBiasAdjustments());
        sender.sendMessage("§7awakening bias: §f" + artifact.getAwakeningBiasAdjustments());
        sender.sendMessage("§7awakening multipliers: §f" + artifact.getAwakeningGainMultipliers());
        sender.sendMessage("§7awakening traits: §f" + artifact.getAwakeningTraits());
        AbilityProfile abilityProfile = plugin.getItemAbilityManager().profileFor(artifact, rep);
        sender.sendMessage("§7isGeneric=§f" + ArtifactEligibility.isGenericItem(artifact)
                + " §7evolutionEligible=§f" + ArtifactEligibility.isEvolutionEligible(artifact)
                + " §7abilityEligible=§f" + ArtifactEligibility.isAbilityEligible(artifact)
                + " §7memoryEligible=§f" + ArtifactEligibility.isMemoryEligible(artifact));
        sender.sendMessage("§7evolutionStage=§f" + ArtifactEvolutionStage.resolveStage(artifact)
                + " §7abilityProfile=§f" + abilityProfile.profileId());
        sender.sendMessage("§7abilityTriggers=§f" + abilityProfile.abilities().stream().map(a -> a.trigger().name()).toList());
        sender.sendMessage("§7abilityEffects=§f" + abilityProfile.abilities().stream().map(a -> a.name() + ":" + a.effects().stream().map(e -> e.type().name()).toList()).toList());
        sender.sendMessage("§7drift influence=§f" + artifact.getDriftAlignment() + " §7awakening influence=§f" + artifact.getAwakeningPath() + " §7fusion influence=§f" + artifact.getFusionPath());
        sender.sendMessage("§7branchPath=§f" + artifact.getLastAbilityBranchPath() + " §7mutationHistory=§f" + artifact.getLastMutationHistory());
        sender.sendMessage("§7memoryInfluence=§f" + artifact.getLastMemoryInfluence());
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


    private boolean ecosystem(CommandSender sender, String[] args) {
        var snapshot = plugin.getEcosystemEngine().snapshot();
        if (args.length >= 2 && "bias".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§dEcosystem bias: §f" + snapshot.get("bias"));
            return true;
        }
        if (args.length >= 2 && "balance".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§dEcosystem balance weights: §f" + snapshot.get("balanceWeights"));
            return true;
        }
        sender.sendMessage("§dEcosystem snapshot: §f" + snapshot);
        return true;
    }

    private boolean lineage(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "lineage");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        var lineage = plugin.getLineageRegistry().assignLineage(artifact);
        sender.sendMessage("§dLineage id=§f" + lineage.lineageId() + " §7generation=§f" + lineage.depth());
        sender.sendMessage("§7traits=§f" + lineage.lineageTraits());
        sender.sendMessage("§7ancestors=§f" + lineage.ancestors().stream().limit(8).toList());
        return true;
    }

    private boolean genome(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || !"interactions".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cUsage: /" + label + " debug genome interactions");
            return true;
        }
        Map<UUID, Artifact> loadedArtifacts = plugin.getArtifactManager().getLoadedArtifacts();
        if (loadedArtifacts.isEmpty()) {
            sender.sendMessage("§eNo loaded artifacts found. Trigger activity first, then retry.");
            return true;
        }

        TraitInteractionAnalyzer analyzer = new TraitInteractionAnalyzer();
        var matrix = analyzer.analyze(loadedArtifacts.values(), plugin.getLineageRegistry().lineages().values());
        InteractionHeatmapExporter exporter = new InteractionHeatmapExporter();
        try {
            var result = exporter.export(
                    matrix,
                    java.nio.file.Path.of("analytics/visualizations/trait-interaction-heatmap.png"),
                    java.nio.file.Path.of("analytics/visualizations/trait-interaction-matrix.csv")
            );
            sender.sendMessage("§aGenerated trait interaction heatmap: " + result.heatmapPath());
            sender.sendMessage("§aGenerated trait interaction matrix: " + result.matrixCsvPath());
        } catch (Exception exception) {
            sender.sendMessage("§cFailed to export interaction heatmap: " + exception.getMessage());
            return true;
        }

        sender.sendMessage("§dTop interaction pairs:");
        for (var entry : matrix.topPairs(10)) {
            sender.sendMessage("§7- §f" + entry.getKey() + " §8=> §b" + entry.getValue());
        }
        return true;
    }

    private boolean projection(CommandSender sender, String[] args) {
        var stats = plugin.getItemAbilityManager().traitProjectionStats();
        if (args.length >= 2 && "cache".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§dProjection mode: §f" + stats.scoringMode());
            sender.sendMessage("§dProjection cache size/capacity: §f" + stats.cacheSize() + "/" + stats.cacheCapacity());
            sender.sendMessage("§7hits=§f" + stats.cacheHits() + " §7misses=§f" + stats.cacheMisses() + " §7evictions=§f" + stats.cacheEvictions()
                    + " §7hitRate=§f" + String.format(Locale.ROOT, "%.2f%%", stats.cacheHitRate() * 100.0D));
            return true;
        }
        if (args.length >= 2 && "stats".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§dProjection stats: mode=§f" + stats.scoringMode() + " §7optimized=§f" + stats.optimizedEnabled()
                    + " §7vectors=§f" + stats.abilityVectorCount() + " §7dims=§f" + stats.dimensions());
            sender.sendMessage("§7scoringCalls=§f" + stats.scoringCalls() + " §7avgMicros=§f"
                    + String.format(Locale.ROOT, "%.3f", stats.averageScoringMicros())
                    + " §7speedup~=§f" + String.format(Locale.ROOT, "%.2fx", stats.estimatedSpeedupX()));
            return true;
        }
        sender.sendMessage("§dProjection optimization: §f" + (stats.optimizedEnabled() ? "enabled" : "disabled"));
        sender.sendMessage("§7Use /obtuseloot debug projection cache or /obtuseloot debug projection stats for details.");
        return true;
    }

    private boolean reset(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 1, "reset");
        if (target == null) return true;
        plugin.getArtifactManager().recreate(target.getUniqueId());
        plugin.getReputationManager().reset(target.getUniqueId());
        plugin.getCombatContextManager().clearContext(target.getUniqueId());
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
        plugin.getItemAbilityManager().setTriggerSubscriptionIndexingEnabled(RuntimeSettings.get().triggerSubscriptionIndexing());
        if (!RuntimeSettings.get().triggerSubscriptionIndexing()) {
            plugin.getItemAbilityManager().clearAllSubscriptions();
        }
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

    private boolean ability(CommandSender sender, String label, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "show";
        int targetIndex = ("explain".equals(action) || "tree".equals(action)) ? 3 : 2;
        Player target = resolveTarget(sender, label, args, targetIndex, "ability " + action);
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        AbilityProfile profile = plugin.getItemAbilityManager().profileFor(artifact, rep);
        if ("refresh".equals(action)) {
            sender.sendMessage("§aAbility profile refreshed for " + target.getName() + ": " + profile.profileId());
            return true;
        }
        if ("explain".equals(action)) {
            sender.sendMessage("§dAbility explain -> id=" + profile.profileId());
            sender.sendMessage("§7branchPath=§f" + artifact.getLastAbilityBranchPath());
            sender.sendMessage("§7mutationHistory=§f" + artifact.getLastMutationHistory());
            sender.sendMessage("§7memoryInfluence=§f" + artifact.getLastMemoryInfluence());
            return true;
        }
        if ("tree".equals(action)) {
            sender.sendMessage("§dAbility tree for " + target.getName() + ": " + artifact.getLastAbilityBranchPath());
            return true;
        }
        sender.sendMessage("§dAbility profile for " + target.getName() + ": §f" + profile.profileId());
        sender.sendMessage("§7Abilities: §f" + profile.abilities().stream().map(a -> a.name() + "(" + a.trigger() + "/" + a.mechanic() + ")").toList());
        return true;
    }


    private boolean memory(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "memory");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        sender.sendMessage("§dMemory for " + target.getName() + ": §f" + artifact.getMemory().snapshot());
        sender.sendMessage("§7memoryInfluence=§f" + artifact.getLastMemoryInfluence());
        return true;
    }

    private boolean simulate(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || "help".equalsIgnoreCase(args[1])) {
            sendSimulateHelp(sender, label);
            return true;
        }

        String scenario = args[1].toLowerCase(Locale.ROOT);
        return switch (scenario) {
            case "hit" -> simulateHit(sender, label, args);
            case "move" -> simulateMove(sender, label, args);
            case "lowhp" -> simulateLowHp(sender, label, args);
            case "kill" -> simulateKill(sender, label, args);
            case "multikill" -> simulateMultiKill(sender, label, args);
            case "bosses" -> simulateBosses(sender, label, args);
            case "chaos" -> simulateChaos(sender, label, args);
            case "cycle" -> simulateCycle(sender, label, args);
            case "resetcontext" -> simulateResetContext(sender, label, args);
            case "path" -> simulatePath(sender, label, args);
            default -> {
                sender.sendMessage("§cUnknown simulate command. Try /" + label + " debug simulate help");
                yield true;
            }
        };
    }

    private boolean simulateHit(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug simulate hit <damage> [player]");
            return true;
        }
        Double damage = parseDouble(sender, args[2]);
        if (damage == null) return true;
        Player target = resolveTarget(sender, label, args, 3, "simulate hit " + args[2]);
        if (target == null) return true;

        ArtifactProcessor.processSimulatedCombat(target, Math.max(0.0D, damage));
        saveOnly(target);
        sender.sendMessage("§aSimulated hit for " + target.getName() + " with damage " + damage + ".");
        return true;
    }

    private boolean simulateMove(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug simulate move <distance> [player]");
            return true;
        }
        Double distance = parseDouble(sender, args[2]);
        if (distance == null) return true;
        Player target = resolveTarget(sender, label, args, 3, "simulate move " + args[2]);
        if (target == null) return true;

        CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
        context.markCombat();
        context.addMovement(Math.max(0.0D, distance));
        saveOnly(target);
        sender.sendMessage("§aInjected " + distance + " movement into " + target.getName() + "'s combat context.");
        return true;
    }

    private boolean simulateLowHp(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "simulate lowhp");
        if (target == null) return true;

        CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
        context.markCombat();
        context.setLowHealthFlag(true);
        context.setLowHealthEnteredAt(System.currentTimeMillis());
        context.setLastKnownHealth(Math.min(RuntimeSettings.get().lowHealthThreshold(), target.getHealth()));
        saveOnly(target);
        sender.sendMessage("§aMarked " + target.getName() + " as low-health for survival testing.");
        return true;
    }

    private boolean simulateKill(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "simulate kill");
        if (target == null) return true;

        prepareKillWindow(target, 1);
        ArtifactProcessor.processSimulatedKill(target);
        saveOnly(target);
        sender.sendMessage("§aSimulated kill progression for " + target.getName() + ".");
        return true;
    }

    private boolean simulateMultiKill(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug simulate multikill <count> [player]");
            return true;
        }
        Integer count = parseInt(sender, args[2]);
        if (count == null || count <= 0) {
            sender.sendMessage("§cCount must be a positive integer.");
            return true;
        }
        Player target = resolveTarget(sender, label, args, 3, "simulate multikill " + args[2]);
        if (target == null) return true;

        prepareKillWindow(target, Math.max(2, count));
        ArtifactProcessor.processSimulatedMultiKill(target, count);
        saveOnly(target);
        sender.sendMessage("§aSimulated " + count + " rapid kills for " + target.getName() + ".");
        return true;
    }

    private boolean simulateBosses(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug simulate bosses <count> [player]");
            return true;
        }
        Integer count = parseInt(sender, args[2]);
        if (count == null || count <= 0) {
            sender.sendMessage("§cCount must be a positive integer.");
            return true;
        }
        Player target = resolveTarget(sender, label, args, 3, "simulate bosses " + args[2]);
        if (target == null) return true;

        for (int i = 0; i < count; i++) {
            prepareKillWindow(target, 1);
            ArtifactProcessor.processSimulatedBossKill(target);
        }
        saveOnly(target);
        sender.sendMessage("§aSimulated " + count + " boss kills for " + target.getName() + ".");
        return true;
    }

    private boolean simulateChaos(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "simulate chaos");
        if (target == null) return true;

        CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
        context.markCombat();
        context.addMovement(RuntimeSettings.get().mobilityDistanceThreshold() + 2.0D);

        long now = System.currentTimeMillis();
        long window = RuntimeSettings.get().killChainWindowMs();
        for (int i = 0; i < Math.max(4, RuntimeSettings.get().multiTargetChaosThreshold()); i++) {
            context.addTarget(UUID.randomUUID());
            context.addKillTimestamp(now - Math.min(window / 2L, i * 300L));
        }

        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        rep.recordChaos();
        saveOnly(target);
        sender.sendMessage("§aPrepared chaotic context for " + target.getName() + ".");
        return true;
    }

    private boolean simulateCycle(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "simulate cycle");
        if (target == null) return true;

        CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
        context.markCombat();
        context.addMovement(RuntimeSettings.get().mobilityDistanceThreshold() + 8.0D);
        context.setLowHealthFlag(true);
        context.setLowHealthEnteredAt(System.currentTimeMillis());
        context.setLastKnownHealth(RuntimeSettings.get().lowHealthThreshold() - 1.0D);

        ArtifactProcessor.processSimulatedCombat(target, RuntimeSettings.get().precisionThresholdDamage() + 2.0D);
        for (int i = 0; i < 4; i++) {
            prepareKillWindow(target, 4);
            ArtifactProcessor.processSimulatedKill(target);
        }
        ArtifactProcessor.processSimulatedBossKill(target);
        ArtifactProcessor.processSimulatedBossKill(target);

        saveOnly(target);
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        sender.sendMessage("§aCompleted simulation cycle for " + target.getName() + ": archetype="
                + artifact.getArchetypePath() + ", evolution=" + artifact.getEvolutionPath() + ", drift=" + artifact.getDriftAlignment() + ".");
        return true;
    }

    private boolean simulateResetContext(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "simulate resetcontext");
        if (target == null) return true;

        plugin.getCombatContextManager().resetTransient(target.getUniqueId());
        plugin.getCombatContextManager().clearContext(target.getUniqueId());
        saveOnly(target);
        sender.sendMessage("§aCleared combat context for " + target.getName() + ".");
        return true;
    }

    private boolean simulatePath(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug simulate path <" + String.join("|", SIM_PATHS) + "> [player]");
            return true;
        }
        String profile = args[2].toLowerCase(Locale.ROOT);
        if (!SIM_PATHS.contains(profile)) {
            sender.sendMessage("§cUnknown profile. Allowed: " + SIM_PATHS);
            return true;
        }
        Player target = resolveTarget(sender, label, args, 3, "simulate path " + profile);
        if (target == null) return true;

        switch (profile) {
            case "precision" -> ArtifactProcessor.processSimulatedCombat(target, RuntimeSettings.get().precisionThresholdDamage() + 3.0D);
            case "brutality" -> ArtifactProcessor.processSimulatedCombat(target, 2.0D);
            case "mobility" -> {
                plugin.getCombatContextManager().get(target.getUniqueId()).addMovement(RuntimeSettings.get().mobilityDistanceThreshold() + 3.0D);
                ArtifactProcessor.processSimulatedCombat(target, 7.0D);
            }
            case "survival" -> {
                CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
                context.markCombat();
                context.setLowHealthFlag(true);
                context.setLowHealthEnteredAt(System.currentTimeMillis());
                context.setLastKnownHealth(RuntimeSettings.get().lowHealthThreshold() - 1.0D);
                ArtifactProcessor.processSimulatedCombat(target, 8.0D);
            }
            case "chaos" -> simulateChaos(sender, label, new String[]{"simulate", "chaos", target.getName()});
            case "boss" -> ArtifactProcessor.processSimulatedBossKill(target);
            case "hybrid" -> {
                ArtifactProcessor.processSimulatedCombat(target, RuntimeSettings.get().precisionThresholdDamage() + 2.0D);
                ArtifactProcessor.processSimulatedCombat(target, 4.0D);
                ArtifactProcessor.processSimulatedKill(target);
            }
            case "awaken" -> {
                for (int i = 0; i < 5; i++) ArtifactProcessor.processSimulatedKill(target);
                Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
                ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
                plugin.getAwakeningEngine().forceAwakening(target, artifact, rep);
                plugin.getLoreEngine().refreshLore(target, artifact, rep);
            }
            case "drift" -> {
                for (int i = 0; i < 5; i++) ArtifactProcessor.processSimulatedKill(target);
                Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
                ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
                plugin.getDriftEngine().forceDrift(target, artifact, rep);
                plugin.getLoreEngine().refreshLore(target, artifact, rep);
            }
            default -> {
            }
        }

        saveOnly(target);
        sender.sendMessage("§aApplied simulate path '" + profile + "' for " + target.getName() + ".");
        return true;
    }


    private boolean seed(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + label + " debug seed <show|reroll|set|export|import> ...");
            return true;
        }

        String mode = args[1].toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "show" -> seedShow(sender, label, args);
            case "reroll" -> seedReroll(sender, label, args);
            case "set" -> seedSet(sender, label, args, false);
            case "import" -> seedSet(sender, label, args, true);
            case "export" -> seedExport(sender, label, args);
            default -> {
                sender.sendMessage("§cUnknown seed mode. Use show/reroll/set/export/import.");
                yield true;
            }
        };
    }

    private boolean seedShow(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "seed show");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        sender.sendMessage("§a" + target.getName() + " artifact seed: §f" + artifact.getArtifactSeed());
        return true;
    }

    private boolean seedReroll(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "seed reroll");
        if (target == null) return true;

        Artifact existing = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        long oldSeed = existing.getArtifactSeed();
        long newSeed = plugin.getArtifactManager().rollSeed();
        Artifact artifact = applySeedChange(target, newSeed);

        sender.sendMessage("§aRerolled " + target.getName() + "'s artifact seed: §f" + oldSeed + " §7-> §f" + artifact.getArtifactSeed());
        sender.sendMessage("§7Seed change reset artifact identity and progression.");
        return true;
    }

    private boolean seedSet(CommandSender sender, String label, String[] args, boolean imported) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " debug seed " + (imported ? "import" : "set") + " <seed> [player]");
            return true;
        }

        Long parsedSeed = parseLong(sender, args[2]);
        if (parsedSeed == null) return true;

        Player target = resolveTarget(sender, label, args, 3, "seed " + (imported ? "import" : "set") + " " + args[2]);
        if (target == null) return true;

        Artifact existing = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        long oldSeed = existing.getArtifactSeed();
        if (oldSeed == parsedSeed) {
            sender.sendMessage("§eSeed is already " + parsedSeed + " for " + target.getName() + ". No reset required.");
            return true;
        }

        Artifact artifact = applySeedChange(target, parsedSeed);

        String verb = imported ? "Imported" : "Set";
        sender.sendMessage("§a" + verb + " artifact seed for " + target.getName() + ": §f" + oldSeed + " §7-> §f" + artifact.getArtifactSeed());
        sender.sendMessage("§7Seed change reset artifact identity and progression.");
        return true;
    }

    private boolean seedExport(CommandSender sender, String label, String[] args) {
        Player target = resolveTarget(sender, label, args, 2, "seed export");
        if (target == null) return true;
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        sender.sendMessage("§a" + target.getName() + " seed export: §f" + artifact.getArtifactSeed());
        sender.sendMessage("§7name=§f" + artifact.getGeneratedName() + " §7lineage=§f" + artifact.getLatentLineage() + " §7currentDriftAlignment=§f" + artifact.getDriftAlignment());
        return true;
    }

    private void prepareKillWindow(Player target, int chainSize) {
        CombatContext context = plugin.getCombatContextManager().get(target.getUniqueId());
        context.markCombat();
        long now = System.currentTimeMillis();
        long window = RuntimeSettings.get().killChainWindowMs();
        for (int i = 0; i < chainSize; i++) {
            context.addKillTimestamp(now - Math.min(window / 2L, i * 250L));
            context.addTarget(UUID.randomUUID());
        }
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        rep.recordKillChain(chainSize);
        if (chainSize >= 2) {
            rep.recordChaos();
        }
    }


    private boolean persistence(CommandSender sender, String label, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if ("status".equals(action) || "backend".equals(action)) {
            sender.sendMessage("§dPersistence backend: §f" + plugin.getPersistenceManager().backendName());
            sender.sendMessage("§7status=§f" + plugin.getPersistenceManager().statusMessage());
            sender.sendMessage("§7fallbackUsed=§f" + plugin.getPersistenceManager().fallbackUsed());
            return true;
        }
        if ("test".equals(action)) {
            sender.sendMessage("§aPersistence test: backend='" + plugin.getPersistenceManager().backendName() + "' status='" + plugin.getPersistenceManager().statusMessage() + "'");
            return true;
        }
        if ("migrate".equals(action)) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /" + label + " debug persistence migrate yaml-to-sqlite|yaml-to-mysql|nbt-artifacts");
                return true;
            }
            String mode = args[2].toLowerCase(Locale.ROOT);
            if ("nbt-artifacts".equals(mode) || "migrate-nbt-artifacts".equals(mode)) {
                int players = 0;
                int migrated = 0;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    players++;
                    migrated += plugin.getArtifactItemStorage().migrateInventory(online);
                }
                sender.sendMessage("§aNBT artifact migration scan complete. onlinePlayers=§f" + players + "§a migratedItems=§f" + migrated);
                return true;
            }
            PersistenceMigrator migrator = new PersistenceMigrator(plugin);
            try {
                int count;
                if ("yaml-to-sqlite".equals(mode)) {
                    SqlitePersistenceProvider provider = new SqlitePersistenceProvider(plugin, obtuseloot.persistence.PersistenceConfig.from(plugin.getConfig(), plugin.getDataFolder()).sqliteFile());
                    count = migrator.migrateYamlTo(provider.playerStateStore());
                    provider.close();
                } else if ("yaml-to-mysql".equals(mode)) {
                    MySqlPersistenceProvider provider = new MySqlPersistenceProvider(plugin, obtuseloot.persistence.PersistenceConfig.from(plugin.getConfig(), plugin.getDataFolder()).mySql());
                    count = migrator.migrateYamlTo(provider.playerStateStore());
                    provider.close();
                } else {
                    sender.sendMessage("§cUnknown migration mode: " + mode);
                    return true;
                }
                sender.sendMessage("§aMigration completed. Player records processed: §f" + count);
            } catch (RuntimeException ex) {
                sender.sendMessage("§cMigration failed: " + ex.getMessage());
            }
            return true;
        }
        sender.sendMessage("§cUsage: /" + label + " debug persistence [backend|test|migrate]");
        return true;
    }

    private boolean subscriptions(CommandSender sender, String label, String[] args) {
        if (args.length >= 2 && "stats".equalsIgnoreCase(args[1])) {
            var stats = plugin.getItemAbilityManager().indexStats();
            sender.sendMessage("§dTrigger subscription index: §f" + (stats.enabled() ? "enabled" : "disabled"));
            sender.sendMessage("§7indexedPlayers=§f" + stats.indexedPlayers()
                    + " §7rebuilds=§f" + stats.rebuildCount()
                    + " §7avgRebuildMicros=§f" + String.format(Locale.ROOT, "%.3f", stats.averageRebuildMicros()));
            sender.sendMessage("§7dispatch=§f" + stats.dispatchCalls()
                    + " §7indexed=§f" + stats.indexedDispatchCalls()
                    + " §7fallbackScans=§f" + stats.fallbackFullScanCalls()
                    + " §7avgSubscribers=§f" + String.format(Locale.ROOT, "%.3f", stats.averageIndexedSubscribers()));
            return true;
        }

        Player target = resolveTarget(sender, label, args, 1, "subscriptions");
        if (target == null) return true;

        PlayerArtifactTriggerMap map = plugin.getItemAbilityManager().triggerMap(target.getUniqueId());
        if (map == null) {
            Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
            ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
            plugin.getItemAbilityManager().rebuildSubscriptions(target.getUniqueId(), artifact, rep, "debug-subscriptions-view");
            map = plugin.getItemAbilityManager().triggerMap(target.getUniqueId());
        }

        sender.sendMessage("§dSubscriptions for §f" + target.getName()
                + " §7(totalBindings=§f" + (map == null ? 0 : map.totalBindings())
                + "§7, lastReason=§f" + (map == null ? "none" : map.lastRebuildReason()) + "§7)");
        if (map == null) {
            sender.sendMessage("§7No trigger map is available.");
            return true;
        }

        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            var bindings = map.bindingsFor(trigger);
            if (!bindings.isEmpty()) {
                sender.sendMessage("§7- §f" + trigger + " §7=> §f" + bindings.size()
                        + " §7" + bindings.stream().limit(4).map(ArtifactTriggerBinding::abilityId).toList()
                        + (bindings.size() > 4 ? " ..." : ""));
            }
        }
        return true;
    }

    private boolean hasDebugPermission(CommandSender sender) {
        if (sender instanceof Player player && !player.isOp() && !sender.hasPermission(PERMISSION_DEBUG)) {
            sender.sendMessage("§cYou do not have permission: " + PERMISSION_DEBUG);
            return false;
        }
        return true;
    }

    private Artifact applySeedChange(Player target, long newSeed) {
        Artifact artifact = plugin.getArtifactManager().reseed(target.getUniqueId(), newSeed);
        plugin.getReputationManager().reset(target.getUniqueId());
        plugin.getCombatContextManager().clearContext(target.getUniqueId());
        refreshAndSave(target);
        return artifact;
    }

    private void refreshAndSave(Player target) {
        Artifact artifact = plugin.getArtifactManager().getOrCreate(target.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(target.getUniqueId());
        plugin.getItemAbilityManager().rebuildSubscriptions(target.getUniqueId(), artifact, rep, "debug-refresh");
        plugin.getLoreEngine().refreshLore(target, artifact, rep);
        saveOnly(target);
    }

    private void saveOnly(Player target) {
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

    private Long parseLong(CommandSender sender, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid long seed: " + value);
            return null;
        }
    }

    private Integer parseInt(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid integer: " + value);
            return null;
        }
    }

    private Double parseDouble(CommandSender sender, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cInvalid number: " + value);
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
            default -> {
            }
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

    private String formatStatMap(Artifact artifact) {
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
                "/" + label + " debug path set <evolutionPath> [player]",
                "/" + label + " debug seed show [player]",
                "/" + label + " debug seed reroll [player]",
                "/" + label + " debug seed set <seed> [player]",
                "/" + label + " debug seed export [player]",
                "/" + label + " debug seed import <seed> [player]",
                "/" + label + " debug ability [player]",
                "/" + label + " debug memory [show|reset] [player]",
                "/" + label + " debug persistence [status|migrate <sqlite|mysql>]",
                "/" + label + " debug artifact [storage|resolve] [player]",
                "/" + label + " debug ecosystem [bias|balance]",
                "/" + label + " debug lineage [player]",
                "/" + label + " debug genome interactions",
                "/" + label + " debug projection [cache|stats]",
                "/" + label + " debug subscriptions [stats|player]",
                "/" + label + " debug simulate help"
        );
        sender.sendMessage("§dObtuseLoot Debug Commands:");
        for (String line : lines) {
            sender.sendMessage("§7- §f" + line);
        }
    }

    private void sendSimulateHelp(CommandSender sender, String label) {
        sender.sendMessage("§dSimulation Commands:");
        sender.sendMessage("§7- §f/" + label + " debug simulate hit <damage> [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate move <distance> [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate lowhp [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate kill [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate multikill <count> [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate bosses <count> [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate chaos [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate cycle [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate resetcontext [player]");
        sender.sendMessage("§7- §f/" + label + " debug simulate path <profile> [player]");
    }
}
