package obtuseloot.abilities;

public record AbilityRuntimeContext(AbilitySource source,
                                    boolean intentional,
                                    boolean coalesced,
                                    boolean chunkAware,
                                    Long chunkKey,
                                    String world,
                                    String dimension) {
    public static AbilityRuntimeContext passive(AbilitySource source) {
        return new AbilityRuntimeContext(source, false, false, false, null, null, null);
    }

    public static AbilityRuntimeContext passive(AbilitySource source, String world, String dimension) {
        return new AbilityRuntimeContext(source, false, false, false, null, world, dimension);
    }

    public static AbilityRuntimeContext intentional(AbilitySource source) {
        return new AbilityRuntimeContext(source, true, false, false, null, null, null);
    }

    public static AbilityRuntimeContext intentional(AbilitySource source, String world, String dimension) {
        return new AbilityRuntimeContext(source, true, false, false, null, world, dimension);
    }

    public static AbilityRuntimeContext chunkAware(AbilitySource source, long chunkKey, boolean coalesced) {
        return new AbilityRuntimeContext(source, false, coalesced, true, chunkKey, null, null);
    }

    public static AbilityRuntimeContext chunkAware(AbilitySource source, long chunkKey, boolean coalesced, String world, String dimension) {
        return new AbilityRuntimeContext(source, false, coalesced, true, chunkKey, world, dimension);
    }
}
