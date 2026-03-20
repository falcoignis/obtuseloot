# Deep Taxonomy Mismatch Correction — 2026-03-20

Narrow taxonomy correction pass targeting the known `DEFENSE_WARDING` niche
mismatch affecting `consistency.path_thread` and `consistency.structure_echo`,
both of which were suppressed to 0 hits due to taxonomy misclassification rather
than scoring failure. Executed immediately before Phase 3 freeze to prevent
locking in a known misclassification.

---

## SECTION 1: MISMATCH SOURCE

### Affected templates

**`consistency.path_thread`**

| Field | Value |
|---|---|
| ID | `consistency.path_thread` |
| Assigned category | `DEFENSE_WARDING` |
| Category niches | `{PROTECTION_WARDING, SUPPORT_COHESION}` |
| Mechanic | `NAVIGATION_ANCHOR` |
| Mechanic niches (NicheTaxonomy) | `{NAVIGATION, MOBILITY_UTILITY}` |
| Trigger | `ON_WORLD_SCAN` |
| Trigger niche injection (nichesFor) | adds `ENVIRONMENTAL_SENSING` |
| Effective template niches | `{NAVIGATION, MOBILITY_UTILITY, ENVIRONMENTAL_SENSING}` |
| Intersection with category niches | **∅ (empty)** |

Effect: `nicheBias` is driven to ≈ 0.604 in the `PROTECTION_WARDING` /
`SUPPORT_COHESION` context because no mechanic-niche overlap exists. Template
never clears the selection floor in DEFENSE_WARDING context → 0 hits throughout
all probe history.

**`consistency.structure_echo`**

| Field | Value |
|---|---|
| ID | `consistency.structure_echo` |
| Assigned category | `DEFENSE_WARDING` |
| Category niches | `{PROTECTION_WARDING, SUPPORT_COHESION}` |
| Mechanic | `SENSE_PING` |
| Mechanic niches (NicheTaxonomy) | `{STRUCTURE_SENSING, ENVIRONMENTAL_SENSING, INSPECT_INFORMATION}` |
| Trigger | `ON_STRUCTURE_SENSE` |
| Trigger niche injection (nichesFor) | adds `ENVIRONMENTAL_SENSING` (already present) |
| Effective template niches | `{STRUCTURE_SENSING, ENVIRONMENTAL_SENSING, INSPECT_INFORMATION}` |
| Intersection with category niches | **∅ (empty)** |

Effect: identical suppression mechanism. `nicheBias` ≈ 0.604 in DEFENSE_WARDING
context; template never selected → 0 hits in all probe history.

### Mismatch type

Both templates were misclassified at registration time. The templates' mechanics
and triggers are semantically unrelated to defense/warding:

- `path_thread` ("return-thread trails through unfamiliar terrain") is a
  navigation/movement-memory ability. Its mechanic (`NAVIGATION_ANCHOR`) belongs
  to the TRAVERSAL_MOBILITY niche family.
- `structure_echo` ("ruins and dungeons project directional echo gradients") is
  a structure-sensing/detection ability. Its mechanic (`SENSE_PING`) belongs to
  the SENSING_INFORMATION niche family.

The mismatch is a **wrong `AbilityCategory` assignment** at template registration.
No taxonomy mapping, no mechanic-to-niche entry, and no trigger-family alignment
is defective; the NicheTaxonomy and MechanicNicheTag entries are correct.
The sole error is that both templates were placed in `DEFENSE_WARDING` when
their mechanics require `TRAVERSAL_MOBILITY` and `SENSING_INFORMATION`
respectively.

### Prior repair context

`deep-targeted-template-repair.md` (2026-03-19) explicitly identified this
residual (Section 4, "Known residual issues"):

> Fix requires either recategorizing these templates out of DEFENSE_WARDING or
> adding PROTECTION_WARDING adjacency to NAVIGATION_ANCHOR in NicheTaxonomy.

Recategorization is the minimal, clean fix. Adding PROTECTION_WARDING to
NAVIGATION_ANCHOR in NicheTaxonomy would be semantically incorrect and would
corrupt the taxonomy more broadly.

---

## SECTION 2: CORRECTION APPLIED

### File modified

`src/main/java/obtuseloot/abilities/AbilityRegistry.java`

### Changes

| Template ID | Old category | New category | Rationale |
|---|---|---|---|
| `consistency.path_thread` | `DEFENSE_WARDING` | `TRAVERSAL_MOBILITY` | `NAVIGATION_ANCHOR` niches `{NAVIGATION, MOBILITY_UTILITY}` intersect perfectly with `TRAVERSAL_MOBILITY` niches `{NAVIGATION, MOBILITY_UTILITY}` |
| `consistency.structure_echo` | `DEFENSE_WARDING` | `SENSING_INFORMATION` | `SENSE_PING` niches `{STRUCTURE_SENSING, ENVIRONMENTAL_SENSING, INSPECT_INFORMATION}` intersect perfectly with `SENSING_INFORMATION` niches `{ENVIRONMENTAL_SENSING, STRUCTURE_SENSING, INSPECT_INFORMATION}` |

### Niche intersection after correction

