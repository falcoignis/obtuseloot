package obtuseloot.abilities;

public enum RegulatoryGate {
    RESONANCE("resonanceGate"),
    VOLATILITY("volatilityGate"),
    MEMORY("memoryGate"),
    ENVIRONMENT("environmentGate"),
    MOBILITY("mobilityGate"),
    SURVIVAL("survivalGate"),
    DISCIPLINE("disciplineGate"),
    LINEAGE_MILESTONE("lineageMilestoneGate");

    private final String id;

    RegulatoryGate(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
