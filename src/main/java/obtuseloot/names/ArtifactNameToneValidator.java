package obtuseloot.names;

public final class ArtifactNameToneValidator {
    private final ArtifactNameSafetyFilter safetyFilter = new ArtifactNameSafetyFilter();
    private final ArtifactCadenceHeuristics cadenceHeuristics = new ArtifactCadenceHeuristics();

    public boolean accept(String candidate, ArtifactVoiceProfile voice, double implicationScore, boolean enforceCadence, boolean enforceSafety) {
        if (enforceSafety && !safetyFilter.isSafe(candidate, implicationScore)) {
            return false;
        }
        return !enforceCadence || cadenceHeuristics.score(candidate, voice) >= 0.35D;
    }
}
