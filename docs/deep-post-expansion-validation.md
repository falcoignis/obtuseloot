# Deep Post-Expansion Validation

This pass was measurement-only. No generator weights, template lists, or systems were changed during validation. Measurements came from a fresh live-generator probe run captured in `analytics/post-expansion-probe-20260319.txt`, with category context grounded against the current template registry in `AbilityRegistry`. The six categories expanded in the low-template pass remain: crafting (`5` templates), combat (`6`), defense (`8`), resource (`8`), social (`8`), and stealth (`9`).【F:analytics/post-expansion-probe-20260319.txt†L7-L17】【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L173-L284】【F:docs/deep-low-template-category-expansion.md†L31-L38】

## SECTION 1: GENERATION OVERVIEW

Probe matrix used:
- `explorer-heavy`
- `ritualist-heavy`
- `warden-heavy`
- `mixed`
- `random-baseline`

Each scenario was run through the live `ProceduralAbilityGenerator` under normal selection pressure and then aggregated by category and template. The resulting category shares confirm that the expanded categories are still being sampled in ordinary generation rather than existing only as edge-case registry entries. Stealth appears in all five scenarios at `8.3%`, `11.1%`, `2.8%`, `11.1%`, and `8.3%` respectively; crafting appears at `5.6%`, `11.1%`, `8.3%`, `11.1%`, and `19.4%`; combat appears at `5.6%`, `2.8%`, `11.1%`, `0%`, and `5.6%`.【F:analytics/post-expansion-probe-20260319.txt†L1-L6】

Overall interpretation:
- Expanded categories are active under normal generation.
- Sampling is real, but not uniformly strong.
- Some categories still show incomplete template reachability in a normal-pressure pass, which blocks a full success judgment.

## SECTION 2: CATEGORY DISTRIBUTION

### Scenario-level distribution snapshot

| Scenario | Leading categories | Expanded-category readout |
|---|---|---|
| explorer-heavy | Survival `19.4%`, Traversal `16.7%`, Sensing `13.9%` | Stealth `8.3%`, Crafting `5.6%`, Combat `5.6%` |
| ritualist-heavy | Ritual `30.6%`, Social `13.9%` | Stealth `11.1%`, Crafting `11.1%`, Combat `2.8%` |
| warden-heavy | Traversal `22.2%`, Survival `16.7%` | Combat `11.1%`, Crafting `8.3%`, Stealth `2.8%` |
| mixed | Resource `19.4%`, Social `16.7%`, Survival `16.7%` | Stealth `11.1%`, Crafting `11.1%`, Combat `0%` |
| random-baseline | Resource `19.4%`, Crafting `19.4%`, Survival `13.9%` | Stealth `8.3%`, Combat `5.6%` |

These results show the post-expansion categories are not dead lanes. Crafting, stealth, and combat all appear across the matrix, with combat being the weakest in `mixed` specifically and stealth being weakest in `warden-heavy`.【F:analytics/post-expansion-probe-20260319.txt†L1-L6】【F:analytics/post-expansion-probe-20260319.txt†L18-L21】

### Focus-category summary

| Category | Total hits | Reachability | Top template share | Top-3 share | Read |
|---|---:|---:|---:|---:|---|
| Stealth / trickery / disruption | 15 | 7 / 9 | 26.7% | 73.3% | Sampled in every scenario, but still incomplete and tail-thin. |
| Crafting / engineering / automation | 20 | 5 / 5 | 35.0% | 70.0% | Healthy breadth and full reachability; moderately top-heavy but not collapsed. |
| Combat / tactical control | 9 | 6 / 6 | 33.3% | 66.7% | Fully reachable, but low-volume and still small-pool. |

The targeted categories are visibly active, but stealth remains the clear weak point of the expansion from a validation standpoint. Crafting is the strongest of the three focus lanes. Combat is viable, though still lightly sampled. 【F:analytics/post-expansion-probe-20260319.txt†L15-L21】

## SECTION 3: TEMPLATE REACHABILITY

### Category-by-category reachability

| Category | Total hits | Reachability | Notes |
|---|---:|---:|---|
| Sensing / information | 16 | 9 / 9 | Full reachability. |
| Traversal / mobility | 17 | 8 / 8 | Full reachability. |
| Survival / adaptation | 26 | 11 / 11 | Full reachability. |
| Ritual / strange utility | 20 | 12 / 13 | `chaos.dust_memory` missed this pass. |
| Defense / warding | 14 | 6 / 8 | `consistency.path_thread` and `consistency.structure_echo` missed. |
| Resource / farming / logistics | 25 | 8 / 8 | Full reachability. |
| Social / support / coordination | 18 | 8 / 8 | Full reachability. |
| Combat / tactical control | 9 | 6 / 6 | Full reachability. |
| Crafting / engineering / automation | 20 | 5 / 5 | Full reachability. |
| Stealth / trickery / disruption | 15 | 7 / 9 | `stealth.threshold_jam` and `stealth.trace_fold` missed. |

Reachability therefore passes for seven categories and fails for three: ritual, defense, and stealth. Because the success criteria asked for all templates to remain reachable, this pass cannot be marked full success. 【F:analytics/post-expansion-probe-20260319.txt†L7-L17】

### Focus on stealth viability

The stealth registry now contains `hushwire`, `paper_trail`, `shadow_proxy`, `threshold_jam`, `dead_drop_lattice`, `echo_shunt`, `ghost_shift`, `social_smoke`, and `trace_fold`. In this probe, `echo_shunt` and `ghost_shift` led with `4` hits each, `dead_drop_lattice` landed `3`, and both `threshold_jam` and `trace_fold` stayed at `0`. This means stealth is still viable as a category, but not yet stable as a fully reachable nine-template system under ordinary pressure. 【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L260-L284】【F:analytics/post-expansion-probe-20260319.txt†L17-L19】

