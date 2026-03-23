package obtuseloot.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8.1A — Dump Command Throttling.
 *
 * Verifies that {@link DumpCooldownTracker} correctly enforces the minimum
 * interval between successive dump executions.
 */
class EcosystemDumpThrottleTest {

    @Test
    void firstCallAlwaysSucceeds() {
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> 1000L);
        assertTrue(tracker.tryExecute(), "First call must always be allowed");
    }

    @Test
    void secondCallWithinCooldownIsRejected() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        assertTrue(tracker.tryExecute(), "First call must succeed");

        time[0] = 2000L; // only 1000 ms elapsed, cooldown is 3000 ms
        assertFalse(tracker.tryExecute(), "Second call within cooldown must be rejected");
    }

    @Test
    void callAfterCooldownExpiresSucceeds() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        assertTrue(tracker.tryExecute(), "First call must succeed");

        time[0] = 4001L; // 3001 ms elapsed, cooldown is 3000 ms
        assertTrue(tracker.tryExecute(), "Call after cooldown must be allowed");
    }

    @Test
    void callExactlyAtCooldownBoundarySucceeds() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        assertTrue(tracker.tryExecute());

        time[0] = 4000L; // exactly 3000 ms elapsed
        assertTrue(tracker.tryExecute(), "Call exactly at cooldown boundary must be allowed");
    }

    @Test
    void remainingMsIsCorrectDuringCooldown() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        tracker.tryExecute(); // records time[0] = 1000

        time[0] = 2500L; // 1500 ms elapsed, 1500 remaining
        assertEquals(1500L, tracker.remainingMs());
    }

    @Test
    void remainingMsIsZeroAfterCooldown() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        tracker.tryExecute();

        time[0] = 5000L; // well past cooldown
        assertEquals(0L, tracker.remainingMs());
    }

    @Test
    void successfulCallResetsTimer() {
        long[] time = {1000L};
        DumpCooldownTracker tracker = new DumpCooldownTracker(3000L, () -> time[0]);

        tracker.tryExecute(); // t=1000

        time[0] = 4001L;
        assertTrue(tracker.tryExecute(), "Second call after cooldown must succeed"); // t=4001, resets timer

        time[0] = 5000L; // only 999 ms since last success
        assertFalse(tracker.tryExecute(), "Call within new cooldown window must be rejected");
    }
}
