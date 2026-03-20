package obtuseloot.equipment;

import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.artifacts.EquipmentRole;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentArchetypeElytraTest {

    @Test
    void registryIncludesElytraAsEquipment() {
        assertTrue(EquipmentArchetype.isEquipment("elytra"));
        assertTrue(EquipmentArchetype.allIds().contains("elytra"));
        assertEquals(EquipmentArchetype.ELYTRA, EquipmentArchetype.fromId("elytra"));
    }

    @Test
    void elytraHasMobilityTraversalArmorRolesWithoutChestplateOrWeaponRoles() {
        EquipmentArchetype archetype = EquipmentArchetype.ELYTRA;

        assertAll(
                () -> assertTrue(archetype.hasRole(EquipmentRole.ARMOR)),
                () -> assertTrue(archetype.hasRole(EquipmentRole.MOBILITY)),
                () -> assertTrue(archetype.hasRole(EquipmentRole.TRAVERSAL)),
                () -> assertFalse(archetype.hasRole(EquipmentRole.DEFENSIVE_ARMOR)),
                () -> assertFalse(archetype.hasRole(EquipmentRole.WEAPON)),
                () -> assertFalse(archetype.hasRole(EquipmentRole.CHESTPLATE))
        );
    }

    @Test
    void defensiveArmorArchetypesRemainDistinctFromElytra() {
        List<String> defensiveArmorIds = EquipmentArchetype.idsMatching(a -> a.hasRole(EquipmentRole.DEFENSIVE_ARMOR));

        assertTrue(defensiveArmorIds.contains("diamond_chestplate"));
        assertTrue(defensiveArmorIds.contains("turtle_helmet"));
        assertFalse(defensiveArmorIds.contains("elytra"));
    }
}
