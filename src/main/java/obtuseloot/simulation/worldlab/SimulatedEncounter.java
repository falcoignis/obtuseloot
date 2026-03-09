package obtuseloot.simulation.worldlab;

public record SimulatedEncounter(Type type, boolean lowHealthMoment, boolean chainCombat) {
    public enum Type {
        EXPLORATION,
        NORMAL_COMBAT,
        MULTI_TARGET,
        BOSS
    }
}
