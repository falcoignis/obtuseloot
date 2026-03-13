package obtuseloot.lineage;

public record BranchLifecycleTransition(
        String branchId,
        BranchLifecycleState from,
        BranchLifecycleState to,
        int graceWindowRemaining,
        double survivalScore,
        double maintenanceCost,
        String reason,
        boolean collapsed
) {
}
