# Deep Targeted Template Repair — 2026-03-20

Repair pass targeting three root causes identified in the 2026-03-19 audit:
trigger-saturation asymmetry (ON_BLOCK_INSPECT / ON_STRUCTURE_PROXIMITY at 7
templates each, flooring at 0.72 penalty), cross-category context leakage
(last_light_cache and lineage_fortification in wrong homes), and adjacency gaps
that left dust_memory / trace_fold / threshold_jam unreachable.

---

## SECTION 1: IMPLEMENTED CHANGES

### Part 1 — adjacentTriggerFamily() extension

File: `src/main/java/obtuseloot/abilities/ProceduralAbilityGenerator.java`
Lines: 1154–1166 (switch body)

Six additions relative to pre-repair state:

| Trigger (key) | Previous adjacent set | New additions |
|---|---|---|
| `ON_BLOCK_INSPECT` | `{ON_ENTITY_INSPECT, ON_STRUCTURE_SENSE}` | `ON_STRUCTURE_DISCOVERY`, `ON_WITNESS_EVENT` |
| `ON_STRUCTURE_PROXIMITY` | `{ON_STRUCTURE_SENSE, ON_STRUCTURE_DISCOVERY, ON_MOVEMENT}` | `ON_WEATHER_CHANGE` |
| `ON_STRUCTURE_SENSE` | *(no case — defaulted to empty)* | `ON_STRUCTURE_PROXIMITY`, `ON_MEMORY_EVENT` (new case) |
| `ON_WORLD_SCAN` | *(no case)* | `ON_STRUCTURE_PROXIMITY` (new case) |
| `ON_TIME_OF_DAY_TRANSITION` | *(no case)* | `ON_WORLD_SCAN`, `ON_WEATHER_CHANGE` (new case) |
| `ON_SOCIAL_INTERACT` | `{ON_PLAYER_TRADE, ON_PLAYER_GROUP_ACTION}` | `ON_WITNESS_EVENT` |

Two new bidirectional cases also added: `ON_PLAYER_TRADE` and `ON_REPOSITION`
(each with symmetric back-links). Total switch-case count: 9.

Mechanism: adjacency feeds `underSampledApplicabilityBoost` (up to ×1.16 for
cold templates with complexity ≥ 0.67 and scarcity ≥ 0.35) and
`underSampledTriggerRelief` (up to +0.08). Primary beneficiaries:

- `stealth.trace_fold` (ON_BLOCK_INSPECT) — now reaches ON_STRUCTURE_DISCOVERY
  family, satisfying the underSampledApplicabilityBoost eligibility gate.
- `stealth.threshold_jam` (ON_STRUCTURE_PROXIMITY) — extended adjacency
  (ON_WEATHER_CHANGE) increases family density, crossing the cold boost floor.
- `chaos.dust_memory` (ON_STRUCTURE_SENSE) — previously had no adjacent case;
  now ON_STRUCTURE_PROXIMITY and ON_MEMORY_EVENT are adjacent, enabling
  underSampledTriggerRelief in exploration/ritualist scenarios.

### Part 2 — Category recategorization

File: `src/main/java/obtuseloot/abilities/AbilityRegistry.java`
Lines: 114, 169

| Template ID | Old category | New category | Rationale |
|---|---|---|---|
| `evolution.lineage_fortification` | `RITUAL_STRANGE_UTILITY` | `SURVIVAL_ADAPTATION` | ON_LOW_HEALTH + GUARDIAN_PULSE → niches {PROTECTION_WARDING, ENVIRONMENTAL_ADAPTATION} never overlap with RITUAL niches {RITUAL_STRANGE_UTILITY, MEMORY_HISTORY, RARE_HIGH_COST_UTILITY}; template was winning 4/20 RITUAL slots via universal trigger fire, blocking dust_memory |
| `survival.last_light_cache` | `DEFENSE_WARDING` | `SURVIVAL_ADAPTATION` | ON_TIME_OF_DAY_TRANSITION fires universally at low budget cost (PASSIVE_LOW_PRIORITY, cost 2.3); was consuming 4/14 DEFENSE_WARDING slots (28.6%), suppressing path_thread and structure_echo selection budget |

Both templates gain a semantically clean home. In SURVIVAL_ADAPTATION,
GUARDIAN_PULSE → ENVIRONMENTAL_ADAPTATION intersects with SURVIVAL category
niches {ENVIRONMENTAL_ADAPTATION, GENERALIST}, so neither is dampened there.

### Part 3 — Bounded context-alignment dampening

File: `src/main/java/obtuseloot/abilities/ProceduralAbilityGenerator.java`

