package obtuseloot.names;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ArtifactVoiceResolver {
    public ArtifactVoiceProfile resolve(ArtifactNameContext context, ArtifactPersonalityProfile personality, Random random) {
        List<ArtifactVoiceRegister> weighted = new ArrayList<>();
        add(weighted, ArtifactVoiceRegister.MARTIAL, has(context.archetypePath(), "brut") ? 3 : 1);
        add(weighted, ArtifactVoiceRegister.LITURGICAL, has(context.awakeningPath(), "ward") ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.WHISPERING, personality.isAny(ArtifactPersonalityTrait.SECRETIVE) ? 3 : 1);
        add(weighted, ArtifactVoiceRegister.VELVETY, personality.isAny(ArtifactPersonalityTrait.INTIMATE) ? 3 : 1);
        add(weighted, ArtifactVoiceRegister.COURTLY, personality.isAny(ArtifactPersonalityTrait.PREDATORY) ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.ELEGIAC, personality.isAny(ArtifactPersonalityTrait.MOURNFUL) ? 3 : 1);
        add(weighted, ArtifactVoiceRegister.RITUAL, context.awakened() ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.MOCKING_CEREMONIAL, personality.isAny(ArtifactPersonalityTrait.MOCKING) ? 3 : 1);
        add(weighted, ArtifactVoiceRegister.HOLLOW, context.storied() ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.SEVERE, personality.isAny(ArtifactPersonalityTrait.COLD) ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.CRUELLY_POLITE, personality.isAny(ArtifactPersonalityTrait.PREDATORY) ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.LULLABY_LIKE, personality.isAny(ArtifactPersonalityTrait.MERCIFUL) ? 2 : 1);
        add(weighted, ArtifactVoiceRegister.ASCETIC, 1);
        add(weighted, ArtifactVoiceRegister.ARCHAIC, 1);
        add(weighted, ArtifactVoiceRegister.PREDATORY, 1);
        add(weighted, ArtifactVoiceRegister.INTIMATE, 1);

        ArtifactVoiceRegister primary = weighted.get(random.nextInt(weighted.size()));
        ArtifactVoiceRegister secondary = weighted.get(random.nextInt(weighted.size()));
        if (secondary == primary) {
            secondary = ArtifactVoiceRegister.ARCHAIC;
        }
        boolean titleHeavy = primary == ArtifactVoiceRegister.LITURGICAL
                || primary == ArtifactVoiceRegister.COURTLY
                || primary == ArtifactVoiceRegister.RITUAL
                || primary == ArtifactVoiceRegister.MOCKING_CEREMONIAL;
        double softness = switch (primary) {
            case WHISPERING, VELVETY, LULLABY_LIKE, INTIMATE, ELEGIAC -> 0.82D;
            case SEVERE, MARTIAL, PREDATORY -> 0.25D;
            default -> 0.55D;
        };
        return new ArtifactVoiceProfile(primary, secondary, titleHeavy, softness);
    }

    private static boolean has(String value, String fragment) {
        return value != null && value.toLowerCase().contains(fragment);
    }

    private static void add(List<ArtifactVoiceRegister> weighted, ArtifactVoiceRegister trait, int count) {
        for (int i = 0; i < count; i++) {
            weighted.add(trait);
        }
    }
}
