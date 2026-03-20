package obtuseloot.ecosystem;

import obtuseloot.artifacts.Artifact;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class LineageTerritoryAnalyzer {
    public List<EcosystemHotspot> lineageHotspots(Map<Player, Artifact> artifactsByPlayer) {
        Map<String, List<Map.Entry<Player, Artifact>>> groups = artifactsByPlayer.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> normalize(entry.getValue().getLatentLineage())));

        List<EcosystemHotspot> hotspots = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<Player, Artifact>>> entry : groups.entrySet()) {
            Location centroid = average(entry.getValue().stream().map(Map.Entry::getKey).toList());
            if (centroid == null) {
                continue;
            }
            hotspots.add(new EcosystemHotspot(centroid, entry.getValue().size(), "lineage:" + entry.getKey()));
        }

        hotspots.sort(Comparator.comparingDouble(EcosystemHotspot::score).reversed());
        return hotspots;
    }

    private String normalize(String lineage) {
        if (lineage == null || lineage.isBlank()) {
            return "unassigned";
        }
        return lineage;
    }

    private Location average(List<Player> players) {
        if (players.isEmpty()) {
            return null;
        }

        Location first = players.get(0).getLocation();
        String worldName = first.getWorld() == null ? null : first.getWorld().getName();
        double x = 0;
        double y = 0;
        double z = 0;
        int count = 0;

        for (Player player : players) {
            Location location = player.getLocation();
            if (location.getWorld() == null || worldName == null || !worldName.equals(location.getWorld().getName())) {
                continue;
            }
            x += location.getX();
            y += location.getY();
            z += location.getZ();
            count++;
        }

        if (count == 0 || first.getWorld() == null) {
            return null;
        }

        return new Location(first.getWorld(), x / count, y / count, z / count);
    }
}
