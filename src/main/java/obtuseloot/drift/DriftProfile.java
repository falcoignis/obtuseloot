package obtuseloot.drift;

import java.util.Map;

public enum DriftProfile {
    VOLATILE(Map.of("chaos", 1.2, "consistency", -0.4), "surge"),
    PREDATORY(Map.of("brutality", 1.1, "survival", -0.2), "bloodhunger"),
    ASCETIC(Map.of("precision", 1.0, "chaos", -0.5), "glass-focus"),
    HOLLOW(Map.of("survival", 1.0, "mobility", -0.3), "entropy-shell"),
    TEMPEST(Map.of("mobility", 1.1, "precision", 0.3), "stormcharge"),
    PARADOX(Map.of("chaos", 0.7, "consistency", 0.7), "fracture");

    private final Map<String, Double> biasDeltaMap;
    private final String instabilityType;

    DriftProfile(Map<String, Double> biasDeltaMap, String instabilityType) {
        this.biasDeltaMap = biasDeltaMap;
        this.instabilityType = instabilityType;
    }

    public Map<String, Double> biasDeltaMap() { return biasDeltaMap; }
    public String instabilityType() { return instabilityType; }
}
