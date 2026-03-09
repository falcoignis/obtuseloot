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
        ranked.sort(Comparator.comparingDouble((AbilityFamily f) -> -scoreFamily(artifact, f, memoryProfile)));
        long seed = artifact.getArtifactSeed() ^ artifact.getArchetypePath().hashCode() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode() ^ memoryProfile.pressure();
        Random random = new Random(seed);

        List<AbilityDefinition> picked = new ArrayList<>();
        int picks = evolutionStage >= 4 ? 3 : 2;
        for (int i = 0; i < Math.min(picks, ranked.size()); i++) {
            List<AbilityTemplate> pool = registry.byFamily(ranked.get(i));
            if (pool.isEmpty()) {
                continue;
            }
            AbilityTemplate t = pickTemplate(pool, artifact, memoryProfile, evolutionStage, random);
            picked.add(fromTemplate(t, ranked.get(i), evolutionStage));
        }

        if (evolutionStage >= 5 && ("paradox".equalsIgnoreCase(artifact.getDriftAlignment()) || memoryProfile.chaosWeight() > 2.5D)) {
            AbilityTemplate paradox = registry.templates().stream().filter(t -> t.id().contains("chaos.paradox")).findFirst().orElse(null);
            if (paradox != null) {
                picked.add(fromTemplate(paradox, AbilityFamily.CHAOS, evolutionStage));
            }
        }

        return new AbilityProfile("procedural-" + ranked.get(0).name().toLowerCase() + "-s" + evolutionStage, picked);
    }

    private AbilityDefinition fromTemplate(AbilityTemplate t, AbilityFamily family, int stage) {
        return new AbilityDefinition(
                t.id(), t.name(), family, t.trigger(), t.mechanic(), t.effectPattern(), t.evolutionVariant(), t.driftVariant(), t.awakeningVariant(), t.fusionVariant(), t.memoryVariant(), t.supportModifiers(),
                List.of(new AbilityEffect(t.effectPattern(), AbilityEffectType.TRIGGERED_BEHAVIOR, 0.015D + (stage * 0.002D))),
                "Template: " + t.id() + " | Core: " + t.effectPattern(),
                "Evolution: " + t.evolutionVariant(),
                "Drift: " + t.driftVariant(),
                "Awakening/Fusion: " + t.awakeningVariant() + " / " + t.fusionVariant(),
                "Memory: " + t.memoryVariant());
    }

    private AbilityTemplate pickTemplate(List<AbilityTemplate> pool, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, Random random) {
        double total = 0.0D;
        double[] scores = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            scores[i] = Math.max(0.01D, scoreTemplate(pool.get(i), artifact, memoryProfile, stage));
            total += scores[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= scores[i];
            if (roll <= 0) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }

    private double scoreTemplate(AbilityTemplate template, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage) {
        double score = 1.0D;
        if (template.trigger() == AbilityTrigger.ON_MEMORY_EVENT) {
            score += memoryProfile.pressure() * 0.08D;
        }
        if (template.trigger() == AbilityTrigger.ON_BOSS_KILL) {
            score += memoryProfile.bossWeight() * 0.15D;
        }
        if (template.mechanic() == AbilityMechanic.UNSTABLE_DETONATION) {
            score += memoryProfile.chaosWeight() * 0.09D;
        }
        if (template.mechanic() == AbilityMechanic.MOVEMENT_ECHO) {
            score += memoryProfile.mobilityWeight() * 0.1D;
        }
        if (template.mechanic() == AbilityMechanic.DEFENSIVE_THRESHOLD || template.mechanic() == AbilityMechanic.RECOVERY_WINDOW) {
            score += memoryProfile.survivalWeight() * 0.1D;
        }
        if (template.id().contains("paradox")) {
            score += ("paradox".equalsIgnoreCase(artifact.getDriftAlignment()) ? 1.2D : 0.0D) + (stage >= 5 ? 0.6D : 0.0D);
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && template.trigger() == AbilityTrigger.ON_AWAKENING) {
            score += 1.0D;
        }
        if (!"none".equalsIgnoreCase(artifact.getFusionPath()) && template.trigger() == AbilityTrigger.ON_FUSION) {
            score += 0.8D;
        }
        return score;
    }

    private double scoreFamily(Artifact artifact, AbilityFamily family, ArtifactMemoryProfile memoryProfile) {
        return switch (family) {
            case PRECISION -> artifact.getSeedPrecisionAffinity() + artifact.getDriftBias("precision") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.2D);
            case BRUTALITY -> artifact.getSeedBrutalityAffinity() + artifact.getDriftBias("brutality") + memoryProfile.aggressionWeight();
            case SURVIVAL -> artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival") + memoryProfile.survivalWeight() + (memoryProfile.traumaWeight() * 0.4D);
            case MOBILITY -> artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility") + memoryProfile.mobilityWeight();
            case CHAOS -> artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos") + memoryProfile.chaosWeight() + (memoryProfile.traumaWeight() * 0.3D);
            case CONSISTENCY -> artifact.getSeedConsistencyAffinity() + artifact.getDriftBias("consistency") + memoryProfile.disciplineWeight() + (memoryProfile.bossWeight() * 0.15D);
        };
    }
}
