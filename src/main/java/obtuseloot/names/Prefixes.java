package obtuseloot.names;

import java.util.Arrays;
import java.util.List;

/**
 * Default prefix entries — 500+ curated entries.
 * All prefixes are item-neutral and work for weapons, armor, and tools.
 * Server owners can replace the generated prefixes.yml freely; this class
 * is only used when the file does not yet exist.
 */
final class Prefixes {
    private Prefixes() {}

    static List<String> get() {
        return Arrays.asList(
            // ── Elemental ─────────────────────────────────────────────────
            "Ashen", "Blazing", "Cinder", "Dawnfire", "Emberlit",
            "Frostborn", "Glacial", "Hailstruck", "Icebound", "Jadestone",
            "Kindled", "Lavastruck", "Molten", "Northwind", "Obsidian",
            "Pyretic", "Quicksilver", "Rimefrost", "Stormborn", "Thunderstruck",
            "Umbral", "Voltaic", "Windcarved", "Zephyr", "Ashcloud",
            "Blizzard", "Coalfire", "Deepfrost", "Emberstruck", "Frostbitten",
            "Glassfire", "Hallowed Flame", "Icehewn", "Jadechill", "Kindle",
            "Lightning", "Magmaborn", "Nightfire", "Overcast", "Permafrost",
            "Quickflame", "Rimestone", "Stormscarred", "Thornfire", "Undying Flame",
            "Voidfire", "Winterborn", "Xenoflame", "Yewfrost", "Zenithal",

            // ── Temporal / Age ────────────────────────────────────────────
            "Ancient", "Archaic", "Bygone", "Centuried", "Decayed",
            "Eternal", "Fossilized", "Graveworn", "Heirloom", "Immemorial",
            "Jadeworn", "Kingsage", "Lingering", "Millennial", "Nightold",
            "Oldworld", "Primeval", "Quenched", "Reliquary", "Senescent",
            "Timeless", "Undying", "Venerable", "Weathered", "Xenolith",
            "Yore", "Ageless", "Bronzeage", "Crumbling", "Decrepit",
            "Elder", "Faded", "Greybeard", "Hoarfrost", "Ironage",
            "Kingsgrave", "Lich", "Mossgrown", "Nether",
            "Ossified", "Petrified", "Quarried", "Rusted", "Stoneworn",
            "Tarnished", "Unaging", "Vintage", "Worn", "Zealot",

            // ── Dark / Void ───────────────────────────────────────────────
            "Abyssal", "Benighted", "Cursed", "Darkened", "Eclipsed",
            "Forsaken", "Grievous", "Haunted", "Inkstained", "Joyless",
            "Knell", "Lost", "Malevolent", "Nightbound", "Ominous",
            "Phantom", "Quietdark", "Riftborn", "Shadowed", "Tenebrous",
            "Unlit", "Voidborn", "Wailing", "Xeric", "Yearning",
            "Accursed", "Blighted", "Crestfallen", "Doomed", "Embargoed",
            "Forlorn", "Gravemarked", "Hexed", "Ill-Starred", "Jadecurse",
            "Knotted", "Lamenting", "Mournful", "Nullbound", "Obscured",
            "Plagued", "Quieted", "Ruinous", "Shrouded", "Tainted",
            "Umbrous", "Veiled", "Wrathbound", "Xerotic", "Yellowed",

            // ── Light / Sacred ────────────────────────────────────────────
            "Aureate", "Blessed", "Consecrated", "Divine", "Exalted",
            "Faithful", "Gilded", "Hallowed", "Illumined", "Jade",
            "Kindred", "Luminous", "Mirrorlit", "Noble", "Ordained",
            "Purified", "Radiant", "Sacred", "Transcendent", "Unsullied",
            "Virtuous", "Whitegold", "Xenial", "Yielding", "Zealous",
            "Anointed", "Brilliant", "Celestial", "Dawnlit", "Effulgent",
            "Fulgent", "Gleaming", "Heavenborn", "Incandescent", "Justified",
            "Knighted", "Lambent", "Moonlit", "Nimbus", "Opaline",
            "Pearlescent", "Quickened", "Resplendent", "Sunlit", "Truthbound",
            "Undimmed", "Vestal", "Whitened", "Xenodochial", "Yielded",

            // ── Nature / Earth ────────────────────────────────────────────
            "Ashwood", "Barkworn", "Coralwoven", "Deeproot", "Earthbound",
            "Fernwrapped", "Grimwood", "Heathborn", "Ironwood", "Jadevine",
            "Kelp", "Loam", "Marshborn", "Nethervine", "Oakbound",
            "Peatborn", "Quartz", "Rootbound", "Stoneborn", "Thornwoven",
            "Underroot", "Vineborn", "Wildgrown", "Xerophyte", "Yewbound",
            "Algal", "Boulderborn", "Cragborn", "Duneworn", "Elfwood",
            "Fieldborn", "Granite", "Hillborn", "Ivorywood", "Junglehewn",
            "Knottedwood", "Lodeshard", "Mossworn", "Nightbloom", "Oreborn",
            "Pineworn", "Quillwood", "Riftvine", "Siltborn", "Tidewrack",
            "Underwood", "Verdant", "Willowworn", "Xenowood", "Yellowstone",

            // ── Myth / Lore ───────────────────────────────────────────────
            "Aegis", "Betrayed", "Chronicled", "Dreaded", "Eldritch",
            "Fabled", "Ghostforged", "Heraldic", "Imperious", "Jaded",
            "Kingsmark", "Legendary", "Mythic", "Nightsung", "Orcish",
            "Prophetic", "Runic", "Spectral", "Titanic",
            "Unsung", "Voidsong", "Wanderer", "Xenologue", "Yarrow",
            "Anathema", "Bardic", "Cronicled", "Doomsung", "Enchanted",
            "Fatewoven", "Grimoire", "Hexforged", "Iconic", "Jinxed",
            "Kingsborn", "Lorewoven", "Mystic", "Nightlore", "Omenbound",
            "Prophesied", "Questbound", "Riddle", "Sagesong", "Talesong",
            "Uncharted", "Visionbound", "Wayfarer", "Xenocrypt", "Zodiac",

            // ── Conflict / Battle ─────────────────────────────────────────
            "Battleborn", "Conflicted", "Deathmarch", "Embattled", "Felled",
            "Graven", "Hardened", "Ironclad", "Jadewrought", "Killmark",
            "Last Stand", "Militant", "Nightwatch", "Outrider", "Proven",
            "Quickstrike", "Raider", "Scarred", "Tireless", "Unbowed",
            "Veteran", "Warchief", "Xenocide", "Yearlong", "Zealbound",
            "Ambush", "Besieger", "Contested", "Dire", "Entrenched",
            "Fallen", "Garrison", "Honed", "Ironforged", "Jackhammer",
            "Kinslayer", "Lancer", "Marked", "Nettlesome", "Outflanked",
            "Relentless", "Stalwart", "Tested", "Unyielding",
            "Vigil", "Warpainted", "Xenowatch", "Yoked", "Zone",

            // ── Color / Material ──────────────────────────────────────────
            "Amaranth", "Bronze", "Cobalt", "Duskgold", "Ebony",
            "Fulvous", "Gilt", "Heliotrope", "Ivory", "Jadite",
            "Kernite", "Lazurite", "Malachite", "Nightsilver", "Opal",
            "Pewter", "Quartzite", "Rufous", "Sable", "Topaz",
            "Ultramarine", "Vermeil", "Whitesteel", "Yewsilver",
            "Alabaster", "Beryl", "Cinnabar", "Diorite", "Enamel",
            "Flint", "Graphite", "Haematite", "Ilmenite", "Jasperous",
            "Kyanite", "Lignite", "Magnetite", "Nacre", "Onyx",
            "Porphyry", "Rhodonite", "Slate", "Tektite",
            "Umber", "Viridian", "Woad", "Xenolite", "Zircon",

            // ── Emotion / State ───────────────────────────────────────────
            "Anguished", "Berserk", "Callous", "Despairing", "Enraged",
            "Furious", "Grieving", "Hollow", "Implacable",
            "Maddened", "Numb", "Obsessed",
            "Pitiless", "Quiet", "Remorseless", "Sorrowful", "Tormented",
            "Unbroken", "Vengeful", "Wrathful", "Xenophobic",
            "Ardent", "Bereaved", "Contemptuous", "Defiant", "Exasperated",
            "Fanatical", "Hateful", "Indifferent", "Jealous",
            "Knowing", "Languid", "Melancholic", "Nihilistic",
            "Proud", "Resolute", "Seething", "Tranquil",
            "Violent", "Weeping", "Xeransis",

            // ── Rare / Unusual ────────────────────────────────────────────
            "Aberrant", "Boundless", "Cavernous", "Consuming", "Daunting",
            "Enduring", "Ferocious", "Grinding", "Harrowing",
            "Jagged", "Knellborn", "Liminal", "Merciless", "Nefarious",
            "Oppressive", "Perilous", "Quelling", "Sinister",
            "Terrible", "Unstoppable", "Vexing", "Withering",
            "Adamant", "Bristling", "Colossal", "Dread", "Fearsome",
            "Grim", "Haggard", "Iron", "Judicious",
            "Lithe", "Menacing", "Notorious", "Oathbound", "Portentous",
            "Raging", "Savage", "Unforgiving",
            "Voracious", "Wicked", "Yieldless",

            // ── Suggestive — all have legitimate primary meanings ─────────────
            // Earn a second reading when combined with generic names or hollow/deep suffixes.
            // Subtle over explicit — the double reading should be discovered, not announced.
            "Supple", "Tender", "Aching", "Eager", "Hungry",
            "Smoldering", "Surging", "Rising", "Mounting", "Plunging",
            "Sinuous", "Writhing", "Feverish", "Simmering", "Fervent",
            "Breathless", "Stirring", "Swelling", "Searching", "Pliant",
            "Drifting", "Restless", "Trembling", "Quivering",
            "Heaving", "Persistent", "Insistent", "Urgent", "Impassioned",
            "Devoted", "Flushed", "Fevered", "Wistful", "Pulsing",
            "Wanting", "Entwined", "Pressing", "Straining", "Reaching",
            "Fluid", "Clinging", "Undulating", "Beckoning", "Inviting",
            "Longing", "Craving", "Burning", "Hungering",
            "Glistening", "Taut", "Yielding", "Roused", "Heady",
            "Slick", "Furtive", "Arching", "Parting", "Spent",
            "Blushing", "Languorous", "Supine", "Flush", "Probing",
            "Surrendered", "Bare", "Coiling", "Panting", "Unfurling", "Overcome",

            // ── Deities — Greek / Roman ────────────────────────────────────────
            // Adjective forms blend naturally into item names; bare names read as epithets.
            "Stygian", "Elysian", "Hadean", "Tartarean", "Promethean",
            "Olympian", "Hyperborean", "Chthonic",
            "Hecate", "Nyx", "Erebus", "Morpheus", "Thanatos",
            "Nemesis", "Eris", "Persephone", "Selene", "Helios",

            // ── Deities — Norse ────────────────────────────────────────────────
            "Fenrir", "Skadi", "Valkyric", "Huginn", "Muninn",

            // ── Deities — Egyptian ─────────────────────────────────────────────
            "Sobek", "Thoth", "Khonsu", "Sekhmet", "Apophis", "Osirian",

            // ── Deities — Mesopotamian ─────────────────────────────────────────
            "Tiamat", "Nergal", "Pazuzu", "Marduk", "Ishtar", "Ereshkigal",

            // ── Deities — Celtic ───────────────────────────────────────────────
            "Morrigan", "Cernunnos", "Arawn",

            // ── Deities — Slavic ───────────────────────────────────────────────
            // Chernobog (darkness), Perun (thunder), Veles (underworld) —
            // all read as natural dark-fantasy epithets.
            "Perun", "Veles", "Chernobog", "Marzanna", "Svarog",

            // ── Deities — Mesoamerican ────────────────────────────────────────
            "Xibalban", "Tlaloc", "Kukulkan", "Quetzal",

            // ── Cryptids ──────────────────────────────────────────────────────
            // Well-known and obscure — read naturally as dark/fantasy epithets.
            "Wendigo", "Mothman", "Skinwalker", "Batsquatch", "Bunyip",
            "Hodag", "Snallygaster", "Flatwoods", "Fresno", "Enfield",
            "Loveland", "Sheepsquatch", "Ahool",

            // ── Demonology — Abrahamic / Ars Goetia ──────────────────────────
            // Demon names from the Ars Goetia, Paradise Lost, and Abrahamic tradition.
            // All read as natural dark-fantasy epithets; several are already in wider culture.
            "Abaddon", "Asmodeus", "Belial", "Mammon", "Moloch",
            "Baphomet", "Azazel", "Beelzebub", "Samael", "Malphas",

            // ── Lovecraftian ──────────────────────────────────────────────────
            // Cyclopean (Lovecraft's favourite adjective for ancient masonry — also a real
            // architectural term). Arkham, Dunwich, Innsmouth are the three cursed towns.
            // Azathoth is the blind idiot god at the centre of the universe.
            // Dagon is both a Lovecraft entity and a real Philistine/Canaanite grain deity.
            "Cyclopean", "Arkham", "Azathoth", "Dagon", "Dunwich", "Innsmouth",

            // ── Japanese mythology / folklore ─────────────────────────────────
            "Izanami", "Susanoo", "Tsukuyomi", "Raijin", "Fujin", "Ryujin",

            // ── Hindu mythology ───────────────────────────────────────────────
            // Kali (destruction), Ravana (demon king of Lanka), Indra (thunder/war),
            // Yama (lord of death), Durga (warrior goddess who slew Mahishasura).
            "Kali", "Ravana", "Indra", "Yama", "Durga",

            // ── Folklore creatures ────────────────────────────────────────────
            // Kelpie (Scottish shapeshifting water horse), Banshee (Irish death omen),
            // Nuckelavee (Orcadian flayed demon horse of the sea),
            // Strigoi (Romanian blood-drinking revenant), Tengu (Japanese mountain demon),
            // Kitsune (Japanese fox spirit), Oni (Japanese horned ogre),
            // Jorogumo (Japanese spider-woman), Gashadokuro (giant skeleton of the starved),
            // Baku (Japanese dream eater).
            "Kelpie", "Banshee", "Nuckelavee", "Strigoi",
            "Tengu", "Kitsune", "Oni", "Jorogumo", "Gashadokuro", "Baku",

            // ── Gaming — Minecraft / Terraria / Stardew Valley ────────────────
            // Only games where the world itself is a creative sandbox — the references
            // sit naturally alongside real folklore because the games draw from it.
            // Minecraft: Herobrine (creepypasta), Farlands (legendary world-edge glitch),
            //   Ghast (the wailing nether jellyfish), Warden (the blind Deep Dark guardian).
            // Terraria: Plantera (the jungle boss — reads as flora + terra),
            //   Crimson (the flesh biome — also just a colour).
            // Stardew Valley: Junimo (the forest spirits), Krobus (the shadow person
            //   who lives in the sewers), Stardrop (the fruit that expands your heart).
            "Herobrine", "Farlands", "Ghast", "Warden",
            "Plantera", "Crimson",
            "Junimo", "Krobus", "Stardrop"
        );
    }
}
