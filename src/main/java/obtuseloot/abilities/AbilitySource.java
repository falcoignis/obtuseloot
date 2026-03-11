package obtuseloot.abilities;

public enum AbilitySource {
    MOVE_WORLD_SCAN("move-world-scan"),
    CHUNK_WORLD_SCAN("chunk-world-scan"),
    STRUCTURE_SENSE("structure-sense"),
    BLOCK_INSPECT("block-inspect"),
    ENTITY_INSPECT("entity-inspect"),
    CROP_HARVEST("crop-harvest"),
    RITUAL_GESTURE("ritual-gesture"),
    SOCIAL_INTERACT("social-interact"),
    WITNESS_EVENT("witness-event"),
    MEMORY_EVENT("memory-event"),
    OTHER("other");

    private final String id;

    AbilitySource(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}

