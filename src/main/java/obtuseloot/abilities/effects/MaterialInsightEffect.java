package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.GenomeRevision;
import obtuseloot.config.RuntimeSettings;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;

/** Reports actual applicable properties; output is measured delivery, not proof of player learning. */
public final class MaterialInsightEffect implements LiveAbilityEffect {
    @Override public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        return execute(definition, context, stage, AbilityPerformance.FIXED);
    }
    @Override public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage,
                                                   AbilityPerformance performance) {
        if (!context.liveExecution() || !(context.liveEvent() instanceof PlayerInteractEvent event)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || (context.activation() == null ? event.getHand() != EquipmentSlot.HAND : event.getHand() != context.activation().slot())
                || event.getClickedBlock() == null || event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY
                || !event.getPlayer().isOnline() || !Bukkit.isPrimaryThread())
            return unavailable(definition, context, "inspection-unavailable");
        Player player = event.getPlayer();
        var artifact = context.artifact();
        if (!player.getUniqueId().equals(artifact.getOwnerId())) return unavailable(definition, context, "holder-mismatch");
        if (context.gameTick() < 0) return suppressed(definition, context, "game-clock-unavailable");
        if (!Objects.equals(context.genomeRevision(), GenomeRevision.capture(artifact)))
            return suppressed(definition, context, "genome-changed-before-inspection");
        long previous = artifact.abilityReadyAt(definition.id());
        if (context.gameTick() < artifact.abilitiesReadyAt()) return suppressed(definition, context, "ability-recovering");
        if (!artifact.beginAbilityExecution(definition.id())) return suppressed(definition, context, "inspection-in-progress");
        try {
            var settings = RuntimeSettings.get();
            long budget = Math.max(1L, (long) Math.floor(performance.output(settings.materialInsightBaseFacts())));
            long recovery = performance.recoveryTicks(settings.materialInsightRecoveryTicks());
            long readyAt = Math.addExact(context.gameTick(), recovery);
            Block block = event.getClickedBlock();
            List<String> facts = facts(block, budget);
            String report = "Material Insight: " + String.join(" | ", facts);
            var result = AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, facts.size(), null, report)
                    .withObservationKey(block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ() + ":" + report)
                    .withPerformance(new AbilityPerformanceMeasurement(facts.size(), recovery));
            // Reserve before the external delivery call, including zero-recovery reentrancy.
            artifact.setAbilityReadyAt(definition.id(), readyAt);
            try { player.sendMessage(report); }
            catch (RuntimeException | Error failure) {
                if (artifact.abilityReadyAt(definition.id()) == readyAt) artifact.setAbilityReadyAt(definition.id(), previous);
                throw failure;
            }
            return result;
        } finally { artifact.endAbilityExecution(definition.id()); }
    }

    private List<String> facts(Block block, long budget) {
        var facts = new ArrayList<String>();
        facts.add(label(block.getType().name()));
        if (facts.size() < budget) facts.add("light " + block.getLightLevel());
        if (facts.size() >= budget) return facts;
        var data = block.getBlockData();
        if (data instanceof Ageable crop) {
            facts.add("growth " + crop.getAge() + "/" + crop.getMaximumAge());
            if (facts.size() < budget && block.getY() > block.getWorld().getMinHeight()) {
                var soil = block.getRelative(BlockFace.DOWN);
                facts.add("soil " + label(soil.getType().name()));
                if (facts.size() < budget && soil.getBlockData() instanceof Farmland farmland)
                    facts.add("moisture " + farmland.getMoisture() + "/" + farmland.getMaximumMoisture());
            }
        }
        if (facts.size() < budget && data instanceof Farmland soil) facts.add("moisture " + soil.getMoisture() + "/" + soil.getMaximumMoisture());
        if (facts.size() < budget && data instanceof Waterlogged wet) facts.add("waterlogged " + wet.isWaterlogged());
        if (facts.size() < budget && data instanceof Powerable powered) facts.add("powered " + powered.isPowered());
        if (facts.size() < budget && data instanceof Directional facing) facts.add("facing " + label(facing.getFacing().name()));
        if (facts.size() < budget && data instanceof Openable open) facts.add("open " + open.isOpen());
        return List.copyOf(facts);
    }
    private String label(String name) { return name.toLowerCase(Locale.ROOT).replace('_', ' '); }
    private AbilityExecutionResult unavailable(AbilityDefinition definition, AbilityEventContext context, String reason) {
        return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, 0, reason, null);
    }
    private AbilityExecutionResult suppressed(AbilityDefinition definition, AbilityEventContext context, String reason) {
        return new AbilityExecutionResult(definition.id(), definition.mechanic(), context.trigger(),
                context.artifact().getArtifactStorageKey(), context.artifact().getOwnerId(), AbilityExecutionStatus.SUPPRESSED,
                AbilityOutcomeType.INFORMATION, false, reason, null);
    }
}