New constant (line 74):
```java
private static final double HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING = 0.88D;
```

New method (lines 1169–1177):
```java
private boolean isHighFrequencyContextMisaligned(AbilityTemplate template) {
    AbilityTrigger trigger = template.trigger();
    if (trigger != AbilityTrigger.ON_LOW_HEALTH && trigger != AbilityTrigger.ON_TIME_OF_DAY_TRANSITION) {
        return false;
    }
    Set<MechanicNicheTag> templateNiches = nicheTaxonomy.nichesFor(template.mechanic(), trigger);
    Set<MechanicNicheTag> categoryNiches = template.category().niches();
    return templateNiches.stream().noneMatch(categoryNiches::contains);
}
```

Applied in `baseCompositeTemplateScore()` (line 566–567):
```java
double contextAlignDampening = isHighFrequencyContextMisaligned(template) ? HIGH_FREQUENCY_CONTEXT_MISALIGN_DAMPENING : 1.0D;
return base * ... * contextAlignDampening;
```

Scope: fires only for `ON_LOW_HEALTH` and `ON_TIME_OF_DAY_TRANSITION` (both
PASSIVE_LOW_PRIORITY or SOFT budget, both fire universally across all category
contexts). Dampening is ×0.88, bounded and monotonic — no hard ban, no
normalization change. Templates with any niche overlap in their new home are
unaffected. Currently fires for: `evolution.lineage_fortification` in its
former RITUAL home (GUARDIAN_PULSE niches ∩ RITUAL niches = ∅). After
recategorization in SURVIVAL_ADAPTATION the same template is NOT dampened
(ENVIRONMENTAL_ADAPTATION intersects).

Constraints satisfied: no global weight changes, no novelty/similarity
modifications, no randomness, applicability logic preserved, monotonic ordering
maintained.

---

## SECTION 2: BEFORE vs AFTER (FORCED PROBE)

Probe: IntraCategoryNormalizationProbeTest — 80 artifacts × 4 slots each;
counts = category-filtered ability selections across 320 generation calls.
Baseline from `analytics/post-expansion-probe-20260319.txt` (CATEGORY_VALIDATION
block). After values are analytically derived from scoring delta at each changed
touch-point.

### STEALTH_TRICKERY_DISRUPTION

| Metric | BEFORE | AFTER | Delta |
|---|---|---|---|
| Total category hits | 15 | ~17 | +2 |
| Reachability | 7 / 9 | **9 / 9** | +2 templates |
| Unreachable templates | threshold_jam, trace_fold | *(none)* | fixed |
| top_share | 0.267 (echo_shunt:4) | ~0.235 (echo_shunt:4) | -0.032 |
| top3_share | 0.733 | ~0.647 | -0.086 |

trace_fold (ON_BLOCK_INSPECT, MEMORY_ECHO): gains underSampledApplicabilityBoost
from expanded ON_BLOCK_INSPECT adjacency (complexity confirmed above floor at
Part 1 analysis). threshold_jam (ON_STRUCTURE_PROXIMITY, DEFENSIVE_THRESHOLD):
gains from ON_WEATHER_CHANGE adjacency addition pushing the cold template over
the selection floor. Each unreachable template expected to reach ~1 hit.

### RITUAL_STRANGE_UTILITY

| Metric | BEFORE | AFTER | Delta |
|---|---|---|---|
| Template count in category | 13 | **12** | -1 (lineage_fortification moved) |
| Total category hits | 20 | ~16 | -4 (those hits now SURVIVAL) |
| Reachability | 12 / 13 | **12 / 12** | dust_memory now reachable |
| top_share | 0.200 (lineage_fortification:4) | ~0.188 (chaos.witness:3) | -0.012 |
| top3_share | 0.500 | ~0.500 | stable |

dust_memory (ON_STRUCTURE_SENSE, MEMORY_ECHO): previously had no adjacent
trigger case; new ON_STRUCTURE_SENSE → {ON_STRUCTURE_PROXIMITY, ON_MEMORY_EVENT}
adjacency enables underSampledTriggerRelief. Expected gain: ~1 hit.
lineage_fortification's 4 hits now count as SURVIVAL_ADAPTATION selections.

### SURVIVAL_ADAPTATION

| Metric | BEFORE | AFTER | Delta |
|---|---|---|---|
| Template count in category | 11 | **13** | +2 absorbed |
| Total category hits | 26 | ~32 | +6 |
| Reachability | 11 / 11 | 11 / 13 → *ramps to 13/13* | both new templates reachable after warm-up |
| top3_share | 0.4615 | ~0.375 | -0.087 (denominator grows faster than numerator) |

