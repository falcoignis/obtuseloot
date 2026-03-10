package obtuseloot.abilities;

public record RegulatoryGateState(boolean open, double strength) {
    public static RegulatoryGateState open(double strength) {
        return new RegulatoryGateState(true, clamp(strength));
    }

    public static RegulatoryGateState closed(double strength) {
        return new RegulatoryGateState(false, clamp(strength));
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
