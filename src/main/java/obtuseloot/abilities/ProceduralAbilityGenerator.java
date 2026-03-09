package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ProceduralAbilityGenerator {
    private final AbilityRegistry registry;

    public ProceduralAbilityGenerator(AbilityRegistry registry) {
        this.registry = registry;
    }

    public AbilityProfile generate(Artifact artifact, int evolutionStage, ArtifactMemoryProfile memoryProfile) {
        List<AbilityFamily> ranked = new ArrayList<>(List.of(AbilityFamily.values()));
        ranked.sort(Comparator.comparingDouble((AbilityFamily f) -> -score(artifact, f, memoryProfile)));
        long seed = artifact.getArtifactSeed() ^ artifact.getArchetypePath().hashCode() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure();
        Random random = new Random(seed);

        List<AbilityDefinition> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(2, ranked.size()); i++) {
            List<AbilityTemplate> pool = registry.byFamily(ranked.get(i));
            if (pool.isEmpty()) {
                continue;
            }
            AbilityTemplate t = pool.get(random.nextInt(pool.size()));
            picked.add(new AbilityDefinition(
                    t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.fusionVariant(), t.memoryVariant(), t.supportModifiers(),
                    List.of(new AbilityEffect(t.effectPattern(), AbilityEffectType.TRIGGERED_BEHAVIOR, 0.015D)),
                    "Core: " + t.effectPattern(),
                    "Evolution: " + t.evolutionVariant(),
                    "Drift: " + t.driftVariant(),
                    "Awakening/Fusion: " + t.awakeningVariant() + " / " + t.fusionVariant(),
                    "Memory: " + t.memoryVariant()));
        }
        return new AbilityProfile("procedural-" + ranked.get(0).name().toLowerCase() + "-s" + evolutionStage, picked);
    }

    private double score(Artifact artifact, AbilityFamily family, ArtifactMemoryProfile memoryProfile) {
        return switch (family) {
            case PRECISION -> artifact.getSeedPrecisionAffinity() + artifact.getDriftBias("precision") + memoryProfile.disciplineWeight();
            case BRUTALITY -> artifact.getSeedBrutalityAffinity() + artifact.getDriftBias("brutality");
            case SURVIVAL -> artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival");
            case MOBILITY -> artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility");
            case CHAOS -> artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos") + memoryProfile.chaosWeight();
            case CONSISTENCY -> artifact.getSeedConsistencyAffinity() + artifact.getDriftBias("consistency") + memoryProfile.disciplineWeight();
        };
    }
}
