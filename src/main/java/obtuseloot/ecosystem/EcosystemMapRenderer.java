package obtuseloot.ecosystem;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.dashboard.DashboardMetrics;

public final class EcosystemMapRenderer {
    public enum MapMode {
        DEFAULT,
        LINEAGE,
        SPECIES,
        COLLAPSE,
        GENOME
    }

    private final ObtuseLoot plugin;
    private final ArtifactSpeciesClusterer speciesClusterer = new ArtifactSpeciesClusterer();
    private final LineageTerritoryAnalyzer lineageAnalyzer = new LineageTerritoryAnalyzer();
    private final GenomeTraitField traitField = new GenomeTraitField();
    private final Map<Player, Session> sessions = new LinkedHashMap<>();

    public EcosystemMapRenderer(ObtuseLoot plugin) {
        this.plugin = plugin;
    }

    public boolean handleCommand(CommandSender sender, String[] ecosystemArgs) {
        if (ecosystemArgs.length < 2 || !"map".equalsIgnoreCase(ecosystemArgs[1])) {
            return false;
        }

        MapMode mode = MapMode.DEFAULT;
        GenomeTrait trait = GenomeTrait.RESONANCE;
        if (ecosystemArgs.length >= 3) {
            String token = ecosystemArgs[2].toLowerCase(Locale.ROOT);
            if ("lineage".equals(token)) {
                mode = MapMode.LINEAGE;
            } else if ("species".equals(token)) {
                mode = MapMode.SPECIES;
            } else if ("collapse".equals(token)) {
                mode = MapMode.COLLAPSE;
            } else if ("genome".equals(token)) {
                mode = MapMode.GENOME;
                if (ecosystemArgs.length >= 4) {
                    GenomeTrait parsed = parseTrait(ecosystemArgs[3]);
                    if (parsed == null) {
                        sender.sendMessage("§cUnknown genome trait: " + ecosystemArgs[3]);
                        return true;
                    }
                    trait = parsed;
                }
            }
        }

        if (!(sender instanceof Player player)) {
            printConsoleHotspots(sender, mode, trait);
            return true;
        }

        start(player, mode, trait);
        return true;
    }

