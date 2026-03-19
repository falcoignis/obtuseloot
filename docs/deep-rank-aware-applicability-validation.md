# Deep Rank-Aware Applicability Validation

This pass was measurement-only and did not modify code. I re-ran the post-authenticity applicability shape against the live branch using the same scenario matrix and volumes as the prior failed pass: 5 normal scenarios at 1,250 selections each plus 850 category-forced selections per category (14,750 total candidate selections overall). I also re-ran `IntraCategoryNormalizationProbeTest` to confirm the small-category normalization guardrail and the inspect/structure adjacency behavior for `stealth.trace_fold` and related templates.

## Validation configuration

- Validation date: 2026-03-19 UTC.
- Normal scenarios: `explorer-heavy`, `ritualist-heavy`, `warden-heavy`, `mixed`, `random-baseline`.
- Normal probe volume: `6,250` weighted selections total.
- Forced probe volume: `8,500` weighted selections total (`850` per category).
- Baseline for comparison: `docs/deep-post-authenticity-applicability-validation.md` (the previous **FAILED** run).
- Additional guardrail check: `mvn -q -Dtest=IntraCategoryNormalizationProbeTest test`.

## A) Dominance correction

### Current forced-probe metrics

| Category | Reachability | Top template % | Top-3 % | Templates with >1 hit | Zero-hit templates |
| --- | ---: | ---: | ---: | ---: | --- |
| Traversal / mobility | 8 / 8 | 60.24% | 99.41% | 3 | none |
| Sensing / information | 9 / 9 | 61.53% | 99.29% | 2 | none |
| Survival / adaptation | 11 / 11 | 63.65% | 97.76% | 4 | none |
| Combat / tactical control | 6 / 6 | 70.00% | 99.65% | 2 | none |
| Defense / warding | 8 / 8 | 52.59% | 99.41% | 2 | none |
| Resource / farming / logistics | 4 / 8 | 83.53% | 99.29% | 4 | `gathering.forager_memory`, `logistics.queue_sight`, `logistics.relay_mesh`, `logistics.spoilage_audit` |
| Crafting / engineering / automation | 2 / 5 | 87.76% | 100.00% | 2 | `engineering.redstone_sympathy`, `engineering.machine_rhythm`, `engineering.fault_isolate` |
| Social / support / coordination | 4 / 8 | 51.76% | 99.76% | 4 | `social.witness_imprint`, `social.collective_insight`, `support.mercy_link`, `support.cover_exchange` |
| Ritual / strange utility | 13 / 13 | 63.88% | 98.82% | 3 | none |
| Stealth / trickery / disruption | 9 / 9 | 87.88% | 99.29% | 3 | none |

### Comparison against the previous FAILED run

| Category | Previous top % | Current top % | Delta | Previous top-3 % | Current top-3 % | Delta | Previous >1 hit | Current >1 hit | Delta |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Traversal / mobility | 59.53% | 60.24% | +0.71 pp | 100.00% | 99.41% | -0.59 pp | 2 | 3 | +1 |
| Sensing / information | 92.47% | 61.53% | -30.94 pp | 100.00% | 99.29% | -0.71 pp | 3 | 2 | -1 |
| Survival / adaptation | 46.47% | 63.65% | +17.18 pp | 100.00% | 97.76% | -2.24 pp | 3 | 4 | +1 |
| Combat / tactical control | 51.06% | 70.00% | +18.94 pp | 100.00% | 99.65% | -0.35 pp | 2 | 2 | +0 |
| Defense / warding | 49.06% | 52.59% | +3.53 pp | 99.41% | 99.41% | +0.00 pp | 3 | 2 | -1 |
| Resource / farming / logistics | 92.82% | 83.53% | -9.29 pp | 99.41% | 99.29% | -0.12 pp | 3 | 4 | +1 |
| Crafting / engineering / automation | 100.00% | 87.76% | -12.24 pp | 100.00% | 100.00% | +0.00 pp | 1 | 2 | +1 |
| Social / support / coordination | 71.18% | 51.76% | -19.42 pp | 100.00% | 99.76% | -0.24 pp | 2 | 4 | +2 |
| Ritual / strange utility | 61.41% | 63.88% | +2.47 pp | 99.88% | 98.82% | -1.06 pp | 3 | 3 | +0 |
| Stealth / trickery / disruption | 55.41% | 87.88% | +32.47 pp | 99.18% | 99.29% | +0.11 pp | 4 | 3 | -1 |

### Dominance verdict

- **Improved:** sensing, resource, crafting, and social all reduced top-template share versus the previous failed run.
- **Regressed:** traversal, survival, combat, defense, ritual, and especially stealth all became more top-heavy.
- **Net result:** dominance is **not corrected overall**. The strongest failure is stealth, which moved from a previously tolerable but still bad `55.41%` top share to an extremely dominant `87.88%`.

## B) Reachability recovery

### Forced reachability per category

