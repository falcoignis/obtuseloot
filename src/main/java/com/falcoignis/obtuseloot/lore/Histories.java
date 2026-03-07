package com.falcoignis.obtuseloot.lore;

import java.util.Arrays;
import java.util.List;

/**
 * Default lore entries for histories — curated entries.
 * Server owners can replace the generated .yml file freely; this class
 * is only used when the file does not yet exist.
 *
 * Register: lowercase archival prose. Past-tense provenance fragment.
 * This is the SECOND segment of the lore line:
 *   [Observation] — [History] — [Secret].
 * Each entry must be intelligible after any observation with no prior context.
 */
final class Histories {
    private Histories() {}

    static List<String> get() {
        return Arrays.asList(

        // ── Origin ────────────────────────────────────────────────────────────
        "forged in the age before names",
        "forged in the same fire that destroyed its maker",
        "forged in a fire that should not have been hot enough",
        "forged in secret and never declared",
        "forged under unusual astronomical conditions",
        "forged during an eclipse by insistence of the buyer",
        "forged while a specific song was played",
        "forged in total silence by ritual requirement",
        "forged in a location that no longer physically exists",
        "forged in a building that burned the next day",
        "forged in a place that is now underwater",
        "forged in a place that is now underground",
        "forged in a place that is now a ruin",
        "forged in a place that is now a garden",
        "completed by someone other than its maker",
        "passed to the next generation before it was finished",
        "commissioned by someone who refused to say why",
        "paid for in advance by someone who was already dead",
        "paid for by someone who vanished immediately after",
        "commissioned for a purpose that was achieved and then sealed away",
        "commissioned twice by the same person who forgot about the first",

        // ── Transfer and ownership ─────────────────────────────────────────────
        "carried by the first of its line",
        "passed from hand to hand without ceremony",
        "stolen from a king who never noticed",
        "given freely by someone who should have kept it",
        "won in a contest that was not fair",
        "won in a contest that was completely fair",
        "won in a contest no one else remembers",
        "taken in lieu of payment",
        "traded for something of lesser value",
        "traded for something of greater value",
        "traded once for a single meal said to be extraordinary",
        "sold at auction for less than expected",
        "sold at auction for more than anyone could explain and never collected",
        "sold below value by an estate that did not understand what it had",
        "taken from a fallen enemy with great respect",
        "taken from a fallen enemy with no ceremony",
        "inherited by someone who had never heard of it",
        "inherited with extensive instructions that were lost before being read",
        "left in a will to someone who had died first",
        "passed to the youngest heir by tradition, who refused it",
        "passed to the eldest heir under protest",
        "passed from teacher to student for eleven generations",
        "used by every member of a family for twelve generations",
        "carried by the same family for sixteen generations without interruption",
        "passed between rivals who respected each other",
        "gifted by a dying ruler to their fiercest enemy",
        "gifted on a Tuesday for no documented reason",
        "made as a gift and never given",
        "made as a threat and never used",
        "made as a prayer and used constantly",
        "made for a child who grew into it",
        "made for a ceremony that was cancelled",
        "made for no one in particular",
        "made for someone very particular who never claimed it",
        "bequeathed to an enemy as an apology",
        "bequeathed to a library with no instructions on use",

        // ── Loss, discovery, return ────────────────────────────────────────────
        "lost at the bottom of a lake for three centuries",
        "buried with its owner and later exhumed",
        "discovered inside a wall being demolished",
        "discovered inside a mountain being quarried",
        "discovered inside the stomach of a large fish",
        "discovered inside a chest that had no bottom",
        "discovered inside a room with no doors",
        "found washed up on a shore with no explanation",
        "found hanging in a tree at exact eye level",
        "found balanced on a rock with no visible means of support",
        "found standing upright in open ground after a storm",
        "found in exactly the place described in a dream",
        "found in a place it could not logically have been",
        "found buried with a note instructing whoever found it to give it away",
        "found buried with no body nearby",
        "described in a document that predates its creation by decades",
        "returned with no explanation",
        "returned with an explanation that made things worse",
        "returned with a note that resolved something older than the item itself",
        "declared lost and written off, then recovered before the ink dried",
        "stolen from the museum three times and returned twice voluntarily",
        "recovered from a fire that destroyed everything around it",
        "recovered from a sunken ship whose manifest listed it as ordinary cargo",
        "recovered from a sealed room where everything else had decayed",
        "recovered after a long search by someone who was not looking for it",
        "discovered in plain sight in a place everyone had already searched",

        // ── Use and purpose ────────────────────────────────────────────────────
        "used once and declared cursed",
        "used once and declared blessed",
        "never used and debated regardless",
        "used in exactly one battle and put away",
        "used in a hundred battles without rest",
        "used as a measuring tool, never as a weapon",
        "used as a weapon once and immediately retired to a museum",
        "used to sign a treaty by pressing it into wax",
        "used to break a treaty by cutting the document in half",
        "used to carve the first known map of a now-dead continent",
        "used to carve the last known map before everything changed",
        "used in a duel where both participants agreed to lose",
        "used in a duel where neither participant survived",
        "used in a duel that resolved nothing",
        "used in a duel that resolved everything between them",
        "used in a ritual that should have used something else",
        "used in a ritual that was improvised and worked better than intended",
        "used in an experiment that was never repeated, results unpublished",
        "used in an experiment whose success the experimenter considered a failure",
        "used in the founding of something that still stands",
        "used in the founding of something that fell immediately",
        "used to settle a dispute between two parties who were both wrong",
        "used in a practice bout that ended two careers on good terms",
        "used to split a document in a dispute, accidentally resolving it",
        "used in a ceremony that has not been performed since",
        "used in a ceremony that is performed every year in memory of it",
        "used once for a purpose that required complete secrecy on all sides",
        "used exactly as much as was needed and set aside immediately",
        "used once in a purely defensive capacity, to unexpected success",

        // ── Travel and campaigns ───────────────────────────────────────────────
        "carried by the first of its line into a country that no longer exists",
        "carried through a war that lasted forty years",
        "carried through a war that lasted a single afternoon",
        "carried across a desert by someone who did not survive",
        "carried across a sea by someone who barely did",
        "carried over a mountain by two people, one of whom returned in silence",
        "carried to the farthest known point and back",
        "carried to the farthest known point and not back",
        "carried by a messenger who never arrived",
        "carried by a messenger who arrived at exactly the wrong moment",
        "carried by a scout who disappeared ahead of an army and returned years later",
        "carried to a meeting that was never held",
        "carried to a meeting that changed the course of three nations",
        "carried to a battle that was called off and fought anyway two years later",
        "carried during a famine by a general who gave away the food",
        "carried during a plague by someone who caught nothing",
        "carried through a border that no longer exists",
        "carried during a voyage that charted new waters later named for someone else",
        "carried on a journey whose destination turned out to be the starting point",
        "carried by someone who was pretending to be someone else for eleven years",
        "carried by someone who feared it",
        "carried by someone who loved it",
        "carried by someone who was not supposed to have it",
        "carried by someone whose name was deliberately erased at their own request",
        "carried for good luck by someone with very bad luck, unrelated to the item",
        "carried by twins who shared ownership without a single recorded dispute",
        "carried in a coat pocket for three years without anyone knowing",

        // ── Institutional and archival ─────────────────────────────────────────
        "kept in a vault that was never successfully broken into",
        "kept behind glass for so long the case became more famous than the item",
        "kept in a family as a reminder of something painful long after the pain faded",
        "kept in a box that required two keys, one of which was lost in the first decade",
        "owned by an institution for three centuries until the institution dissolved",
        "held by a record-keeper who intentionally recorded nothing about it",
        "held for ransom and ransomed back for less than either party expected",
        "held hostage during a negotiation and returned in worse condition than promised",
        "stored in a warehouse of remarkable things that burned in an unremarkable fire",
        "left behind in a sealed room and found undamaged two centuries later",
        "left in trust with a monastery that burned, found undisturbed in the ashes",
        "left at a crossroads as a marker, retrieved too early, and used as a marker anyway",
        "sent as repayment for a debt two hundred years old, the recipient unconvinced",
        "passed through the hands of nine thieves, the ninth of whom returned it",
        "traded between cities as a symbol of diplomatic trust until the fifth exchange failed",
        "traded between merchants eighteen times in a single year",
        "passed between armies without a war, the informal transfer later made formal",
        "possessed by someone who had no use for it and kept it out of stubbornness",
        "displayed publicly for one year as a condition of inheritance",
        "held by a child for ten years, who handed it to the first person they trusted",
        "lent for a season and returned after three, the extra time never explained",
        "donated to a cause that failed, the failure considered noble by most accounts",

        // ── Circumstance and mystery ───────────────────────────────────────────
        "never used in battle by its intended owner",
        "returned with a note that was in the wrong language and followed exactly",
        "stolen from a shrine and replaced with a copy considered superior by observers",
        "never used and debated by scholars who could not agree on its purpose",
        "purchased under a false name that became more famous than the real one",
        "the smith who made it survived but refused to speak of it",
        "records of its forging survived in a dead language, later translated to nothing",
        "passed from smith to smith as a teaching piece until the additions were original",
        "gifted by a dying ruler to their fiercest enemy, the formality now a tradition",
        "found in a place it could not logically have been, the impossibility documented",
        "carried by someone who claimed rightful ownership, the claim never verified",
        "three people claimed rightful ownership simultaneously, the arbitrator kept it",
        "lent to someone who accomplished the intended purpose and did not return it",
        "used to resolve something that could not be discussed openly",
        "confiscated legally and then returned by illegal means",
        "confiscated illegally and then returned by legal means",

        // ── Earns a second reading in lore context ────────────────────────────
        // All use the existing lower-case archival register; the second reading
        // emerges only in combination with the right observation and secret.
        "kept beneath a pillow for seven years",
        "recovered from beneath the floorboards of a private room",
        "found tucked inside a bundle of personal clothing",
        "carried from one room to another after midnight",
        "passed hand to hand under a table at a formal occasion",
        "given in the dark and found in daylight",
        "offered without words and accepted without them",
        "pressed into someone's hands in a corridor and not discussed afterward",
        "returned the morning after being borrowed with no explanation offered",
        "kept in a bedside drawer under a separate key for reasons never recorded",
        "borrowed without asking and returned without speaking",
        "changed hands repeatedly over the course of one evening",
        "everyone who handled it that evening declined to be recorded",
        "returned with considerably more wear than it left with",
        "used quietly in a back room of an inn, the innkeeper saying nothing",
        "kept from a household member without explanation until it was found",
        "used for private purposes that were never entered into any ledger",
        "slipped under a door without announcement or return address",
        "discovered hidden behind a loose stone in a private room",
        "passed between two people who publicly denied knowing each other for eleven years",
        "brought out only after the guests had gone",
        "used to resolve something that could not be discussed openly, discretion held",
        "carried upstairs at an inn and not recovered until well after noon",
        "its employment that evening required all windows to remain shuttered",
        "given as thanks for something not spoken of directly",
        "returned with a note that read simply: as promised",
        "pressed between two persons as witness to a private agreement, honor held",
        "found tucked between the mattress and the wall of a rented room",
        "recovered from a private room the following morning by a third party",
        "left at a bedside by someone who did not stay until morning",
        "borrowed for a night and returned before anyone was awake",
        "changed hands three times in a single room over one night",
        "found under a bunk at a roadside inn with no explanation from either occupant",
        "used for a purpose that required lowered voices throughout",
        "returned with more warmth retained than when it was taken",
        "offered as payment for something not listed in any receipt",
        "found in a position that could not have been accidental",
        "the evening it was borrowed lasted considerably longer than intended",
        "carried upstairs by someone who emerged some time later looking satisfied",
        "put to uses not listed in any formal inventory of its known history",
        // ── Additional standalone provenance ──────────────────────────────────
        "given in settlement of a dispute both parties later denied having",
        "the denial arrived before the settlement was formally concluded",
        "kept in a reliquary that was labelled incorrectly from the first day",
        "the incorrect label became the accepted name and the correct one was lost",
        "carried by a pilgrim who never reached the destination but arrived somewhere",
        "the somewhere was considered sufficient by those who sent them",
        "used to mark a boundary that no longer legally exists",
        "the boundary dissolved but the marker remained in use by local custom",
        "passed between enemies who had more in common than either admitted",
        "the commonality was only acknowledged after both were dead",
        "found in the foundations of a building constructed two centuries after it was made",
        "carried by a physician who used it for purposes outside their training",
        "the results were considered acceptable by the patient",
        "traded for passage on a ship whose captain asked no questions",
        "the captain asked one question and was satisfied with the non-answer",
        "kept in a locked box by someone who had lost the key before acquiring the box",
        "the box was opened eventually by someone who did not own either",
        "sent ahead to a destination and arrived before the person who sent it",
        "the timing could not be explained by the available transport options",
        "carried during a period of voluntary exile that lasted longer than intended",
        "the exile ended when it was returned by someone who had taken it without asking",
        "used in the founding ceremony of an institution that still operates under a false name",
        "the false name was adopted to protect the true purpose and became the true purpose",
        "donated anonymously to a collection that spent forty years trying to identify the donor",
        "the donor was identified and requested anonymity again, which was granted",
        "held by a translator who refused to translate the inscription on it",
        "the refusal was professional rather than personal, they said",
        "carried by three consecutive rulers none of whom were told about the previous ones",
        "the fourth was told and chose not to continue the sequence",
        // All read as straight archival history; the mythological echo is secondary.
        // "traded to a blind man who said he had been expecting it" — Tiresias,
        //   the blind seer of Thebes who sees past and future simultaneously.
        // "carried to a crossroads and left at midnight" — Hecate, goddess of
        //   crossroads; also the blues tradition of selling one's soul at the crossroads.
        // "accepted as payment by the boatman who usually refuses substitutes" —
        //   Charon the ferryman, who requires a coin placed on the corpse's tongue.
        // "recovered from a sunken city where the bells still ring at low tide" —
        //   the legend of Ys, the drowned Breton city; also Dunwich in Suffolk.
        // "carried by a wanderer with one eye who asked too many questions" — Odin,
        //   who disguised himself as a traveller called Grimnir or Wanderer.
        // "used to cut the thread before the appointed time" — the Moirai, the
        //   Greek Fates who spin, measure, and cut the thread of each life.
        // "given to a smith who worked only at midnight and accepted no payment" —
        //   a recurring figure across Celtic, Germanic, and African forge myth.
        // "traded for a riddle answered correctly on a bridge over still water" —
        //   the riddle-contest motif (Sphinx, Gollum, the Jötnar); also Rumpelstiltskin.
        // "stolen from under a mountain by someone who should have left it alone" —
        //   Smaug's hoard in The Hobbit; also the general dragon-hoard trope.
        // "recovered from an island that was not there the following year" — Hy-Brasil,
        //   the phantom Irish island said to appear once every seven years; also Avalon.
        // "used to bind something in a place between two mountains" — Gleipnir and
        //   the binding of Fenrir on the island Lyngvi between mountains.
        "traded to a blind man who said he had been expecting it",
        "carried to a crossroads and left at midnight by instruction",
        "accepted as payment by the boatman who usually refuses substitutes",
        "recovered from a sunken city where the bells still ring at low tide",
        "carried by a wanderer with one eye who asked too many questions",
        "used to cut the thread before the appointed time",
        "given to a smith who worked only at midnight and accepted no payment",
        "traded for a riddle answered correctly on a bridge over still water",
        "stolen from under a mountain by someone who should have left it alone",
        "recovered from an island that was not there the following year",
        "used to bind something in a place between two mountains",

        // ── Cryptid and paranormal provenance ─────────────────────────────────
        // All written as dry archival entries; the strange detail is offhand.
        // "recovered from a field where the crop geometry suggested prior activity" —
        //   crop circles; presented as a routine field recovery.
        // "found in the possession of someone who had been reported missing" —
        //   abduction / disappearance cases where the returned person has no memory.
        // "recovered from a nest that contained nothing else organic" — bigfoot,
        //   Yowie, Yeti; creatures known for building nests in folklore accounts.
        // "carried by a witness who saw something over the ridge and would not say" —
        //   the class of encounter reports where the witness refuses to elaborate.
        // "left on a doorstep in a county associated with livestock disturbances" —
        //   chupacabra territory (Puerto Rico, Texas); also black dog country (UK).
        // "found tucked inside a tree in forest considered off-limits by locals" —
        //   skinwalker territory; also the Blair Witch forest motif.
        "recovered from a field where the geometry suggested prior activity",
        "found in the possession of someone who had been reported missing",
        "recovered from a nest that contained nothing else organic",
        "carried by a witness who saw something over the ridge and would not say",
        "left on a doorstep in a county associated with livestock disturbances",
        "found tucked inside a tree in a forest locals considered off-limits",

        // ── Gaming — item lore tropes ─────────────────────────────────────────
        // Dry archival language; the gaming resonance is in the framing, not the word.
        // "carried by the last of a hollow line who stopped keeping count" — Dark Souls
        //   hollowing as metaphor for giving up; also a real genealogical dead end.
        // "dropped in the final room by something that had been alone a long time" —
        //   the classic final-boss item drop; the loneliness is the Dark Souls note.
        // "found in a chest that was the only thing in the room" — the Zelda chest;
        //   also a genuinely common archaeological find in burial contexts.
        // "passed down through a guild whose last member retired without ceremony" —
        //   MMORPG guild dissolution; also a real craft-guild historical pattern.
        // "recovered after the encampment was cleared of its previous occupants" —
        //   the post-battle loot mechanic; also a literal archaeological process.
        // "obtained from a merchant who had better stock before everything went wrong" —
        //   the downgraded merchant archetype (Resident Evil 4, Dark Souls); also just
        //   a merchant who fell on hard times.
        "carried by the last of a hollow line who stopped keeping count",
        "dropped in the final room by something that had been alone a long time",
        "found in a chest that was the only object in the room",
        "passed down through a guild whose last member retired without ceremony",
        "recovered after the encampment was cleared of its previous occupants",
        "obtained from a merchant who had better stock before everything went wrong",

        // ── Provenance gaps and institutional failures ─────────────────────────
        "deaccessioned from a collection that was never publicly catalogued",
        "purchased from an estate that refused to name the original owner",
        "transferred between three institutions in the same week for no stated reason",
        "removed from display without announcement and replaced with a reproduction",
        "the reproduction is now in a museum and no one has corrected the label",
        "recovered from a private vault opened for the first time in eighty years",
        "the contents of the vault were never fully disclosed",
        "shipped to a restoration workshop and not returned for thirty years",
        "the restoration notes describe a different object entirely",
        "subjected to provenance review, results sealed by mutual agreement",
        "the mutual agreement included a clause no one will discuss",

        // ── Unusual acquisition and transfer ──────────────────────────────────
        "won in a game that only two people understood",
        "the other player has not requested a rematch",
        "exchanged at a border crossing that has since been closed",
        "the customs record lists it as something it clearly is not",
        "carried by a courier who delivered it to the wrong address and did not correct it",
        "the correct recipient never came forward to claim it",
        "acquired at an estate sale where the seller did not know what they were selling",
        "inherited under a codicil written in a different hand than the rest of the will",
        "the codicil was upheld despite the handwriting discrepancy",
        "left in lieu of explanation by someone who had none to give",
        "given as a parting gift by someone leaving a situation they refused to describe",
        "passed along a chain of acquaintances none of whom knew the original source",
        "the chain was traced backward for eleven links and then stopped",

        // ── Records, documentation, and institutional memory ───────────────────
        "the only surviving record of it is a single line in a ledger marked miscellaneous",
        "the ledger it appears in covers only one other entry, also unexplained",
        "formally documented in a language that was already archaic at the time",
        "the translation was completed by one scholar who then refused further work on it",
        "referenced in a footnote to a document that no longer exists",
        "the footnote is now better known than the document it annotated",
        "catalogued under a classification system that predates the institution itself",
        "the classification system has no key that anyone living can read",
        "listed in an inventory that predates the building it was found in",
        "the inventory number does not match any other item in the collection",
        "described in a letter as ordinary, which was noted as suspicious by the recipient",
        "the recipient's reply has been lost, but their expression was documented",

        // ── Time, return, and cycles ───────────────────────────────────────────
        "returned to the same shelf in the same library four times across two centuries",
        "each return was made by a different person acting on independent instinct",
        "recovered from a location it had been taken from forty years prior",
        "left at the original location as if the forty years had not occurred",
        "the interval between its appearances follows a pattern no one has named",
        "carried by every generation of one family without the next generation being told",
        "the tradition was maintained by instinct rather than instruction",
        "returned to its place of origin by someone who did not know that was what they were doing",

        // ── Earns a second reading in lore context ────────────────────────────
        "passed hand to hand in a dark room where no one gave their name",
        "used in a private capacity by three successive holders of the same office",
        "kept in an inner pocket and not declared at any border crossing",
        "returned to a bedside before dawn without discussion",
        "the engagement was brief, vigorous, and not recorded",
        "used for warmth on a cold journey by two people sharing a room",
        "its tenure in that particular drawer lasted longer than the relationship",
        "handled extensively by parties who have since stopped acknowledging each other",
        "loaned for an evening and returned with the request not to ask",
        "employed in a fashion its maker almost certainly did not intend",
        "passed beneath a table during a formal dinner with practiced ease",
        "the inn has no record of how many occupants shared the room that night",
        "its use that season required the discretion of all involved",
        "kept in a place reserved for things one does not discuss at table",
        "returned with warmth still in it and no explanation forthcoming",
        "the servants were asked to give the room an extra hour",
        "the hour became two and no one complained",
        // ── Mythology and legend — provenance ─────────────────────────────
        // Entries read as archival history; mythological resonance is secondary.
        // "passed from a giant to a smith to a shepherd in a single generation" —
        //   the three-transfer origin story common to Norse and Irish forge myth.
        // "found at the base of a world tree with no record of how it arrived" —
        //   Yggdrasil; objects found at its roots carry cosmological significance.
        // "carried by a hero who descended alive and returned without it" —
        //   the katabasis motif: Orpheus, Heracles, Odysseus all went down and came back.
        // "used to seal a bargain at a crossroads by moonlight" — the blues/folk
        //   crossroads deal; also Hecate's domain (crossroads, moon, magic).
        // "brought up from a depth where sunlight does not reach by someone who should not have survived" —
        //   Jonah, Odysseus in the cave, any number of descent-and-return myths.
        // "passed through the hands of twelve and lost with the thirteenth" —
        //   the thirteen motif: twelve disciples + Judas; twelve gods + Loki at Baldr's feast.
        // "carried by the wandering figure who appeared at every major battle but fought in none" —
        //   Odin as the wandering observer; also Merlin, the Wandering Jew, the Flying Dutchman.
        // "taken from a sleeping guardian by someone too clever for their own good" —
        //   Prometheus stealing fire; Jack and the Beanstalk; Perseus and the sleeping Graeae.
        // "won in a riddle contest against something older than the mountains" —
        //   Bilbo vs Gollum; also the Sphinx; also Odin's contests with the Jötnar.
        // "made for a war between things that were not gods but were worshipped anyway" —
        //   the Titans vs Olympians; the Fomorians vs Tuatha Dé Danann; the Mahabharata war.
        "passed from a giant to a smith to a shepherd in a single generation",
        "found at the base of a world tree with no record of how it arrived",
        "carried by a hero who descended alive and returned without it",
        "used to seal a bargain at a crossroads by moonlight",
        "brought up from a depth where sunlight does not reach by someone who survived",
        "passed through the hands of twelve and lost with the thirteenth",
        "carried by the wandering figure who appeared at every battle but fought in none",
        "taken from a sleeping guardian by someone too clever for their own good",
        "won in a riddle contest against something older than the mountains",
        "made for a war between things that were not gods but were worshipped anyway",

        // ── Cryptid and strange history ────────────────────────────────────────
        // "recovered from a structure that should not have been there" — the unexplained
        //   megalithic structures: Nan Madol, the Bosnian pyramids, Gobekli Tepe.
        // "found at the edge of a site where compasses stopped working" — magnetic anomaly
        //   zones: the Bermuda Triangle, the Hessdalen valley, certain igneous formations.
        // "present during the three days a town was evacuated for reasons never disclosed" —
        //   the genre of inexplicable government evacuations (Tooele, Dugway, etc.).
        // "carried by someone who had been missing for years and returned without aging" —
        //   time-slip and alien abduction return narratives; also the fairy hill legend.
        // "recovered from a vehicle found running on an empty road with no driver" —
        //   the ghost vehicle trope; also a real recurring police report category.
        "recovered from a structure that should not have been there",
        "found at the edge of a site where compasses stopped working",
        "present during the three days a town was evacuated for undisclosed reasons",
        "carried by someone who had been missing for years and returned unchanged",
        "recovered from a vehicle found running on an empty road with no driver",
        "documented in a photograph where it should not appear",
        "the photographer did not remember taking the photograph",
        "filed alongside reports from a county known for unusual disappearances",
        "recovered from the second floor of a building that only has one floor",
        "the building has only ever had one floor and this has been confirmed",

        // ── Historical — unusual transfers and events ──────────────────────────
        "offered to a conqueror who refused it as too valuable to carry",
        "left behind by a retreating army as a deliberate insult",
        "the insult was not received as intended",
        "used to ratify a document that was immediately declared void",
        "the void was itself later voided",
        "looted from a looted collection, the original looting undocumented",
        "restored to a nation that no longer existed to receive it",
        "held in trust by a neutral party for sixty years with no resolution",
        "the neutral party eventually took a side",
        "carried by an envoy who was arrested on arrival and released without explanation",
        "the explanation arrived eleven years after the release",
        "presented to a gathering that adjourned before receiving it",
        "the adjournment was called specifically to avoid receiving it",
        "traded across three wars without being used in any of them",
        "the three wars were fought over smaller things",
        "discovered in a shipment whose declared contents were entirely different",
        "the declared contents were also unusual",
        "passed through fourteen countries in a single year under different names",
        "the fourteenth name is the one it still uses",
        "commissioned by one regime and delivered to the next",
        "the next regime had no record of the commission",
        "found in an archive that was supposed to have been destroyed in the fire",
        "the fire did not reach the archive",
        "the archive's survival was not publicized",
        "held by a caretaker dynasty for four hundred years without the dynasty knowing",
        "they thought it was something else",
        "the mistake was corrected and then immediately re-made",

        // ── Personal and intimate history ──────────────────────────────────────
        "given between people who would not speak of it and didn't need to",
        "kept for fifty years by someone who claimed to have lost it",
        "lost deliberately and found by exactly the right person",
        "carried through a marriage by one party without the other knowing",
        "the other party knew and chose not to say",
        "inherited by a child who was told it was ordinary until they were old enough",
        "passed at the end of a long conversation that was never summarized",
        "given to a stranger on a train who wrote back once and then did not",
        "the letter was kept",
        "carried by a lighthouse keeper who never explained how they came to have it",
        "traded for a kindness with no expectation of trade",
        "found under the floorboards of a house bought from an estate",
        "the estate had no record of it",
        "left by someone who assumed they would return and did not",
        "the room was kept as they left it for years",

        // ── Institutional and archival edge cases ──────────────────────────────
        "catalogued by a librarian who retired the following day without explanation",
        "the catalogue entry is the only thing they left",
        "stored in a building that was demolished before the contents were removed",
        "survived the demolition and was found in the rubble",
        "its condition after the demolition was better than before",
        "entered into a permanent collection and removed within the week",
        "the removal was not recorded in the accessions register",
        "the accessions register for that year is missing its final pages",
        "referenced in a will as a specific bequest with the wrong description",
        "the wrong description fit something else that was also in the estate",
        "both items were bequeathed to the same recipient, who kept only this one",
        "the one that was not kept has not been located",

        // ── Earns a second reading in lore context (histories) ────────────────
        "employed in an arrangement that required the participation of both parties",
        "the arrangement was renewed on three separate occasions",
        "kept in a private drawer and produced when the situation called for it",
        "the situation called for it more often than expected",
        "used in a context that required the door to be locked",
        "the door was locked and remained so for the duration",
        "found in the morning on a bedside table beside a note that only said: thank you",
        "carried upstairs on two documented occasions and many undocumented ones",
        "the undocumented occasions can be estimated from the wear pattern",
        "exchanged between two people whose correspondence was otherwise very formal",
        "the formality broke down on the third exchange",
        "used for purposes that both parties were too polite to name afterward",
        "the purposes were named eventually, in private",
        // ── Final top-up to 500 ───────────────────────────────────────────
        "carried by a mapmaker who drew the territory around it rather than what was there",
        "the territory drawn does not match any geography",
        "found in a city that was built on top of the city where it was made",
        "the lower city does not appear on any official record",
        "passed between astronomers who disagreed about what they were observing",
        "the disagreement was never resolved because one of them disappeared",
        "used in an experiment whose success was considered heretical",
        "the heresy was eventually reclassified as methodology",
        "kept by a translator who noted every language it had been described in",
        "the list of languages is longer than expected",
        "carried through a siege by the only person who left before it ended",
        "the ending was worse than expected",
        "donated to an archive with a letter explaining everything",
        "the letter has never been opened",
        "the archive does not explain why the letter has not been opened",
        "the archive also has a letter about itself that has not been opened",
        "held by a keeper who never slept in the same place twice while holding it",
        "the keeper's route, if mapped, spells something in an extinct alphabet",
        "traded for a debt that the debtor maintained did not exist",
        "the debt existed",
        "owned by three generations of a family none of whom liked each other",
        "the dislike was the only thing they had in common besides this",
        "found sealed in a cave that had been sealed from the inside",
        "the sealing mechanism is not the mystery",
        "carried into a conference that produced no written record",
        "the lack of written record was itself written into the terms",
        "used to sign the terms, which is noted here instead",
        "passed to someone in a dream and found on their pillow",
        "the someone had it independently appraised before telling anyone",
        "the appraiser's report is held separately and summarized only as: consistent",
        "consistent with what is not specified",
        "found in the possession of an infant who could not explain where they got it",
        "the infant later gave a partial explanation",
        "the partial explanation was considered credible",
        "inherited by someone who had previously tried to destroy it",
        "the attempt failed and they accepted the inheritance",
        "given as security for a loan that was repaid on time for once",
        "the lender kept it anyway out of sentiment",
        "the sentiment was mutual and undiscussed",
        "held by a dynasty that ran out of heirs and left it to the most sensible stranger",
        "the stranger accepted and was sensible about it",
        "sold at auction by someone who did not own it",
        "the buyer did not know this and the sale was valid",
        "the validity was later tested and held",
        "carried by a pilgrim who completed the pilgrimage and felt nothing",
        "the feeling nothing was the point",
        "loaned to a poet for a single afternoon that produced a significant work",
        "the poem does not mention it",
        "the poet insisted the poem was not about it",
        "later analysis suggested otherwise",
        // ── Top-up to 500 ────────────────────────────────────────────────
        "carried by three people who each believed they were the first to hold it",
        "passed at a funeral to someone who had never met the deceased",
        "recovered from a place that had been confirmed empty on the previous inspection",
        "held by a kingdom that no longer claims to have existed",
        "found in a drawer that was not there the last time the room was searched",
        "left as the only item in an otherwise empty bequest",
        "carried by someone who had previously sworn never to carry it",
        "the oath was not broken — the circumstances were reclassified",
        "used to settle a score that predated everyone involved by a century",
        "purchased with currency that was not accepted anywhere else that day",
        "the currency was accepted anyway and not examined too closely",
        "carried through a city during the hours it did not officially exist",
        "kept in a place that required two people to access, one of whom was absent",
        "it was accessed regardless and the absence was not noted in the record",
        "traded three times in a single afternoon, ending where it started",
        "no one involved admitted to having set this outcome in motion",
        "passed to a stranger who said they had been sent to collect it",
        "no one could say who had sent them and everyone agreed to continue",
        "held by an institution that has changed its name four times since acquiring it",
        "each name change was for unrelated reasons that were never fully explained",
        "carried across a threshold that no longer exists between places that still do",
        "the crossing was considered significant by one party and routine by the other",
        "loaned indefinitely to a collection that considers indefinite loans ownership",
        "the original lender's position on this is not recorded",
        "kept in a location that appeared in two different maps as two different things",
        "the maps were made by the same person on the same day",
        "used to ratify an agreement between parties who could not be in the same room",
        "the agreement held for longer than either party expected",
        "neither party will say why they expected it to fail",
        "recovered by someone who had given up and stopped looking the day before",
        "the stopping and the finding may not have been unrelated",
        "offered as a gift and refused four times before being accepted on the fifth",
        "the fifth refusal was retracted without explanation",
        "kept in continuous use for a century by people who thought it was something else",
        "the correction, when it came, did not change how they used it",
        "it has passed through more hands than have been recorded and fewer than exist",
        "it arrived before anyone knew it was missing"

    );
    }
}
