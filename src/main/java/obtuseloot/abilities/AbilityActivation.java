package obtuseloot.abilities;

import org.bukkit.inventory.EquipmentSlot;
import java.util.Set;

/** Permissions and duplicate-contributor selection are resolved on the server thread at dispatch. */
public record AbilityActivation(EquipmentSlot slot, AbilityInput input, Set<String> allowedAbilities) {
    public AbilityActivation { allowedAbilities = Set.copyOf(allowedAbilities); }
    public boolean allows(AbilityRegistration registration) {
        return allowedAbilities.contains(registration.template().id()) && registration.activation().accepts(slot, input);
    }
}
