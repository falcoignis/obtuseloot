package obtuseloot.evolution;

import obtuseloot.reputation.ArtifactReputation;

public final class HybridEvolutionResolver {
    private HybridEvolutionResolver() {
    }

    public static String resolve(String archetype, ArtifactReputation rep) {
        return switch (archetype) {
            case "deadeye" -> rep.mobility() > rep.survival() ? "deadeye-tempest" : "deadeye-warden";
            case "vanguard" -> rep.consistency() > rep.chaos() ? "vanguard-aegis" : "vanguard-dreadnought";
            case "ravager" -> rep.chaos() > rep.consistency() ? "ravager-maelstrom" : "ravager-executioner";
            case "strider" -> rep.precision() > rep.brutality() ? "strider-falcon" : "strider-reaver";
            case "harbinger" -> rep.chaos() > rep.mobility() ? "harbinger-void" : "harbinger-storm";
            case "warden" -> rep.survival() > rep.mobility() ? "warden-scholar" : "warden-ranger";
            default -> rep.precision() > rep.brutality() ? "paragon-lumen" : "paragon-umbra";
        };
    }
}
