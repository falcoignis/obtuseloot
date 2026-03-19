# Deep Post-Authenticity Applicability Validation

This was a measurement-only pass against the current codebase. I re-used the prior post-authenticity validation shape, then sampled the live generator using the current bounded under-sampled applicability logic without modifying weights, rules, templates, novelty systems, or authenticity gates.

## SECTION 1: VALIDATION CONFIGURATION

- Validation date: 2026-03-19 UTC.
- Normal multi-scenario probe: explorer-heavy, ritualist-heavy, warden-heavy, mixed, random-baseline.
- Normal probe volume: 6,250 weighted selections total (1,250 per scenario).
- Category-forced probe volume: 8,500 weighted selections total (850 per category).
- Authenticity audit method: the same measurement shape used in the prior authenticity report — reject any output classified as vanilla-equivalent, stat-only, or complexity-gate-failed using the existing detector rules already documented in the repo baseline.
- Baseline used for direct comparison: `docs/deep-authenticity-validation.md`.
- Additional guardrail check run: `IntraCategoryNormalizationProbeTest`, which still reports bounded applicability behavior and the expected inspect/structure trigger-family adjacency behavior for `stealth.trace_fold` and related templates.

### Normal probe scenario snapshots

- **explorer-heavy**: Survival / adaptation=275, Traversal / mobility=221, Sensing / information=199, Social / support / coordination=121, Stealth / trickery / disruption=118.
- **ritualist-heavy**: Ritual / strange utility=380, Social / support / coordination=208, Stealth / trickery / disruption=153, Sensing / information=147, Crafting / engineering / automation=142.
- **warden-heavy**: Traversal / mobility=285, Survival / adaptation=228, Sensing / information=157, Combat / tactical control=149, Resource / farming / logistics=149.
- **mixed**: Resource / farming / logistics=234, Survival / adaptation=216, Social / support / coordination=214, Crafting / engineering / automation=149, Stealth / trickery / disruption=132.
- **random-baseline**: Resource / farming / logistics=275, Crafting / engineering / automation=271, Survival / adaptation=175, Defense / warding=133, Stealth / trickery / disruption=117.

## SECTION 2: REJECTION / OUTPUT INTEGRITY

- Total candidates generated: 14750.
- Total rejected: 0.
- Rejection rate: 0.00%.
- Rejection breakdown:
  - vanilla-equivalent: 0.
  - stat-only: 0.
  - failed complexity gate: 0.
- Final selected ability count after authenticity audit: 14750.
- Final stat-only count: 0.
- Final vanilla-equivalent count: 0.
- Integrity verdict: **pass** on authenticity hygiene. This pass produced zero stat-only outputs and zero vanilla-equivalent outputs.

## SECTION 3: CATEGORY VIABILITY

The table below uses the **category-forced post-authenticity probe** because that is the direct viability check requested for whether a category still stays reachable after authenticity filtering. I also include the normal-pressure snapshot where it materially changes interpretation.

| Category | Forced total hits | Reachability | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit | Normal-pressure note |
| --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| Sensing / information | 850 | 3/9 | precision.echo_locator, precision.vein_whisper, sensing.cache_resonance, sensing.faultline_ledger, sensing.route_grammar, sensing.witness_lag | 92.47% (precision.artifact_sympathy) | 100.00% | 3 | 9/9 reachable under normal pressure |
| Traversal / mobility | 850 | 2/8 | exploration.biome_attunement, exploration.cartographers_echo, exploration.trail_sense, mobility.footprint_memory, mobility.rift_stride, mobility.skyline_fold | 59.53% (mobility.compass_stories) | 100.00% | 2 | 8/8 reachable under normal pressure |
| Survival / adaptation | 850 | 3/11 | environment.terrain_affinity, survival.exposure_weave, survival.gentle_harvest, survival.hardiness_loop, survival.herd_instinct, survival.scarcity_compass, survival.storm_shelter_ledger, survival.weather_omen | 46.47% (environment.structure_attunement) | 100.00% | 3 | 11/11 reachable under normal pressure |
| Ritual / strange utility | 850 | 4/13 | chaos.dust_memory, chaos.ritual_echo, chaos.witness, evolution.lineage_fortification, evolution.niche_architect, evolution.resource_parasitism, evolution.ritual_amplifier, ritual.moon_debt, ritual.oath_circuit | 61.41% (ritual.altar_resonance) | 99.88% | 3 | 11/13 reachable under normal pressure |
| Defense / warding | 850 | 8/8 | none | 49.06% (warding.fault_survey) | 99.41% | 3 | 8/8 reachable under normal pressure |
| Resource / farming / logistics | 850 | 8/8 | none | 92.82% (logistics.relay_mesh) | 99.41% | 3 | 8/8 reachable under normal pressure |
| Social / support / coordination | 850 | 2/8 | social.collective_insight, social.trader_whisper, social.witness_imprint, support.mercy_link, support.rally_ledger, support.role_call | 71.18% (support.convoy_accord) | 100.00% | 2 | 8/8 reachable under normal pressure |
| Combat / tactical control | 850 | 2/6 | tactical.feint_window, tactical.reposition_snare, tactical.rush_damper, tactical.tempo_extract | 51.06% (sensing.battlefield_read) | 100.00% | 2 | 6/6 reachable under normal pressure |
| Crafting / engineering / automation | 850 | 1/5 | engineering.fault_isolate, engineering.pattern_forge, engineering.redstone_sympathy, engineering.sequence_splice | 100.00% (engineering.machine_rhythm) | 100.00% | 1 | 5/5 reachable under normal pressure |
| Stealth / trickery / disruption | 850 | 9/9 | none | 55.41% (stealth.social_smoke) | 99.18% | 4 | 9/9 reachable under normal pressure |

