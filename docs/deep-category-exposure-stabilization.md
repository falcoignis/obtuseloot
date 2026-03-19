# Deep Category Exposure Stabilization

Probe commands used for this refinement pass:

- `timeout 180s mvn -q -DskipTests compile`
- `timeout 180s mvn -q -Dtest=BroadSkillCategoryExpansionProbeTest test`
- `javac -cp "$(cat /tmp/obtuseloot.cp):target/classes" /tmp/CategoryExposureProbeMain.java`
- `java -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes" CategoryExposureProbeMain`

## SECTION 1: OVERCORRECTION ANALYSIS

The rebalance pass was still structurally correct, but its controls were interacting too early and too often:

- **Category balancing** and **template anti-dominance** were both applied inside the same composite template score, so category-level competitiveness could be reduced before intra-category redistribution had a chance to work.
- **Penalty stacking** could compound category exposure pressure, template recency pressure, and niche-consistency pressure into an aggregate reduction that was large enough to collapse category volume.
- **Narrow-trigger categories** such as stealth were still paying too much for near-trigger compatibility even when the candidate stayed inside the same trigger family.
- **Low-volume categories** had no conditional recovery path once a candidate was already valid, so reachability remained fragile under ordinary probes.

This refinement pass keeps the novelty, similarity, anti-dominance, and category balancing systems intact, but stages them so they stop suppressing category competitiveness and return to redistributing exposure.

## SECTION 2: SCORING FIXES

The implementation changes stayed inside `ProceduralAbilityGenerator` scoring and selection only.

1. **Decoupled inter-category and intra-category effects**
   - Category exposure balancing now operates at **category selection time**.
   - Template anti-dominance remains **template-local** through recent-template recency pressure and no longer reduces category-level probability mass directly.

2. **Category-mass normalization after category adjustments**
   - After category-stage niche weighting, category exposure pressure, and low-volume recovery boosts are applied, category scores are rescaled so total category mass remains stable.
   - The normalization is multiplicative, so it preserves category ordering rather than flattening the distribution.

3. **Bounded cumulative penalty stacking**
   - The combined penalty from template anti-dominance and niche-consistency pressure is capped so the total reduction cannot exceed roughly **25%**.
   - Category-stage penalties are bounded separately before category normalization.

4. **Conditional low-volume recovery**
   - Low-volume category boosts are only applied when the candidate already clears the novelty floor, remains niche-compatible, and is below the high-similarity cutoff.
   - This keeps stealth and sensing reachability improvements from forcing invalid candidates.

5. **Trigger smoothing for narrow-span categories**
   - Categories with trigger span `<= 4` now get stronger bounded smoothing.
   - Near-trigger matches in the same or adjacent trigger class retain materially more value, while invalid trigger contexts remain excluded.

6. **Stealth-specific intra-category diversification path**
   - Once stealth wins category selection, template choice is sampled with a tempered weight curve instead of a hard winner-take-all pick.
   - This keeps the category reachable under normal probes without removing anti-dominance or category balancing.

## SECTION 3: BEFORE vs AFTER PROBES

Baseline reference from the prior validation report:

| Category | Prior total hits | Prior notable issue |
| --- | ---: | --- |
| Survival / adaptation | 14 | Category volume collapsed after rebalance |
| Sensing / information | 4 | Focused probe volume still too low |
| Stealth / trickery / disruption | 3 | Only 5 / 6 reachable in normal probe |

Current refinement probe results:

| Category | After total hits | Reachability | Top-3 share | Notes |
| --- | ---: | --- | ---: | --- |
| Survival / adaptation | 49 | 11 / 11 | 51.02% | Volume restored well above the collapsed post-rebalance state while keeping the tail active. |
| Sensing / information | 31 | 9 / 9 | 51.61% | Focused probe volume recovered with no single template monopolizing the category. |
| Stealth / trickery / disruption | 30 | 6 / 6 | 83.33% | Normal-probe reachability is restored, but concentration is still higher than ideal. |

Detailed probe snapshots:

- **Survival**
  - Hits: `{environment.structure_attunement=2, environment.terrain_affinity=6, environment.weather_sensitivity=6, survival.ember_keeper=5, survival.exposure_weave=5, survival.gentle_harvest=8, survival.hardiness_loop=6, survival.herd_instinct=3, survival.scarcity_compass=1, survival.storm_shelter_ledger=6, survival.weather_omen=1}`
- **Sensing**
  - Hits: `{precision.artifact_sympathy=4, precision.echo_locator=2, precision.material_insight=3, precision.vein_whisper=1, sensing.cache_resonance=2, sensing.contraband_tell=3, sensing.faultline_ledger=5, sensing.route_grammar=6, sensing.witness_lag=5}`
- **Stealth**
  - Hits: `{stealth.dead_drop_lattice=5, stealth.echo_shunt=14, stealth.hushwire=6, stealth.paper_trail=1, stealth.shadow_proxy=2, stealth.threshold_jam=2}`

## SECTION 4: CATEGORY STABILITY

### Preserved

- **Novelty ordering** was left structurally intact because novelty weights and similarity thresholds were not changed.
- **Niche identity** remains encoded in the original niche-weight and category-weight functions rather than being replaced by a new routing system.
- **Redundancy pressure** remains active through similarity penalties and anti-dominance recency pressure.
- **Survival long-tail participation** improved materially relative to the collapsed post-rebalance state.

### Remaining concern

- The legacy broad niche-distribution probe still reports **weaker-than-target explorer vs builder separation** under the new staging logic.
- Stealth now passes normal-probe reachability, but it still leans too heavily on `stealth.echo_shunt` in the focused probe.

## SECTION 5: FINAL READINESS

Readiness judgment for the requested goals:

- **Survival:** materially improved and within target on concentration for this probe.
- **Sensing:** materially improved and balanced in focused sampling.
- **Stealth:** normal-probe reachability restored to 6 / 6, but concentration still needs one more refinement pass.
- **Stability:** protected systems were preserved; however, the existing broad niche-separation regression means the pass is not a full clean sign-off yet.

Recommended interpretation: this pass successfully fixes the major overcorrection behavior and restores ordinary category competitiveness, but it should be treated as a **stabilizing partial** rather than the final terminal polish.

CATEGORY_EXPOSURE_STABILIZATION_RESULT: PARTIAL
