package obtuseloot.ecosystem;

import obtuseloot.abilities.genome.*;
import obtuseloot.analytics.TraitInteractionAnalyzer;
import obtuseloot.artifacts.Artifact;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StoredGenomeConsumersTest {
    private Artifact item(double gene) {
        var item = new Artifact(new UUID(0, 9), "elytra");
        item.setArtifactSeed(42);
        setGenes(item, gene);
        return item;
    }

    private void setGenes(Artifact item, double gene) {
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) traits.put(trait, gene);
        item.setGenome(new ArtifactGenome(42, traits, traits, Set.of()));
    }

    @Test void traitCorrelationReportsUseStoredGenesRatherThanTheSeedProjection() {
        var item = item(0);
        var analyzer = new TraitInteractionAnalyzer();
        assertEquals(0, analyzer.analyze(List.of(item), List.of()).get("kineticBias", "kineticBias"));
        setGenes(item, 1);
        assertEquals(1, analyzer.analyze(List.of(item), List.of()).get("kineticBias", "kineticBias"));
    }

    @Test void spatialTraitHotspotsUseTheCurrentStoredGenomeWithoutChangingIt() {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUID")) return new UUID(0, 5);
                    throw new UnsupportedOperationException(method.getName());
                });
        var location = new Location(world, 2, 70, 2);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLocation" -> location;
                    case "hashCode" -> 5;
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var item = item(0.1D);
        var field = new GenomeTraitField();
        assertEquals(0.1D, field.traitHotspots(Map.of(player, item), GenomeTrait.KINETIC_BIAS).getFirst().score());
        setGenes(item, 0.9D);
        var stored = item.getGenome();
        assertEquals(0.9D, field.traitHotspots(Map.of(player, item), GenomeTrait.KINETIC_BIAS).getFirst().score());
        assertSame(stored, item.getGenome());
    }
}
