package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SeededAbilityResolver implements AbilityResolver {
    private final AbilityRegistry registry;

    public SeededAbilityResolver(AbilityRegistry registry) {
        this.registry = registry;
    }

    @Override
    public AbilityProfile resolve(Artifact artifact, ArtifactReputation reputation) {
        List<AbilityFamily> ranked = new ArrayList<>(List.of(AbilityFamily.values()));
        ranked.sort(Comparator.comparingDouble((AbilityFamily f) -> -score(artifact, reputation, f)));
        long seed = artifact.getArtifactSeed() ^ artifact.getEvolutionPath().hashCode() ^ artifact.getDriftAlignment().hashCode()
                ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode();
        Random random = new Random(seed);
        List<AbilityDefinition> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(2, ranked.size()); i++) {
            List<AbilityDefinition> pool = registry.byFamily(ranked.get(i));
            if (!pool.isEmpty()) {
                picked.add(pool.get(random.nextInt(pool.size())));
            }
        }
        return new AbilityProfile("seeded-" + ranked.get(0).name().toLowerCase(), picked);
    }

    private double score(Artifact artifact, ArtifactReputation rep, AbilityFamily family) {
        return switch (family) {
            case PRECISION -> rep.getPrecision() + artifact.getSeedPrecisionAffinity() + artifact.getDriftBias("precision");
            case BRUTALITY -> rep.getBrutality() + artifact.getSeedBrutalityAffinity() + artifact.getDriftBias("brutality");
            case SURVIVAL -> rep.getSurvival() + artifact.getSeedSurvivalAffinity() + artifact.getDriftBias("survival");
            case MOBILITY -> rep.getMobility() + artifact.getSeedMobilityAffinity() + artifact.getDriftBias("mobility");
            case CHAOS -> rep.getChaos() + artifact.getSeedChaosAffinity() + artifact.getDriftBias("chaos");
            case CONSISTENCY -> rep.getConsistency() + artifact.getSeedConsistencyAffinity() + artifact.getDriftBias("consistency");
        };
    }
}
