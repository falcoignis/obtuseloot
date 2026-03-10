package obtuseloot.names;

public final class ArtifactCadenceHeuristics {
    public double score(String name, ArtifactVoiceProfile voice) {
        if (name == null || name.isBlank()) {
            return 0.0D;
        }
        String normalized = name.toLowerCase();
        int vowels = 0;
        int hard = 0;
        int soft = 0;
        for (char c : normalized.toCharArray()) {
            if ("aeiouy".indexOf(c) >= 0) vowels++;
            if ("kptdrgx".indexOf(c) >= 0) hard++;
            if ("shlmvfnw".indexOf(c) >= 0) soft++;
        }
        double vowelRatio = (double) vowels / Math.max(1, normalized.replace(" ", "").length());
        double softnessBias = voice.softness();
        double texture = (soft * softnessBias + hard * (1.0D - softnessBias)) / Math.max(1.0D, soft + hard);
        double lengthPenalty = normalized.length() > 34 ? 0.15D : 0.0D;
        return Math.max(0.0D, Math.min(1.0D, 0.55D * texture + 0.45D * (1.0D - Math.abs(0.45D - vowelRatio)) - lengthPenalty));
    }
}
