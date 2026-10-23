package obtuseloot.ecology;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HabitatCaptureFrameTest {
    @Test void standingAirborneAndLootSourcesDescribeTheSameVerticalCavity() {
        World world = world(63, 67, false);
        var standing = HabitatContext.capture(new Location(world, .5, 64, .5));
        var airborne = HabitatContext.capture(new Location(world, .5, 65.1, .5));
        var loot = HabitatContext.capture(new Location(world, 2.5, 64.5, .5));
        assertEquals(standing, airborne, "Actor elevation must not turn the same room into another habitat");
        assertEquals(standing, loot);
    }
    @Test void equalGeometryAtAnotherAltitudeStillMatchesAndTheOriginMayBeAnOccupiedLootCell() {
        var room = HabitatContext.capture(new Location(world(63, 67, false), .5, 64.5, .5));
        var translated = HabitatContext.capture(new Location(world(103, 107, false), .5, 104.5, .5));
        var chest = HabitatContext.capture(new Location(world(63, 67, true), .5, 64.5, .5));
        assertEquals(room, translated); assertEquals(room, chest);
        assertNotEquals(room, HabitatContext.capture(new Location(world(63, 68, false), .5, 65.1, .5)),
                "The native adjacent-ceiling counterexample must remain distinguishable");
    }
    @Test void aRoofWithoutAnObservedSupportingBoundaryIsUnknown() {
        assertNull(HabitatContext.capture(new Location(world(-100, 67, false), .5, 64, .5)));
    }
    @Test void aGroundBlockSourceUsesTheSameFrameAsTheStandingActor() {
        World world = world(63, 67, false);
        Block block = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> world;
                    case "getX", "getZ" -> 0;
                    case "getY" -> 63;
                    default -> throw new AssertionError(method.getName());
                });
        assertEquals(HabitatContext.capture(new Location(world, .5, 64, .5)), WorldPosition.from(block).habitat());
    }
    private World world(int floor, int ceiling, boolean occupiedOrigin) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> new UUID(0, 12);
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "isChunkLoaded" -> true;
            case "getBlockAt" -> {
                int y = (Integer) args[1];
                yield Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class}, (block, query, ignored) -> {
                    assertEquals("isPassable", query.getName()); return y > floor && y < ceiling && (!occupiedOrigin || y != floor + 1);
                });
            }
            default -> throw new AssertionError(method.getName());
        });
    }
}
