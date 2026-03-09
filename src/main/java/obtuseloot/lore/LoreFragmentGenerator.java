package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;

public class LoreFragmentGenerator {
    public String driftFragment(Artifact artifact) { return "Drift " + artifact.getDriftAlignment() + " (" + artifact.getDriftLevel() + ")"; }
    public String awakeningFragment(Artifact artifact) { return "Awakening: " + artifact.getAwakeningPath(); }
    public String lineageFragment(Artifact artifact) { return "Lineage: " + artifact.getLatentLineage(); }
    public String instabilityFragment(Artifact artifact) { return artifact.hasInstability() ? "Instability: " + artifact.getCurrentInstabilityState() : "Stable"; }
}
