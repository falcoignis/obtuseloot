package com.falcoignis.obtuseloot.data;

/**
 * Immutable snapshot of which soul effects a player currently has active, keyed by
 * equipment slot. Rebuilt every {@link SoulEngine#CACHE_INTERVAL} ticks by the
 * cache refresher task — never read from inside that task.
 *
 * <p>Slot semantics:
 * <ul>
 *   <li>{@code helmet}     — helmet slot soul → spinning halo above the head</li>
 *   <li>{@code chestplate} — chestplate slot soul (non-elytra) → orbit swirl when stationary</li>
 *   <li>{@code elytra}     — chestplate slot occupied by an ELYTRA item → trail while gliding</li>
 *   <li>{@code leggings}   — leggings slot soul → ambient particle cloud around the player</li>
 *   <li>{@code boots}      — boots slot soul → ground trail while walking/running</li>
 *   <li>{@code shield}     — off-hand (or main-hand) SHIELD soul → rotating particle wall when stationary</li>
 *   <li>{@code weapon}     — main-hand weapon soul → burst on hit, larger burst on kill</li>
 *   <li>{@code tool}       — main-hand tool soul → small burst on interact, larger burst on block break</li>
 * </ul>
 */
record PlayerSoulState(
    SoulData helmet,
    SoulData chestplate,
    SoulData elytra,
    SoulData leggings,
    SoulData boots,
    SoulData shield,
    SoulData weapon,
    SoulData tool
) {
    /** Convenience constant for players with no soul equipment. */
    static final PlayerSoulState EMPTY =
        new PlayerSoulState(null, null, null, null, null, null, null, null);

    /** Returns {@code true} if any slot has an active soul. */
    boolean hasAny() {
        return helmet != null || chestplate != null || elytra   != null
            || leggings != null || boots   != null || shield   != null
            || weapon   != null || tool    != null;
    }
}
