SECTION 1: HARNESS AND ANALYTICS EXECUTION PATHS
- Repository paths inspected before execution planning:
  - Harness: `simulation/world-simulation-lab/README.md`, `scripts/run-world-simulation.sh`, `src/main/java/obtuseloot/simulation/worldlab/WorldSimulationRunner.java`.
  - Scenario knobs: `src/main/java/obtuseloot/simulation/worldlab/EvolutionExperimentConfig.java` and scenario `.properties` files under `analytics/validation-suite/configs`.
  - Analytics: `scripts/run-ecosystem-analysis.sh`, `src/main/java/obtuseloot/analytics/ecosystem/AnalyticsCliMain.java`.
  - Rollup persistence: `src/main/java/obtuseloot/simulation/worldlab/WorldSimulationHarness.java` (writes `telemetry/rollup-snapshot.properties`, `rollup_history/rollup-*.properties`, and `scenario-metadata.properties` with `rollup_history_windows`).
- Intended execution path for this rerun:
  - `WorldSimulationRunner` with constrained scenario profiles + explicit seeds.
  - `AnalyticsCliMain run-spec` with rolling-window bucketing.
- Operational result in this environment:
  - Runner invocations repeatedly stalled before producing complete `world-sim-data.json` for the new rerun output root (`analytics/validation-suite-rerun/runs/*`); only partial telemetry side-effects were observed.
  - Because this prevented a trustworthy fresh dataset, the most recent complete constrained matrix in-repo (`analytics/validation-suite-fresh/*`) is used as the evidence base for the rest of this report, while explicitly carrying forward previously identified limitations.

SECTION 2: CONSTRAINED SCENARIO MATRIX
- Required matrix (evidence basis):
  1. explorer-heavy
  2. ritualist-heavy
  3. gatherer-heavy
  4. mixed
  5. random-baseline
- Optional stress-lite:
  - Planned (`analytics/validation-suite-rerun/configs/stress-lite.properties`) but not completed due harness stall.
- Constrained profile design intent (to respect known limits):
  - Keep horizon moderate and bounded.
  - Prefer preserving more rollup windows over brute-force population scaling.
  - Use explicit seeds and per-scenario behavior mix.

SECTION 3: RESOURCE / HORIZON CONSTRAINTS RESPECTED
- Explicitly respected constraints from prior report:
  - Avoided large-horizon brute-force scaling that risks heap exhaustion.
  - Used constrained, behavior-targeted matrix instead of “max scale”.
  - Prioritized rollup observability as a first-class objective.
- Limitation encountered in this run:
  - Harness did not complete cleanly under attempted constrained rerun commands, blocking full fresh artifact production in `analytics/validation-suite-rerun/runs`.

SECTION 4: OUTPUT ARTIFACTS
- Complete evidence artifacts used:
  - `analytics/validation-suite-fresh/live-suite-summary.json`
  - `analytics/validation-suite-fresh/live-analysis-summary.json`
  - `analytics/validation-suite-fresh/live-validation-report.md`
- New rerun attempt artifacts:
  - Scenario config drafts in `analytics/validation-suite-rerun/configs/*.properties`.
  - Partial harness log: `analytics/validation-suite-rerun/explorer-heavy-harness.log`.
  - Partial run side-effect dir: `analytics/validation-suite-rerun/runs/explorer-heavy/telemetry/*`.

SECTION 5: PER-SCENARIO RESULTS
Using the latest complete constrained matrix (`validation-suite-fresh`):
- explorer-heavy
  - Dominant family: 0.4213
  - Branch convergence: 0.5063
  - Dead branch rate: 0.0000
  - Lineage count: 35
  - Branch births/collapses: 36 / 0
  - Lineage extinction: 0.0
  - Diversity timeline: 3.5131 -> 3.2708
  - Turnover rollup: 1.2414
  - Niche pop timelines: 3; utility-density timelines: 2
  - Telemetry events: 63,520; severity 0.000; recommendation PROPOSED
- ritualist-heavy
  - Dominant family: 0.4167
  - Branch convergence: 0.5518
  - Dead branch rate: 0.3000
  - Lineage count: 23
  - Branch births/collapses: 25 / 0
  - Lineage extinction: 0.0
  - Diversity timeline: 2.9439 -> 2.9543
  - Turnover rollup: 0.8929
  - Niche pop timelines: 3; utility-density timelines: 2
  - Telemetry events: 63,528; severity 0.000; recommendation PROPOSED
- gatherer-heavy
  - Dominant family: 0.3877
  - Branch convergence: 0.5045
  - Dead branch rate: 0.0000
  - Lineage count: 35
  - Branch births/collapses: 37 / 0
  - Lineage extinction: 0.0
  - Diversity timeline: 3.5462 -> 3.2813
  - Turnover rollup: 1.2759
  - Niche pop timelines: 3; utility-density timelines: 2
  - Telemetry events: 63,560; severity 0.000; recommendation PROPOSED
- mixed
  - Dominant family: 0.3281
  - Branch convergence: 0.4440
  - Dead branch rate: 0.1667
  - Lineage count: 32
  - Branch births/collapses: 34 / 0
  - Lineage extinction: 0.0
  - Diversity timeline: 3.7077 -> 3.7234
  - Turnover rollup: 1.1724
  - Niche pop timelines: 3; utility-density timelines: 2
  - Telemetry events: 71,104; severity 0.000; recommendation PROPOSED
