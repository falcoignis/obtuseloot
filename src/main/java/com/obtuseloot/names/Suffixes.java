package com.obtuseloot.names;

import java.util.Arrays;
import java.util.List;

/**
 * Default suffix entries — 500+ curated entries.
 * All suffixes are item-neutral and work for weapons, armor, and tools.
 * Server owners can replace the generated suffixes.yml freely; this class
 * is only used when the file does not yet exist.
 */
final class Suffixes {
    private Suffixes() {}

    static List<String> get() {
        return Arrays.asList(
            // ── Realms / Places ───────────────────────────────────────────
            "of the Abyss", "of the Ancients", "of the Barrows", "of the Catacombs", "of the Deep",
            "of the Eastern Reaches", "of the Farthest Shore", "of the Grave", "of the Hollow",
            "of the Iron Keep", "of the Jade Coast", "of the Killing Fields", "of the Lost Realm",
            "of the Molten Core", "of the Northern Wastes", "of the Old Country", "of the Pale Shore",
            "of the Quarry", "of the Ruined City", "of the Sunken Road", "of the Tidal Marches",
            "of the Underworld", "of the Vale", "of the Western Deep", "of the Xenolith",
            "of the Yellow Mountains", "of the Ashen Plain", "of the Black Coast",
            "of the Charnel Fields", "of the Dead Sea", "of the Endless Dark",
            "of the Forgotten Realm", "of the Grey Wastes", "of the Howling Dark",
            "of the Inner Dark", "of the Jade Mountains", "of the Known World",
            "of the Labyrinth", "of the Misty Vale", "of the Nether Reaches",
            "of the Obsidian Shore", "of the Pale Wastes", "of the Quiet Deep",
            "of the Rift", "of the Shattered Isles", "of the Twilight Shore",
            "of the Umbral Coast", "of the Void", "of the Whispering Dark",
            "of the Xenodrome", "of the Yawning Chasm",

            // ── Elements ──────────────────────────────────────────────────
            "of Ash", "of Brimstone", "of Cinders", "of the Deep Flame", "of Embers",
            "of Flame", "of Frost", "of the Glaciers", "of Hail", "of Ice",
            "of the Inferno", "of the Jade Flame", "of the Killing Frost", "of Lightning",
            "of the Maelstrom", "of the Nightfire", "of the Old Flame", "of the Permafrost",
            "of the Quenched Flame", "of Rain", "of the Storm", "of Thunder",
            "of the Unquenched", "of Void Fire", "of the Winter Storm",
            "of the Ashen Wind", "of the Burning Cold", "of the Cold Deep",
            "of the Dead Flame", "of the Ember Sea", "of the Frozen North",
            "of the Grey Storm", "of the Howling Wind", "of the Iron Wind",
            "of the Jade Storm", "of the Killing Wind", "of the Last Frost",
            "of the Molten Deep", "of the Northern Storm", "of the Old Ice",
            "of the Pale Flame", "of the Quiet Storm", "of the Rift Storm",
            "of the Shattered Ice", "of the Tidal Storm", "of the Undying Flame",
            "of the Void Storm", "of the Winter Sea", "of the Zenith Storm",
            "of the Ashen Storm", "of the Black Ice",

            // ── Abstract Concepts ─────────────────────────────────────────
            "of Anguish", "of Betrayal", "of Chaos", "of Despair", "of Endurance",
            "of Fate", "of Glory", "of Hatred", "of Inevitability", "of Judgment",
            "of Knowing", "of Loss", "of Madness", "of Necessity", "of Oblivion",
            "of Pain", "of Quiet", "of Ruin", "of Sorrow", "of Torment",
            "of Undoing", "of Vengeance", "of Wrath", "of Xenogenesis", "of Yearning",
            "of Abandon", "of Burden", "of Conviction", "of Doubt", "of Entropy",
            "of Failure", "of Grief", "of Hunger", "of Ignorance", "of Justice",
            "of Killing", "of Longing", "of Malice", "of Nihilism", "of Obsession",
            "of Purpose", "of Questioning", "of Regret", "of Sacrifice", "of Triumph",
            "of Uncertainty", "of Violence", "of War", "of Xenophobia", "of Zeal",
            "of Absolution", "of Bitterness", "of Contempt", "of Dread", "of Exile",

            // ── Time ──────────────────────────────────────────────────────
            "of Ages Past", "of the Bygone Era", "of the Coming Dark", "of the Dead Age",
            "of the Dying Days", "of the End Times", "of the Fallen Age", "of the Forgotten Age",
            "of the Grey Age", "of the Hollow Years", "of the Iron Age", "of the Jade Era",
            "of the Known Ages", "of the Last Age", "of the Long Night",
            "of the Midnight Hour", "of the New Dawn", "of the Old Wars",
            "of the Passing Age", "of the Quiet Years", "of the Ruined Age",
            "of the Second Age", "of the Third Age", "of the Undying Age",
            "of the Vanished Era", "of the Waning Days", "of the Xenolith Age",
            "of the Yore", "of the Zero Hour", "of the Ashen Age",
            "of the Black Years", "of the Crumbling Age", "of the Dying Era",
            "of the Elder Days", "of the Forsaken Age", "of the Golden Age",
            "of the Hallowed Era", "of the Iron Years", "of the Jade Age",
            "of the Killing Years", "of the Lost Age", "of the Mourning Age",
            "of the Night Eternal", "of the Old Kingdom", "of the Pale Era",
            "of the Quiet Age", "of the Rift Age", "of the Sundering",
            "of the Twilight Age", "of the Undying Era",

            // ── Creatures / Beasts ────────────────────────────────────────
            "of the Asp", "of the Bear", "of the Crow", "of the Dragon", "of the Eagle",
            "of the Fallen King", "of the Ghost", "of the Hydra", "of the Iron Wolf",
            "of the Jade Serpent", "of the Kin", "of the Leviathan", "of the Manticore",
            "of the Nightstalker", "of the Old Wolf", "of the Phantom", "of the Questing Beast",
            "of the Raven", "of the Serpent", "of the Thunderbird", "of the Undying Beast",
            "of the Viper", "of the Wyvern", "of the Xenobeast", "of the Yeti",
            "of the Ancient Serpent", "of the Black Wolf", "of the Cold Drake",
            "of the Dire Wolf", "of the Elder Dragon", "of the Fallen Beast",
            "of the Grey Wolf", "of the Howling Beast", "of the Iron Drake",
            "of the Jade Dragon", "of the Killing Beast", "of the Last Dragon",
            "of the Midnight Raven", "of the Night Crow", "of the Old Drake",
            "of the Pale Wolf", "of the Quiet Beast", "of the Rift Drake",
            "of the Shadow Wolf", "of the Tidal Serpent", "of the Undying Wolf",
            "of the Void Drake", "of the Winter Wolf", "of the Zenith Dragon",
            "of the Ashen Drake", "of the Black Raven",

            // ── People / Legacy ───────────────────────────────────────────
            "of the Betrayed", "of the Condemned", "of the Damned", "of the Fallen",
            "of the Forsaken", "of the Grateful Dead", "of the Hollow King",
            "of the Imprisoned", "of the Jade King", "of the Kinslayer",
            "of the Lost", "of the Marked", "of the Nameless", "of the Outlawed",
            "of the Penitent", "of the Quiet Dead", "of the Risen",
            "of the Scattered", "of the True King", "of the Unnamed",
            "of the Vanquished", "of the Wandering Dead", "of the Xenos",
            "of the Yearning Dead", "of the Abandoned", "of the Broken King",
            "of the Cast Out", "of the Dead King", "of the Exiled",
            "of the Sunken King", "of the Grey King", "of the Hollow Queen",
            "of the Iron King", "of the Jade Queen", "of the Killing King",
            "of the Last King", "of the Mourning Dead", "of the Night Dead",
            "of the Old King", "of the Pale Queen", "of the Quiet King",
            "of the Rift King", "of the Shadow King", "of the Twilight King",
            "of the Undying King", "of the Void King", "of the Winter King",
            "of the Zenith King", "of the Ashen King",

            // ── Celestial ─────────────────────────────────────────────────
            "of the Black Moon", "of the Comet", "of the Dark Star",
            "of the Dead Moon", "of the Eclipse", "of the Falling Star",
            "of the First Moon", "of the Grey Moon", "of the Hollow Star",
            "of the Iron Moon", "of the Jade Star", "of the Known Stars",
            "of the Last Moon", "of the Midnight Sun", "of the New Moon",
            "of the Night Sky", "of the Old Moon", "of the Pale Moon",
            "of the Quiet Star", "of the Red Moon", "of the Second Moon",
            "of the Shattered Moon", "of the Twilight Star", "of the Undying Star",
            "of the Void Star", "of the Wandering Star", "of the Winter Moon",
            "of the Zenith Star", "of the Ashen Moon", "of the Bleeding Moon",
            "of the Cold Star", "of the Dying Star", "of the Elder Moon",
            "of the Forsaken Star", "of the Golden Moon", "of the Howling Star",
            "of the Iron Star", "of the Jade Moon", "of the Killing Star",
            "of the Lost Star", "of the Mourning Moon", "of the Northern Star",
            "of the Omen Star", "of the Pale Star", "of the Rift Moon",
            "of the Shadow Moon", "of the Sundering Star", "of the Twin Moons",
            "of the Unnamed Star",

            // ── War / Death ───────────────────────────────────────────────
            "of the Battlefield", "of the Broken Army", "of the Culling",
            "of the Dead March", "of the Eternal War", "of the Fallen Field",
            "of the Final Battle", "of the Grey War", "of the Hollow War",
            "of the Iron Battle", "of the Jade War", "of the Killing Ground",
            "of the Last Battle", "of the Massacre", "of the Night War",
            "of the Bitter Wars", "of the Pale March", "of the Quiet War",
            "of the Rift War", "of the Siege", "of the Thousand Battles",
            "of the Undying War", "of the Void War", "of the Warlord",
            "of the Ashen War", "of the Black War", "of the Cold War",
            "of the Dying War", "of the Elder War", "of the Forsaken Battle",
            "of the Grinding War", "of the Hollow Battle", "of the Iron March",
            "of the Jade Battle", "of the Long March", "of the Mourning War",
            "of the Northern War", "of the Old Battle", "of the Pale War",
            "of the Quiet Battle", "of the Rift Battle", "of the Shadow War",
            "of the Sundering War", "of the Twilight War", "of the Undying Battle",
            "of the Void Battle", "of the Winter War", "of the Zenith Battle",
            "of the Ashen Battle", "of the Blood War",

            // ── Nature / Wild ─────────────────────────────────────────────
            "of the Ashen Wood", "of the Black Forest", "of the Cold River",
            "of the Dead River", "of the Deep Forest", "of the Dying Wood",
            "of the Elder Forest", "of the Fallen Grove", "of the Grey Forest",
            "of the Harvest", "of the Hollow Wood", "of the Iron Forest",
            "of the Jade Forest", "of the Bitter Frost", "of the Last Grove",
            "of the Midnight Wood", "of the North", "of the Old Forest",
            "of the Pale Wood", "of the Quiet Grove", "of the Rift Wood",
            "of the Shadow Wood", "of the Tidal Shore", "of the Undying Grove",
            "of the Void Wood", "of the Wandering River", "of the Winter Wood",
            "of the Zenith Grove", "of the Amber Grove", "of the Bitter Shore",
            "of the Crumbling Cliff", "of the Drowned Wood", "of the Endless Shore",
            "of the Frozen River", "of the Grey Shore", "of the Howling Wood",
            "of the Iron Shore", "of the Jade Grove", "of the Killing Shore",
            "of the Lost Grove", "of the Mourning Wood", "of the Night Grove",
            "of the Old Shore", "of the Pale Grove", "of the Quiet Shore",
            "of the Rift Grove", "of the Sunken Shore", "of the Twilight Grove",
            "of the Undying Shore", "of the Waning Wood",

            // ── Miscellaneous / Abstract Places ───────────────────────────
            "of the Broken Path", "of the Cairn", "of the Dead Road",
            "of the Empty Throne", "of the Forsaken Road", "of the Hollow Throne",
            "of the Iron Road", "of the Last Road", "of the Midnight Path",
            "of the Nameless Road", "of the Old Road", "of the Ruined Road",
            "of the Sunken Path", "of the Wandering Road", "of the Blind Road",
            "of the Circling Dark", "of the Rift Road", "of the Shadow Path",
            "of the Twilight Road", "of the Undying Path", "of the Void Road",

            // ── Deities — Greek / Roman ───────────────────────────────────────
            // Named deity suffixes earn weight from real mythology while reading
            // as natural item lore to players unfamiliar with the source.
            "of the Styx", "of Elysium", "of Tartarus", "of Olympus",
            "of Prometheus", "of Hephaestus", "of the Fates", "of the Furies",
            "of Hecate", "of Nyx", "of Erebus", "of Morpheus", "of Thanatos",
            "of Nemesis", "of Eris", "of Persephone", "of Charon",

            // ── Deities — Norse ────────────────────────────────────────────────
            "of Valhalla", "of Yggdrasil", "of Asgard", "of Helheim",
            "of the World Tree", "of Fenrir", "of the Norns", "of Skadi",
            "of Hel", "of Loki", "of the Allfather",

            // ── Deities — Egyptian ─────────────────────────────────────────────
            "of Anubis", "of Osiris", "of Sekhmet", "of Thoth",
            "of Apophis", "of Set", "of Sobek", "of Khonsu",

            // ── Deities — Mesopotamian ─────────────────────────────────────────
            "of Tiamat", "of Nergal", "of Ereshkigal", "of Pazuzu",
            "of Marduk", "of Ishtar", "of Enki", "of the Void Serpent",

            // ── Deities — Celtic ───────────────────────────────────────────────
            "of the Morrigan", "of Cernunnos", "of the Dagda", "of Arawn",
            "of the Tuatha",

            // ── Deities — Slavic / Mesoamerican ───────────────────────────────
            "of Chernobog", "of Perun", "of Veles", "of Quetzalcoatl",
            "of Xibalba", "of Kukulkan", "of Mictlan",

            // ── Cryptids — well-known ─────────────────────────────────────────
            "of the Mothman", "of the Wendigo", "of the Chupacabra",
            "of the Skinwalker", "of the Jersey Devil", "of the Black Shuck",
            "of the Yeti", "of the Kraken",

            // ── Cryptids — obscure ────────────────────────────────────────────
            // The Flatwoods Monster (1952, WV), Dover Demon (1977, MA),
            // Fresno Nightcrawlers (CA, video footage), Batsquatch (WA),
            // Hodag (WI logging camp legend), Bunyip (Aboriginal Australian),
            // Ahool (giant bat, Java), Loveland Frogman (OH), Mongolian Death Worm,
            // Enfield Horror (IL, 1973), Ogopogo (Lake Okanagan, BC),
            // Snallygaster (MD), Sheepsquatch (WV), Van Meter Visitor (IA, 1903).
            "of the Flatwoods Monster", "of the Dover Demon",
            "of the Fresno Nightcrawler", "of the Batsquatch",
            "of the Hodag", "of the Bunyip", "of the Ahool",
            "of the Loveland Frogman", "of the Mongolian Death Worm",
            "of the Enfield Horror", "of the Ogopogo",
            "of the Snallygaster", "of the Sheepsquatch",
            "of the Van Meter Visitor",

            // ── Gaming — Minecraft / Terraria / Stardew Valley ────────────────
            // Minecraft: locations and legends that read as natural dark-fantasy lore.
            // Terraria: biomes and bosses — the Moon Lord and Hallow read as mythology.
            // Stardew Valley: the Junimos and Stardrop are already gently mythological.
            // Bahamut and Gilgamesh are kept — both are genuine ancient mythology that
            // happens to recur in games.
            "of Herobrine", "of the Far Lands", "of the First Night", "of the Ancient City",
            "of the Deep Dark", "of the Stronghold", "of the Warden",
            "of the Crimson", "of the Wall of Flesh", "of the Moon Lord",
            "of the Hallow", "of the Corruption", "of the Blood Moon", "of the Underworld",
            "of the Stardrop", "of the Junimos",
            "of Bahamut", "of Gilgamesh",

            // ── Demonology — Abrahamic / Ars Goetia ──────────────────────────
            "of Abaddon", "of Asmodeus", "of Belial", "of Mammon", "of Moloch",
            "of Baphomet", "of Azazel", "of Beelzebub", "of Samael", "of Malphas",

            // ── Lovecraftian ──────────────────────────────────────────────────
            // The cursed towns (Arkham, Innsmouth, Dunwich), the sunken city (R'lyeh),
            // and the layered names for the pantheon (Elder Gods, Great Old Ones, Deep Ones).
            "of Cthulhu", "of Azathoth", "of Dagon", "of R'lyeh",
            "of Arkham", "of Innsmouth", "of Dunwich",
            "of the Elder Gods", "of the Great Old Ones", "of the Deep Ones",
            "of the Outer Dark",

            // ── Japanese mythology / folklore ─────────────────────────────────
            "of Izanami", "of Susanoo", "of Ryujin", "of Raijin",
            "of the Tengu", "of the Kitsune", "of the Oni",
            "of the Jorogumo", "of the Gashadokuro", "of the Baku",

            // ── Hindu mythology ───────────────────────────────────────────────
            "of Kali", "of Ravana", "of Indra", "of Yama", "of Durga",

            // ── Folklore creatures ────────────────────────────────────────────
            "of the Banshee", "of the Kelpie", "of the Nuckelavee",
            "of the Strigoi", "of the Carbuncle",

            // ── Suggestive — all have legitimate primary meanings ─────────────
            // The double reading should be discovered, not telegraphed.
            // Removed anything too blunt; what remains earns its reading
            // only in combination with a suggestive prefix or generic name.
            "of the Long Reach", "of the Deep Passage", "of the Hidden Hollow",
            "of the Secret Chamber", "of the Forbidden Depth", "of the Narrow Pass",
            "of the Soft Hollow", "of the Warm Hollow", "of the Secret Depth",
            "of the Forbidden Hollow", "of the Deep Hollow", "of the Long Hollow",
            "of the Waiting Dark", "of the Hungry Dark", "of the Restless Night",
            "of the Fevered Dark", "of the Trembling Deep", "of the Urgent Dark",
            "of the Breathless Dark", "of the Mounting Dark", "of the Rising Dark",
            "of the Long Ache", "of the Deep Ache", "of the Endless Want",
            "of the Quiet Want", "of the Slow Burn", "of the Long Burn",
            "of the Deep Want", "of the Hidden Want", "of the Secret Want",
            "of Endless Longing", "of the Quiet Longing", "of the Deep Longing",
            "of the Restless Deep", "of the Hungry Deep",
            "of the Waiting Hollow", "of the Trembling Hollow", "of the Aching Deep",
            "of the Soft Dark", "of the Warm Dark", "of the Welcoming Dark",
            "of the Inviting Hollow", "of the Wanting Dark", "of the Yielding Deep",
            "of the Willing Hollow", "of the Surrendered Deep",
            "of the Long Embrace", "of the Warm Embrace", "of the Close Embrace",
            "of the Slick Dark", "of the Taut Reach", "of the Heady Dark",
            "of the Roused Deep", "of the Furtive Hollow", "of the Glistening Deep",
            "of the Long Shaft", "of the Hidden Shaft", "of the Firm Grip",
            "of the Long Stroke", "of the Rising Swell", "of the Deep Plunge",
            "of the Slow Swell", "of Quiet Surrender", "of the Parted Veil",
            "of the Spent Flame", "of the Open Bloom", "of the First Touch",
            "of the Long Pull", "of the Arching Dark", "of the Flowering Dark",
            "of the Yielding Earth", "of the Panting Dark", "of the Close Press"
        );
    }
}
