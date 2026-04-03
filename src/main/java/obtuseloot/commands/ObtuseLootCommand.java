package obtuseloot.commands;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.abilities.AbilityFamily;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.ArtifactIdentityTransition;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.memory.MemoryInfluenceResolver;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.significance.ArtifactSignificanceResolver;
import obtuseloot.names.NamePoolManager;
import obtuseloot.names.ArtifactNameResolver;

import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime command handling for both player and console senders.
 */
public final class ObtuseLootCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION_HELP = "obtuseloot.help";
    private static final String PERMISSION_INFO = "obtuseloot.info";
    private static final String PERMISSION_ADMIN = "obtuseloot.admin";
    private static final String PERMISSION_EDIT = "obtuseloot.edit";
    private static final String PERMISSION_COMMAND_GIVE = "obtuseloot.command.give";
    private static final String PERMISSION_COMMAND_CONVERT = "obtuseloot.command.convert";
    private static final String PERMISSION_COMMAND_REROLL = "obtuseloot.command.reroll";
    private static final String PERMISSION_COMMAND_INSPECT = "obtuseloot.command.inspect";
    private static final String PERMISSION_COMMAND_FORCE_AWAKEN = "obtuseloot.command.forceawaken";
    private static final String PERMISSION_COMMAND_FORCE_CONVERGE = "obtuseloot.command.forceconverge";
    private static final String PERMISSION_COMMAND_REPAIR_STATE = "obtuseloot.command.repairstate";
    private static final String PERMISSION_COMMAND_DEBUG_PROFILE = "obtuseloot.command.debugprofile";
    private static final String PERMISSION_COMMAND_GIVE_SPECIFIC = "obtuseloot.command.givespecific";
    private static final String PERMISSION_COMMAND_DUMP_HELD = "obtuseloot.command.dumpheld";

    private final ObtuseLoot plugin;
    private final DebugCommand debugCommand;
    private final DebugTabCompleter debugTabCompleter;

    public ObtuseLootCommand(ObtuseLoot plugin) {
        this.plugin = plugin;
        this.debugCommand = new DebugCommand(plugin);
        this.debugTabCompleter = new DebugTabCompleter();
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
            sender.sendMessage("§7/" + label + " inspect §8- §fInspect held artifact identity details §8[" + PERMISSION_COMMAND_INSPECT + "]");
            sender.sendMessage("§7/" + label + " refresh [player] §8- §fRegenerate a player's artifact profile §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " reset [player] §8- §fClear a player's tracked artifact and reputation state §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " reload §8- §fReload config-driven runtime settings and name pools §8["
                    + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " dashboard §8- §fShow ecosystem health and dashboard link §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem [health] §8- §fShow ecosystem health and live safety metrics §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem dump §8- §fOutput a JSON safety snapshot to " + plugin.getPaths().analyticsRoot().resolve("safety") + "/ §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem reset-metrics §8- §fClear rolling safety metrics §8[" + PERMISSION_ADMIN + "]");
            sender.sendMessage("§7/" + label + " ecosystem map [lineage|species|collapse] §8- §fStart live ecosystem hotspot rendering §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem map genome <trait> §8- §fRender genome trait intensity hotspots §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem map off §8- §fDisable live ecosystem map rendering §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " ecosystem environment §8- §fShow active environmental selection pressure modifiers §8[" + PERMISSION_INFO + "]");
            sender.sendMessage("§7/" + label + " debug help §8- §fArtifact ecosystem debug suite (seed + simulate tooling) §8[obtuseloot.debug]");
            sender.sendMessage("§7/" + label + " debug seed show|reroll|set|export|import §8- §fDeterministic seed controls §8[obtuseloot.debug]");
            sender.sendMessage("§7/" + label + " debug simulate help §8- §fSimulation scenarios and path profiles §8[obtuseloot.debug]");
            sender.sendMessage("§7/" + label + " addname <pool> <value> §8- §fAdd a name entry to a pool (prefixes/suffixes) §8["
                    + PERMISSION_EDIT + "]");
            sender.sendMessage("§7/" + label + " removename <pool> <value> §8- §fRemove a name entry from a pool §8["
                    + PERMISSION_EDIT + "]");
            sender.sendMessage("§7/" + label + " give <player> §8- §fGenerate and deliver a fresh artifact item §8[" + PERMISSION_COMMAND_GIVE + "]");
            sender.sendMessage("§7/" + label + " convert §8- §fConvert held equipment into a fresh artifact identity §8[" + PERMISSION_COMMAND_CONVERT + "]");
            sender.sendMessage("§7/" + label + " reroll §8- §fReroll held artifact identity §8[" + PERMISSION_COMMAND_REROLL + "]");
            sender.sendMessage("§7/" + label + " force-awaken §8- §fForce awakening on held artifact identity §8[" + PERMISSION_COMMAND_FORCE_AWAKEN + "]");
            sender.sendMessage("§7/" + label + " force-converge §8- §fForce convergence via the real convergence pipeline §8[" + PERMISSION_COMMAND_FORCE_CONVERGE + "]");
            sender.sendMessage("§7/" + label + " repair-state §8- §fRebuild held artifact derived state without identity mutation §8[" + PERMISSION_COMMAND_REPAIR_STATE + "]");
            sender.sendMessage("§7/" + label + " debug-profile §8- §fShow structured held artifact deep diagnostics §8[" + PERMISSION_COMMAND_DEBUG_PROFILE + "]");
            sender.sendMessage("§7/" + label + " give-specific <player> <archetype|family> §8- §fGenerate constrained artifact identity §8[" + PERMISSION_COMMAND_GIVE_SPECIFIC + "]");
            sender.sendMessage("§7/" + label + " dump-held §8- §fLog structured snapshot for held artifact §8[" + PERMISSION_COMMAND_DUMP_HELD + "]");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_INFO)) {
                return true;
            }

            sender.sendMessage("§dObtuseLoot is active. Progression hooks are wired for combat/kill events.");
            return true;
        }

        if ("inspect".equalsIgnoreCase(args[0])) return inspectHeldArtifact(sender);
        if ("give".equalsIgnoreCase(args[0])) return handleGive(sender, label, args);
        if ("convert".equalsIgnoreCase(args[0])) return handleConvert(sender);
        if ("reroll".equalsIgnoreCase(args[0])) return handleReroll(sender);
        if ("force-awaken".equalsIgnoreCase(args[0])) return handleForceAwaken(sender);
        if ("force-converge".equalsIgnoreCase(args[0])) return handleForceConverge(sender);
        if ("repair-state".equalsIgnoreCase(args[0])) return handleRepairState(sender);
        if ("debug-profile".equalsIgnoreCase(args[0])) return handleDebugProfile(sender);
        if ("give-specific".equalsIgnoreCase(args[0])) return handleGiveSpecific(sender, label, args);
        if ("dump-held".equalsIgnoreCase(args[0])) return handleDumpHeld(sender);

        if ("refresh".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_ADMIN)) {
                return true;
            }

            Player target = resolveTarget(sender, args, label, "refresh");
            if (target == null) {
                return true;
            }

            plugin.getArtifactManager().unload(target.getUniqueId());
            Artifact refreshed = plugin.getArtifactManager().getOrCreate(target.getUniqueId());

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

            plugin.getArtifactManager().unload(target.getUniqueId());
            plugin.getReputationManager().unload(target.getUniqueId());
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

        if ("ecosystem".equalsIgnoreCase(args[0])) {
            if (!hasPermission(sender, PERMISSION_INFO)) {
                return true;
            }
            if (args.length >= 2 && "environment".equalsIgnoreCase(args[1])) {
                var pressureEngine = plugin.getExperienceEvolutionEngine().pressureEngine();
                var event = pressureEngine.currentEvent();
                sender.sendMessage("§dEnvironmental event: §f" + event.name() + " §7(remaining seasons: " + event.remainingSeasons() + ")");
                for (GenomeTrait trait : GenomeTrait.values()) {
                    double multiplier = pressureEngine.multiplierFor(trait);
                    sender.sendMessage("§7- §f" + trait.name().toLowerCase() + " §8x§d" + String.format(java.util.Locale.ROOT, "%.3f", multiplier));
                }
                return true;
            }
            if (args.length >= 2 && "map".equalsIgnoreCase(args[1])) {
                if (args.length >= 3 && "off".equalsIgnoreCase(args[2]) && sender instanceof Player player) {
                    plugin.getEcosystemMapRenderer().stop(player);
                    sender.sendMessage("§aEcosystem map visualization disabled.");
                    return true;
                }
                return plugin.getEcosystemMapRenderer().handleCommand(sender, args);
            }
        }


        if ("debug".equalsIgnoreCase(args[0])) {
            String[] debugArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            return debugCommand.execute(sender, label, debugArgs);
        }

        if ("addname".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /" + label + " addname <prefixes|suffixes> <value>");
                return true;
            }

            return handleNameEdit(sender, true, args[1], joinFrom(args, 2));
        }

        if ("removename".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /" + label + " removename <prefixes|suffixes> <value>");
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
            sender.sendMessage("§cUnknown pool '" + pool + "'. Valid pools: prefixes, suffixes.");
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
            addIfPermitted(sender, candidates, "give", PERMISSION_COMMAND_GIVE);
            addIfPermitted(sender, candidates, "convert", PERMISSION_COMMAND_CONVERT);
            addIfPermitted(sender, candidates, "reroll", PERMISSION_COMMAND_REROLL);
            addIfPermitted(sender, candidates, "inspect", PERMISSION_COMMAND_INSPECT);
            addIfPermitted(sender, candidates, "force-awaken", PERMISSION_COMMAND_FORCE_AWAKEN);
            addIfPermitted(sender, candidates, "force-converge", PERMISSION_COMMAND_FORCE_CONVERGE);
            addIfPermitted(sender, candidates, "repair-state", PERMISSION_COMMAND_REPAIR_STATE);
            addIfPermitted(sender, candidates, "debug-profile", PERMISSION_COMMAND_DEBUG_PROFILE);
            addIfPermitted(sender, candidates, "give-specific", PERMISSION_COMMAND_GIVE_SPECIFIC);
            addIfPermitted(sender, candidates, "dump-held", PERMISSION_COMMAND_DUMP_HELD);
            addIfPermitted(sender, candidates, "refresh", PERMISSION_ADMIN);
            addIfPermitted(sender, candidates, "reset", PERMISSION_ADMIN);
            addIfPermitted(sender, candidates, "reload", PERMISSION_ADMIN);
            addIfPermitted(sender, candidates, "ecosystem", PERMISSION_INFO);
            if (sender.hasPermission("obtuseloot.debug") || !(sender instanceof Player) || ((Player) sender).isOp()) {
                candidates.add("debug");
            }
            if (hasAnyEditPermission(sender)) {
                candidates.add("addname");
                candidates.add("removename");
            }
            return filterByPrefix(candidates, args[0]);
        }

        if (args.length == 2 && isPlayerTargetCommand(args[0])) {
            if ((("give".equalsIgnoreCase(args[0]) && !sender.hasPermission(PERMISSION_COMMAND_GIVE))
                    || ("give-specific".equalsIgnoreCase(args[0]) && !sender.hasPermission(PERMISSION_COMMAND_GIVE_SPECIFIC))
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

        if (args.length == 2 && "ecosystem".equalsIgnoreCase(args[0])) {
            List<String> ecosystemSubs = new ArrayList<>(List.of("health", "dashboard", "map", "environment", "dump"));
            if (sender.hasPermission(PERMISSION_ADMIN)) {
                ecosystemSubs.add("reset-metrics");
            }
            return filterByPrefix(ecosystemSubs, args[1]);
        }

        if (args.length == 3 && "ecosystem".equalsIgnoreCase(args[0]) && "map".equalsIgnoreCase(args[1])) {
            return filterByPrefix(List.of("lineage", "genome", "collapse", "species", "off"), args[2]);
        }

        if (args.length == 3 && "give-specific".equalsIgnoreCase(args[0])) {
            List<String> constrainedTypes = new ArrayList<>(EquipmentArchetype.allIds());
            for (AbilityFamily family : AbilityFamily.values()) {
                constrainedTypes.add(family.name().toLowerCase(java.util.Locale.ROOT));
            }
            return filterByPrefix(constrainedTypes, args[2]);
        }

        if (args.length == 4 && "ecosystem".equalsIgnoreCase(args[0]) && "map".equalsIgnoreCase(args[1])
                && "genome".equalsIgnoreCase(args[2])) {
            List<String> traits = new ArrayList<>();
            for (var trait : obtuseloot.abilities.genome.GenomeTrait.values()) {
                traits.add(trait.name().toLowerCase());
            }
            return filterByPrefix(traits, args[3]);
        }

        if (args.length >= 1 && "debug".equalsIgnoreCase(args[0])) {
            return debugTabCompleter.complete(sender, args);
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
        return "give".equalsIgnoreCase(subcommand)
                || "give-specific".equalsIgnoreCase(subcommand)
                || "refresh".equalsIgnoreCase(subcommand)
                || "reset".equalsIgnoreCase(subcommand);
    }

    private boolean handleGive(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, PERMISSION_COMMAND_GIVE)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /" + label + " give <player>");
            return true;
        }
        Player target = sender.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cInvalid player: " + args[1]);
            return true;
        }
        try {
            Artifact artifact = plugin.getArtifactManager().recreate(target.getUniqueId());
            ArtifactReputation reputation = plugin.getReputationManager().get(target.getUniqueId());
            plugin.getItemAbilityManager().rebuildSubscriptions(target.getUniqueId(), artifact, reputation, "command-give");
            plugin.getLoreEngine().refreshLore(target, artifact, reputation);
            plugin.getLineageRegistry().assignLineage(artifact);
            plugin.getArtifactManager().markDirty(target.getUniqueId());
            plugin.getArtifactManager().save(target.getUniqueId());
            ItemStack item = renderArtifactItem(artifact, reputation);
            var leftovers = target.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
                sender.sendMessage("§eInventory full. Artifact dropped at " + target.getName() + "'s feet.");
                target.sendMessage("§eYour inventory was full; artifact dropped at your feet.");
            } else {
                sender.sendMessage("§aGave artifact to " + target.getName() + ": §f" + artifact.getDisplayName());
            }
            return true;
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("[Command] /obtuseloot give failed for " + args[1] + ": " + ex.getMessage());
            sender.sendMessage("§cArtifact generation failed. Check server logs.");
            return true;
        }
    }

    private boolean handleConvert(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_CONVERT)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot convert");
        if (player == null) return true;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            sender.sendMessage("§cHold an equipment item first.");
            return true;
        }
        if (plugin.getArtifactItemStorage().isArtifactItem(held)) {
            sender.sendMessage("§cHeld item is already an artifact.");
            return true;
        }
        EquipmentArchetype archetype = resolveEquipmentArchetype(held);
        if (archetype == null) {
            sender.sendMessage("§cHeld item is not valid equipment (weapon/tool/armor).");
            return true;
        }
        Artifact replacement = plugin.getArtifactManager().recreateWithArchetype(
                player.getUniqueId(), archetype, "command-convert:" + archetype.id());
        finalizeIdentityReplacement(player, null, replacement, "command-convert");
        sender.sendMessage("§aConverted held item into artifact identity: §f" + replacement.getDisplayName());
        return true;
    }

    private boolean handleReroll(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_REROLL)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot reroll");
        if (player == null) return true;
        ItemStack held = player.getInventory().getItemInMainHand();
        Artifact current = resolveHeldArtifact(player, held, true);
        if (current == null) return true;
        EquipmentArchetype archetype = ArtifactArchetypeValidator.requireValid(current, "command-reroll");
        Artifact replacement = plugin.getArtifactManager().recreateWithArchetype(
                player.getUniqueId(), archetype, "command-reroll:" + archetype.id());
        finalizeIdentityReplacement(player, current, replacement, "command-reroll");
        sender.sendMessage("§aRerolled artifact identity: §f" + replacement.getDisplayName());
        return true;
    }

    private boolean inspectHeldArtifact(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_INSPECT)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot inspect");
        if (player == null) return true;
        Artifact artifact = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (artifact == null) return true;
        ArtifactSignificanceResolver significanceResolver = new ArtifactSignificanceResolver();
        sender.sendMessage("§d=== Held Artifact ===");
        sender.sendMessage("§7name=§f" + artifact.getDisplayName());
        sender.sendMessage("§7archetype=§f" + artifact.getArchetypePath() + " §7item=§f" + artifact.getItemCategory());
        sender.sendMessage("§7namingSeed=§f" + artifact.getNaming().getNamingSeed() + " §7lineage=§f" + artifact.getLatentLineage());
        sender.sendMessage("§7awakening=§f" + artifact.getAwakeningPath() + " §7convergence=§f" + artifact.getConvergencePath());
        sender.sendMessage("§7identityBirthTimestamp=§f" + artifact.getIdentityBirthTimestamp());
        sender.sendMessage("§7persistenceOriginTimestamp=§f" + artifact.getPersistenceOriginTimestamp());
        sender.sendMessage("§7significance=§f" + significanceResolver.resolve(artifact).format());
        sender.sendMessage("§7profile=§f" + artifact.getLastRegulatoryProfile() + " §7trigger=§f" + artifact.getLastTriggerProfile());
        sender.sendMessage("§7posture=§f" + artifact.getDriftAlignment() + " §7instability=§f" + artifact.getCurrentInstabilityState());
        return true;
    }

    private boolean handleForceAwaken(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_FORCE_AWAKEN)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot force-awaken");
        if (player == null) return true;
        Artifact current = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (current == null) return true;
        if (!"dormant".equalsIgnoreCase(current.getAwakeningPath())) {
            sender.sendMessage("§cHeld artifact is already awakened.");
            return true;
        }
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        ArtifactIdentityTransition transition = plugin.getAwakeningEngine().forceAwakening(player, current, rep);
        if (transition == null) {
            sender.sendMessage("§cNo valid awakening path for held artifact.");
            return true;
        }
        Artifact replacement = plugin.getArtifactManager().replaceIdentity(player.getUniqueId(), transition);
        finalizeIdentityReplacement(player, current, replacement, "command-force-awaken");
        plugin.getArtifactUsageTracker().trackAwakening(replacement);
        sender.sendMessage("§aAwakening forced: §f" + replacement.getAwakeningPath());
        return true;
    }

    private boolean handleForceConverge(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_FORCE_CONVERGE)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot force-converge");
        if (player == null) return true;
        Artifact current = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (current == null) return true;
        ArtifactReputation reputation = plugin.getReputationManager().get(player.getUniqueId());
        ArtifactIdentityTransition transition = plugin.getConvergenceEngine().evaluate(player, current, reputation);
        if (transition == null) {
            sender.sendMessage("§cConvergence rejected: no valid/non-no-op convergence path for held artifact.");
            return true;
        }
        Artifact replacement = transition.replacement();
        if (replacement == current || replacement.getArtifactSeed() == current.getArtifactSeed()) {
            throw new IllegalStateException("Force convergence must produce a replacement identity.");
        }
        if ("none".equalsIgnoreCase(replacement.getConvergencePath())
                || "none".equalsIgnoreCase(replacement.getConvergenceVariantId())
                || "none".equalsIgnoreCase(replacement.getConvergenceIdentityShape())
                || "none".equalsIgnoreCase(replacement.getConvergenceLineageTrace())
                || "none".equalsIgnoreCase(replacement.getConvergenceExpressionTrace())
                || "none".equalsIgnoreCase(replacement.getConvergenceMemorySignature())) {
            throw new IllegalStateException("Force convergence replacement missing required convergence metadata.");
        }
        plugin.getArtifactManager().replaceIdentity(player.getUniqueId(), transition);
        finalizeIdentityReplacement(player, current, replacement, "command-force-converge");
        plugin.getArtifactUsageTracker().trackConvergenceParticipation(replacement);
        sender.sendMessage("§aConvergence forced: §f" + replacement.getConvergencePath() + " §7variant=§f" + replacement.getConvergenceVariantId());
        return true;
    }

    private boolean handleRepairState(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_REPAIR_STATE)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot repair-state");
        if (player == null) return true;
        Artifact artifact = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (artifact == null) return true;
        ArtifactArchetypeValidator.requireValid(artifact, "command-repair-state");
        ArtifactReputation reputation = plugin.getReputationManager().get(player.getUniqueId());
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        plugin.getLineageRegistry().assignLineage(artifact);
        plugin.getItemAbilityManager().profileFor(artifact, reputation);
        plugin.getItemAbilityManager().rebuildSubscriptions(player.getUniqueId(), artifact, reputation, "command-repair-state");
        plugin.getLoreEngine().refreshLore(player, artifact, reputation);
        player.getInventory().setItemInMainHand(renderArtifactItem(artifact, reputation));
        plugin.getArtifactManager().markDirty(player.getUniqueId());
        plugin.getArtifactManager().save(player.getUniqueId());
        plugin.getReputationManager().save(player.getUniqueId());
        sender.sendMessage("§aRebuilt held artifact derived state (lore, naming, projections) without identity mutation.");
        return true;
    }

    private boolean handleDebugProfile(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_DEBUG_PROFILE)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot debug-profile");
        if (player == null) return true;
        Artifact artifact = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (artifact == null) return true;
        ArtifactReputation reputation = plugin.getReputationManager().get(player.getUniqueId());
        var memoryProfile = new MemoryInfluenceResolver().profileFor(artifact.getMemory());
        var significance = new ArtifactSignificanceResolver().resolve(artifact);
        sender.sendMessage("§d=== Artifact Debug Profile ===");
        sender.sendMessage("§7identity: §fname=" + artifact.getDisplayName()
                + " §7storage=§f" + artifact.getArtifactStorageKey()
                + " §7shape=§f" + artifact.getConvergenceIdentityShape());
        sender.sendMessage("§7lineage trace: §flatent=" + artifact.getLatentLineage()
                + " §7conv=" + artifact.getConvergenceLineageTrace()
                + " §7species=" + artifact.getSpeciesId());
        sender.sendMessage("§7awakening/convergence: §f" + artifact.getAwakeningPath()
                + " §7/ §f" + artifact.getConvergencePath()
                + " §7variant=§f" + artifact.getConvergenceVariantId());
        sender.sendMessage("§7memory signature: §f" + artifact.getConvergenceMemorySignature()
                + " §7pressure=§f" + memoryProfile.pressure()
                + " §7snapshot=§f" + artifact.getMemory().snapshot());
        sender.sendMessage("§7state/posture: §fdrift=" + artifact.getDriftAlignment()
                + " §7instability=§f" + artifact.getCurrentInstabilityState()
                + " §7evolution=§f" + artifact.getEvolutionPath());
        sender.sendMessage("§7significance inputs: §fhistory=" + artifact.getHistoryScore()
                + " §7repTotal=§f" + reputation.getTotalScore()
                + " §7resolved=§f" + significance.format());
        return true;
    }

    private boolean handleGiveSpecific(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, PERMISSION_COMMAND_GIVE_SPECIFIC)) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /" + label + " give-specific <player> <archetype|family>");
            return true;
        }
        Player target = sender.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cInvalid player: " + args[1]);
            return true;
        }
        String selector = args[2].toLowerCase(java.util.Locale.ROOT);
        EquipmentArchetype constrainedArchetype = resolveConstrainedArchetype(selector, target.getUniqueId());
        if (constrainedArchetype == null) {
            sender.sendMessage("§cInvalid type '" + args[2] + "'. Use a valid equipment archetype id or family "
                    + java.util.Arrays.stream(AbilityFamily.values()).map(v -> v.name().toLowerCase(java.util.Locale.ROOT)).toList());
            return true;
        }
        Artifact artifact = plugin.getArtifactManager().recreateWithArchetype(
                target.getUniqueId(),
                constrainedArchetype,
                "command-give-specific:" + selector + ":" + constrainedArchetype.id());
        deliverGeneratedArtifact(sender, target, artifact);
        return true;
    }

    private boolean handleDumpHeld(CommandSender sender) {
        if (!hasPermission(sender, PERMISSION_COMMAND_DUMP_HELD)) {
            return true;
        }
        Player player = requirePlayer(sender, "/obtuseloot dump-held");
        if (player == null) return true;
        Artifact artifact = resolveHeldArtifact(player, player.getInventory().getItemInMainHand(), true);
        if (artifact == null) return true;
        String snapshot = "artifact_snapshot{owner=" + player.getUniqueId()
                + ", storageKey=" + artifact.getArtifactStorageKey()
                + ", name=" + artifact.getDisplayName()
                + ", item=" + artifact.getItemCategory()
                + ", archetypePath=" + artifact.getArchetypePath()
                + ", evolutionPath=" + artifact.getEvolutionPath()
                + ", awakeningPath=" + artifact.getAwakeningPath()
                + ", convergencePath=" + artifact.getConvergencePath()
                + ", lineage=" + artifact.getLatentLineage()
                + ", convergenceLineageTrace=" + artifact.getConvergenceLineageTrace()
                + ", identityBirthTimestamp=" + artifact.getIdentityBirthTimestamp()
                + ", persistenceOriginTimestamp=" + artifact.getPersistenceOriginTimestamp()
                + ", lastDriftTimestamp=" + artifact.getLastDriftTimestamp()
                + '}';
        plugin.getLogger().info("[Command] /obtuseloot dump-held " + snapshot);
        sender.sendMessage("§aStructured held artifact snapshot logged.");
        return true;
    }

    private void finalizeIdentityReplacement(Player player, Artifact previous, Artifact replacement, String reason) {
        ArtifactReputation reputation = plugin.getReputationManager().get(player.getUniqueId());
        if (previous != null) {
            plugin.getArtifactUsageTracker().transitionIdentity(previous, replacement);
            plugin.getLineageRegistry().recordIdentityTransition(previous, replacement, reason, replacement.getConvergencePath());
        } else {
            plugin.getLineageRegistry().assignLineage(replacement);
        }
        plugin.getItemAbilityManager().rebuildSubscriptions(player.getUniqueId(), replacement, reputation, reason);
        plugin.getLoreEngine().refreshLore(player, replacement, reputation);
        player.getInventory().setItemInMainHand(renderArtifactItem(replacement, reputation));
        plugin.getArtifactManager().markDirty(player.getUniqueId());
        plugin.getArtifactManager().save(player.getUniqueId());
        plugin.getReputationManager().save(player.getUniqueId());
    }

    private Artifact resolveHeldArtifact(Player player, ItemStack held, boolean notify) {
        if (held == null || held.getType() == Material.AIR) {
            if (notify) player.sendMessage("§cHold an artifact item first.");
            return null;
        }
        if (!plugin.getArtifactItemStorage().isArtifactItem(held)) {
            if (notify) player.sendMessage("§cHeld item is not an artifact.");
            return null;
        }
        Artifact artifact = plugin.getArtifactItemStorage().resolve(held, player.getUniqueId());
        if (artifact == null) {
            throw new IllegalStateException("Artifact storage key could not be resolved for held item.");
        }
        return artifact;
    }

    private EquipmentArchetype resolveEquipmentArchetype(ItemStack item) {
        if (item == null || item.getType() == null) return null;
        String id = item.getType().name().toLowerCase(java.util.Locale.ROOT);
        if (!EquipmentArchetype.isEquipment(id)) return null;
        return EquipmentArchetype.fromId(id);
    }

    private ItemStack renderArtifactItem(Artifact artifact, ArtifactReputation reputation) {
        Material material = Material.matchMaterial(artifact.getItemCategory().toUpperCase(java.util.Locale.ROOT));
        if (material == null || material == Material.AIR) {
            throw new IllegalStateException("Unable to render artifact item for category " + artifact.getItemCategory());
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Unable to render item meta for " + material);
        }
        meta.setDisplayName("§d" + artifact.getDisplayName());
        meta.setLore(plugin.getLoreEngine().buildLoreLines(artifact, reputation));
        item.setItemMeta(meta);
        plugin.getArtifactItemStorage().stampMinimalIdentity(item, artifact);
        return item;
    }

    private void deliverGeneratedArtifact(CommandSender sender, Player target, Artifact artifact) {
        ArtifactReputation reputation = plugin.getReputationManager().get(target.getUniqueId());
        plugin.getItemAbilityManager().rebuildSubscriptions(target.getUniqueId(), artifact, reputation, "command-give-specific");
        plugin.getLoreEngine().refreshLore(target, artifact, reputation);
        plugin.getLineageRegistry().assignLineage(artifact);
        plugin.getArtifactManager().markDirty(target.getUniqueId());
        plugin.getArtifactManager().save(target.getUniqueId());
        ItemStack item = renderArtifactItem(artifact, reputation);
        var leftovers = target.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
            sender.sendMessage("§eInventory full. Artifact dropped at " + target.getName() + "'s feet.");
            target.sendMessage("§eYour inventory was full; artifact dropped at your feet.");
            return;
        }
        sender.sendMessage("§aGave constrained artifact to " + target.getName() + ": §f" + artifact.getDisplayName()
                + " §7(" + artifact.getItemCategory() + ")");
    }

    private EquipmentArchetype resolveConstrainedArchetype(String selector, java.util.UUID ownerId) {
        if (EquipmentArchetype.isEquipment(selector)) {
            return EquipmentArchetype.fromId(selector);
        }
        AbilityFamily family;
        try {
            family = AbilityFamily.valueOf(selector.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        List<EquipmentArchetype> choices = archetypesForFamily(family);
        if (choices.isEmpty()) {
            throw new IllegalStateException("No constrained archetypes configured for family " + family.name().toLowerCase(java.util.Locale.ROOT));
        }
        long seed = plugin.getArtifactManager().rollSeed() ^ ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits();
        int index = (int) Math.floorMod(seed, choices.size());
        return choices.get(index);
    }

    static List<EquipmentArchetype> archetypesForFamily(AbilityFamily family) {
        return switch (family) {
            case PRECISION -> List.of(EquipmentArchetype.BOW, EquipmentArchetype.CROSSBOW, EquipmentArchetype.TRIDENT);
            case BRUTALITY -> List.of(EquipmentArchetype.NETHERITE_AXE, EquipmentArchetype.NETHERITE_SWORD, EquipmentArchetype.DIAMOND_AXE);
            case SURVIVAL -> List.of(EquipmentArchetype.NETHERITE_CHESTPLATE, EquipmentArchetype.TURTLE_HELMET, EquipmentArchetype.NETHERITE_HELMET);
            case MOBILITY -> List.of(EquipmentArchetype.ELYTRA, EquipmentArchetype.NETHERITE_BOOTS, EquipmentArchetype.DIAMOND_BOOTS);
            case CHAOS -> List.of(EquipmentArchetype.TRIDENT, EquipmentArchetype.GOLDEN_SWORD, EquipmentArchetype.GOLDEN_AXE);
            case CONSISTENCY -> List.of(EquipmentArchetype.NETHERITE_HELMET, EquipmentArchetype.NETHERITE_CHESTPLATE, EquipmentArchetype.NETHERITE_LEGGINGS);
        };
    }

    private Player requirePlayer(CommandSender sender, String usage) {
        if (sender instanceof Player player) {
            return player;
        }
        if (sender instanceof BlockCommandSender || !(sender instanceof Player)) {
            sender.sendMessage("§cPlayer-only command. Usage: " + usage);
        }
        return null;
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