| Category | Previous forced reach | Current forced reach | Delta |
| --- | ---: | ---: | ---: |
| Traversal / mobility | 2 / 8 | 8 / 8 | +6 |
| Sensing / information | 3 / 9 | 9 / 9 | +6 |
| Survival / adaptation | 3 / 11 | 11 / 11 | +8 |
| Combat / tactical control | 2 / 6 | 6 / 6 | +4 |
| Defense / warding | 8 / 8 | 8 / 8 | +0 |
| Resource / farming / logistics | 8 / 8 | 4 / 8 | -4 |
| Crafting / engineering / automation | 1 / 5 | 2 / 5 | +1 |
| Social / support / coordination | 2 / 8 | 4 / 8 | +2 |
| Ritual / strange utility | 4 / 13 | 13 / 13 | +9 |
| Stealth / trickery / disruption | 9 / 9 | 9 / 9 | +0 |

### Zero-hit templates

- Traversal / mobility: none.
- Sensing / information: none.
- Survival / adaptation: none.
- Combat / tactical control: none.
- Defense / warding: none.
- Resource / farming / logistics: `gathering.forager_memory`, `logistics.queue_sight`, `logistics.relay_mesh`, `logistics.spoilage_audit`.
- Crafting / engineering / automation: `engineering.redstone_sympathy`, `engineering.machine_rhythm`, `engineering.fault_isolate`.
- Social / support / coordination: `social.witness_imprint`, `social.collective_insight`, `support.mercy_link`, `support.cover_exchange`.
- Ritual / strange utility: none.
- Stealth / trickery / disruption: none.

### Reachability verdict

- Reachability **recovered strongly** in traversal, sensing, survival, combat, and ritual.
- Reachability **held** in defense and stealth.
- Reachability **regressed badly** in resource/logistics and still remains weak in crafting and social.
- The forced-probe system is therefore **recovered in breadth for several categories, but not stably across the whole matrix**.

## C) Recovery persistence

Previously recovered templates still appear under normal-pressure generation in this pass:

- `stealth.trace_fold`: **87** normal-pressure hits.
- `stealth.threshold_jam`: **69** normal-pressure hits.
- Structure / inspect-adjacent templates also remain visible:
  - `consistency.structure_echo`: **120** normal-pressure hits.
  - `consistency.path_thread`: **90** normal-pressure hits.

Guardrail confirmation from `IntraCategoryNormalizationProbeTest` remains positive as well:

- Stealth normalization probe still reached all 9 stealth templates with a top share of roughly **34%** and top-3 share of roughly **69%**.
- The same test still confirms bounded inspect/structure adjacency for `stealth.trace_fold` and bounded reachability help for `stealth.hushwire` / `stealth.threshold_jam`.

### Recovery persistence verdict

- **Pass** for persistence. The key recovered stealth templates still appear.
- **Pass** for structure/inspect-adjacent persistence. `consistency.structure_echo` and `consistency.path_thread` are both still active.
- **Caveat:** persistence exists mainly under normal pressure; it does **not** prevent renewed forced-probe dominance collapse.

## D) Regression check

### Any new dominant template >70%?

Yes.

- `stealth.echo_shunt`: **87.88%**.
- `engineering.sequence_splice`: **87.76%**.
- `logistics.stockpile_tide`: **83.53%**.
- `tactical.killzone_lattice`: **70.00%** exactly.

### Any category still collapsing to ≤2 templates?

Yes.

- Crafting / engineering / automation: **2** templates with >1 hit.
- Combat / tactical control: **2** templates with >1 hit.
- Defense / warding: **2** templates with >1 hit.
- Sensing / information: **2** templates with >1 hit.

### Any category worse than the previous FAILED state?

Yes.

- **Stealth** is materially worse on top-share and slightly worse on top-3 share.
- **Combat** is worse on top-share and unchanged on multi-hit breadth.
- **Survival** is worse on top-share despite better reachability.
- **Traversal**, **defense**, and **ritual** also worsen on top-share.

### Regression verdict

- **FAIL**. New dominant templates above 70% exist.
- **FAIL**. Multiple categories still collapse to effectively 2 templates.
- **FAIL**. Several categories are worse than the previous failed state, even where reachability improved.

## E) Authenticity integrity

Using the existing post-authenticity detector shape against the live registry:

- stat-only outputs = **0**.
- vanilla-equivalent outputs = **0**.

### Authenticity verdict

- **Pass** for the two requested integrity checks.
- No evidence from this pass that rank-aware applicability suppression introduced stat-only or vanilla-equivalent outputs.

## Overall judgment

Rank-aware applicability suppression appears to have produced a **mixed trade** rather than a clean win:

- It **does improve reachability** in several categories that previously collapsed under forced probing.
- It **preserves key recovery persistence** for `stealth.trace_fold`, `stealth.threshold_jam`, and structure/inspect-adjacent templates.
- It **does not solve forced dominance**, and in several important places it makes dominance worse.
- The most serious new problem is that **stealth is now fully reachable but overwhelmingly dominated by `stealth.echo_shunt`**, which is a weaker overall outcome than the previous failed run for that category.

Bottom line:

- **Recovery:** partial success.
- **Dominance control:** failed.
- **Authenticity integrity:** preserved.
- **Full validation objective:** not met.

RANK_AWARE_APPLICABILITY_VALIDATION_RESULT: FAILED
