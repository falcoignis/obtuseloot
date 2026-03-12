SECTION 1: HARNESS AND ANALYTICS EXECUTION PATHS
- Harness entry points discovered:
  - `scripts/run-world-simulation.sh` → `obtuseloot.simulation.worldlab.WorldSimulationRunner`.
  - `scripts/run-world-lab-validation.sh` (multi-run world-lab wrapper).
  - Direct Maven execution of `WorldSimulationRunner` with `world.*` system properties.
- Analytics entry points discovered:
  - `scripts/run-ecosystem-analysis.sh`.
  - `obtuseloot.analytics.ecosystem.AnalyticsCliMain` (`analyze`, `run-spec`, `decide`, `export-accepted`).
- Execution path actually used in this pass:
  1) `mvn -q -DskipTests ... -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner ...`
  2) `java -cp target/classes obtuseloot.analytics.ecosystem.AnalyticsCliMain analyze --dataset ... --output ...`
- Scenario controls validated from config + runtime overrides:
  - Scenario file knobs: `artifact_population_size`, `generations`, `mutation_intensity`, `competition_pressure`, `ecology_sensitivity`, `lineage_drift_window`, `player_behavior_mix.*`.
  - Runtime knobs: `world.players`, `world.artifactsPerPlayer`, `world.sessionsPerSeason`, `world.seasonCount`, `world.encounterDensity`, `world.seed`, plus ecosystem flags in `WorldSimulationRunner`.

SECTION 2: SCENARIO MATRIX
| Scenario | Behavior mix (E/R/G/RB) | Artifact pop | Generations | Mutation | Competition | Ecology | Drift window | Players | Artifacts/player | Seasons | Sessions/season | Encounter density | Seed |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| explorer-heavy | 0.70/0.10/0.10/0.10 | 54 | 24 | 1.20 | 1.15 | 1.10 | 1.25 | 6 | 2 | 2 | 2 | 2 | 11101 |
| ritualist-heavy | 0.10/0.70/0.10/0.10 | 54 | 24 | 0.95 | 1.25 | 1.20 | 0.90 | 6 | 2 | 2 | 2 | 2 | 22202 |
| gatherer-heavy | 0.10/0.10/0.70/0.10 | 54 | 24 | 1.10 | 1.05 | 0.95 | 1.40 | 6 | 2 | 2 | 2 | 2 | 33303 |
| mixed | 0.30/0.30/0.30/0.10 | 60 | 27 | 1.05 | 1.20 | 1.15 | 1.10 | 6 | 2 | 2 | 2 | 2 | 44404 |
| random-baseline | 0.00/0.00/0.00/1.00 | 54 | 24 | 1.00 | 1.00 | 1.00 | 1.00 | 6 | 2 | 2 | 2 | 2 | 55505 |

SECTION 3: OUTPUT ARTIFACTS
- Harness outputs per scenario:
  - `world-sim-data.json`
  - `world-sim-report.md`
  - `world-sim-balance-findings.md`
  - `world-sim-meta-shifts.md`
  - `rollup-snapshots.json`
  - `telemetry/ecosystem-events.log`
  - `telemetry/rollup-snapshot.properties`
  - `scenario-metadata.properties`
- Analytics outputs per scenario:
  - `validation-20260312-<scenario>-analysis-report.txt`
  - `validation-20260312-<scenario>-job-record.properties`
  - `validation-20260312-<scenario>-output-manifest.properties`
  - `validation-20260312-<scenario>-run-metadata.properties`
  - `recommendation-history.log`

SECTION 4: PER-SCENARIO RESULTS
- explorer-heavy
  - Dominant family 0.3634; branch convergence 0.5398; dead-branch 0.0000.
  - Lineages 28; branch births/collapses 9/0; lineage extinction 0.0.
  - Diversity timeline 3.2372 → 3.2623; turnover rollup 0.3333.
  - Niche population timelines present (3 niches), niche utility density timelines present (2 niches).
  - Analytics: 101,068 telemetry events; severity 0.000; recommendation emitted (PROPOSED).
- ritualist-heavy
  - Dominant family 0.4109; branch convergence 0.5257; dead-branch 0.1111.
  - Lineages 27; branch births/collapses 6/0; lineage extinction 0.0.
  - Diversity timeline 3.2907 → 3.1804; turnover rollup 0.2400.
  - Niche population timelines present (3 niches), niche utility density timelines present (2 niches).
  - Analytics: 99,012 telemetry events; severity 0.000; recommendation emitted (PROPOSED).
- gatherer-heavy
  - Dominant family 0.4120; branch convergence 0.5225; dead-branch 0.0000.
  - Lineages 28; branch births/collapses 6/0; lineage extinction 0.0.
  - Diversity timeline 3.2149 → 3.1251; turnover rollup 0.2222.
  - Niche population timelines present (3 niches), niche utility density timelines present (2 niches).
  - Analytics: 100,806 telemetry events; severity 0.000; recommendation emitted (PROPOSED).
- mixed
  - Dominant family 0.4354; branch convergence 0.5065; dead-branch 0.0000.
  - Lineages 35; branch births/collapses 10/0; lineage extinction 0.0.
  - Diversity timeline 3.4263 → 3.2802; turnover rollup 0.3226.
  - Niche population timelines present (3 niches), niche utility density timelines present (2 niches).
  - Analytics: 113,884 telemetry events; severity 0.000; recommendation emitted (PROPOSED).
