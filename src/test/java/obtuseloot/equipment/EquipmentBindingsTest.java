package obtuseloot.equipment;

import obtuseloot.artifacts.*;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentBindingsTest {
    @Test void sixIndependentIndividualsAndChestReplacement() {
        var state = new EquipmentBindings(); var holder = UUID.randomUUID();
        var gear = new EnumMap<EquipmentSlot, Artifact>(EquipmentSlot.class);
        for (var slot : EquipmentBindings.SLOTS) gear.put(slot, artifact(holder, "diamond_chestplate"));
        var first = state.reconcile(holder, gear);
        assertEquals(6, first.size());
        assertEquals(6, first.values().stream().map(EquippedArtifact::key).distinct().count());
        var elytra = artifact(holder, "elytra"); gear.put(EquipmentSlot.CHEST, elytra);
        var next = state.reconcile(holder, gear);
        assertSame(elytra, next.get(EquipmentSlot.CHEST).artifact());
        assertEquals(6, next.size());
        assertNotEquals(first.get(EquipmentSlot.CHEST).key(), next.get(EquipmentSlot.CHEST).key());
        assertThrows(UnsupportedOperationException.class, () -> next.clear());
    }
    @Test void duplicateIdentityCannotBindTwiceOrToTwoPlayers() {
        var state = new EquipmentBindings(); var first = UUID.randomUUID(); var second = UUID.randomUUID();
        var item = artifact(first, "diamond_sword");
        assertEquals(1, state.reconcile(first, Map.of(EquipmentSlot.HAND, item, EquipmentSlot.OFF_HAND, item)).size());
        assertTrue(state.reconcile(second, Map.of(EquipmentSlot.HAND, item)).isEmpty());
        state.clear(first);
        assertSame(item, state.reconcile(second, Map.of(EquipmentSlot.OFF_HAND, item)).get(EquipmentSlot.OFF_HAND).artifact());
    }
    @Test void capturedIdentitySurvivesNeitherReseedingNorSlotSwap() {
        var state = new EquipmentBindings(); var holder = UUID.randomUUID(); var item = artifact(holder, "elytra");
        var old = state.reconcile(holder, Map.of(EquipmentSlot.CHEST, item));
        item.setArtifactSeed(-2);
        var next = state.reconcile(holder, Map.of(EquipmentSlot.CHEST, item));
        assertFalse(old.get(EquipmentSlot.CHEST).sameIdentity(next.get(EquipmentSlot.CHEST)));
        assertEquals(-1, old.get(EquipmentSlot.CHEST).seed());
    }
    static Artifact artifact(UUID holder, String type) {
        var item = new Artifact(holder, type); item.setArtifactSeed(-1);
        item.setArtifactStorageKey(ArtifactStorageKeys.create()); return item;
    }
}
