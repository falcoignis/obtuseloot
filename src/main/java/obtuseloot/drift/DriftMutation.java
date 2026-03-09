package obtuseloot.drift;

public record DriftMutation(boolean applied, String profileId, String message, boolean causedEvolutionRecheck,
                            String instabilityState) {
}
