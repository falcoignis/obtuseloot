package obtuseloot.lineage;

import java.util.ArrayList;
import java.util.List;

public class LineageBranchProfile {
    private final String branchId;
    private final String parentLineageId;
    private final EvolutionaryBiasGenome biasGenome;
    private final List<Long> members = new ArrayList<>();
    private int stabilizationCount;
    private int ageWindows;
    private int windowsSinceLastContribution;
    private double lastSurvivalScore;
    private double lastMaintenanceCost;
    private double lastCrowdingPenalty;
    private double lastStagnationPenalty;
    private BranchLifecycleState lifecycleState = BranchLifecycleState.STABLE;
    private int persistentWeakWindows;
    private int persistentRecoveryWindows;
    private int collapseGraceRemaining;
    private String lastCollapseReason = "na";

    public LineageBranchProfile(String branchId, String parentLineageId, EvolutionaryBiasGenome biasGenome) {
        this.branchId = branchId;
        this.parentLineageId = parentLineageId;
        this.biasGenome = biasGenome;
    }

    public String branchId() {
        return branchId;
    }

    public String parentLineageId() {
        return parentLineageId;
    }

    public EvolutionaryBiasGenome biasGenome() {
        return biasGenome;
    }

    public List<Long> members() {
        return List.copyOf(members);
    }

    public int stabilizationCount() {
        return stabilizationCount;
    }

    public int ageWindows() { return ageWindows; }
    public int windowsSinceLastContribution() { return windowsSinceLastContribution; }
    public double lastSurvivalScore() { return lastSurvivalScore; }
    public double lastMaintenanceCost() { return lastMaintenanceCost; }
    public double lastCrowdingPenalty() { return lastCrowdingPenalty; }
    public double lastStagnationPenalty() { return lastStagnationPenalty; }
    public BranchLifecycleState lifecycleState() { return lifecycleState; }
    public int persistentWeakWindows() { return persistentWeakWindows; }
    public int persistentRecoveryWindows() { return persistentRecoveryWindows; }
    public int collapseGraceRemaining() { return collapseGraceRemaining; }
    public String lastCollapseReason() { return lastCollapseReason; }

    public void registerMember(long artifactSeed) {
        members.add(artifactSeed);
        stabilizationCount++;
        windowsSinceLastContribution = 0;
    }

    public void advanceWindow(boolean contributedInWindow) {
        ageWindows++;
        if (!contributedInWindow) {
            windowsSinceLastContribution++;
        } else {
            windowsSinceLastContribution = 0;
        }
    }

    public void applySurvivalSignals(double survivalScore,
                                     double maintenanceCost,
                                     double crowdingPenalty,
                                     double stagnationPenalty) {
        this.lastSurvivalScore = survivalScore;
        this.lastMaintenanceCost = maintenanceCost;
        this.lastCrowdingPenalty = crowdingPenalty;
        this.lastStagnationPenalty = stagnationPenalty;
    }

    public void setLifecycleState(BranchLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public void setPersistentWeakWindows(int persistentWeakWindows) {
        this.persistentWeakWindows = persistentWeakWindows;
    }

    public void setPersistentRecoveryWindows(int persistentRecoveryWindows) {
        this.persistentRecoveryWindows = persistentRecoveryWindows;
    }

    public void setCollapseGraceRemaining(int collapseGraceRemaining) {
        this.collapseGraceRemaining = collapseGraceRemaining;
    }

    public void setLastCollapseReason(String lastCollapseReason) {
        this.lastCollapseReason = lastCollapseReason == null || lastCollapseReason.isBlank() ? "na" : lastCollapseReason;
    }

    public void resetStagnation() {
        windowsSinceLastContribution = 0;
    }
}
