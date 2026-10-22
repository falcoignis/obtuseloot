package obtuseloot.ecosystem;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.artifacts.Artifact;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class GenomeTraitField {

    public List<EcosystemHotspot> traitHotspots(Map<Player, Artifact> artifactsByPlayer, GenomeTrait trait) {
        List<EcosystemHotspot> hotspots = new ArrayList<>();
        for (Map.Entry<Player, Artifact> entry : artifactsByPlayer.entrySet()) {
            Location location = entry.getKey().getLocation();
            if (location.getWorld() == null) {
                continue;
            }
            ArtifactGenome genome = entry.getValue().getGenome();
            double value = genome.trait(trait);
            hotspots.add(new EcosystemHotspot(location.clone(), value, "trait:" + trait.name().toLowerCase()));
        }

        hotspots.sort(Comparator.comparingDouble(EcosystemHotspot::score).reversed());
        return hotspots;
    }
}
