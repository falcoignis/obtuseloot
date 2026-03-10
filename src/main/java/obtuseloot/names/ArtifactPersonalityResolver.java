package obtuseloot.names;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ArtifactPersonalityResolver {
    public ArtifactPersonalityProfile resolve(ArtifactNameContext context, Random random) {
        List<ArtifactPersonalityTrait> weighted = new ArrayList<>();
        add(weighted, ArtifactPersonalityTrait.SOLEMN, has(context.evolutionPath(), "tempered") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.REVERENT, has(context.awakeningPath(), "ward") || has(context.awakeningPath(), "sanct") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.PREDATORY, has(context.archetypePath(), "brut") ? 3 : 1);
        add(weighted, ArtifactPersonalityTrait.PATIENT, has(context.archetypePath(), "consist") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.MOCKING, has(context.driftAlignment(), "chaos") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.INTIMATE, context.storied() ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.MOURNFUL, context.storied() ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.SECRETIVE, context.awakened() ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.WATCHFUL, has(context.archetypePath(), "precision") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.HUNGRY, context.fused() ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.MERCIFUL, has(context.archetypePath(), "survival") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.SERENE, has(context.evolutionPath(), "myth") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.COLD, has(context.itemCategory(), "generic") ? 2 : 1);
        add(weighted, ArtifactPersonalityTrait.PLAYFUL, 1);
        add(weighted, ArtifactPersonalityTrait.ZEALOUS, 1);
        add(weighted, ArtifactPersonalityTrait.THEATRICAL, 1);
        add(weighted, ArtifactPersonalityTrait.JEALOUS, 1);
        add(weighted, ArtifactPersonalityTrait.SPITEFUL, 1);

        ArtifactPersonalityTrait dominant = weighted.get(random.nextInt(weighted.size()));
        ArtifactPersonalityTrait secondary = weighted.get(random.nextInt(weighted.size()));
        if (secondary == dominant) {
            secondary = ArtifactPersonalityTrait.PATIENT;
        }
        double intensity = 0.45D + (context.awakened() ? 0.15D : 0.0D) + (context.storied() ? 0.2D : 0.0D);
        return new ArtifactPersonalityProfile(dominant, secondary, intensity);
    }

    private static boolean has(String value, String fragment) {
        return value != null && value.toLowerCase().contains(fragment);
    }

    private static void add(List<ArtifactPersonalityTrait> weighted, ArtifactPersonalityTrait trait, int count) {
        for (int i = 0; i < count; i++) {
            weighted.add(trait);
        }
    }
}
