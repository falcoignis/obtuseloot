package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;

import java.util.ArrayList;
import java.util.List;

public final class TraitInterferenceFieldMatrix {
    public TraitInterferenceSnapshot evaluate(AbilityTemplate template, ArtifactGenome genome) {
        List<String> effects = new ArrayList<>();
        double modifier = 0.0D;

        double triggerMechanic = triggerMechanicModifier(template.trigger(), template.mechanic());
        modifier += triggerMechanic;
        append(effects, "trigger×mechanic", triggerMechanic);

        double environmentSignal = (genome.trait(GenomeTrait.SURVIVAL_INSTINCT) + genome.trait(GenomeTrait.MOBILITY_AFFINITY) + genome.trait(GenomeTrait.CHAOS_AFFINITY)) / 3.0D;
        double mechanicEnvironment = (template.mechanic() == AbilityMechanic.MOVEMENT_ECHO ? 0.08D : template.mechanic() == AbilityMechanic.DEFENSIVE_THRESHOLD ? 0.06D : -0.02D) * centered(environmentSignal);
        modifier += mechanicEnvironment;
        append(effects, "mechanic×environment", mechanicEnvironment);

        double gateTrigger = (template.trigger() == AbilityTrigger.ON_LOW_HEALTH || template.trigger() == AbilityTrigger.ON_MEMORY_EVENT) ? (genome.trait(GenomeTrait.STABILITY) - 0.5D) * 0.10D : 0.0D;
        modifier += gateTrigger;
        append(effects, "gate×trigger", gateTrigger);

        double memoryMechanic = template.mechanic() == AbilityMechanic.MEMORY_ECHO ? centered(genome.trait(GenomeTrait.RESONANCE)) * 0.12D : 0.0D;
        modifier += memoryMechanic;
        append(effects, "memory×mechanic", memoryMechanic);

        double branchEnvironment = centered(genome.trait(GenomeTrait.MOBILITY_AFFINITY)) * (template.family() == AbilityFamily.MOBILITY ? 0.09D : 0.02D);
        modifier += branchEnvironment;
        append(effects, "branch tendency×environment affinity", branchEnvironment);

        double roleSignal = template.supportModifiers().isEmpty() ? -0.03D : 0.05D;
        double persistenceRole = centered(genome.trait(GenomeTrait.STABILITY)) * roleSignal;
        modifier += persistenceRole;
        append(effects, "persistence style×support/combat role", persistenceRole);

        double bounded = clamp(modifier, -0.18D, 0.18D);
        double latentBias = clamp(0.5D + (bounded * 1.5D), 0.30D, 0.70D);
        double mutationBias = clamp(0.5D + (bounded * 1.2D), 0.30D, 0.70D);
        return new TraitInterferenceSnapshot(bounded, latentBias, mutationBias, List.copyOf(effects));
    }

    private double triggerMechanicModifier(AbilityTrigger trigger, AbilityMechanic mechanic) {
        if ((trigger == AbilityTrigger.ON_MEMORY_EVENT || trigger == AbilityTrigger.ON_AWAKENING)
                && (mechanic == AbilityMechanic.MEMORY_ECHO || mechanic == AbilityMechanic.REVENANT_TRIGGER)) {
            return 0.10D;
        }
        if ((trigger == AbilityTrigger.ON_MOVEMENT || trigger == AbilityTrigger.ON_REPOSITION)
                && mechanic == AbilityMechanic.MOVEMENT_ECHO) {
            return 0.08D;
        }
        if (trigger == AbilityTrigger.ON_LOW_HEALTH && mechanic == AbilityMechanic.UNSTABLE_DETONATION) {
            return -0.06D;
        }
        return 0.0D;
    }

    private void append(List<String> effects, String label, double value) {
        if (Math.abs(value) >= 0.035D) {
            effects.add(label + "=" + String.format(java.util.Locale.ROOT, "%.3f", value));
        }
    }

    private double centered(double value) {
        return clamp(value, 0.0D, 1.0D) - 0.5D;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
