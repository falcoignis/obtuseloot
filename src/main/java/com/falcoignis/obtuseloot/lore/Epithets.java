package com.falcoignis.obtuseloot.lore;

import java.util.Arrays;
import java.util.List;

/**
 * Default lore entries for epithets — curated entries.
 * Server owners can replace the generated .yml file freely; this class
 * is only used when the file does not yet exist.
 *
 * Register: lowercase clipped archival. A final curator's stamp or verdict.
 * This is the FOURTH and final segment of the lore line:
 *   [Observation] — [History] — [Secret] — [Epithet].
 * Each entry must read as a conclusive note after any preceding three segments.
 */
final class Epithets {
    private Epithets() {}

    static List<String> get() {
        return Arrays.asList(

        // ── Archival / curatorial stamps ──────────────────────────────────────
        "provenance disputed",
        "origin unknown",
        "handle accordingly",
        "not for trade",
        "currently unclaimed",
        "do not return to sender",
        "believed destroyed",
        "previously returned",
        "condition: see notes",
        "not the original",
        "see also: the other one",
        "last catalogued in the deep",
        "filed under: unresolved",
        "the collector is still looking",
        "acquired under unusual circumstances",
        "removal not recommended",
        "documentation incomplete",
        "third known example",
        "the only known example",
        "the second known example was worse",
        "appraised and re-sealed",
        "deaccessioned without explanation",
        "not included in the final inventory",
        "the estate did not disclose this one",
        "loan: extended indefinitely",
        "ownership: contested",
        "the auction was stopped early",

        // ── Verdicts and conclusions ───────────────────────────────────────────
        "use at your discretion",
        "do not bring to court",
        "the verdict was not guilty",
        "the verdict was not recorded",
        "no charges were filed",
        "the inquest was inconclusive",
        "the committee could not agree",
        "the matter was quietly dropped",
        "the record has been corrected",
        "the correction was also incorrect",
        "further research discouraged",
        "subsequent testing suspended",
        "the study was never published",
        "the publisher declined",
        "peer review: abandoned",

        // ── Mythology — Greek / Roman ─────────────────────────────────────────
        // Charon is the ferryman of the dead who demands payment.
        // The Moirai (Fates) spin, measure, and cut the thread of life.
        // Argus Panoptes had a hundred eyes and never slept.
        // Tiresias was the blind prophet who saw everything.
        "charon would not accept it as toll",
        "the thread is still attached",
        "the three sisters could not agree on it",
        "the blind man said he had been expecting it",
        "the eye that never closes has noted it",

        // ── Mythology — Norse ─────────────────────────────────────────────────
        // Huginn and Muninn are Odin's ravens, Thought and Memory.
        // Gleipnir is the magical ribbon that bound Fenrir.
        // Nidavellir is the realm of the dwarven smiths.
        "the ravens have seen it",
        "the dwarves made better and know it",
        "bound, but not by gleipnir",

        // ── Mythology — various ───────────────────────────────────────────────
        // Anansi is the West African spider trickster who owns all stories.
        // Charon (see above). Hecate governs crossroads, magic, and liminal spaces.
        // The Jasconius is the whale-island of medieval Irish legend; sailors landed
        // on its back mistaking it for shore.
        "the spider had it first",
        "the crossroads remembers the transaction",
        "the whale was not consulted",
        "do not eat the fruit",

        // ── Cryptids ──────────────────────────────────────────────────────────
        // Point Pleasant, WV: site of Mothman sightings, 1966-67.
        // The Flatwoods Monster appeared in WV the same year — and was formally
        // known as the "Braxton County Monster".
        // The Lake was quieter afterward: deliberate non-attribution; applies to
        // Loch Ness, Lake Champlain, Lake Van, or any number of lake-monster sites.
        "observed near point pleasant",
        "the flatwoods green was nearby",
        "the lake was quieter afterward",
        "recovered from the exclusion zone",
        "associated with the missing forty-seven",

        // ── Lovecraftian / cosmic ─────────────────────────────────────────────
        // The colour out of space is an entity in Lovecraft that has no
        // earthly colour — "a colour out of space" is the actual story title.
        // R'lyeh is the sunken city where Cthulhu waits dreaming.
        // Non-Euclidean: Lovecraft's term for geometry that shouldn't work.
        "the colour is not from here",
        "last catalogued in r'lyeh",
        "the geometry is non-euclidean",
        "do not look at it in dreams",

        // ── Gaming — subtle ───────────────────────────────────────────────────
        // "the flame is fading" — Dark Souls: the First Flame, the central metaphor
        //   of the series. Also just a fact about fire.
        // "hollowing detected" — Dark Souls: going hollow is the loss of humanity.
        //   Also a real metallurgical defect.
        // "the fog wall held here" — Dark Souls fog gates before bosses.
        //   Also a meteorological observation.
        // "obtained from a dying merchant" — universal RPG trope. Stands alone.
        // "it levels with you" — Oblivion's infamous enemy-scaling system.
        //   Also just a description of a surface.
        // "soulbound" — WoW's item binding system, and our own soul mechanic.
        "the flame is fading",
        "hollowing detected",
        "the fog wall held here",
        "obtained from a dying merchant",
        "soulbound: do not discard",
        "dropped by the last of its kind",
        "the chest was unguarded",

        // ── Innuendo — plausibly deniable ─────────────────────────────────────
        // All have straightforward archival or tool-related readings.
        // The second reading emerges only in combination with the right prior segments.
        "returned with more wear than expected",
        "used more than the record suggests",
        "required two hands",
        "the grip was described as adequate",
        "size disputed by all parties",
        "the fit was considered acceptable",
        "returned the following morning",
        "duration of use: unrecorded",
        "the motion was tabled",
        "all parties satisfied",
        // ── Archival / acquisition stamps (continued) ─────────────────────────
        "currently in transit",
        "arrived ahead of its paperwork",
        "the paperwork has since been lost",
        "acquired by bequest, conditions undisclosed",
        "held in escrow",
        "the escrow has not cleared",
        "gifted with restrictions",
        "the restrictions were ignored",
        "currently on extended loan",
        "the lending institution has dissolved",
        "subject to prior claim",
        "the prior claim was withdrawn",
        "the withdrawal was not voluntary",
        "authentication pending",
        "authentication: inconclusive",
        "authentication: do not attempt again",
        "repatriation requested",
        "repatriation refused on technical grounds",
        "technical grounds remain disputed",
        "condition: stable",
        "condition: deteriorating",
        "condition: inexplicably improving",
        "condition: unchanged for two centuries",
        "last examined: before living memory",
        "examined once, results sealed",
        "not to be examined without two witnesses",
        "the second witness declined to sign",
        "inventory number does not match any known system",
        "catalogued under a name that belongs to something else",
        "the something else was also unusual",
        "originally part of a larger collection",
        "the rest of the collection is unaccounted for",
        "stored separately from its documentation",
        "the documentation refers to a different object",
        "both objects are in this collection",
        "neither has been fully explained",
        "field notes indicate irregular behavior",
        "the field notes were written in the margin of an unrelated report",
        "the margin was not large enough",
        "additional notes on reverse",
        "reverse is blank",

        // ── Legal and judicial ─────────────────────────────────────────────────
        "case closed: insufficient evidence",
        "case closed: all witnesses unavailable",
        "case still open",
        "the judge recused themselves",
        "the second judge also recused",
        "no verdict was returned",
        "the verdict was appealed and upheld",
        "the appeal was not heard",
        "possession: nine-tenths",
        "exhibit b",
        "not admitted into evidence",
        "seized under warrant",
        "warrant later found to be invalid",
        "confiscated pending investigation",
        "investigation ongoing",
        "investigation concluded without findings",
        "filed under: anomalous",
        "filed under: miscellaneous",
        "filed under: do not file",
        "statute of limitations: unclear",
        "jurisdiction: contested",
        "no applicable jurisdiction found",
        "the claimant did not appear",
        "the defendant also did not appear",
        "the bailiff's report was unusual",
        "ruled inadmissible",
        "admissibility: see footnote",
        "the footnote was stricken",

        // ── Medical and clinical ───────────────────────────────────────────────
        "contraindicated for extended contact",
        "contact: limit to sixty minutes",
        "the sixty-minute limit is approximate",
        "side effects: see attached",
        "the attachment has been separated",
        "no known antidote",
        "the antidote exists but is worse",
        "do not expose to the immunocompromised",
        "do not expose to those who are grieving",
        "do not expose to those who are content",
        "effects may vary",
        "effects have varied considerably",
        "four of the subjects reported improvement",
        "the fifth subject did not report anything",
        "the study was not peer reviewed",
        "the study was peer reviewed and the peers were alarmed",
        "prognosis: unclear",
        "prognosis: see separate file",
        "the separate file is not included here",
        "treatment: not indicated",
        "not recommended for those with prior exposure",
        "prior exposure cannot be confirmed or denied",
        "diagnosis: pending further observation",
        "further observation has been suspended",
        "the physician who recommended suspension did not elaborate",

        // ── Military and tactical ──────────────────────────────────────────────
        "classified: level not specified",
        "need to know: you do not",
        "eyes only",
        "destroy after reading",
        "the reading has occurred",
        "do not deploy in populated areas",
        "deployment in populated areas has occurred",
        "operational status: unknown",
        "field-tested under unusual conditions",
        "the conditions are not replicated in this environment",
        "decommissioned",
        "the decommissioning was symbolic",
        "not included in the official armory",
        "the unofficial armory is better stocked",
        "handle as unexploded ordnance",
        "the ordnance is not the concern",
        "cleared for restricted use",
        "the restriction has been loosened twice",
        "chain of custody: broken",
        "chain of custody: deliberately broken",
        "recovered from a theater that was officially inactive",

        // ── Diplomatic and political ───────────────────────────────────────────
        "declared neutral by parties who do not agree on anything else",
        "the neutrality is formal and does not reflect reality",
        "not subject to treaty",
        "not covered by existing agreements",
        "no existing agreement applies",
        "status: in negotiation",
        "negotiations stalled on the third day",
        "negotiations resumed under different parties",
        "the different parties were also unable to agree",
        "mentioned in a communique that was later denied",
        "the denial was not convincing",
        "redacted from the official record",
        "present in the unofficial record",
        "the unofficial record is more detailed",
        "both parties deny involvement",
        "one party is more convincing than the other",
        "transferred under diplomatic cover",
        "the cover was not examined",
        "covered by sovereign immunity, technically",
        "the technically is doing considerable work",
        "held by a power that no longer recognizes itself",

        // ── Religious and ritual ───────────────────────────────────────────────
        "blessed by three traditions, none of which agree on what it is",
        "the blessing was contested",
        "the blessing may have made it worse",
        "cursed, then uncursed, then blessed, status uncertain",
        "excommunicated from two separate institutions",
        "both institutions have since dissolved",
        "left as an offering and not accepted",
        "the deity is believed to have declined politely",
        "returned from the altar without explanation",
        "the altar was undamaged",
        "the altar was not undamaged",
        "considered sacred by a tradition that does not name it",
        "considered profane by the same tradition",
        "the contradiction is theological",
        "the theologians have been arguing for six generations",
        "do not bring to a consecrated space",
        "it has been in several consecrated spaces",
        "the spaces were reconsecrated afterward",
        "one was not",
        "reliquary-grade containment recommended",
        "the reliquary is unavailable",
        "last anointed by someone who did not know what they were anointing",
        "the anointing appears to have worked",

        // ── Scientific and research ────────────────────────────────────────────
        "results not yet replicated",
        "results replicated once, then not again",
        "the second experiment did not match the first in any respect",
        "peer review: pending",
        "peer review: completed, findings embargoed",
        "the embargo has not been lifted",
        "sample size: insufficient",
        "sample size: one",
        "the sample was not returned after testing",
        "anomalous readings: documented",
        "anomalous readings: consistent",
        "consistency makes it more anomalous",
        "control group showed similar results, which was unexpected",
        "the unexpected results have not been published",
        "hypothesis: unconfirmed",
        "hypothesis: confirmed in ways that raised further questions",
        "the further questions are more concerning than the original hypothesis",
        "methodology: unconventional",
        "methodology: disputed by one reviewer",
        "methodology: disputed by all reviewers",
        "instrument error cannot be fully excluded",
        "the instruments are new",
        "data withheld pending further analysis",
        "further analysis has been indefinitely postponed",

        // ── Bureaucratic / administrative ──────────────────────────────────────
        "form 7c not on file",
        "the correct form has not been determined",
        "requires three signatures",
        "only two signatures have been obtained",
        "the third signatory is unavailable",
        "the third signatory has been unavailable for eleven years",
        "pending transfer",
        "the transfer was approved in a different fiscal year",
        "see attached memo",
        "memo not attached",
        "no memo was written",
        "this item should not be in this department",
        "the correct department is not accepting transfers",
        "the department that should handle this no longer exists",
        "see protocol b",
        "protocol b refers to protocol c",
        "protocol c was superseded",
        "no superseding protocol covers this",
        "flagged for review in the next cycle",
        "the cycle has passed",
        "the review was tabled",
        "the table was cleared without action",
        "processing time: indeterminate",
        "please allow additional time",
        "additional time has been allowed",
        "no update is expected",

        // ── Mythology — brief stamps ───────────────────────────────────────────
        // Sisyphean: the task that never ends (Sisyphus, condemned to roll the boulder).
        // Pyrrhic: a victory that costs too much (Pyrrhus of Epirus, "one more such victory").
        // The ferryman: Charon — already referenced in full entries above, abbreviated here.
        // The weaver: Arachne / Penelope / the Moirai — weaving as fate.
        // The labyrinth: Daedalus / Minos — containment that also entraps the maker.
        // Promethean: Prometheus; something stolen from those above for those below.
        // The anchor of argo: the Argo's anchor was made from a millstone from Cyzicus.
        // The apple: the apple of discord (Eris), or the apple of knowledge.
        "sisyphean ownership confirmed",
        "a pyrrhic acquisition by all accounts",
        "the ferryman would not say where it had been",
        "the weaver made it and denied it afterward",
        "the labyrinth was built to hold something like this",
        "promethean provenance suspected",
        "do not offer it to anyone named paris",
        "the apple has been accounted for separately",
        "not included in the ship's manifest",
        "the knot has been cut, not untied",

        // ── Cryptid and paranormal — brief stamps ─────────────────────────────
        // Hessdalen lights: persistent unexplained lights in a Norwegian valley since 1930s.
        // The Dyatlov Pass incident: nine hikers, 1959, unknown cause of death.
        // The numbers station: shortwave radio broadcasts of numbers to unknown recipients.
        // The Taos Hum: a persistent low-frequency hum heard by a minority in Taos, NM.
        // Skinwalker Ranch: Utah property associated with decades of paranormal reports.
        "associated with the hessdalen phenomenon",
        "recovered near the pass, winter, investigators unavailable",
        "the numbers station broadcast its coordinates twice",
        "the hum is louder near it",
        "the ranch does not discuss it publicly",
        "removed from the grid at the owner's request",
        "the owner's request was not made in writing",
        "not on any official registry of unusual items",
        "on at least two unofficial ones",
        "the unofficial registries do not agree on what it is",

        // ── Literary and cultural references ──────────────────────────────────
        // Voynich: the Voynich manuscript — a 15th-century illustrated codex in an
        //   undeciphered script. Not a weapon; a book. But the stamp transfers.
        // The Amber Room: the 8th wonder of the world, looted by the Nazis, never recovered.
        // The Antikythera mechanism: the ancient Greek astronomical computer.
        // Stradivari: instruments made by Antonio Stradivari, whose formula has never
        //   been fully replicated.
        // The lost chord: the 1877 Arthur Sullivan song about a chord accidentally struck
        //   and never found again. Also a real musical phenomenon.
        // The voynich parallel: the script matches nothing.
        // Ozymandias: the shelley poem; all great works eventually become ruins.
        // The dagger of brutus: not confirmed to exist, but loaded as a stamp.
        "script matches nothing in the voynich",
        "origin: possibly antikythera-adjacent",
        "the amber room does not account for it",
        "formula unknown, results consistent",
        "the chord was struck once and not recovered",
        "two legs of stone remain",
        "attributed to the craftsman — the craftsman is disputed",
        "not in any chronicle",
        "the chronicle that should contain it has a gap",
        "the gap is the right size",

        // ── Gaming — brief stamps ──────────────────────────────────────────────
        // These all stand alone as legitimate administrative stamps.
        // "unique: do not duplicate" — the Unique item type in many RPGs.
        // "drop rate: classified" — loot table secrecy.
        // "cannot be repaired" — the unbreakable-but-deteriorating item trope.
        // "set bonus: incomplete" — the set-armor trope; wearing all pieces grants a bonus.
        // "the vendor has no further stock" — sold-out vendor; also a historical reality.
        // "fast travel not available here" — Dark Souls pre-Lordvessel; also just true.
        // "the boss was not supposed to drop this" — data-mined loot table anomalies.
        // "noclip required to reach its location" — out-of-bounds item placement.
        // "this item was patched in version unknown" — unannounced content.
        "unique: do not duplicate",
        "drop rate: classified",
        "cannot be repaired by conventional means",
        "set bonus: incomplete",
        "the vendor has no further stock",
        "fast travel not available from this location",
        "the boss was not supposed to drop this",
        "patched in an unannounced update",
        "quest item: quest unknown",
        "the quest has been removed",
        "lore: see separate entry",
        "the separate entry was not written",
        "requires key item not in this inventory",
        "the key item is in a different playthrough",
        "binding on equip",
        "binding on pickup was too late",
        "level requirement: unclear",
        "the requirement was not enforced",
        "not intended for this area",
        "the intended area is inaccessible",
        "two-handed",
        "one-handed by those who know how",
        "obtained before the tutorial ends",

        // ── SCP / institutional horror — stamps ────────────────────────────────
        "object class: see addendum",
        "the addendum is not in this packet",
        "containment procedures: revised",
        "revised procedures also failed",
        "incident report: filed",
        "incident report: sealed",
        "incident report: the incident report is also anomalous",
        "d-class exposure: not recommended",
        "d-class exposure: happened anyway",
        "site director informed",
        "site director also informed at the previous site",
        "cognitohazard: mild",
        "cognitohazard: disputed",
        "amnestics: administered",
        "amnestics: partially effective",
        "the partially is the problem",
        "memetic properties: under review",
        "under review is a polite way to describe the situation",
        "keter protocols not yet authorized",
        "authorization is pending escalation",
        "escalation has been escalating for some time",

        // ── Innuendo — plausibly deniable stamps ──────────────────────────────
        // All have legitimate primary readings as archival or tool-related stamps.
        "handle with both hands",
        "not for solo use",
        "do not share without consent",
        "shared successfully",
        "performance: satisfactory",
        "performance: exceeded expectations",
        "performance: exceeded the expectations of all parties",
        "duration: longer than anticipated",
        "duration: shorter than hoped",
        "insertion recommended before use",
        "the manual describes a two-person operation",
        "the second person was not documented",
        "recommended grip: firm",
        "grip: adjusted mid-operation",
        "repeated use advised for best results",
        "rated for heavy use",
        "rated for daily use",
        "the daily rating has been tested",
        "well-worn in the relevant areas",
        "the relevant areas are polished from contact",
        "moisture-resistant",
        "moisture: encountered",
        "extended session noted in the log",
        "the log was not included in this report",
        "both parties agreed to omit the details",
        "the omission is standard practice in these cases",
        "returned wetter than it was taken",
        "the returning party declined to elaborate",
        "maintenance required after extended use",
        "maintenance was performed without a technician present",
        "vibration at operating frequency: notable",
        "noted with appreciation by the inspector",
        "the inspection was not brief",
        "do not operate near open flames",
        "the open flame was not the issue",
        // ── Philosophical and existential stamps ──────────────────────────
        "existence: confirmed, under protest",
        "existence: debated in three traditions",
        "the three traditions each have a different answer",
        "the answer may not be the point",
        "the question is more useful than the answer",
        "the answer changes depending on who is asking",
        "asking changes the answer",
        "observation alters the observed",
        "the observer has also been altered",
        "causality: not applicable in this case",
        "time of origin: ambiguous",
        "the ambiguity is not a flaw",
        "the paradox has been noted",
        "the paradox is load-bearing",
        "do not resolve the paradox",

        // ── Mythology — brief stamps ───────────────────────────────────────────
        // Cassandra: prophet whose accurate warnings were never believed.
        // Tantalus: eternal punishment of near-reach (Tartarus); also tantalum the element.
        // Schrödinger: superposition — both states simultaneously until observed.
        // The oracle at Delphi: technically correct, practically useless answers.
        // Occam: simplest explanation preferred; here the simplest was wrong.
        // Sisyphean: the task that never ends.
        // Pyrrhic: victory that costs too much to matter.
        "cassandra noted this and was not believed",
        "tantalized by proximity to an explanation",
        "state: superimposed",
        "the simplest explanation was wrong",
        "the oracle was consulted; the answer was technically correct",
        "the technically correct answer caused considerable trouble",
        "bound by laws older than the binding",
        "the laws do not require belief to function",
        "the contract was verbal and is still in force",
        "verbal contracts of this type do not expire",
        "sisyphean provenance confirmed",

        // ── Gaming — brief stamps ──────────────────────────────────────────────
        // All stand alone as legitimate administrative stamps.
        // "unique: do not duplicate" — the Unique item type in RPGs.
        // "resting has been tried" — Dark Souls bonfire rest mechanic.
        // "the merchant was there, which raised questions" — RE4 / DS merchant ubiquity.
        // "this item skips the dungeon" — sequence-breaking in Zelda / Metroid.
        // "softlocked one previous owner" — stuck in an unwinnable game state.
        // "binding on equip" — WoW item binding; also our own soul mechanic.
        "resting has been tried",
        "the merchant was there, which raised questions",
        "harder on a second playthrough",
        "obtained out of intended sequence",
        "this item skips the dungeon",
        "softlocked one previous owner",
        "the softlock was eventually escaped",
        "new game plus not recommended",
        // ── Final top-up to 500 ───────────────────────────────────────────
        "entered into the record under protest",
        "the protest was noted and dismissed",
        "last examined before living memory",
        // ── Top-up to 500 ────────────────────────────────────────────────
        "arrived before the request was made",
        "the request was made afterward to account for it",
        "not the item being sought",
        "the item being sought has not been found",
        "see also: the missing forty-seven",
        "the forty-seven is a separate matter",
        "cross-referenced to a file that does not exist",
        "the file not existing is itself documented",
        "reviewed and re-sealed",
        "re-sealed without review",
        "current location: known to two people",
        "one of those people is unavailable",
        "the other is not speaking",
        "physical description: insufficient",
        "physical description: inconsistent across observers",
        "requires additional classification",
        "additional classification is pending approval",
        "approval pending since the previous occupant of this office",
        "last signed out and not returned",
        "the sign-out log ends here",
        "not insured",
        "insurance was declined without explanation",
        "underwriters were not given a reason",
        "the reason would not have helped",
        "believed unique",
        "uniqueness: disputed by one party",
        "the disputing party has not produced their example",
        "their example may not be better",
        "acquired at considerable cost",
        "the cost was not financial",
        "value: incalculable",
        "value: zero, according to one assessment",
        "the assessment was not peer reviewed",
        "the assessor has since revised their position",
        "the revised position is also disputed",
        "status: present",
        "status: unaccounted",
        "status: present and unaccounted simultaneously",
        "this has been noted",
        "noting it did not resolve it",
        "chain of custody: intact on paper",
        "the paper is not intact",
        "the custody is not a chain",
        "returned to a place that no longer holds things",
        "the place was specifically designated for this",
        "the designation was informal",
        "informal designations of this kind are often the most reliable",
        "do not submerge",
        "it has been submerged",
        "do not heat above ambient",
        "ambient is disputed in this context",
        "do not combine with other items",
        "it has been combined",
        "the combination was reversible",
        "the reversibility was partial",
        "handle as per training",
        "training for this has not been conducted",
        "conduct training before handling",
        "training was conducted and the results were mixed",
        "the mixed results are better than previous results",
        "previous results are sealed",
        "cleared for general use by those who know what they are doing",
        "what they are doing is not general use",
        "disposition: pending",
        "disposition: unknown",
        "disposition: irrelevant to current circumstances",
        "returned with thanks",
        "returned without thanks",
        "returned without explanation and the explanation was not requested",
        "the explanation would have been interesting",
        "the explanation was offered privately and accepted privately",
        "the explanation exists",
        "the explanation is not available here",
        "it is available elsewhere but not easily"

    );
    }
}
