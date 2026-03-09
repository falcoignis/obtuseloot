package obtuseloot.abilities;

import java.util.List;

public record AbilityDefinition(
        String id,
        String name,
        AbilityFamily family,
        AbilityTrigger trigger,
        List<AbilityEffect> effects,
        String stage1,
        String stage2,
        String stage3,
        String stage4,
        String stage5
) {
    public String stageDescription(int stage) {
        return switch (Math.max(1, Math.min(5, stage))) {
            case 1 -> stage1;
            case 2 -> stage2;
            case 3 -> stage3;
            case 4 -> stage4;
            default -> stage5;
        };
    }
}