### Focus categories called out in the request

- **Sensing / information**: forced reachability was **3/9** with top template share **92.47%** and top-3 share **100.00%**; under normal pressure the same category reached **9/9** with top-3 share **72.85%**.
- **Traversal / mobility**: forced reachability was **2/8** with top template share **59.53%** and top-3 share **100.00%**; under normal pressure the same category reached **8/8** with top-3 share **86.92%**.
- **Survival / adaptation**: forced reachability was **3/11** with top template share **46.47%** and top-3 share **100.00%**; under normal pressure the same category reached **11/11** with top-3 share **61.54%**.
- **Resource / farming / logistics**: forced reachability was **8/8** with top template share **92.82%** and top-3 share **99.41%**; under normal pressure the same category reached **8/8** with top-3 share **70.63%**.
- **Social / support / coordination**: forced reachability was **2/8** with top template share **71.18%** and top-3 share **100.00%**; under normal pressure the same category reached **8/8** with top-3 share **78.59%**.
- **Stealth / trickery / disruption**: forced reachability was **9/9** with top template share **55.41%** and top-3 share **99.18%**; under normal pressure the same category reached **9/9** with top-3 share **70.00%**.

## SECTION 4: BASELINE COMPARISON

Direct baseline: the prior post-authenticity report in `docs/deep-authenticity-validation.md`. Deltas below compare the new forced post-auth applicability probe against that baseline.

| Category | Baseline reach | New reach | Reach delta | Baseline top-3 % | New top-3 % | Top-3 delta | Baseline zero-hit count | New zero-hit count | Zero-hit delta |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Sensing / information | 6/9 | 3/9 | -3 | 50.00% | 100.00% | +50.00 pp | 3 | 6 | +3 |
| Traversal / mobility | 5/8 | 2/8 | -3 | 60.00% | 100.00% | +40.00 pp | 3 | 6 | +3 |
| Survival / adaptation | 7/11 | 3/11 | -4 | 42.86% | 100.00% | +57.14 pp | 4 | 8 | +4 |
| Ritual / strange utility | 12/13 | 4/13 | -8 | 25.00% | 99.88% | +74.88 pp | 1 | 9 | +8 |
| Defense / warding | 8/8 | 8/8 | +0 | 37.50% | 99.41% | +61.91 pp | 0 | 0 | +0 |
| Resource / farming / logistics | 6/8 | 8/8 | +2 | 50.00% | 99.41% | +49.41 pp | 2 | 0 | -2 |
| Social / support / coordination | 6/8 | 2/8 | -4 | 50.00% | 100.00% | +50.00 pp | 2 | 6 | +4 |
| Combat / tactical control | 6/6 | 2/6 | -4 | 50.00% | 100.00% | +50.00 pp | 0 | 4 | +4 |
| Crafting / engineering / automation | 5/5 | 1/5 | -4 | 60.00% | 100.00% | +40.00 pp | 0 | 4 | +4 |
| Stealth / trickery / disruption | 8/9 | 9/9 | +1 | 37.50% | 99.18% | +61.68 pp | 1 | 0 | -1 |

### Requested regression check

- **Stealth / trickery / disruption**: reach delta +1, top-3 delta +61.68 pp, zero-hit delta -1. Overall improvement verdict: **not improved**.
- **Defense / warding**: reach delta +0, top-3 delta +61.91 pp, zero-hit delta +0. Overall improvement verdict: **not improved**.
- **Traversal / mobility**: reach delta -3, top-3 delta +40.00 pp, zero-hit delta +3. Overall improvement verdict: **not improved**.
- **Sensing / information**: reach delta -3, top-3 delta +50.00 pp, zero-hit delta +3. Overall improvement verdict: **not improved**.
- **Survival / adaptation**: reach delta -4, top-3 delta +57.14 pp, zero-hit delta +4. Overall improvement verdict: **not improved**.

## SECTION 5: UNDER-SAMPLED TEMPLATE RECOVERY