    public void start(Player player, MapMode mode, GenomeTrait trait) {
        stop(player);
        Session session = new Session(mode, trait);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> update(player), 1L, 60L);
        session.task = task;
        sessions.put(player, session);
        player.sendMessage("§aEcosystem map visualization enabled: §f" + modeLabel(mode, trait));
    }

    public void stop(Player player) {
        Session session = sessions.remove(player);
        if (session == null) {
            return;
        }
        if (session.task != null) {
            session.task.cancel();
        }
        cleanupMarkers(session);
    }

    public void shutdown() {
        for (Player player : new ArrayList<>(sessions.keySet())) {
            stop(player);
        }
    }

    public List<EcosystemHotspot> hotspots(MapMode mode, GenomeTrait trait) {
        Map<Player, Artifact> artifactsByPlayer = onlineArtifacts();
        if (artifactsByPlayer.isEmpty()) {
            return List.of();
        }

        return switch (mode) {
            case LINEAGE -> lineageAnalyzer.lineageHotspots(artifactsByPlayer);
            case SPECIES -> speciesClusterer.clusterBySpecies(artifactsByPlayer);
            case COLLAPSE -> collapseHotspots(artifactsByPlayer);
            case GENOME -> traitField.traitHotspots(artifactsByPlayer, trait);
            default -> defaultHotspots(artifactsByPlayer);
        };
    }

    public GenomeTrait parseTrait(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (GenomeTrait trait : GenomeTrait.values()) {
            if (trait.name().equals(normalized)) {
                return trait;
            }
        }
        return null;
    }

    private void update(Player player) {
        Session session = sessions.get(player);
        if (session == null || !player.isOnline()) {
            stop(player);
            return;
        }

        List<EcosystemHotspot> hotspots = hotspots(session.mode, session.trait);
        cleanupMarkers(session);

        int limit = Math.min(4, hotspots.size());
        for (int i = 0; i < limit; i++) {
            EcosystemHotspot hotspot = hotspots.get(i);
            renderHotspot(player, hotspot, i == 0, session);
        }

        String overlay = hotspots.isEmpty() ? "No active hotspots" : "Hotspot: " + hotspots.get(0).label();
        player.sendActionBar("§b[Ecosystem Map] §f" + overlay + " §8(" + modeLabel(session.mode, session.trait) + ")");
    }

    private void renderHotspot(Player player, EcosystemHotspot hotspot, boolean primary, Session session) {
        Location location = hotspot.location().clone().add(0.0D, 1.0D, 0.0D);
        Particle.DustOptions dust = new Particle.DustOptions(primary ? Color.AQUA : Color.PURPLE, primary ? 1.3F : 1.0F);
        player.spawnParticle(Particle.DUST, location, 20, 0.6, 0.4, 0.6, dust);
        player.spawnParticle(Particle.END_ROD, location, 8, 0.4, 0.6, 0.4, 0.01);

        ArmorStand marker = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        marker.setInvisible(true);
        marker.setMarker(true);
        marker.setSmall(true);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setCustomNameVisible(true);
        marker.setGlowing(true);
        marker.setCustomName("§d" + hotspot.label() + " §7(" + String.format(Locale.ROOT, "%.2f", hotspot.score()) + ")");
        session.markers.add(marker);
    }

    private void cleanupMarkers(Session session) {
        for (ArmorStand marker : session.markers) {
            if (marker != null && marker.isValid()) {
                marker.remove();
            }
        }
        session.markers.clear();
    }

    private void printConsoleHotspots(CommandSender sender, MapMode mode, GenomeTrait trait) {
        List<EcosystemHotspot> hotspots = hotspots(mode, trait);
        sender.sendMessage("ObtuseLoot ecosystem map " + modeLabel(mode, trait) + " hotspots:");
        if (hotspots.isEmpty()) {
            sender.sendMessage("- none (no online players with loaded artifacts)");
            return;
        }

        int max = Math.min(10, hotspots.size());
        for (int i = 0; i < max; i++) {
            EcosystemHotspot hotspot = hotspots.get(i);
            Location location = hotspot.location();
            sender.sendMessage(String.format(Locale.ROOT,
                    "%d) %s world=%s x=%.1f y=%.1f z=%.1f score=%.3f",
                    i + 1,
                    hotspot.label(),
                    location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    hotspot.score()));
        }
    }

    private List<EcosystemHotspot> defaultHotspots(Map<Player, Artifact> artifactsByPlayer) {
        Map<String, List<Map.Entry<Player, Artifact>>> byLineage = new LinkedHashMap<>();
        for (Map.Entry<Player, Artifact> entry : artifactsByPlayer.entrySet()) {
            byLineage.computeIfAbsent(entry.getValue().getLatentLineage(), ignored -> new ArrayList<>()).add(entry);
        }
        List<EcosystemHotspot> hotspots = new ArrayList<>();
        byLineage.forEach((lineage, entries) -> {
            Location location = entries.get(0).getKey().getLocation();
            hotspots.add(new EcosystemHotspot(location, entries.size(), "ecosystem:" + lineage));
        });
        hotspots.sort(Comparator.comparingDouble(EcosystemHotspot::score).reversed());
        return hotspots;
    }

    private List<EcosystemHotspot> collapseHotspots(Map<Player, Artifact> artifactsByPlayer) {
        double collapseRisk = 0.5D;
        try {
            DashboardMetrics.CollapseRisk risk = plugin.getDashboardService().calculateMetrics().collapseRisk();
            collapseRisk = switch (risk) {
                case LOW -> 0.35D;
                case MEDIUM -> 0.65D;
                case HIGH -> 0.90D;
            };
        } catch (IOException ignored) {
        }

        List<EcosystemHotspot> baseline = speciesClusterer.clusterBySpecies(artifactsByPlayer);
        List<EcosystemHotspot> weighted = new ArrayList<>();
        for (EcosystemHotspot hotspot : baseline) {
            weighted.add(new EcosystemHotspot(hotspot.location(), hotspot.score() * collapseRisk, "collapse:" + hotspot.label()));
        }
        weighted.sort(Comparator.comparingDouble(EcosystemHotspot::score).reversed());
        return weighted;
    }

    private Map<Player, Artifact> onlineArtifacts() {
        Map<Player, Artifact> artifactsByPlayer = new LinkedHashMap<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
            artifactsByPlayer.put(player, artifact);
        }
        return artifactsByPlayer;
    }

    private String modeLabel(MapMode mode, GenomeTrait trait) {
        if (mode == MapMode.GENOME) {
            return "genome:" + trait.name().toLowerCase(Locale.ROOT);
        }
        return mode.name().toLowerCase(Locale.ROOT);
    }

    private static final class Session {
        private final MapMode mode;
        private final GenomeTrait trait;
        private BukkitTask task;
        private final List<ArmorStand> markers = new ArrayList<>();

        private Session(MapMode mode, GenomeTrait trait) {
            this.mode = mode;
            this.trait = trait;
        }
    }
}
