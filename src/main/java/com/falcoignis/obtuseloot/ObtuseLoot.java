package com.falcoignis.obtuseloot;

import com.falcoignis.obtuseloot.data.SoulData;
import com.falcoignis.obtuseloot.obtuseengine.ObtuseEngine;
import com.falcoignis.obtuseloot.lore.Histories;
import com.falcoignis.obtuseloot.lore.Observations;
import com.falcoignis.obtuseloot.lore.Epithets;
import com.falcoignis.obtuseloot.lore.Secrets;
import com.falcoignis.obtuseloot.names.Generic;
import com.falcoignis.obtuseloot.names.Prefixes;
import com.falcoignis.obtuseloot.names.Suffixes;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ObtuseLoot — Procedural loot generation with soul-bound particle effects.
 *
 * <h3>Threading model</h3>
 * All Bukkit API calls and data mutations run on the main server thread.
 * {@link #loadAllData()} asserts this at entry. The only state shared across
 * threads is {@code pendingChests}, which uses a simple volatile flag per entry
 * via a Set backed by identity comparison on the main thread (no concurrent reads).
 *
 * <h3>Package layout</h3>
 * <pre>
 *   com.falcoignis.obtuseloot            — plugin main class (this file)
 *   com.falcoignis.obtuseloot.data       — SoulData, PlayerSoulState records
 *   com.falcoignis.obtuseloot.obtuseengine — ObtuseEngine (particle + event logic)
 *   com.falcoignis.obtuseloot.lore       — Observations, Histories, Secrets default lists
 *   com.falcoignis.obtuseloot.names      — Prefixes, Suffixes, Generic default lists
 * </pre>
 */
public class ObtuseLoot extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    // ─────────────────────────────────────────────────────────────────────────
    // Static constants
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Every material eligible for artifact generation.
     * Allocated once at class-load; never mutated at runtime.
     */
    private static final Material[] ARTIFACT_MATERIALS = {
        // ── Weapons ───────────────────────────────────────────────────────────
        Material.IRON_SWORD,    Material.DIAMOND_SWORD,  Material.NETHERITE_SWORD,
        Material.MACE,
        Material.COPPER_SPEAR,  Material.IRON_SPEAR,     Material.DIAMOND_SPEAR,
        Material.NETHERITE_SPEAR,
        Material.TRIDENT,       Material.BOW,            Material.CROSSBOW,
        Material.IRON_AXE,      Material.DIAMOND_AXE,    Material.NETHERITE_AXE,

        // ── Armor ─────────────────────────────────────────────────────────────
        Material.IRON_HELMET,       Material.DIAMOND_HELMET,   Material.NETHERITE_CHESTPLATE,
        Material.IRON_LEGGINGS,     Material.DIAMOND_LEGGINGS,
        Material.IRON_BOOTS,        Material.DIAMOND_BOOTS,
        Material.ELYTRA,

        // ── Tools ─────────────────────────────────────────────────────────────
        Material.IRON_PICKAXE,  Material.DIAMOND_PICKAXE,  Material.NETHERITE_PICKAXE,
        Material.IRON_SHOVEL,   Material.DIAMOND_SHOVEL,   Material.NETHERITE_SHOVEL,
        Material.IRON_HOE,      Material.DIAMOND_HOE,      Material.NETHERITE_HOE,

        // ── Utility ───────────────────────────────────────────────────────────
        Material.SHIELD,         Material.FISHING_ROD,    Material.FLINT_AND_STEEL,
        Material.SHEARS,         Material.SADDLE,         Material.NAME_TAG,
        Material.LEAD,           Material.COMPASS,        Material.CLOCK,
        Material.LEATHER_HORSE_ARMOR, Material.IRON_HORSE_ARMOR,
        Material.GOLDEN_HORSE_ARMOR,  Material.DIAMOND_HORSE_ARMOR,
        Material.ENDER_PEARL,    Material.BLAZE_ROD,      Material.BUCKET,
        Material.ECHO_SHARD,     Material.EXPERIENCE_BOTTLE,
        Material.GOLDEN_APPLE,   Material.BOOK,

        // ── Food ─────────────────────────────────────────────────────────────
        Material.APPLE,          Material.BREAD,          Material.COOKIE,
        Material.PUMPKIN_PIE,    Material.CAKE,           Material.HONEY_BOTTLE,
        Material.DRIED_KELP,     Material.MELON_SLICE,    Material.BAKED_POTATO,
        Material.SWEET_BERRIES,  Material.GLOW_BERRIES,   Material.CHORUS_FRUIT,
        Material.MUSHROOM_STEW,  Material.RABBIT_STEW,    Material.BEETROOT_SOUP,
        Material.SUSPICIOUS_STEW,
        Material.BEEF,           Material.COOKED_BEEF,
        Material.PORKCHOP,       Material.COOKED_PORKCHOP,
        Material.CHICKEN,        Material.COOKED_CHICKEN,
        Material.MUTTON,         Material.COOKED_MUTTON,
        Material.RABBIT,         Material.COOKED_RABBIT,
        Material.COD,            Material.COOKED_COD,
        Material.SALMON,         Material.COOKED_SALMON,

        // ── Mob heads ─────────────────────────────────────────────────────────
        Material.CREEPER_HEAD,         Material.ZOMBIE_HEAD,
        Material.SKELETON_SKULL,       Material.WITHER_SKELETON_SKULL,
        Material.PIGLIN_HEAD,

        // ── Flowers ───────────────────────────────────────────────────────────
        Material.DANDELION,      Material.POPPY,          Material.BLUE_ORCHID,
        Material.ALLIUM,         Material.AZURE_BLUET,    Material.RED_TULIP,
        Material.ORANGE_TULIP,   Material.WHITE_TULIP,    Material.PINK_TULIP,
        Material.OXEYE_DAISY,    Material.CORNFLOWER,     Material.LILY_OF_THE_VALLEY,
        Material.WITHER_ROSE,    Material.SUNFLOWER,      Material.LILAC,
        Material.ROSE_BUSH,      Material.PEONY,          Material.TORCHFLOWER,
        Material.PITCHER_PLANT,  Material.SPORE_BLOSSOM,  Material.AZALEA,
        Material.FLOWERING_AZALEA,

        // ── Saplings & propagules ─────────────────────────────────────────────
        Material.OAK_SAPLING,    Material.BIRCH_SAPLING,  Material.SPRUCE_SAPLING,
        Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
        Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE, Material.BAMBOO_SAPLING,

        // ── Fungi & aquatic plants ────────────────────────────────────────────
        Material.BROWN_MUSHROOM,  Material.RED_MUSHROOM,
        Material.CRIMSON_FUNGUS,  Material.WARPED_FUNGUS,
        Material.NETHER_WART,     Material.LILY_PAD,
        Material.SEA_PICKLE,      Material.BAMBOO,

        // ── Candles ───────────────────────────────────────────────────────────
        Material.CANDLE,
        Material.WHITE_CANDLE,      Material.ORANGE_CANDLE,    Material.MAGENTA_CANDLE,
        Material.LIGHT_BLUE_CANDLE, Material.YELLOW_CANDLE,    Material.LIME_CANDLE,
        Material.PINK_CANDLE,       Material.GRAY_CANDLE,      Material.LIGHT_GRAY_CANDLE,
        Material.CYAN_CANDLE,       Material.PURPLE_CANDLE,    Material.BLUE_CANDLE,
        Material.BROWN_CANDLE,      Material.GREEN_CANDLE,     Material.RED_CANDLE,
        Material.BLACK_CANDLE
    };

    /**
     * Fast O(1) lookup set — used by enchant and grindstone handlers to check
     * whether a held item's material is eligible for ObtuseLoot conversion.
     * Built once from ARTIFACT_MATERIALS at class-load time.
     */
    private static final Set<Material> ARTIFACT_MATERIAL_SET =
        Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(ARTIFACT_MATERIALS)));

    // ─────────────────────────────────────────────────────────────────────────
    // Instance fields
    // ─────────────────────────────────────────────────────────────────────────

    private NamespacedKey soulKey;
    private NamespacedKey generatedKey;
    /**
     * Marker key written to every ObtuseLoot artifact, regardless of whether it
     * received a soul. Used by the grindstone strip handler so that soulless
     * artifacts (Common items, items that lost the soul probability roll) are still
     * recognised and stripped correctly.
     */
    private NamespacedKey artifactKey;

    // All collections below are accessed exclusively on the main server thread.
    private final Map<String, List<String>> dictionaries = new HashMap<>();
    private final Map<String, SoulData>     activeSouls  = new HashMap<>();
    // Replaced atomically on each reload; always an unmodifiable snapshot so
    // async particle threads can read individual entries without data races.
    private volatile Map<String, Boolean>   abilityEnabled = Collections.emptyMap();
    private final List<String>  prefixes     = new ArrayList<>();
    private final List<String>  suffixes     = new ArrayList<>();
    /**
     * Suffix pool with all deity/entity "of [Name]" suffixes removed.
     * Pre-filtered in {@link #loadAllData()} from {@link #suffixes} so that the
     * deity-prefix check in {@link #applyArtifactMeta} requires no stream allocation.
     * Rebuilt whenever suffixes are reloaded.
     */
    private final List<String>  nonDeitySuffixes = new ArrayList<>();

    /**
     * Deity and named-entity prefixes that have a matching "of [Name]" suffix.
     * When one of these is drawn as the prefix, deity suffixes are excluded from
     * the suffix pool to prevent "Fenrir of Hecate"-style double-entity names.
     */
    private static final Set<String> DEITY_PREFIXES = Set.of(
            "Abaddon",
            "Apophis",
            "Arawn",
            "Arkham",
            "Asmodeus",
            "Azathoth",
            "Azazel",
            "Baphomet",
            "Beelzebub",
            "Cernunnos",
            "Chernobog",
            "Dagon",
            "Durga",
            "Fenrir",
            "Fujin",
            "Gashadokuro",
            "Hecate",
            "Helios",
            "Herobrine",
            "Huginn",
            "Indra",
            "Ishtar",
            "Izanami",
            "Jorogumo",
            "Junimo",
            "Kali",
            "Kelpie",
            "Khonsu",
            "Kitsune",
            "Krobus",
            "Malphas",
            "Mammon",
            "Marduk",
            "Marzanna",
            "Moloch",
            "Morpheus",
            "Muninn",
            "Nemesis",
            "Nuckelavee",
            "Nyx",
            "Oni",
            "Pazuzu",
            "Persephone",
            "Perun",
            "Plantera",
            "Raijin",
            "Ravana",
            "Ryujin",
            "Samael",
            "Sekhmet",
            "Selene",
            "Skadi",
            "Sobek",
            "Stardrop",
            "Strigoi",
            "Susanoo",
            "Svarog",
            "Tengu",
            "Thanatos",
            "Thoth",
            "Tiamat",
            "Tlaloc",
            "Tsukuyomi",
            "Veles",
            "Wendigo",
            "Yama");

    /**
     * Pattern that matches the deity/entity "of [Name]" suffixes.
     * Compiled once at class load — used during name assembly to filter the
     * suffix pool whenever a deity prefix has been selected.
     */
    private static final java.util.regex.Pattern DEITY_SUFFIX_PATTERN =
            java.util.regex.Pattern.compile(
                    "^of (?:the )?(?:" +
                    String.join("|", DEITY_PREFIXES) +
                    ")$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private final List<String>  observations = new ArrayList<>();
    private final List<String>  histories    = new ArrayList<>();
    private final List<String>  secrets      = new ArrayList<>();
    private final List<String>  epithets     = new ArrayList<>();
    private final List<SoulData> soulList    = new ArrayList<>();

    /**
     * Tracks chests that have a loot-generation task in flight.
     * Key format: {@code "worldName|x|y|z"} — "|" cannot appear in Bukkit world names.
     * Main-thread-only; HashSet is sufficient.
     */
    private final Set<String> pendingChests = new HashSet<>();

    private ObtuseEngine soulEngine;

    // Volatile so they are safely visible in the runTask lambda without requiring
    // a full synchronized block. Snapshotted into locals at the top of every method
    // that reads them in a loop to avoid repeated volatile barriers.
    private volatile boolean lootEnabled;         // master switch for chest loot injection

    private volatile double  baseLootChance;
    private volatile double  multiItemDecay;
    private volatile double  suffixChance;
    private volatile double  proceduralLoreChance;
    private volatile int     maxItemsPerChest;
    private volatile boolean useBoldNames;
    private volatile Rarity  minSoulRarity;
    private volatile boolean enchantConvert;   // apply ObtuseLoot when enchanting an eligible item
    private volatile boolean grindstoneStrip;  // strip ObtuseLoot data when using a grindstone
    // Vanilla name chance — chance that a non-generic item uses its Minecraft material name
    // (e.g. "Diamond Sword") instead of a curated category name.
    // Still receives prefix, optional suffix, and procedural lore.
    private volatile double vanillaNameChance;
    private final Map<String, Double> vanillaNameChanceOverrides = new HashMap<>();

    // Display settings loaded from config — all configurable.
    private volatile NamedTextColor nameColor;      // item name color
    private volatile NamedTextColor loreTextColor;  // procedural lore text color
    private volatile NamedTextColor soulTagColor;   // soul tag text color
    private volatile boolean        showDivider;    // show rarity divider line in tooltip
    // loreSeparator removed — lore fragments are now rendered on separate lines
    private volatile String         dividerString;  // the actual divider bar characters
    private volatile String         rarityGlyph;    // glyph flanking the rarity label
    private volatile String         soulGlyph;      // glyph prefixing the soul tag

    // Rarity weights — loaded from rarity-weights config section; indexed by Rarity.ordinal().
    private volatile int[] rarityWeights     = {50, 30, 15, 4, 1};
    private volatile int   rarityWeightTotal = 100;
    private volatile int   listPageSize      = 20;  // /obtuseloot list <list> [page] page size

    /**
     * Pre-computed per-category material arrays, built once in {@link #loadAllData()}.
     * Keys are category IDs (e.g. {@code "swords"}, {@code "generic"}); values are the
     * sub-array of {@link #ARTIFACT_MATERIALS} that belong to that category.
     * Rebuilt on every reload so adding new categories to category files is supported.
     * Main-thread-only — no volatile needed.
     */
    private Map<String, Material[]> categoryMaterials = new HashMap<>();

    /**
     * Relative weight of the generic material bucket vs. each named category.
     * Loaded from {@code loot.generic-weight} in config.yml.
     * At the default of 8: generic ≈ 33 %, each named category ≈ 4.2 %
     * (there are 16 named categories, each with weight 1).
     */
    private volatile int genericWeight = 8;

    /**
     * Snapshot of named (non-generic) category IDs that have at least one material in
     * {@link #ARTIFACT_MATERIALS}. Rebuilt in {@link #loadAllData()} alongside
     * {@link #categoryMaterials} so {@link #generateArtifact()} pays no allocation cost
     * on the hot path.
     */
    private volatile List<String> namedCategoryIds = List.of();
    /** Size of {@link #namedCategoryIds}; cached to avoid repeated volatile list .size() reads. */
    private volatile int namedCategoryCount = 0;

    // Per-rarity soul chance — indexed by Rarity.ordinal().
    // COMMON (index 0) is unused as it is typically below min-rarity.
    private volatile double[] soulChanceByRarity = {0.0, 0.40, 0.65, 0.90, 1.0};
    private volatile boolean allowMultiOpen = false;
    private volatile boolean showLoreGap    = true;
    private volatile boolean showSoulGap    = true;

    // ── Shield pattern fields ─────────────────────────────────────────────────
    // patternTypes is populated in loadAllData() from Registry.BANNER_PATTERN so
    // Bukkit's registry is guaranteed to be initialized before we read it.
    private volatile PatternType[] patternTypes       = new PatternType[0];
    private static final DyeColor[] DYE_COLORS        = DyeColor.values();
    private volatile boolean shieldPatternEnabled     = true;
    private volatile int     shieldMinLayers          = 1;
    private volatile int     shieldMaxLayers          = 4;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        this.soulKey      = new NamespacedKey(this, "soul_effect");
        this.generatedKey = new NamespacedKey(this, "chest_generated");
        this.artifactKey  = new NamespacedKey(this, "artifact");
        this.soulEngine = new ObtuseEngine(this, soulKey);

        initDefaultConfig();
        setupFolders();
        initDefaultFiles();
        loadAllData();

        engine = new ObtuseEngine(this);
        engine.initialize();

        getLogger().info("ObtuseLoot engine initialized.");
    }

    @Override
    public void onDisable() {
        soulEngine.stop();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes a commented config.yml on first run without needing a bundled resource.
     * YamlConfiguration strips comments on save, so we write raw text here.
     */
    private void initDefaultConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            String content = String.join("\n",
                                "# ───────────────────────────────────────────────────────────────────────────",
                "# ObtuseLoot — config.yml",
                "# Changes take effect after /obtuseloot reload  (permission: obtuseloot.admin)",
                "#",
                "# Permission reference:",
                "#   obtuseloot.admin     — reload",
                "#   obtuseloot.use       — info, souls",
                "#   obtuseloot.give      — give",
                "#   obtuseloot.givesoul  — givesoul",
                "#   obtuseloot.convert   — convert",
                "#   obtuseloot.inspect   — inspect",
                "#   obtuseloot.clearitem — clearitem",
                "#   obtuseloot.edit      — add / remove / list (all lists)",
                "#   obtuseloot.edit.prefixes   — prefixes list only",
                "#   obtuseloot.edit.suffixes   — suffixes list only",
                "#   obtuseloot.edit.lore       — observations / histories / secrets",
                "#   obtuseloot.edit.categories — all category name files",
                "# ───────────────────────────────────────────────────────────────────────────",
                "",
                "# ── Loot generation ──────────────────────────────────────────────────────────",
                "# Controls what gets injected into chests and how often.",
                "# Permission to change all settings in this section: obtuseloot.admin",
                "loot:",
                "  # Master switch — disables all artifact injection into chests when false.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  enabled: true",
                "",
                "  # Probability that a single artifact slot is filled each time a chest is opened.",
                "  # The first slot rolls at this value; each additional slot multiplies by",
                "  # multi-item-decay, so slot 2 = item-chance × decay, slot 3 = item-chance × decay².",
                "  # Valid:   0.0–1.0  (0.0 = never, 1.0 = every chest opening)",
                "  # Default: 0.75",
                "  item-chance: 0.75",
                "",
                "  # Relative weight of the generic item bucket compared to each named category.",
                "  # There are 16 named categories each with weight 1; this value is the weight",
                "  # assigned to the generic pool (flowers, food, candles, heads, saplings, etc.).",
                "  #   P(generic)       = generic-weight / (16 + generic-weight)",
                "  #   P(any named cat) = 1              / (16 + generic-weight)",
                "  # At the default of 8:  generic ≈ 33 %,  each named category ≈ 4.2 %",
                "  # Valid:   integer ≥ 1",
                "  # Default: 8",
                "  generic-weight: 8",
                "",
                "  # After each item placed in a chest, the remaining chance is multiplied by this.",
                "  # Controls how quickly multi-item drops fall off.",
                "  # Valid:   0.0–1.0  (0.0 = at most one item per chest, 1.0 = no decay)",
                "  # Default: 0.45",
                "  multi-item-decay: 0.45",
                "",
                "  # Hard cap on how many artifacts can appear in a single chest opening.",
                "  # The decay curve makes high values rarely reachable at default settings.",
                "  # Valid:   integer ≥ 1",
                "  # Default: 4",
                "  max-items-per-chest: 4",
                "",
                "  # Probability that a generated item's name includes a suffix (e.g. of the Void).",
                "  # Applies to both named-category and generic items.",
                "  # Valid:   0.0–1.0  (0.0 = never, 1.0 = always)",
                "  # Default: 0.50",
                "  suffix-chance: 0.50",
                "",
                "  # Probability that a named item receives a randomly composed lore line",
                "  # (observation, history fragment, or secret drawn from the lore files).",
                "  # Does not apply to generic items.",
                "  # Valid:   0.0–1.0  (0.0 = never, 1.0 = always)",
                "  # Default: 0.85",
                "  procedural-lore-chance: 0.85",
                "",
                "  souls:",
                "    # Minimum rarity tier an item must roll before a soul can be assigned.",
                "    # Items below this threshold never receive soul effects.",
                "    # Valid:   COMMON | RARE | EPIC | LEGENDARY | MYTHIC",
                "    # Default: RARE",
                "    min-rarity: RARE",
                "",
                "    # Per-rarity probability that a qualifying item receives a soul.",
                "    # Each value is checked independently after min-rarity is met,",
                "    # so rarer items have a significantly higher soul rate.",
                "    # Valid:   0.0–1.0  (0.0 = never, 1.0 = always)",
                "    # Default: rare 0.40 | epic 0.65 | legendary 0.90 | mythic 1.0",
                "    soul-chance-rare:      0.40",
                "    soul-chance-epic:      0.65",
                "    soul-chance-legendary: 0.90",
                "    soul-chance-mythic:    1.00",
                "  # \u2500\u2500 Soul abilities \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
                "  # Each ability can be disabled independently without removing its soul from souls.yml.",
                "  # Abilities are slot-restricted: they only trigger on the item type they were designed for.",
                "  # Permission to change all settings in this section: obtuseloot.admin",
                "  soul-abilities:",
                "",
                "    # Lantern Soul (boots) \u2014 places a temporary light block (level 15) at the",
                "    # player's feet while walking. The light fades automatically after ~5 seconds.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    lantern-enabled: true",
                "",
                "    # Bloom Soul (boots) \u2014 places a temporary flower at the player's feet while",
                "    # walking. The flower wilts automatically after ~5 seconds.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    bloom-enabled: true",
                "",
                "    # Mercy Soul (bow) \u2014 arrows heal the mob they hit for half the damage they would",
                "    # have dealt, instead of damaging it.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    mercy-enabled: true",
                "",
                "    # Volley Soul (crossbow) \u2014 each shot fires 3 arrows in a tight spread.",
                "    # The two extra arrows have a small random angular offset per shot.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    volley-enabled: true",
                "",
                "    # Shatter Soul (mace) \u2014 killing a mob scatters its item drops outward in a burst.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    shatter-enabled: true",
                "",
                "    # Gravity Well Soul (mace) \u2014 striking a mob creates a brief gravity well at the",
                "    # impact point, pulling all nearby entities inward.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    gravitywell-enabled: true",
                "",
                "    # Radius in blocks of the gravity well's pull.",
                "    # Valid:   integer > 0",
                "    # Default: 5",
                "    gravitywell-radius: 5",
                "",
                "    # Duration in ticks the gravity well persists. 20 ticks = 1 second.",
                "    # Valid:   integer > 0",
                "    # Default: 12",
                "    gravitywell-duration: 12",
                "",
                "    # Pull strength per tick \u00d7 100. 30 = 0.30 blocks/tick impulse toward centre.",
                "    # Entities are internally capped at 0.6 blocks/tick toward centre.",
                "    # Valid:   integer > 0",
                "    # Default: 30",
                "    gravitywell-strength: 30",
                "",
                "    # Groundpound Soul (boots) \u2014 landing from a qualifying fall cancels fall damage",
                "    # and deals it instead to all entities within the configured radius.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    groundpound-enabled: true",
                "",
                "    # Minimum fall distance in blocks before Groundpound triggers.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 4",
                "    groundpound-min-fall: 4",
                "",
                "    # Radius in blocks of the Groundpound area-of-effect.",
                "    # Valid:   integer > 0",
                "    # Default: 3",
                "    groundpound-radius: 3",
                "",
                "    # Fall distance subtracted (in hundredths of a block) before calculating damage.",
                "    # 300 = 3.0 blocks offset \u2014 a 7-block fall deals 4.0 blocks' worth of damage.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 300",
                "    groundpound-damage-offset: 300",
                "",
                "    # Cooldown in seconds between Groundpound activations per player.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 3",
                "    groundpound-cooldown: 3",
                "",
                "    # Tracking Soul (trident) \u2014 thrown tridents home toward the nearest living",
                "    # entity within range.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    tracking-enabled: true",
                "",
                "    # Range in blocks within which the trident detects and homes toward a target.",
                "    # Valid:   integer > 0",
                "    # Default: 8",
                "    tracking-range: 8",
                "",
                "    # Steering sharpness per tick \u00d7 100. 12 = 0.12 turn rate (gentle arc).",
                "    # 100 = instant snap to target direction.",
                "    # Valid:   1\u2013100",
                "    # Default: 12",
                "    tracking-turn: 12",
                "",
                "    # Lifesteal Soul (sword) \u2014 melee hits heal the attacker.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    lifesteal-enabled: true",
                "",
                "    # Amount healed per hit in half-hearts. 1 = half a heart, 2 = full heart.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 1",
                "    lifesteal-amount: 1",
                "",
                "    # Echolocation Soul (helmet) \u2014 while worn, all living entities within range",
                "    # receive the Glowing effect.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    echolocation-enabled: true",
                "",
                "    # Range in blocks within which entities receive Glowing.",
                "    # Valid:   integer > 0",
                "    # Default: 8",
                "    echolocation-range: 8",
                "",
                "    # Dowsing Soul (shovel) \u2014 breaking a block fires a particle beam toward the",
                "    # nearest ore within range.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    dowsing-enabled: true",
                "",
                "    # Scan radius in blocks for the nearest ore.",
                "    # Valid:   integer > 0",
                "    # Default: 12",
                "    dowsing-range: 12",
                "",
                "    # Magnetize Soul (pickaxe) \u2014 breaking a block pulls all nearby item drops",
                "    # toward the player.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    magnetize-enabled: true",
                "",
                "    # Speed at which items fly toward the player, in blocks per tick \u00d7 100.",
                "    # 40 = 0.40 blocks/tick \u2014 fast enough to feel magnetic but not instant.",
                "    # Valid:   integer > 0",
                "    # Default: 40",
                "    magnetize-speed: 40",
                "",
                "    # Momentum Soul (leggings) \u2014 landing from any fall applies a Speed II burst.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    momentum-enabled: true",
                "",
                "    # Duration in ticks of the Speed II burst. 20 ticks = 1 second.",
                "    # Valid:   integer > 0",
                "    # Default: 40",
                "    momentum-duration: 40",
                "",
                "    # Dead Drop Soul (elytra) \u2014 sneaking while gliding cuts velocity to near-zero,",
                "    # creating a controlled vertical stall.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    deaddrop-enabled: true",
                "",
                "    # Velocity retain factor \u00d7 100 applied each tick while stalling.",
                "    # 5 = retain 5% per tick \u2014 rapid stall without being instant.",
                "    # Valid:   0\u2013100",
                "    # Default: 5",
                "    deaddrop-dampen: 5",
                "",
                "    # Leash Soul (trident) \u2014 hitting an entity with the trident tethers it to the",
                "    # impact point for the configured duration. The mob can still attack but cannot",
                "    # move more than leash-radius blocks from its anchor.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    leash-enabled: true",
                "",
                "    # Duration in ticks the leash holds. 20 ticks = 1 second.",
                "    # Valid:   integer > 0",
                "    # Default: 80",
                "    leash-duration: 80",
                "",
                "    # Radius in blocks the leashed entity may wander before being teleported back.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 3",
                "    leash-radius: 3",
                "",
                "    # Recall Soul (chestplate) \u2014 if a hit would leave you at or below the health",
                "    # threshold, the chestplate teleports you back to your oldest recorded position.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    recall-enabled: true",
                "",
                "    # Health threshold in full hearts. Recall fires when incoming damage would leave",
                "    # you at or below this value. 2 = triggers at 2 hearts (4 half-hearts) remaining.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 2",
                "    recall-threshold-hearts: 2",
                "",
                "    # Number of 1-second position snapshots stored. Higher = teleports further back.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 10",
                "    recall-snapshot-count: 10",
                "",
                "    # Cooldown in seconds before Recall can fire again.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 30",
                "    recall-cooldown: 30",
                "",
                "    # Molt Soul (chestplate) \u2014 taking a hit above the damage threshold spawns a",
                "    # decoy copy of you, turns you invisible, and redirects nearby mobs to the decoy.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    molt-enabled: true",
                "",
                "    # Minimum final damage in half-hearts required to trigger Molt.",
                "    # 6 = 3 full hearts in a single hit.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 6",
                "    molt-damage-threshold: 6",
                "",
                "    # Duration in ticks of both the Invisibility effect and the decoy's lifespan.",
                "    # Valid:   integer > 0",
                "    # Default: 30",
                "    molt-invis-duration: 30",
                "",
                "    # Cooldown in seconds before Molt can fire again.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 20",
                "    molt-cooldown: 20",
                "",
                "    # Range in blocks within which nearby mobs are redirected to the decoy.",
                "    # Valid:   integer > 0",
                "    # Default: 12",
                "    molt-redirect-range: 12",
                "",
                "    # Compendium Soul (axe) \u2014 every distinct mob type killed with the axe is",
                "    # permanently recorded in its lore, growing into a hunting ledger over time.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    compendium-enabled: true",
                "",
                "    # Witness Soul (sword) \u2014 silently journals every player you cross paths with.",
                "    # Entries grow more detailed the more times you encounter the same player.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    witness-enabled: true",
                "",
                "    # Range in blocks within which nearby players are witnessed and journaled.",
                "    # Valid:   integer > 0",
                "    # Default: 20",
                "    witness-range: 20",
                "",
                "    # Maximum number of journal entries stored on the sword.",
                "    # When the cap is reached, the oldest entry is evicted.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 20",
                "    witness-cap: 20",
                "",
                "    # Surveyor Soul (crossbow) \u2014 when a bolt embeds in a block, your camera briefly",
                "    # shifts to the impact point, letting you scout terrain from the bolt's vantage.",
                "    # Game mode is temporarily set to spectator and restored on return.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    surveyor-enabled: true",
                "",
                "    # Duration in ticks the camera holds at the bolt's position before returning.",
                "    # Valid:   integer > 0",
                "    # Default: 100",
                "    surveyor-duration: 100",
                "",
                "    # Bore Soul (shovel) \u2014 right-clicking dirt, sand, gravel, or clay reads the",
                "    # geological column below that block down to bedrock and sends a layered readout",
                "    # to the action bar.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    bore-enabled: true",
                "",
                "    # Verdant Soul (hoe) \u2014 right-clicking farmland temporarily un-tills it to dirt.",
                "    # Entities standing on the block receive Regeneration I each cache cycle.",
                "    # Passive mobs within range are gently nudged toward the block.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    verdant-enabled: true",
                "",
                "    # Duration in seconds the block stays un-tilled before restoring to farmland.",
                "    # Valid:   integer > 0",
                "    # Default: 20",
                "    verdant-duration: 20",
                "",
                "    # Duration in ticks of the Regeneration I effect applied each cache cycle.",
                "    # Valid:   integer > 0",
                "    # Default: 40",
                "    verdant-regen-duration: 40",
                "",
                "    # Range in blocks within which passive mobs are attracted to the verdant block.",
                "    # Valid:   integer > 0",
                "    # Default: 8",
                "    verdant-attract-range: 8",
                "",
                "    # Census Soul (hoe) \u2014 while the hoe is held, a live tally of every passive mob",
                "    # type within range is displayed on the action bar.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    census-enabled: true",
                "",
                "    # Scan radius in blocks for the Census mob tally.",
                "    # Valid:   integer > 0",
                "    # Default: 32",
                "    census-range: 32",
                "",
                "    # Breadcrumb Soul (chestplate) \u2014 every N seconds of walking, your current",
                "    # coordinates are silently recorded (up to a cap). Sneak + right-click to",
                "    # project particle pillars at all stored locations for 8 seconds.",
                "    # Creative flight and teleports do not count toward walk-time credit.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    breadcrumb-enabled: true",
                "",
                "    # Seconds of continuous walking required to record one crumb.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 60",
                "    breadcrumb-walk-seconds: 60",
                "",
                "    # Maximum crumb locations stored on the item. Oldest entry is overwritten when full.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 10",
                "    breadcrumb-max-entries: 10",
                "",
                "    # Resonance Soul (pickaxe) \u2014 breaking an ore or adjacent block pulses particles",
                "    # at all ore blocks within range, revealing the shape of the whole vein.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    resonance-enabled: true",
                "",
                "    # Scan radius in blocks for ores to highlight.",
                "    # Valid:   integer > 0",
                "    # Default: 5",
                "    resonance-range: 5",
                "",
                "    # Duration in ticks the vein highlight pulses. 20 ticks = 1 second.",
                "    # Valid:   integer > 0",
                "    # Default: 60",
                "    resonance-duration: 60",
                "",
                "    # Reflect Soul (shield) \u2014 projectiles that hit a blocking player are cancelled",
                "    # and returned toward the shooter.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    reflect-enabled: true",
                "",
                "    # Return speed of the reflected projectile as a fraction \u00d7 100 of its original speed.",
                "    # 80 = 80% of the incoming projectile's speed.",
                "    # Valid:   1\u2013100",
                "    # Default: 80",
                "    reflect-speed: 80",
                "",
                "    # Damage dealt by the reflected projectile as a fraction \u00d7 100 of the original.",
                "    # 75 = 75% of the original arrow's damage.",
                "    # Valid:   1\u2013100",
                "    # Default: 75",
                "    reflect-damage: 75",
                "",
                "    # Precognition Soul (helmet) \u2014 emits a particle flash when any hostile mob within",
                "    # range first locks onto the wearer as its target.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    precognition-enabled: true",
                "",
                "    # Range in blocks within which new mob targeting events are detected.",
                "    # Valid:   integer > 0",
                "    # Default: 16",
                "    precognition-range: 16",
                "",
                "    # Blitz Soul (leggings) \u2014 sprinting into a mob applies extra knockback and",
                "    # inflicts a brief Slowness effect on the target.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    blitz-enabled: true",
                "",
                "    # Extra knockback impulse \u00d7 100 added on top of vanilla knockback on a sprint hit.",
                "    # 40 = 0.40 blocks/tick additional impulse.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 40",
                "    blitz-knockback: 40",
                "",
                "    # Duration in ticks of the Slowness effect applied to the sprint-hit target.",
                "    # Valid:   integer > 0",
                "    # Default: 60",
                "    blitz-slowness-duration: 60",
                "",
                "    # Surge Soul (spear) \u2014 charge hits above a minimum speed apply Slowness II",
                "    # to the target, rewarding aggressive mounted or sprinting attacks.",
                "    # Never modifies damage values \u2014 vanilla charge speed-scaling is untouched.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    surge-enabled: true",
                "",
                "    # Minimum horizontal speed \u00d7 100 required to trigger Surge.",
                "    # 20 = 0.20 blocks/tick \u2014 any deliberate movement qualifies; standing still does not.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 20",
                "    surge-min-speed: 20",
                "",
                "    # Duration in ticks of the Slowness II effect applied to the struck target.",
                "    # Valid:   integer > 0",
                "    # Default: 60",
                "    surge-slowness-duration: 60",
                "",
                "    # Vault Soul (spear) \u2014 a charge hit at sufficient speed launches the attacker",
                "    # upward, using the impact as a pole-vault. Horizontal momentum is preserved.",
                "    # Does not fire while mounted.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    vault-enabled: true",
                "",
                "    # Upward launch impulse \u00d7 100. 90 = 0.90 blocks/tick vertical velocity.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 90",
                "    vault-impulse: 90",
                "",
                "    # Cooldown in seconds between Vault activations per player.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 3",
                "    vault-cooldown: 3",
                "",
                "    # Minimum horizontal speed \u00d7 100 required to trigger Vault.",
                "    # Mirrors surge-min-speed so both souls activate on the same charge.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 20",
                "    vault-min-speed: 20",
                "",
                "    # Ricochet Soul (bow) \u2014 arrows that hit a mob bounce to the nearest second",
                "    # target within range.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    ricochet-enabled: true",
                "",
                "    # Range in blocks to search for a secondary bounce target.",
                "    # Valid:   integer > 0",
                "    # Default: 4",
                "    ricochet-range: 4",
                "",
                "    # Damage dealt to the secondary target as a percentage \u00d7 100 of the primary hit.",
                "    # 60 = 60% of the original hit's damage.",
                "    # Valid:   1\u2013100",
                "    # Default: 60",
                "    ricochet-damage: 60",
                "",
                "    # Comet Soul (elytra) \u2014 landing from a glide releases a shockwave proportional",
                "    # to horizontal flight speed at the moment of impact, launching nearby entities away.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    comet-enabled: true",
                "",
                "    # Radius in blocks of the Comet shockwave.",
                "    # Valid:   integer > 0",
                "    # Default: 5",
                "    comet-radius: 5",
                "",
                "    # Minimum horizontal speed \u00d7 100 required to trigger. 60 = 0.60 blocks/tick.",
                "    # Prevents accidental triggering from slow vertical descents.",
                "    # Valid:   integer \u2265 1",
                "    # Default: 60",
                "    comet-min-speed: 60",
                "",
                "    # Cooldown in seconds between Comet activations per player.",
                "    # Valid:   integer \u2265 0",
                "    # Default: 5",
                "    comet-cooldown: 5",
                "",
                "    # Maximum knockback multiplier \u00d7 100 at the highest speeds. Scales linearly.",
                "    # 200 = 2.0\u00d7 vanilla knockback at peak speed.",
                "    # Valid:   integer \u2265 100",
                "    # Default: 200",
                "    comet-knockback-max: 200",
                "",
                "    # Brand Soul (axe) \u2014 hitting a mob marks it as a target, redirecting the aggro",
                "    # of all nearby mobs toward it for the configured duration.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    brand-enabled: true",
                "",
                "    # Duration in ticks the brand lasts. 200 = 10 seconds.",
                "    # Valid:   integer > 0",
                "    # Default: 200",
                "    brand-duration: 200",
                "",
                "    # Range in blocks within which nearby mobs are redirected toward the branded target.",
                "    # Valid:   integer > 0",
                "    # Default: 6",
                "    brand-range: 6",
                "",
                "    # Taunt Soul (shield) \u2014 while actively blocking, all nearby hostile mobs are",
                "    # forced to target the blocker instead of other players or mobs.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    taunt-enabled: true",
                "",
                "    # Range in blocks within which hostile mobs are taunted while blocking.",
                "    # Valid:   integer > 0",
                "    # Default: 8",
                "    taunt-range: 8",
                "",
                "  # When false, a chest is stamped after its first opening and never re-generates",
                "  # artifacts. When true, every open of the same chest can yield new artifacts.",
                "  # Valid:   true | false",
                "  # Default: false",
                "  allow-multi-open: false",
                "",
                "  # ── Shield patterns ──────────────────────────────────────────────────────────",
                "  # Permission to change all settings in this section: obtuseloot.admin",
                "  shields:",
                "    # When true, generated shields receive a random heraldic banner pattern",
                "    # (random base dye color + random layered banner patterns). Purely cosmetic.",
                "    # Valid:   true | false",
                "    # Default: true",
                "    pattern-enabled: true",
                "",
                "    # Minimum number of banner pattern layers placed on a generated shield.",
                "    # Valid:   0–6  (must be ≤ max-layers)",
                "    # Default: 1",
                "    min-layers: 1",
                "",
                "    # Maximum number of banner pattern layers placed on a generated shield.",
                "    # Minecraft renders at most 6 layers; values above 6 are capped to 6.",
                "    # Higher-rarity shields are weighted toward the maximum.",
                "    # Valid:   0–6  (must be ≥ min-layers)",
                "    # Default: 4",
                "    max-layers: 4",
                "",
                "# ── Integration ─────────────────────────────────────────────────────────────────────",
                "# Controls how ObtuseLoot interacts with vanilla mechanics.",
                "# Permission to change all settings in this section: obtuseloot.admin",
                "integration:",
                "  # When true, enchanting an ObtuseLoot-eligible item at an enchanting table",
                "  # automatically stamps it with a name, rarity, lore, and possible soul.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  enchant-convert: true",
                "",
                "  # When true, placing an ObtuseLoot artifact in a grindstone removes its",
                "  # soul binding. The artifact name, lore, and rarity are preserved.",
                "  # Enchantments are handled separately by vanilla grindstone logic.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  grindstone-strip: true",
                "",
                "  # ── Vanilla name mode ──────────────────────────────────────────────────────────",
                "  # Probability that a named-category item uses its plain Minecraft material name",
                "  # (e.g. Diamond Sword) instead of a curated fantasy name from its category file.",
                "  # The item still receives a prefix and optional suffix either way, e.g.:",
                "  #   \"Supple Diamond Sword of the Deep Passage\"",
                "  # 0.0 = always curated names; 1.0 = always vanilla material names.",
                "  # Valid:   0.0–1.0",
                "  # Default: 0.10",
                "  vanilla-name-chance: 0.10",
                "",
                "  # Optional per-category overrides for vanilla-name-chance.",
                "  # Uncomment any line to override the global value for that category only.",
                "  # vanilla-name-chance-overrides:",
                "  #   swords:      0.10",
                "  #   maces:       0.10",
                "  #   spears:      0.10",
                "  #   tridents:    0.10",
                "  #   bows:        0.10",
                "  #   crossbows:   0.10",
                "  #   axes:        0.10",
                "  #   pickaxes:    0.15",
                "  #   shovels:     0.20",
                "  #   hoes:        0.20",
                "  #   helmets:     0.10",
                "  #   chestplates: 0.10",
                "  #   leggings:    0.10",
                "  #   boots:       0.10",
                "  #   elytra:      0.05",
                "",
                "# ── Display ────────────────────────────────────────────────────────────────────────",
                "# Controls tooltip colors, glyphs, and formatting.",
                "# Permission to change all settings in this section: obtuseloot.admin",
                "display:",
                "  # When true, artifact display names are rendered in bold text.",
                "  # Valid:   true | false",
                "  # Default: false",
                "  bold-names: false",
                "",
                "  # Text color used for the artifact's display name.",
                "  # Valid:   black | dark_blue | dark_green | dark_aqua | dark_red | dark_purple |",
                "  #          gold | gray | dark_gray | blue | green | aqua | red | light_purple |",
                "  #          yellow | white",
                "  # Default: gold",
                "  name-color: gold",
                "",
                "  # Text color of the procedural lore line (observation, history, or secret).",
                "  # Uses the same color names as name-color.",
                "  # Valid:   see name-color for the full list",
                "  # Default: gray",
                "  lore-color: gray",
                "",
                "  # Text color of the soul tag line at the bottom of soul item tooltips.",
                "  # Uses the same color names as name-color.",
                "  # Valid:   see name-color for the full list",
                "  # Default: dark_purple",
                "  soul-color: dark_purple",
                "",
                "  # When true, a rarity-colored decorative divider is placed at the top of the tooltip.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  show-divider: true",
                "",
                "  # String inserted between the four procedural lore fragments when joined into one line.",
                "  # Valid:   any string",
                "  # Default: \" — \"",
                "  # Note: lore-separator is no longer used — each fragment is on its own line.",
                "  #lore-separator: \" — \"",
                "",
                "  # The decorative string rendered as the rarity-colored divider line.",
                "  # Unicode box-drawing characters (─) render cleanly in the Minecraft tooltip font.",
                "  # Valid:   any non-empty string",
                "  # Default: \"─────────────────────────\"",
                "  divider-string: \"─────────────────────────\"",
                "",
                "  # Symbol placed on both sides of the rarity label (e.g. ◆ Legendary ◆).",
                "  # Valid:   any string (1–3 chars recommended for visual balance)",
                "  # Default: \"◆\"",
                "  rarity-glyph: \"◆\"",
                "",
                "  # Symbol that flanks the soul tag on both sides (e.g. ✦  [Witness Soul]  ✦).",
                "  # Valid:   any string (1–3 chars recommended)",
                "  # Default: \"✦\"",
                "  soul-glyph: \"✦\"",
                "",
                "  # When true, a blank line is inserted between the rarity label and the lore text.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  show-lore-gap: true",
                "",
                "  # When true, a blank line is inserted between the lore text and the soul tag.",
                "  # Valid:   true | false",
                "  # Default: true",
                "  show-soul-gap: true",
                "",
                "# ── Command settings ──────────────────────────────────────────────────────────────────",
                "# Permission to change all settings in this section: obtuseloot.admin",
                "commands:",
                "  # Number of list entries displayed per page by /obtuseloot list.",
                "  # Valid:   integer ≥ 1",
                "  # Default: 20",
                "  list-page-size: 20",
                "",
                "# ── Rarity weights ────────────────────────────────────────────────────────────────────",
                "# Relative probability weights for each rarity tier.",
                "# Higher values = more common. Weights are normalized automatically,",
                "# but keeping the values summing to 100 makes each weight a direct",
                "# percentage — e.g. common: 50 means exactly 50 % of items are Common.",
                "#",
                "# To change these at runtime: edit this file and run /obtuseloot reload",
                "#   (permission: obtuseloot.admin)",
                "#",
                "# Defaults: common 50 | rare 30 | epic 15 | legendary 4 | mythic 1",
                "rarity-weights:",
                "  common:    50   # Valid: integer ≥ 0",
                "  rare:      30   # Valid: integer ≥ 0",
                "  epic:      15   # Valid: integer ≥ 0",
                "  legendary:  4   # Valid: integer ≥ 0",
                "  mythic:     1   # Valid: integer ≥ 0"
            );
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(content);
            } catch (IOException e) {
                getLogger().warning("Could not write default config.yml: " + e.getMessage());
            }
        }
        reloadConfig();
    }

    /**
     * Creates the plugin data-folder sub-directories on first run.
     * Each call is idempotent — {@link File#mkdirs()} is a no-op if the
     * directory already exists.
     *
     * <p>Folder layout:
     * <pre>
     *   plugins/ObtuseLoot/
     *   ├── config.yml
     *   ├── souls.yml
     *   ├── lore/          ← observations, histories, secrets
     *   ├── names/         ← prefixes, suffixes
     *   └── categories/    ← one file per item type
     * </pre>
     */
    private void setupFolders() {
        new File(getDataFolder(), "lore").mkdirs();
        new File(getDataFolder(), "names").mkdirs();
        new File(getDataFolder(), "categories").mkdirs();
    }

    private void initDefaultFiles() {
        // ── Lore lists ─────────────────────────────────────────────────────────
        generateCuratedFile("lore/observations.yml", "list", Observations.get());
        generateCuratedFile("lore/histories.yml",    "list", Histories.get());
        generateCuratedFile("lore/secrets.yml",      "list", Secrets.get());
        generateCuratedFile("lore/epithets.yml",     "list", Epithets.get());

        // ── Name lists ─────────────────────────────────────────────────────────
        generateCuratedFile("names/prefixes.yml", "list", Prefixes.get());
        generateCuratedFile("names/suffixes.yml", "list", Suffixes.get());

        // ── Weapons ─────────────────────────────────────────────────────────────
        //
        // Each category contains 35 solid fantasy names followed by 15 that earn
        // a second reading when combined with the suggestive prefixes and suffixes
        // in names/prefixes.yml and names/suffixes.yml — e.g.:
        //   "Supple Yearner of the Deep Passage"   (sword)
        //   "Hungry Poker of the Hidden Hollow"     (spear)
        //   "Insistent Probe of the Secret Chamber" (pickaxe)
        // None of the names below are crass in isolation.

        generateCuratedFile("categories/swords.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Blade", "Blight Edge", "Coldfire", "Dawnbreaker", "Embercleave",
            "Grimfang", "Heartseeker", "Ironsong", "Jadeflame", "Kingsfall",
            "Lifetaker", "Moonshard", "Nightedge", "Oathkeeper", "Plaguebringer",
            "Ravenclaw", "Shadowmend", "Tidecutter", "Voidedge", "Bonecleave",
            "Crimsonfang", "Duskblade", "Ebonedge", "Forgecleave", "Hexblade",
            "Lodeshard", "Riftblade", "Soulrender", "Thornblade", "Wraithcleave",
            "Quickstrike", "Iron Verdict", "Netheredge", "Grimwhisper", "Moonkiss",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Eager Yearner of the Deep Passage", "Insistent Bore of the Secret Chamber"
            "Yearner", "Piercer", "Delver", "Seeker", "Reacher",
            "Probe", "Drive", "Longblade", "Deepcleave", "Deeprender",
            "Bore", "Whetted Edge", "The Longing", "Close Reach", "Narrow Seeker"
        ));

        generateCuratedFile("categories/maces.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashcrown", "Bonecrusher", "Coldhammer", "Dawncrusher", "Earthfall",
            "Frostmaw", "Grimhammer", "Ironjaw", "Jadecrush", "Kinghammer",
            "Lifebane", "Moonfist", "Nightfall", "Oathsmash", "Plaguemaw",
            "Ravenfist", "Shatterfist", "Tidesmash", "Voidfall", "Boneshatter",
            "Crimsonfall", "Duskcrush", "Emberfist", "Forgesmash", "Grimcrush",
            "Jadesmash", "Lodefall", "Miresmash", "Netherfall", "Riftcrush",
            "Soulsmash", "Wraithsmash", "Thornfall", "Underlash", "Ashfall",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Surging Sweller of the Deep Passage", "Mounting Deepstroke of the Restless Night"
            "Thumper", "Pounder", "Pummel", "Sweller", "Beater",
            "Grinder", "Driver", "Striker", "Pummeler", "Broadhead",
            "Longhandle", "Deepstroke", "The Stroker", "Firm Knock", "Longstroke"
        ));

        generateCuratedFile("categories/spears.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Lance", "Bonebreaker", "Coldfang", "Dawnpiercer", "Earthshaker",
            "Galepoint", "Harbinger", "Ironveil", "Jadestrike", "Kindlepoint",
            "Lifereaper", "Moonpiercer", "Nightthorn", "Oathbreaker", "Plaguepoint",
            "Ravenspine", "Tidecaller", "Venomfang", "Ashwood Lance", "Crimson Fang",
            "Duskpiercer", "Ember Lance", "Grimthorn", "Hexspike", "Nether Fang",
            "Obsidian Tip", "Riftborn", "Wraithspine", "Thornweald", "Stormbrand",
            "Underlance", "Ironbark", "Kingsthorn", "Frostbite", "Skullsplitter",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Supple Shaft of the Deep Passage", "Hungry Poker of the Hidden Hollow"
            "Shaft", "Poker", "Prodder", "The Needle", "Probe",
            "Yearner", "Seeker", "Stinger", "Driver", "Delver",
            "Reacher", "Longreach", "Narrow Tip", "Deep Reach", "Close Pierce"
        ));

        generateCuratedFile("categories/tridents.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Abyssreach", "Brinecaller", "Coralspine", "Deepcleave", "Ebbfang",
            "Foamcutter", "Gulfpiercer", "Hightide Fang", "Inkfang", "Jadecurrent",
            "Kelpcleave", "Lagoonpiercer", "Maelstromfang", "Nethertide", "Oceancleave",
            "Phantomwave", "Riptidefang", "Seapiercer", "Tidecaller", "Undertow",
            "Voidwave", "Waterfang", "Abyssal Call", "Brinepiercer", "Coralcleave",
            "Deepfang", "Ebbtide", "Fogsurge", "Gulfcleave", "Harbourfang",
            "Kelpwave", "Maelstromcleave", "Netherwave", "Oceanfang", "Quellfang",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Mounting Longprong of the Deep Passage", "Insistent Prong of the Hidden Hollow"
            "Longprong", "The Prong", "Depthdiver", "Long Fork", "Poker",
            "Shaft", "Probe", "Delver", "Yearner", "Seeker",
            "Driver", "Deep Reach", "Borer", "Narrow Prong", "The Plunger"
        ));

        generateCuratedFile("categories/bows.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashwood Draw", "Briarsong", "Crownsong", "Dawnsong", "Elderbow",
            "Farreach", "Grimshot", "Hawkeye", "Ironstring", "Jadedraw",
            "Kindleshot", "Longwhisper", "Moonsong", "Nightdraw", "Oakheart",
            "Phantomstring", "Quickdraw", "Ravenstring", "Shadowshot", "Tidedraw",
            "Voidstring", "Wailbow", "Ashen String", "Bonebow", "Crimsonshot",
            "Duskdraw", "Emberdraw", "Forgestring", "Grimdraw", "Hexbow",
            "Jadearch", "Kindlestring", "Miresong", "Nethershot", "Souldraw",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Breathless Release of the Hidden Hollow", "Mounting Tension of the Deep Passage"
            "The Draw", "The Pull", "Quiver", "Nock", "The Shaft",
            "The Release", "The Tension", "Long Pull", "Deep Draw", "Firm Draw",
            "The Taut", "Stringer", "Long String", "The Reach", "Close Draw"
        ));

        generateCuratedFile("categories/crossbows.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashbolt", "Bonecaster", "Coldcaster", "Dawncaster", "Emberbolt",
            "Grimcaster", "Hexcaster", "Ironbolt", "Jadecaster", "Kindlebolt",
            "Lodecaster", "Netherbolt", "Phantomcaster", "Riftbolt", "Shadowcaster",
            "Tidebolt", "Voidbolt", "Wailcaster", "Ashen Bolt", "Boneshot",
            "Crimsonbolt", "Duskcaster", "Embercaster", "Forgecaster", "Grimbolt",
            "Iron Cast", "Jadebolt", "Kindlecaster", "Mirecast", "Nethercaster",
            "Phantom Bolt", "Riftcaster", "Soulbolt", "Thorncast", "Wraithcaster",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Insistent Crank of the Secret Chamber", "Eager Quick Release of the Deep Passage"
            "The Crank", "The Winch", "Deep Shot", "The Tensioner", "The Striker",
            "Bolt Driver", "The Draw", "Firm Grip", "Tight Draw", "The Nock",
            "Quick Release", "The Loader", "The Pull", "Deep Release", "Taut Arm"
        ));

        generateCuratedFile("categories/axes.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashbite", "Bonehewer", "Coldsever", "Dawnhewer", "Emberhew",
            "Frostfell", "Grimhewer", "Headtaker", "Ironbite", "Jadehew",
            "Kindlehew", "Lodehewer", "Mirehew", "Netherhew", "Phantomhew",
            "Rifthew", "Shadowhew", "Tidehew", "Voidhew", "Wailhew",
            "Ashen Bite", "Bonehew", "Crimsonhew", "Duskhewer", "Emberbite",
            "Forgehew", "Grimfell", "Hexhew", "Iron Fell", "Jadebite",
            "Kindlefell", "Lodefell", "Netherfell", "Soulfell", "Wraithfell",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Eager Wedge of the Narrow Pass", "Supple Butt of the Deep Hollow"
            "The Wedge", "Cleaver", "Splitter", "The Cleft", "The Spread",
            "Butt", "Cheek", "Notchmaker", "Groover", "The Gap",
            "Divider", "The Bit", "Backswing", "Long Hew", "Deep Notch"
        ));

        generateCuratedFile("categories/pickaxes.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Bore", "Bonepick", "Coldtine", "Dawnbore", "Earthpierce",
            "Frostpick", "Grimtine", "Ironbore", "Jadepick", "Kindletine",
            "Lodebore", "Miredigger", "Netherbore", "Phantombore", "Riftpick",
            "Shadowbore", "Tidepick", "Voidtine", "Ashen Tine", "Bonebore",
            "Crimsonbore", "Duskpick", "Embertine", "Forgebore", "Grimmine",
            "Hexpick", "Iron Bore", "Jademine", "Kindlemine", "Lodetine",
            "Mirehead", "Nethertine", "Obsidian Pick", "Riftmine", "Soulbore",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Insistent Driller of the Secret Chamber", "Supple Shaft of the Deep Passage"
            "The Driller", "The Plunger", "Shaft", "Narrow Borer", "Deep Borer",
            "Probe", "Poker", "The Tine", "Long Tine", "The Bore",
            "Delver", "Rock Splitter", "The Pricker", "Digger", "Hard Point"
        ));

        generateCuratedFile("categories/shovels.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Scoop", "Bonedigger", "Coldspade", "Dawnspade", "Earthturner",
            "Frostspade", "Grimspade", "Hearthdigger", "Ironscoop", "Jadespade",
            "Kindlespade", "Lodespade", "Netherspade", "Phantomspade", "Riftspade",
            "Shadowscoop", "Tidespade", "Voidspade", "Wailscoop", "Ashen Spade",
            "Bonescoop", "Crimsonspade", "Duskscoop", "Emberspade", "Forgescoop",
            "Grimscoop", "Hexspade", "Iron Earth", "Jadescoop", "Kindlescoop",
            "Lodescoop", "Mirewarden", "Netherschoop", "Riftscoop", "Soulspade",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Surging Spreader of the Hidden Hollow", "Insistent Pusher of the Narrow Pass"
            "The Pusher", "Spreader", "Flat Head", "Long Handle", "The Packer",
            "Hole Maker", "The Fill", "Backfiller", "Mound Maker", "The Slot",
            "Deep Digger", "The Compacter", "The Scooper", "The Plug", "Firm Head"
        ));

        generateCuratedFile("categories/hoes.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Reaper", "Bonereaper", "Coldtine", "Dawnreaper", "Earthtender",
            "Grimreaper", "Harvesttine", "Ironreaper", "Jadereaper", "Kindlereaper",
            "Lodereaper", "Mirereaper", "Netherreaper", "Phantomreaper", "Riftreaper",
            "Shadowreaper", "Tidereaper", "Voidreaper", "Wailreaper", "Ashen Tine",
            "Bonecutter", "Crimsonreaper", "Duskreaper", "Embertine", "Forgereaper",
            "Grimcutter", "Hexreaper", "Iron Harvest", "Jadecutter", "Kindlecutter",
            "Lodecutter", "Mirecutter", "Nethercutter", "Riftcutter", "Wraithreaper",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Hungry Furrow of the Hidden Hollow", "Mounting Cleft of the Secret Chamber"
            "The Furrow", "The Cleft", "The Tiller", "Groove Maker", "The Slot",
            "The Plowhead", "Cleft Maker", "The Raker", "Long Tine", "Deep Tine",
            "Crevice Maker", "The Splitter", "The Spreader", "The Divider", "Soil Turner"
        ));

        // ── Armor ────────────────────────────────────────────────────────────

        generateCuratedFile("categories/helmets.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Crown", "Bonecrown", "Coldguard", "Dawncrown", "Emberhelm",
            "Frostguard", "Grimhelm", "Headguard", "Ironguard", "Jadecrown",
            "Kindlehelm", "Lodeguard", "Netherhelm", "Obsidian Crown", "Phantomhelm",
            "Rifthelm", "Shadowhelm", "Tidehelm", "Voidhelm", "Ashen Guard",
            "Bonehelm", "Crimsonhelm", "Duskhelm", "Emberguard", "Forgeguard",
            "Grimcrown", "Hexhelm", "Iron Crown", "Jadeguard", "Kindleguard",
            "Lodecrown", "Netherguard", "Obsidian Guard", "Riftcrown", "Soulguard",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Tender Hood of the Hidden Hollow", "Insistent Fitting of the Close Embrace"
            "The Hood", "The Cap", "Coif", "The Cowl", "The Fitting",
            "The Enclosure", "The Caul", "Close Helm", "Headpiece", "The Crest",
            "Snug Cap", "Tight Cap", "The Casing", "The Wrapping", "Firm Cap"
        ));

        generateCuratedFile("categories/chestplates.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Plate", "Boneplate", "Coldward", "Dawnplate", "Emberplate",
            "Frostplate", "Grimplate", "Heartguard", "Ironplate", "Jadeplate",
            "Kindleplate", "Lodeplate", "Mireplate", "Netherplate", "Obsidian Plate",
            "Phantomplate", "Riftplate", "Shadowplate", "Tideplate", "Voidplate",
            "Ashen Ward", "Boneward", "Crimsonplate", "Duskplate", "Emberward",
            "Forgeplate", "Grimward", "Hexplate", "Iron Ward", "Jadeward",
            "Kindleward", "Lodeward", "Netherward", "Soulplate", "Wraithplate",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Straining Bodice of the Warm Embrace", "Supple Binding of the Long Embrace"
            "The Bodice", "The Binding", "The Cradle", "Cuirass", "The Corselet",
            "The Fitting", "Close Guard", "The Casing", "The Shell", "The Carapace",
            "Breastplate", "Tight Guard", "The Wrap", "The Fastening", "The Mold"
        ));

        generateCuratedFile("categories/leggings.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Greaves", "Bonegreaves", "Coldstride", "Dawngreaves", "Embergreaves",
            "Froststride", "Grimgreaves", "Irongreaves", "Jadegreaves", "Kindlegreaves",
            "Lodegreaves", "Miregreaves", "Nethergreaves", "Phantomgreaves", "Riftgreaves",
            "Shadowgreaves", "Tidegreaves", "Voidgreaves", "Ashen Stride", "Bonestride",
            "Crimsonstride", "Duskgreaves", "Emberstride", "Forgestride", "Grimstride",
            "Hexgreaves", "Iron Stride", "Jadestride", "Kindlestride", "Lodestride",
            "Mirestride", "Netherstride", "Soulgreaves", "Thornstride", "Wraithgreaves",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Supple Hose of the Warm Embrace", "Insistent Girdle of the Hidden Hollow"
            "The Hose", "The Stocking", "The Girdle", "The Loin Guard", "The Sheath",
            "The Binding", "Leg Wrap", "The Tube", "The Clincher", "The Squeeze",
            "Skin Guard", "Close Fitting", "The Press", "The Snug", "The Clench"
        ));

        generateCuratedFile("categories/boots.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Treads", "Bonetreads", "Coldstep", "Dawntreads", "Embertreads",
            "Froststep", "Grimtreads", "Hearthstep", "Irontreads", "Jadetreads",
            "Kindletreads", "Lodetreads", "Miretreads", "Nethertreads", "Phantomtreads",
            "Rifttreads", "Shadowtreads", "Tidetreads", "Voidtreads", "Ashen Step",
            "Bonestep", "Crimsonstep", "Dusktreads", "Emberstep", "Forgestep",
            "Grimstep", "Iron Step", "Jadestep", "Kindlestep", "Lodestep",
            "Mirestep", "Netherstep", "Soulstep", "Thornstep", "Wraithstep",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Supple Muff of the Warm Embrace", "Insistent Tuck of the Hidden Hollow"
            "The Muff", "The Grip", "Ankle Wrap", "The Sock", "The Tuck",
            "The Clench", "Snug Sole", "The Cuff", "Foot Cradle", "The Sheath",
            "The Hug", "Tight Step", "The Press", "Leg Casing", "The Clinch"
        ));

        generateCuratedFile("categories/elytra.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            "Ashen Wings", "Bonewing", "Coldglide", "Dawnwing", "Emberwing",
            "Frostwing", "Grimwing", "Highsoar", "Ironwing", "Jadewing",
            "Kindlewing", "Lodewing", "Netherwing", "Obsidian Wing", "Phantomwing",
            "Riftwing", "Shadowwing", "Tidewing", "Umbrawing", "Voidwing",
            "Ashen Glide", "Boneglide", "Crimsonwing", "Duskwing", "Emberglide",
            "Forgewings", "Grimglide", "Hexwing", "Iron Wing", "Jadeglide",
            "Kindleglide", "Lodeglide", "Mireglide", "Netherglide", "Soulwing",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // e.g. "Eager Spread of the Long Reach", "Hungry Opening of the Hidden Hollow"
            "The Spread", "The Opening", "Wide Glide", "The Unfurling", "Open Wing",
            "Full Extension", "Long Glide", "Deep Drop", "Open Soar", "The Stretcher",
            "The Expansion", "Spread Wing", "The Plunging", "The Descent", "The Mounting"
        ));

        generateCuratedFile("categories/shields.yml", "names", Arrays.asList(
            // ── Epic fantasy ──────────────────────────────────────────────────
            // Names evoke heraldry, barriers, warding, and deflection.
            "Ashward", "Boneward", "Coldwall", "Dawnward", "Emberbuckler",
            "Frostguard", "Grimwall", "Heartguard", "Ironwall", "Jadeward",
            "Kindlewall", "Lodeward", "Mirewall", "Netherward", "Obsidian Wall",
            "Phantomward", "Riftward", "Shadowwall", "Tideward", "Voidwall",
            "Ashen Bulwark", "Boneguard", "Crownwall", "Duskward", "Emberwall",
            "Forgewall", "Grimguard", "Hexwall", "Iron Bulwark", "Jadewall",
            "Kindleguard", "Lodewall", "Mireguard", "Netherwall", "Soulward",
            // ── Earns a second reading with suggestive prefixes / suffixes ────
            // All names are genuine heraldic or structural terms.
            // "The Boss" (the central stud of a shield), "The Mount" (heraldic term),
            // "The Bearing" (heraldic device), "The Mound" (heraldic field charge).
            // e.g. "Eager Boss of the Deep Passage", "Firm Mount of the Hidden Hollow",
            //      "Hungry Broad Face of the Secret Chamber"
            "The Boss", "Hard Face", "The Mount", "Broad Face", "The Bearing",
            "The Mound", "Wide Face", "The Front", "The Spread", "The Bulge",
            "Face Plate", "The Broad", "The Frontpiece", "The Face", "Firm Face"
        ));

        generateCuratedFile("categories/generic.yml", "names", Generic.get());

        // souls.yml — written as raw text so comments are preserved.
        File soulsFile = new File(getDataFolder(), "souls.yml");
        if (!soulsFile.exists()) {
            String soulsContent = String.join("\n",
                "# ─────────────────────────────────────────────────────────────────────────",
                "# ObtuseLoot — souls.yml",
                "# Defines soul effects that can be bound to artifacts.",
                "#",
                "# Each soul entry supports the following fields:",
                "#   tag              – Lore line shown on the item (e.g. \"[Void Soul]\")",
                "#   particle         – Bukkit Particle enum name (no-data particles only,",
                "#                      e.g. FLAME, ASH, SQUID_INK, ELECTRIC_SPARK)",
                "#   strength         – Particles spawned per tick (ambient effect)",
                "#   offset-y         – Height above the player's feet (default 1.0)",
                "#   spread-x         – Horizontal X spread (default 0.2)",
                "#   spread-y         – Vertical spread (default 0.5)",
                "#   spread-z         – Horizontal Z spread (default 0.2)",
                "#   extra            – Particle speed / extra data (default 0.01)",
                "#   only-when-moving – If true, ambient particles only spawn while moving",
                "#   ability          – Optional special ability ID (omit or leave blank for",
                "#                      particles-only souls). Built-in ability IDs:",
                "#                        lantern – place a temporary light block while walking (boots only)",
                "#                        bloom  – place a temporary flower while walking    (boots only)",
                "#                      Ability IDs are controlled by soul-abilities in config.yml.",
                "#",
                "# Apply changes with: /obtuseloot reload",
                "# ─────────────────────────────────────────────────────────────────────────",
                "",
                "souls:",
                "",
                "  void:",
                "    tag:              \"[Void Soul]\"",
                "    particle:         SQUID_INK",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.2",
                "    spread-y:         0.5",
                "    spread-z:         0.2",
                "    extra:            0.01",
                "    only-when-moving: true",
                "",
                "  flame:",
                "    tag:              \"[Flame Soul]\"",
                "    particle:         FLAME",
                "    strength:         4",
                "    offset-y:         0.5",
                "    spread-x:         0.3",
                "    spread-y:         0.6",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: true",
                "",
                "  frost:",
                "    tag:              \"[Frost Soul]\"",
                "    particle:         SNOWFLAKE",
                "    strength:         5",
                "    offset-y:         1.0",
                "    spread-x:         0.4",
                "    spread-y:         0.8",
                "    spread-z:         0.4",
                "    extra:            0.005",
                "    only-when-moving: false",
                "",
                "  ash:",
                "    tag:              \"[Ash Soul]\"",
                "    particle:         WHITE_ASH",
                "    strength:         6",
                "    offset-y:         1.2",
                "    spread-x:         0.3",
                "    spread-y:         0.7",
                "    spread-z:         0.3",
                "    extra:            0.01",
                "    only-when-moving: true",
                "",
                "  storm:",
                "    tag:              \"[Storm Soul]\"",
                "    particle:         ELECTRIC_SPARK",
                "    strength:         4",
                "    offset-y:         0.8",
                "    spread-x:         0.4",
                "    spread-y:         0.9",
                "    spread-z:         0.4",
                "    extra:            0.02",
                "    only-when-moving: true",
                "",
                "  soulfire:",
                "    tag:              \"[Soul Fire]\"",
                "    particle:         SOUL_FIRE_FLAME",
                "    strength:         3",
                "    offset-y:         0.6",
                "    spread-x:         0.2",
                "    spread-y:         0.5",
                "    spread-z:         0.2",
                "    extra:            0.01",
                "    only-when-moving: true",
                "",
                "  shadow:",
                "    tag:              \"[Shadow Soul]\"",
                "    particle:         LARGE_SMOKE",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.6",
                "    spread-z:         0.3",
                "    extra:            0.005",
                "    only-when-moving: false",
                "",
                "  ender:",
                "    tag:              \"[Ender Soul]\"",
                "    particle:         PORTAL",
                "    strength:         8",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.8",
                "    spread-z:         0.3",
                "    extra:            0.1",
                "    only-when-moving: true",
                "",
                "  crimson:",
                "    tag:              \"[Crimson Soul]\"",
                "    particle:         CRIMSON_SPORE",
                "    strength:         7",
                "    offset-y:         1.0",
                "    spread-x:         0.4",
                "    spread-y:         0.9",
                "    spread-z:         0.4",
                "    extra:            0.01",
                "    only-when-moving: false",
                "",
                "  warped:",
                "    tag:              \"[Warped Soul]\"",
                "    particle:         REVERSE_PORTAL",
                "    strength:         5",
                "    offset-y:         1.0",
                "    spread-x:         0.4",
                "    spread-y:         0.9",
                "    spread-z:         0.4",
                "    extra:            0.05",
                "    only-when-moving: false",
                "",
                "  tide:",
                "    tag:              \"[Tide Soul]\"",
                "    particle:         BUBBLE",
                "    strength:         6",
                "    offset-y:         0.5",
                "    spread-x:         0.3",
                "    spread-y:         0.7",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: true",
                "",
                "  plague:",
                "    tag:              \"[Plague Soul]\"",
                "    particle:         MYCELIUM",
                "    strength:         8",
                "    offset-y:         1.2",
                "    spread-x:         0.5",
                "    spread-y:         1.0",
                "    spread-z:         0.5",
                "    extra:            0.005",
                "    only-when-moving: false",
                "",
                "  end:",
                "    tag:              \"[End Soul]\"",
                "    particle:         END_ROD",
                "    strength:         4",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.7",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: true",
                "",
                "  sculk:",
                "    tag:              \"[Sculk Soul]\"",
                "    particle:         SCULK_SOUL",
                "    strength:         3",
                "    offset-y:         0.8",
                "    spread-x:         0.2",
                "    spread-y:         0.5",
                "    spread-z:         0.2",
                "    extra:            0.01",
                "    only-when-moving: false",
                "",
                "  totem:",
                "    tag:              \"[Totem Soul]\"",
                "    particle:         TOTEM_OF_UNDYING",
                "    strength:         5",
                "    offset-y:         1.0",
                "    spread-x:         0.4",
                "    spread-y:         0.8",
                "    spread-z:         0.4",
                "    extra:            0.05",
                "    only-when-moving: true",
                "",
                "  recall:",
                "    tag:              \"[Recall Soul]\"",
                "    particle:         REVERSE_PORTAL",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.5",
                "    spread-z:         0.3",
                "    extra:            0.05",
                "    only-when-moving: false",
                "    ability:          recall",
                "",
                "  molt:",
                "    tag:              \"[Molt Soul]\"",
                "    particle:         LARGE_SMOKE",
                "    strength:         5",
                "    offset-y:         1.0",
                "    spread-x:         0.4",
                "    spread-y:         0.6",
                "    spread-z:         0.4",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          molt",
                "",
                "  compendium:",
                "    tag:              \"[Compendium Soul]\"",
                "    particle:         ENCHANTED_HIT",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.4",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          compendium",
                "",
                "  witness:",
                "    tag:              \"[Witness Soul]\"",
                "    particle:         ENCHANT",
                "    strength:         2",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.5",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: false",
                "    ability:          witness",
                "",
                "  surveyor:",
                "    tag:              \"[Surveyor Soul]\"",
                "    particle:         SCRAPE",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.2",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          surveyor",
                "",
                "  bore:",
                "    tag:              \"[Bore Soul]\"",
                "    particle:         DRIPPING_LAVA",
                "    strength:         3",
                "    offset-y:         0.3",
                "    spread-x:         0.3",
                "    spread-y:         0.1",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          bore",
                "",
                "  verdant:",
                "    tag:              \"[Verdant Soul]\"",
                "    particle:         COMPOSTER",
                "    strength:         3",
                "    offset-y:         0.3",
                "    spread-x:         0.3",
                "    spread-y:         0.1",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          verdant",
                "",
                "  census:",
                "    tag:              \"[Census Soul]\"",
                "    particle:         HAPPY_VILLAGER",
                "    strength:         2",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.2",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          census",
                "",
                "  breadcrumb:",
                "    tag:              \"[Breadcrumb Soul]\"",
                "    particle:         END_ROD",
                "    strength:         2",
                "    offset-y:         1.0",
                "    spread-x:         0.2",
                "    spread-y:         0.4",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          breadcrumb",
                "",
                "  # ── Ability souls ─────────────────────────────────────────────────────────",
                "  # These souls carry a special world ability in addition to their particle",
                "  # effect. Each ability is slot-restricted to its designed item type.",
                "  # Each ability can be disabled in config.yml under soul-abilities.",
                "",
                "  lantern:",
                "    tag:              \"[Lantern Soul]\"",
                "    particle:         END_ROD",
                "    strength:         2",
                "    offset-y:         0.3",
                "    spread-x:         0.1",
                "    spread-y:         0.1",
                "    spread-z:         0.1",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          lantern",
                "",
                "  bloom:",
                "    tag:              \"[Bloom Soul]\"",
                "    particle:         CHERRY_LEAVES",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.3",
                "    spread-y:         0.3",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          bloom",
                "",
                "  mercy:",
                "    tag:              \"[Mercy Soul]\"",
                "    particle:         HEART",
                "    strength:         4",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.5",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          mercy",
                "",
                "  volley:",
                "    tag:              \"[Volley Soul]\"",
                "    particle:         CRIT",
                "    strength:         3",
                "    offset-y:         0.8",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.02",
                "    only-when-moving: true",
                "    ability:          volley",
                "",
                "  shatter:",
                "    tag:              \"[Shatter Soul]\"",
                "    particle:         EXPLOSION",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.5",
                "    spread-z:         0.3",
                "    extra:            0.01",
                "    only-when-moving: true",
                "    ability:          shatter",
                "",
                "  gravitywell:",
                "    tag:              \"[Gravity Well]\"",
                "    particle:         NAUTILUS",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.5",
                "    spread-z:         0.3",
                "    extra:            0.01",
                "    only-when-moving: true",
                "    ability:          gravitywell",
                "",
                "  groundpound:",
                "    tag:              \"[Groundpound Soul]\"",
                "    particle:         CAMPFIRE_COSY_SMOKE",
                "    strength:         4",
                "    offset-y:         0.1",
                "    spread-x:         0.3",
                "    spread-y:         0.05",
                "    spread-z:         0.3",
                "    extra:            0.005",
                "    only-when-moving: true",
                "    ability:          groundpound",
                "",
                "  tracking:",
                "    tag:              \"[Tracking Soul]\"",
                "    particle:         ENCHANT",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.02",
                "    only-when-moving: true",
                "    ability:          tracking",
                "",
                "  lifesteal:",
                "    tag:              \"[Lifesteal Soul]\"",
                "    particle:         HEART",
                "    strength:         2",
                "    offset-y:         1.2",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          lifesteal",
                "",
                "  echolocation:",
                "    tag:              \"[Echolocation Soul]\"",
                "    particle:         GLOW",
                "    strength:         3",
                "    offset-y:         2.4",
                "    spread-x:         0.3",
                "    spread-y:         0.1",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          echolocation",
                "",
                "  dowsing:",
                "    tag:              \"[Dowsing Soul]\"",
                "    particle:         DRIPPING_HONEY",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.25",
                "    spread-y:         0.15",
                "    spread-z:         0.25",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          dowsing",
                "",
                "  magnetize:",
                "    tag:              \"[Magnetize Soul]\"",
                "    particle:         GLOW_SQUID_INK",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.25",
                "    spread-y:         0.2",
                "    spread-z:         0.25",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          magnetize",
                "",
                "  momentum:",
                "    tag:              \"[Momentum Soul]\"",
                "    particle:         CHERRY_LEAVES",
                "    strength:         4",
                "    offset-y:         0.6",
                "    spread-x:         0.5",
                "    spread-y:         0.3",
                "    spread-z:         0.5",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          momentum",
                "",
                "  deaddrop:",
                "    tag:              \"[Dead Drop Soul]\"",
                "    particle:         FALLING_WATER",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          deaddrop",
                "",
                "  leash:",
                "    tag:              \"[Leash Soul]\"",
                "    particle:         WITCH",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          leash",
                "",
                "  resonance:",
                "    tag:              \"[Resonance Soul]\"",
                "    particle:         DRIPPING_LAVA",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          resonance",
                "",
                "  reflect:",
                "    tag:              \"[Reflect Soul]\"",
                "    particle:         CRIT",
                "    strength:         4",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.4",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: false",
                "    ability:          reflect",
                "",
                "  precognition:",
                "    tag:              \"[Precognition Soul]\"",
                "    particle:         CRIT",
                "    strength:         3",
                "    offset-y:         2.4",
                "    spread-x:         0.2",
                "    spread-y:         0.1",
                "    spread-z:         0.2",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          precognition",
                "",
                "  blitz:",
                "    tag:              \"[Blitz Soul]\"",
                "    particle:         SWEEP_ATTACK",
                "    strength:         3",
                "    offset-y:         0.6",
                "    spread-x:         0.4",
                "    spread-y:         0.2",
                "    spread-z:         0.4",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          blitz",
                "",
                "  ricochet:",
                "    tag:              \"[Ricochet Soul]\"",
                "    particle:         END_ROD",
                "    strength:         3",
                "    offset-y:         0.5",
                "    spread-x:         0.2",
                "    spread-y:         0.3",
                "    spread-z:         0.2",
                "    extra:            0.01",
                "    only-when-moving: true",
                "    ability:          ricochet",
                "",
                "  comet:",
                "    tag:              \"[Comet Soul]\"",
                "    particle:         FIREWORKS_SPARK",
                "    strength:         4",
                "    offset-y:         0.5",
                "    spread-x:         0.4",
                "    spread-y:         0.2",
                "    spread-z:         0.4",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          comet",
                "",
                "  brand:",
                "    tag:              \"[Brand Soul]\"",
                "    particle:         FLAME",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.3",
                "    spread-z:         0.3",
                "    extra:            0.02",
                "    only-when-moving: true",
                "    ability:          brand",
                "",
                "  surge:",
                "    tag:              \"[Surge Soul]\"",
                "    particle:         CLOUD",
                "    strength:         3",
                "    offset-y:         0.8",
                "    spread-x:         0.3",
                "    spread-y:         0.2",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: true",
                "    ability:          surge",
                "",
                "  vault:",
                "    tag:              \"[Vault Soul]\"",
                "    particle:         FIREWORKS_SPARK",
                "    strength:         4",
                "    offset-y:         0.5",
                "    spread-x:         0.25",
                "    spread-y:         0.1",
                "    spread-z:         0.25",
                "    extra:            0.01",
                "    only-when-moving: false",
                "    ability:          vault",
                "",
                "  taunt:",
                "    tag:              \"[Taunt Soul]\"",
                "    particle:         ENCHANTED_HIT",
                "    strength:         3",
                "    offset-y:         1.0",
                "    spread-x:         0.3",
                "    spread-y:         0.4",
                "    spread-z:         0.3",
                "    extra:            0.0",
                "    only-when-moving: false",
                "    ability:          taunt"
            );
            try (FileWriter fw = new FileWriter(soulsFile)) {
                fw.write(soulsContent);
            } catch (IOException e) {
                getLogger().warning("Could not write souls.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Writes a YAML list file under the data folder only if it doesn't exist,
     * preserving any edits the server owner has already made.
     *
     * <p>A commented header block is prepended so server admins know what the
     * file contains and how to edit it.
     *
     * @param relativePath path relative to the plugin data folder (may include sub-dirs)
     * @param key          the YAML key under which the list is stored
     * @param names        the default entries to write
     */
    private void generateCuratedFile(String relativePath, String key, List<String> names) {
        File file = new File(getDataFolder(), relativePath);
        if (file.exists()) return;
        file.getParentFile().mkdirs();

        // Build a header comment that explains the file's purpose.
        String filename   = file.getName().replace(".yml", "");
        String headerLine = switch (filename) {
            case "prefixes"      -> "# Prefix words prepended to every artifact name.";
            case "suffixes"      -> "# Suffix phrases appended to artifact names (see suffix-chance in config.yml).";
            case "observations"  -> "# First segment of the procedural lore line — describes how the item behaves.";
            case "histories"     -> "# Second segment of the procedural lore line — describes the item's past.";
            case "secrets"       -> "# Third segment of the procedural lore line — a cryptic final note.";
            case "epithets"      -> "# Fourth segment of the procedural lore line — a final archivist's note.";
            default              -> "# Display names used for items in the '" + filename + "' category.";
        };

        // YamlConfiguration.save() strips comments, so we write the header as
        // raw text first, then append the YAML data produced by the library.
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set(key, names);
        String yaml = cfg.saveToString();

        String header = String.join("\n",
            "# ─────────────────────────────────────────────────────────────────────────",
            "# ObtuseLoot — " + file.getName(),
            headerLine,
            "#",
            "# Add, remove, or reorder entries freely.",
            "# Apply changes with: /obtuseloot reload",
            "# ─────────────────────────────────────────────────────────────────────────",
            ""
        ) + "\n";

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(header);
            fw.write(yaml);
        } catch (IOException e) {
            getLogger().warning("Could not write " + relativePath + ": " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reloads all configuration and data from disk.
     * Must be called on the main server thread.
     */
    void loadAllData() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("loadAllData() must be called from the main server thread.");
        }

        reloadConfig();
        lootEnabled          = getConfig().getBoolean("loot.enabled",                  true);
        baseLootChance       = getConfig().getDouble ("loot.item-chance",             0.75);
        multiItemDecay       = getConfig().getDouble ("loot.multi-item-decay",         0.45);
        suffixChance         = getConfig().getDouble ("loot.suffix-chance",            0.50);
        proceduralLoreChance = getConfig().getDouble ("loot.procedural-lore-chance",   0.85);
        maxItemsPerChest     = getConfig().getInt    ("loot.max-items-per-chest",      4);
        useBoldNames         = getConfig().getBoolean("display.bold-names",            false);
        enchantConvert       = getConfig().getBoolean("integration.enchant-convert",   true);
        grindstoneStrip      = getConfig().getBoolean("integration.grindstone-strip",  true);
        vanillaNameChance    = getConfig().getDouble ("integration.vanilla-name-chance", 0.10);

        // Display
        nameColor     = parseColor(getConfig().getString("display.name-color",  "gold"),        NamedTextColor.GOLD);
        loreTextColor = parseColor(getConfig().getString("display.lore-color",  "gray"),        NamedTextColor.GRAY);
        soulTagColor  = parseColor(getConfig().getString("display.soul-color",  "dark_purple"), NamedTextColor.DARK_PURPLE);
        showDivider   = getConfig().getBoolean("display.show-divider",  true);
        // lore-separator is no longer used — lore fragments are each on their own line
        dividerString = getConfig().getString ("display.divider-string", "─────────────────────────");
        rarityGlyph   = getConfig().getString ("display.rarity-glyph",  "◆");
        soulGlyph     = getConfig().getString ("display.soul-glyph",    "✦");
        showLoreGap   = getConfig().getBoolean("display.show-lore-gap",  true);
        showSoulGap   = getConfig().getBoolean("display.show-soul-gap",  true);

        // Rarity weights — load from config, fall back to sensible defaults, guard total > 0.
        {
            Rarity[] rarityValues = RARITY_VALUES;
            int[] defaults = {50, 30, 15, 4, 1}; // COMMON, RARE, EPIC, LEGENDARY, MYTHIC
            int[] weights  = new int[rarityValues.length];
            int   total    = 0;
            for (int i = 0; i < rarityValues.length; i++) {
                int w = getConfig().getInt("rarity-weights." + rarityValues[i].name().toLowerCase(), defaults[i]);
                weights[i] = Math.max(0, w);
                total     += weights[i];
            }
            rarityWeights     = weights;
            rarityWeightTotal = total > 0 ? total : 1; // guard against all-zero config
        }

        listPageSize = Math.max(1, getConfig().getInt("commands.list-page-size", 20));

        vanillaNameChanceOverrides.clear();
        ConfigurationSection overrideSec = getConfig().getConfigurationSection(
            "integration.vanilla-name-chance-overrides");
        if (overrideSec != null) {
            for (String key : overrideSec.getKeys(false)) {
                vanillaNameChanceOverrides.put(key, overrideSec.getDouble(key));
            }
        }

        try {
            minSoulRarity = Rarity.valueOf(
                getConfig().getString("loot.souls.min-rarity", "RARE").toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid loot.souls.min-rarity in config — defaulting to RARE.");
            minSoulRarity = Rarity.RARE;
        }
        {
            double[] sc = new double[RARITY_VALUES.length];
            sc[0] = 0.0; // COMMON — excluded by min-rarity; value unused
            sc[1] = Math.max(0.0, Math.min(1.0, getConfig().getDouble("loot.souls.soul-chance-rare",      0.40)));
            sc[2] = Math.max(0.0, Math.min(1.0, getConfig().getDouble("loot.souls.soul-chance-epic",      0.65)));
            sc[3] = Math.max(0.0, Math.min(1.0, getConfig().getDouble("loot.souls.soul-chance-legendary", 0.90)));
            sc[4] = Math.max(0.0, Math.min(1.0, getConfig().getDouble("loot.souls.soul-chance-mythic",    1.00)));
            soulChanceByRarity = sc;
        }
        allowMultiOpen = getConfig().getBoolean("loot.allow-multi-open",   false);

        // Soul ability enables — each key matches an ability ID in SoulData.ability().
        // Built into a local map first; the volatile field is replaced in a single
        // atomic write with an unmodifiable snapshot so async readers never see a
        // partially-populated map.
        {
            Map<String, Boolean> ae = new HashMap<>();
            ae.put("lantern",     getConfig().getBoolean("loot.soul-abilities.lantern-enabled",     true));
            ae.put("bloom",       getConfig().getBoolean("loot.soul-abilities.bloom-enabled",       true));
            ae.put("mercy",       getConfig().getBoolean("loot.soul-abilities.mercy-enabled",       true));
            ae.put("volley",      getConfig().getBoolean("loot.soul-abilities.volley-enabled",      true));
            ae.put("shatter",     getConfig().getBoolean("loot.soul-abilities.shatter-enabled",     true));
            ae.put("groundpound", getConfig().getBoolean("loot.soul-abilities.groundpound-enabled", true));
            ae.put("tracking",    getConfig().getBoolean("loot.soul-abilities.tracking-enabled",    true));
            ae.put("lifesteal",   getConfig().getBoolean("loot.soul-abilities.lifesteal-enabled",   true));
            ae.put("gravitywell", getConfig().getBoolean("loot.soul-abilities.gravitywell-enabled", true));
            ae.put("echolocation",getConfig().getBoolean("loot.soul-abilities.echolocation-enabled",true));
            ae.put("dowsing",     getConfig().getBoolean("loot.soul-abilities.dowsing-enabled",     true));
            ae.put("magnetize",   getConfig().getBoolean("loot.soul-abilities.magnetize-enabled",   true));
            ae.put("momentum",    getConfig().getBoolean("loot.soul-abilities.momentum-enabled",    true));
            ae.put("deaddrop",    getConfig().getBoolean("loot.soul-abilities.deaddrop-enabled",    true));
            ae.put("leash",       getConfig().getBoolean("loot.soul-abilities.leash-enabled",       true));
            ae.put("precognition",getConfig().getBoolean("loot.soul-abilities.precognition-enabled",true));
            ae.put("blitz",       getConfig().getBoolean("loot.soul-abilities.blitz-enabled",       true));
            ae.put("surge",       getConfig().getBoolean("loot.soul-abilities.surge-enabled",       true));
            ae.put("vault",       getConfig().getBoolean("loot.soul-abilities.vault-enabled",       true));
            ae.put("ricochet",    getConfig().getBoolean("loot.soul-abilities.ricochet-enabled",    true));
            ae.put("comet",       getConfig().getBoolean("loot.soul-abilities.comet-enabled",       true));
            ae.put("brand",       getConfig().getBoolean("loot.soul-abilities.brand-enabled",       true));
            ae.put("taunt",       getConfig().getBoolean("loot.soul-abilities.taunt-enabled",       true));
            ae.put("recall",      getConfig().getBoolean("loot.soul-abilities.recall-enabled",      true));
            ae.put("molt",        getConfig().getBoolean("loot.soul-abilities.molt-enabled",        true));
            ae.put("compendium",  getConfig().getBoolean("loot.soul-abilities.compendium-enabled",  true));
            ae.put("witness",     getConfig().getBoolean("loot.soul-abilities.witness-enabled",     true));
            ae.put("surveyor",    getConfig().getBoolean("loot.soul-abilities.surveyor-enabled",    true));
            ae.put("bore",        getConfig().getBoolean("loot.soul-abilities.bore-enabled",        true));
            ae.put("verdant",     getConfig().getBoolean("loot.soul-abilities.verdant-enabled",     true));
            ae.put("census",      getConfig().getBoolean("loot.soul-abilities.census-enabled",      true));
            ae.put("breadcrumb",  getConfig().getBoolean("loot.soul-abilities.breadcrumb-enabled",  true));
            ae.put("resonance",   getConfig().getBoolean("loot.soul-abilities.resonance-enabled",   true));
            ae.put("reflect",     getConfig().getBoolean("loot.soul-abilities.reflect-enabled",     true));
            abilityEnabled = Collections.unmodifiableMap(ae);
        }

        // Numeric ability parameters — passed alongside the boolean enables so ObtuseEngine
        // can read configurable values without needing a back-reference to ObtuseLoot.
        Map<String, Integer> abilityParams = new HashMap<>();
        abilityParams.put("groundpound-min-fall",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.groundpound-min-fall",  4)));
        abilityParams.put("groundpound-radius",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.groundpound-radius",    3)));
        abilityParams.put("groundpound-damage-offset",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.groundpound-damage-offset", 300)));
        abilityParams.put("groundpound-cooldown",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.groundpound-cooldown",  3)));
        abilityParams.put("tracking-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.tracking-range",        8)));
        abilityParams.put("tracking-turn",
            Math.min(100, Math.max(1, getConfig().getInt("loot.soul-abilities.tracking-turn", 12))));
        abilityParams.put("lifesteal-amount",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.lifesteal-amount",       1)));
        abilityParams.put("gravitywell-radius",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.gravitywell-radius",     5)));
        abilityParams.put("gravitywell-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.gravitywell-duration",  12)));
        abilityParams.put("gravitywell-strength",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.gravitywell-strength",  30)));
        abilityParams.put("echolocation-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.echolocation-range",     8)));
        abilityParams.put("dowsing-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.dowsing-range",         12)));
        abilityParams.put("magnetize-speed",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.magnetize-speed",       40)));
        abilityParams.put("momentum-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.momentum-duration",     40)));
        abilityParams.put("deaddrop-dampen",
            Math.max(1, Math.min(99,
                        getConfig().getInt("loot.soul-abilities.deaddrop-dampen",         5))));
        abilityParams.put("leash-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.leash-duration",        80)));
        abilityParams.put("leash-radius",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.leash-radius",           3)));
        abilityParams.put("precognition-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.precognition-range",     16)));
        abilityParams.put("blitz-knockback",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.blitz-knockback",        40)));
        abilityParams.put("blitz-slowness-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.blitz-slowness-duration",60)));
        abilityParams.put("surge-min-speed",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.surge-min-speed",        20)));
        abilityParams.put("surge-slowness-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.surge-slowness-duration",60)));
        abilityParams.put("vault-impulse",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.vault-impulse",          90)));
        abilityParams.put("vault-cooldown",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.vault-cooldown",          3)));
        abilityParams.put("vault-min-speed",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.vault-min-speed",         20)));
        abilityParams.put("ricochet-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.ricochet-range",          4)));
        abilityParams.put("ricochet-damage",
            Math.max(1, Math.min(100,
                        getConfig().getInt("loot.soul-abilities.ricochet-damage",         60))));
        abilityParams.put("comet-radius",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.comet-radius",            5)));
        abilityParams.put("comet-min-speed",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.comet-min-speed",        60)));
        abilityParams.put("comet-cooldown",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.comet-cooldown",          5)));
        abilityParams.put("comet-knockback-max",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.comet-knockback-max",    200)));
        abilityParams.put("brand-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.brand-duration",        200)));
        abilityParams.put("brand-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.brand-range",             6)));
        abilityParams.put("taunt-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.taunt-range",             8)));
        abilityParams.put("recall-threshold-hearts",
            Math.max(1, Math.min(10,
                        getConfig().getInt("loot.soul-abilities.recall-threshold-hearts",  2))));
        abilityParams.put("recall-snapshot-count",
            Math.max(1, Math.min(30,
                        getConfig().getInt("loot.soul-abilities.recall-snapshot-count",   10))));
        abilityParams.put("recall-cooldown",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.recall-cooldown",         30)));
        abilityParams.put("molt-damage-threshold",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.molt-damage-threshold",    6)));
        abilityParams.put("molt-invis-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.molt-invis-duration",     30)));
        abilityParams.put("molt-cooldown",
            Math.max(0,  getConfig().getInt("loot.soul-abilities.molt-cooldown",           20)));
        abilityParams.put("molt-redirect-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.molt-redirect-range",     12)));
        abilityParams.put("witness-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.witness-range",           20)));
        abilityParams.put("witness-cap",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.witness-cap",             20)));
        abilityParams.put("surveyor-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.surveyor-duration",      100)));
        abilityParams.put("verdant-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.verdant-duration",        20)));
        abilityParams.put("verdant-regen-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.verdant-regen-duration",  40)));
        abilityParams.put("verdant-attract-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.verdant-attract-range",    8)));
        abilityParams.put("census-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.census-range",            32)));
        abilityParams.put("breadcrumb-walk-seconds",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.breadcrumb-walk-seconds", 60)));
        abilityParams.put("breadcrumb-max-entries",
            Math.max(1, Math.min(20,
                        getConfig().getInt("loot.soul-abilities.breadcrumb-max-entries",   10))));
        abilityParams.put("resonance-range",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.resonance-range",         5)));
        abilityParams.put("resonance-duration",
            Math.max(1,  getConfig().getInt("loot.soul-abilities.resonance-duration",     60)));
        abilityParams.put("reflect-speed",
            Math.max(1, Math.min(200,
                        getConfig().getInt("loot.soul-abilities.reflect-speed",           80))));
        abilityParams.put("reflect-damage",
            Math.max(1, Math.min(100,
                        getConfig().getInt("loot.soul-abilities.reflect-damage",          75))));

        shieldPatternEnabled = getConfig().getBoolean("loot.shields.pattern-enabled", true);
        shieldMinLayers      = Math.max(0, Math.min(6,
                                   getConfig().getInt("loot.shields.min-layers", 1)));
        shieldMaxLayers      = Math.max(shieldMinLayers, Math.min(6,
                                   getConfig().getInt("loot.shields.max-layers", 4)));

        // Cache all registered banner pattern types. Done here (not at class-load) so
        // Bukkit's registry is guaranteed to be fully initialized.
        {
            List<PatternType> pts = new ArrayList<>();
            Registry.BANNER_PATTERN.forEach(pts::add);
            patternTypes = pts.isEmpty() ? new PatternType[0]
                                         : pts.toArray(new PatternType[0]);
        }

        loadList(prefixes,     "names/prefixes.yml",    "list");
        loadList(suffixes,     "names/suffixes.yml",    "list");
        // Pre-filter the non-deity suffix pool so applyArtifactMeta pays no stream cost.
        nonDeitySuffixes.clear();
        for (String s : suffixes) {
            if (!DEITY_SUFFIX_PATTERN.matcher(s).matches()) nonDeitySuffixes.add(s);
        }
        loadList(observations, "lore/observations.yml", "list");
        loadList(histories,    "lore/histories.yml",    "list");
        loadList(secrets,      "lore/secrets.yml",      "list");
        loadList(epithets,     "lore/epithets.yml",     "list");

        dictionaries.clear();
        File catDir = new File(getDataFolder(), "categories");
        if (catDir.isDirectory()) {
            File[] files = catDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) {
                getLogger().warning("Could not read categories/ — dictionary load skipped.");
            } else {
                for (File f : files) {
                    String catId = f.getName().replace(".yml", "");
                    List<String> names = YamlConfiguration.loadConfiguration(f).getStringList("names");
                    if (names.isEmpty()) {
                        getLogger().warning("Category file " + f.getName() + " has no 'names' list — skipping.");
                    } else {
                        dictionaries.put(catId, names);
                    }
                }
            }
        }

        genericWeight = Math.max(1, getConfig().getInt("loot.generic-weight", 8));

        // Pre-compute per-category material sub-arrays so generateArtifact() and the
        // generic-to-named upgrade never allocate streams or filter arrays at runtime.
        // Only categories that have at least one material in ARTIFACT_MATERIALS are kept
        // so the upgrade path never picks an empty slot.
        {
            Map<String, List<Material>> catListsBuilder = new HashMap<>();
            for (Material m : ARTIFACT_MATERIALS) {
                catListsBuilder
                    .computeIfAbsent(getCategory(m), k -> new ArrayList<>())
                    .add(m);
            }
            Map<String, Material[]> built = new HashMap<>();
            for (Map.Entry<String, List<Material>> e : catListsBuilder.entrySet()) {
                built.put(e.getKey(), e.getValue().toArray(new Material[0]));
            }
            categoryMaterials = built;

            // Pre-build the named-category list so generateArtifact() is allocation-free.
            List<String> named = new ArrayList<>(built.keySet());
            named.remove("generic");
            namedCategoryIds    = List.copyOf(named); // unmodifiable snapshot
            namedCategoryCount  = named.size();
        }

        activeSouls.clear();
        soulList.clear();
        File soulsFile = new File(getDataFolder(), "souls.yml");
        if (soulsFile.exists()) {
            ConfigurationSection sec = YamlConfiguration
                .loadConfiguration(soulsFile)
                .getConfigurationSection("souls");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    String particleStr = sec.getString(k + ".particle", "ASH").toUpperCase();
                    Particle particle;
                    try {
                        particle = Particle.valueOf(particleStr);
                    } catch (IllegalArgumentException ex) {
                        getLogger().warning("Soul '" + k + "' has unknown particle '" + particleStr + "' — using ASH.");
                        particle = Particle.ASH;
                    }
                    SoulData soul = new SoulData(
                        k.toLowerCase(),
                        sec.getString(k + ".tag", "[" + k + "]"),
                        particle,
                        sec.getInt   (k + ".strength",          3),
                        sec.getDouble(k + ".offset-y",          1.0),
                        sec.getDouble(k + ".spread-x",          0.2),
                        sec.getDouble(k + ".spread-y",          0.5),
                        sec.getDouble(k + ".spread-z",          0.2),
                        sec.getDouble(k + ".extra",             0.01),
                        sec.getBoolean(k + ".only-when-moving", true),
                        sec.getString(k + ".ability",           "").toLowerCase().trim()
                    );
                    activeSouls.put(soul.id(), soul);
                    soulList.add(soul);
                }
            }
        }

        soulEngine.reload(activeSouls, abilityEnabled, abilityParams, soulTagColor, soulGlyph);
        buildEditTargets();
    }

    /**
     * Clears and reloads a single list from a YAML file.
     *
     * @param list     the in-memory list to populate
     * @param filename path relative to the data folder
     * @param yamlKey  the YAML key containing the string list
     */
    private void loadList(List<String> list, String filename, String yamlKey) {
        list.clear();
        File f = new File(getDataFolder(), filename);
        if (!f.exists()) return;
        List<String> loaded = YamlConfiguration.loadConfiguration(f).getStringList(yamlKey);
        if (loaded.isEmpty()) {
            getLogger().warning(filename + " exists but contains no entries under '" + yamlKey + "'.");
        }
        list.addAll(loaded);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loot chest population
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects ObtuseLoot artifacts into lootable containers on first open.
     *
     * <p>The container's loot table is allowed to populate normally first (Bukkit handles
     * this before HIGHEST priority). We then add artifacts to empty slots.
     *
     * <p>A one-tick deferred task is used so the vanilla loot table has already been
     * written by the time we access the inventory, avoiding a race against Bukkit's
     * internal loot processing.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootPopulate(InventoryOpenEvent event) {
        if (!lootEnabled) return;
        if (!(event.getInventory().getHolder(false) instanceof Container container)) return;
        if (!(container instanceof Lootable lootable) || lootable.getLootTable() == null) return;
        if (container.getPersistentDataContainer().has(generatedKey, PersistentDataType.BYTE)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Block block = container.getBlock();
        String chestKey = block.getWorld().getName()
            + "|" + block.getX() + "|" + block.getY() + "|" + block.getZ();
        if (!pendingChests.add(chestKey)) return;

        // Snapshot volatile fields for use in the lambda.
        final boolean multiOpen = this.allowMultiOpen;
        final double baseChance = this.baseLootChance;
        final double decay      = this.multiItemDecay;
        final int    maxItems   = this.maxItemsPerChest;

        Bukkit.getScheduler().runTask(this, () -> {
            try {
                // If the block was broken during the 1-tick delay, bail out cleanly.
                if (!(block.getState() instanceof Container)) return;

                // Re-fetch the live inventory from the container rather than
                // holding the reference from the original event.
                Inventory inv = container.getInventory();
                double chance = baseChance;
                for (int i = 0; i < maxItems; i++) {
                    if (ThreadLocalRandom.current().nextDouble() < chance) {
                        int slot = findEmptySlot(inv);
                        if (slot == -1) break; // chest is full
                        inv.setItem(slot, generateArtifact());
                        chance *= decay;
                    } else {
                        break; // once we fail a roll the decayed odds are worse — stop early
                    }
                }

                // Stamp the chest only after the loot loop completes successfully.
                // Placing the stamp here ensures that if generateArtifact() throws
                // mid-loop the chest is not permanently locked as generated-but-empty.
                if (!multiOpen) {
                    container.getPersistentDataContainer().set(generatedKey, PersistentDataType.BYTE, (byte) 1);
                    container.update();
                }
            } finally {
                pendingChests.remove(chestKey);
            }
        });
    }

    /**
     * Reservoir sampling over empty slots — O(n) single pass, zero allocation.
     * Returns -1 if the inventory is full.
     */
    private int findEmptySlot(Inventory inv) {
        int chosen = -1, count = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                if (ThreadLocalRandom.current().nextInt(++count) == 0) chosen = i;
            }
        }
        return chosen;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enchanting-table integration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When a player enchants an eligible item, applies ObtuseLoot metadata on top
     * of the chosen enchantments. Controlled by {@code integration.enchant-convert}.
     *
     * <p>We use {@link EnchantItemEvent} which fires after the player clicks an
     * enchantment option — at this point the enchantments are committed and the
     * item reference in the event reflects the enchanted state. We apply ObtuseLoot
     * metadata directly so both name/lore and enchantments are present on the item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (!enchantConvert) return;
        ItemStack item = event.getItem();
        if (!ARTIFACT_MATERIAL_SET.contains(item.getType())) return;

        // Don't re-convert items that are already ObtuseLoot artifacts.
        if (item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                       .has(artifactKey, PersistentDataType.BYTE)) return;

        // Apply metadata. EnchantItemEvent.getItem() returns the actual item reference
        // in the enchanting table slot; modifying it in-place is sufficient on Paper.
        applyArtifactMeta(item, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grindstone integration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When a player takes an item out of a grindstone result slot, removes the
     * soul binding from ObtuseLoot artifacts. The artifact name, lore, and rarity
     * label are preserved — only the soul PDC entry is cleared.
     * Controlled by {@code integration.grindstone-strip}.
     *
     * <p>We listen on {@link InventoryClickEvent} for clicks on slot 2 (the result
     * slot) of a GRINDSTONE inventory. This is the canonical approach for grindstone
     * result interception on Paper — {@code PrepareGrindstoneEvent} fires
     * during preview and is not the right place to mutate the final item.
     *
     * <p>Shift-clicking also triggers this event with the same slot, so both
     * normal and shift-click paths are handled correctly.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrindstoneResult(InventoryClickEvent event) {
        if (!grindstoneStrip) return;
        if (event.getInventory().getType() != InventoryType.GRINDSTONE) return;
        if (event.getSlot() != 2) return; // slot 2 = output slot
        if (!(event.getWhoClicked() instanceof Player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;
        if (!result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.getPersistentDataContainer().has(artifactKey, PersistentDataType.BYTE)) return;

        // Remove only the soul binding — name, lore, and rarity label are preserved.
        // The artifactKey marker is also kept so the item is still recognised as an
        // ObtuseLoot artifact (e.g. by the enchant-convert guard).
        if (meta.getPersistentDataContainer().has(soulKey, PersistentDataType.STRING)) {
            meta.getPersistentDataContainer().remove(soulKey);
            result.setItemMeta(meta);
        }
        // The click event proceeds normally — the grindstone gives the item to the player.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Artifact generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a fresh random artifact with weighted category selection.
     *
     * <p>The generic pool (flowers, food, candles, etc.) is assigned a relative weight
     * of {@code loot.generic-weight} (default 8); every named category gets weight 1.
     * This gives operators direct control over how often generic items appear in chests
     * without affecting the balance between individual named categories.
     *
     * <p>Both {@link #namedCategoryIds} and {@link #categoryMaterials} are precomputed
     * in {@link #loadAllData()} so this method performs no heap allocation on the hot path.
     */
    private ItemStack generateArtifact() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        final List<String> named      = this.namedCategoryIds;   // volatile snapshot
        final int          namedCount = this.namedCategoryCount;
        final int          gw         = this.genericWeight;
        final int          totalWeight = gw + namedCount;
        final int          roll        = rng.nextInt(totalWeight);

        Material[] pool;
        if (roll < gw || namedCount == 0) {
            Material[] genericPool = categoryMaterials.get("generic");
            pool = (genericPool != null && genericPool.length > 0)
                    ? genericPool : ARTIFACT_MATERIALS;
        } else {
            String chosenCat = named.get(roll - gw);
            pool = categoryMaterials.getOrDefault(chosenCat, ARTIFACT_MATERIALS);
        }
    }

    public static ObtuseLoot get() {
        return instance;
    }

    private void applyArtifactMetaImpl(ItemStack item, Rarity rarity, SoulData forcedSoul) {
        final double        lSuffixChance      = this.suffixChance;
        final double        lLoreChance        = this.proceduralLoreChance;
        final boolean       lBold              = this.useBoldNames;
        final Rarity        lMinSoulRarity     = this.minSoulRarity;
        final double[]      lSoulChanceByRarity = this.soulChanceByRarity;
        final double        lVanillaNameChance = this.vanillaNameChance;
        final NamedTextColor lNameColor        = this.nameColor;
        final NamedTextColor lLoreTextColor    = this.loreTextColor;
        final NamedTextColor lSoulTagColor     = this.soulTagColor;
        final boolean        lShowDivider      = this.showDivider;
        final boolean        lShowLoreGap      = this.showLoreGap;
        final boolean        lShowSoulGap      = this.showSoulGap;
        // lLoreSeparator removed — no longer used
        final String         lDividerString    = this.dividerString;
        final String         lRarityGlyph      = this.rarityGlyph;
        final String         lSoulGlyph        = this.soulGlyph;
        final ThreadLocalRandom rng            = ThreadLocalRandom.current();

        if (rarity == null) rarity = rollRarity(rng);

        // ── Generic-to-named upgrade ──────────────────────────────────────────
        // Generic materials (flowers, food, candles, etc.) are only used for COMMON.
        // For RARE and above, swap to a random material from a named category so the
        // item has a proper type, name, and full lore treatment.
        // This must happen before getItemMeta() — setType() can invalidate a held meta
        // reference on some Paper versions.
        String catId = getCategory(item.getType());
        if (rarity != Rarity.COMMON && "generic".equals(catId)) {
            // Upgrade to a random named category that has at least one material in
            // ARTIFACT_MATERIALS. Use the precomputed categoryMaterials map so no
            // stream allocation occurs here on every qualifying artifact.
            // "generic" itself is already excluded because categoryMaterials only
            // contains the key if the category has matching materials, and the
            // generic bucket is kept separate — only truly named categories are picked.
            // Use the precomputed volatile snapshot — zero allocation on this path.
            final List<String> namedIds = this.namedCategoryIds;
            final int          namedCnt = this.namedCategoryCount;
            if (namedCnt > 0) {
                catId = namedIds.get(rng.nextInt(namedCnt));
                Material[] catMats = categoryMaterials.get(catId);
                // catMats is guaranteed non-null and non-empty by the precompute step.
                item.setType(catMats[rng.nextInt(catMats.length)]);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return; // material has no meta (e.g. AIR) — shouldn't happen in normal usage

        // ── Name ─────────────────────────────────────────────────────────────

        // Vanilla name mode: non-generic items have a configurable chance to use
        // the cleaned-up Minecraft material name ("Diamond Sword") instead of a
        // curated name, while still receiving a prefix, optional suffix, and lore.
        double effectiveVanillaChance = vanillaNameChanceOverrides.getOrDefault(catId, lVanillaNameChance);
        boolean useVanillaName = !"generic".equals(catId) && rng.nextDouble() < effectiveVanillaChance;

        String baseName;
        boolean isNamed;
        if (useVanillaName) {
            baseName = capitalize(item.getType().name()); // e.g. "Diamond Sword"
            isNamed  = true; // vanilla names get full lore + prefix + suffix treatment
        } else {
            List<String> pool = dictionaries.get(catId);
            if (pool != null && !pool.isEmpty()) {
                baseName = pool.get(rng.nextInt(pool.size()));
                isNamed  = true;
            } else {
                baseName = capitalize(item.getType().name()); // fallback — no lore
                isNamed  = false;
            }
        }

        String pre = prefixes.isEmpty() ? "" : prefixes.get(rng.nextInt(prefixes.size())) + " ";
        // When the chosen prefix is a deity/entity name, exclude matching deity suffixes
        // to prevent double-entity names like "Fenrir of Hecate".
        // nonDeitySuffixes is pre-filtered in loadAllData() — no stream allocation here.
        List<String> availableSuffixes = (!pre.isBlank() && DEITY_PREFIXES.contains(pre.trim()))
                ? nonDeitySuffixes : suffixes;
        String suf = (!availableSuffixes.isEmpty() && rng.nextDouble() < lSuffixChance)
                     ? " " + availableSuffixes.get(rng.nextInt(availableSuffixes.size())) : "";

        // ── Name — color from display.name-color in config.yml (default: gold) ─
        // Boldness is controlled by display.bold-names in config.yml.
        Component displayName = Component.text(pre + baseName + suf, lNameColor)
                                         .decoration(TextDecoration.ITALIC, false);
        if (lBold) displayName = displayName.decorate(TextDecoration.BOLD);
        meta.displayName(displayName);

        // ── Lore ─────────────────────────────────────────────────────────────
        //
        // Tooltip layout:
        //   ─────────────────────────   (rarity color divider — if display.show-divider: true)
        //   ◆ Legendary ◆               (rarity label, rarity color)
        //                               (blank gap — only when lore follows)
        //   Observations, histories, secrets.  (display.lore-color, when present)
        //                               (blank gap — only when soul follows)
        //   ✦ [Soul Tag]                (display.soul-color, when present)

        List<Component> lore = new ArrayList<>();

        // Divider line — color signals rarity at a glance; toggled by display.show-divider.
        if (lShowDivider) {
            lore.add(Component.text(lDividerString, rarity.color)
                               .decoration(TextDecoration.ITALIC, false));
        }

        // Rarity label flanked by configurable glyphs.
        lore.add(Component.text(lRarityGlyph + " " + rarity.label + " " + lRarityGlyph, rarity.color)
                           .decoration(TextDecoration.ITALIC, false));

        // Procedural lore — four fragments on separate lines so each reads cleanly
        // without wrapping mid-sentence. Gap above only when lore is present.
        boolean hasLore = isNamed && rng.nextDouble() < lLoreChance
                && !observations.isEmpty() && !histories.isEmpty()
                && !secrets.isEmpty() && !epithets.isEmpty();
        if (hasLore) {
            if (lShowLoreGap) lore.add(Component.empty());
            lore.add(Component.text(observations.get(rng.nextInt(observations.size())), lLoreTextColor)
                               .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(histories.get(rng.nextInt(histories.size())), lLoreTextColor)
                               .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(secrets.get(rng.nextInt(secrets.size())), lLoreTextColor)
                               .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(epithets.get(rng.nextInt(epithets.size())) + ".", lLoreTextColor)
                               .decoration(TextDecoration.ITALIC, false));
        }

        // Soul tag — blank gap above it so it reads as a distinct block.
        // Guard: if the item already carries a soul (e.g. re-converted artifact), do not
        // assign a second one. The PDC set() would silently overwrite, so we check first.
        // In normal generation this never triggers — applyArtifactMeta is only called once
        // per item — but the guard makes the invariant explicit and protects future call sites.
        boolean alreadyHasSoul = meta.getPersistentDataContainer()
                                     .has(soulKey, PersistentDataType.STRING);
        if (!alreadyHasSoul && rarity.level >= lMinSoulRarity.level && !soulList.isEmpty()
                && (forcedSoul != null || rng.nextDouble() < lSoulChanceByRarity[rarity.ordinal()])) {
            // Filter the soul pool to only souls whose ability (if any) is permitted
            // for this item's category. When a soul is force-overridden (givesoul command)
            // the single forced soul is used directly without category filtering.
            //   lantern, bloom, groundpound → boots only
            //   mercy, ricochet             → bows and crossbows / bows only
            //   volley, surveyor            → crossbows only
            //   shatter, gravitywell        → maces only
            //   dowsing, bore               → shovels only
            //   tracking, leash             → tridents only
            //   lifesteal, witness          → swords only
            //   echolocation, precognition  → helmets only
            //   magnetize, resonance        → pickaxes only
            //   momentum, blitz             → leggings only
            //   breadcrumb, recall, molt        → chestplates only
            //   census, verdant                 → hoes only
            //   brand, compendium           → axes only
            //   taunt, reflect              → shields only
            // Any future ability soul is excluded from all slots until added to soulAllowedForCategory().
            List<SoulData> candidates = forcedSoul != null
                ? List.of(forcedSoul)
                : soulList.stream()
                    .filter(s -> soulAllowedForCategory(s, catId))
                    .collect(java.util.stream.Collectors.toList());
            if (!candidates.isEmpty()) {
                SoulData soul = candidates.get(rng.nextInt(candidates.size()));
                // Witness and Compendium souls own the lore space entirely — their content
                // populates lore lines dynamically after generation. Strip any procedural
                // lore already added so the tooltip starts clean.
                if (soul.ability().equals("witness") || soul.ability().equals("compendium")) {
                    if (hasLore) {
                        // Remove 4 lore lines + leading gap (if present).
                        // Lore list at this point: [divider?, rarity, gap?, obs, his, sec, epi]
                        int toRemove = 4 + (lShowLoreGap ? 1 : 0);
                        for (int k = 0; k < toRemove && !lore.isEmpty(); k++)
                            lore.remove(lore.size() - 1);
                    }
                }
                if (lShowSoulGap) lore.add(Component.empty()); // breathing room
                lore.addAll(buildSoulFooter(soul.tag(), lSoulTagColor, lSoulGlyph));
                meta.getPersistentDataContainer().set(soulKey, PersistentDataType.STRING, soul.id());
            }
        }

        meta.lore(lore);

        // Mark every artifact unconditionally so the grindstone strip handler
        // can identify ObtuseLoot items even when no soul was assigned.
        meta.getPersistentDataContainer().set(artifactKey, PersistentDataType.BYTE, (byte) 1);

        // ── Shield pattern ────────────────────────────────────────────────────
        // Applied after lore is built so the same meta object carries both.
        // BlockStateMeta is the meta type for shields; we check instanceof to be safe.
        if (item.getType() == Material.SHIELD && meta instanceof BlockStateMeta blockMeta
                && this.shieldPatternEnabled) {
            applyShieldPattern(blockMeta, rarity, rng);
            // blockMeta IS meta — no re-assignment needed; setItemMeta below writes it.
        }

        item.setItemMeta(meta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shield pattern generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies a randomized heraldic banner pattern to the given {@link BlockStateMeta}.
     *
     * <p>Layer count is biased toward {@link #shieldMaxLayers} at higher rarities and
     * toward {@link #shieldMinLayers} at lower rarities. The base color and all layer
     * colors are chosen uniformly from {@link DyeColor#values()}.
     *
     * <p>The caller owns the meta and is responsible for calling
     * {@link ItemStack#setItemMeta(ItemMeta)} afterward — this method does not.
     *
     * @param blockMeta the shield's meta (must be a {@link BlockStateMeta} whose
     *                  block state is a {@link Banner}); no-op if the block state
     *                  is not a Banner (e.g. the material was changed between calls)
     * @param rarity    used to bias the layer count toward the configured maximum
     * @param rng       caller-supplied RNG for consistency
     */
    private void applyShieldPattern(BlockStateMeta blockMeta, Rarity rarity,
                                    ThreadLocalRandom rng) {
        if (!(blockMeta.getBlockState() instanceof Banner banner)) return;

        // ── Base color — fully random ─────────────────────────────────────────
        banner.setBaseColor(DYE_COLORS[rng.nextInt(DYE_COLORS.length)]);

        // ── Layer count — rarity-biased ───────────────────────────────────────
        // Snapshot volatile fields.
        final int    lMin    = this.shieldMinLayers;
        final int    lMax    = this.shieldMaxLayers;
        final int    range   = lMax - lMin;           // 0 if min == max
        final int    maxRank = RARITY_VALUES.length - 1; // 4 for the five tiers

        // Rarity fraction [0,1]: COMMON = 0, MYTHIC = 1.
        final double rarityFrac = (maxRank > 0) ? (rarity.level / (double) maxRank) : 1.0;

        // At COMMON: upper bound for the roll is (lMin + range/2); at MYTHIC: lMax.
        // This means COMMON rolls between lMin and mid-range, MYTHIC between lMin and lMax.
        final int rollMax = lMin + (int) Math.round(range * (0.5 + rarityFrac * 0.5));
        final int layers  = (rollMax > lMin)
                            ? lMin + rng.nextInt(rollMax - lMin + 1)
                            : lMin;

        // ── Layers ────────────────────────────────────────────────────────────
        PatternType[] pts = this.patternTypes;
        banner.setPatterns(Collections.emptyList()); // clear any inherited patterns
        if (pts.length > 0) {
            for (int i = 0; i < layers; i++) {
                PatternType pt  = pts[rng.nextInt(pts.length)];
                DyeColor    col = DYE_COLORS[rng.nextInt(DYE_COLORS.length)];
                banner.addPattern(new org.bukkit.block.banner.Pattern(col, pt));
            }
        }

        blockMeta.setBlockState(banner);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps a lowercase material name to a category dictionary ID, or {@code "generic"}
     * if no specific category matches.
     *
     * <p>Order is significant: {@code pickaxe} must precede {@code axe} because
     * IRON_PICKAXE's name contains both substrings. {@code crossbow} must precede {@code bow}
     * for the same reason.
     *
     * @return a category ID matching a key in {@link #dictionaries}, never {@code null}
     */
    private String getCategory(String n) {
        if (n.contains("pickaxe"))    return "pickaxes";   // before "axe"
        if (n.contains("sword"))      return "swords";
        if (n.contains("mace"))       return "maces";
        if (n.contains("spear"))      return "spears";
        if (n.contains("trident"))    return "tridents";
        if (n.contains("crossbow"))   return "crossbows";  // before "bow"
        if (n.contains("bow"))        return "bows";
        if (n.contains("axe"))        return "axes";
        if (n.contains("shovel"))     return "shovels";
        if (n.contains("hoe"))        return "hoes";
        if (n.contains("chestplate")) return "chestplates";
        if (n.contains("helmet"))     return "helmets";
        if (n.contains("leggings"))   return "leggings";
        if (n.contains("boots"))      return "boots";
        if (n.contains("elytra"))     return "elytra";
        if (n.contains("shield"))     return "shields";
        return "generic";
    }

    /**
     * Convenience overload — maps a {@link Material} to its category ID.
     * Delegates to {@link #getCategory(String)} so the lowercase-and-match
     * logic lives in exactly one place.
     */
    private String getCategory(Material m) {
        return getCategory(m.name().toLowerCase());
    }

    /**
     * Builds the two-line soul footer appended to an artifact's lore.
     *
     * <p>Line 1 — a thin separator rule in dark gray, styled to visually separate the
     * soul footer from the item's other lore content.
     *
     * <p>Line 2 — the soul tag centred between the soul glyph on each side, rendered in
     * the configured soul tag color with italic suppressed so it reads as a label, not flavour.
     *
     * <p>Example:
     * <pre>
     *   ────────────────────
     *   ✦  Witness Soul  ✦
     * </pre>
     *
     * @param tag      the soul's display tag string (e.g. {@code "[Witness Soul]"})
     * @param color    the soul tag color from config
     * @param glyph    the soul glyph character from config
     * @return an unmodifiable two-element list of {@link Component}s ready to append to lore
     */
    private List<Component> buildSoulFooter(String tag, NamedTextColor color, String glyph) {
        return List.of(
            Component.text("─────────────────────────", NamedTextColor.DARK_GRAY)
                     .decoration(TextDecoration.ITALIC, false),
            Component.text(glyph + "  " + tag + "  " + glyph, color)
                     .decoration(TextDecoration.ITALIC, false)
        );
    }

    /**
     * Returns {@code true} if the given soul is eligible to be assigned to an item
     * in the given category.
     *
     * <p>Souls with no ability are always eligible. Ability souls are restricted to
     * specific categories so they are never silently wasted on an item where their
     * effect cannot trigger. The mapping is:
     * boots (lantern, bloom, groundpound), bows+crossbows (mercy), crossbows (volley, surveyor),
     * maces (shatter, gravitywell), shovels (dowsing, bore), tridents (tracking, leash),
     * spears (surge, vault), swords (lifesteal, witness), helmets (echolocation, precognition),
     * pickaxes (magnetize, resonance), leggings (momentum, blitz), elytra (deaddrop, comet),
     * bows (ricochet), axes (brand, compendium), shields (taunt, reflect),
     * chestplates (breadcrumb, recall, molt), hoes (census, verdant).
     *
     * <p>Any ability ID not listed here returns {@code false} (excluded from all categories
     * until explicitly added to the switch).
     *
     * @param soul  the candidate soul
     * @param catId the item's category ID as returned by {@link #getCategory}
     * @return {@code true} if the soul may be assigned to this category
     */
    private boolean soulAllowedForCategory(SoulData soul, String catId) {
        if ("generic".equals(catId)) return false;     // generic items never receive souls
        if (!soul.hasAbility()) return true;           // particle-only souls fit any named slot
        return switch (soul.ability()) {
            case "lantern", "bloom", "groundpound" -> "boots".equals(catId);
            case "mercy"                           -> "bows".equals(catId);
            case "volley", "surveyor"              -> "crossbows".equals(catId);
            case "shatter", "gravitywell"          -> "maces".equals(catId);
            case "dowsing", "bore"                 -> "shovels".equals(catId);
            case "tracking", "leash"               -> "tridents".equals(catId);
            case "surge", "vault"                  -> "spears".equals(catId);
            case "lifesteal", "witness"             -> "swords".equals(catId);
            case "echolocation", "precognition"    -> "helmets".equals(catId);
            case "magnetize", "resonance"          -> "pickaxes".equals(catId);
            case "momentum", "blitz"               -> "leggings".equals(catId);
            case "deaddrop", "comet"               -> "elytra".equals(catId);
            case "ricochet"                        -> "bows".equals(catId);
            case "brand", "compendium"             -> "axes".equals(catId);
            case "taunt", "reflect"                -> "shields".equals(catId);
            case "breadcrumb", "recall", "molt"    -> "chestplates".equals(catId);
            case "census", "verdant"               -> "hoes".equals(catId);
            default                                -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String capitalize(String s) {
        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0)));
            sb.append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Resolves a {@link NamedTextColor} by name (case-insensitive), falling back to
     * {@code fallback} if the name is null or unrecognised.
     *
     * <p>Valid names match the keys returned by {@code NamedTextColor.NAMES}:
     * {@code black}, {@code dark_blue}, {@code dark_green}, {@code dark_aqua},
     * {@code dark_red}, {@code dark_purple}, {@code gold}, {@code gray},
     * {@code dark_gray}, {@code blue}, {@code green}, {@code aqua}, {@code red},
     * {@code light_purple}, {@code yellow}, {@code white}.
     */
    private static NamedTextColor parseColor(String name, NamedTextColor fallback) {
        if (name == null || name.isBlank()) return fallback;
        NamedTextColor c = NamedTextColor.NAMES.value(name.trim().toLowerCase());
        return c != null ? c : fallback;
    }

    /** Cached to avoid array allocation on every rarity roll. */
    private static final Rarity[] RARITY_VALUES = Rarity.values();

    /**
     * Rolls a weighted random rarity using the weights loaded from config.
     * Thread-safe: snapshots the volatile arrays at entry.
     */
    private Rarity rollRarity(ThreadLocalRandom rng) {
        int[] weights = this.rarityWeights;
        int   total   = this.rarityWeightTotal;
        int hit = rng.nextInt(total), cur = 0;
        for (int i = 0; i < RARITY_VALUES.length; i++) {
            cur += weights[i];
            if (hit < cur) return RARITY_VALUES[i];
        }
        return Rarity.COMMON;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit-target registry
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Describes a user-editable list: its backing file, YAML key, permission node,
     * and the live in-memory list it corresponds to.
     */
    private record EditTarget(String file, String yamlKey, String permission, List<String> liveList) {}

    /** Ordered for deterministic tab-completion. */
    private final Map<String, EditTarget> editTargets = new LinkedHashMap<>();

    private void buildEditTargets() {
        editTargets.clear();
        addEditTarget("prefixes",          "names/prefixes.yml",    "list",  "obtuseloot.edit.prefixes",   prefixes);
        addEditTarget("suffixes",          "names/suffixes.yml",    "list",  "obtuseloot.edit.suffixes",   suffixes);
        addEditTarget("lore.observations", "lore/observations.yml", "list",  "obtuseloot.edit.lore",       observations);
        addEditTarget("lore.histories",    "lore/histories.yml",    "list",  "obtuseloot.edit.lore",       histories);
        addEditTarget("lore.secrets",      "lore/secrets.yml",      "list",  "obtuseloot.edit.lore",       secrets);
        addEditTarget("lore.epithets",     "lore/epithets.yml",     "list",  "obtuseloot.edit.lore",       epithets);
        // Category dictionaries — sorted so tab-completion order is stable across reloads.
        new TreeMap<>(dictionaries).forEach((id, list) ->
            addEditTarget("categories." + id,
                          "categories/" + id + ".yml",
                          "names",
                          "obtuseloot.edit.categories",
                          list));
    }

    private void addEditTarget(String name, String file, String key, String perm, List<String> list) {
        editTargets.put(name, new EditTarget(file, key, perm, list));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Command handling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permission nodes:
     * <ul>
     *   <li>{@code obtuseloot.admin}            — reload, setloot, ability</li>
     *   <li>{@code obtuseloot.give}              — give</li>
     *   <li>{@code obtuseloot.givesoul}          — givesoul</li>
     *   <li>{@code obtuseloot.convert}           — convert</li>
     *   <li>{@code obtuseloot.inspect}           — inspect, findartifacts</li>
     *   <li>{@code obtuseloot.clearitem}         — clearitem</li>
     *   <li>{@code obtuseloot.clearinventory}    — clearinventory</li>
     *   <li>{@code obtuseloot.use}               — info, souls (default: true)</li>
     *   <li>{@code obtuseloot.edit}              — add/remove/list on all lists</li>
     *   <li>{@code obtuseloot.edit.prefixes}     — prefixes only</li>
     *   <li>{@code obtuseloot.edit.suffixes}     — suffixes only</li>
     *   <li>{@code obtuseloot.edit.lore}         — observations, histories, secrets, epithets</li>
     *   <li>{@code obtuseloot.edit.categories}   — all category files</li>
     * </ul>
     */
    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (args.length == 0) { sendHelp(s); return true; }

        switch (args[0].toLowerCase()) {
            case "help"      -> sendHelp(s);
            case "reload"    -> cmdReload(s);
            case "give"      -> cmdGive(s, args);
            case "givesoul"  -> cmdGiveSoul(s, args);
            case "convert"   -> cmdConvert(s, args);
            case "inspect"   -> cmdInspect(s, args);
            case "clearitem" -> cmdClearItem(s, args);
            case "clearinventory" -> cmdClearInventory(s, args);
            case "findartifacts"  -> cmdFindArtifacts(s, args);
            case "setloot"   -> {
                if (args.length < 2) { sendUsage(s, "/obtuseloot setloot <on|off>"); return true; }
                cmdSetLoot(s, args[1]);
            }
            case "ability"   -> {
                if (args.length < 3) { sendUsage(s, "/obtuseloot ability <id> <on|off>"); return true; }
                cmdSetAbility(s, args[1], args[2]);
            }
            case "info"      -> cmdInfo(s);
            case "souls"     -> {
                int page = 1;
                if (args.length >= 2) {
                    try { page = Integer.parseInt(args[1]); }
                    catch (NumberFormatException e) { sendError(s, "Page must be a number."); return true; }
                }
                cmdSouls(s, page);
            }
            case "add"     -> {
                if (args.length < 3) { sendUsage(s, "/obtuseloot add <list> <entry...>"); return true; }
                cmdAdd(s, args[1], joinArgs(args, 2));
            }
            case "remove"  -> {
                if (args.length < 3) { sendUsage(s, "/obtuseloot remove <list> <entry...>"); return true; }
                cmdRemove(s, args[1], joinArgs(args, 2));
            }
            case "list"    -> {
                if (args.length < 2) { sendUsage(s, "/obtuseloot list <list> [page]"); return true; }
                int page = 1;
                if (args.length >= 3) {
                    try { page = Integer.parseInt(args[2]); }
                    catch (NumberFormatException e) { sendError(s, "Page must be a number."); return true; }
                }
                cmdList(s, args[1], page);
            }
            default -> sendHelp(s);
        }
        return true;
    }

    private void cmdReload(CommandSender s) {
        if (!s.hasPermission("obtuseloot.admin")) { sendNoPerms(s); return; }
        loadAllData();
        s.sendMessage(Component.text("ObtuseLoot reloaded.", NamedTextColor.GREEN));
    }

    private void cmdGive(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.give")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;
        Rarity rarity = resolveRarity(s, args, 2);
        if (args.length >= 3 && rarity == null) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        ItemStack item = new ItemStack(ARTIFACT_MATERIALS[rng.nextInt(ARTIFACT_MATERIALS.length)]);
        applyArtifactMeta(item, rarity);
        giveOrDrop(target, item);

        target.sendMessage(Component.text("You received an ObtuseLoot artifact.", NamedTextColor.GOLD));
        if (!target.equals(s)) {
            s.sendMessage(Component.text("Gave an artifact to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void cmdConvert(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.convert")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;
        Rarity rarity = resolveRarity(s, args, 2);
        if (args.length >= 3 && rarity == null) return;

        ItemStack held = target.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            sendError(s, target.getName() + " is not holding anything.");
            return;
        }

        applyArtifactMeta(held, rarity);
        target.getInventory().setItemInMainHand(held);

        target.sendMessage(Component.text("Your held item was converted to an ObtuseLoot artifact.", NamedTextColor.GOLD));
        if (!target.equals(s)) {
            s.sendMessage(Component.text("Converted " + target.getName() + "'s held item.", NamedTextColor.GREEN));
        }
    }

    private void cmdInfo(CommandSender s) {
        if (!s.hasPermission("obtuseloot.use")) { sendNoPerms(s); return; }
        long enabledCount = abilityEnabled.values().stream().filter(Boolean.TRUE::equals).count();
        s.sendMessage(Component.text("── ObtuseLoot Info ──", NamedTextColor.GOLD));
        s.sendMessage(Component.text("Version: ", NamedTextColor.YELLOW)
            .append(Component.text(getDescription().getVersion(), NamedTextColor.WHITE)));
        s.sendMessage(Component.text("Souls loaded: ", NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(soulList.size()), NamedTextColor.WHITE)));
        s.sendMessage(Component.text("Abilities enabled: ", NamedTextColor.YELLOW)
            .append(Component.text(enabledCount + "/" + abilityEnabled.size(), NamedTextColor.WHITE)));
        s.sendMessage(Component.text("Loot enabled: ", NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf(lootEnabled), lootEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    private void cmdSouls(CommandSender s, int page) {
        if (!s.hasPermission("obtuseloot.use")) { sendNoPerms(s); return; }
        if (soulList.isEmpty()) { sendError(s, "No souls are currently loaded."); return; }
        int pages = Math.max(1, (int) Math.ceil(soulList.size() / (double) listPageSize));
        if (page < 1 || page > pages) { sendError(s, "Page " + page + " out of range (1–" + pages + ")."); return; }
        int start = (page - 1) * listPageSize;
        int end   = Math.min(start + listPageSize, soulList.size());
        s.sendMessage(Component.text(
            "── Souls [" + page + "/" + pages + "] ──", NamedTextColor.GOLD));
        for (int i = start; i < end; i++) {
            SoulData soul = soulList.get(i);
            String ability = soul.ability().isBlank() ? "particles only" : soul.ability();
            s.sendMessage(Component.text(soul.id(), NamedTextColor.YELLOW)
                .append(Component.text(" — " + soul.tag() + " (" + ability + ")", NamedTextColor.GRAY)));
        }
        if (page < pages)
            s.sendMessage(Component.text("Next: /obtuseloot souls " + (page + 1), NamedTextColor.DARK_GRAY));
    }

    private void cmdInspect(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.inspect")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;

        s.sendMessage(Component.text("── " + target.getName() + "'s souls ──", NamedTextColor.GOLD));
        var inv = target.getInventory();
        printSoulSlot(s, "Helmet",     inv.getHelmet());
        ItemStack chest = inv.getChestplate();
        printSoulSlot(s, chest != null && chest.getType() == Material.ELYTRA ? "Elytra" : "Chestplate", chest);
        printSoulSlot(s, "Leggings",   inv.getLeggings());
        printSoulSlot(s, "Boots",      inv.getBoots());
        printSoulSlot(s, "Main Hand",  inv.getItemInMainHand());
        printSoulSlot(s, "Off Hand",   inv.getItemInOffHand());
    }

    private void printSoulSlot(CommandSender s, String slotLabel, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            s.sendMessage(Component.text(slotLabel + ": ", NamedTextColor.GRAY)
                .append(Component.text("empty", NamedTextColor.DARK_GRAY)));
            return;
        }
        String soulId = item.getItemMeta().getPersistentDataContainer()
                            .get(soulKey, PersistentDataType.STRING);
        if (soulId == null) {
            s.sendMessage(Component.text(slotLabel + ": ", NamedTextColor.GRAY)
                .append(Component.text("no soul", NamedTextColor.DARK_GRAY)));
        } else {
            SoulData soul = activeSouls.get(soulId);
            String tag    = soul != null ? soul.tag() : soulId;
            s.sendMessage(Component.text(slotLabel + ": ", NamedTextColor.GRAY)
                .append(Component.text(tag, NamedTextColor.LIGHT_PURPLE)));
        }
    }

    private void cmdGiveSoul(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.givesoul")) { sendNoPerms(s); return; }
        if (args.length < 2) { sendUsage(s, "/obtuseloot givesoul [player] <soul> [rarity]"); return; }

        // Support self-target: if args[1] is a known soul ID, treat the sender as the target.
        Player target;
        int soulArgIdx;
        if (activeSouls.containsKey(args[1].toLowerCase()) || args.length < 3) {
            // args[1] looks like a soul ID (or there are too few args for player+soul+optional).
            // Fall back to sender as target.
            target = resolvePlayer(s, args, Integer.MAX_VALUE); // never reaches an arg — falls back
            if (target == null) return;
            soulArgIdx = 1;
        } else {
            target = resolvePlayer(s, args, 1);
            if (target == null) return;
            soulArgIdx = 2;
        }
        if (args.length <= soulArgIdx) { sendUsage(s, "/obtuseloot givesoul [player] <soul> [rarity]"); return; }

        String soulId = args[soulArgIdx].toLowerCase();
        SoulData soul = activeSouls.get(soulId);
        if (soul == null) {
            sendError(s, "Unknown soul '" + soulId + "'. Use /obtuseloot souls to list available souls.");
            return;
        }

        Rarity rarity = resolveRarity(s, args, soulArgIdx + 1);
        if (args.length >= soulArgIdx + 2 && rarity == null) return;

        // Filter ARTIFACT_MATERIALS to those whose category matches the soul's required
        // slot, so the soul ability actually triggers on the generated item.
        // Particle-only souls (no ability) and unknown abilities fall back to the full pool.
        // Uses precomputed categoryMaterials — no stream allocation at command time.
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Material[] pool = ARTIFACT_MATERIALS;
        if (soul.hasAbility()) {
            List<Material> filtered = new ArrayList<>();
            for (Map.Entry<String, Material[]> entry : categoryMaterials.entrySet()) {
                if (soulAllowedForCategory(soul, entry.getKey())) {
                    filtered.addAll(Arrays.asList(entry.getValue()));
                }
            }
            if (!filtered.isEmpty()) pool = filtered.toArray(new Material[0]);
            // If filtered is empty (ability maps to a category with no materials),
            // fall back silently to the full pool rather than throwing.
        }
        ItemStack item = new ItemStack(pool[rng.nextInt(pool.length)]);
        applyArtifactMeta(item, rarity, soul);
        giveOrDrop(target, item);

        target.sendMessage(Component.text("You received an artifact with " + soul.tag() + ".", NamedTextColor.GOLD));
        if (!target.equals(s))
            s.sendMessage(Component.text("Gave " + target.getName() + " an artifact with " + soul.tag() + ".", NamedTextColor.GREEN));
    }

    private void cmdClearItem(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.clearitem")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;

        ItemStack held = target.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) { sendError(s, target.getName() + " is not holding anything."); return; }
        if (!held.hasItemMeta()) { sendError(s, "Held item has no metadata to clear."); return; }

        var meta = held.getItemMeta();
        meta.getPersistentDataContainer().remove(soulKey);
        meta.getPersistentDataContainer().remove(artifactKey);
        meta.getPersistentDataContainer().remove(generatedKey);
        meta.displayName(null);
        meta.lore(null);
        held.setItemMeta(meta);
        target.getInventory().setItemInMainHand(held);

        target.sendMessage(Component.text("Your held item was cleared of all ObtuseLoot data.", NamedTextColor.GOLD));
        if (!target.equals(s))
            s.sendMessage(Component.text("Cleared ObtuseLoot data from " + target.getName() + "'s held item.", NamedTextColor.GREEN));
    }

    /**
     * Strips all ObtuseLoot PDC data from every item in a player's inventory
     * (armour, hotbar, main storage, and off-hand). Requires the stronger
     * {@code obtuseloot.clearinventory} permission rather than {@code clearitem}
     * because it is far more destructive.
     */
    private void cmdClearInventory(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.clearinventory")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;

        int cleared = 0;
        PlayerInventory inv = target.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;
            var meta = item.getItemMeta();
            var pdc  = meta.getPersistentDataContainer();
            // Both keys are stored as BYTE (presence flags). generatedKey lives on containers,
            // not items — never present here, so we don't check it.
            if (pdc.has(soulKey, PersistentDataType.STRING)
                    || pdc.has(artifactKey, PersistentDataType.BYTE)) {
                pdc.remove(soulKey);
                pdc.remove(artifactKey);
                meta.displayName(null);
                meta.lore(null);
                item.setItemMeta(meta);
                cleared++;
            }
        }

        target.sendMessage(Component.text(
            "All ObtuseLoot data cleared from your inventory (" + cleared + " item"
            + (cleared == 1 ? "" : "s") + " affected).", NamedTextColor.GOLD));
        if (!target.equals(s))
            s.sendMessage(Component.text(
                "Cleared ObtuseLoot data from " + target.getName() + "'s inventory ("
                + cleared + " item" + (cleared == 1 ? "" : "s") + " affected).", NamedTextColor.GREEN));
    }

    /**
     * Scans a player's full inventory (armour, hotbar, main storage, off-hand) and
     * lists every ObtuseLoot artifact found — showing rarity, soul, and slot position.
     * Uses the same {@code obtuseloot.inspect} permission as the existing inspect command.
     */
    private void cmdFindArtifacts(CommandSender s, String[] args) {
        if (!s.hasPermission("obtuseloot.inspect")) { sendNoPerms(s); return; }
        Player target = resolvePlayer(s, args, 1);
        if (target == null) return;

        s.sendMessage(Component.text("── Artifacts in " + target.getName() + "'s inventory ──", NamedTextColor.GOLD));
        PlayerInventory inv = target.getInventory();
        int found = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;
            var meta = item.getItemMeta();
            var pdc  = meta.getPersistentDataContainer();
            // artifactKey is stored as BYTE (presence flag). Wrong type always returns false.
            if (!pdc.has(artifactKey, PersistentDataType.BYTE)) continue;

            // Rarity is not stored in PDC — read the plain-text rarity label from lore.
            // Lore layout: [divider?, rarityLine, gap?, loreLine?, gap?, soulLine?]
            // The rarity label is the first non-empty lore line after an optional divider.
            String rarityStr = "?";
            if (meta.hasLore() && meta.lore() != null) {
                for (Component loreComp : meta.lore()) {
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                            .plainText().serialize(loreComp).trim();
                    if (!plain.isEmpty()) { rarityStr = plain; break; }
                }
            }

            String soulId    = pdc.get(soulKey, PersistentDataType.STRING);
            String soulLabel = soulId == null ? "no soul"
                    : (activeSouls.containsKey(soulId) ? activeSouls.get(soulId).tag() : soulId);
            String slotLabel = slotLabel(i);
            String itemName  = item.getType().name().replace("_", " ").toLowerCase();

            s.sendMessage(Component.text("  [" + slotLabel + "] ", NamedTextColor.DARK_GRAY)
                .append(Component.text(itemName, NamedTextColor.WHITE))
                .append(Component.text(" — " + rarityStr + " — " + soulLabel, NamedTextColor.GRAY)));
            found++;
        }
        if (found == 0)
            s.sendMessage(Component.text("  No artifacts found.", NamedTextColor.DARK_GRAY));
        else
            s.sendMessage(Component.text("  Total: " + found, NamedTextColor.YELLOW));
    }

    /** Maps Bukkit inventory slot index to a human-readable label. */
    private static String slotLabel(int slot) {
        if (slot == 36) return "boots";
        if (slot == 37) return "leggings";
        if (slot == 38) return "chestplate";
        if (slot == 39) return "helmet";
        if (slot == 40) return "off-hand";
        if (slot >= 0  && slot <= 8)  return "hotbar " + slot;
        if (slot >= 9  && slot <= 35) return "inv "    + slot;
        return "slot " + slot;
    }

    /**
     * Toggles the master loot-generation switch at runtime without requiring a reload.
     * The change is reflected immediately in {@link #onLootPopulate} but is NOT persisted
     * to config.yml — a reload will restore the config value.
     */
    private void cmdSetLoot(CommandSender s, String value) {
        if (!s.hasPermission("obtuseloot.admin")) { sendNoPerms(s); return; }
        boolean enable = switch (value.toLowerCase()) {
            case "on",  "true",  "enable",  "yes", "1" -> true;
            case "off", "false", "disable", "no",  "0" -> false;
            default -> { sendError(s, "Expected 'on' or 'off', got '" + value + "'."); yield lootEnabled; }
        };
        if (enable == lootEnabled) {
            s.sendMessage(Component.text(
                "Loot generation is already " + (lootEnabled ? "enabled" : "disabled") + ".",
                NamedTextColor.YELLOW));
            return;
        }
        lootEnabled = enable;
        s.sendMessage(Component.text(
            "Loot generation " + (lootEnabled ? "enabled" : "disabled") + ". "
            + "(Not persisted — reload to restore config value.)",
            lootEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    /**
     * Toggles a single soul ability on or off at runtime without a full reload.
     * Only abilities that are already known (present in {@link #abilityEnabled}) can be
     * toggled — unknown IDs are rejected to prevent typo-silently-no-oping.
     * The change is NOT persisted to config.yml — a reload restores config values.
     */
    private void cmdSetAbility(CommandSender s, String abilityId, String value) {
        if (!s.hasPermission("obtuseloot.admin")) { sendNoPerms(s); return; }
        String id = abilityId.toLowerCase();
        if (!abilityEnabled.containsKey(id)) {
            sendError(s, "Unknown ability '" + id + "'. Use /obtuseloot info to see loaded abilities.");
            return;
        }
        boolean enable = switch (value.toLowerCase()) {
            case "on",  "true",  "enable",  "yes", "1" -> true;
            case "off", "false", "disable", "no",  "0" -> false;
            default -> {
                sendError(s, "Expected 'on' or 'off', got '" + value + "'.");
                yield Boolean.TRUE.equals(abilityEnabled.get(id));
            }
        };
        // abilityEnabled is unmodifiable — replace with a new map containing the override.
        Map<String, Boolean> updated = new HashMap<>(abilityEnabled);
        updated.put(id, enable);
        abilityEnabled = Collections.unmodifiableMap(updated);
        // Push the updated map to ObtuseEngine so it reads the new value immediately.
        soulEngine.reload(activeSouls, abilityEnabled, abilityParams, soulTagColor, soulGlyph);
        s.sendMessage(Component.text(
            "Ability '" + id + "' " + (enable ? "enabled" : "disabled") + ". "
            + "(Not persisted — reload to restore config value.)",
            enable ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void cmdAdd(CommandSender s, String listName, String entry) {
        EditTarget t = editTargets.get(listName.toLowerCase());
        if (t == null) { sendError(s, "Unknown list '" + listName + "'."); return; }
        if (!canEdit(s, t)) { sendNoPerms(s); return; }
        if (entry.isBlank()) { sendError(s, "Entry cannot be blank."); return; }
        if (t.liveList().contains(entry)) {
            s.sendMessage(Component.text("\"" + entry + "\" is already in " + listName + ".", NamedTextColor.YELLOW));
            return;
        }
        t.liveList().add(entry);
        saveLiveList(t);
        s.sendMessage(Component.text("Added \"" + entry + "\" to " + listName
            + ". (" + t.liveList().size() + " entries)", NamedTextColor.GREEN));
    }

    private void cmdRemove(CommandSender s, String listName, String entry) {
        EditTarget t = editTargets.get(listName.toLowerCase());
        if (t == null) { sendError(s, "Unknown list '" + listName + "'."); return; }
        if (!canEdit(s, t)) { sendNoPerms(s); return; }
        if (!t.liveList().remove(entry)) {
            s.sendMessage(Component.text("\"" + entry + "\" was not found in " + listName + ".", NamedTextColor.YELLOW));
            return;
        }
        saveLiveList(t);
        s.sendMessage(Component.text("Removed \"" + entry + "\" from " + listName
            + ". (" + t.liveList().size() + " entries)", NamedTextColor.GREEN));
    }

    private void cmdList(CommandSender s, String listName, int page) {
        EditTarget t = editTargets.get(listName.toLowerCase());
        if (t == null) { sendError(s, "Unknown list '" + listName + "'."); return; }
        if (!canEdit(s, t)) { sendNoPerms(s); return; }
        List<String> entries = t.liveList();
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) listPageSize));
        if (page < 1 || page > pages) { sendError(s, "Page " + page + " out of range (1–" + pages + ")."); return; }
        int start = (page - 1) * listPageSize;
        int end   = Math.min(start + listPageSize, entries.size());
        s.sendMessage(Component.text(
            "── " + listName + " [" + page + "/" + pages + "] (" + entries.size() + " entries) ──",
            NamedTextColor.GOLD));
        for (int i = start; i < end; i++) {
            s.sendMessage(Component.text((i + 1) + ". " + entries.get(i), NamedTextColor.GRAY));
        }
        if (page < pages) {
            s.sendMessage(Component.text(
                "Next: /obtuseloot list " + listName + " " + (page + 1), NamedTextColor.DARK_GRAY));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Command helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the target player from command args at position {@code idx}.
     * Falls back to the sender if the arg is absent (sender must be a Player).
     *
     * @return the resolved player, or {@code null} if resolution failed (error already sent)
     */
    private Player resolvePlayer(CommandSender s, String[] args, int idx) {
        if (args.length > idx) {
            Player p = Bukkit.getPlayerExact(args[idx]);
            if (p == null) { sendError(s, "Player '" + args[idx] + "' not found or not online."); }
            return p;
        }
        if (!(s instanceof Player ps)) {
            sendError(s, "Specify a player name when running from console.");
            return null;
        }
        return ps;
    }

    /**
     * Returns the named rarity, or {@code null} if the argument is absent (caller
     * should treat as "random"). Also returns {@code null} and sends an error if the
     * argument is present but not a valid rarity name.
     */
    private Rarity resolveRarity(CommandSender s, String[] args, int idx) {
        if (args.length <= idx) return null;
        try {
            return Rarity.valueOf(args[idx].toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(s, "Unknown rarity '" + args[idx] + "'. Valid: "
                + Arrays.stream(RARITY_VALUES).map(Enum::name).collect(Collectors.joining(", ")));
            return null;
        }
    }

    private void giveOrDrop(Player p, ItemStack item) {
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(item);
        overflow.values().forEach(excess -> p.getWorld().dropItemNaturally(p.getLocation(), excess));
    }

    /**
     * Writes the live list to its backing YAML file.
     * Creates a new YamlConfiguration from scratch to avoid loading the old file
     * and potentially corrupting data if concurrent edits occurred.
     */
    private void saveLiveList(EditTarget t) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set(t.yamlKey(), new ArrayList<>(t.liveList()));
        File f = new File(getDataFolder(), t.file());
        try { cfg.save(f); }
        catch (IOException e) { getLogger().warning("Failed to save " + t.file() + ": " + e.getMessage()); }
    }

    private boolean canEdit(CommandSender s, EditTarget t) {
        return s.hasPermission("obtuseloot.edit") || s.hasPermission(t.permission());
    }

    private String joinArgs(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(Component.text("── ObtuseLoot ──", NamedTextColor.GOLD));
        s.sendMessage(cmd("/obtuseloot help",                                "Show this help message."));
        s.sendMessage(cmd("/obtuseloot info",                                "Plugin version, souls loaded, status."));
        s.sendMessage(cmd("/obtuseloot souls [page]",                        "List all soul types and their abilities."));
        s.sendMessage(cmd("/obtuseloot reload",                              "Reload all data and config from disk."));
        s.sendMessage(cmd("/obtuseloot setloot <on|off>",                    "Toggle loot generation at runtime."));
        s.sendMessage(cmd("/obtuseloot ability <id> <on|off>",               "Toggle a single ability at runtime."));
        s.sendMessage(cmd("/obtuseloot give [player] [rarity]",              "Give a random artifact."));
        s.sendMessage(cmd("/obtuseloot givesoul [player] <soul> [rarity]",   "Give an artifact with a specific soul."));
        s.sendMessage(cmd("/obtuseloot convert [player] [rarity]",           "Convert the target's held item."));
        s.sendMessage(cmd("/obtuseloot inspect [player]",                    "Show what souls a player is wearing."));
        s.sendMessage(cmd("/obtuseloot findartifacts [player]",              "List all OL artifacts in a player's inventory."));
        s.sendMessage(cmd("/obtuseloot clearitem [player]",                  "Strip OL data from held item."));
        s.sendMessage(cmd("/obtuseloot clearinventory [player]",             "Strip OL data from entire inventory."));
        s.sendMessage(cmd("/obtuseloot add <list> <entry...>",               "Add an entry to a list."));
        s.sendMessage(cmd("/obtuseloot remove <list> <entry...>",            "Remove an entry from a list."));
        s.sendMessage(cmd("/obtuseloot list <list> [page]",                  "Browse a list's entries."));
    }

    private Component cmd(String syntax, String desc) {
        return Component.text(syntax, NamedTextColor.YELLOW)
                        .append(Component.text(" — " + desc, NamedTextColor.GRAY));
    }

    private void sendUsage(CommandSender s, String usage) {
        s.sendMessage(Component.text("Usage: " + usage, NamedTextColor.YELLOW));
    }

    private void sendError(CommandSender s, String msg) {
        s.sendMessage(Component.text(msg, NamedTextColor.RED));
    }

    private void sendNoPerms(CommandSender s) {
        s.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab completion
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        // Guard: Bukkit can technically call with an empty args array.
        if (args.length == 0) return Collections.emptyList();

        String sub = args[0].toLowerCase();

        if (args.length == 1) {
            return Stream.of("help", "reload", "give", "givesoul", "convert", "inspect",
                             "clearitem", "clearinventory", "findartifacts",
                             "setloot", "ability", "info", "souls", "add", "remove", "list")
                .filter(o -> o.startsWith(sub))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (sub.equals("give") || sub.equals("convert") || sub.equals("inspect")
                    || sub.equals("clearitem") || sub.equals("clearinventory")
                    || sub.equals("findartifacts")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("givesoul")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("setloot")) {
                String partial = args[1].toLowerCase();
                return Stream.of("on", "off")
                    .filter(o -> o.startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("ability")) {
                String partial = args[1].toLowerCase();
                return activeSouls.keySet().stream()
                    .filter(id -> id.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
            }
            if (sub.equals("add") || sub.equals("remove") || sub.equals("list")) {
                String partial = args[1].toLowerCase();
                return editTargets.entrySet().stream()
                    .filter(e -> canEdit(sender, e.getValue()))
                    .map(Map.Entry::getKey)
                    .filter(k -> k.startsWith(partial))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (sub.equals("give") || sub.equals("convert")) {
                String partial = args[2].toUpperCase();
                return Arrays.stream(RARITY_VALUES)
                    .map(Enum::name)
                    .filter(n -> n.startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("givesoul")) {
                // arg[1] = player, arg[2] = soul id
                String partial = args[2].toLowerCase();
                return soulList.stream()
                    .map(SoulData::id)
                    .filter(id -> id.startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("ability")) {
                // arg[1] = soul id, arg[2] = on|off
                String partial = args[2].toLowerCase();
                return Stream.of("on", "off")
                    .filter(o -> o.startsWith(partial))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && sub.equals("givesoul")) {
            String partial = args[3].toUpperCase();
            return Arrays.stream(RARITY_VALUES)
                .map(Enum::name)
                .filter(n -> n.startsWith(partial))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rarity
    // ─────────────────────────────────────────────────────────────────────────

    enum Rarity {
        COMMON   ("Common",    NamedTextColor.WHITE,        0),
        RARE     ("Rare",      NamedTextColor.GREEN,        1),
        EPIC     ("Epic",      NamedTextColor.AQUA,         2),
        LEGENDARY("Legendary", NamedTextColor.GOLD,         3),
        MYTHIC   ("Mythic",    NamedTextColor.LIGHT_PURPLE, 4);

        final String         label;
        final NamedTextColor color;
        final int            level;

        Rarity(String label, NamedTextColor color, int level) {
            this.label = label;
            this.color = color;
            this.level = level;
        }
    }
}
