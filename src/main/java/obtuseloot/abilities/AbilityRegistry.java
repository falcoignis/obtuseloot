package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public class AbilityRegistry {
    private final List<AbilityTemplate> templates;

    public AbilityRegistry() {
        this.templates = List.of(
                new AbilityTemplate("precision.sigil", "Sigil of Delay", AbilityFamily.PRECISION, AbilityTrigger.ON_HIT, AbilityMechanic.MARK,
                        "Marks on cadence then ruptures during reposition windows", "extended mark chain", "volatile puncture", "timed exposed window", "mark persistence through swaps", "echo mark from memory", List.of(new AbilityModifier("support.window", "narrow damage amp window", 0.06, false))),
                new AbilityTemplate("brutality.howl", "Howl of Pursuit", AbilityFamily.BRUTALITY, AbilityTrigger.ON_KILL, AbilityMechanic.BURST_STATE,
                        "Kills open frenzy state with chase pulses", "frenzy overlap", "detonation on drift", "awakening extends frenzy", "fusion converts frenzy to shockfront", "memory chain heat", List.of(new AbilityModifier("support.tempo", "small tempo gain", 0.04, false))),
                new AbilityTemplate("survival.ward", "Ward of Last Breath", AbilityFamily.SURVIVAL, AbilityTrigger.ON_LOW_HEALTH, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Low health raises threshold ward and retaliatory pulse", "ward echo", "hollow rebuke", "awakening recovery latch", "fusion projects ward", "memory survival lock", List.of()),
                new AbilityTemplate("mobility.wake", "Wake Spiral", AbilityFamily.MOBILITY, AbilityTrigger.ON_REPOSITION, AbilityMechanic.MOVEMENT_ECHO,
                        "Movement stores wake that loops into next engagement", "wake forks", "chaotic wake jitter", "awakening leaves sustained lane", "fusion adds pull field", "memory lane recall", List.of()),
                new AbilityTemplate("chaos.spore", "Spore of Divergence", AbilityFamily.CHAOS, AbilityTrigger.ON_DRIFT_MUTATION, AbilityMechanic.UNSTABLE_DETONATION,
                        "Drift erupts anomaly spores that alter local combat", "anomaly split", "instability bloom", "awakening reroll", "fusion chain reaction", "memory scar resonance", List.of()),
                new AbilityTemplate("consistency.rhythm", "Rhythm Bastion", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.CHAIN_ESCALATION,
                        "Sustained combat opens rhythm gates and fallback beats", "longer rhythm", "drift converts miss to pulse", "awakening carryover", "fusion team sync", "memory cadence", List.of())
        );
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
