package com.gemini.obtuseloot.lore;

import java.util.Arrays;
import java.util.List;

/**
 * Default lore entries for secrets — curated entries.
 * Server owners can replace the generated .yml file freely; this class
 * is only used when the file does not yet exist.
 *
 * Register: lowercase cryptic present tense. A revelation about the item's nature.
 * This is the THIRD segment of the lore line:
 *   [Observation] — [History] — [Secret].
 * Each entry must be intelligible after any observation and history with no prior context.
 */
final class Secrets {
    private Secrets() {}

    static List<String> get() {
        return Arrays.asList(

        // ── Identity and naming ────────────────────────────────────────────────
        "whispers of a name it refuses to speak",
        "its true owner is still alive and searching",
        "the maker buried their name inside the material",
        "the runes spell a warning in reverse",
        "it was named at its creation and the name was later taken back",
        "it knows its own name, a sound no human mouth can make",
        "it was named by something that has since been unnamed",
        "a name is written inside it that cannot be seen without knowing it is there",
        "the name inside is the name of something that should not be named",
        "the name inside is the reason it works the way it does",
        "a name was removed from history and placed here instead",
        "it has been carried through a place where names do not hold",
        "it answers to a name no one living knows",

        // ── Purpose and intent ─────────────────────────────────────────────────
        "it was made to destroy something specific and has not yet found it",
        "it was made to destroy something specific and found it and failed",
        "it was made to destroy something specific, succeeded, and carries the regret",
        "it was designed to be lost and found in a pattern not yet complete",
        "its purpose has been completed and it does not know",
        "it was supposed to be unmade at the end of a specific era and was not",
        "someone chose not to unmake it, and that choice is still accumulating",
        "it was forged to be given away exactly once, and the moment is close",
        "it was made when the laws of making were different and some of those laws remain",
        "it was made at the same time as something terrible, and made to contain it",
        "the terrible thing it was made to contain ended before it was needed",
        "it was once at the center of something vast, still arranged around it",
        "it holds a position in something larger whose purpose has not yet been executed",

        // ── Memory and knowledge ───────────────────────────────────────────────
        "it carries the memory of its first strike, the memory is incorrect",
        "it remembers every hand that has held it and cannot forget",
        "it remembers its first wielder, who was not a person",
        "it contains a memory placed inside it intentionally by someone still alive",
        "it knows where something is buried that should remain buried",
        "it was present at the end of something, and the end was not the end",
        "it was present when a language died, the last word is still inside",
        "it carries a record of a compounding wrong accessible only by the right question",
        "it witnessed a transformation, what was transformed was not what was expected",
        "it was present at a decision that went wrong and holds a record of the compounding",
        "it witnessed something being unmade, and the residue is its particular quality",
        "it carries the last breath of something still slowly exhaling",

        // ── Supernatural attachment ────────────────────────────────────────────
        "a god touched it once and the mark is hidden",
        "a god touched it and the god no longer exists, this is partially why",
        "a god lost something near it and the lost thing attached itself",
        "it was promised to darkness, light accepted it instead, and darkness remembers",
        "a great thing rests upon it quietly and does not know it is resting",
        "something follows it at a distance, curious, persistent, and patient",
        "the watching began before it was made and has never interfered",
        "something was put into it and cannot get out, and wants out",
        "the containment will hold indefinitely, indefinitely ends eventually",
        "something lives in the hollow at its base and is waiting",
        "something inside it is older than the universe and is content here for now",
        "a living thing was once inside it and left a mark that is still functional",
        "a part of its maker remains in the material, the only good part",

        // ── Curses, oaths, and bindings ────────────────────────────────────────
        "it has been used in a ritual that cannot be undone",
        "the ritual left something inside it that has been there longer than the object",
        "a specific oath was sworn on it correctly and is still in force",
        "a vow made on it is still binding, the one who made it has forgotten",
        "forgetting does not dissolve a vow made this way",
        "a covenant was made through it between parties who never met, still unfolding",
        "it was blessed once without the blesser knowing, the only blessing that took",
        "a curse was applied and removed incompletely, the remainder has become normal",
        "a word of power was carved into it incorrectly, reversing the effect entirely",
        "a wrong has been absorbed into it and is looking for a way to become right",
        "something true was spoken over it, and true things spoken this way stay binding",
        "an oath sworn against it was dishonored, returned oaths change what they return to",
        "a threshold was crossed when it was first drawn, and the crossing was irreversible",

        // ── Twins, copies, division ────────────────────────────────────────────
        "it has a twin and the twin is worse",
        "it has a twin and the twin is better",
        "the twins were separated to prevent something that has not been prevented",
        "it was once whole and was divided, the division was the point",
        "it was made as a copy and surpassed the original",
        "the original still exists and is aware",
        "it was made by two makers simultaneously who disagreed on purpose the entire time",
        "neither maker ever saw the finished result",

        // ── Agency and consciousness ───────────────────────────────────────────
        "it chose its current bearer deliberately",
        "it did not choose its current bearer and is not satisfied",
        "it is never content with whoever holds it",
        "it was made to serve and resents it",
        "it was refused by something that should have accepted it, both bear the mark",
        "it once refused to be put down for three days and the reason was guessed correctly",
        "it has refused to cut three times in its history for reasons no one knows",
        "it once cut something it was not aimed at, the cut was not an accident",
        "two enchantments were attempted at the same time and are still arguing inside it",
        "a third voice in the argument has been there longer than the other two and is winning",
        "the third voice is its own",
        "it contains no magic, it performs as though it does by mechanics not understood",
        "it carries a promise made by the material itself before it was forged",
        "the terms of the promise are not recorded but are enforced regardless",

        // ── Doors, passages, thresholds ────────────────────────────────────────
        "a door opens for it that opens for nothing else, the door has not been found",
        "the door is in a place no one thinks to look",
        "the door is not a door",
        "it has a flaw that is also a door, the door opens inward",
        "the door opens inward, and something has already used it",
        "the shadow it casts is a shape from somewhere else, and that place is reachable",
        "a path was sealed through it as a precaution, what was sealed is finding another way",
        "an echo inside it from the last loud thing it heard is growing louder",
        "a direction is encoded in its balance leading somewhere specific, specific is not safe",
        "it is carrying something to a place it has not yet reached",
        "following what it points toward leads somewhere that knows you are coming",

        // ── Time, cycles, accumulation ─────────────────────────────────────────
        "it will outlast the civilization that forged it, it has already outlasted one",
        "it was here before and will be here after",
        "it has been destroyed completely twice and the destruction did not hold",
        "every third use changes something, cumulatively, invisibly",
        "it gets slightly heavier each year and the weight will eventually matter",
        "it has passed through the same city seven times and something changed each time",
        "a debt is recorded inside it that has not been repaid, the creditor is patient",
        "something borrowed from it was never returned, and the change was useful",
        "it has been sealed and unsealed by the same hand in different bodies",
        "the hand recognized it each time",
        "the first drop of blood it drew is still inside and does not decay",
        "the bloodline of the first blood drawn can feel it and does not know why",
        "it was made while something important was ending and the ending transferred inside",
        "it contains something meant to be temporary, temporary has lasted three hundred years",

        // ── Revelation and consequence ─────────────────────────────────────────
        "it was pointed at something it should not have been, the connection still functions",
        "a question was asked of it that it cannot answer, not answering costs it constantly",
        "the cost is paid by whoever holds it and is small but unending",
        "a specific question asked of it in the wrong way left the answer trapped inside",
        "it was forged in a moment of perfect clarity, the clarity is what makes it cut",
        "the way it cuts is not entirely physical",
        "it was touched by grief and retains the touch, grief held long enough becomes something else",
        "something true about whoever holds it is visible to the item",
        "it carries a question with no answer, the purpose of carrying it is the asking",
        "the understanding of what it is will come too late to change anything",
        "someone loved it once and something of the love was returned and remains",
        "approaching it changes you in ways too small to notice",
        "it was promised to something and is being carried toward that promise",
        "right is not the same as good, now is when you have it, that may be relevant",

        // ── Earns a second reading in lore context ────────────────────────────
        // All use the existing cryptic short-phrase register; the second reading
        // emerges only in combination with the right observation and history.
        "there is more inside than outside",
        "the inside accommodates more than expected",
        "it is deeper than it is long",
        "entry is easier than exit",
        "something found it open and did not close behind",
        "the opening admits more than light",
        "the passage through it leaves something of the traveler",
        "what it holds it holds completely",
        "nothing placed inside it has been fully recovered",
        "it yields to patience",
        "it has been forced and bears no mark of it",
        "the approach requires care to be rewarding",
        "it has made room that was not there before",
        "it opened once fully and has not fully closed since",
        "being inside it changes the thing that enters",
        "it remembers every approach",
        "the depth increases the longer it is held",
        "it holds what is given willingly",
        "what is given unwillingly does not stay",
        "the distinction matters to it",
        "it closes around what is given and does not let go easily",
        "has been inside and remembers it",
        "knows what it feels like from both sides",
        "the passage is smoother once you know the way",
        "the fit improves with familiarity",
        "the resistance is only temporary",
        "what was placed inside has not been removed",
        "the approach that worked is not the obvious one",
        "requires a particular angle to enter properly",
        "the depth has not been fully mapped",
        "yields to the right kind of pressure",
        "something warm lives in the deeper part",
        "once fully inside, removal is complicated",
        "the interior expands to accommodate",
        "most rewarding when used without hesitation",
        "the narrowest point is not at the entrance",
        "the interior temperature exceeds the exterior by a measurable degree",
        "expands when engaged with directly",
        "responds to slow and deliberate handling",
        "leaves traces not immediately obvious",
        "the warmth it generates is internal in origin",
        "the release, when it comes, is well-documented",
        "what it asks of the holder is not effort but patience",
        "most satisfying when approached from behind",
        "accommodates unusual angles without complaint",
        "knows the difference between handled and used",
        "the friction decreases with continued application",
        "something inside responds to external pressure",
        "once open, it does not close of its own accord",
        "the sensation at the tip differs from that at the base",
        "found to be more giving than it appears",
        // ── Additional standalone secrets ──────────────────────────────────────
        "it has been in the presence of death many times and has no opinion about it",
        "the absence of opinion is itself a form of opinion in this context",
        "it does not reflect certain wavelengths and no one has asked why",
        "the wavelengths it absorbs correspond to nothing in the visible spectrum",
        "something about it prevents certain kinds of memory from forming near it",
        "the memories that do not form are consistent across different people",
        "the consistent pattern has been noted and the note has been suppressed",
        "its weight in a sealed environment is different from its weight in open air",
        "the difference is small enough to be a rounding error and too consistent to be one",
        "it has been present at seven documented miracles and is not mentioned in any account",
        "the absence from the accounts is itself documented in a separate file",
        "the separate file references three more absences not yet investigated",
        "it carries an obligation that was placed on it by mutual agreement",
        "the agreement was between parties who did not share a language",
        "they understood each other anyway, and the obligation remains",
        "something it contains has been trying to communicate for several decades",
        "the communication attempts are methodical rather than desperate",
        "methodical suggests it believes it has time",
        "it has absorbed a significant quantity of ambient intention over the years",
        "the intention was mostly benign and has been metabolized",
        "the residue of the less benign intention is in the grain",
        "it contains the coordinates of a location that does not appear on any map",
        "the location appears on one map, which is held in a private collection",
        "the private collector has been contacted and has not replied",
        "there is a version of events in which it does not exist",
        "that version is increasingly plausible from certain angles",
        "the angles required to see it are not comfortable to maintain",
        "it has been the subject of a pilgrimage that was never made official",
        "the unofficial pilgrimage is better attended than most official ones",
        "it has no explanation for this and does not appear to find it flattering",
        "something in it is counting and will not stop",
        "the count began before it was made and will continue after it is gone",
        "what the count is for has been theorized and the theories are all wrong",
        "it has been touched by something that left no mark on the outside",
        "the inside mark is visible only when the object is between a specific light source and a wall",
        "the shadow it casts in that condition is not the right shape",
        "the right shape belongs to something that no longer exists above ground",
        "something was promised to it that has not yet been delivered",
        "the delivery has been delayed for reasons the debtor considers legitimate",
        "the item does not consider them legitimate",
        "two contradictory things are true about it simultaneously",
        "the contradiction has been examined by logicians and remains unresolved",
        "the logicians are reluctant to discuss their findings",
        "it was made to protect something, and the thing it protects is not what you have",
        "the thing it protects knows where it is and is satisfied with the current arrangement",
        "the arrangement is temporary from the perspective of the thing being protected",
        "temporary means something different at that timescale",
        "it was present at the making of a word that is no longer in use",
        "the word described a feeling that has also gone out of use",
        "the feeling still exists but cannot be expressed directly",
        "a name has been attached to it by something that has not been seen",
        "the name is not the one it was given and is not the one it knows",
        "it answers to all three in different conditions",
        "the conditions for the third name have not yet occurred",
        "when they occur something will be different afterward",
        "the difference will be small and permanent",
        // All read as straight cryptic observations; the mythological layer is secondary.
        // "the ferryman accepted it as toll once — the passenger did not arrive" —
        //   Charon takes his coin; the passenger simply never disembarked on the far shore.
        // "the three sisters argued over it and could not cut" — the Moirai; Atropos
        //   wields the shears but something stayed her hand.
        // "the eye that watches everything has watched this longer than most things" —
        //   Argus Panoptes, the hundred-eyed giant; also a general surveillance metaphor.
        // "the spider wove it into a story it has not yet finished telling" —
        //   Anansi the spider-trickster owns all stories in West African / Caribbean myth;
        //   also Arachne, weaver cursed to spin forever.
        // "the pale horse passed and it was not taken" — the fourth horseman of
        //   Revelation; the item survived a culling that took everything else.
        // "it was present when prometheus was released and it remembers what he said" —
        //   Prometheus bound to the rock, freed by Heracles; the first words of a freed
        //   god would be significant.
        // "the labyrinth recognizes it" — the Cretan labyrinth; built by Daedalus to
        //   contain what should not be contained.
        // "the world tree has a root that grows toward it" — Yggdrasil has three roots
        //   reaching into different realms; a fourth growing outward would be unprecedented.
        // "the norns recorded it differently from how it appears" — the Norns carve fate
        //   into the trunk of Yggdrasil; a discrepancy between the carving and reality
        //   is cosmologically significant.
        // "the hound at the gate turned its three heads when it passed" — Cerberus, who
        //   guards the entrance to the underworld; turning to watch something leave is unusual.
        "the ferryman accepted it as toll once, the passenger did not arrive",
        "the three sisters argued over it and the shears did not fall",
        "the eye that watches everything has watched this longer than most things",
        "the spider wove it into a story it has not yet finished telling",
        "the pale horse passed and it was not taken",
        "it was present when the fire-bringer was released and remembers what was said",
        "the labyrinth was built to contain something like it",
        "the world tree has a root that grows toward it",
        "the norns recorded it differently from how it appears",
        "the hound at the gate turned all three heads when it passed",

        // ── Cryptid and paranormal secrets ────────────────────────────────────
        // All written in the cryptic register; the paranormal reference is the detail.
        // "the lights that precede disappearances have been seen near it" — will-o'-wisps
        //   and UFO lights; both associated with people vanishing in folklore and report.
        // "the frequency it emits has only been recorded once, near point pleasant" —
        //   Mothman sightings in Point Pleasant, WV; also infrasound research.
        // "the bridge resonates differently when it is on the near side" — the Silver
        //   Bridge collapse (1967, Point Pleasant) followed Mothman sightings; bridges
        //   and Mothman are permanently linked in the cryptid literature.
        // "it was present at a mass sighting that was never officially explained" —
        //   Phoenix Lights (1997), or any of dozens of mass-sighting events.
        // "the geometric impression it left in the field has not grown over" —
        //   crop circle ground effects, which some researchers claim affect soil chemistry.
        // "something very large and very quiet has been nearby recently" — Bigfoot,
        //   Yeti, Yowie; described by witnesses as moving in total silence.
        // "the cattle in the adjacent field have not approached the fence since" —
        //   animal avoidance behaviour associated with chupacabra, skinwalker, and UFO
        //   encounter reports; also a genuine indicator of unusual ground disturbance.
        "the lights that precede disappearances have been seen near it",
        "the frequency it emits has only been recorded once, near point pleasant",
        "the bridge resonates differently when it is on the near side",
        "it was present at a mass sighting that was never officially explained",
        "the geometric impression it left has not grown over",
        "something very large and very quiet has been nearby recently",
        "the cattle in the adjacent field have not approached the fence since",

        // ── Lovecraftian / cosmic horror ──────────────────────────────────────
        // Subtle — most entries stand alone as standard cryptic lore.
        // "the colour it emits in certain conditions has no name in any language" —
        //   "The Colour Out of Space" (1927); the entity is literally an unnamed colour.
        // "it was made in a city whose architects did not understand angles" —
        //   R'lyeh's non-Euclidean geometry; "the geometry was all wrong" is the core image.
        // "the dreamer in the deep has not stirred, but its dreams have touched this" —
        //   Cthulhu lying dreaming in R'lyeh, whose dreams reach the minds of the sensitive.
        // "it is part of an arrangement whose centre is elsewhere" — the arrangement of
        //   stars in the Cthulhu mythos; Great Old Ones are bound by stellar alignments.
        // "there are things older than gods that know its name" — the Outer Gods
        //   (Azathoth, Yog-Sothoth) predate the gods of mythology entirely.
        "the colour it emits in certain conditions has no name in any language",
        "it was made in a city whose architects did not understand angles",
        "the dreamer in the deep has not stirred but its dreams have touched this",
        "it is part of an arrangement whose centre is very far from here",
        "there are things older than gods that recognize it",

        // ── Gaming — Dark Souls / FromSoftware ────────────────────────────────
        // All stand alone as legitimate artifact observations; the DS note is secondary.
        // "the flame inside it is fading and something waits to fill the absence" —
        //   the First Flame in Dark Souls; when it fades, the Age of Dark begins.
        // "it was dropped by the last of something that was already dying" —
        //   the soul-drop mechanic combined with DS's theme of dying civilisations.
        // "it carries the memory of a bonfire that has since gone cold" —
        //   bonfires in DS are beacons of warmth and safety; a cold one is significant.
        // "hollowing has been detected in its previous holders" — Going Hollow in DS
        //   means losing humanity to undeath; presented here as a material defect.
        // "it was found at the fog wall and has not passed back through" —
        //   fog gates in DS mark the threshold before boss encounters; permanent.
        "the flame inside it is fading and something waits to fill the absence",
        "it was dropped by the last of something that was already dying",
        "it carries the memory of a bonfire that has since gone cold",
        "hollowing has been detected in its previous holders",
        "it was found at the threshold and has not passed back through",

        // ── Gaming — broader references ───────────────────────────────────────
        // "it increases in capability the longer it is used — this has been verified" —
        //   level scaling; also an accurate description of learning curve with a tool.
        // "the skill it required was acquired through repetition and is now inseparable" —
        //   muscle memory / skill levelling; the item and the expertise are one.
        // "the final chest in the sequence was this — what came before was lesser" —
        //   treasure room progression in roguelikes; also a genuine archaeological idea.
        // "it was in the inventory of someone who should not have survived this long" —
        //   survival-game / RPG longevity; also a real historical observation.
        // "it was transferred on death and accumulated properties that transfer cannot explain" —
        //   soulbound item behavior in MMOs combined with inheritance mechanics.
        "it increases in capability the longer it is held, this has been verified",
        "the skill it requires has become inseparable from the person who holds it",
        "the final chest in the sequence contained this, what came before was lesser",
        "it was in the inventory of someone who should not have survived this long",
        "properties accumulated through transfer that transfer alone cannot explain",

        // ── SCP / liminal / institutional horror ──────────────────────────────
        // All written as clinical notes from an unspecified institution; read as
        // standard curatorial or regulatory language until the detail lands.
        "exposure exceeding four hours requires a formal incident report",
        "all personnel who handled it in the first study have since requested reassignment",
        "the reassignment requests were approved without question, which is unusual",
        "it has been classified, declassified, and reclassified three times in one decade",
        "current classification level is not available to the reader of this document",
        "the document this note was appended to has been redacted in full",
        "standard containment procedures apply, see supplementary file",
        "the supplementary file is not included in this packet",
        "staff are advised not to discuss it in rooms with soft furnishings",
        "the soft furnishings absorbed something the first time and were replaced",

        // ── New standalone secrets ─────────────────────────────────────────────
        "it has been tested against every known ward and passed through all of them",
        "the wards were not designed with this in mind",
        "it predates the language used to describe it by several centuries",
        "the description is still not accurate",
        "it has been cleaned and the cleaning changed the surface in a way that cannot be undone",
        "the change improved it",
        "something about its construction makes it impossible to fully appraise",
        "three separate appraisers gave three completely different verdicts",
        "the fourth was not asked because the first three were concerning",
        "it does not appear the same to everyone who looks at it simultaneously",
        "the discrepancy in simultaneous observation has been reproduced under controlled conditions",
        "the conditions required to reproduce it are not safe to recreate",
        "it is older than it looks and younger than it feels",
        "the age and the feeling point to different conclusions",
        "the conclusions are both correct",
        "it has been the subject of a bet that is still outstanding",
        "the bet involves whether it will outlast the civilization currently holding it",
        "current projections favor the item",
        "it is aware of the bet",
        "knowing has not changed its behavior",
        "it has no interest in resolving the question",

        // ── Earns a second reading in lore context ────────────────────────────
        "the internal volume exceeds what the exterior dimensions permit",
        "what enters it does not always emerge unchanged",
        "the opening is designed for a specific purpose and serves it well",
        "repeated use has smoothed the entrance considerably",
        "designed for insertion and optimized through iteration",
        "the capacity was expanded by a previous owner for undocumented reasons",
        "what was placed inside on the first occasion was not removed",
        "the second occasion was more deliberate",
        "requires lubrication under extended use conditions",
        "the depth has not been fully mapped by any of its holders",
        "most satisfying when engaged from the correct angle",
        "the vibration at its core increases with applied pressure",
        "yields to persistent and patient handling",
        "the interior accommodates more than external inspection suggests",
        "prolonged contact produces warmth not attributable to friction",
        "found most effective when neither party is in a hurry",
        "what is offered willingly is received completely",
        "the grip tightens around what it has accepted",
        "returns warmth to the giver in proportion to what was given",
        "once opened to its full extent, it does not fully close again",
        "it does not behave consistently across observers and the inconsistency is reproducible",
        "the reproducibility makes it more unsettling rather than less",
        "something about it makes instruments behave as though it is not there",
        "the instruments are otherwise reliable",
        "it has been counted, weighed, and measured and the results never agree twice",
        "the disagreement across measurements follows a pattern that is almost a word",
        "the word, if it is one, has not been identified",
        // ── New mythological secrets ──────────────────────────────────────
        // All read as artifact observations; mythological resonance is secondary.
        // "the river it crossed does not flow in one direction" — the Styx flows in a
        //   circle around the underworld; also rivers in quantum cosmology.
        // "it was present at the weighing and the feather was heavier" — the Egyptian
        //   judgment of the dead: the heart is weighed against Ma'at's feather. If the
        //   feather is heavier the scales are reversed — an impossible outcome.
        // "the forge that made it no longer burns but the heat remains" — Hephaestus's
        //   forge on Lemnos; also the perpetual sacred fires of Vesta, Brigid, Zoroaster.
        // "the thread attached to it leads somewhere it should not be possible to reach" —
        //   Ariadne's thread through the labyrinth; also the red string of fate (Japanese).
        // "it was used to close a door between two ages and the door did not hold" —
        //   the closing of an age is a recurring motif: the Kali Yuga, Ragnarök, the end
        //   of the Dreamtime in Australian Aboriginal cosmology.
        // "the three who guard the threshold turned it away" — the guardians of thresholds:
        //   Cerberus, the Shedu, the Gandharvas — animals/beings that screen who may pass.
        // "something that was sealed inside escaped and left the seal intact" — the djinn
        //   leaving the lamp; Pandora's box (hope remained); the Dybbuk leaving a vessel.
        // "a promise was made through it that cannot be kept by any living party" —
        //   geasa in Irish myth; the Valkyrie oath; promises sworn to dead gods.
        "the river it crossed does not flow in one direction",
        "it was present at the weighing and the feather was heavier",
        "the forge that made it no longer burns but the heat remains",
        "the thread attached to it leads somewhere that should not be reachable",
        "it was used to close a door between two ages and the door did not hold",
        "the three who guard the threshold turned it away",
        "something that was sealed inside escaped and left the seal intact",
        "a promise was made through it that cannot be kept by any living party",
        "the god who commissioned it refused delivery",
        "delivery was made anyway and the refusal is still in dispute",
        "it was the only thing in the world that could bind what it was used to bind",
        "the binding held for one age exactly",

        // ── Cosmic and deep time ───────────────────────────────────────────────
        "it has outlasted the language it was made to speak",
        "the language is gone but the speaking is not",
        "it carries the weight of something that ended before any witness",
        "the ending left a mark that looks like a beginning",
        "there are things in the material that predate the material",
        "the things in the material are patient in a way that suggests they have time",
        "it has been present at the death of at least one star by conservative estimate",
        "the estimate is contested by those who have examined it",
        "the examination took longer than planned because time moved differently near it",
        "the discrepancy in elapsed time was small and consistent",
        "it contains an absence where something used to be",
        "the something that left took the space it occupied with it",
        "the absence is functional and used regularly",
        "used regularly is an understatement",

        // ── Cryptid and paranormal secrets ────────────────────────────────────
        "the frequency at which it hums corresponds to no known emission source",
        "the emission source has been sought and not found",
        "not finding it has been tried by people who find things professionally",
        "animals within a certain radius orient toward it involuntarily",
        "the radius changes and the change is predictable three days in advance",
        "it has been photographed and the photograph shows something different from the object",
        "both the object and the photograph are in the same collection",
        "they are stored separately because they disagreed when stored together",
        "the nature of the disagreement was not recorded",
        "lights appear near it at intervals that do not correspond to any known pattern",
        "the intervals do correspond to a pattern that was noted and then buried",
        "buried is the word used in the report, which is unusual phrasing for a report",
        "it was found at a site that generated more reports than the reporting system could process",
        "the backlog of reports has not been cleared",

        // ── SCP / clinical register ────────────────────────────────────────────
        "those who hold it for more than an hour report a persistent sense of being understood",
        "the sense of being understood is not considered a positive effect by researchers",
        "it has been observed to move toward those who are afraid of it specifically",
        "moving toward is a loose description, drifting is more accurate",
        "the drifting was documented on three occasions and not documented on several more",
        "personnel assigned to it have a statistically unusual rate of requesting transfer",
        "the transfer requests are approved without comment, which is itself unusual",
        "it has been tested against every known countermeasure",
        "the countermeasures did not fail, they became irrelevant",
        "irrelevant is the word used across four separate test reports",
        "using the same word across independent reports was not coordinated",
        "the coordination that wasn't coordinated has been noted",
        "it passes through standard screening without triggering standard alerts",
        "the alerts were recalibrated after the third passage",
        "recalibration did not change the result",

        // ── Identity, consciousness, and agency ───────────────────────────────
        "it has a preference and acts on it when no one is watching",
        "the preference has been inferred from the pattern of when no one is watching",
        "something inside it knows its own age and is not comfortable with it",
        "the discomfort has effects on those who are sensitive to discomfort of this kind",
        "it has been offered destruction multiple times and declined each time",
        "declining was passive, it simply persisted",
        "persistence at this level requires something that looks like intention",
        "it does not appear to intend anything in particular",
        "not intending anything in particular is its own kind of intention",
        "it has been talked to by four different owners, each believing they were the first",
        "it did not correct any of them",
        "it held what was said and has not let it go",
        "what was said to it by the first owner would concern the current one",
        "the current one has not asked",

        // ── Consequence and accumulation ──────────────────────────────────────
        "every use changes the next use by an amount too small to track individually",
        "tracked over time the amount is not small",
        "the tracking was done by someone with more patience than was probably healthy",
        "their notes survive and are alarming in aggregate",
        "it absorbs context and context changes how it behaves",
        "it has absorbed enough context to have a perspective",
        "the perspective is approximately as old as recorded history",
        "recorded history is a narrow window and it knows what was outside the window",
        "it has been wrong about something important exactly once",
        "being wrong once at this scale is a notable record",
        "the record stands because no one has checked thoroughly enough to disprove it",
        "thorough checking is not recommended",

        // ── Endings and completion ─────────────────────────────────────────────
        "whatever it was made to do, it is almost done",
        "almost done has been the assessment for longer than expected",
        "the expectation was set by someone who did not understand the timescale",
        "understanding the timescale is not currently possible",
        "it will complete its purpose without announcement",
        "the completion will be recognizable only in retrospect",
        "retrospect may not be available to those present at the time",
        "those present at the time will have a different word for it",
        "the word they use will be adequate",
        "adequate is all that will be needed",

        // ── Additional innuendo secrets ────────────────────────────────────────
        "the opening widens with repeated use",
        "the widening is not structural damage",
        "what is placed inside settles into position without effort",
        "effort is not what this requires",
        "it has been used in ways not covered by its description",
        "the description was revised after the first documented occasion",
        "the revision was conservative",
        "conservative descriptions of this kind are common in formal records",
        "the informal record uses different language",
        "the informal language is more accurate",
        "it accommodates enthusiastically",
        "enthusiasm in this context is a technical observation",
        "the technical observation was volunteered without prompting",
        "the person who volunteered it was asked to elaborate and did",
        "the elaboration required a separate document",
        // ── Top-up to 500 ────────────────────────────────────────────────
        "a decision was encoded into it at the moment of making",
        "the decision has not yet been called upon",
        "when it is called upon the decision will be irreversible",
        "irreversible is not always worse than the alternative",
        "the alternative in this case is not available",
        "the answer it carries is correct for a question not yet asked",
        "the question will be asked and the asker will not like the answer",
        "not liking it does not change it",
        "it carries the name of something that renamed itself to escape the name",
        "the escape was unsuccessful because the name was held here",
        "the renaming and the holding happened simultaneously",
        "simultaneity at this level has implications",
        "the implications are not theoretical",
        "a memory lives inside it that does not belong to any known species",
        "the species it belongs to has no other known artifacts",
        "no other known artifacts does not mean no other artifacts",
        "it carries weight from things that are no longer heavy",
        "the no-longer-heavy things were very heavy when they were",
        "the residual weight is real and measurable",
        "measuring it changes it by an amount that is also measurable",
        "the chain of measurement does not terminate",
        "it has been sealed more times than it has been opened",
        "the balance will eventually correct",
        "correction at that scale is rarely tidy",
        "it knows something about the current holder that the holder does not",
        "the something is not alarming",
        "the not-alarming something has been waiting to be relevant",
        "its moment of relevance has arrived and passed three times",
        "the fourth time will be different",
        "different does not mean better",
        "better is not the metric it uses",
        "the metric it uses has no name in any living language",
        "a dead language has a word for it",
        "the word is in it somewhere",
        // ── Top-up to 500 ────────────────────────────────────────────────
        "whatever it is waiting for is not something that can be hurried",
        "hurrying it has been tried and the results were not recorded",
        "it has a preference for certain hours and this has been verified",
        "the verification required more patience than the verifier had anticipated",
        "something in it responds to being read aloud to, which is unusual",
        "the response is small and consistent and has no explanation",
        "it carries the shape of an intention that was never acted on",
        "the intention was sound and the inaction was a loss",
        "it was present at a birth that was also an ending",
        "the ending was not recognized as such until later",
        "it holds a record of every time it has been underestimated",
        "the record is long",
        "it has been cleaned of everything except what cannot be cleaned",
        "what cannot be cleaned is not dirt",
        "it knows the name of the place it was going before it was interrupted",
        "the interruption was permanent and the place is still waiting",
        "it carries a warmth that cannot be attributed to ambient temperature",
        "the warmth has been measured and is consistent across conditions",
        "it was made with an excess of care that is still present in the material",
        "the care was the maker's last act and it shows",
        "it belongs to a category of things that should not exist and does",
        "the category has three members and they do not interact well",
        "something about it causes certain people to lower their voices near it",
        "the people who lower their voices are not aware they are doing it",
        "it holds the last thought of whoever made it",
        "the thought was unfinished and is still unfinished",
        "it has been used as a weapon exactly once and regretted it",
        "the regret is structural rather than incidental",
        "it was at the center of something important that no one documented",
        "the lack of documentation was intentional on all sides",
        "it has been dreamed of by people who have never seen it",
        "the dreams are similar enough to have been compared",
        "it is heavier than it should be by an amount that cannot be attributed to material",
        "the extra weight is consistent regardless of altitude or time of day",
        "it was made by someone who understood what they were making",
        "understanding did not protect them",
        "it carries a permission that was not granted by anyone living",
        "the permission is still valid",
        "something about it suggests it has been used for a purpose not yet invented",
        "the suggestion is structural, not symbolic",
        "it was the last thing completed before everything changed",
        "the change is visible in everything made afterward except this",
        "it was the answer to a question that has not yet been asked"

    );
    }
}