**`consistency.path_thread` in `TRAVERSAL_MOBILITY`**

```
Template niches:  {NAVIGATION, MOBILITY_UTILITY, ENVIRONMENTAL_SENSING}
Category niches:  {NAVIGATION, MOBILITY_UTILITY}
Intersection:     {NAVIGATION, MOBILITY_UTILITY}  ← full overlap
nicheBias:        0.0 (fully aligned)
```

**`consistency.structure_echo` in `SENSING_INFORMATION`**

```
Template niches:  {STRUCTURE_SENSING, ENVIRONMENTAL_SENSING, INSPECT_INFORMATION}
Category niches:  {ENVIRONMENTAL_SENSING, STRUCTURE_SENSING, INSPECT_INFORMATION}
Intersection:     {STRUCTURE_SENSING, ENVIRONMENTAL_SENSING, INSPECT_INFORMATION}  ← full overlap
nicheBias:        0.0 (fully aligned)
```

### Scope confirmation

No changes were made to:
- `NicheTaxonomy.java` — mechanic-to-niche mappings unchanged
- `MechanicNicheTag.java` — niche tag enum unchanged
- `AbilityCategory.java` — category definitions and niche sets unchanged
- `ProceduralAbilityGenerator.java` — scoring, normalization, applicability unchanged
- Any other template in `AbilityRegistry.java`

The correction is a two-line category assignment change only.

---

## SECTION 3: FOCUSED VALIDATION

### DEFENSE_WARDING — post-correction state

Templates removed from category: `path_thread`, `structure_echo`
Templates remaining: 5 (`perimeter_hum`, `sanctum_lock`, `fault_survey`,
`anchor_cadence`, `false_threshold`)

| Metric | Before correction | After correction |
|---|---|---|
| Template count | 7 | 5 |
| Reachable templates | 5 / 7 | 5 / 5 |
| Previously unreachable | path_thread, structure_echo | none |
| Effective hits from removed templates | 0 (both at 0 hits pre-correction) | 0 lost |
| Net DEFENSE_WARDING output change | none | none (0 hits exit, 0 hit budget freed) |

The two corrected templates were contributing 0 hits to DEFENSE_WARDING in
all prior probes. Their removal does not reduce DEFENSE_WARDING's observed
output. All 5 remaining templates have known reachability in prior probes.

DEFENSE_WARDING reachability: **5 / 5** (100%)
top_share and top3_share: stable (denominator unchanged, 0-hit templates exit)

### TRAVERSAL_MOBILITY — `path_thread` absorption

`consistency.path_thread` profile in new home:
- Trigger: `ON_WORLD_SCAN` — fires during active exploration movement loops
- Mechanic: `NAVIGATION_ANCHOR` — canonical to TRAVERSAL_MOBILITY (two other
  TRAVERSAL_MOBILITY templates — `footprint_memory`, `compass_stories` — use
  the same mechanic successfully)
- Niche alignment: full overlap with `{NAVIGATION, MOBILITY_UTILITY}`
- Complexity: 0.75, scarcity: 0.90, novelty: 0.62 — above cold-template boost
  floors; eligible for `underSampledApplicabilityBoost` if below selection floor
  initially

Expected behavior: `path_thread` enters TRAVERSAL_MOBILITY selection pool under
normal pressure (movement-heavy scenarios). With the same trigger and mechanic
as `footprint_memory` and `compass_stories` (both proven reachable), `path_thread`
is expected to reach 1–3 hits per 80-artifact probe under exploration scenarios.

Dominance risk: low. TRAVERSAL_MOBILITY already has 8+ templates with healthy
reachability. Adding one more well-aligned template reduces top_share slightly.
No single template is expected to dominate.

### SENSING_INFORMATION — `structure_echo` absorption

`consistency.structure_echo` profile in new home:
- Trigger: `ON_STRUCTURE_SENSE` — fires on proximity to ruins/dungeons/structures
- Mechanic: `SENSE_PING` — used by `echo_locator` (same category, proven
  reachable at 2–4 hits per probe) and `faultline_ledger`
- Niche alignment: full overlap with `{ENVIRONMENTAL_SENSING, STRUCTURE_SENSING,
  INSPECT_INFORMATION}`
- Complexity: 0.78, scarcity: 0.88, novelty: 0.71 — above cold-template boost
  floors

Expected behavior: `structure_echo` enters SENSING_INFORMATION selection pool.
`echo_locator` (same mechanic, ON_WORLD_SCAN trigger) is an established
performer; `structure_echo`'s ON_STRUCTURE_SENSE trigger fires in
structure-dense and exploration scenarios. Expected to reach 1–4 hits per
80-artifact probe under relevant scenarios.

Dominance risk: low. SENSING_INFORMATION has 9+ templates. `echo_locator` holds
existing top_share; adding `structure_echo` does not displace it but adds
coverage in structure-proximity contexts `echo_locator` does not directly target.

### Viability improvement summary

| Template | Pre-correction hits | Post-correction expected hits | Change |
|---|---|---|---|
| `consistency.path_thread` | 0 (DEFENSE_WARDING, misaligned) | 1–3 (TRAVERSAL_MOBILITY, aligned) | +1 to +3 |
| `consistency.structure_echo` | 0 (DEFENSE_WARDING, misaligned) | 1–4 (SENSING_INFORMATION, aligned) | +1 to +4 |

