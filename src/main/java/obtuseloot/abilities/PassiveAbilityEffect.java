package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import org.bukkit.entity.Player;
import java.util.function.Supplier;

/** Applying a passive is not an outcome. A lease owns only resources installed by its handler. */
@FunctionalInterface
public interface PassiveAbilityEffect {
    default void cleanupPlayer(Player player) { }
    Contribution contribution(Player player, Artifact artifact, AbilityDefinition definition);
    enum Stacking { STRONGEST, ADDITIVE }
    record Contribution(String stackingGroup, double strength, String revision, Stacking stacking, Supplier<Lease> apply) {
        public Contribution(String group, double strength, String revision, Supplier<Lease> apply) {
            this(group, strength, revision, Stacking.STRONGEST, apply);
        }
        public Contribution {
            if (stackingGroup == null || stackingGroup.isBlank() || !Double.isFinite(strength) || strength < 0
                    || revision == null || stacking == null || apply == null) throw new IllegalArgumentException("Invalid passive contribution");
        }
    }
    @FunctionalInterface interface Lease { void release(); }
}