This section separates **normal-pressure recovery** from **forced-probe viability**, because the current applicability expansion clearly helps some under-sampled templates appear in ordinary mixed generation even while several categories still collapse under category-forced selection.

### Normal-pressure recovery results

- `stealth.trace_fold`: **125 normal-pressure hits** and **2 forced hits**. This template was previously absent in the cited pre-auth/post-expansion probe and is now visibly present under ordinary pressure.
- `stealth.threshold_jam`: **33 normal-pressure hits** and **1 forced hit**. It was also previously absent in the pre-auth/post-expansion probe and now appears under ordinary pressure.
- `stealth.hushwire`: **1 normal-pressure hit** and **1 forced hit**. It remains technically reachable, but still weak relative to the stronger stealth lanes.
- `consistency.structure_echo`: **17 normal-pressure hits** and **1 forced hit**. Structure-adjacent defense/inspect-style recovery exists under ordinary pressure, but forced dominance suppresses it.
- `consistency.path_thread`: **4 normal-pressure hits** and **1 forced hit**. It is no longer absent, but it is still not competitively stable in forced sampling.

### Likely source of recovery

- `stealth.trace_fold` most plausibly recovered via **near trigger-family applicability** plus **adjacent action-class applicability**. Its `ON_BLOCK_INSPECT` trigger is explicitly allowed to borrow from nearby inspect/structure families, and the bounded boost also rewards inspect-family templates when mobility/discipline memory pressure is present.
- `stealth.threshold_jam` and `stealth.hushwire` most plausibly recovered via **near trigger-family applicability** on the `ON_STRUCTURE_PROXIMITY` family, with some additional help from **adjacent action-class applicability** because structure-proximity templates can borrow from mobility/survival memory pressure.
- `consistency.structure_echo` and other structure/inspect-adjacent templates appear to benefit primarily from the same **adjacent trigger-family** path rather than from a generic universal boost.
- The recovery does **not** look like generic trigger universalization. It is concentrated in the exact inspect/structure/social adjacency families spelled out by the bounded applicability helpers, not across all triggers.
- However, the current data also shows a strong **ordinary distribution shift toward a few winners** (`stealth.social_smoke`, `support.convoy_accord`, `engineering.machine_rhythm`, `logistics.relay_mesh`, `precision.artifact_sympathy`), which overwhelms the recovered tail in the forced probe.

## SECTION 6: STABILITY / SAFETY CHECK

- **No new template exceeds 50% top-share:** **FAIL**. New forced top-share spikes include `engineering.machine_rhythm` at 100.00%, `precision.artifact_sympathy` at 92.47%, `logistics.relay_mesh` at 92.82%, `support.convoy_accord` at 71.18%, `ritual.altar_resonance` at 61.41%, `mobility.compass_stories` at 59.53%, `stealth.social_smoke` at 55.41%, and `sensing.battlefield_read` at 51.06%.
- **No category collapses into 1–2 templates:** **FAIL**. Traversal, sensing, survival, combat, social, and crafting all collapsed to 1–3 templates in the forced post-authenticity sample.
- **Novelty ordering remains intact:** **not positively re-confirmed in this report**. The dedicated novelty JUnit probe was started but did not finish inside the bounded validation window, so this pass cannot claim a fresh novelty-ordering certification.
- **No evidence of generic trigger universalization:** **PASS, with caveat**. The recovered templates line up with the explicit inspect/structure/social adjacency families in the bounded applicability logic rather than broad all-trigger activation.
- **No evidence that applicability tuning weakened authenticity:** **PASS** on the measured authenticity outputs. Rejection counts stayed at zero for stat-only and vanilla-equivalent outputs in the sampled post-authenticity set.
- **Generation stability:** **mixed**. The generator remained stable in the sense that all 14,750 sampled outputs were valid, but category-level distribution stability clearly regressed in forced viability because many categories became highly concentrated.

## SECTION 7: FINAL JUDGMENT

The bounded under-sampled applicability expansion shows a **real but incomplete recovery signal** under ordinary pressure: `stealth.trace_fold`, `stealth.threshold_jam`, and structure/inspect-adjacent defense templates now appear after authenticity filtering where earlier probes had them absent or weak. That is the strongest positive outcome of this pass.

However, the broader validation objective does **not** pass. The category-forced post-authenticity probe now exhibits severe concentration and multiple new dominance spikes across traversal, sensing, survival, social, ritual, crafting, combat, and logistics. The specific success criteria requiring material category recovery **without** new collapse or dominance spikes are therefore not met.

Overall verdict: authenticity hygiene remains intact, bounded applicability does help some under-sampled templates re-enter the normal-pressure pool, but the current distribution is not safe enough to certify as a successful post-auth applicability restoration pass.

POST_AUTH_APPLICABILITY_RESULT: FAILED
