package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.data.PlayerSoulState;
import com.falcoignis.obtuseloot.data.SoulData;

import org.bukkit.*;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages all soul particle effects, keyed by equipment slot.
 *
 * <h3>Particle behaviors by slot</h3>
 * <ul>
 *   <li><b>Helmet</b>      — Spinning halo ring above the player's head. Always visible.</li>
 *   <li><b>Chestplate</b>  — Particles orbiting the player's body. Only while stationary.</li>
 *   <li><b>Elytra</b>      — Stream trailing behind the player. Only while actively gliding.</li>
 *   <li><b>Leggings</b>    — Soft ambient cloud around the player's lower body. Always visible.</li>
 *   <li><b>Boots</b>       — Stream trailing behind the player's feet. Only while moving.
 *                            Boots with the <em>lantern</em> ability place temporary light blocks
 *                            while walking; the <em>bloom</em> ability places temporary flowers.
 *                            The <em>groundpound</em> ability releases an AoE shockwave on landing
 *                            from a qualifying fall — fires even when fall damage is suppressed.
 *                            All ability blocks expire after 5 real seconds and are protected from
 *                            player interaction and explosions.</li>
 *   <li><b>Shield</b>      — Rotating vertical wall of particles encircling the player.
 *                            Only while stationary; dissolves the moment the player moves.</li>
 *   <li><b>Weapon</b>      — Burst on hit (×3 intensity), larger burst on kill (×8 intensity).
 *                            Projectiles fired from soul-bound ranged weapons trail through the
 *                            air and burst on impact. Tridents trail themselves while in flight.
 *                            Swords with <em>lifesteal</em> heal the attacker on every melee hit.
 *                            Maces with <em>shatter</em> scatter and double item drops on kill.
 *                            Maces with <em>gravitywell</em> pull nearby mobs toward the impact point.
 *                            Spears with <em>surge</em> slow targets on charge hits; spears with
 *                            <em>vault</em> launch the attacker upward on a qualifying charge hit.
 *                            Tridents with <em>tracking</em> home toward the nearest living entity.</li>
 *   <li><b>Tool</b>        — Small burst on block interaction (×1), larger burst on block break (×4).
 *                            Shovels with <em>dowsing</em> fire a particle beam toward the nearest ore on block break.</li>
 * </ul>
 *
 * <h3>Threading</h3>
 * All tasks and event handlers run on the main server thread. Bukkit's particle, player,
 * and entity APIs are not thread-safe and must never be called asynchronously.
 *
 * <h3>Usage</h3>
 * <pre>
 *   engine = new ObtuseEngine(this, soulKey);                                    // registers event listener
 *   engine.reload(activeSouls, abilityEnabled, abilityParams, color, glyph);  // call after every config load
 *   engine.stop();                                                              // call from onDisable
 * </pre>
 */
public final class ObtuseEngine implements Listener {

    // ── Timing ────────────────────────────────────────────────────────────────

    /** Ticks between full soul-cache rebuilds (~2 s). */
    static final long CACHE_INTERVAL = 40L;

    /** Ticks between particle loop iterations (~150 ms). */
    static final long PARTICLE_INTERVAL = 3L;

    /** Ticks between projectile trail particle spawns (every tick = smooth trail). */
    private static final long PROJECTILE_INTERVAL = 1L;

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Minimum velocity² required for movement-gated effects. */
    private static final double MOVING_THRESHOLD_SQ = 0.005;

    // ── Halo geometry (helmet) ────────────────────────────────────────────────

    /** Height above the player's feet at which the halo ring is centered. */
    private static final double HALO_Y      = 2.3;

    /** Radius of the halo ring in blocks. */
    private static final double HALO_RADIUS = 0.45;

    /** Number of evenly-spaced particle points spawned per loop iteration. */
    private static final int    HALO_POINTS = 6;

    /** Radians the ring rotates per loop iteration (~3 s per full revolution). */
    private static final double HALO_STEP   = 0.45;

    // ── Swirl geometry (chestplate) ───────────────────────────────────────────

    /** Height above the player's feet at which the swirl orbit is centered. */
    private static final double SWIRL_Y      = 1.0;

    /** Orbit radius of the swirl in blocks. */
    private static final double SWIRL_RADIUS = 0.85;

    /** Number of evenly-spaced swirl points per loop iteration. */
    private static final int    SWIRL_POINTS = 5;

    /** Radians the swirl advances per loop iteration (~6 s per full revolution). */
    private static final double SWIRL_STEP   = 0.25;

    // ── Cloud geometry (leggings) ─────────────────────────────────────────────

    /** Height above the player's feet at which the cloud is centered. */
    private static final double CLOUD_Y      = 0.7;

    /** Horizontal radius of the cloud volume. */
    private static final double CLOUD_RADIUS = 1.2;

    /** Vertical half-height of the cloud volume. */
    private static final double CLOUD_HEIGHT = 0.6;

    // ── Wall geometry (shield) ────────────────────────────────────────────────

    /**
     * Radius of the rotating wall cylinder in blocks.
     * Wider than the chestplate swirl (0.85) so the wall reads as a barrier rather
     * than an orbit — the player is visibly inside it.
     */
    private static final double   WALL_RADIUS  = 1.4;

    /**
     * Y offsets (above the player's feet) at which horizontal rings are spawned.
     * Five rings from ground level to just above the crown give a full-body wall.
     */
    private static final double[] WALL_HEIGHTS = {0.1, 0.6, 1.1, 1.6, 2.1};

    /** Number of evenly-spaced particle points per ring. 5 rings × 8 points = 40 per tick. */
    private static final int      WALL_POINTS  = 8;

    /**
     * Radians the wall rotates per loop iteration (~2.7 s per full revolution).
     * Slightly faster than the chestplate swirl (0.25) to feel active and urgent.
     */
    private static final double WALL_STEP = 0.35;

    /**
     * Fraction of the velocity vector replaced by the toward-target vector each tick
     * for the tracking trident ability. The effective value is loaded from config at
     * runtime via {@code abilityParams.get("tracking-turn")} (stored as integer hundredths,
     * e.g. 12 → 0.12). This comment documents the intended range: 0.05–0.25 gives a
     * natural-feeling curve; higher values produce instant-snap behaviour.
     */

    // ── Ability constants ─────────────────────────────────────────────────────

    /**
     * How long (real milliseconds) an ability-placed block persists before being removed.
     * Using wall-clock time rather than ticks makes the duration immune to server lag.
     */
    private static final long ABILITY_BLOCK_DURATION_MS = 5_000L;

    /**
     * Minimum squared distance (blocks²) between consecutive ability block placements
     * for the same player. Prevents flooding adjacent blocks every loop tick.
     * At 1.0 the player must move at least 1 block between placements.
     */
    private static final double ABILITY_MIN_DISTANCE_SQ = 1.0;

    /**
     * Single-tall flowers eligible for the "bloom" ability.
     * Two-block-tall flowers (SUNFLOWER, LILAC, ROSE_BUSH, PEONY) are excluded
     * because placing them requires two air blocks and complicates removal.
     */
    private static final Material[] ABILITY_FLOWERS = {
        Material.DANDELION,       Material.POPPY,           Material.BLUE_ORCHID,
        Material.ALLIUM,          Material.AZURE_BLUET,     Material.RED_TULIP,
        Material.ORANGE_TULIP,    Material.WHITE_TULIP,     Material.PINK_TULIP,
        Material.OXEYE_DAISY,     Material.CORNFLOWER,      Material.LILY_OF_THE_VALLEY,
        Material.WITHER_ROSE,     Material.TORCHFLOWER
    };