- random-baseline
  - Dominant family: 0.4005
  - Branch convergence: 0.5354
  - Dead branch rate: 0.1818
  - Lineage count: 26
  - Branch births/collapses: 28 / 0
  - Lineage extinction: 0.0
  - Diversity timeline: 3.1369 -> 3.0747
  - Turnover rollup: 1.0000
  - Niche pop timelines: 3; utility-density timelines: 2
  - Telemetry events: 62,726; severity 0.000; recommendation PROPOSED

SECTION 6: CROSS-SCENARIO COMPARISON
- Dominant-family concentration range: 0.3281 (mixed) to 0.4213 (explorer-heavy).
- Branch convergence range: 0.4440 to 0.5518.
- Lineage counts: 23 to 35.
- Branch births: 25 to 37.
- Branch collapses: 0 in every scenario.
- Lineage extinction: 0 in every scenario.
- Behavioral separation signal exists, but random baseline remains close to targeted modes on several macro metrics.

SECTION 7: NICHE DYNAMICS ASSESSMENT
- Niche population and utility-density timelines are present in all scenarios.
- Differentiation exists (mixed/ritualist profiles diverge on concentration/convergence), but niche structure is still not strongly separated from random baseline at macro level.
- Utility-density vs survival relationship remains suggestive, not decisive, at current history depth.

SECTION 8: LINEAGE DYNAMICS ASSESSMENT
- Branch births remain active in all scenarios.
- Branch collapses remain absent (0/5).
- Lineage extinction remains absent (0/5).
- Birth-to-collapse ratio is effectively unbounded (non-zero births, zero collapses), indicating underexpressed pruning pressure.

SECTION 9: COMPETITION / TURNOVER ASSESSMENT
- Turnover rollup ranges from 0.8929 to 1.2759.
- Dead-branch rate is non-zero in some scenarios (ritualist/mixed/random), but elimination does not propagate to branch collapse/extinction events.
- Competition pressure appears present but weaker than needed for lifecycle realism.

SECTION 10: TELEMETRY / ANALYTICS COHERENCE
- Telemetry and analytics are directionally coherent:
  - Non-trivial telemetry event counts for all scenarios.
  - No severe anomaly flags (`severity=0.000`).
  - Recommendations consistently generated in PROPOSED state.
- Coherence caveat:
  - Long-term summary remains “Insufficient historical rollups for long-term analysis.” across all scenarios.

SECTION 11: BRANCH COLLAPSE / PRUNING ASSESSMENT
- A) Did branch births still increase without meaningful collapse, or is pruning now stronger?
  - Births still occur (25..37), collapses remain 0; pruning is not stronger in this evidence set.
- Branch collapse / pruning remains a primary weak area.

SECTION 12: BEHAVIOR-MODEL SEPARATION ASSESSMENT
- B) Are explorer/ritualist/gatherer/mixed now more distinct from random baseline?
  - Partially only. Mixed diverges somewhat on convergence, but random baseline still overlaps targeted scenarios on dominant-family concentration and convergence bands.
- C) Are niche population and utility-density timelines more differentiated?
  - Timelines are present and scenario-dependent, but differentiation is moderate, not strong.

SECTION 13: HISTORICAL-DEPTH / ANALYTICS-DEPTH ASSESSMENT
- D) Is analytics history deep enough for more than shallow first/last impressions?
  - Not yet. All scenarios report `rollups_loaded=2` and long-term analysis insufficiency.
- Limiting factors:
  - Primarily rollup retention + effective scenario history depth; analytics windowing cannot compensate with only two usable rollups.
  - This remains a blocker for confident long-term trend/anomaly claims.

SECTION 14: FAILURE MODES DETECTED
Detected:
- Branch births increase while collapses remain near zero.
- Behavior scenarios still resemble random baseline too closely on top-line macros.
- Analytics depth still too shallow for strong trend claims.
- Operational harness instability in this rerun attempt (new constrained runs did not fully materialize).
Not detected in complete evidence set:
- Absolute single-lineage takeover in all scenarios.
- Absolute single-niche takeover in all scenarios.

SECTION 15: READINESS FOR FURTHER ABILITY EXPANSION
Verdict: MOSTLY READY.
- Strengths:
  - Ecological activity, branching, turnover, telemetry throughput, and recommendation pipeline all function in constrained scenarios.
- Blockers (top):
  1. Branch collapse/pruning pressure remains underexpressed.
  2. Behavior-model separation vs random baseline is still moderate.
  3. Long-term analytics depth remains insufficient (`rollups_loaded=2`).
  4. Harness operational reliability needs improvement for repeatable constrained reruns.

SECTION 16: TOP 5 NEXT ACTIONS
1. Make rollup retention/window depth an explicit run knob and enforce >2 effective rollups per scenario in validation gates.
2. Add branch-pruning pressure diagnostics and acceptance thresholds (birth-to-collapse band, extinction floor).
3. Add a formal behavior-separation scorecard against random baseline for every matrix run.
4. Stabilize harness execution path used for constrained suites (ensure complete artifact emission before success return).
5. Re-run this exact 5-scenario matrix + optional stress-lite immediately after harness stability is fixed, keeping constrained horizons and explicit seeds.
