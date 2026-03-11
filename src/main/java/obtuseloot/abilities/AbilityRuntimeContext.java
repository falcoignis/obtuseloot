package obtuseloot.abilities;

public record AbilityRuntimeContext(AbilitySource source,
                                    boolean intentional,
                                    boolean coalesced,
                                    boolean chunkAware,
                                    Long chunkKey) {
    public static AbilityRuntimeContext passive(AbilitySource source) {
        return new AbilityRuntimeContext(source, false, false, false, null);
    }

    public static AbilityRuntimeContext intentional(AbilitySource source) {
        return new AbilityRuntimeContext(source, true, false, false, null);
    }

    public static AbilityRuntimeContext chunkAware(AbilitySource source, long chunkKey, boolean coalesced) {
        return new AbilityRuntimeContext(source, false, coalesced, true, chunkKey);
    }
}

