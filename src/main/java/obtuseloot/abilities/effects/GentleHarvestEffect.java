package obtuseloot.abilities.effects;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityEventContext;
import obtuseloot.abilities.AbilityExecutionResult;
import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.LiveAbilityEffect;
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
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class GentleHarvestEffect implements LiveAbilityEffect {
    private static final Map<Material, Material> PLANTING_ITEMS = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS, Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO, Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART);

    @Override
    public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        if (!(context.liveEvent() instanceof BlockDropItemEvent event) || event.isCancelled()
                || !Bukkit.isPrimaryThread() || !event.getPlayer().isOnline()
                || event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 0, "harvest-unavailable", null);
        }
        // No recovery of its own, but it shares the item's recovery from other abilities. Without the clock none applies.
        if (context.gameTick() >= 0 && context.gameTick() < context.artifact().abilitiesReadyAt()) {
            return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                    context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(), AbilityExecutionStatus.SUPPRESSED,
                    AbilityOutcomeType.CROP_REPLANT, false, "ability-recovering", null);
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockState harvested = event.getBlockState();
        Material seed = PLANTING_ITEMS.get(harvested.getType());
        if (!player.getUniqueId().equals(context.artifact().getOwnerId())
                || !player.getWorld().equals(block.getWorld()) || block.getType() != Material.AIR
                || seed == null || !(harvested.getBlockData() instanceof Ageable crop)
                || crop.getAge() != crop.getMaximumAge()) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 0, "no-mature-crop", null);
        }
        Block soil = block.getRelative(BlockFace.DOWN);
        Material requiredSoil = harvested.getType() == Material.NETHER_WART ? Material.SOUL_SAND : Material.FARMLAND;
        if (soil.getType() != requiredSoil || spawnProtected(player, block)) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 0, "planting-denied", null);
        }
        Item payment = event.getItems().stream().filter(item -> usableSeed(item, seed)).findFirst().orElse(null);
        if (payment == null) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 0, "no-planting-item", null);
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
                return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 0, "planting-denied", null);
            }
            ItemStack remaining = payment.getItemStack().clone();
            if (remaining.getAmount() == 1) {
                payment.remove();
            } else {
                remaining.setAmount(remaining.getAmount() - 1);
                payment.setItemStack(remaining);
            }
            committed = true;
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.CROP_REPLANT, 1, null, "Gentle Harvest: replanted one crop using one planting item.")
                    .withObservationKey(block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY()
                            + ":" + block.getZ() + ":" + block.getWorld().getGameTime());
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

}
