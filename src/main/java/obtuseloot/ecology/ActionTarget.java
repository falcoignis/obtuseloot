package obtuseloot.ecology;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Locale;

/** The exact block material an action was measured on; no grouping or adjacency between materials. */
public final class ActionTarget {
    public static final String NONE = "none";
    private ActionTarget() { }

    /** Read when the observation is recorded, after the effect ran; a broken block is read from its captured state. */
    public static String of(Event event) {
        if (event instanceof PlayerInteractEvent interaction && interaction.getClickedBlock() != null)
            return key(interaction.getClickedBlock().getType().name());
        if (event instanceof BlockDropItemEvent drop) return key(drop.getBlockState().getType().name());
        if (event instanceof BlockEvent block) return key(block.getBlock().getType().name());
        return NONE;
    }
    public static boolean valid(String target) { return target != null && target.matches("[a-z0-9_]+"); }
    private static String key(String material) { return material.toLowerCase(Locale.ROOT); }
}
