package obtuseloot.abilities.tree;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.List;
import java.util.Random;

public class AbilityBranchResolver {
    public AbilityEvolutionTree resolveTree(String abilityId, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, String behaviorHint) {
        long seed = artifact.getArtifactSeed() ^ abilityId.hashCode() ^ artifact.getDriftAlignment().hashCode() ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode();
        seed ^= (long) stage * 17L;
        seed ^= behaviorHint.hashCode();
        Random r = new Random(seed);

        AbilityBranch adaptive = new AbilityBranch("adaptive", List.of(
                new AbilityNode("n1", "setup", "mark"),
                new AbilityNode("n2", "pressure", "chainEscalation"),
                new AbilityNode("n3", "closure", "guardianPulse")));
        AbilityBranch chaotic = new AbilityBranch("chaotic", List.of(
                new AbilityNode("n1", "spark", "unstableDetonation"),
                new AbilityNode("n2", "spread", "battlefieldField"),
                new AbilityNode("n3", "detonate", "burstState")));
        AbilityBranch resilient = new AbilityBranch("resilient", List.of(
                new AbilityNode("n1", "brace", "defensiveThreshold"),
                new AbilityNode("n2", "counter", "retaliation"),
                new AbilityNode("n3", "stabilize", "recoveryWindow")));

        List<AbilityBranch> branches = List.of(adaptive, chaotic, resilient);
        int index = (int) ((r.nextInt(200) + memoryProfile.pressure() + (memoryProfile.chaosWeight() > memoryProfile.disciplineWeight() ? 1 : 0)) % branches.size());
        return new AbilityEvolutionTree(abilityId, branches.get(index).id(), branches);
    }
}
