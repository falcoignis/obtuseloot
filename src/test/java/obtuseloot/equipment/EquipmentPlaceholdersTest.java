package obtuseloot.equipment;

import obtuseloot.integrations.ObtuseLootExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentPlaceholdersTest {
    @Test void expansionReadsImmutableSlotSnapshotsAndHandlesMissingOfflineOrUnknownValues() {
        var views = new EquipmentViews(); var owner = UUID.randomUUID();
        var item = EquipmentBindingsTest.artifact(owner, "elytra"); item.setDisplayName("A real item");
        views.publish(owner, Map.of(EquipmentSlot.CHEST, new EquippedArtifact(EquipmentSlot.CHEST, item)),
                Map.of(EquipmentSlot.CHEST, List.of("Fleetfoot")), Set.of(item.getArtifactStorageKey() + "@mobility.fleetfoot"));
        var expansion = new ObtuseLootExpansion(views, "test");
        var player = (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(), new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> { if (method.getName().equals("getUniqueId")) return owner; throw new AssertionError(method.getName()); });
        item.setDisplayName("Changed mutable profile");
        assertEquals("A real item", expansion.onRequest(player, "chest_name"));
        assertEquals("mobility.fleetfoot", expansion.onRequest(player, "chest_passives"));
        assertEquals("false", expansion.onRequest(player, "offhand_present"));
        assertEquals("0", expansion.onRequest(null, "equipped_count"));
        assertNull(expansion.onRequest(player, "invented_placeholder"));
        assertTrue(expansion.persist()); assertEquals("obtuseloot", expansion.getIdentifier());
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 500; i++) assertEquals("A real item", expansion.onRequest(player, "chest_name"));
        }).join();
        views.remove(owner); assertEquals("", expansion.onRequest(player, "chest_name"));
        assertEquals("0", expansion.onRequest(player, "equipped_count"));
    }
}
