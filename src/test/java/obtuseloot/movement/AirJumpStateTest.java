package obtuseloot.movement;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AirJumpStateTest {
    private final UUID world = new UUID(0, 1);
    @Test void needsLandingAndDistinctPressAndDoesNotRepeatHeldInput() {
        var state = new AirJumpState(); state.frame(world, false, true);
        assertEquals(0, state.input(true, false, 1));
        state.frame(world, true, true); assertEquals(0, state.input(false, true, 2));
        assertEquals(0, state.input(true, true, 2)); state.frame(world, false, true);
        assertEquals(0, state.input(true, false, 3));
        state.input(false, false, 3); long ticket = state.input(true, false, 4); assertNotEquals(0, ticket);
        state.input(false, false, 4); assertEquals(0, state.input(true, false, 4), "One pending physical request");
        state.complete(ticket, true, 4, 60); state.input(false, false, 100);
        assertEquals(0, state.input(true, false, 100), "Cooldown does not manufacture another airborne charge");
    }
    @Test void landingDoesNotBypassGameTickCooldown() {
        var state = new AirJumpState(); state.frame(world, true, true); state.frame(world, false, true);
        long ticket = state.input(true, false, 1); state.complete(ticket, true, 1, 60);
        state.frame(world, true, true); state.frame(world, false, true); state.input(false, false, 2);
        assertEquals(0, state.input(true, false, 60)); state.input(false, false, 61);
        assertNotEquals(0, state.input(true, false, 61));
    }
    @Test void worldChangesAndMovementInvalidationRejectQueuedAttemptsUntilLanding() {
        var state = new AirJumpState(); state.frame(world, true, true); state.frame(world, false, true);
        long ticket = state.input(true, false, 1); state.invalidate(); assertFalse(state.reserved(ticket));
        state.input(false, false, 2); assertEquals(0, state.input(true, false, 2));
        state.frame(world, true, true); state.input(false, true, 3); state.frame(world, false, true);
        ticket = state.input(true, false, 3); state.frame(new UUID(0, 2), false, true); assertFalse(state.reserved(ticket));
    }
    @Test void vetoCanRetryButCannotCompleteAnObsoleteTicket() {
        var state = new AirJumpState(); state.frame(world, true, true); state.frame(world, false, true);
        long first = state.input(true, false, 1); state.complete(first, false, 1, 60);
        state.input(false, false, 2); long second = state.input(true, false, 2);
        assertNotEquals(first, second); state.complete(first, true, 2, 60); assertTrue(state.reserved(second));
    }
    @Test void disabledAbilityDoesNotOccupyLiveSelectionSlots() throws Exception {
        var previous = obtuseloot.config.RuntimeSettings.get();
        try {
            var config = new org.bukkit.configuration.file.YamlConfiguration(); obtuseloot.config.RuntimeSettings.load(config);
            assertNull(new obtuseloot.abilities.AbilityRegistry().implementedOnly().registrationById("mobility.double_jump"));
            config.set("abilities.double-jump.enabled", true); obtuseloot.config.RuntimeSettings.load(config);
            assertNotNull(new obtuseloot.abilities.AbilityRegistry().implementedOnly().registrationById("mobility.double_jump"));
        } finally {
            var field = obtuseloot.config.RuntimeSettings.class.getDeclaredField("current"); field.setAccessible(true); field.set(null, previous);
        }
    }
    @Test void invalidConfigLeavesLastSnapshotIntact() {
        var previous = obtuseloot.config.RuntimeSettings.get();
        var config = new org.bukkit.configuration.file.YamlConfiguration(); config.set("abilities.double-jump.upward-velocity", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> obtuseloot.config.RuntimeSettings.load(config));
        assertSame(previous, obtuseloot.config.RuntimeSettings.get());
        config.set("abilities.double-jump.upward-velocity", .42); config.set("abilities.double-jump.cooldown-ticks", -1);
        assertThrows(IllegalArgumentException.class, () -> obtuseloot.config.RuntimeSettings.load(config));
        assertSame(previous, obtuseloot.config.RuntimeSettings.get());
    }
}