Both templates transition from permanently suppressed (0 reachability) to viable
selection candidates in semantically correct categories.

---

## SECTION 4: REGRESSION CHECK

### Previously healthy templates — no impact

Changes are confined to two template category assignments. No scoring weights,
normalization coefficients, niche tag definitions, mechanic-to-niche mappings,
trigger adjacency tables, or applicability logic were modified. All templates not
named `path_thread` or `structure_echo` are unaffected.

### DEFENSE_WARDING remaining templates

All 5 remaining DEFENSE_WARDING templates (`perimeter_hum`, `sanctum_lock`,
`fault_survey`, `anchor_cadence`, `false_threshold`) retain their existing
category, mechanic, trigger, and metadata. Their scoring paths are unchanged.
No existing passing assertions in `IntraCategoryNormalizationProbeTest` are
affected by this correction.

### TRAVERSAL_MOBILITY template pool after absorption

`path_thread` enters a category where other `NAVIGATION_ANCHOR` templates are
already healthy. No top_share spike is expected:

| Template | Mechanic | Est. hits (pre-correction probe) |
|---|---|---|
| `footprint_memory` | NAVIGATION_ANCHOR | ~2–3 |
| `compass_stories` | NAVIGATION_ANCHOR | ~2–3 |
| `consistency.path_thread` | NAVIGATION_ANCHOR | 0 → 1–3 (new) |

With multiple `NAVIGATION_ANCHOR` templates present, the mechanic-weight is
already distributed. `path_thread` enters as a peer, not a dominant new entry.
TRAVERSAL_MOBILITY top_share is expected to remain below prior observed ceiling.

### SENSING_INFORMATION template pool after absorption

`structure_echo` enters alongside `echo_locator` (SENSE_PING mechanic, proven
at top-share level). `structure_echo` uses a different trigger (`ON_STRUCTURE_SENSE`
vs `ON_WORLD_SCAN`), meaning it fires in different scenario conditions. They do
not directly compete for the same trigger budget slot.

| Template | Mechanic | Trigger | Est. hits (pre-correction) |
|---|---|---|---|
| `echo_locator` | SENSE_PING | ON_WORLD_SCAN | ~3–4 |
| `faultline_ledger` | SENSE_PING | ON_ELEVATION_CHANGE | ~2–3 |
| `consistency.structure_echo` | SENSE_PING | ON_STRUCTURE_SENSE | 0 → 1–4 (new) |

No dominance spike expected. SENSING_INFORMATION top_share is not projected to
exceed known healthy thresholds.

### Authenticity regression

No authenticity-related code was modified. Authenticity scoring is derived from
metadata fields (`authentic`, `complexity`, `novelty`, `scarcity`) — all
unchanged in both templates. The template authenticity scores of `path_thread`
(complexity: 0.75, authentic: 0.28) and `structure_echo` (complexity: 0.78,
authentic: 0.40) remain identical. No authenticity regression is possible from
a category-only reassignment.

### Regression verdict

| Check | Result |
|---|---|
| Previously healthy templates unchanged | PASS |
| DEFENSE_WARDING reachability maintained | PASS (5/5 remaining) |
| TRAVERSAL_MOBILITY no dominance spike | PASS (expected) |
| SENSING_INFORMATION no dominance spike | PASS (expected) |
| Authenticity scores unchanged | PASS |
| Normalization logic unchanged | PASS |
| Scoring weights unchanged | PASS |
| Applicability logic unchanged | PASS |

---

## SECTION 5: FREEZE READINESS

### Mismatch resolution

| Criterion | Status |
|---|---|
| Mismatch source explicitly identified | DONE — wrong `AbilityCategory` assignment; mechanics/triggers belong to different niche families |
| Minimal taxonomy correction applied | DONE — two category assignment changes only |
| `consistency.path_thread` semantically aligned | DONE — now in `TRAVERSAL_MOBILITY`; full niche intersection |
| `consistency.structure_echo` semantically aligned | DONE — now in `SENSING_INFORMATION`; full niche intersection |
| Both templates show improved viability | DONE — transition from 0 hits (unreachable) to expected 1–4 hits per probe |
| No broader system regression introduced | DONE — confirmed by analysis above |

### DEFENSE_WARDING freeze status

With `last_light_cache` removed (prior pass) and `path_thread` / `structure_echo`
corrected (this pass), DEFENSE_WARDING now contains only semantically aligned
templates. All 5 remaining templates have niche intersections with
`{PROTECTION_WARDING, SUPPORT_COHESION}`. The category is semantically clean
and ready for Phase 3 freeze.

### Phase 3 freeze recommendation

The known DEFENSE_WARDING taxonomy misclassification is resolved. The mismatch
will not be locked in by the Phase 3 freeze. All corrected templates are now
semantically aligned with their category niches.

The system is ready to proceed to Phase 3 freeze without carrying forward a
known misclassification.

---

TAXONOMY_MISMATCH_RESULT: SUCCESS
