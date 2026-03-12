package obtuseloot.lineage;

import java.util.ArrayList;
import java.util.List;

public class LineageBranchProfile {
    private final String branchId;
    private final String parentLineageId;
    private final EvolutionaryBiasGenome biasGenome;
    private final List<Long> members = new ArrayList<>();
    private int stabilizationCount;

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

    public void registerMember(long artifactSeed) {
        members.add(artifactSeed);
        stabilizationCount++;
    }
}
