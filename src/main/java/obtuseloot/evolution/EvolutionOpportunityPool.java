package obtuseloot.evolution;

import java.util.Map;

public record EvolutionOpportunityPool(
        AdaptiveSupportBudget budget,
        Map<MechanicNicheTag, NicheOpportunityAllocation> nicheAllocations,
        LineageMomentumPool lineageMomentumPool
) {
}
