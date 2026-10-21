package obtuseloot.ecology;

import obtuseloot.artifacts.*;
import obtuseloot.events.LootInsertionListener;
import obtuseloot.persistence.*;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class LootInheritanceIntegrationTest {
    @TempDir Path temp;
    private final UUID worldId = new UUID(0, 10);
    private final WorldPosition origin = new WorldPosition(worldId, 0, 0);
    private final Map<ItemStack, Artifact> stamped = new IdentityHashMap<>();

    @Test void actualLootEventKeepsItemsAndOnlyStampsDistinctUntrackedEquipment() throws Exception {
        var ecosystem = ecosystem(store());
        var first = stack(Material.IRON_AXE, 1);
        var second = stack(Material.DIAMOND_HELMET, 1);
        var existing = stack(Material.WOODEN_SWORD, 1);
        var food = stack(Material.WHEAT, 3);
        var stackedEquipment = stack(Material.IRON_AXE, 2);
        var items = List.of(first, second, existing, food, stackedEquipment);
        var listener = new LootInsertionListener(ecosystem, item -> item == existing, stamped::put, Logger.getLogger("loot-test"));
        var event = event(items);
        listener.onLootGenerate(event);
        assertEquals(items.size(), event.getLoot().size());
        for (int i = 0; i < items.size(); i++) assertSame(items.get(i), event.getLoot().get(i));
        assertEquals(2, stamped.size());
        assertTrue(stamped.containsKey(first)); assertTrue(stamped.containsKey(second));
        assertNotEquals(stamped.get(first).getArtifactStorageKey(), stamped.get(second).getArtifactStorageKey());
        assertEquals("iron_axe", stamped.get(first).getItemCategory());
        assertEquals("diamond_helmet", stamped.get(second).getItemCategory());
        assertEquals(3, food.getAmount()); assertEquals(2, stackedEquipment.getAmount());
        assertFalse(event.isCancelled());
    }

    @Test void lootGenerationPositionSelectsParentEvenWithoutAPlayerEntity() throws Exception {
        var ecosystem = ecosystem(store());
        var parent = ecosystem.createEquipment(new UUID(0, 90), EquipmentArchetype.IRON_AXE, origin, new Random(1));
        ecosystem.observe(LocalInheritancePoolTest.observation(parent, new WorldPosition(worldId, 320, 0), true, 1, 1, "success"));
        var listener = new LootInsertionListener(ecosystem, item -> false, stamped::put, Logger.getLogger("loot-test"));
        var loot = stack(Material.IRON_AXE, 1);
        listener.onLootGenerate(event(List.of(loot)));
        assertEquals(parent.getArtifactStorageKey(), stamped.get(loot).getParentStorageKey());
    }

    @Test void cancelledLootDoesNotCreateOrStampProfiles() throws Exception {
        var ecosystem = ecosystem(store());
        var listener = new LootInsertionListener(ecosystem, item -> false, stamped::put, Logger.getLogger("loot-test"));
        var event = event(List.of(stack(Material.IRON_AXE, 1)));
        event.setCancelled(true); listener.onLootGenerate(event);
        assertTrue(stamped.isEmpty());
    }

    @Test void failedProfileSaveLeavesOriginalLootAvailableAndUnstamped() {
        var refusingStore = new PlayerStateStore() {
            public void saveArtifact(UUID owner, Artifact artifact) { throw new AssertionError(); }
            public Artifact loadArtifact(UUID owner) { throw new AssertionError(); }
            public void saveReputation(UUID owner, ArtifactReputation reputation) { throw new AssertionError(); }
            public ArtifactReputation loadReputation(UUID owner) { throw new AssertionError(); }
            public String loadEcologyState() { return null; }
            public Artifact loadArtifactProfile(String key) { return null; }
            public void saveArtifactProfile(Artifact artifact) { throw new IllegalStateException("test disk failure"); }
        };
        var listener = new LootInsertionListener(ecosystem(refusingStore), item -> false, stamped::put, Logger.getLogger("loot-test"));
        var loot = stack(Material.IRON_AXE, 1);
        var event = event(List.of(loot));
        listener.onLootGenerate(event);
        assertTrue(stamped.isEmpty()); assertSame(loot, event.getLoot().getFirst());
        assertEquals(Material.IRON_AXE, loot.getType()); assertFalse(event.isCancelled());
    }

    private ItemStack stack(Material material, int initialAmount) {
        return new ItemStack() {
            private int amount = initialAmount;
            @Override public Material getType() { return material; }
            @Override public int getAmount() { return amount; }
            @Override public void setAmount(int value) { amount = value; }
            @Override public boolean hasItemMeta() { return false; }
            @Override public ItemStack clone() { return stack(material, amount); }
        };
    }

    private LocalEcosystem ecosystem(PlayerStateStore store) { return new LocalEcosystem(store, new ArtifactManager(store)); }
    private PlayerStateStore store() throws Exception {
        var database = new DatabaseManager("jdbc:sqlite:" + temp.resolve("loot.db"), null, null);
        try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
        return new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE);
    }
    private LootGenerateEvent event(List<ItemStack> items) {
        World world = api(World.class);
        var location = new Location(world, 0, 64, 0);
        return new LootGenerateEvent(world, null, null, api(LootTable.class), new LootContext.Builder(location).build(), items, false);
    }
    private <T> T api(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> worldId;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + " fixture";
            default -> throw new AssertionError("Unexpected API call " + method.getName());
        }));
    }
}
