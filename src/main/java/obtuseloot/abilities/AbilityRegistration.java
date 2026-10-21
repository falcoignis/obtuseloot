package obtuseloot.abilities;

import java.util.Objects;

/** Owns metadata and its optional implementation without putting handler identity into template hashes. */
public record AbilityRegistration(AbilityTemplate template, LiveAbilityEffect liveEffect) {
    public AbilityRegistration {
        Objects.requireNonNull(template);
    }

    public AbilityRegistration withLiveEffect(LiveAbilityEffect effect) {
        return new AbilityRegistration(template, Objects.requireNonNull(effect));
    }

    public boolean supports(AbilityDefinition definition) {
        return liveEffect != null && template.id().equals(definition.id())
                && template.trigger() == definition.trigger() && template.mechanic() == definition.mechanic()
                && template.family() == definition.family();
    }
}