- random-baseline
  - Dominant family 0.4259; branch convergence 0.5037; dead-branch 0.1111.
  - Lineages 28; branch births/collapses 5/0; lineage extinction 0.0.
  - Diversity timeline 3.3528 → 3.2258; turnover rollup 0.1923.
  - Niche population timelines present (3 niches), niche utility density timelines present (2 niches).
  - Analytics: 100,394 telemetry events; severity 0.000; recommendation emitted (PROPOSED).

SECTION 5: CROSS-SCENARIO COMPARISON
- Dominant family concentration range: 0.3634 (explorer-heavy) to 0.4354 (mixed).
- Branch convergence range: 0.5037 to 0.5398 (stable attractor band, not extreme lock-in).
- Dead-branch rate: 0.0 in three scenarios, 0.1111 in ritualist-heavy and random-baseline.
- Branch births: 5..10; branch collapses: 0 in every scenario.
- Diversity changed downward in 4/5 runs; explorer-heavy rose slightly.
- Random-baseline remains close to behavior-weighted scenarios on several top-line metrics, indicating moderate but not strong behavior separation.

SECTION 6: NICHE DYNAMICS ASSESSMENT
A. Niche formation
- Yes: explicit niche population timelines are present per scenario (`niche-1`, `GENERALIST`, `unassigned`) with per-window counts.
- But niche identity depth appears shallow: dominant trajectory tends toward `niche-1` quickly in this short horizon.

B. Niche saturation and specialization
- Saturation pressure exists: niche-1 utility density grows strongly from window 1 → 2 in all runs.
- Specialization signal exists but is limited to early-window differentiation; rare niches are represented but mostly as low-volume tracks.

SECTION 7: LINEAGE DYNAMICS ASSESSMENT
C. Lineage behavior
- Lineages emerge robustly (27..35 lineages).
- Branches do form (5..10 births) and branch survival rates remain high in per-season lifecycle records.
- Weak-lineage collapse remains underexpressed: collapse counts in branch lifecycle are low and rollup branch-collapses remain 0.
- Drift-window influence is visible in scenario-level shifts (e.g., gatherer-heavy high drift window still did not increase collapse pressure materially).

SECTION 8: COMPETITION / TURNOVER ASSESSMENT
D. Competition pressure
- Competition creates displacement (turnover rollup 0.1923..0.3333), but elimination pressure is weak.
- Diminishing returns are present indirectly via capped convergence and diversity softening, not by strong branch extinction.
- Carrying-capacity behavior appears bounded rather than runaway: no catastrophic monoculture, but also limited pruning.

E/F. Utility signal integrity and stability vs stagnation
- Utility-weighted ecosystems are stable and coherent, not chaotic.
- Potential stagnation risk: low branch collapse and zero lineage extinction across all scenarios.
- Flavor/no-op distortion does not dominate outputs, but behavior-specific differences vs random baseline are not yet strong enough for high-confidence separation claims.

SECTION 9: TELEMETRY / ANALYTICS COHERENCE
- Coherence checks passed:
  - Harness emits niche timelines, lineage curves, branch lifecycle diagnostics, telemetry logs, and rollups.
  - Analytics CLI consumed each dataset and produced anomaly/recommendation reports.
- Analytics findings:
  - `rollups_loaded=2`, `window_size=1` across scenarios.
  - No anomaly spikes (`severity=0.000`, no runaway lineage, no niche collapse flags).
  - Recommendations generated in PROPOSED state for every run.
- Limitation:
  - Long-term analyzer reports insufficient historical rollups for deep temporal trend claims.

SECTION 10: FAILURE MODES DETECTED
- Detected / likely:
  - Branch-collapse underexpression (births present; collapses effectively absent in rollup-level accounting).
  - Zero lineage extinction in all scenarios.
  - Behavior-model separation is moderate; random baseline still resembles weighted mixes on several aggregate metrics.
  - Long-term analytics remains shallow due short rollup history.
- Not detected:
  - Single-lineage universal dominance.
  - Total niche failure (niche timelines are now populated).
  - Runaway anomaly conditions.

SECTION 11: READINESS FOR ABILITY EXPANSION
- Verdict: **MOSTLY READY**.
- Rationale:
  - Telemetry + rollups + analytics pipeline are operational and coherent.
  - Ecology/lineage dynamics are active, but collapse pressure is weak and behavior differentiation is not yet strong.
  - Good for targeted additions, but risky for broad ability floods that assume mature competitive pruning.
- Safest ability expansion now:
  - Abilities that intentionally sharpen niche separation and branch tradeoffs (rather than broad power additions).

SECTION 12: TOP 5 NEXT ACTIONS
1. Increase branch-collapse observability and pressure calibration (explicit birth/collapse target bands).
2. Extend validation runs to more windows/seasons and preserve additional rollups to unlock long-term analytics.
3. Add cross-scenario behavior-separation scorecards so random-baseline similarity is continuously tracked.
4. Validate utility-density/survival correlation directly in exported summaries (per-lineage regression or rank correlation).
5. Re-run this suite after collapse-pressure tuning to verify branch lifecycle (birth, persistence, collapse) reaches expected equilibrium.

Tiny fixes made to enable the run:
- None to simulation code. Operationally, an attempted larger-horizon run was aborted due Java heap exhaustion; suite was re-scoped to the supported short-horizon validation profile for successful completion.
