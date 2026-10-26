# Niche scaffolding ablation: forced bifurcation and one-occupant reconciliation

The owner ruled both mechanisms scaffolding to remove, each behind an ablation (decision ledger, emergence criterion 1). This note pre-registers the comparison before the ablated arms run.

## Mechanisms

- **Forced bifurcation.** `NichePopulationTracker.tryForcedBifurcation` splits the most populous niche with at least 10% share, bypassing the pressure gates, if no bifurcation has happened by evaluation window 3 (`FORCED_BIFURCATION_WINDOW`). `NicheBifurcationRegistry.forceBifurcation` is its only implementation.
- **One-occupant reconciliation.** `reconcileProtectedOccupancy` and the empty-child branch of `enforceAssignmentContinuity` put one artifact back into an emptied child niche that is eligible for continuity protection (`seedContinuityOccupant`, restoring the last known occupant first). A protected child therefore cannot empty, so it never collapses.

Out of scope (classified top-down but not ruled on): the sticky lock on an existing occupant, grace periods, collapse hysteresis, re-entry stabilization, caps, cooldowns, migration, displacement and inversion multipliers. They stay unchanged in every arm.

## Reproducibility prerequisite

Before this change, the comparison could not be replayed:
- Evaluation was throttled on wall time inside `recordTelemetry`, so the number of windows depended on machine speed.
- Retirement stamped wall time into the cooldown map that evaluations compare with the passed time.
- Child loops iterated `Set.copyOf`, and signal snapshots and niche profiles used `Map.copyOf`/`Set.copyOf`, whose iteration order is salted per JVM (three JVMs printed three orders). That order decided which artifact a continuity pass assigned and the summation order of utility mass.

The tracker, registry and usage tracker now share one clock (wall time by default), and those loops iterate in a fixed order. `NicheScaffoldingAblationTest` replays a reduced scenario exactly with fresh state, and checks that a retired child's cooldown runs on the supplied clock.

## Harness

`NicheScaffoldingAblationTest#nicheScaffoldingAblation`, opt-in with `-Dobtuseloot.experiments=true`:
- 16 seeds, 48 artifacts in 8 lineages, 24 windows of 6 s on a synthetic clock (10 ms per execution), with the real generator, `ExperienceEvolutionEngine` feedback, `ArtifactUsageTracker.trackAbilityExecution` and the real niche tracker.
- Outcomes are meaningful with a hidden per-seed success rate per niche tag (uniform 0.2 to 0.9). No niche, share or survival target is named.
- Output is per seed: per-window live children, births, collapses, Simpson diversity and effective niches over `effectiveNicheName` occupancy, plus per-child birth and collapse windows.
- Limit: the generator's `variantFor` bias needs the plugin singleton and is not exercised offline. The tracker reaches selection here only through outcome relevance (`nicheCompetitionFactor`, `nicheAdoptionFitnessMultiplier`). No in-game validation.

The runs execute on GitHub Actions (`.github/workflows/niche-scaffolding-ablation.yml`), one job per arm and seed group, and upload the CSVs as artifacts.

## Arms

| Arm | Commit | Change |
|---|---|---|
| control | reproducibility commit | both mechanisms present |
| forced_off | next commit | forced bifurcation removed |
| both_off | following commit | forced bifurcation and one-occupant reconciliation removed |

Comparing forced_off with control isolates forced bifurcation, and both_off with forced_off isolates reconciliation. Removing forcing alone could cut births and so hide extinctions.

## Pre-registered rules

Each rule is shown if it holds in at least 12 of 16 seeds. There is no calculated prediction for either rule.
- **Rule F (organic births without forcing):** forced_off has at least one child birth.
- **Rule R (extinction no longer blocked):** both_off has more child collapses than forced_off.

Also reported, not rules: seeds whose output is identical between arms (the mechanism never acted there), first-birth window, child-windows, mean lifetime of collapsed children, censored children, final Simpson and effective niches. The guardrail is the `RelativeDiversityHealth` largest relative Simpson drop against the first 3 windows, with the count of seeds over 25%. It is reported only and never used for tuning.

Replay check: a second control job runs seed 0 on a separate runner, and its output must match the main control run byte for byte.

Summarizer: `scripts/summarize-niche-scaffolding.py CONTROL FORCED_OFF BOTH_OFF`.

## Result

Run on GitHub Actions, run 40251774417, arm commits c04d42c (control), d082463 (forced_off) and edac947 (both_off). The workflow that ran them is `.github/workflows/niche-scaffolding-ablation.yml` at 64e2c37; it was removed afterward because it pins this branch's commits. The full suite at 64e2c37 passed in the same run: 606 tests, 0 failures, 0 errors, 9 opt-in experiments skipped. The control's seed 0 on a separate runner matched the main control run byte for byte. It also matched a pre-run local control on falco-servo (children and births), so the harness replays across machines.

| | control | forced_off | both_off |
|---|---|---|---|
| mean child births per seed | 2.00 | 1.12 | 1.12 |
| mean child collapses per seed | 0.50 | 0.12 | 0.12 |
| mean live child-windows | 37.31 | 24.06 | 24.06 |
| mean final Simpson | 0.8535 | 0.8518 | 0.8518 |
| mean final effective niches | 8.112 | 7.953 | 7.953 |
| guardrail drop over 25% | 0/16 | 0/16 | 0/16 |

- **Rule F: not shown (9/16).** forced_off is identical to the control in 9 seeds. In each of those, the one bifurcation is organic and happens in window 1. In the other 7 seeds, the control's only bifurcation is in window 2 (evaluation 3, `FORCED_BIFURCATION_WINDOW`), and without forcing those seeds have no bifurcation in 24 windows. In 6 of the 7 forced pairs, one child collapsed after 6 windows and the other stayed to the end; in the seventh both stayed.
- **Rule R: not shown (0/16).** both_off is byte-identical to forced_off in all 16 seeds, so reconciliation never acted in this harness. Continuity protection needs smoothed lineage support of at least 0.20. Affinity grows by `0.05 x outcome quality` per update, decays by 0.93 per window, and is pruned below 0.005. The unit witness `emptiedProtectedChildIsNotRefilledByReconciliation` shows the removed behavior did act when that support is reached (it fails on the previous commit, where the emptied child is refilled), but this harness never reached it.
- Organic children collapsed in 2 seeds in every arm, with reconciliation present, so those children were never protected.
- Not explained by this run: in every seed and arm, no bifurcation happened after the first one, although the 60 s cooldown (10 windows) left at least 11 more windows. Which gate blocks later splits is not identified.

Interpretation, limited to this harness: forced bifurcation guaranteed a niche split that 7 of 16 seeds did not produce from their own pressure. Removing it lowered child-niche presence, but generated-niche diversity stayed about the same (final Simpson 0.8535 vs 0.8518, no seed over the guardrail). Reconciliation was inert here. The ablation does not show organic niche formation is robust: births are rare and one-shot. The generator's `variantFor` path and live play are untested.
