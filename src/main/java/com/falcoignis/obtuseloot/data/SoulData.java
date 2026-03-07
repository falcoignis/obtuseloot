package com.falcoignis.obtuseloot.data;

import org.bukkit.Particle;

/**
 * Immutable data object representing one soul type loaded from souls.yml.
 *
 * <p>souls.yml structure per soul:
 * <pre>
 * souls:
 *   void:
 *     tag:              "[Void Soul]"   # Display tag shown in item lore
 *     particle:         SQUID_INK       # Any Bukkit Particle enum name
 *     strength:         3               # Number of particles spawned per tick
 *     offset-y:         1.0             # Height above the player to spawn particles
 *     spread-x:         0.2             # Horizontal spread (X axis)
 *     spread-y:         0.5             # Vertical spread (Y axis)
 *     spread-z:         0.2             # Horizontal spread (Z axis)
 *     extra:            0.01            # Particle speed / extra data
 *     only-when-moving: true            # Only spawn particles when the player is moving
 *     ability:          ""              # Optional special ability ID (omit or leave blank
 *                                       # for particles-only souls). 32 abilities exist across
 *                                       # 16 equipment slots; see ObtuseLoot#soulAllowedForCategory.
 *                                       # Examples: "lantern", "bloom" (boots), "lifesteal" (swords),
 *                                       # "comet" (elytra), "surveyor" (crossbows), etc.
 * </pre>
 *
 * <p>The {@code ability} field is an empty string when no special ability is assigned.
 * SoulEngine checks this field and routes to the correct handler. The ability
 * is slot-specific; the SoulEngine dispatches it only when the item is equipped
 * in the correct slot as defined by ObtuseLoot#soulAllowedForCategory.
 */
record SoulData(
    String   id,
    String   tag,
    Particle particle,
    int      intensity,
    double   offsetY,
    double   spreadX,
    double   spreadY,
    double   spreadZ,
    double   extra,
    boolean  onlyWhenMoving,
    String   ability          // "" = no ability; ability IDs are slot-specific slugs (see ObtuseLoot#soulAllowedForCategory)
) {
    /** Convenience: returns true when this soul carries a special world ability. */
    boolean hasAbility() { return !ability.isEmpty(); }
}