## SECTION 4: DOMINANCE ANALYSIS

### Single-template dominance check

No category breached the explicit hard-fail line of `>50%` top-template dominance. Top-share ceilings stayed between `15.4%` and `35.0%`, so there is no evidence of complete single-template lock. 【F:analytics/post-expansion-probe-20260319.txt†L8-L17】

### Top-3 concentration check

| Category | Top-3 share | Judgment |
|---|---:|---|
| Survival | 46.2% | Healthy spread. |
| Resource | 48.0% | Healthy spread. |
| Ritual | 50.0% | Acceptable but concentrated. |
| Sensing | 56.3% | Acceptable. |
| Social | 61.1% | Moderate concentration. |
| Combat | 66.7% | Small-pool concentration but still rotating. |
| Crafting | 70.0% | Concentrated but still full-reach. |
| Defense | 71.4% | Weak spread; partial reachability amplifies concentration. |
| Stealth | 73.3% | Weakest expanded-category spread. |

### Small-category rotation and long-tail presence

- Crafting (`5` templates) shows full rotation: all five templates appeared, and all five appeared multiple times. 【F:analytics/post-expansion-probe-20260319.txt†L16-L16】
- Combat (`6` templates) also shows full rotation, but only two templates appeared more than once, so its long tail is reachable without yet being robust. 【F:analytics/post-expansion-probe-20260319.txt†L15-L15】
- Stealth (`9` templates) does **not** currently satisfy the “all templates reachable” or “long-tail templates appear multiple times” condition. Only three stealth templates appeared more than once, and two templates were absent. 【F:analytics/post-expansion-probe-20260319.txt†L17-L17】

Dominance verdict:
- **Pass** on the strict “no single template >50%” rule.
- **Partial fail** on small-category rotation and long-tail robustness, mainly due to stealth and secondarily defense/combat.

## SECTION 5: NOVELTY + SIMILARITY

Probe novelty metrics:
- Average novelty: `0.3657`
- Nearest-neighbor similarity: `0.6343`
- Average intra-niche novelty: `0.8264`
- Average global novelty: `0.3657`

Interpretation:
- Novelty remains materially above duplication territory.
- Nearest-neighbor similarity is well below a near-copy regime.
- Intra-niche novelty remains higher than global novelty, so the ordering that preserves niche-local differentiation is still intact.
- There is no evidence in this probe of regression toward duplicate-heavy generation. 【F:analytics/post-expansion-probe-20260319.txt†L22-L22】

Novelty verdict: **stable / preserved**.

## SECTION 6: NICHE DIVERGENCE

Recomputed Jensen-Shannon divergence:
- Explorer vs ritualist: `0.0458`
- Explorer vs warden: `0.0712`
- Ritualist vs warden: `0.0734`

These values are non-zero, so niche separation has not collapsed to literal identity, but they are also materially weaker than the stronger divergence bands documented in earlier novelty-tuning reports. This pass therefore shows **reduced but still present** niche separation rather than a clean preservation result. 【F:analytics/post-expansion-probe-20260319.txt†L23-L23】【F:docs/deep-ability-expansion.md†L50-L52】

Niche-divergence verdict: **partial**.

## SECTION 7: AUTHENTICITY READINESS

### Stat-buff dependence check

The expanded categories are still composed of process, routing, deception, timing, maintenance, and tactical-reading templates rather than plain stat-buff entries. Examples include `engineering.sequence_splice`, `engineering.machine_rhythm`, `engineering.fault_isolate`, `tactical.reposition_snare`, `tactical.tempo_extract`, `tactical.rush_damper`, `warding.fault_survey`, `warding.anchor_cadence`, `stealth.ghost_shift`, `stealth.social_smoke`, and `stealth.trace_fold`. That supports the claim that the categories do not depend on flat stat-buff-like fillers for breadth. 【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L179-L185】【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L195-L198】【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L227-L233】【F:src/main/java/obtuseloot/abilities/AbilityRegistry.java†L278-L284】

### Filter-collapse risk

- Crafting looks reasonably safe: full reachability, all templates multi-hit, and no runaway leader. 【F:analytics/post-expansion-probe-20260319.txt†L16-L16】
- Combat looks viable but still thinly sampled; filtering simple templates would increase tail pressure quickly. 【F:analytics/post-expansion-probe-20260319.txt†L15-L15】
- Defense is at risk because two templates were unreachable in a normal pass and the top-3 already hold `71.4%`. 【F:analytics/post-expansion-probe-20260319.txt†L12-L12】
- Stealth remains the biggest authenticity-readiness concern because it is sampled in all scenarios but still missed `threshold_jam` and `trace_fold`, meaning stricter filtering could collapse it back toward a narrow live subset. 【F:analytics/post-expansion-probe-20260319.txt†L17-L19】

### Final judgment

Requested success criteria were **not fully met**:
- all categories show meaningful sampling: **mostly yes**;
- all templates are reachable: **no**;
- no category shows extreme dominance: **yes** on the strict `>50%` rule;
- novelty + niche behavior remains stable: **novelty yes, niche separation only partial**;
- expanded categories behave as full systems, not edge cases: **crafting yes, combat partial, stealth no**.

Accordingly, this validation pass lands at **PARTIAL** rather than full success.

POST_EXPANSION_VALIDATION_RESULT: PARTIAL