    /**
     * {@link Set} mirror of {@link #ABILITY_FLOWERS} for O(1) membership checks in
     * {@link #removeManagedBlock}. Built once at class-load time from the same source
     * array so the two are guaranteed to stay in sync.
     */
    private static final Set<Material> ABILITY_FLOWER_SET =
        Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(ABILITY_FLOWERS)));

    /**
     * All ore block types recognized by the Dowsing soul's ore scan.
     * Includes both standard and deepslate variants, plus nether and ancient debris.
     */
    private static final Set<Material> DOWSING_ORES = Collections.unmodifiableSet(EnumSet.of(
        Material.COAL_ORE,             Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,             Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE,           Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE,             Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE,         Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE,            Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE,          Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE,          Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE,    Material.NETHER_GOLD_ORE,
        Material.ANCIENT_DEBRIS
    ));

    // ── State ─────────────────────────────────────────────────────────────────

    private final Plugin        plugin;
    private final NamespacedKey soulKey;

    /** Active soul data — replaced atomically on every reload(). */
    private Map<String, SoulData> souls = new HashMap<>();

    /** Per-player slot state — rebuilt every CACHE_INTERVAL ticks. */
    private final Map<UUID, PlayerSoulState> playerStateCache   = new HashMap<>();

    /**
     * Projectiles currently in flight that carry a soul effect.
     * UUID → SoulData of the weapon/item that launched or is the projectile.
     * Entries are removed when the projectile hits something or becomes invalid.
     */
    private final Map<UUID, SoulData> trackedProjectiles = new HashMap<>();

    /** Global rotation angles advanced each loop iteration. */
    private double haloAngle      = 0;
    private double swirlAngle     = 0;
    private double shieldWallAngle = 0;

    private BukkitTask cacheTask;
    private BukkitTask particleTask;
    private BukkitTask projectileTask;
    private BukkitTask abilityCleanerTask;

    /**
     * Short-lived repeating tasks, one per active gravity well strike.
     * Each task self-removes from this list when its duration expires.
     * All tasks are cancelled in {@link #stop()} so no well outlives a reload.
     */
    private final List<BukkitTask> activeWells = new ArrayList<>();

    /**
     * Short-lived repeating tasks, one per active leash.
     * Structure mirrors {@link #activeWells}.
     */
    private final List<BukkitTask>  activeLeashes       = new ArrayList<>();
    /** UUIDs of entities currently tethered by an active Leash task. Prevents stacking. */
    private final Set<UUID>         activeLeashTargets  = new HashSet<>();

    /**
     * Short-lived repeating tasks, one per active resonance pulse.
     * Each task highlights a set of nearby ore locations with particles for a
     * fixed duration, then self-removes. All tasks are cancelled by {@link #stop()}.
     */
    private final List<BukkitTask> activeResonanceTasks = new ArrayList<>();

    /**
     * Per-player comet cooldown. Stores the System.currentTimeMillis() of the last
     * Comet shockwave so rapid re-triggers are prevented.
     */
    private final Map<UUID, Long> cometCooldowns = new HashMap<>();

    /**
     * Per-player cooldown expiry timestamps for the Vault spear ability.
     */
    private final Map<UUID, Long> vaultCooldowns = new HashMap<>();

    /**
     * Per-player accumulated walk ticks since the last breadcrumb was recorded.
     * Incremented in the cache loop only when the player moves a believable distance
     * (rules out teleports) and is not flying. Reset to zero each time a crumb is written.
     */
    private final Map<UUID, Integer> breadcrumbWalkTicks = new HashMap<>();

    /**
     * The player's location as of the last cache cycle, used to measure per-cycle
     * walk distance and to reject teleport jumps from accumulating walk credit.
     */
    private final Map<UUID, Location> breadcrumbLastLoc = new HashMap<>();

    /**
     * Short-lived repeating tasks, one per active breadcrumb projection.
     * Each task pulses particle pillars at the stored crumb locations for 8 seconds
     * then self-removes. All tasks are cancelled by {@link #stop()}.
     */
    private final List<BukkitTask> activeBreadcrumbTasks = new ArrayList<>();

    /** PDC key for the serialised breadcrumb location list stored on the chestplate item. */
    private final NamespacedKey breadcrumbKey;

    /** PDC key for the witness journal stored on a Witness soul sword. */
    private final NamespacedKey witnessKey;

    /** PDC key for the mob-type kill set stored on a Compendium soul axe. */
    private final NamespacedKey compendiumKey;

    /**
     * Soul tag color — mirrors {@code ObtuseLoot.soulTagColor} and is refreshed on
     * every {@link #reload} call so dynamic lore rebuilds (e.g. Witness) stay in sync
     * with the server's display configuration.
     */
    private net.kyori.adventure.text.format.NamedTextColor soulTagColor =
        net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;

    /**
     * Soul glyph string — mirrors {@code ObtuseLoot.soulGlyph} and is refreshed on
     * every {@link #reload} call.
     */
    private String soulGlyph = "✦";

    /**
     * Per-player rolling snapshot queue for the Recall chestplate ability.
     * Holds up to 10 recent locations, one per second. On a qualifying hit the
     * player is teleported to the oldest entry and the queue is cleared.
     * Capped via {@code removeFirst()} when full.
     */
    private final Map<UUID, ArrayDeque<Location>> recallSnapshots = new HashMap<>();

    /**
     * Per-player cooldown expiry timestamps for Recall (ms).
     * If {@code System.currentTimeMillis() < recallCooldowns.get(uid)} the ability
     * will not fire even if the player takes a qualifying hit.
     */
    private final Map<UUID, Long> recallCooldowns = new HashMap<>();

    /** Snapshot task — runs every 20 ticks to record Recall position history. */
    private BukkitTask recallSnapshotTask;

    /**
     * Active Molt decoys. Key = player UUID, Value = the ArmorStand decoy's UUID.
     * Cleared when the decoy is removed (on invisibility expiry or on stop/quit).
     */
    private final Map<UUID, UUID> activeMoltDecoys = new HashMap<>();

    /**
     * Per-player cooldown expiry timestamps for Molt (ms).
     */
    private final Map<UUID, Long> moltCooldowns = new HashMap<>();

    /**
     * Maps a player UUID to the ArmorStand UUID acting as their active Surveyor
     * camera anchor. Only one active per player at a time — a second shot cancels
     * the first. Cleaned up in stop() and onPlayerQuit.
     */
    private final Map<UUID, UUID>              surveyorStands        = new HashMap<>();
    /** Stores the game mode a player had before Surveyor switched them to SPECTATOR. */
    private final Map<UUID, org.bukkit.GameMode> surveyorOriginalModes = new HashMap<>();

    /**
     * Active Verdant blocks. Key = block location, Value = expiry timestamp (ms).
     * Each entry represents a DIRT block that was FARMLAND before Verdant un-tilled it.
     * The ability cleaner restores them to FARMLAND when they expire.
     * Stored separately from {@link #managedBlocks} because restoration is to FARMLAND
     * rather than AIR, requiring different removal logic.
     *
     * <p>{@code verdantExpiry} is a {@link java.util.TreeMap} keyed by
     * {@code expiryMs * 1_000_000 + (verdantSeq % 1_000_000)} so every entry has a
     * unique key regardless of when it was placed or the block's hash code.
     * Ordering is preserved because the high part (expiry) dominates for any two
     * distinct millisecond values, and the low part (sequence mod 1M) ensures no
     * collision even when multiple blocks are placed in the same millisecond.
     */
    private final java.util.TreeMap<Long, Location> verdantExpiry  = new java.util.TreeMap<>();
    private final Map<Location, Long>               verdantBlocks  = new HashMap<>();

    /** Monotonically increasing counter used to make {@link #verdantExpiry} keys unique. */
    private long verdantSeq = 0;

    /**
     * Computes a collision-free TreeMap key for a Verdant block expiry entry.
     * The high part ({@code expiryMs * 1_000_000}) dominates the sort order, giving
     * expiry-based ordering. The low part ({@code verdantSeq++ % 1_000_000}) makes
     * every key unique regardless of expiry timestamp, eliminating the hash-collision
     * bug that caused blocks placed in the same millisecond to silently overwrite
     * each other's TreeMap entries.
     */
    private long verdantKey(long expiryMs) {
        return expiryMs * 1_000_000L + (verdantSeq++ % 1_000_000L);
    }

    /**
     * Tracks whether each player was gliding on the previous move tick.
     * Used by the Comet ability to detect the gliding→grounded transition and
     * read horizontal speed at the moment of impact.
     */
    private final Set<UUID> wasGliding = new HashSet<>();

    /**
     * Tracks which players were on the ground as of the previous {@link PlayerMoveEvent}.
     * Used to detect the airborne→grounded transition (landing) without calling the
     * non-existent {@code Location.isOnGround()} — ground state is a property of
     * {@link Entity}, not {@link org.bukkit.Location}.
     */
    private final Set<UUID> onGroundLastTick = new HashSet<>();

    /**
     * Active Brand marks. Key = attacker UUID, Value = (branded target UUID, expiry ms).
     * Updated on axe hits; read each cache cycle to redirect nearby mob aggro.
     * Entries are removed when the brand expires or the target dies.
     */
    private final Map<UUID, Map.Entry<UUID, Long>> activeBrands = new HashMap<>();

    /**
     * UUIDs of entities currently glowing due to the Echolocation helmet ability.
     * Maintained by the cache refresher — entities are added when they enter range
     * and removed (and un-glowed) when they leave range, the player unequips the
     * helmet, or the engine stops.
     */
    private final Set<UUID> glowedEntities = new HashSet<>();

    /**
     * Per-player set of mob UUIDs that have already triggered a Precognition alert burst
     * for that specific player. Keyed by the wearing player's UUID.
     *
     * <p>Using a per-player map (rather than a single global set) ensures that:
     * <ul>
     *   <li>Removing and re-equipping the helmet clears the player's own alert history
     *       independently of other Precognition wearers on the server.</li>
     *   <li>Two players near the same mob each see their own independent alert, rather
     *       than the first player's alert suppressing the second's.</li>
     * </ul>
     * Entries are added when a mob first acquires a player, and removed (along with the
     * whole player entry) when the player is no longer wearing a Precognition helmet.
     * All entries are cleared on reload and stop.
     */
    private final Map<UUID, Set<UUID>> precognitionAlerted = new HashMap<>();

    // ── Ability state ─────────────────────────────────────────────────────────

    /**
     * Which ability IDs are currently enabled. Replaced atomically on every reload().
     * Defaults to an empty map so no ability fires before the first reload() call.
     */
    private Map<String, Boolean> abilityEnabled = Collections.emptyMap();

    /**
     * Numeric configuration values for abilities (e.g. durations, counts).
     * Keyed by {@code "ability-parameter"} convention (e.g. {@code "sinkhole-duration"}).
     * Replaced atomically on every reload().
     */
    private Map<String, Integer> abilityParams = new HashMap<>();

    /**
     * Tracks every block placed by a soul ability.
     * Key  = block Location (world + x/y/z — Bukkit equals/hashCode are correct).
     * Value = {@link System#currentTimeMillis()} at which the block should be removed.
     *
     * <p>A {@link java.util.LinkedHashMap} is used so iteration order matches insertion
     * order, which is approximately expiry order. The cleaner can stop early when it
     * first encounters an unexpired entry, making the common case (few expirations per
     * sweep) very cheap.
     */
    private final LinkedHashMap<Location, Long> managedBlocks = new LinkedHashMap<>();

    /**
     * The last location at which each player's boots ability placed a block.
     * Used to enforce {@link #ABILITY_MIN_DISTANCE_SQ} spacing so the player
     * must travel at least 1 block before the next placement.
     */
    private final Map<UUID, Location> lastAbilityLoc = new HashMap<>();

    /**
     * Tracks when each player last triggered the Groundpound ability.
     * Value is {@link System#currentTimeMillis()} at the moment of the strike.
     * Used to enforce the configurable cooldown between activations.
     */
    private final Map<UUID, Long> groundpoundCooldown = new HashMap<>();

    /**
     * Tracks the highest fall distance recorded for each airborne player.
     * Updated each tick while the player is not on the ground.
     * Read and cleared in {@link #onPlayerLand} the moment they touch down.
     * Allows groundpound to trigger even when fall damage is fully suppressed
     * (Feather Falling IV, slow falling potion, etc.) since we measure the fall
     * ourselves rather than relying on {@link org.bukkit.event.entity.EntityDamageEvent}.
     */
    private final Map<UUID, Float> peakFallDistance = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    public ObtuseEngine(Plugin plugin, NamespacedKey soulKey) {
        this.plugin        = plugin;
        this.soulKey       = soulKey;
        this.breadcrumbKey = new NamespacedKey(plugin, "breadcrumb_data");
        this.witnessKey    = new NamespacedKey(plugin, "witness_journal");
        this.compendiumKey = new NamespacedKey(plugin, "compendium_kills");
        // Register once; survives reload() calls.
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Updates the active soul set, clears caches, and restarts all tasks.
     * Must be called from the main server thread.
     *
     * @param activeSouls    the soul map freshly loaded from souls.yml
     * @param abilityEnabled map of ability ID → enabled flag from config.yml
     * @param abilityParams  map of ability parameter key → integer value from config.yml
     *                       (e.g. {@code "sinkhole-duration"} → ticks)
     */
    public void reload(Map<String, SoulData> activeSouls,
                Map<String, Boolean>  abilityEnabled,
                Map<String, Integer>  abilityParams,
                net.kyori.adventure.text.format.NamedTextColor soulTagColor,
                String soulGlyph) {
        stop(); // cancels tasks, removes managed blocks, clears all ability state maps
        this.souls          = activeSouls;
        this.abilityEnabled = abilityEnabled;   // already an unmodifiable snapshot from ObtuseLoot
        this.abilityParams  = new HashMap<>(abilityParams);
        this.soulTagColor   = soulTagColor;
        this.soulGlyph      = soulGlyph;
        playerStateCache.clear();
        trackedProjectiles.clear();
        startCacheRefresher();
        startParticleLoop();
        startProjectileTracker();
        startAbilityCleaner();
        startRecallSnapshotTask();
    }

    /** Cancels all tasks and removes any ability-placed blocks still in the world. Safe to call before the engine has started. */
    public void stop() {
        if (cacheTask          != null && !cacheTask.isCancelled())          cacheTask.cancel();
        if (particleTask       != null && !particleTask.isCancelled())       particleTask.cancel();
        if (projectileTask     != null && !projectileTask.isCancelled())     projectileTask.cancel();
        if (abilityCleanerTask != null && !abilityCleanerTask.isCancelled()) abilityCleanerTask.cancel();
        activeWells.forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        activeWells.clear();
        activeLeashes.forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        activeLeashes.clear();
        activeLeashTargets.clear();
        activeResonanceTasks.forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        activeResonanceTasks.clear();
        activeBreadcrumbTasks.forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        activeBreadcrumbTasks.clear();
        breadcrumbWalkTicks.clear();
        breadcrumbLastLoc.clear();
        cometCooldowns.clear();
        vaultCooldowns.clear();
        wasGliding.clear();
        onGroundLastTick.clear();
        activeBrands.clear();
        // Surveyor — restore game modes of any active sessions, then remove anchor stands.
        // Must restore modes before clearing surveyorOriginalModes, since the lookup is needed here.
        for (Map.Entry<UUID, UUID> entry : surveyorStands.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                org.bukkit.GameMode original = surveyorOriginalModes.getOrDefault(
                        entry.getKey(), org.bukkit.GameMode.SURVIVAL);
                p.setGameMode(original);
            }
            Entity stand = Bukkit.getEntity(entry.getValue());
            if (stand != null && !stand.isDead()) stand.remove();
        }
        surveyorStands.clear();
        surveyorOriginalModes.clear();
        // Recall — clear snapshots and cooldowns; no world state to undo.
        recallSnapshots.clear();
        recallCooldowns.clear();
        if (recallSnapshotTask != null && !recallSnapshotTask.isCancelled())
            recallSnapshotTask.cancel();
        // Molt — remove any live decoy ArmorStands so they don't persist through a reload.
        for (UUID decoyUid : activeMoltDecoys.values()) {
            Entity decoy = Bukkit.getEntity(decoyUid);
            if (decoy != null && !decoy.isDead()) decoy.remove();
        }
        activeMoltDecoys.clear();
        moltCooldowns.clear();
        // Verdant — restore any live un-tilled blocks back to FARMLAND immediately.
        for (Location loc : verdantBlocks.keySet()) {
            if (loc.getBlock().getType() == Material.DIRT)
                loc.getBlock().setType(Material.FARMLAND, false);
        }
        verdantBlocks.clear();
        verdantExpiry.clear();
        verdantSeq = 0;
        clearAllGlow();
        removeAllManagedBlocks();
    }

    /**
     * Immediately removes every block in {@link #managedBlocks} from the world,
     * then clears the map. Called from {@link #stop()} and before reload.
     * Only removes the block if its material still matches what we placed —
     * if a player somehow placed something on top, we leave it alone.
     */
    private void removeAllManagedBlocks() {
        for (Location loc : managedBlocks.keySet()) {
            removeManagedBlock(loc);
        }
        managedBlocks.clear();
        lastAbilityLoc.clear();
        groundpoundCooldown.clear();
        peakFallDistance.clear();
    }

    // ── Recall snapshot task ──────────────────────────────────────────────────

    /**
     * Runs every 20 ticks. For every online player currently wearing a Recall soul
     * chestplate, appends their current location to a rolling deque capped at
     * {@code recall-snapshot-count} entries. Older entries fall off the front.
     *
     * <p>Snapshots are consumed (and the deque is cleared) when the ability fires,
     * so the deque always represents the most recent N × 1s window of positions.
     */
    private void startRecallSnapshotTask() {
        recallSnapshotTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!Boolean.TRUE.equals(abilityEnabled.get("recall"))) return;
            int cap = abilityParams.getOrDefault("recall-snapshot-count", 10);
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                if (st == null || st.chestplate() == null) continue;
                if (!st.chestplate().ability().equals("recall")) continue;
                ArrayDeque<Location> deque = recallSnapshots
                        .computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
                deque.addLast(p.getLocation().clone());
                while (deque.size() > cap) deque.pollFirst();
            }
        }, 0L, 20L);
    }

    /**
     * Fires the Recall ability for the given player.
     * Teleports them to the oldest location in their snapshot deque, clears the
     * deque, and sets a cooldown so the ability can't chain-trigger.
     */
    private void activateRecall(Player p, SoulData soul) {
        ArrayDeque<Location> deque = recallSnapshots.get(p.getUniqueId());
        if (deque == null || deque.isEmpty()) return;
        Location dest = deque.peekFirst(); // oldest = furthest back in time
        recallSnapshots.remove(p.getUniqueId());

        // Guard against the snapshot's world having been unloaded since it was taken
        // (e.g. multiworld setup with a world that was removed or restarted).
        if (dest.getWorld() == null) {
            p.sendMessage(net.kyori.adventure.text.Component.text(
                "Recall failed — the destination world is no longer loaded.", 
                net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        int cooldownSec = abilityParams.getOrDefault("recall-cooldown", 30);
        recallCooldowns.put(p.getUniqueId(),
            System.currentTimeMillis() + cooldownSec * 1000L);

        // Burst at the departure point so the visual communicates "left from here".
        spawnBurst(p.getLocation().add(0, 1, 0), soul, soul.intensity() * 8);
        p.teleport(dest);
        // Brief slow-falling so a destination at a slightly different elevation
        // doesn't surprise the player with unexpected fall damage.
        p.addPotionEffect(
            new PotionEffect(PotionEffectType.SLOW_FALLING, 30, 0, false, false, false));
    }

    /**
     * Rebuilds the per-player {@link PlayerSoulState} every {@value #CACHE_INTERVAL} ticks.
     * PDC reads are expensive; doing them here (infrequently) keeps the hot particle loop
     * and event handlers free of PDC overhead.
     */
    private void startCacheRefresher() {
        cacheTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            playerStateCache.clear();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerStateCache.put(p.getUniqueId(), buildPlayerState(p));
            }

            // Echolocation — update glow on entities each cache cycle.
            // Build the set of entities that SHOULD be glowing right now, diff against
            // the current set, apply setGlowing changes only where needed.
            if (Boolean.TRUE.equals(abilityEnabled.get("echolocation"))) {
                Set<UUID> shouldGlow = new HashSet<>();
                int echoRange = abilityParams.getOrDefault("echolocation-range", 8);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                    if (st == null || st.helmet() == null) continue;
                    if (!st.helmet().ability().equals("echolocation")) continue;
                    for (Entity nearby : p.getNearbyEntities(echoRange, echoRange, echoRange)) {
                        if (nearby instanceof LivingEntity && !nearby.getUniqueId().equals(p.getUniqueId()))
                            shouldGlow.add(nearby.getUniqueId());
                    }
                }
                // Remove glow from entities no longer in range.
                Iterator<UUID> iter = glowedEntities.iterator();
                while (iter.hasNext()) {
                    UUID id = iter.next();
                    if (!shouldGlow.contains(id)) {
                        Entity e = Bukkit.getEntity(id);
                        if (e != null) e.setGlowing(false);
                        iter.remove();
                    }
                }
                // Apply glow to newly in-range entities.
                for (UUID id : shouldGlow) {
                    if (glowedEntities.add(id)) {
                        Entity e = Bukkit.getEntity(id);
                        if (e != null) e.setGlowing(true);
                    }
                }
            }

            // Precognition — alert the player when a hostile mob first locks onto them.
            // Build the set of mob UUIDs currently targeting each precognition-wearing player,
            // diff against precognitionAlerted per-player, and fire a burst only on newly
            // targeting mobs. Per-player tracking means removing/re-equipping the helmet
            // correctly resets each player's alert history independently.
            if (Boolean.TRUE.equals(abilityEnabled.get("precognition"))) {
                int precogRange = abilityParams.getOrDefault("precognition-range", 16);

                // Track which players are currently wearing Precognition this cycle.
                Set<UUID> currentWearers = new HashSet<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                    if (st == null || st.helmet() == null) continue;
                    if (!st.helmet().ability().equals("precognition")) continue;

                    UUID playerUid = p.getUniqueId();
                    currentWearers.add(playerUid);
                    Set<UUID> alreadyAlerted = precognitionAlerted
                            .computeIfAbsent(playerUid, k -> new HashSet<>());
                    Set<UUID> nowTargetingThisPlayer = new HashSet<>();

                    for (Entity nearby : p.getNearbyEntities(precogRange, precogRange, precogRange)) {
                        if (!(nearby instanceof Mob mob)) continue;
                        if (!p.equals(mob.getTarget())) continue;
                        nowTargetingThisPlayer.add(mob.getUniqueId());

                        // Fire alert only on the first cycle the mob acquires this player.
                        if (!alreadyAlerted.contains(mob.getUniqueId())) {
                            // Burst at the mob's location so the player can see where the threat is.
                            spawnBurst(mob.getLocation().add(0, 1, 0), st.helmet(), st.helmet().intensity() * 5);
                            // Particle line from mob toward player as a directional cue.
                            drawParticleLine(mob.getLocation().add(0, 1, 0),
                                             p.getLocation().add(0, 1, 0),
                                             st.helmet(), 0.75);
                        }
                    }

                    // Retain only mobs still targeting this player so they can re-alert if
                    // they de-target and re-acquire.
                    alreadyAlerted.retainAll(nowTargetingThisPlayer);
                    alreadyAlerted.addAll(nowTargetingThisPlayer);
                }

                // Remove entries for players no longer wearing Precognition so their
                // alert history is fresh if they re-equip the helmet next cycle.
                precognitionAlerted.keySet().retainAll(currentWearers);
            }

            // Brand — redirect nearby mob aggro toward the branded entity each cache cycle.
            if (!activeBrands.isEmpty() && Boolean.TRUE.equals(abilityEnabled.get("brand"))) {
                long now = System.currentTimeMillis();
                int brandRange = abilityParams.getOrDefault("brand-range", 6);
                Iterator<Map.Entry<UUID, Map.Entry<UUID, Long>>> iter = activeBrands.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, Map.Entry<UUID, Long>> entry = iter.next();
                    Map.Entry<UUID, Long> brandData = entry.getValue();
                    if (now > brandData.getValue()) { iter.remove(); continue; } // expired

                    Entity brandedEntity = Bukkit.getEntity(brandData.getKey());
                    if (!(brandedEntity instanceof LivingEntity brandTarget)
                            || brandTarget.isDead() || !brandTarget.isValid()) {
                        iter.remove(); continue;
                    }
                    // Redirect all nearby mobs that have any target to the branded entity instead.
                    for (Entity nearby : brandTarget.getNearbyEntities(brandRange, brandRange, brandRange)) {
                        if (!(nearby instanceof Mob mob)) continue;
                        if (mob.getUniqueId().equals(brandTarget.getUniqueId())) continue;
                        if (mob.getTarget() != null && !mob.getTarget().getUniqueId().equals(brandTarget.getUniqueId())) {
                            mob.setTarget(brandTarget);
                        }
                    }
                }
            }

            // Breadcrumb — accumulate walk ticks for chestplate-wearers and write crumbs.
            // Distance-delta check filters out teleports; flying is excluded so only
            // ground travel and swimming count toward the walk timer.
            if (Boolean.TRUE.equals(abilityEnabled.get("breadcrumb"))) {
                int walkTicksNeeded = abilityParams.getOrDefault("breadcrumb-walk-seconds", 60) * 20;
                int maxCrumbs       = abilityParams.getOrDefault("breadcrumb-max-entries",  10);
                // Maximum believable per-cycle displacement: CACHE_INTERVAL ticks × sprint speed.
                // ~0.29 b/t sprint × 40 ticks = 11.6 — anything beyond 15 is a teleport.
                final double MAX_WALK_DIST_SQ = 15.0 * 15.0;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                    if (st == null || st.chestplate() == null) continue;
                    if (!st.chestplate().ability().equals("breadcrumb")) continue;

                    Location cur  = p.getLocation();
                    Location last = breadcrumbLastLoc.get(p.getUniqueId());
                    breadcrumbLastLoc.put(p.getUniqueId(), cur.clone());

                    // First-seen this session — just record position, don't credit ticks.
                    if (last == null || last.getWorld() != cur.getWorld()) continue;

                    double distSq = last.distanceSquared(cur);
                    // Skip if stationary or teleport-range jump.
                    if (distSq < 0.01 || distSq > MAX_WALK_DIST_SQ) continue;
                    // Skip if the player is in creative/spectator flight.
                    if (p.isFlying()) continue;

                    int ticks = breadcrumbWalkTicks.merge(p.getUniqueId(), CACHE_INTERVAL, Integer::sum);
                    if (ticks >= walkTicksNeeded) {
                        breadcrumbWalkTicks.put(p.getUniqueId(), 0);
                        // Retrieve the chestplate ItemStack directly — PDC write needs the live item.
                        ItemStack chest = p.getInventory().getChestplate();
                        if (chest == null || !chest.hasItemMeta()) continue;
                        List<int[]> crumbs = readBreadcrumbs(chest);
                        crumbs.add(new int[]{cur.getBlockX(), cur.getBlockY(), cur.getBlockZ()});
                        if (crumbs.size() > maxCrumbs) crumbs.remove(0);
                        writeBreadcrumbs(chest, crumbs);
                    }
                }
            }
            // Verdant — apply Regeneration I to any living entity standing on an active
            // Verdant block, and nudge passive mobs within 8 blocks toward the nearest one.
            if (!verdantBlocks.isEmpty() && Boolean.TRUE.equals(abilityEnabled.get("verdant"))) {
                int regenDur    = abilityParams.getOrDefault("verdant-regen-duration",   40);
                int attractRange = abilityParams.getOrDefault("verdant-attract-range",    8);
                long now = System.currentTimeMillis();
                for (Location loc : verdantBlocks.keySet()) {
                    if (now > verdantBlocks.get(loc)) continue; // about to expire — skip
                    // Block above the dirt is where entities stand.
                    Location above = loc.clone().add(0, 1, 0);
                    // Regen any entity standing within 0.8 blocks of the block centre.
                    for (Entity e : loc.getWorld().getNearbyEntities(above, 0.8, 0.8, 0.8)) {
                        if (!(e instanceof LivingEntity le)) continue;
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.REGENERATION, regenDur, 0, true, false, false));
                    }
                    // Attract passive mobs in range — nudge them 0.15 b/t toward the block.
                    for (Entity e : loc.getWorld().getNearbyEntities(above, attractRange, attractRange, attractRange)) {
                        if (!(e instanceof Animals)) continue;
                        Vector toward = above.toVector().subtract(e.getLocation().toVector()).setY(0);
                        if (toward.lengthSquared() < 0.25) continue; // already on it
                        e.setVelocity(e.getVelocity().add(toward.normalize().multiply(0.15)));
                    }
                }
            }

            // Census — live action bar tally of nearby passive/animal mobs.
            if (Boolean.TRUE.equals(abilityEnabled.get("census"))) {
                int censusRange = abilityParams.getOrDefault("census-range", 32);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                    if (st == null || st.tool() == null) continue;
                    if (!st.tool().ability().equals("census")) continue;

                    // Count each entity type — only passive/ambient mobs.
                    // TreeMap sorts entries alphabetically so the action bar readout
                    // is stable frame-to-frame regardless of entity iteration order.
                    Map<String, Integer> tally = new TreeMap<>();
                    for (Entity nearby : p.getNearbyEntities(censusRange, censusRange, censusRange)) {
                        if (!(nearby instanceof Animals || nearby instanceof WaterMob
                                || nearby instanceof Ambient || nearby instanceof Villager)) continue;
                        String name = nearby.getType().name().toLowerCase().replace('_', ' ');
                        tally.merge(name, 1, Integer::sum);
                    }

                    if (tally.isEmpty()) {
                        p.sendActionBar(net.kyori.adventure.text.Component.text(
                            "▸ no animals nearby", net.kyori.adventure.text.format.NamedTextColor.GRAY));
                    } else {
                        StringBuilder sb = new StringBuilder("▸ ");
                        boolean first = true;
                        for (Map.Entry<String, Integer> entry : tally.entrySet()) {
                            if (!first) sb.append("  ·  ");
                            sb.append(entry.getValue()).append(' ').append(entry.getKey());
                            first = false;
                        }
                        p.sendActionBar(net.kyori.adventure.text.Component.text(
                            sb.toString(), net.kyori.adventure.text.format.NamedTextColor.GREEN));
                    }
                }
            }
            // Witness — for each online player holding a Witness soul sword, scan for
            // nearby players and update the journal on the sword item itself.
            // Journal format per entry: uuid§name§count§timeBracket§healthBracket§heldItem
            // Entries separated by |. Name and heldItem are human-readable strings.
            // The journal is read once per sword-holder, all nearby witnesses are batched
            // in memory, then written and the lore rebuilt once — not once per witness.
            if (Boolean.TRUE.equals(abilityEnabled.get("witness"))) {
                int witnessRange = abilityParams.getOrDefault("witness-range", 20);
                int witnessCap   = abilityParams.getOrDefault("witness-cap",   20);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                    if (st == null || st.weapon() == null) continue;
                    if (!st.weapon().ability().equals("witness")) continue;

                    ItemStack sword = p.getInventory().getItemInMainHand();
                    if (!sword.hasItemMeta()) continue;

                    // Read journal once for this sword-holder.
                    LinkedHashMap<String, WitnessEntry> journal = readWitnessJournal(sword);
                    boolean changed = false;

                    for (Entity nearby : p.getNearbyEntities(witnessRange, witnessRange, witnessRange)) {
                        if (!(nearby instanceof Player witnessed)) continue;
                        if (witnessed.getUniqueId().equals(p.getUniqueId())) continue;

                        String uid  = witnessed.getUniqueId().toString();
                        String name = witnessed.getName();

                        // Time of day bucket: dawn/day/dusk/night
                        long timeOfDay = witnessed.getWorld().getTime();
                        int timeBracket = timeOfDay < 1000 ? 0
                                        : timeOfDay < 13000 ? 1
                                        : timeOfDay < 14000 ? 2 : 3;

                        // Health bracket: healthy/wounded/critical
                        double hp  = witnessed.getHealth();
                        double max = witnessed.getMaxHealth();
                        int healthBracket = hp > max * 0.75 ? 0 : hp > max * 0.40 ? 1 : 2;

                        // Held item label — only captured at tier 15, but read eagerly.
                        ItemStack heldItem = witnessed.getInventory().getItemInMainHand();
                        String heldLabel = heldItem.getType() == Material.AIR ? ""
                            : heldItem.getType().name().toLowerCase().replace('_', ' ');

                        // Update journal entry in memory — no PDC round-trip per witness.
                        WitnessEntry entry = journal.getOrDefault(uid,
                            new WitnessEntry(name, 0, timeBracket, healthBracket, ""));
                        entry = new WitnessEntry(
                            name,
                            entry.count() + 1,
                            timeBracket,
                            healthBracket,
                            entry.count() + 1 >= 15 ? heldLabel : entry.heldItem()
                        );
                        journal.put(uid, entry);
                        changed = true;
                    }

                    if (changed) {
                        // Enforce cap — evict oldest entries until within limit.
                        while (journal.size() > witnessCap) {
                            journal.remove(journal.keySet().iterator().next());
                        }
                        // Write once and rebuild lore once regardless of how many witnesses seen.
                        writeWitnessJournal(sword, journal);
                        updateWitnessLore(sword, journal, st.weapon().tag());
                    }
                }
            }
        }, 0L, CACHE_INTERVAL);
    }

    // ── Particle loop ─────────────────────────────────────────────────────────

    /**
     * Applies ambient particle effects every {@value #PARTICLE_INTERVAL} ticks.
     * No PDC reads occur here — all heavy lifting is done by the cache refresher.
     */
    private void startParticleLoop() {
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            haloAngle       = (haloAngle       + HALO_STEP)  % (2 * Math.PI);
            swirlAngle      = (swirlAngle      + SWIRL_STEP) % (2 * Math.PI);
            shieldWallAngle = (shieldWallAngle + WALL_STEP)  % (2 * Math.PI);

            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerSoulState state = playerStateCache.get(p.getUniqueId());
                if (state == null || !state.hasAny()) continue;

                if (state.helmet()     != null) spawnHalo(p,        state.helmet());
                if (state.chestplate() != null) spawnSwirl(p,       state.chestplate());
                if (state.elytra()     != null) spawnFlightTrail(p, state.elytra());
                if (state.elytra()     != null && state.elytra().hasAbility())
                                                 dispatchElytraAbility(p, state.elytra());
                if (state.leggings()   != null) spawnCloud(p,       state.leggings());
                if (state.boots()      != null) spawnTrail(p,       state.boots());
                if (state.boots()      != null && state.boots().hasAbility())
                                                 dispatchBootsAbility(p, state.boots());
                if (state.shield()     != null) spawnShieldWall(p,  state.shield());
                if (state.shield()     != null && state.shield().hasAbility())
                                                 dispatchShieldAbility(p, state.shield());
            }
        }, 0L, PARTICLE_INTERVAL);
    }

    // ── Ability system ────────────────────────────────────────────────────────

    /**
     * Runs every 20 ticks (1 s) and removes any ability-placed blocks whose
     * {@link #ABILITY_BLOCK_DURATION_MS} has elapsed.
     *
     * <p>Because {@link #managedBlocks} is a {@link java.util.LinkedHashMap} with
     * insertion order, entries are approximately in expiry order. We iterate until
     * we find the first entry that has not yet expired and stop — meaning the sweep
     * is O(k) where k is the number of blocks that actually expired this tick, not
     * O(total managed blocks).
     */
    private void startAbilityCleaner() {
        abilityCleanerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // Managed blocks (lantern, bloom) — restore to AIR.
            if (!managedBlocks.isEmpty()) {
                var iter = managedBlocks.entrySet().iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    if (entry.getValue() > now) break;
                    removeManagedBlock(entry.getKey());
                    iter.remove();
                }
            }

            // Verdant blocks — restore DIRT back to FARMLAND when expiry is reached.
            // The TreeMap is ordered by verdantKey(expiry, loc), which sorts correctly
            // because the expiry component dominates — hashCode offsets are bounded
            // and never span more than ~2 seconds of expiry range in practice.
            if (!verdantExpiry.isEmpty()) {
                var iter = verdantExpiry.entrySet().iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    Location loc = entry.getValue();
                    Long blockExpiry = verdantBlocks.get(loc);
                    // Reuse 'now' computed above — both checks occur within the same tick.
                    if (blockExpiry != null && blockExpiry > now) break;
                    if (loc.getBlock().getType() == Material.DIRT)
                        loc.getBlock().setType(Material.FARMLAND, false);
                    verdantBlocks.remove(loc);
                    iter.remove();
                }
            }
        }, 20L, 20L);
    }

    /**
     * Checks ability eligibility and places a block for the boots ability if all
     * conditions are met:
     * <ol>
     *   <li>The ability ID is non-empty and enabled in config.</li>
     *   <li>The player is moving (velocity² > {@link #MOVING_THRESHOLD_SQ}).</li>
     *   <li>The player has moved at least {@link #ABILITY_MIN_DISTANCE_SQ} blocks
     *       since the last placement.</li>
     * </ol>
     *
     * <p>This method is only ever called from the boots branch of the particle loop,
     * so slot enforcement is structural — no explicit boots-slot check is required here.
     */
    private void dispatchBootsAbility(Player p, SoulData soul) {
        String ability = soul.ability();
        if (!Boolean.TRUE.equals(abilityEnabled.get(ability))) return;
        if (p.getVelocity().lengthSquared() <= MOVING_THRESHOLD_SQ) return;

        // Distance throttle — skip if the player hasn't moved far enough since last placement.
        Location current = p.getLocation();
        Location last    = lastAbilityLoc.get(p.getUniqueId());
        if (last != null && last.getWorld() == current.getWorld()
                && last.distanceSquared(current) < ABILITY_MIN_DISTANCE_SQ) return;

        placeAbilityBlock(p, ability, current);
    }

    /**
     * Places one ability block at the player's foot-level position and records it in
     * {@link #managedBlocks}. Only places if the target block is {@link Material#AIR}.
     *
     * <p>Light blocks are placed at level 15 (maximum brightness).
     * Bloom blocks are a randomly chosen single-tall flower.
     *
     * @param p       the player whose foot location is used
     * @param ability {@code "lantern"} or {@code "bloom"}
     * @param loc     the player's current location (pre-fetched by the caller)
     */
    private void placeAbilityBlock(Player p, String ability, Location loc) {
        // Place at the block the player is standing on, not inside the player.
        org.bukkit.block.Block block = loc.getBlock();
        if (block.getType() != Material.AIR) return;

        switch (ability) {
            case "lantern" -> {
                Light lightData = (Light) Bukkit.createBlockData(Material.LIGHT);
                lightData.setLevel(15);
                block.setBlockData(lightData, false); // false = skip physics update
            }
            case "bloom" -> {
                Material flower = ABILITY_FLOWERS[
                    ThreadLocalRandom.current().nextInt(ABILITY_FLOWERS.length)];
                block.setType(flower, false);
            }
            default -> { return; } // unknown ability — don't record
        }

        long expiry = System.currentTimeMillis() + ABILITY_BLOCK_DURATION_MS;
        managedBlocks.put(block.getLocation(), expiry);
        lastAbilityLoc.put(p.getUniqueId(), loc.clone());
    }

    /**
     * Removes a single managed block from the world if its material is still one
     * that we placed (LIGHT or a flower). If the block was replaced by something
     * else in the meantime, we leave it alone — we never overwrite player work.
     *
     * @param loc the exact location stored in {@link #managedBlocks}
     */
    private void removeManagedBlock(Location loc) {
        if (loc.getWorld() == null) return;
        org.bukkit.block.Block block = loc.getBlock();
        Material m = block.getType();
        if (m == Material.LIGHT || ABILITY_FLOWER_SET.contains(m)) {
            block.setType(Material.AIR, false);
        }
        // If material changed, something else was placed there — leave it alone.
    }

    /**
     * Removes the Glowing status from every entity in {@link #glowedEntities}, then
     * clears the set. Also clears {@link #precognitionAlerted} so stale targeting
     * state doesn't carry across reloads. Called from {@link #stop()}.
     */
    private void clearAllGlow() {
        for (UUID id : glowedEntities) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.setGlowing(false);
        }
        glowedEntities.clear();
        precognitionAlerted.clear();
    }

    /**
     * Dispatches the Dead Drop elytra ability each particle loop tick.
     * While the player is gliding AND sneaking, velocity is dampened toward zero
     * for a controlled vertical stall. Slow Falling is applied simultaneously so
     * the resulting drop doesn't deal fall damage.
     *
     * <p>Only called from the elytra branch of the particle loop — slot enforcement
     * is structural.
     */
    private void dispatchElytraAbility(Player p, SoulData soul) {
        if (!soul.ability().equals("deaddrop")) return;
        if (!Boolean.TRUE.equals(abilityEnabled.get("deaddrop"))) return;
        if (!p.isGliding() || !p.isSneaking()) return;
        double dampen = abilityParams.getOrDefault("deaddrop-dampen", 5) / 100.0;
        p.setVelocity(p.getVelocity().multiply(dampen));
        p.addPotionEffect(
            new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, false));
    }

    /**
     * Dispatches the Taunt shield ability each particle loop tick.
     * While the player is actively blocking, all nearby hostile mobs have their
     * target redirected to the player, pulling aggro away from allies.
     * Lowering the shield instantly releases the taunt.
     */
    private void dispatchShieldAbility(Player p, SoulData soul) {
        if (!soul.ability().equals("taunt")) return;
        if (!Boolean.TRUE.equals(abilityEnabled.get("taunt"))) return;
        if (!p.isBlocking()) return;
        int range = abilityParams.getOrDefault("taunt-range", 8);
        for (Entity nearby : p.getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof Mob mob)) continue;
            if (!p.equals(mob.getTarget())) mob.setTarget(p);
        }
    }
    // ── Projectile tracker ────────────────────────────────────────────────────

    /**
     * Spawns trail particles on every tracked projectile every tick.
     * Also evicts stale entries — projectiles that left loaded chunks or were
     * somehow never removed by the hit event.
     */
    private void startProjectileTracker() {
        projectileTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (trackedProjectiles.isEmpty()) return;

            Iterator<Map.Entry<UUID, SoulData>> iter = trackedProjectiles.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<UUID, SoulData> entry = iter.next();
                Entity entity = Bukkit.getEntity(entry.getKey());

                // Evict if the entity has despawned, landed, or is otherwise gone.
                if (entity == null || !entity.isValid() || entity.isOnGround()) {
                    iter.remove();
                    continue;
                }

                SoulData soul = entry.getValue();
                // Tight spread relative to the ambient effect — reads as a clean stream.
                entity.getWorld().spawnParticle(
                    soul.particle(),
                    entity.getLocation(),
                    soul.intensity(),
                    soul.spreadX() * 0.25,
                    soul.spreadY() * 0.25,
                    soul.spreadZ() * 0.25,
                    soul.extra()
                );

                // Tracking — nudge trident velocity toward nearest mob each tick.
                if (soul.ability().equals("tracking")
                        && entity instanceof Trident
                        && Boolean.TRUE.equals(abilityEnabled.get("tracking"))) {
                    nudgeTowardTarget(entity, soul);
                }
            }
        }, 0L, PROJECTILE_INTERVAL);
    }

    // ── Particle shapes ───────────────────────────────────────────────────────

    /**
     * Spawns a spinning ring of particles above the player's head.
     * Always visible regardless of movement state.
     *
     * <p>A single {@link Location} is mutated between iterations rather than allocating
     * a new one per point — {@code World.spawnParticle} reads coordinates immediately
     * and does not retain the reference.
     */
    private void spawnHalo(Player p, SoulData soul) {
        Location loc = p.getLocation().add(0, HALO_Y, 0);
        World world  = loc.getWorld();
        double cx    = loc.getX();
        double cz    = loc.getZ();
        for (int i = 0; i < HALO_POINTS; i++) {
            double angle = haloAngle + (2 * Math.PI * i / HALO_POINTS);
            loc.setX(cx + HALO_RADIUS * Math.cos(angle));
            loc.setZ(cz + HALO_RADIUS * Math.sin(angle));
            world.spawnParticle(soul.particle(), loc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawns particles orbiting around the player's body in a slow spiral.
     * Only active while the player is stationary.
     *
     * <p>A single {@link Location} is mutated between iterations — see {@link #spawnHalo}.
     */
    private void spawnSwirl(Player p, SoulData soul) {
        if (p.getVelocity().lengthSquared() > MOVING_THRESHOLD_SQ) return;
        Location loc = p.getLocation().add(0, SWIRL_Y, 0);
        World    world = loc.getWorld();
        double cx = loc.getX();
        double cy = loc.getY();
        double cz = loc.getZ();
        for (int i = 0; i < SWIRL_POINTS; i++) {
            double angle = swirlAngle + (2 * Math.PI * i / SWIRL_POINTS);
            loc.setX(cx + SWIRL_RADIUS * Math.cos(angle));
            loc.setY(cy + Math.sin(angle * 1.5) * 0.4); // gentle vertical wave for 3-D feel
            loc.setZ(cz + SWIRL_RADIUS * Math.sin(angle));
            world.spawnParticle(soul.particle(), loc, 1, 0, 0, 0, soul.extra());
        }
    }

    /**
     * Spawns a soft ambient cloud of particles around the player's lower body.
     * Always visible regardless of movement state — it follows the player passively.
     * Spread values are larger than the swirl to give a loose, volumetric feel.
     */
    private void spawnCloud(Player p, SoulData soul) {
        Location center = p.getLocation().add(0, CLOUD_Y, 0);
        center.getWorld().spawnParticle(soul.particle(), center,
            soul.intensity(),
            CLOUD_RADIUS,
            CLOUD_HEIGHT,
            CLOUD_RADIUS,
            soul.extra());
    }

    /**
     * Spawns a rotating vertical cylinder of particles encircling the player.
     * Only active while the player is stationary — the wall dissolves the instant
     * they begin moving, reinforcing the "plant your feet and raise your shield" feel.
     *
     * <p>Five horizontal rings span from ground level (y = 0.1) to just above the
     * player's crown (y = 2.1), each consisting of {@value #WALL_POINTS} evenly-spaced
     * points at {@value #WALL_RADIUS} blocks radius. Adjacent rings are given a small
     * angular phase offset so the points do not stack vertically into spokes but instead
     * tile across the cylinder surface.
     *
     * <p>A single {@link Location} is mutated across all 40 point placements per frame —
     * see {@link #spawnHalo}. Each particle is placed exactly (count=1, zero spread)
     * for geometric precision; drift-style spread would blur the wall silhouette.
     */
    private void spawnShieldWall(Player p, SoulData soul) {
        if (p.getVelocity().lengthSquared() > MOVING_THRESHOLD_SQ) return;
        Location loc  = p.getLocation();
        World    world = loc.getWorld();
        double cx = loc.getX();
        double cy = loc.getY();
        double cz = loc.getZ();
        int numRings = WALL_HEIGHTS.length;
        for (int ring = 0; ring < numRings; ring++) {
            double ringPhase = (2 * Math.PI * ring / numRings) * 0.3;
            loc.setY(cy + WALL_HEIGHTS[ring]);
            for (int pt = 0; pt < WALL_POINTS; pt++) {
                double angle = shieldWallAngle + ringPhase + (2 * Math.PI * pt / WALL_POINTS);
                loc.setX(cx + WALL_RADIUS * Math.cos(angle));
                loc.setZ(cz + WALL_RADIUS * Math.sin(angle));
                world.spawnParticle(soul.particle(), loc, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawns a particle stream behind the player based on their movement vector.
     * Only active while the player is moving on the ground.
     */
    private void spawnTrail(Player p, SoulData soul) {
        Vector vel = p.getVelocity();
        if (vel.lengthSquared() <= MOVING_THRESHOLD_SQ) return;
        // Half a block behind the movement direction at foot level.
        Location loc = p.getLocation().add(vel.clone().normalize().multiply(-0.5));
        p.getWorld().spawnParticle(soul.particle(), loc,
            soul.intensity(),
            soul.spreadX(), 0.05, soul.spreadZ(),
            soul.extra());
    }

    /**
     * Spawns a particle stream behind the player while they are actively gliding.
     * Only active while {@link Player#isGliding()} is true.
     */
    private void spawnFlightTrail(Player p, SoulData soul) {
        if (!p.isGliding()) return;
        Vector vel = p.getVelocity();
        if (vel.lengthSquared() <= MOVING_THRESHOLD_SQ) return;
        // One block behind and slightly above centre of mass for a clean wing-trail look.
        Location loc = p.getLocation()
                        .add(0, 0.5, 0)
                        .add(vel.clone().normalize().multiply(-1.0));
        p.getWorld().spawnParticle(soul.particle(), loc,
            soul.intensity(),
            soul.spreadX(), soul.spreadY(), soul.spreadZ(),
            soul.extra());
    }

    /**
     * Spawns a burst of particles at the given location.
     *
     * @param loc   centre of the burst
     * @param soul  soul whose particle type and spread to use
     * @param count number of particles
     */
    private void spawnBurst(Location loc, SoulData soul, int count) {
        loc.getWorld().spawnParticle(soul.particle(), loc, count,
            soul.spreadX() * 1.5,
            soul.spreadY() * 1.5,
            soul.spreadZ() * 1.5,
            soul.extra() * 3);
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Small burst when a soul-tagged tool is used on a block.
     * Fires on left-click (mining start) and right-click (shovel path, hoe till).
     * Also cancels any interaction with ability-managed blocks.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Protect managed blocks from all interaction — check first, regardless of soul state.
        if (e.getClickedBlock() != null
                && managedBlocks.containsKey(e.getClickedBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (e.getAction() != Action.LEFT_CLICK_BLOCK
                && e.getAction() != Action.RIGHT_CLICK_BLOCK
                && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();

        // Breadcrumb projection — sneak + right-click with an empty main hand (or anything).
        // Fires before the tool-only check below so it triggers from the chestplate slot.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (p.isSneaking() && Boolean.TRUE.equals(abilityEnabled.get("breadcrumb"))) {
                PlayerSoulState st = playerStateCache.get(p.getUniqueId());
                if (st != null && st.chestplate() != null && st.chestplate().ability().equals("breadcrumb")) {
                    ItemStack chest = p.getInventory().getChestplate();
                    if (chest != null && chest.hasItemMeta()) {
                        List<int[]> crumbs = readBreadcrumbs(chest);
                        if (!crumbs.isEmpty()) {
                            projectBreadcrumbs(p, crumbs, st.chestplate());
                            e.setCancelled(true); // suppress block interaction during projection
                        }
                    }
                }
            }
        }

        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.LEFT_CLICK_BLOCK
                && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        PlayerSoulState state = playerStateCache.get(p.getUniqueId());
        if (state == null || state.tool() == null) return;

        SoulData soul = state.tool();
        Location loc = e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
        spawnBurst(loc, soul, soul.intensity());

        // Bore — right-clicking dirt, sand, gravel, or clay with a shovel reads the
        // geological column downward to bedrock and displays the distinct layers
        // as a formatted action bar message.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && soul.ability().equals("bore")
                && Boolean.TRUE.equals(abilityEnabled.get("bore"))) {
            Material clicked = e.getClickedBlock().getType();
            if (clicked == Material.DIRT || clicked == Material.SAND
                    || clicked == Material.GRAVEL || clicked == Material.CLAY) {
                fireBore(p, e.getClickedBlock().getLocation());
                e.setCancelled(true); // suppress vanilla shovel path-creation
            }
        }
        // Verdant — right-clicking farmland with a Verdant soul hoe un-tills the block to DIRT
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && soul.ability().equals("verdant")
                && Boolean.TRUE.equals(abilityEnabled.get("verdant"))
                && e.getClickedBlock().getType() == Material.FARMLAND) {
            org.bukkit.block.Block target = e.getClickedBlock();
            Location blockLoc = target.getLocation();
            // Don't stack durations — skip if already active on this block.
            if (!verdantBlocks.containsKey(blockLoc)) {
                int durationSec = abilityParams.getOrDefault("verdant-duration", 20);
                long expiry = System.currentTimeMillis() + durationSec * 1000L;
                target.setType(Material.DIRT, false);
                verdantBlocks.put(blockLoc, expiry);
                verdantExpiry.put(verdantKey(expiry), blockLoc);
            }
        }
    }

    /**
     * Larger burst when a soul-tagged tool breaks a block.
     * Also cancels any attempt to break an ability-managed block.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        // Protect managed blocks — cancel before checking tool soul.
        if (managedBlocks.containsKey(e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }

        PlayerSoulState state = playerStateCache.get(e.getPlayer().getUniqueId());
        if (state == null || state.tool() == null) return;

        SoulData soul = state.tool();
        Location loc  = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        spawnBurst(loc, soul, soul.intensity() * 4);

        // Dowsing — fire a particle beam toward the nearest ore within range.
        if (soul.ability().equals("dowsing")
                && Boolean.TRUE.equals(abilityEnabled.get("dowsing"))) {
            int range = abilityParams.getOrDefault("dowsing-range", 12);
            fireDowsing(e.getPlayer(), e.getBlock().getLocation(), range, soul);
        }

        // Resonance — pulse particles at all nearby ores so the vein shape is visible.
        if (soul.ability().equals("resonance")
                && Boolean.TRUE.equals(abilityEnabled.get("resonance"))) {
            int range    = abilityParams.getOrDefault("resonance-range",    5);
            int duration = abilityParams.getOrDefault("resonance-duration", 60);
            List<Location> ores = findAllOresInRange(e.getBlock().getLocation(), range);
            if (!ores.isEmpty()) activateResonance(ores, soul, duration);
        }
    }

    /**
     * Removes ability-managed block locations from entity explosion block lists,
     * preventing creepers, TNT, end crystals, etc. from destroying them.
     * The blocks themselves are not removed — they'll expire naturally.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (managedBlocks.isEmpty()) return;
        e.blockList().removeIf(b -> managedBlocks.containsKey(b.getLocation()));
    }

    /**
     * Removes ability-managed block locations from block explosion lists
     * (TNT-primed blocks, respawn anchor explosions, etc.).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (managedBlocks.isEmpty()) return;
        e.blockList().removeIf(b -> managedBlocks.containsKey(b.getLocation()));
    }

    /**
     * Cleans up the distance-throttle entry for a player who disconnects.
     * Their managed blocks will expire naturally via the cleaner task —
     * no immediate cleanup needed because the cleaner keeps running until stop().
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        playerStateCache.remove(uid);
        onGroundLastTick.remove(uid);
        lastAbilityLoc.remove(uid);
        groundpoundCooldown.remove(uid);
        peakFallDistance.remove(uid);
        wasGliding.remove(uid);
        cometCooldowns.remove(uid);
        vaultCooldowns.remove(uid);
        breadcrumbWalkTicks.remove(uid);
        breadcrumbLastLoc.remove(uid);
        recallSnapshots.remove(uid);
        recallCooldowns.remove(uid);
        moltCooldowns.remove(uid);
        precognitionAlerted.remove(uid);  // clear per-player alert history on disconnect
        activeBrands.remove(uid);         // quitter can't have an active brand outstanding
        // Remove live Molt decoy so it doesn't linger after the player disconnects.
        UUID decoyUid = activeMoltDecoys.remove(uid);
        if (decoyUid != null) {
            Entity decoy = Bukkit.getEntity(decoyUid);
            if (decoy != null && !decoy.isDead()) decoy.remove();
        }
        // Remove live Surveyor stand and clear both surveyor maps.
        // cancelSurveyor() cannot be used here because it tries to restore the
        // player's game mode — that API call is meaningless for a disconnecting player.
        UUID standUid = surveyorStands.remove(uid);
        surveyorOriginalModes.remove(uid); // always clear, even if no stand was live
        if (standUid != null) {
            Entity stand = Bukkit.getEntity(standUid);
            if (stand != null && !stand.isDead()) stand.remove();
        }
    }

    /**
     * Immediately populates the state cache for a player on join so soul effects
     * are active from their first tick rather than waiting up to 40 ticks for the
     * next cache refresh cycle.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        refreshPlayerState(e.getPlayer());
    }

    /**
     * Refreshes the state cache immediately when a player switches their active
     * hotbar slot. Without this, the weapon/tool slot in {@link PlayerSoulState}
     * would lag by up to {@link #CACHE_INTERVAL} ticks (20 ticks / 1 second),
     * meaning abilities like Lifesteal and Brand could fire from the previously
     * held item rather than the newly selected one.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent e) {
        refreshPlayerState(e.getPlayer());
    }

    /**
     * Refreshes the state cache after respawn — the player's inventory is reset
     * and any prior soul equipment they re-equip needs to be re-detected.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        // Run one tick later so the server has applied the respawn inventory.
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshPlayerState(e.getPlayer()), 1L);
    }

    /**
     * Cancels any live Surveyor camera session when the player teleports.
     * Without this, the player remains in SPECTATOR mode at their old location
     * indefinitely if the scheduled restore task references the old world.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
        cancelSurveyor(e.getPlayer());
    }

    /**
     * Prevents players from interacting with (and looting equipment from) Molt decoys
     * and Surveyor anchor ArmorStands. Both stand types are INVULNERABLE but not
     * otherwise locked against PlayerArmorStandManipulateEvent.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStandManipulate(org.bukkit.event.player.PlayerArmorStandManipulateEvent e) {
        UUID standUid = e.getRightClicked().getUniqueId();
        // Cancel interaction if the stand belongs to any active Molt decoy
        // or Surveyor session (values of both maps are stand UUIDs).
        boolean isMoltDecoy     = activeMoltDecoys.containsValue(standUid);
        boolean isSurveyorStand = surveyorStands.containsValue(standUid);
        if (isMoltDecoy || isSurveyorStand) {
            e.setCancelled(true);
        }
    }

    /**
     * Cancels any live Surveyor camera session if an external source changes the
     * player's game mode while a session is active — prevents them being stuck in
     * SPECTATOR if an admin uses {@code /gamemode} mid-session.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent e) {
        // Only cancel if the change is coming from outside our own surveyor code.
        // Our code sets SPECTATOR, so any change away from SPECTATOR during a session
        // means something external fired — clean up the stand.
        UUID uid = e.getPlayer().getUniqueId();
        if (surveyorStands.containsKey(uid)
                && e.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
            UUID standUid = surveyorStands.remove(uid);
            surveyorOriginalModes.remove(uid); // must match surveyorStands removal
            if (standUid != null) {
                Entity stand = Bukkit.getEntity(standUid);
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
    }

    /**
     * Terminates an active Surveyor camera session for a player — restores their
     * game mode, removes the anchor stand, and clears the session entry.
     * Safe to call if no session is active.
     */
    private void cancelSurveyor(Player p) {
        UUID uid      = p.getUniqueId();
        UUID standUid = surveyorStands.remove(uid);
        org.bukkit.GameMode originalMode = surveyorOriginalModes.remove(uid);
        if (standUid == null) return;
        Entity stand = Bukkit.getEntity(standUid);
        if (stand != null && !stand.isDead()) stand.remove();
        if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            p.setSpectatorTarget(null);
            p.setGameMode(originalMode != null ? originalMode : org.bukkit.GameMode.SURVIVAL);
        }
    }

    /**
     * Builds or refreshes the {@link PlayerSoulState} cache entry for one player.
     * Delegates to {@link #buildPlayerState(Player)} so the inventory-inspection logic
     * lives in exactly one place.
     */
    void refreshPlayerState(Player p) {
        if (!p.isOnline()) return;
        playerStateCache.put(p.getUniqueId(), buildPlayerState(p));
    }

    /**
     * Reads a player's equipped items and classifies each slot into a {@link PlayerSoulState}.
     * Called both by the periodic cache refresher and on-demand from join/respawn handlers.
     *
     * <ul>
     *   <li>The chestplate slot is disambiguated between elytra and chestplate by material.</li>
     *   <li>Shields are checked in the off-hand first, then the main hand (edge case).</li>
     *   <li>The main hand is classified as weapon or tool; AIR and unrecognised materials
     *       leave both {@code weapon} and {@code tool} null.</li>
     * </ul>
     */
    private PlayerSoulState buildPlayerState(Player p) {
        PlayerInventory inv = p.getInventory();

        SoulData helmet = getSoulFromPDC(inv.getHelmet());

        // Elytra occupies the chestplate slot — distinguish by material.
        ItemStack chestItem = inv.getChestplate();
        SoulData chestplate = null;
        SoulData elytra     = null;
        if (chestItem != null && chestItem.getType() == Material.ELYTRA) {
            elytra = getSoulFromPDC(chestItem);
        } else {
            chestplate = getSoulFromPDC(chestItem);
        }

        SoulData boots    = getSoulFromPDC(inv.getBoots());
        SoulData leggings = getSoulFromPDC(inv.getLeggings());

        // Off-hand: check for a soul-bound shield.
        // Shields live in the off-hand in normal play; we also catch the edge
        // case of a shield held in the main hand (isWeapon/isTool both return
        // false for SHIELD, so the main-hand block below would skip it).
        ItemStack mainHand = inv.getItemInMainHand();
        ItemStack offHand  = inv.getItemInOffHand();
        SoulData shield = (offHand != null && offHand.getType() == Material.SHIELD)
                          ? getSoulFromPDC(offHand) : null;
        if (shield == null && mainHand.getType() == Material.SHIELD) {
            shield = getSoulFromPDC(mainHand);
        }

        // Main hand: classify as weapon or tool (mutually exclusive).
        SoulData weapon = null;
        SoulData tool   = null;
        if (mainHand.getType() != Material.AIR) {
            SoulData heldSoul = getSoulFromPDC(mainHand);
            if (heldSoul != null) {
                if (isTool(mainHand.getType()))        tool   = heldSoul;
                else if (isWeapon(mainHand.getType())) weapon = heldSoul;
            }
        }

        return new PlayerSoulState(helmet, chestplate, elytra, leggings, boots, shield, weapon, tool);
    }

    /**
     * Handles movement-triggered soul abilities in a single listener to avoid multiple
     * registrations on the same event type.
     *
     * <p><b>Groundpound (boots)</b> — tracks peak fall distance while airborne; on landing
     * releases an AoE shockwave. Fires unconditionally regardless of fall-damage suppression.
     *
     * <p><b>Momentum (leggings)</b> — on any landing (no minimum height), applies Speed II
     * for a configurable duration, rewarding continuous movement.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Skip rotation-only events — Paper API; avoids processing every head turn.
        if (!e.hasChangedPosition()) return;

        Player p = e.getPlayer();
        PlayerSoulState state = playerStateCache.get(p.getUniqueId());
        if (state == null) return;

        UUID uid = p.getUniqueId();
        boolean wasGrounded = onGroundLastTick.contains(uid);
        boolean isGrounded  = p.isOnGround();
        boolean landing     = !wasGrounded && isGrounded;

        // Update ground-state tracking for next event.
        if (isGrounded) onGroundLastTick.add(uid);
        else            onGroundLastTick.remove(uid);

        // ── Groundpound (boots) ───────────────────────────────────────────────
        if (state.boots() != null && state.boots().ability().equals("groundpound")) {
            if (landing) {
                Float peak = peakFallDistance.remove(p.getUniqueId());
                if (peak != null && Boolean.TRUE.equals(abilityEnabled.get("groundpound"))) {
                    int minFall = abilityParams.getOrDefault("groundpound-min-fall", 4);
                    if (peak >= minFall) {
                        long cooldownMs = abilityParams.getOrDefault("groundpound-cooldown", 3) * 1000L;
                        long now        = System.currentTimeMillis();
                        Long lastStrike = groundpoundCooldown.get(p.getUniqueId());
                        if (lastStrike == null || now - lastStrike >= cooldownMs) {
                            groundpoundCooldown.put(p.getUniqueId(), now);
                            int    radius    = abilityParams.getOrDefault("groundpound-radius", 3);
                            // Damage scales with fall height above the offset baseline.
                            int    dmgOffset100 = abilityParams.getOrDefault("groundpound-damage-offset", 300);
                            double aoeDamage = Math.max(0.0, peak - dmgOffset100 / 100.0);
                            for (Entity nearby : p.getNearbyEntities(radius, radius, radius)) {
                                if (!(nearby instanceof LivingEntity le)) continue;
                                le.damage(aoeDamage, p);
                            }
                            spawnBurst(p.getLocation().add(0, 0.1, 0),
                                       state.boots(), state.boots().intensity() * 8);
                        }
                    }
                }
            } else if (!isGrounded) {
                float fd = p.getFallDistance();
                if (fd > 0) peakFallDistance.merge(p.getUniqueId(), fd, Math::max);
            }
        }

        // ── Momentum (leggings) ───────────────────────────────────────────────
        if (landing && state.leggings() != null
                && state.leggings().ability().equals("momentum")
                && Boolean.TRUE.equals(abilityEnabled.get("momentum"))) {
            int duration = abilityParams.getOrDefault("momentum-duration", 40);
            p.addPotionEffect(
                new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false, false));
            // amplifier=1 → Speed II; ambient=false, particles=false, icon=false (silent)
        }

        // ── Comet (elytra) ───────────────────────────────────────────────────
        // Track gliding state so we can detect the gliding→grounded transition.
        boolean currentlyGliding = p.isGliding();
        boolean wasGlidingBefore = wasGliding.contains(p.getUniqueId());
        if (currentlyGliding) wasGliding.add(p.getUniqueId());
        else wasGliding.remove(p.getUniqueId());

        if (landing && wasGlidingBefore
                && state.elytra() != null
                && state.elytra().ability().equals("comet")
                && Boolean.TRUE.equals(abilityEnabled.get("comet"))) {

            int  minSpeed100 = abilityParams.getOrDefault("comet-min-speed", 60);
            long cooldownMs  = abilityParams.getOrDefault("comet-cooldown", 5) * 1000L;
            long now         = System.currentTimeMillis();
            Long last        = cometCooldowns.get(p.getUniqueId());

            // Read horizontal speed from the from-location's velocity snapshot via the player object.
            Vector vel      = p.getVelocity();
            double hSpeed   = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

            if (hSpeed * 100 >= minSpeed100
                    && (last == null || now - last >= cooldownMs)) {
                cometCooldowns.put(p.getUniqueId(), now);
                int radius = abilityParams.getOrDefault("comet-radius", 5);
                // Knockback scales with horizontal impact speed, capped at configurable max.
                double knockMax  = abilityParams.getOrDefault("comet-knockback-max", 200) / 100.0;
                double knockMult = Math.min(knockMax, hSpeed);
                for (Entity nearby : p.getNearbyEntities(radius, radius, radius)) {
                    if (!(nearby instanceof LivingEntity le)) continue;
                    Vector push = nearby.getLocation().toVector()
                                       .subtract(p.getLocation().toVector());
                    if (push.lengthSquared() > 0.0001) {
                        // Normalize first, then apply upward arc so the Y component
                        // is always 0.3 regardless of entity distance. Setting Y before
                        // normalize would reduce it to ≈ 0.3/|push|, which is nearly
                        // zero for entities at the edge of the radius.
                        push.normalize().setY(0.3).normalize().multiply(knockMult);
                        nearby.setVelocity(push);
                    }
                }
                spawnBurst(p.getLocation().add(0, 0.1, 0), state.elytra(), state.elytra().intensity() * 8);
            }
        }
    }

    /**
     * Nudges a tracking trident's velocity toward the nearest living entity within range.
     * Called every projectile-tracker tick for tridents carrying the tracking ability.
     *
     * <p>The velocity vector is blended between its current direction and a vector pointing
     * directly at the target, weighted by the configurable {@code tracking-turn} value
     * (from {@code abilityParams}, stored as integer hundredths; default 12 → 0.12). Speed
     * (vector length) is preserved so the trident doesn't slow down mid-flight. If no target
     * is within range the trident continues on its current trajectory unchanged.
     *
     * <p>The shooter is excluded from targeting so the trident cannot home back onto them.
     *
     * @param proj the trident entity currently in flight
     * @param soul the tracking soul (used to read configurable range)
     */
    private void nudgeTowardTarget(Entity proj, SoulData soul) {
        if (!(proj instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;

        int range = abilityParams.getOrDefault("tracking-range", 8);
        // tracking-turn is stored as integer hundredths (default 12 = 0.12).
        double turnRate = abilityParams.getOrDefault("tracking-turn", 12) / 100.0;

        // Find the nearest living entity within range, excluding the shooter.
        LivingEntity target  = null;
        double       minDistSq = (double) range * range;
        for (Entity nearby : proj.getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof LivingEntity le)) continue;
            if (nearby.getUniqueId().equals(shooter.getUniqueId())) continue;
            if (le.isDead()) continue;
            double distSq = proj.getLocation().distanceSquared(nearby.getLocation());
            if (distSq < minDistSq) {
                minDistSq = distSq;
                target    = le;
            }
        }
        if (target == null) return;

        Vector current = proj.getVelocity();
        double speed   = current.length();
        if (speed < 0.001) return; // degenerate case — don't divide by near-zero

        // Unit vector pointing from trident to target's centre of mass.
        Vector toward = target.getLocation().add(0, 0.5, 0).toVector()
                              .subtract(proj.getLocation().toVector())
                              .normalize();

        // Blend: new_dir = current_dir * (1 - TURN) + toward * TURN, then restore speed.
        Vector blended = current.clone().normalize()
                                .multiply(1 - turnRate)
                                .add(toward.multiply(turnRate))
                                .normalize()
                                .multiply(speed);
        proj.setVelocity(blended);
    }

    /**
     * Heal-on-hit handler for the "mercy" ability soul.
     * Runs at HIGH priority so armor and enchantment reductions are already
     * reflected in {@link EntityDamageByEntityEvent#getFinalDamage()} but the
     * event can still be cancelled before MONITOR handlers see it.
     *
     * <p>When an arrow from a bow or crossbow carrying a heal-ability soul hits a
     * living entity, the damage is cancelled and the entity is instead healed for
     * half the final damage that would have been dealt. The heal is clamped to the
     * entity's max health so it cannot overflow. The existing MONITOR burst handler
     * does not fire for cancelled events, so no damage burst occurs — correct
     * behavior since the arrow healed rather than harmed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageHeal(EntityDamageByEntityEvent e) {
        // Only arrow projectiles from tracked bows/crossbows carry the heal ability.
        if (!(e.getDamager() instanceof Projectile proj)) return;
        SoulData soul = trackedProjectiles.get(proj.getUniqueId());
        if (soul == null || !soul.ability().equals("mercy")) return;
        if (!Boolean.TRUE.equals(abilityEnabled.get("mercy"))) return;

        // Target must be a living entity to have health.
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        double healAmount = e.getFinalDamage() / 2.0;
        e.setCancelled(true);

        double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + healAmount);
        target.setHealth(newHealth);

        // Spawn heart particles at the healed entity so the effect is visible.
        Location loc = target.getLocation().add(0, 1, 0);
        spawnBurst(loc, soul, soul.intensity() * 3);
    }

    /**
     * Reflect — shield ability. If an incoming projectile hits a player who is blocking
     * with a Reflect soul shield, the damage is cancelled and the projectile is sent back
     * toward its shooter at a configurable fraction of the original speed and damage.
     *
     * <p>Runs at HIGH priority so it fires before the MONITOR burst handler, and the
     * cancellation prevents a spurious hit burst on the player.
     *
     * <p>If the projectile has no known shooter (e.g. dispensed by a block), the incoming
     * velocity is simply inverted so the arrow travels back along its original path.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onReflect(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Projectile proj)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!victim.isBlocking()) return;

        PlayerSoulState state = playerStateCache.get(victim.getUniqueId());
        if (state == null || state.shield() == null) return;
        if (!state.shield().ability().equals("reflect")) return;
        if (!Boolean.TRUE.equals(abilityEnabled.get("reflect"))) return;

        e.setCancelled(true);

        double speedFrac  = abilityParams.getOrDefault("reflect-speed",  80) / 100.0;
        double damageFrac = abilityParams.getOrDefault("reflect-damage", 75) / 100.0;
        double origSpeed  = proj.getVelocity().length();

        // Aim back at the shooter; fall back to velocity inversion for dispensers.
        Vector reflectDir;
        if (proj.getShooter() instanceof Entity shooterEntity) {
            reflectDir = shooterEntity.getLocation().add(0, 0.5, 0).toVector()
                             .subtract(victim.getLocation().toVector());
            if (reflectDir.lengthSquared() < 0.0001) reflectDir = proj.getVelocity().clone().negate();
            else reflectDir.normalize();
        } else {
            reflectDir = proj.getVelocity().clone().negate().normalize();
        }

        double origDamage = (proj instanceof Arrow a) ? a.getDamage() : 2.0;
        final double finalDmg = origDamage * damageFrac;
        final Vector finalVel = reflectDir.multiply(origSpeed * speedFrac);

        victim.getWorld().spawnEntity(victim.getLocation().add(0, 0.5, 0),
                EntityType.ARROW, false, entity -> {
                    Arrow r = (Arrow) entity;
                    r.setShooter(victim);
                    r.setDamage(finalDmg);
                    r.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    r.setVelocity(finalVel);
                });
        spawnBurst(victim.getLocation().add(0, 1, 0), state.shield(), state.shield().intensity() * 3);
    }

    /**
     * Handles chestplate soul abilities that fire when the *wearer* takes damage.
     *
     * <p><b>Recall</b> — if the hit would reduce the player below 2 hearts and
     * the cooldown has cleared, teleports them back to the oldest snapshot position.
     *
     * <p><b>Molt</b> — if the hit exceeds a configurable damage threshold and
     * the cooldown has cleared, spawns a decoy ArmorStand at the player's position
     * (dressed in a copy of their equipment), applies Invisibility to the player,
     * redirects all nearby mobs to target the decoy, and schedules the decoy's removal
     * when the invisibility expires.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerSoulState state = playerStateCache.get(p.getUniqueId());
        if (state == null || state.chestplate() == null) return;
        SoulData soul = state.chestplate();

        // ── Recall ───────────────────────────────────────────────────────────
        if (soul.ability().equals("recall")
                && Boolean.TRUE.equals(abilityEnabled.get("recall"))) {
            long now = System.currentTimeMillis();
            Long cooldownExpiry = recallCooldowns.get(p.getUniqueId());
            boolean onCooldown = cooldownExpiry != null && now < cooldownExpiry;
            if (!onCooldown) {
                double healthAfter = p.getHealth() - e.getFinalDamage();
                int threshold = abilityParams.getOrDefault("recall-threshold-hearts", 2);
                if (healthAfter < threshold * 2.0) { // hearts × 2 = half-hearts (health units)
                    activateRecall(p, soul);
                }
            }
        }

        // ── Molt ─────────────────────────────────────────────────────────────
        if (soul.ability().equals("molt")
                && Boolean.TRUE.equals(abilityEnabled.get("molt"))) {
            long now = System.currentTimeMillis();
            Long cooldownExpiry = moltCooldowns.get(p.getUniqueId());
            boolean onCooldown = cooldownExpiry != null && now < cooldownExpiry;
            // Only one decoy at a time — skip if one is already live.
            boolean decoyLive = activeMoltDecoys.containsKey(p.getUniqueId());
            if (!onCooldown && !decoyLive) {
                int threshold = abilityParams.getOrDefault("molt-damage-threshold", 6);
                if (e.getFinalDamage() >= threshold) {
                    activateMolt(p, soul);
                }
            }
        }
    }

    /**
     * Spawns a decoy ArmorStand at the player's location, applies Invisibility to
     * the player, and redirects all nearby mobs to the decoy. Schedules decoy removal
     * after the invisibility duration so the mob retargeting dissolves naturally.
     */
    private void activateMolt(Player p, SoulData soul) {
        int invisDuration  = abilityParams.getOrDefault("molt-invis-duration",   30); // ticks
        int cooldownSec    = abilityParams.getOrDefault("molt-cooldown",          20);
        int redirectRange  = abilityParams.getOrDefault("molt-redirect-range",    12);

        moltCooldowns.put(p.getUniqueId(),
            System.currentTimeMillis() + cooldownSec * 1000L);

        // Spawn a silent ArmorStand wearing a copy of the player's current armour.
        // ARMOR_STAND is always LivingEntity so mob.setTarget() accepts it.
        Location spawnLoc = p.getLocation().clone();
        ArmorStand decoy = (ArmorStand) p.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        decoy.setVisible(true);           // visible body so mobs have something to look at
        decoy.setGravity(false);
        decoy.setInvulnerable(true);      // decoy absorbs attention, not damage
        decoy.setAI(false);
        decoy.setCollidable(false);
        decoy.setSilent(true);
        decoy.setCustomName(p.getName()); // name-tag makes it read as the player at a glance
        decoy.setCustomNameVisible(true);

        // Copy player's current armour so the decoy looks like them.
        PlayerInventory inv = p.getInventory();
        if (inv.getHelmet()     != null) decoy.getEquipment().setHelmet(inv.getHelmet().clone());
        if (inv.getChestplate() != null) decoy.getEquipment().setChestplate(inv.getChestplate().clone());
        if (inv.getLeggings()   != null) decoy.getEquipment().setLeggings(inv.getLeggings().clone());
        if (inv.getBoots()      != null) decoy.getEquipment().setBoots(inv.getBoots().clone());

        activeMoltDecoys.put(p.getUniqueId(), decoy.getUniqueId());

        // Invisibility — silent and icon-free so the player's screen stays clean.
        p.addPotionEffect(
            new PotionEffect(PotionEffectType.INVISIBILITY, invisDuration, 0, false, false, false));

        // Redirect all nearby hostile mobs to the decoy immediately.
        for (Entity nearby : p.getNearbyEntities(redirectRange, redirectRange, redirectRange)) {
            if (nearby instanceof Mob mob) mob.setTarget(decoy);
        }

        // Schedule decoy removal at the exact tick invisibility expires.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity d = Bukkit.getEntity(decoy.getUniqueId());
            if (d != null && !d.isDead()) d.remove();
            activeMoltDecoys.remove(p.getUniqueId());
        }, invisDuration);
    }

    /**
     * Burst on hit — handles both melee and tracked-projectile entity damage.
     * Heal-ability projectile hits are handled by {@link #onEntityDamageHeal}
     * which cancels the event before this handler runs.
     *
     * <p>Projectile entity hits are handled here (not in {@link #onProjectileHit}) so the
     * burst fires at the precise moment of damage and at the correct entity position.
     * Maces with the gravity well ability activate a pull field at the impact point.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        SoulData soul = null;
        Player attacker = null;

        if (e.getDamager() instanceof Player p) {
            // Melee — pull weapon soul from the state cache.
            PlayerSoulState state = playerStateCache.get(p.getUniqueId());
            if (state != null) soul = state.weapon();
            attacker = p;

        } else if (e.getDamager() instanceof Projectile proj) {
            // Ranged — check the projectile tracking map.
            soul = trackedProjectiles.get(proj.getUniqueId());
        }

        if (soul == null) return;
        Location loc = e.getEntity().getLocation().add(0, 1, 0);
        spawnBurst(loc, soul, soul.intensity() * 3);

        // Gravity well — mace only. Anchored to the impact point, not the target.
        if (attacker != null
                && soul.ability().equals("gravitywell")
                && Boolean.TRUE.equals(abilityEnabled.get("gravitywell"))) {
            activateGravityWell(e.getEntity().getLocation().add(0, 0.5, 0), attacker, soul);
        }

        // Lifesteal — sword only. Heal the attacker on every melee hit.
        if (attacker != null
                && soul.ability().equals("lifesteal")
                && Boolean.TRUE.equals(abilityEnabled.get("lifesteal"))) {
            double healAmount = abilityParams.getOrDefault("lifesteal-amount", 1) * 0.5;
            double newHealth  = Math.min(attacker.getMaxHealth(),
                                         attacker.getHealth() + healAmount);
            attacker.setHealth(newHealth);
        }

        // Leash — tridents only (melee or ranged hit). Tether the struck entity to
        // the impact point so it can't chase or flee for the configured duration.
        if (soul.ability().equals("leash")
                && Boolean.TRUE.equals(abilityEnabled.get("leash"))
                && e.getEntity() instanceof LivingEntity target) {
            activateLeash(target, e.getEntity().getLocation().clone());
        }

        // Brand — axes only. Mark the struck entity so nearby mob aggro redirects to it.
        if (attacker != null
                && soul.ability().equals("brand")
                && Boolean.TRUE.equals(abilityEnabled.get("brand"))
                && e.getEntity() instanceof LivingEntity brandTarget) {
            int durationTicks = abilityParams.getOrDefault("brand-duration", 200);
            long expiryMs     = System.currentTimeMillis() + (durationTicks * 50L);
            activeBrands.put(attacker.getUniqueId(),
                new java.util.AbstractMap.SimpleImmutableEntry<>(brandTarget.getUniqueId(), expiryMs));
            spawnBurst(brandTarget.getLocation().add(0, 1, 0), soul, soul.intensity() * 4);
        }

        // Surge — spear only. Charge hits above a minimum horizontal speed inflict
        // Slowness II on the target. We never modify damage values.
        if (attacker != null
                && soul.ability().equals("surge")
                && Boolean.TRUE.equals(abilityEnabled.get("surge"))
                && e.getEntity() instanceof LivingEntity surgeTarget) {
            int minSpeed100 = abilityParams.getOrDefault("surge-min-speed", 20);
            Vector vel = attacker.getVelocity();
            double hSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
            if (hSpeed * 100 >= minSpeed100) {
                int slowDur = abilityParams.getOrDefault("surge-slowness-duration", 60);
                surgeTarget.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, slowDur, 1, false, false, false));
            }
        }

        // Vault — spear only. A charge hit at sufficient horizontal speed launches the
        // attacker upward using the impact as a pole-vault. Horizontal momentum is preserved.
        if (attacker != null
                && soul.ability().equals("vault")
                && Boolean.TRUE.equals(abilityEnabled.get("vault"))
                && !attacker.isInsideVehicle()) {
            int minSpeed100 = abilityParams.getOrDefault("vault-min-speed", 20);
            Vector vel = attacker.getVelocity();
            double hSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
            if (hSpeed * 100 >= minSpeed100) {
                long cooldownMs = abilityParams.getOrDefault("vault-cooldown", 3) * 1000L;
                long now        = System.currentTimeMillis();
                Long last       = vaultCooldowns.get(attacker.getUniqueId());
                if (last == null || now - last >= cooldownMs) {
                    vaultCooldowns.put(attacker.getUniqueId(), now);
                    double impulse = abilityParams.getOrDefault("vault-impulse", 90) / 100.0;
                    attacker.setVelocity(new Vector(vel.getX(), impulse, vel.getZ()));
                    spawnBurst(attacker.getLocation().add(0, 0.2, 0),
                               soul, soul.intensity() * 6);
                }
            }
        }

        // Blitz — leggings only. Sprint hits knock the target back harder and slow them.
        if (attacker != null
                && soul.ability().equals("blitz")
                && Boolean.TRUE.equals(abilityEnabled.get("blitz"))
                && attacker.isSprinting()) {
            // Extra knockback: push in the direction away from the attacker.
            double impulse = abilityParams.getOrDefault("blitz-knockback", 40) / 100.0;
            Vector push = e.getEntity().getLocation().toVector()
                           .subtract(attacker.getLocation().toVector())
                           .setY(0);
            if (push.lengthSquared() > 0.0001) {
                e.getEntity().setVelocity(
                    e.getEntity().getVelocity().add(push.normalize().multiply(impulse)));
            }
            // Slowness on the target — makes the tackle a genuine engagement-opener.
            if (e.getEntity() instanceof LivingEntity le) {
                int slowDur = abilityParams.getOrDefault("blitz-slowness-duration", 60);
                le.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, slowDur, 1, false, false, false));
                // amplifier=1 → Slowness II; silent (no particles/icon)
            }
        }
    }

    /**
     * Spawns a short-lived repeating task that applies an inward velocity impulse to
     * all nearby living entities every tick, simulating a gravity well at the impact point.
     *
     * <p>The well is anchored to the hit location at the moment of impact and does not
     * follow the struck entity. Pull strength accumulates per tick (like gravity), capped
     * at {@code 0.6 * strength} blocks/tick so entities don't rocket into the centre.
     * The attacker is excluded from the pull.
     *
     * <p>The task self-removes from {@link #activeWells} when its duration expires.
     * All active wells are force-cancelled by {@link #stop()} on reload or shutdown.
     *
     * @param center   the world-space anchor point of the well
     * @param attacker the player who struck — excluded from the pull
     * @param soul     the gravity well soul (used for particle bursts)
     */
    private void activateGravityWell(Location center, Player attacker, SoulData soul) {
        int    radius   = abilityParams.getOrDefault("gravitywell-radius",   5);
        int    duration = abilityParams.getOrDefault("gravitywell-duration", 12);
        double strength = abilityParams.getOrDefault("gravitywell-strength", 30) / 100.0;
        double maxSpeed = strength * 2.0; // velocity cap — prevents runaway accumulation

        int[] ticksLeft = {duration}; // array wrapper so lambda can mutate it
        BukkitTask[] taskRef = {null};

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (ticksLeft[0]-- <= 0) {
                taskRef[0].cancel();
                activeWells.remove(taskRef[0]);
                return;
            }

            // Spawn a tight inward particle ring each tick so the well is visible.
            spawnBurst(center, soul, soul.intensity() * 2);

            for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(nearby instanceof LivingEntity le)) continue;
                if (nearby.getUniqueId().equals(attacker.getUniqueId())) continue;
                if (le.isDead()) continue;

                // Inward unit vector from entity toward the centre, then scaled to impulse.
                Vector toward = center.toVector()
                                      .subtract(nearby.getLocation().toVector());
                if (toward.lengthSquared() < 0.0001) continue; // already at centre

                Vector dir = toward.normalize(); // unit direction; toward and dir are the same object
                Vector impulse = dir.clone().multiply(strength); // scaled impulse, dir stays unit
                Vector newVel  = nearby.getVelocity().add(impulse);

                // Cap only the inward component so tangential motion (e.g. from knockback)
                // is unaffected. Subtract any excess beyond maxSpeed.
                double inwardSpeed = newVel.dot(dir);
                if (inwardSpeed > maxSpeed) {
                    newVel.subtract(dir.clone().multiply(inwardSpeed - maxSpeed));
                }
                nearby.setVelocity(newVel);
            }
        }, 0L, 1L);

        activeWells.add(taskRef[0]);
    }

    /**
     * Larger kill burst when the killing blow came from a soul-tagged weapon.
     * Maces with the shatter ability also scatter the mob's item drops outward.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        PlayerSoulState state = playerStateCache.get(killer.getUniqueId());
        if (state == null || state.weapon() == null) return;

        SoulData soul = state.weapon();
        Location loc  = e.getEntity().getLocation().add(0, 1, 0);
        spawnBurst(loc, soul, soul.intensity() * 8);

        // Shatter — scatter item drops outward instead of piling at feet.
        if (soul.ability().equals("shatter")
                && Boolean.TRUE.equals(abilityEnabled.get("shatter"))
                && !e.getDrops().isEmpty()) {
            scatterDrops(e);
        }

        // Compendium — permanently record the killed mob type on the axe.
        // The kill set is stored as a pipe-delimited string of EntityType names in PDC.
        // Lore is rebuilt immediately so the new entry appears without cache delay.
        if (soul.ability().equals("compendium")
                && Boolean.TRUE.equals(abilityEnabled.get("compendium"))) {
            ItemStack axe = killer.getInventory().getItemInMainHand();
            if (axe.hasItemMeta()) {
                String mobName = e.getEntity().getType().name();
                Set<String> kills = readCompendium(axe);
                if (kills.add(mobName)) { // add() returns false if already present — skip lore rebuild
                    writeCompendium(axe, kills);
                    updateCompendiumLore(axe, kills, soul.tag());
                }
            }
        }

    }

    /**
     * Reads the compendium kill set from an axe's PDC.
     * Stored as a pipe-delimited string of {@link EntityType} names.
     * Returns a mutable {@link LinkedHashSet} preserving insertion order
     * so entries appear in the lore in the order they were first killed.
     */
    private Set<String> readCompendium(ItemStack item) {
        Set<String> result = new LinkedHashSet<>();
        if (!item.hasItemMeta()) return result;
        String raw = item.getItemMeta().getPersistentDataContainer()
                         .get(compendiumKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split("\\|")) {
            if (!entry.isBlank()) result.add(entry);
        }
        return result;
    }

    /**
     * Writes the compendium kill set back to the axe's PDC.
     */
    private void writeCompendium(ItemStack item, Set<String> kills) {
        if (!item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer()
            .set(compendiumKey, PersistentDataType.STRING, String.join("|", kills));
        item.setItemMeta(meta);
    }

    /**
     * Rebuilds the axe's visible lore from the current compendium kill set.
     *
     * <p>Each killed mob type occupies one lore line, formatted as a readable name:
     * {@code ▸ zombie · creeper · skeleton · enderman}
     * packed four per line to keep the tooltip compact.
     * The soul footer is always appended last.
     *
     * @param item    the axe item to update
     * @param kills   the current full kill set
     * @param soulTag the soul's display tag for the footer
     */
    private void updateCompendiumLore(ItemStack item, Set<String> kills, String soulTag) {
        if (!item.hasItemMeta()) return;
        var meta = item.getItemMeta();

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        // Pack kill entries four per lore line for a compact, scannable layout.
        List<String> formatted = kills.stream()
            .map(k -> k.toLowerCase().replace('_', ' '))
            .collect(java.util.stream.Collectors.toList());

        for (int i = 0; i < formatted.size(); i += 4) {
            String line = "▸ " + String.join("  ·  ",
                formatted.subList(i, Math.min(i + 4, formatted.size())));
            lore.add(net.kyori.adventure.text.Component.text(
                line, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }

        // Soul footer — separator then tagged name line.
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text(
            "───────────────────",
            net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.text(
            soulGlyph + "  " + soulTag + "  " + soulGlyph,
            soulTagColor)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Doubles and scatters all item drops from a shatter kill.
     * Each drop's amount is doubled before spawning, then given a random outward
     * velocity so items burst away from the mob's position instead of piling at its feet.
     * Item types and NBT are unchanged — only counts and positions differ.
     *
     * @param e the death event whose drops will be doubled and scattered
     */
    private void scatterDrops(EntityDeathEvent e) {
        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        e.getDrops().clear();               // prevent vanilla drop-at-feet

        Location origin = e.getEntity().getLocation().add(0, 0.5, 0);
        World world     = origin.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            // Double the stack amount — clone first so we don't mutate the original.
            ItemStack doubled = drop.clone();
            doubled.setAmount(drop.getAmount() * 2);
            Item item = world.dropItem(origin, doubled);
            // Random horizontal direction, fixed outward speed, small upward component.
            double angle = rng.nextDouble() * 2 * Math.PI;
            double speed = 0.2 + rng.nextDouble() * 0.25; // 0.20–0.45 blocks/tick
            item.setVelocity(new Vector(
                Math.cos(angle) * speed,
                0.25 + rng.nextDouble() * 0.15,  // slight upward arc
                Math.sin(angle) * speed
            ));
            item.setPickupDelay(20); // 1 second before the player can grab it
        }
    }

    /**
     * Begins tracking a projectile when it is launched from a soul-tagged weapon.
     *
     * <ul>
     *   <li><b>Trident</b> — soul tag is read from the trident item itself, since the
     *       trident is its own projectile.</li>
     *   <li><b>Arrows / spectral arrows</b> — soul tag is read from the shooter's main
     *       hand (bow) or off-hand (crossbow loaded off-hand).</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player shooter)) return;
        Projectile proj = e.getEntity();
        SoulData soul   = null;

        if (proj instanceof Trident trident) {
            // Soul PDC tag lives on the trident item itself.
            soul = getSoulFromPDC(trident.getItem());

        } else if (proj instanceof AbstractArrow) {
            // Soul tag is on the bow or crossbow that fired it.
            soul = getSoulFromPDC(shooter.getInventory().getItemInMainHand());
            if (soul == null)
                soul = getSoulFromPDC(shooter.getInventory().getItemInOffHand());
        }

        if (soul != null) {
            trackedProjectiles.put(proj.getUniqueId(), soul);
            // Volley — crossbow only. Spawn two additional arrows beside the original.
            if (soul.ability().equals("volley")
                    && proj instanceof AbstractArrow
                    && Boolean.TRUE.equals(abilityEnabled.get("volley"))) {
                spawnVolleyArrows(shooter, (AbstractArrow) proj, soul);
            }
        }
    }

    /**
     * Spawns two additional arrows flanking the original volley arrow.
     * Each extra arrow inherits the original's velocity with a small random lateral
     * offset so no two volleys look identical. All three arrows are tracked and will
     * produce trail/burst effects independently.
     *
     * <p>Extra arrows are fired as {@link Arrow} entities with damage set to match
     * the original, and {@code setPickupStatus(DISALLOWED)} so they can't be collected.
     *
     * @param shooter  the player who fired the crossbow
     * @param original the arrow Bukkit already spawned for this shot
     * @param soul     the volley soul (stored in trackedProjectiles for each extra arrow)
     */
    private void spawnVolleyArrows(Player shooter, AbstractArrow original, SoulData soul) {
        Vector base   = original.getVelocity();
        World world   = original.getWorld();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Build a perpendicular vector to spread arrows left/right of the shot direction.
        // Cross-product of velocity and world-up gives a horizontal lateral axis.
        Vector lateral = base.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (lateral.lengthSquared() < 0.001) {
            // Edge case: arrow fired straight up/down — use X axis as fallback.
            lateral = new Vector(1, 0, 0);
        }

        double baseSpread  = 0.08; // fixed lateral separation between arrows
        double randomRange = 0.04; // ± random jitter added per arrow per shot

        for (int i = 0; i < 2; i++) {
            // One arrow to each side, each with its own random jitter.
            double sign   = (i == 0) ? 1.0 : -1.0;
            double jitter = (rng.nextDouble() * 2 - 1) * randomRange;
            Vector offset = lateral.clone().multiply(sign * (baseSpread + jitter));
            Vector vel    = base.clone().add(offset);

            Arrow extra = world.spawnEntity(original.getLocation(), EntityType.ARROW, false,
                    entity -> {
                        Arrow a = (Arrow) entity;
                        a.setShooter(shooter);
                        a.setVelocity(vel);
                        a.setDamage(original instanceof Arrow oa ? oa.getDamage() : 2.0);
                        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        a.setCritical(original instanceof Arrow oa && oa.isCritical());
                    });
            trackedProjectiles.put(extra.getUniqueId(), soul);
        }
    }

    /**
     * Removes a projectile from the tracking map when it hits something.
     * Spawns a burst if the hit target was a block; entity hits are handled
     * by {@link #onEntityDamage}, which fires first.
     * Bow arrows with the Ricochet ability bounce to a secondary target on entity hit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent e) {
        SoulData soul = trackedProjectiles.remove(e.getEntity().getUniqueId());
        if (soul == null) return;

        if (e.getHitBlock() != null) {
            Location loc = e.getHitBlock().getLocation().add(0.5, 0.5, 0.5);
            spawnBurst(loc, soul, soul.intensity() * 3);
        }

        // Surveyor — crossbow bolt hits a block, spawns an invisible camera anchor
        // ArmorStand at the impact point, shifts the shooter's view to it, then
        // restores the view and removes the stand after the configured duration.
        if (soul.ability().equals("surveyor")
                && Boolean.TRUE.equals(abilityEnabled.get("surveyor"))
                && e.getHitBlock() != null
                && e.getEntity().getShooter() instanceof Player shooter) {

            int viewDuration = abilityParams.getOrDefault("surveyor-duration", 100); // ticks

            // Cancel any previous Surveyor stand for this player.
            cancelSurveyor(shooter);

            // Place the anchor at the bolt's final position, slightly above the block face.
            Location anchorLoc = e.getEntity().getLocation().clone().add(0, 0.1, 0);
            ArmorStand anchor = (ArmorStand) shooter.getWorld()
                    .spawnEntity(anchorLoc, EntityType.ARMOR_STAND);
            anchor.setVisible(false);
            anchor.setGravity(false);
            anchor.setInvulnerable(true);
            anchor.setAI(false);
            anchor.setSilent(true);
            anchor.setCollidable(false);
            anchor.setSmall(true);

            surveyorStands.put(shooter.getUniqueId(), anchor.getUniqueId());
            surveyorOriginalModes.put(shooter.getUniqueId(), shooter.getGameMode());

            // Temporarily switch the player to spectator so setSpectatorTarget works.
            // The original mode is stored in surveyorOriginalModes for correct restoration.
            shooter.setGameMode(org.bukkit.GameMode.SPECTATOR);
            shooter.setSpectatorTarget(anchor);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only restore if this stand is still the active one for this player.
                if (anchor.getUniqueId().equals(surveyorStands.get(shooter.getUniqueId()))) {
                    cancelSurveyor(shooter);
                } else if (!anchor.isDead()) {
                    anchor.remove(); // stand was already replaced — just clean up
                }
            }, viewDuration);
        }

        // Ricochet — bounce the arrow to a nearby second target on entity hit.
        if (soul.ability().equals("ricochet")
                && Boolean.TRUE.equals(abilityEnabled.get("ricochet"))
                && e.getHitEntity() instanceof LivingEntity hitEntity
                && e.getEntity() instanceof AbstractArrow originalArrow
                && e.getEntity().getShooter() instanceof Player shooter) {

            int    range     = abilityParams.getOrDefault("ricochet-range", 4);
            double damFrac   = abilityParams.getOrDefault("ricochet-damage", 60) / 100.0;
            double bounceDmg = (originalArrow instanceof Arrow a ? a.getDamage() : 2.0) * damFrac;

            // Find nearest living entity within range, excluding the hit entity and the shooter.
            LivingEntity secondary = null;
            double minDistSq = (double) range * range;
            for (Entity nearby : hitEntity.getNearbyEntities(range, range, range)) {
                if (!(nearby instanceof LivingEntity le)) continue;
                if (nearby.getUniqueId().equals(hitEntity.getUniqueId())) continue;
                if (nearby.getUniqueId().equals(shooter.getUniqueId())) continue;
                double dSq = hitEntity.getLocation().distanceSquared(nearby.getLocation());
                if (dSq < minDistSq) { minDistSq = dSq; secondary = le; }
            }
            if (secondary == null) return;

            // Spawn an untracked arrow aimed at the secondary target.
            final LivingEntity finalSecondary = secondary;
            final double       finalDmg       = bounceDmg;
            hitEntity.getWorld().spawnEntity(hitEntity.getLocation().add(0, 0.5, 0),
                    EntityType.ARROW, false, entity -> {
                        Arrow bounce = (Arrow) entity;
                        bounce.setShooter(shooter);
                        bounce.setDamage(finalDmg);
                        bounce.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        Vector dir = finalSecondary.getLocation().add(0, 0.5, 0).toVector()
                                        .subtract(hitEntity.getLocation().toVector())
                                        .normalize()
                                        .multiply(originalArrow.getVelocity().length());
                        bounce.setVelocity(dir);
                    });
            spawnBurst(hitEntity.getLocation().add(0, 1, 0), soul, soul.intensity() * 2);
        }
    }

    // ── PDC lookup ────────────────────────────────────────────────────────────

    /**
     * Magnetize — when a soul-tagged pickaxe breaks a block, pulls all dropped items
     * toward the player by applying an inward velocity to each {@link Item} entity.
     *
     * <p>{@link BlockDropItemEvent} provides the breaking player directly, so no cache
     * lookup is needed to correlate the break with the tool's soul.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent e) {
        if (e.getPlayer() == null) return;
        PlayerSoulState state = playerStateCache.get(e.getPlayer().getUniqueId());
        if (state == null || state.tool() == null) return;
        if (!state.tool().ability().equals("magnetize")) return;
        if (!Boolean.TRUE.equals(abilityEnabled.get("magnetize"))) return;

        double speed = abilityParams.getOrDefault("magnetize-speed", 40) / 100.0;
        Location playerLoc = e.getPlayer().getLocation().add(0, 1, 0); // aim at centre of mass

        for (Item item : e.getItems()) {
            Vector toward = playerLoc.toVector()
                                     .subtract(item.getLocation().toVector());
            double dist = toward.length();
            if (dist < 0.01) continue;
            item.setVelocity(toward.normalize().multiply(speed));
        }
    }

    /**
     * Scans outward from {@code origin} up to {@code range} blocks and returns the
     * ore block with the smallest Euclidean distance, or {@code null} if none.
     *
     * <p>Iterates shells by Chebyshev radius so we can stop the outer loop as soon as
     * a shell's minimum possible Euclidean distance exceeds the best distance already
     * found. Within each shell, all blocks are checked and the geometrically closest
     * one is tracked — this guarantees that a face-adjacent block at radius r is
     * preferred over a corner block at the same Chebyshev radius r√3.
     */
    private org.bukkit.block.Block findNearestOre(Location origin, int range) {
        World world = origin.getWorld();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        org.bukkit.block.Block best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int r = 1; r <= range; r++) {
            // Minimum possible Euclidean distance² for any block in this shell is r²
            // (a face-adjacent block). If that already exceeds our best, stop.
            if (r * r > bestDistSq) break;

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        // Only blocks on the surface of this Chebyshev shell.
                        if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) continue;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq >= bestDistSq) continue; // can't beat current best
                        org.bukkit.block.Block b = world.getBlockAt(ox + dx, oy + dy, oz + dz);
                        if (DOWSING_ORES.contains(b.getType())) {
                            best = b;
                            bestDistSq = distSq;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * Scans a full cube of radius {@code range} around {@code origin} and returns the
     * centre location of every ore block found. Used by the Resonance ability so the
     * entire vein shape is illuminated, not just the single nearest block.
     *
     * <p>At range 5 this scans at most 11³ = 1331 blocks — acceptable for an infrequent
     * block-break event, but the range should not be set above ~8 in config.
     *
     * @param origin the broken block's location (scan is centred here)
     * @param range  maximum Manhattan-cube distance to scan
     * @return mutable list of block-centre locations for each ore found; empty if none
     */
    private List<Location> findAllOresInRange(Location origin, int range) {
        List<Location> found = new ArrayList<>();
        World world = origin.getWorld();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    org.bukkit.block.Block b = world.getBlockAt(ox + dx, oy + dy, oz + dz);
                    if (DOWSING_ORES.contains(b.getType()))
                        found.add(b.getLocation().add(0.5, 0.5, 0.5));
                }
            }
        }
        return found;
    }

    /**
     * Spawns a short-lived repeating task that pulses particles at each ore location
     * so the player can read the shape of the vein.
     *
     * <p>Fires every 2 ticks for {@code durationTicks} total ticks. The task
     * self-removes from {@link #activeResonanceTasks} when done. All active resonance
     * tasks are force-cancelled by {@link #stop()}.
     *
     * @param oreLocs  block-centre locations of the ores to highlight
     * @param soul     the resonance soul (provides particle type)
     * @param durationTicks total ticks to keep pulsing
     */
    private void activateResonance(List<Location> oreLocs, SoulData soul, int durationTicks) {
        int[]        ticksLeft = {durationTicks};
        BukkitTask[] taskRef   = {null};

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksLeft[0] -= 2;
            if (ticksLeft[0] <= 0) {
                taskRef[0].cancel();
                activeResonanceTasks.remove(taskRef[0]);
                return;
            }
            for (Location loc : oreLocs) {
                loc.getWorld().spawnParticle(soul.particle(), loc, 3, 0.25, 0.25, 0.25, soul.extra());
            }
        }, 0L, 2L);

        activeResonanceTasks.add(taskRef[0]);
    }

    /**
     * Reads the block column from the clicked block downward to bedrock, collapses
     * consecutive identical materials into a single layer entry, and sends the result
     * to the player as an action bar message.
     *
     * <p>Format: {@code ▸ dirt (3) · stone (12) · iron_ore (1) · deepslate (8) · bedrock (1)}
     * Each entry is the material name followed by its run-length in parentheses.
     * Air layers are skipped — they represent caves, which are informative by their
     * absence (a gap in the readout is itself a signal).
     *
     * @param player the player to send the readout to
     * @param origin the location of the right-clicked block (scan starts here)
     */
    private void fireBore(Player player, Location origin) {
        World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int startY = origin.getBlockY();
        int minY = world.getMinHeight();

        // Build a run-length-encoded list of (material, count) pairs, skipping air.
        List<String> layers = new ArrayList<>();
        Material current = null;
        int run = 0;

        for (int y = startY; y >= minY; y--) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR) {
                // Flush current run before the gap, then emit a cave marker.
                if (current != null && run > 0) {
                    layers.add(formatLayer(current, run));
                    current = null; run = 0;
                }
                // Collapse consecutive air into a single "cave" marker.
                if (layers.isEmpty() || !layers.get(layers.size() - 1).startsWith("cave")) {
                    layers.add("cave");
                }
                continue;
            }
            if (m == current) {
                run++;
            } else {
                if (current != null && run > 0) layers.add(formatLayer(current, run));
                current = m;
                run = 1;
            }
        }
        if (current != null && run > 0) layers.add(formatLayer(current, run));

        String readout = layers.isEmpty()
            ? "▸ nothing below"
            : "▸ " + String.join(" · ", layers);

        player.sendActionBar(net.kyori.adventure.text.Component.text(
            readout, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
    }

    /** Formats a single bore layer entry: {@code "iron_ore (3)"}. */
    private static String formatLayer(Material m, int count) {
        return m.name().toLowerCase().replace('_', ' ') + " (" + count + ")";
    }

    /** Immutable value type for a single witness journal entry. */
    private record WitnessEntry(String name, int count, int timeBracket,
                                int healthBracket, String heldItem) {}

    /**
     * Deserialises the witness journal from the sword's PDC.
     * Format: {@code uuid§name§count§timeBracket§healthBracket§heldItem} per entry,
     * entries delimited by {@code |}. Returns a mutable {@link LinkedHashMap} so
     * insertion order (= chronological order) is preserved for cap enforcement.
     */
    private LinkedHashMap<String, WitnessEntry> readWitnessJournal(ItemStack item) {
        LinkedHashMap<String, WitnessEntry> result = new LinkedHashMap<>();
        if (!item.hasItemMeta()) return result;
        String raw = item.getItemMeta().getPersistentDataContainer()
                         .get(witnessKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split("\\|")) {
            String[] p = entry.split("§", -1);
            if (p.length < 6) continue;
            try {
                result.put(p[0], new WitnessEntry(
                    p[1],
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]),
                    p[5]
                ));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /**
     * Serialises the witness journal to the sword's PDC.
     */
    private void writeWitnessJournal(ItemStack item,
                                     LinkedHashMap<String, WitnessEntry> journal) {
        if (!item.hasItemMeta()) return;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, WitnessEntry> e : journal.entrySet()) {
            if (!first) sb.append('|');
            WitnessEntry we = e.getValue();
            sb.append(e.getKey()).append('§')
              .append(we.name()).append('§')
              .append(we.count()).append('§')
              .append(we.timeBracket()).append('§')
              .append(we.healthBracket()).append('§')
              .append(we.heldItem());
            first = false;
        }
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer()
            .set(witnessKey, PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
    }

    /**
     * Rebuilds the sword's visible lore from the current journal state.
     *
     * <p>Detail tiers unlock progressively as encounter count grows:
     * <ul>
     *   <li>1+  — name only:              {@code ▸ Notch}</li>
     *   <li>3+  — adds time of day:       {@code ▸ Notch · at dusk}</li>
     *   <li>7+  — adds health bracket:    {@code ▸ Notch · at dusk · wounded}</li>
     *   <li>15+ — adds held item:         {@code ▸ Notch · at dusk · wounded · iron sword in hand}</li>
     * </ul>
     * Encounter count is appended in parentheses so rarity is always visible.
     */
    private void updateWitnessLore(ItemStack item,
                                   LinkedHashMap<String, WitnessEntry> journal,
                                   String soulTag) {
        if (!item.hasItemMeta()) return;
        var meta = item.getItemMeta();

        String[] timeLabels   = {"at dawn", "by day", "at dusk", "at night"};
        String[] healthLabels = {"in good health", "wounded", "nearly dead"};

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (WitnessEntry we : journal.values()) {
            StringBuilder line = new StringBuilder("▸ ").append(we.name());
            if (we.count() >= 3)
                line.append(" · ").append(timeLabels[Math.min(we.timeBracket(), 3)]);
            if (we.count() >= 7)
                line.append(" · ").append(healthLabels[Math.min(we.healthBracket(), 2)]);
            if (we.count() >= 15 && !we.heldItem().isBlank())
                line.append(" · ").append(we.heldItem()).append(" in hand");
            line.append("  (").append(we.count()).append(')');

            lore.add(net.kyori.adventure.text.Component.text(
                line.toString(),
                net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }

        // Always append the soul footer at the very bottom so it persists across
        // every journal update cycle, matching the footer built by applyArtifactMeta.
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(net.kyori.adventure.text.Component.text(
            "───────────────────",
            net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(net.kyori.adventure.text.Component.text(
            soulGlyph + "  " + soulTag + "  " + soulGlyph,
            soulTagColor)
            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads the breadcrumb location list from a chestplate's PDC.
     * Returns an empty mutable list if no crumbs have been recorded yet.
     * Each entry is an {@code int[3]} of block coordinates: {@code [x, y, z]}.
     */
    private List<int[]> readBreadcrumbs(ItemStack item) {
        List<int[]> result = new ArrayList<>();
        if (!item.hasItemMeta()) return result;
        String raw = item.getItemMeta().getPersistentDataContainer()
                         .get(breadcrumbKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(",");
            if (parts.length != 3) continue;
            try {
                result.add(new int[]{
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
                });
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /**
     * Writes a breadcrumb location list to a chestplate's PDC.
     * Serialises as {@code "x1,y1,z1;x2,y2,z2;..."}.
     */
    private void writeBreadcrumbs(ItemStack item, List<int[]> crumbs) {
        if (!item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        if (crumbs.isEmpty()) {
            meta.getPersistentDataContainer().remove(breadcrumbKey);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < crumbs.size(); i++) {
                if (i > 0) sb.append(';');
                int[] c = crumbs.get(i);
                sb.append(c[0]).append(',').append(c[1]).append(',').append(c[2]);
            }
            meta.getPersistentDataContainer()
                .set(breadcrumbKey, PersistentDataType.STRING, sb.toString());
        }
        item.setItemMeta(meta);
    }

    /**
     * Spawns a short-lived repeating task that projects particle pillars at each
     * breadcrumb location for 8 seconds (160 ticks, pulsing every 2 ticks).
     * Each pillar spans 10 blocks upward from the stored Y so it's visible even if
     * the player is at a different elevation. The task self-removes when done.
     *
     * @param player the player who triggered the projection (for world reference)
     * @param crumbs the list of block coordinates to project
     * @param soul   the breadcrumb soul (provides particle type)
     */
    private void projectBreadcrumbs(Player player, List<int[]> crumbs, SoulData soul) {
        int[]        ticksLeft = {160};
        BukkitTask[] taskRef   = {null};
        World        world     = player.getWorld();

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksLeft[0] -= 2;
            if (ticksLeft[0] <= 0) {
                taskRef[0].cancel();
                activeBreadcrumbTasks.remove(taskRef[0]);
                return;
            }
            for (int[] c : crumbs) {
                // Vertical pillar from stored Y up 10 blocks — visible across elevation changes.
                for (int dy = 0; dy <= 10; dy++) {
                    world.spawnParticle(soul.particle(),
                        c[0] + 0.5, c[1] + dy, c[2] + 0.5,
                        1, 0.1, 0.1, 0.1, soul.extra());
                }
            }
        }, 0L, 2L);

        activeBreadcrumbTasks.add(taskRef[0]);
    }

    /**
     * Fires the Dowsing ability: scans for the nearest ore within range, then draws a
     * particle beam from the player's eye position to the ore's centre. If no ore is
     * found within range, no particles are spawned — the absence of a beam is itself
     * informative.
     *
     * @param player the player who broke the block
     * @param origin the location of the broken block (scan starts here)
     * @param range  maximum search distance in blocks
     * @param soul   the dowsing soul (for particle type)
     */
    private void fireDowsing(Player player, Location origin, int range, SoulData soul) {
        org.bukkit.block.Block ore = findNearestOre(origin, range);
        if (ore == null) return;
        Location end = ore.getLocation().add(0.5, 0.5, 0.5);
        drawParticleLine(player.getEyeLocation(), end, soul, 0.5);
        spawnBurst(end, soul, soul.intensity() * 3);
    }

    /**
     * Draws a line of particles between two locations at the given step interval.
     * Used by Dowsing (shovel ore beam) and Precognition (mob-targeting alert line).
     *
     * @param from     start of the line
     * @param to       end of the line
     * @param soul     soul whose particle type to use
     * @param stepSize distance in blocks between each particle
     */
    private void drawParticleLine(Location from, Location to, SoulData soul, double stepSize) {
        Vector dir  = to.toVector().subtract(from.toVector());
        double dist = dir.length();
        if (dist < 0.1) return;
        dir.normalize().multiply(stepSize);
        Location cur  = from.clone();
        World    world = from.getWorld();
        int steps = (int) (dist / stepSize);
        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(soul.particle(), cur, 1, 0, 0, 0, 0);
            cur.add(dir);
        }
    }

    /**
     * Activates a leash on a struck entity, tethering it near the impact point.
     * A spring-like force is applied every 2 ticks when the entity strays beyond
     * the leash radius, pulling it back toward the anchor without a jarring teleport.
     * The mob can still attack but cannot chase or flee.
     *
     * <p>The task self-removes from {@link #activeLeashes} when the duration expires
     * or the entity dies. All active leashes are force-cancelled by {@link #stop()}.
     *
     * @param target the entity to tether
     * @param anchor the world-space point to tether to (the hit location)
     */
    private void activateLeash(LivingEntity target, Location anchor) {
        // Prevent stacking multiple tether tasks on the same entity.
        if (!activeLeashTargets.add(target.getUniqueId())) return;

        int    duration  = abilityParams.getOrDefault("leash-duration", 80);
        int    radius    = abilityParams.getOrDefault("leash-radius", 3);
        double radiusSq  = (double) radius * radius;

        int[]        ticksLeft = {duration};
        BukkitTask[] taskRef   = {null};

        taskRef[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticksLeft[0] -= 2;
            if (ticksLeft[0] <= 0 || target.isDead() || !target.isValid()) {
                taskRef[0].cancel();
                activeLeashes.remove(taskRef[0]);
                activeLeashTargets.remove(target.getUniqueId());
                return;
            }
            double distSq = target.getLocation().distanceSquared(anchor);
            if (distSq > radiusSq) {
                Vector toward = anchor.toVector().subtract(target.getLocation().toVector());
                double excess = Math.sqrt(distSq) - radius;
                // Spring force: stronger the further the entity has strayed, capped at 0.6.
                double pullSpeed = Math.min(0.6, excess * 0.25);
                target.setVelocity(target.getVelocity().add(toward.normalize().multiply(pullSpeed)));
            }
        }, 0L, 2L);

        activeLeashes.add(taskRef[0]);
    }

    // ── PDC lookup ────────────────────────────────────────────────────────────

    /**
     * Reads the soul ID from an item's PDC and resolves it against the active soul map.
     * Returns {@code null} if the item is null, has no meta, has no soul tag, or the
     * soul ID is not present in the active map.
     */
    private SoulData getSoulFromPDC(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(soulKey, PersistentDataType.STRING);
        return id != null ? souls.get(id) : null;
    }

    // ── Material classification ───────────────────────────────────────────────

    /**
     * Returns {@code true} if the material is a weapon type.
     * Axes are classified as weapons (combat-primary). This means axe soul effects
     * trigger on combat events, not on block events.
     */
    private static boolean isWeapon(Material m) {
        String n = m.name();
        return n.contains("SWORD")   || n.contains("MACE")
            || n.contains("BOW")     // covers both BOW and CROSSBOW
            || n.contains("TRIDENT") || n.contains("SPEAR")
            || n.contains("AXE");
    }

    /**
     * Returns {@code true} if the material is a dedicated tool type.
     * Axes are excluded (classified as weapons above).
     */
    private static boolean isTool(Material m) {
        String n = m.name();
        return n.contains("PICKAXE") || n.contains("SHOVEL") || n.contains("HOE");
    }
}
