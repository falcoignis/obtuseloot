package obtuseloot.abilities;

import java.util.Objects;

/** Owns metadata and its optional implementation without putting handler identity into template hashes. */
public record AbilityRegistration(AbilityTemplate template, LiveAbilityEffect liveEffect,
                                  AbilityActivationPolicy activation, PassiveAbilityEffect passiveEffect,
                                  AbilityPerformanceModel performanceModel) {
    public AbilityRegistration {
        Objects.requireNonNull(template);
        Objects.requireNonNull(activation);
        Objects.requireNonNull(performanceModel);
    }

    public AbilityRegistration(AbilityTemplate template, LiveAbilityEffect liveEffect,
                               AbilityActivationPolicy activation, PassiveAbilityEffect passiveEffect) {
        this(template, liveEffect, activation, passiveEffect, AbilityPerformanceModel.FIXED);
    }

    public AbilityRegistration(AbilityTemplate template, LiveAbilityEffect liveEffect) {
        this(template, liveEffect, AbilityActivationPolicy.defaults(template.trigger()), null);
    }

    public AbilityRegistration withLiveEffect(LiveAbilityEffect effect) {
        return new AbilityRegistration(template, Objects.requireNonNull(effect), activation, null, performanceModel);
    }

    public AbilityRegistration withPassiveEffect(java.util.Set<org.bukkit.inventory.EquipmentSlot> slots, PassiveAbilityEffect effect) {
        return new AbilityRegistration(template, null,
                new AbilityActivationPolicy(AbilityActivationPolicy.Mode.PASSIVE, slots), Objects.requireNonNull(effect), performanceModel);
    }
    public AbilityRegistration withPerformanceModel(AbilityPerformanceModel model) {
        return new AbilityRegistration(template, liveEffect, activation, passiveEffect, model);
    }
    public boolean implemented() { return liveEffect != null || passiveEffect != null; }
    public String permission() { return "obtuseloot.ability." + template.id(); }

    public boolean supports(AbilityDefinition definition) {
        return implemented() && template.id().equals(definition.id())
                && template.trigger() == definition.trigger() && template.mechanic() == definition.mechanic()
                && template.family() == definition.family();
    }
}
