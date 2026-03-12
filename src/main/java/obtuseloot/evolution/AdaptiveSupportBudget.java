package obtuseloot.evolution;

public record AdaptiveSupportBudget(
        double totalBudget,
        double utilizedBudget,
        double carryingCapacity,
        double capacityUtilization,
        double saturationIndex,
        double turnoverPressure,
        double explorationReserve
) {
}
