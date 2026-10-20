package obtuseloot.abilities;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

public final class AbilityExecutor {
    private static final Map<Material, Material> PLANTING_ITEMS = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS, Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO, Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART);

    public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        if (definition.trigger() != context.trigger()) {
            return new AbilityExecutionResult(
                    definition.id(),
                    definition.mechanic(),
                    context.trigger(),
                    context.artifact().getArtifactStorageKey(),
                    context.artifact().getOwnerId(),
                    AbilityExecutionStatus.SUPPRESSED,
                    AbilityOutcomeType.FLAVOR_ONLY,
                    false,
                    "trigger-mismatch",
                    null
            );
        }

        if (!Double.isFinite(context.value()) || context.value() < 0.0D) {
            return new AbilityExecutionResult(
                    definition.id(),
                    definition.mechanic(),
                    context.trigger(),
                    context.artifact().getArtifactStorageKey(),
                    context.artifact().getOwnerId(),
                    AbilityExecutionStatus.FAILED,
                    resolveOutcomeType(definition.mechanic()),
                    false,
                    "invalid-context-value",
                    definition.name() + " -> execution failed"
            );
        }

        // These templates require a real event and observation, including in offline callers.
        if (definition.id().equals("survival.gentle_harvest")) {
            if (definition.mechanic() != AbilityMechanic.HARVEST_RELAY || definition.trigger() != AbilityTrigger.ON_BLOCK_HARVEST) {
                return observed(definition, context, 0, "effect-variant-not-implemented", null);
            }
            return harvest(definition, context);
        }
        if (definition.id().equals("precision.material_insight")) {
            if (definition.mechanic() != AbilityMechanic.INSIGHT_REVEAL || definition.trigger() != AbilityTrigger.ON_BLOCK_INSPECT) {
                return observed(definition, context, 0, "effect-variant-not-implemented", null);
            }
            return inspect(definition, context);
        }
        // A live event must never turn an unimplemented template's label into success.
        if (context.liveExecution()) {
            return observed(definition, context, 0, "effect-not-implemented", null);
        }

        AbilityOutcomeType outcomeType = resolveOutcomeType(definition.mechanic());
        boolean meaningful = isMeaningful(definition, context, outcomeType);
        AbilityExecutionStatus status = meaningful ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP;
        return new AbilityExecutionResult(
                definition.id(),
                definition.mechanic(),
                context.trigger(),
                context.artifact().getArtifactStorageKey(),
                context.artifact().getOwnerId(),
                status,
                outcomeType,
                meaningful,
                null,
                definition.name() + " -> " + definition.stageDescription(stage)
        );
    }

    private AbilityExecutionResult inspect(AbilityDefinition definition, AbilityEventContext context) {
        if (!(context.liveEvent() instanceof PlayerInteractEvent event)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || event.useInteractedBlock() == Event.Result.DENY
                || !event.getPlayer().isOnline() || !Bukkit.isPrimaryThread()) {
            return observed(definition, context, 0, "inspection-unavailable", null);
        }
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(context.artifact().getOwnerId())) {
            return observed(definition, context, 0, "holder-mismatch", null);
        }
        Block block = event.getClickedBlock();
        String report = "Material Insight: " + block.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " | light " + block.getLightLevel();
        if (block.getBlockData() instanceof Ageable crop) {
            report += " | growth " + crop.getAge() + "/" + crop.getMaximumAge();
        }
        player.sendMessage(report);
        return observed(definition, context, 1, null, report);
    }

    private AbilityExecutionResult harvest(AbilityDefinition definition, AbilityEventContext context) {
        if (!(context.liveEvent() instanceof BlockDropItemEvent event) || event.isCancelled()
                || !Bukkit.isPrimaryThread() || !event.getPlayer().isOnline()
                || event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return observed(definition, context, 0, "harvest-unavailable", null);
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockState harvested = event.getBlockState();
        Material seed = PLANTING_ITEMS.get(harvested.getType());
        if (!player.getUniqueId().equals(context.artifact().getOwnerId())
                || !player.getWorld().equals(block.getWorld()) || block.getType() != Material.AIR
                || seed == null || !(harvested.getBlockData() instanceof Ageable crop)
                || crop.getAge() != crop.getMaximumAge()) {
            return observed(definition, context, 0, "no-mature-crop", null);
        }
        Block soil = block.getRelative(BlockFace.DOWN);
        Material requiredSoil = harvested.getType() == Material.NETHER_WART ? Material.SOUL_SAND : Material.FARMLAND;
        if (soil.getType() != requiredSoil || spawnProtected(player, block)) {
            return observed(definition, context, 0, "planting-denied", null);
        }
        Item payment = event.getItems().stream().filter(item -> usableSeed(item, seed)).findFirst().orElse(null);
        if (payment == null) {
            return observed(definition, context, 0, "no-planting-item", null);
        }

        BlockState replaced = block.getState();
        Ageable planted = (Ageable) crop.clone();
        planted.setAge(0);
        boolean committed = false;
        try {
            block.setBlockData(planted, false);
            // As with normal placement, protection listeners see the provisional crop and air snapshot.
            ItemStack plantingItem = payment.getItemStack().clone();
            plantingItem.setAmount(1);
            BlockPlaceEvent placement = new BlockPlaceEvent(block, replaced, soil,
                    plantingItem, player, true, EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(placement);
            if (placement.isCancelled() || !placement.canBuild() || event.isCancelled()
                    || !block.getBlockData().matches(planted) || soil.getType() != requiredSoil
                    || !event.getItems().contains(payment) || !usableSeed(payment, seed)) {
                return observed(definition, context, 0, "planting-denied", null);
            }
            ItemStack remaining = payment.getItemStack().clone();
            if (remaining.getAmount() == 1) {
                payment.remove();
            } else {
                remaining.setAmount(remaining.getAmount() - 1);
                payment.setItemStack(remaining);
            }
            committed = true;
            return observed(definition, context, 1, null, "Gentle Harvest: replanted one crop using one planting item.");
        } finally {
            // Do not overwrite an unrelated edit a protection listener made during the callback.
            if (!committed && block.getBlockData().matches(planted)) {
                block.setBlockData(replaced.getBlockData(), false);
            }
        }
    }

    private boolean usableSeed(Item item, Material seed) {
        return item.isValid() && !item.isDead() && item.getItemStack().getType() == seed
                && item.getItemStack().getAmount() > 0 && !item.getItemStack().hasItemMeta();
    }

    private boolean spawnProtected(Player player, Block block) {
        // Plugin-fired placement events do not run vanilla's spawn-protection check.
        // Conservatively protect spawn in normal worlds; claim plugins handle their own events below.
        int radius = Bukkit.getSpawnRadius();
        if (player.isOp() || radius <= 0 || block.getWorld().getEnvironment() != World.Environment.NORMAL) return false;
        var spawn = block.getWorld().getSpawnLocation();
        return Math.max(Math.abs((long) block.getX() - spawn.getBlockX()),
                Math.abs((long) block.getZ() - spawn.getBlockZ())) <= radius;
    }

    private AbilityExecutionResult observed(AbilityDefinition definition, AbilityEventContext context,
                                            double amount, String reason, String text) {
        return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(),
                amount > 0 ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP,
                resolveOutcomeType(definition.mechanic()), amount > 0, reason, text, amount);
    }

    private AbilityOutcomeType resolveOutcomeType(AbilityMechanic mechanic) {
        return switch (mechanic) {
            case HARVEST_RELAY, FORAGING_MEMORY, FORAGER_MEMORY, CLUSTER_INTUITION -> AbilityOutcomeType.CROP_REPLANT;
            case NAVIGATION_ANCHOR, ECOLOGICAL_PATHING, TRAIL_SENSE, CARTOGRAPHIC_ECHO, CARTOGRAPHERS_ECHO, TERRAIN_ADAPTATION -> AbilityOutcomeType.NAVIGATION_HINT;
            case SENSE_PING, BIOME_RESONANCE, WEATHER_ATTUNEMENT, STRUCTURE_ATTUNEMENT -> AbilityOutcomeType.STRUCTURE_SENSE;
            case MEMORY_ECHO, WITNESS_IMPRINT, ALTAR_SIGNAL_BOOST, PATTERN_RESONANCE -> AbilityOutcomeType.MEMORY_MARK;
            case SOCIAL_ATTUNEMENT, RITUAL_CHANNEL, RITUAL_STABILIZATION, COLLECTIVE_RELAY, TRADE_SCENT -> AbilityOutcomeType.SOCIAL_SIGNAL;
            case INSIGHT_REVEAL, RESOURCE_ECOLOGY_SCAN, TEMPORAL_SPECIALIZATION -> AbilityOutcomeType.INFORMATION;
            default -> AbilityOutcomeType.FLAVOR_ONLY;
        };
    }

    private boolean isMeaningful(AbilityDefinition definition,
                                 AbilityEventContext context,
                                 AbilityOutcomeType outcomeType) {
        if (outcomeType == AbilityOutcomeType.FLAVOR_ONLY) {
            return false;
        }
        return switch (definition.mechanic()) {
            case TRAIL_SENSE -> context.runtimeContext() != null && context.runtimeContext().chunkAware()
                    && context.value() >= 0.45D;
            case FORAGER_MEMORY -> context.value() >= 0.55D;
            case PATTERN_RESONANCE -> context.value() >= 0.65D;
            case WITNESS_IMPRINT -> context.value() >= 0.60D;
            case CARTOGRAPHERS_ECHO -> context.value() >= 0.60D;
            default -> true;
        };
    }
}
