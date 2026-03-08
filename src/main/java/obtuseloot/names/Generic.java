package obtuseloot.names;

import java.util.Arrays;
import java.util.List;

/**
 * Default name entries for the generic category.
 * All names describe provenance, reputation, or vague physical character
 * rather than function, making them work across any item type.
 *
 * A meaningful portion of entries earn a double reading only in combination
 * with a suggestive prefix ("Hungry", "Arching", "Spent") or suffix
 */
final class Generic {
    private Generic() {}

    static List<String> get() {
        return Arrays.asList(
            // ── Provenance / travel ──────────────────────────────────────────
            // Items defined by where they've been rather than what they do.
            "Saltborne", "The Castoff", "Drifter's Find", "The Unclaimed", "Ashkeep",
            "The Recovered", "Remnant", "Gravekeep", "Shorecast", "The Forgotten",
            "Duskkeep", "The Abandoned", "The Mislaid", "The Stranded", "Tidewrack",
            "The Unreturned", "The Passing Thing", "The Discarded", "The Nameless Thing",
            "Riftborn", "Bleakkeep", "The Lingering", "The Misplaced", "Voidkeep",
            "The Lasthand", "The Passed On", "The Adrift", "The Turned Over",
            "The Carried Far", "The Left Behind", "The Unmarked", "The Survived",
            "The Cast Out", "The Enduring", "The Long Kept", "The Twice Found",
            "The Unfinished", "The Nearly Lost", "The Kept Thing", "The Once Lost",
            "The Still Kept", "The Outlasting", "The Passed Through", "The Barely Kept",
            "Ironkeep", "Coldkeep", "Darkborn", "Greykeep", "Nightborn", "Ashborn",
            "Wanderer's Remnant", "Bleached Find", "Wrackline Find", "Boneyard Find",

            // ── Reputation / rumour ──────────────────────────────────────────
            // Names earned through whisper rather than deed.
            "The Much-Sought", "The Long-Rumoured", "The Whispered Of", "The Storied",
            "The Fabled", "The Oft-Spoken", "The Seldom-Found", "The Never-Kept",
            "The Quietly-Kept", "The Loudly-Claimed", "The Reluctantly-Kept",
            "The Often-Returned", "The Willingly-Passed", "The Seldom-Touched",
            "The Much-Discussed", "The Disputed", "The Contested", "The Alleged",
            "The Presumed", "The Rumoured", "The Supposed", "The Suspected",
            "The Ill-Favoured", "The Well-Regarded", "The Poorly-Understood",
            "The Frequently Misused", "The Rarely Appreciated",

            // ── Named by what it cost ────────────────────────────────────────
            // Bitter prizes, heavy gifts — items defined by what carrying them meant.
            "The Bitter Prize", "The Dearly Bought", "The Hard-Won", "The Cheaply Had",
            "The Freely Given", "The Ill-Gotten", "The Unnamed Gift", "The Sweet Burden",
            "The Heavy Gift", "The Light Curse", "The Warm Poison", "The Cold Remedy",
            "The Known Danger", "The Unknown Comfort", "The Strange Mercy",
            "The Familiar Wound", "The Old Sorrow", "The New Pain", "The First Joy",
            "The Last Comfort", "The Long Memory", "The Short Reckoning", "The Deep Debt",
            "The Steep Price", "The Unpaid Debt", "The Settled Score",

            // ── Simple evocative ─────────────────────────────────────────────
            "The Found One", "The Lost One", "The Kept One", "The Given One",
            "The Taken One", "The Made One", "The Broken One", "The Fixed One",
            "The Old One", "The New One", "The First One", "The Last One",
            "The Only One", "The Other One", "The Strange One", "The Familiar One",
            "The Known One", "The Unknown One", "The Seen One", "The Unseen One",
            "The Expected One", "The Unexpected One", "The Common One", "The Uncommon One",
            "The Hidden One", "The Open One", "The Full One", "The Empty One",
            "The Heavy One", "The Light One", "The Large One", "The Petite One",
            "The Wide One", "The Narrow One", "The Tall One", "The Brief One",
            "The Straight One", "The Bent One", "The Flat One", "The Round One",

            // ── Emotional / relational ───────────────────────────────────────
            "The Cherished", "The Coveted", "The Desired", "The Yearned For", "The Wanted",
            "The Sought", "The Craved", "The Longed For", "The Needed", "The Missed",
            "The Treasured", "The Prized", "The Adored", "The Beloved",
            "The Worshipped", "The Revered", "The Celebrated", "The Famed",
            "The Rarely Released", "The Tightly Held", "The Often Returned",
            "The Willingly Given", "The Reluctantly Shared", "The Eagerly Sought",

            // ── Sensation ────────────────────────────────────────────────────
            // Named for what carrying it feels like rather than what it does.
            // Each entry works as a mood or state; the second reading emerges
            // only from prefix/suffix combination.
            "The Lingering Ache", "The Persistent Need", "The Slow Burn", "The Deep Itch",
            "The Quiet Hunger", "The Low Pulse", "The Rising Need", "The Building Tension",
            "The Unmet Want", "The Prolonged Ache", "The Unreleased Tension",
            "The Mounting Pressure", "The Unresolved Want", "The Unsatisfied Need",
            "The Restrained Find", "The Pent-Up Keep", "The Held-Back Remnant",
            "The Yielding Find", "The Resistant Keep", "The Compliant Find",
            "The Eager Keep", "The Reluctant Find", "The Willing Keep",
            "The Responsive One", "The Hesitant Keep",

            // ── Physical — subtle double reading ─────────────────────────────
            // Ordinary descriptors in isolation. The second reading requires
            // a suggestive prefix or suffix to land — it is never explicit alone.
            "The Long One", "The Short One", "The Thick One", "The Firm Find",
            "The Hard Keep", "The Taut Find", "The Deep Find", "The Long Remnant",
            "The Veined One", "The Knotted One", "The Smooth One", "The Rough One",
            "The Supple One", "The Yielding One", "The Slick One", "The Glistening One",
            "The Heady One", "The Spent One", "The Flushed One", "The Arching One",
            "The Parting One", "The Bare One", "The Close One", "The Eager Find",
            "The Hungry Find", "The Wanting Keep", "The Devoted Find", "The Longing Keep",
            "The Searching One", "The Reaching One", "The Pressing One", "The Straining One",
            "The Pliant One", "The Fluid One", "The Coiling One", "The Unfurling One",
            "The Overcome One", "The Surrendered Find", "The Panting Keep", "The Roused One",
            "The Taut One", "The Supple Keep", "The Pliant Find"
        );
    }
}
