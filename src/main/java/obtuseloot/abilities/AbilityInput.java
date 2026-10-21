package obtuseloot.abilities;

import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

/** Physical input is separate from an ecological trigger or a measured outcome. */
public record AbilityInput(Kind kind, EquipmentSlot hand, Action action) {
    public enum Kind { EVENT, SECONDARY_CLICK, JUMP }
    public static AbilityInput jump() { return new AbilityInput(Kind.JUMP, null, null); }
    public static AbilityInput event() { return new AbilityInput(Kind.EVENT, null, null); }
    public static AbilityInput secondary(EquipmentSlot hand, Action action) {
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) throw new IllegalArgumentException("Input requires a hand");
        return new AbilityInput(Kind.SECONDARY_CLICK, hand, action);
    }
}
