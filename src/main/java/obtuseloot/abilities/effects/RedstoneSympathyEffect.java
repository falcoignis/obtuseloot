package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.Locale;

/** Target-local block data only: no inventories, neighboring machines, chunk loads or circuit edits. */
public final class RedstoneSympathyEffect extends MeasuredUtilityEffect {
    public RedstoneSympathyEffect() { super("redstone-sympathy", 4, 80); }
    @Override protected Report prepare(AbilityDefinition definition, AbilityEventContext context, Player player, double output) {
        if (!(context.liveEvent() instanceof PlayerInteractEvent event) || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return null;
        var block = event.getClickedBlock();
        if (!block.getWorld().getUID().equals(player.getWorld().getUID())
                || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return null;
        var data = block.getBlockData();
        if (!(data instanceof AnaloguePowerable || data instanceof Powerable || data instanceof Switch
                || data instanceof Repeater || data instanceof org.bukkit.block.data.type.Comparator
                || data instanceof Piston || data instanceof Hopper)) return null;
        var facts = new ArrayList<String>();
        facts.add(block.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        if (data instanceof AnaloguePowerable power) facts.add("signal " + power.getPower() + "/" + power.getMaximumPower());
        if (data instanceof Powerable power) facts.add("powered " + power.isPowered());
        if (data instanceof Repeater repeater) {
            facts.add("delay " + repeater.getDelay() + " redstone ticks"); facts.add("locked " + repeater.isLocked());
        }
        if (data instanceof org.bukkit.block.data.type.Comparator comparator) facts.add("mode " + comparator.getMode().name().toLowerCase(Locale.ROOT));
        if (data instanceof Piston piston) facts.add("extended " + piston.isExtended());
        if (data instanceof Hopper hopper) facts.add("enabled " + hopper.isEnabled());
        if (data instanceof Directional facing) facts.add("facing " + facing.getFacing().name().toLowerCase(Locale.ROOT));
        if (data instanceof RedstoneWire wire) for (var face : wire.getAllowedFaces().stream().sorted().toList())
            facts.add(face.name().toLowerCase(Locale.ROOT) + " " + wire.getFace(face).name().toLowerCase(Locale.ROOT));
        return information("Redstone Sympathy", facts, output,
                "block:" + block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ());
    }
}
