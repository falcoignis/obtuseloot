package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public class AbilityRegistry {
    private final List<AbilityDefinition> all;

    public AbilityRegistry() {
        this.all = List.of(
                new AbilityDefinition("precision.mark", "Marked Interval", AbilityFamily.PRECISION, AbilityTrigger.ON_HIT,
                        List.of(new AbilityEffect("Consecutive hits mark target and unleash delayed puncture", AbilityEffectType.ENEMY_INTERACTION, 0.02)),
                        "Marks after 3 hits.", "Mark duration increases after reposition.", "Marked enemies chain to nearest foe.", "Awakening adds exposed-window timing.", "Fusion lets marks persist through target swaps."),
                new AbilityDefinition("brutality.rush", "Ravenous Momentum", AbilityFamily.BRUTALITY, AbilityTrigger.ON_KILL,
                        List.of(new AbilityEffect("Kills trigger temporary aggression window with cleave pulses", AbilityEffectType.TEMPORARY_STATE, 0.03)),
                        "Kill grants momentum burst.", "Bursts stack during chain combat.", "Multi-kill detonates intimidation wave.", "Awakening extends burst on low health.", "Fusion converts burst to battlefield shockwave."),
                new AbilityDefinition("survival.rebuke", "Lastline Rebuke", AbilityFamily.SURVIVAL, AbilityTrigger.ON_LOW_HEALTH,
                        List.of(new AbilityEffect("Low health creates retaliatory ward and stagger pulse", AbilityEffectType.CONDITIONAL_MECHANIC, 0.01)),
                        "Ward appears below threshold.", "Ward grants one retaliatory pulse.", "Pulse steals tempo on hit received.", "Awakening adds recovery echo.", "Fusion causes ward to project to nearby ally."),
                new AbilityDefinition("mobility.echo", "Slipstream Echo", AbilityFamily.MOBILITY, AbilityTrigger.ON_REPOSITION,
                        List.of(new AbilityEffect("Repositioning stores kinetic echo released on next hit", AbilityEffectType.MOVEMENT_INTERACTION, 0.02)),
                        "Dash stores one echo.", "Echo can fork to two targets.", "Chain movement refreshes echo immediately.", "Awakening leaves wake zone.", "Fusion turns wake into pull field."),
                new AbilityDefinition("chaos.bloom", "Unstable Bloom", AbilityFamily.CHAOS, AbilityTrigger.ON_DRIFT_MUTATION,
                        List.of(new AbilityEffect("Drift mutation seeds random anomaly zones", AbilityEffectType.BATTLEFIELD_INFLUENCE, 0.02)),
                        "Mutation spawns minor anomaly.", "Anomaly picks one of three hostile effects.", "Boss combat doubles anomaly interactions.", "Awakening lets player reroll anomaly once.", "Fusion links anomaly to kill-chain tempo."),
                new AbilityDefinition("consistency.rhythm", "Measured Cadence", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_CHAIN_COMBAT,
                        List.of(new AbilityEffect("Rhythm windows reward precise combat timing", AbilityEffectType.TIMING_MECHANIC, 0.02)),
                        "Every 4th hit triggers cadence.", "Cadence windows broaden with sustained combat.", "Missed rhythm converts into defensive beat.", "Awakening grants cadence carryover after kill.", "Fusion causes cadence to synchronize with allies.")
        );
    }

    public List<AbilityDefinition> all() {
        return all;
    }

    public List<AbilityDefinition> byFamily(AbilityFamily family) {
        List<AbilityDefinition> out = new ArrayList<>();
        for (AbilityDefinition def : all) {
            if (def.family() == family) {
                out.add(def);
            }
        }
        return out;
    }
}
