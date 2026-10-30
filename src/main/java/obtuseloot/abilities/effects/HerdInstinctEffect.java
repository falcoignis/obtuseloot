package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.ArrayList;
import java.util.Locale;

/** Reports only the intentionally targeted animal's current condition, never invented migration/danger. */
public final class HerdInstinctEffect extends MeasuredUtilityEffect {
    public HerdInstinctEffect() { super("herd-instinct", 4, 80); }
    @Override protected Report prepare(AbilityDefinition definition, AbilityEventContext context, Player player, double output) {
        if (!(context.liveEvent() instanceof PlayerInteractEntityEvent event)
                || !(event.getRightClicked() instanceof Animals animal) || animal.isDead() || !animal.isValid()
                || !animal.getWorld().getUID().equals(player.getWorld().getUID()) || !player.canSee(animal)) return null;
        var facts = new ArrayList<String>();
        double health = animal.getHealth();
        boolean adult = animal.isAdult(), breed = animal.canBreed();
        int love = animal.getLoveModeTicks();
        facts.add(animal.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        facts.add(String.format(Locale.ROOT, "health %.1f", health));
        facts.add(adult ? "adult" : "young; growth in " + Math.max(0L, -(long) animal.getAge()) + " ticks");
        facts.add("breeding ready " + breed);
        facts.add("love mode " + love + " ticks");
        var identities = new ArrayList<>(facts);
        identities.set(2, adult ? "adult" : "young"); identities.set(4, "love mode " + (love > 0));
        return information("Herd Instinct", facts, identities, output, "animal:" + animal.getUniqueId());
    }
}
