package obtuseloot.abilities.tree;

import obtuseloot.artifacts.Artifact;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilityBranchResolver {
    public AbilityEvolutionTree resolveTree(String abilityId, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, String behaviorHint) {
        String family = abilityId.contains(".") ? abilityId.substring(0, abilityId.indexOf('.')) : "adaptive";
        long seed = artifact.getArtifactSeed() ^ abilityId.hashCode() ^ artifact.getDriftAlignment().hashCode() ^ artifact.getAwakeningPath().hashCode() ^ artifact.getFusionPath().hashCode();
        seed ^= (long) stage * 17L;
        seed ^= behaviorHint.hashCode();
        Random r = new Random(seed);

        List<AbilityBranch> branches = branchesForFamily(family, artifact, stage);
        int selected = selectIndex(branches, artifact, memoryProfile, stage, family, r);
        return new AbilityEvolutionTree(abilityId, branches.get(selected).id(), branches);
    }

    private int selectIndex(List<AbilityBranch> branches, Artifact artifact, ArtifactMemoryProfile memoryProfile, int stage, String family, Random random) {
        double total = 0.0D;
        double[] scores = new double[branches.size()];
        for (int i = 0; i < branches.size(); i++) {
            double score = 1.0D;
            String id = branches.get(i).id();
            if (id.contains("chaos") || id.contains("paradox")) score += memoryProfile.chaosWeight();
            if (id.contains("discipline") || id.contains("clock") || id.contains("focus")) score += memoryProfile.disciplineWeight();
            if (id.contains("guardian") || id.contains("shelter")) score += memoryProfile.survivalWeight();
            if (id.contains("dash") || id.contains("lane")) score += memoryProfile.mobilityWeight();
            if (id.contains("boss") || id.contains("quarry") || id.contains("anchor")) score += memoryProfile.bossWeight();
            if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath()) && id.contains("awakened")) score += 1.1D;
            if (!"none".equalsIgnoreCase(artifact.getFusionPath()) && id.contains("fusion")) score += 0.9D;
            if ("chaos".equals(family) && id.contains("paradox")) score += stage >= 4 ? 1.2D : 0.0D;
            scores[i] = Math.max(0.1D, score);
            total += scores[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < scores.length; i++) {
            roll -= scores[i];
            if (roll <= 0) return i;
        }
        return scores.length - 1;
    }

    private List<AbilityBranch> branchesForFamily(String family, Artifact artifact, int stage) {
        List<AbilityBranch> out = switch (family) {
            case "precision" -> List.of(
                    branch("precision.focus", "acquire", "mark", "window", "critical lock"),
                    branch("precision.clock", "cadence", "prism", "thread", "timed rupture"),
                    branch("precision.awakened-discipline", "trace", "stitch", "refine", "truth aperture"));
            case "brutality" -> List.of(
                    branch("brutality.mauler", "hunt", "break", "pile-on", "execution burst"),
                    branch("brutality.quarry", "stalk", "fracture", "stampede", "overrun"),
                    branch("brutality.fusion-predator", "bloodcall", "relay", "sunder", "pack collapse"));
            case "survival" -> List.of(
                    branch("survival.guardian", "brace", "counter", "stabilize", "bastion lock"),
                    branch("survival.shelter", "buffer", "recover", "harden", "eternal shelter"),
                    branch("survival.awakened-remnant", "echo", "rebound", "absorb", "undying pulse"));
            case "mobility" -> List.of(
                    branch("mobility.lane-dancer", "setup", "sidestep", "dash", "corridor climax"),
                    branch("mobility.relay", "tag", "zip", "crossfire", "blink lattice"),
                    branch("mobility.fusion-slipstream", "draft", "hook", "spiral", "kinetic storm"));
            case "chaos" -> List.of(
                    branch("chaos.sprawl", "spark", "split", "cascade", "anomaly bloom"),
                    branch("chaos.paradox", "glitch", "flip", "overwrite", "paradox verdict"),
                    branch("chaos.awakened-entropy", "tear", "scatter", "collapse", "entropy singularity"));
            case "consistency" -> List.of(
                    branch("consistency.anchor", "ground", "pace", "fortify", "perfect refrain"),
                    branch("consistency.discipline", "line", "measure", "repeat", "certainty lock"),
                    branch("consistency.boss-ledger", "record", "calibrate", "settle", "ledger consummation"));
            default -> List.of(branch("adaptive", "setup", "pressure", "closure", "seal"));
        };
        List<AbilityBranch> mod = new ArrayList<>(out);
        if (stage < 3) {
            return mod.subList(0, Math.min(2, mod.size()));
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            mod.add(branch(family + ".awakened-variant", "awakening", "specialize", "escalate", "apex"));
        }
        return mod;
    }

    private AbilityBranch branch(String id, String base, String mid, String advanced, String apex) {
        return new AbilityBranch(id, List.of(
                new AbilityNode("n1", "base", base),
                new AbilityNode("n2", "mid", mid),
                new AbilityNode("n3", "advanced", advanced),
                new AbilityNode("n4", "apex", apex)));
    }
}
