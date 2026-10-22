package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactStorageKeys;
import obtuseloot.config.RuntimeSettings;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/** Transient additive-base modifiers; never overwrites base speed, flight or other plugins' effects. */
public final class WalkingSpeedEffect implements PassiveAbilityEffect {
    private final java.util.function.Function<Player, org.bukkit.attribute.AttributeInstance> attributes;
    public WalkingSpeedEffect() {
        this(player -> player.getAttribute(Attribute.MOVEMENT_SPEED));
    }
    /** Allows controlled attribute fixtures without booting the server registry. */
    public WalkingSpeedEffect(java.util.function.Function<Player, org.bukkit.attribute.AttributeInstance> attributes) {
        this.attributes = java.util.Objects.requireNonNull(attributes);
    }
    @Override public Contribution contribution(Player player, Artifact artifact, AbilityDefinition definition) {
        var settings = RuntimeSettings.get();
        double bonus = settings.passiveSpeedBonus();
        if (bonus == 0 || !player.isOnline() || player.isDead() || player.isFlying() || player.isGliding()
                || player.isInsideVehicle() || player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return null;
        var attribute = attributes.apply(player);
        if (attribute == null) return null;
        var key = new NamespacedKey("obtuseloot", "passive_speed/" + ArtifactStorageKeys.itemId(artifact.getArtifactStorageKey()));
        var modifier = new AttributeModifier(key, bonus, AttributeModifier.Operation.ADD_SCALAR);
        return new Contribution("walking-speed", bonus, artifact.getArtifactSeed() + ":" + bonus + ":" + settings.passiveSpeedStacking(),
                settings.passiveSpeedStacking(), () -> {
                    // Recover only our own key after an interrupted plugin lifecycle.
                    attribute.removeModifier(key);
                    attribute.addTransientModifier(modifier);
                    return () -> attribute.removeModifier(key);
                });
    }
    @Override public void cleanupPlayer(Player player) {
        var attribute = attributes.apply(player);
        if (attribute == null) return;
        for (var modifier : java.util.List.copyOf(attribute.getModifiers())) {
            var key = modifier.getKey();
            if (key.getNamespace().equals("obtuseloot") && key.getKey().startsWith("passive_speed/")) attribute.removeModifier(modifier);
        }
    }
}
