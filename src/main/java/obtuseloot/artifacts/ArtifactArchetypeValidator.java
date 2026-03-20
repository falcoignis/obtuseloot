package obtuseloot.artifacts;

public final class ArtifactArchetypeValidator {
    private ArtifactArchetypeValidator() {
    }

    public static EquipmentArchetype requireValid(Artifact artifact, String context) {
        if (artifact == null) {
            throw new IllegalStateException("Artifact is required for " + context);
        }
        String itemCategory = artifact.getItemCategory();
        if (itemCategory == null || itemCategory.isBlank()) {
            throw new IllegalStateException("Artifact archetype is missing for " + context);
        }
        try {
            return EquipmentArchetype.fromId(itemCategory);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid artifact archetype '" + itemCategory + "' for " + context, exception);
        }
    }

    public static String requireValidId(String itemCategory, String context) {
        if (itemCategory == null || itemCategory.isBlank()) {
            throw new IllegalStateException("Artifact archetype is missing for " + context);
        }
        try {
            return EquipmentArchetype.fromId(itemCategory).id();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid artifact archetype '" + itemCategory + "' for " + context, exception);
        }
    }
}
