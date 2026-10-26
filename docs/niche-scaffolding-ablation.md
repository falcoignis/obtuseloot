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

RESULT_PENDING