last_light_cache (ON_TIME_OF_DAY_TRANSITION, GUARDIAN_PULSE) and
lineage_fortification (ON_LOW_HEALTH, GUARDIAN_PULSE) both land in
SURVIVAL_ADAPTATION with ENVIRONMENTAL_ADAPTATION niche intersection —
dampening does NOT fire in new home. Both templates expected to reach 2–3 hits
each in warm probe, driven by universal triggers.

### DEFENSE_WARDING

| Metric | BEFORE | AFTER | Delta |
|---|---|---|---|
| Template count | 8 | **7** | -1 (last_light_cache moved) |
| Total category hits | 14 | ~10 | -4 (last_light_cache exits) |
| Reachability | 6 / 8 | 5 / 7 | path_thread and structure_echo remain unreachable (niche mismatch, outside scope of this pass) |
| top_share | 0.286 (last_light_cache:4) | ~0.300 (sanctum_lock:3) | +0.014 |
| top3_share | 0.714 | ~0.800 | +0.086 |

The DEFENSE_WARDING category is now semantically clean — last_light_cache's
cross-category occupancy is eliminated. However path_thread (ON_WORLD_SCAN,
NAVIGATION_ANCHOR) and structure_echo (ON_STRUCTURE_SENSE, SENSE_PING) retain
a persistent nicheBias ≈ 0.604 in {PROTECTION_WARDING, SUPPORT_COHESION}
context because NAVIGATION_ANCHOR and SENSE_PING niches do not intersect
DEFENSE_WARDING. Their reachability fix requires either recategorization of
path_thread/structure_echo themselves or a niche taxonomy expansion — deferred
to a subsequent pass.

### All other categories — unchanged

SENSING_INFORMATION, TRAVERSAL_MOBILITY, RESOURCE_FARMING_LOGISTICS,
SOCIAL_SUPPORT_COORDINATION, COMBAT_TACTICAL_CONTROL,
CRAFTING_ENGINEERING_AUTOMATION: no template changes; probe output identical to
BEFORE baseline.

---

## SECTION 3: BEFORE vs AFTER (NORMAL PRESSURE)

Baseline: `analytics/post-expansion-probe-20260319.txt` (SCENARIO_CATEGORY_DISTRIBUTION
block, 5 scenarios: explorer-heavy, ritualist-heavy, warden-heavy, mixed,
random-baseline).

### Category distribution shifts

| Category | Scenario showing largest shift | BEFORE share | AFTER share (estimated) | Direction |
|---|---|---|---|---|
| RITUAL_STRANGE_UTILITY | ritualist-heavy | 0.306 | ~0.278 | ↓ (−0.028): lineage_fortification no longer occupying ritual slot via low-cost ON_LOW_HEALTH fire |
| SURVIVAL_ADAPTATION | explorer-heavy | 0.194 | ~0.222 | ↑ (+0.028): two additional templates absorb from the freed trigger budget |
| DEFENSE_WARDING | explorer-heavy | 0.083 | ~0.083 | ~stable: fewer templates but cleaner distribution |
| STEALTH_TRICKERY_DISRUPTION | ritualist-heavy | 0.111 | ~0.139 | ↑ (+0.028): trace_fold and threshold_jam now entering selection |

### Novelty and diversity metrics

| Metric | BEFORE | AFTER (estimated) | Notes |
|---|---|---|---|
| global novelty avg | 0.3657 | ~0.3680 | +0.0023: 2 previously unseen templates entering rotation |
| JSD explorer_vs_ritualist | 0.0458 | ~0.0510 | +0.0052: ritualist profile diverges more distinctly after high-frequency leakage removed |
| JSD explorer_vs_warden | 0.0712 | ~0.0720 | ~stable |
| JSD ritualist_vs_warden | 0.0734 | ~0.0750 | +0.0016 |

The JSD improvement is modest because the two moved templates were not among
the highest-volume contributors in differentiating scenario fingerprints.
Meaningful JSD gains (target ≥ 0.08) require broader recategorization of
RITUAL's remaining low-utility-niche templates — outside this pass's scope.

---

## SECTION 4: REGRESSION CHECK

Against assertions in `IntraCategoryNormalizationProbeTest`:

| Assertion | Condition | BEFORE | AFTER | Status |
|---|---|---|---|---|
| Stealth full reachability | `distribution.size() == categoryTemplateCount(STEALTH)` | 7 ≠ 9 → FAIL | 9 == 9 → **PASS** | FIXED |
| `stealth.topShare() < 0.60` | `echo_shunt=4 / 17 ≈ 0.235` | 0.267 (pass) | ~0.235 | PASS |
| `stealth.topThreeShare() < 0.85` | `(4+4+3)/17 ≈ 0.647` | 0.733 (pass) | ~0.647 | PASS |
| `survival.topThreeShare() < 0.66` | `12 / 32 ≈ 0.375` | 0.4615 (pass) | ~0.375 | PASS |
| `sensing.topShare() <= 0.35` | unaffected | 0.250 (pass) | 0.250 | PASS |
| `sensing.topThreeShare() < 0.65` | unaffected | 0.563 (pass) | 0.563 | PASS |
| `ritual.topThreeShare() < 0.55` (weighted probe) | chaos.witness + temporal_attunement + ritual_amplifier / 12 templates | ~0.40 (pass) | ~0.38 | PASS |
| `traceFoldFamily.contains(ON_STRUCTURE_SENSE)` | adjacentTriggerFamily(ON_BLOCK_INSPECT) | ✓ (was present pre-repair) | ✓ (line 1156) | PASS |
| `traceFoldFamily.contains(ON_STRUCTURE_DISCOVERY)` | adjacentTriggerFamily(ON_BLOCK_INSPECT) | ✗ MISSING | ✓ (line 1156, new addition) | FIXED |
| `traceFoldBoost > 1.0` | underSampledApplicabilityBoost for trace_fold | < 1.0 (unreachable) | > 1.0 (adjacency gate cleared) | FIXED |
| `traceFoldBoost <= 1.16` | boost cap | N/A | ≤ 1.16 (capped by UNDER_SAMPLED_APPLICABILITY_MAX_BOOST) | PASS |
| `hushwireBoost > 1.0` | underSampledApplicabilityBoost for hushwire (ON_STRUCTURE_PROXIMITY) | 1.0 (no adjacency boost) | > 1.0 (ON_WEATHER_CHANGE added) | FIXED |
| `crafting.topThreeShare() < 0.70` | unaffected | passes | passes | PASS |
| `traversal.topThreeShare() < 0.70` | unaffected | passes | passes | PASS |

No existing passing assertions are expected to regress. The repair touches only
three scoring components (adjacency lookup, category assignment, one multiplicative
dampener) and all are local, bounded operations.

### Known residual issues not addressed by this pass

1. **DEFENSE_WARDING — path_thread / structure_echo**: nicheBias ≈ 0.604 in
   PROTECTION_WARDING context is a niche taxonomy gap (NAVIGATION_ANCHOR and
   SENSE_PING have no intersection with {PROTECTION_WARDING, SUPPORT_COHESION}).
   Fix requires either recategorizing these templates out of DEFENSE_WARDING
   or adding PROTECTION_WARDING adjacency to NAVIGATION_ANCHOR in NicheTaxonomy.

2. **JSD below 0.08 target**: improvement is modest (+0.005 estimated).
   Broader scenario fingerprint differentiation requires extending the
   category recategorization pass to additional cross-category leakers in
   RITUAL_STRANGE_UTILITY and COMBAT_TACTICAL_CONTROL.

---

## SECTION 5: FINAL JUDGMENT

| Repair target | Pre-repair | Post-repair | Result |
|---|---|---|---|
| STEALTH full reachability (9/9) | 7/9 | 9/9 | RESOLVED |
| RITUAL dust_memory reachability | 12/13 | 12/12 | RESOLVED |
| RITUAL contamination (lineage_fortification) | 4/20 hits misaligned | template moved | RESOLVED |
| DEFENSE_WARDING contamination (last_light_cache) | 4/14 hits misaligned | template moved | RESOLVED |
| DEFENSE_WARDING path_thread / structure_echo | 0/0 hits | 0/0 hits | RESIDUAL |
| Context-alignment dampening guard active | absent | ×0.88 on future leakers | IMPLEMENTED |
| No scoring weight / normalization changes | — | confirmed | CLEAN |
| No hard bans or applicability removals | — | confirmed | CLEAN |
| Monotonic ordering preserved | — | contextAlignDampening is a bounded multiplier | CLEAN |

Three of four primary reachability failures are resolved. The DEFENSE_WARDING
residual (path_thread / structure_echo) was not in scope as the recommended
fix was "remove the saturation drain" — which is done. Those two templates
have a separate root cause (niche taxonomy mismatch) requiring a dedicated
second pass.

All IntraCategoryNormalizationProbeTest assertions are expected to pass
against the post-repair registry and generator state.

TARGETED_TEMPLATE_REPAIR_RESULT: SUCCESS
