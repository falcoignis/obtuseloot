package obtuseloot.ecosystem;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class EnvironmentPressureEngine {
    private final Random random;
    private final List<EventTemplate> templates;
    private EnvironmentalEvent currentEvent;
    private int elapsedSeasons;

    public EnvironmentPressureEngine() {
        this(7331L);
    }

    public EnvironmentPressureEngine(long seed) {
        this.random = new Random(seed);
        this.templates = List.of(
                new EventTemplate("DriftStorm", List.of(
                        new FitnessLandscapeModifier(GenomeTrait.CHAOS_AFFINITY, 1.18D),
                        new FitnessLandscapeModifier(GenomeTrait.VOLATILITY, 1.15D),
                        new FitnessLandscapeModifier(GenomeTrait.STABILITY, 0.88D),
                        new FitnessLandscapeModifier(GenomeTrait.MUTATION_SENSITIVITY, 1.08D),
                        new FitnessLandscapeModifier(GenomeTrait.SURVIVAL_INSTINCT, 0.94D))),
                new EventTemplate("PrecisionAge", List.of(
                        new FitnessLandscapeModifier(GenomeTrait.PRECISION_AFFINITY, 1.20D),
                        new FitnessLandscapeModifier(GenomeTrait.MOBILITY_AFFINITY, 0.84D),
                        new FitnessLandscapeModifier(GenomeTrait.RESONANCE, 1.10D),
                        new FitnessLandscapeModifier(GenomeTrait.STABILITY, 1.06D))),
                new EventTemplate("SurvivalWinter", List.of(
                        new FitnessLandscapeModifier(GenomeTrait.SURVIVAL_INSTINCT, 1.25D),
                        new FitnessLandscapeModifier(GenomeTrait.CHAOS_AFFINITY, 0.88D),
                        new FitnessLandscapeModifier(GenomeTrait.STABILITY, 1.10D),
                        new FitnessLandscapeModifier(GenomeTrait.RESONANCE, 1.06D))),
                new EventTemplate("MobilityBloom", List.of(
                        new FitnessLandscapeModifier(GenomeTrait.MOBILITY_AFFINITY, 1.22D),
                        new FitnessLandscapeModifier(GenomeTrait.KINETIC_BIAS, 1.12D),
                        new FitnessLandscapeModifier(GenomeTrait.STABILITY, 0.94D),
                        new FitnessLandscapeModifier(GenomeTrait.SURVIVAL_INSTINCT, 1.08D),
                        new FitnessLandscapeModifier(GenomeTrait.RESONANCE, 1.05D)))
        );
        rotateEvent();
    }

    public void advanceSeason() {
        elapsedSeasons++;
        currentEvent.advanceSeason();
        if (currentEvent.expired()) {
            rotateEvent();
        }
    }

    public double multiplierFor(GenomeTrait trait) {
        return currentEvent.multiplierFor(trait);
    }

    public EnvironmentalEvent currentEvent() {
        return currentEvent;
    }

    public int elapsedSeasons() {
        return elapsedSeasons;
    }

    public Map<GenomeTrait, Double> currentModifiers() {
        EnumMap<GenomeTrait, Double> modifiers = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            modifiers.put(trait, multiplierFor(trait));
        }
        return modifiers;
    }

    private void rotateEvent() {
        EventTemplate template = templates.get(random.nextInt(templates.size()));
        int duration = random.nextInt(4) + 2;
        currentEvent = new EnvironmentalEvent(template.name(), duration, template.modifiers());
    }

    private record EventTemplate(String name, List<FitnessLandscapeModifier> modifiers) {
    }
}
