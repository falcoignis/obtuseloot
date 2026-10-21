package obtuseloot.abilities.effects;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityEventContext;
import obtuseloot.abilities.AbilityExecutionResult;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.LiveAbilityEffect;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;

public final class MaterialInsightEffect implements LiveAbilityEffect {
    @Override
    public AbilityExecutionResult execute(AbilityDefinition definition, AbilityEventContext context, int stage) {
        if (!(context.liveEvent() instanceof PlayerInteractEvent event)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || (context.activation() == null ? event.getHand() != EquipmentSlot.HAND : event.getHand() != context.activation().slot())
                || event.getClickedBlock() == null || event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY
                || !event.getPlayer().isOnline() || !Bukkit.isPrimaryThread()) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, 0, "inspection-unavailable", null);
        }
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(context.artifact().getOwnerId())) {
            return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, 0, "holder-mismatch", null);
        }
        Block block = event.getClickedBlock();
        String report = "Material Insight: " + block.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                + " | light " + block.getLightLevel();
        if (block.getBlockData() instanceof Ageable crop) {
            report += " | growth " + crop.getAge() + "/" + crop.getMaximumAge();
        }
        player.sendMessage(report);
        return AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, 1, null, report)
                .withObservationKey(block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY()
                        + ":" + block.getZ() + ":" + report);
    }

}
