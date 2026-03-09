package obtuseloot.ecosystem;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EnvironmentalEvent {
    private final String name;
    private int remainingSeasons;
    private final EnumMap<GenomeTrait, Double> traitMultipliers;

    public EnvironmentalEvent(String name, int remainingSeasons, List<FitnessLandscapeModifier> modifiers) {
        this.name = name;
        this.remainingSeasons = Math.max(1, remainingSeasons);
        this.traitMultipliers = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            traitMultipliers.put(trait, 1.0D);
        }
        for (FitnessLandscapeModifier modifier : modifiers) {
            traitMultipliers.put(modifier.trait(), modifier.multiplier());
        }
    }

    public String name() {
        return name;
    }

    public int remainingSeasons() {
        return remainingSeasons;
    }

    public double multiplierFor(GenomeTrait trait) {
        return traitMultipliers.getOrDefault(trait, 1.0D);
    }

    public Map<GenomeTrait, Double> traitMultipliers() {
        return Map.copyOf(traitMultipliers);
    }

    public void advanceSeason() {
        remainingSeasons = Math.max(0, remainingSeasons - 1);
    }

    public boolean expired() {
        return remainingSeasons == 0;
    }
}
