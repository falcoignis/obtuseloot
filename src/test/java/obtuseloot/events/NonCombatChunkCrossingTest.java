package obtuseloot.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NonCombatChunkCrossingTest {
    @Test void ordinaryWalkingAcrossBothPositiveAndNegativeChunkEdgesTriggersSensing() {
        World world = world(1);
        assertTrue(NonCombatAbilityListener.crossesChunk(new Location(world, 15.99, 64, 0), new Location(world, 16.01, 64, 0)));
        assertTrue(NonCombatAbilityListener.crossesChunk(new Location(world, 0.01, 64, 0), new Location(world, -.01, 64, 0)));
        assertFalse(NonCombatAbilityListener.crossesChunk(new Location(world, 1, 64, 0), new Location(world, 15, 64, 0)));
    }
    @Test void dimensionChangesAndNullDestinationsAreNotWalkingScans() {
        World world = world(1);
        assertFalse(NonCombatAbilityListener.crossesChunk(new Location(world, 15, 64, 0), new Location(world(2), 16, 64, 0)));
        assertFalse(NonCombatAbilityListener.crossesChunk(new Location(world, 0, 64, 0), null));
    }
    private World world(long id) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
            if (method.getName().equals("getUID")) return new UUID(0, id);
            throw new AssertionError("Unexpected world access " + method.getName());
        });
    }
}
