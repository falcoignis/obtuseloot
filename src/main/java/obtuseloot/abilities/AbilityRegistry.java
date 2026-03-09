package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public class AbilityRegistry {
    private final List<AbilityTemplate> templates;

    public AbilityRegistry() {
        this.templates = List.of(
                // Precision
                template("precision.sigil", "Sigil of Delay", AbilityFamily.PRECISION, AbilityTrigger.ON_HIT, AbilityMechanic.MARK,
                        "Marks on cadence then ruptures during reposition windows", "extended mark chain", "volatile puncture", "timed exposed window", "mark persistence through swaps", "echo mark from memory"),
                template("precision.gambit", "Gambit Needle", AbilityFamily.PRECISION, AbilityTrigger.ON_MULTI_KILL, AbilityMechanic.CHAIN_ESCALATION,
                        "Successive precision hits tighten a crescendo finisher", "combo stabilizer", "drift injects false tells", "awakening keeps combo in downtime", "fusion adds allied follow-through", "precision history preserves stacks"),
                template("precision.refractor", "Refractor Lattice", AbilityFamily.PRECISION, AbilityTrigger.ON_REPOSITION, AbilityMechanic.PULSE,
                        "Reposition refracts prior trajectory into delayed pulse cuts", "additional prism nodes", "drift skews pulse timing", "awakening doubles refracted lane", "fusion links prism endpoints", "memory replays best lane"),
                template("precision.vow", "Vow of Exposed Truth", AbilityFamily.PRECISION, AbilityTrigger.ON_BOSS_KILL, AbilityMechanic.GUARDIAN_PULSE,
                        "Boss pressure builds truth-seal windows for precision punish", "boss aperture extension", "drift inverts weakpoint windows", "awakening adds truth overguard", "fusion propagates truth-seal", "boss memory sharpens exposure"),

                // Brutality
                template("brutality.howl", "Howl of Pursuit", AbilityFamily.BRUTALITY, AbilityTrigger.ON_KILL, AbilityMechanic.BURST_STATE,
                        "Kills open frenzy state with chase pulses", "frenzy overlap", "detonation on drift", "awakening extends frenzy", "fusion converts frenzy to shockfront", "memory chain heat"),
                template("brutality.maul", "Ravenous Maul", AbilityFamily.BRUTALITY, AbilityTrigger.ON_MULTI_KILL, AbilityMechanic.RETALIATION,
                        "Chain takedowns arm retaliatory maul arcs", "wider retaliation window", "drift may overcommit arc", "awakening hardens maul cadence", "fusion shares retaliation mark", "memory of overkill adds bite"),
                template("brutality.quarry", "Quarry Breaker", AbilityFamily.BRUTALITY, AbilityTrigger.ON_BOSS_KILL, AbilityMechanic.BATTLEFIELD_FIELD,
                        "Boss finish fractures zone into hunt lanes", "more fracture lanes", "drift causes lane collapse", "awakening adds predation anchor", "fusion enables lane chaining", "boss hunts sustain zone"),
                template("brutality.bleedrush", "Bleedrush Overrun", AbilityFamily.BRUTALITY, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.CHAIN_ESCALATION,
                        "Prolonged pressure overdrives momentum into impact rushes", "rush stack retention", "drift causes reckless surges", "awakening converts stacks to armor shred", "fusion syncs rush with allies", "memory keeps fury tempo"),

                // Survival
                template("survival.ward", "Ward of Last Breath", AbilityFamily.SURVIVAL, AbilityTrigger.ON_LOW_HEALTH, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Low health raises threshold ward and retaliatory pulse", "ward echo", "hollow rebuke", "awakening recovery latch", "fusion projects ward", "memory survival lock"),
                template("survival.embankment", "Embankment Loop", AbilityFamily.SURVIVAL, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.RECOVERY_WINDOW,
                        "Long engagements open recurring sustain windows", "extra restoration loops", "drift leaks sustain into spikes", "awakening banks unused restoration", "fusion emits team embankment", "long battle memory extends loop"),
                template("survival.eidolon", "Eidolon Shelter", AbilityFamily.SURVIVAL, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.MEMORY_ECHO,
                        "Near-failure memories summon protective eidolon echoes", "echo shelter duplication", "drift causes shelter flicker", "awakening grants adaptive shell", "fusion turns shell communal", "trauma memory hardens shell"),
                template("survival.remnant", "Remnant Bastion", AbilityFamily.SURVIVAL, AbilityTrigger.ON_AWAKENING, AbilityMechanic.GUARDIAN_PULSE,
                        "Awakening anchors defensive remnant that retaliates", "remnant pulse interval", "drift destabilizes remnant spacing", "awakening overclocks remnant", "fusion nests second remnant", "memory binds remnant to rescues"),

                // Mobility
                template("mobility.wake", "Wake Spiral", AbilityFamily.MOBILITY, AbilityTrigger.ON_REPOSITION, AbilityMechanic.MOVEMENT_ECHO,
                        "Movement stores wake that loops into next engagement", "wake forks", "chaotic wake jitter", "awakening leaves sustained lane", "fusion adds pull field", "memory lane recall"),
                template("mobility.skimmer", "Skimmer's Draft", AbilityFamily.MOBILITY, AbilityTrigger.ON_MOVEMENT, AbilityMechanic.PULSE,
                        "Continuous motion sheds draft pulses that curve around targets", "draft persistence", "drift bends pulse vectors", "awakening stores vector debt", "fusion crossfeeds draft lines", "memory recovers best movement arcs"),
                template("mobility.latch", "Latchstep Relay", AbilityFamily.MOBILITY, AbilityTrigger.ON_HIT, AbilityMechanic.MARK,
                        "Hits place relay marks consumed by rapid reposition", "relay chain depth", "drift randomizes latch destination", "awakening enables instant relay reuse", "fusion shares relay lattice", "memory favors safe latches"),
                template("mobility.escape", "Escape Velocity", AbilityFamily.MOBILITY, AbilityTrigger.ON_LOW_HEALTH, AbilityMechanic.RECOVERY_WINDOW,
                        "Danger thresholds convert evasive bursts into recover windows", "longer evasive frame", "drift can overshoot escape vector", "awakening adds controlled rollback", "fusion pulls ally into corridor", "survival memory stabilizes exits"),

                // Chaos
                template("chaos.spore", "Spore of Divergence", AbilityFamily.CHAOS, AbilityTrigger.ON_DRIFT_MUTATION, AbilityMechanic.UNSTABLE_DETONATION,
                        "Drift erupts anomaly spores that alter local combat", "anomaly split", "instability bloom", "awakening reroll", "fusion chain reaction", "memory scar resonance"),
                template("chaos.fracture", "Fracture Psalm", AbilityFamily.CHAOS, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.BATTLEFIELD_FIELD,
                        "Contradictory memories fracture the arena into shifting psalms", "additional psalm nodes", "drift warps hymn cadence", "awakening inverts one psalm", "fusion overlays psalms", "trauma memory feeds dissonance"),
                template("chaos.paradox", "Paradox Lantern", AbilityFamily.CHAOS, AbilityTrigger.ON_FUSION, AbilityMechanic.REVENANT_TRIGGER,
                        "Fusion awakens paradox revenant that rewrites trigger order", "extra paradox checkpoints", "drift shuffles checkpoint rewards", "awakening freezes one paradox branch", "fusion doubles revenant span", "memory pressure deepens paradox"),
                template("chaos.gale", "Entropy Gale", AbilityFamily.CHAOS, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.UNSTABLE_DETONATION,
                        "Sustained combat births entropy gales with nonlinear detonations", "gale persistence", "drift introduces singular spikes", "awakening pins one safe corridor", "fusion chains gales", "memory of rampages expands gales"),

                // Consistency
                template("consistency.rhythm", "Rhythm Bastion", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.CHAIN_ESCALATION,
                        "Sustained combat opens rhythm gates and fallback beats", "longer rhythm", "drift converts miss to pulse", "awakening carryover", "fusion team sync", "memory cadence"),
                template("consistency.clock", "Clockwork Covenant", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_HIT, AbilityMechanic.RECOVERY_WINDOW,
                        "Reliable hit cadence grants predictable recovery slices", "covenant cadence extension", "drift offsets cadence by one beat", "awakening stores unused slices", "fusion shares covenant beat", "discipline memory fixes cadence"),
                template("consistency.anchor", "Anchor Thesis", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_BOSS_KILL, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Boss victories forge stable anchors for future engagements", "anchor stack depth", "drift adds temporary anchor decay", "awakening hard-locks primary anchor", "fusion creates linked anchors", "boss memory strengthens locks"),
                template("consistency.refrain", "Refrain Protocol", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.MEMORY_ECHO,
                        "Memory events replay proven patterns to avoid collapse", "refrain queue depth", "drift injects dissonant refrain", "awakening filters dissonance", "fusion shares pattern library", "memory confirms safe recursion")
        );
    }

    private AbilityTemplate template(String id, String name, AbilityFamily family, AbilityTrigger trigger, AbilityMechanic mechanic,
                                    String effectPattern, String evolutionVariant, String driftVariant, String awakeningVariant, String fusionVariant,
                                    String memoryVariant) {
        return new AbilityTemplate(id, name, family, trigger, mechanic, effectPattern, evolutionVariant, driftVariant, awakeningVariant, fusionVariant,
                memoryVariant, List.of(new AbilityModifier("support.signature", "secondary tuning hook", 0.04, false)));
    }

    public List<AbilityTemplate> templates() { return templates; }

    public List<AbilityTemplate> byFamily(AbilityFamily family) {
        List<AbilityTemplate> out = new ArrayList<>();
        for (AbilityTemplate template : templates) {
            if (template.family() == family) {
                out.add(template);
            }
        }
        return out;
    }
}
