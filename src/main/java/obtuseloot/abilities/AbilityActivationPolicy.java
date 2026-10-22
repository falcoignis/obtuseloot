package obtuseloot.abilities;

import org.bukkit.inventory.EquipmentSlot;
import java.util.Set;

public record AbilityActivationPolicy(Mode mode, Set<EquipmentSlot> slots) {
    public enum Mode { ACTION, REACTION, PASSIVE }
    public AbilityActivationPolicy { slots = Set.copyOf(slots); }
    public static final Set<EquipmentSlot> EQUIPMENT = Set.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public boolean accepts(EquipmentSlot slot, AbilityInput input) {
        if (!slots.contains(slot) || mode == Mode.PASSIVE) return false;
        return mode != Mode.ACTION || input.kind() == AbilityInput.Kind.SECONDARY_CLICK && input.hand() == slot;
    }
    public static AbilityActivationPolicy defaults(AbilityTrigger trigger) {
        return switch (trigger) {
            case ON_BLOCK_INSPECT, ON_ENTITY_INSPECT, ON_RITUAL_INTERACT, ON_SOCIAL_INTERACT ->
                    new AbilityActivationPolicy(Mode.ACTION, Set.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND));
            case ON_HIT, ON_KILL, ON_MULTI_KILL, ON_BOSS_KILL, ON_CHAIN_COMBAT ->
                    new AbilityActivationPolicy(Mode.REACTION, Set.of(EquipmentSlot.HAND));
            default -> new AbilityActivationPolicy(Mode.REACTION, EQUIPMENT);
        };
    }
}
