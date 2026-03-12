SECTION 1: HARNESS AND ANALYTICS EXECUTION PATHS
- Harness entry points discovered:
  - `scripts/run-world-simulation.sh` -> `obtuseloot.simulation.worldlab.WorldSimulationRunner`.
  - `scripts/run-world-lab-validation.sh` multi-run wrapper for world-lab.
  - `scripts/run-open-endedness-test.sh` -> `OpenEndednessTestRunner`.
- Analytics entry points discovered: `scripts/run-ecosystem-analysis.sh` and `obtuseloot.analytics.ecosystem.AnalyticsCliMain` (`analyze`, `run-spec`).
- Entry points actually used in this validation pass:
  - `mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner ...` for five behavior scenarios.
  - `java -cp target/classes obtuseloot.analytics.ecosystem.AnalyticsCliMain analyze --dataset ... --output ... --job-id ...` for each run.

SECTION 2: SCENARIO MATRIX
| Scenario | Behavior mix | Players | Artifacts/player | Seasons | Sessions/season | Encounter density | Seed |
|---|---|---:|---:|---:|---:|---:|---:|
| explorer-heavy | 70/10/10/10 (Explorer/Ritualist/Gatherer/Random) | 6 | 2 | 2 | 2 | 2 | 11101 |
| ritualist-heavy | 10/70/10/10 (Explorer/Ritualist/Gatherer/Random) | 6 | 2 | 2 | 2 | 2 | 22202 |
| gatherer-heavy | 10/10/70/10 (Explorer/Ritualist/Gatherer/Random) | 6 | 2 | 2 | 2 | 2 | 33303 |
| mixed | 30/30/30/10 (Explorer/Ritualist/Gatherer/Random) | 6 | 2 | 2 | 2 | 2 | 44404 |
| random-baseline | 0/0/0/100 (Explorer/Ritualist/Gatherer/Random) | 6 | 2 | 2 | 2 | 2 | 55505 |
- Additional scenario knobs set via scenario config files: `artifact_population_size`, `generations`, `mutation_intensity`, `competition_pressure`, `ecology_sensitivity`, `lineage_drift_window`.

SECTION 3: OUTPUT ARTIFACTS
- Per scenario harness outputs: `world-sim-data.json`, `world-sim-report.md`, `world-sim-balance-findings.md`, `world-sim-meta-shifts.md`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`, `rollup-snapshots.json`, `scenario-metadata.properties`.
- Per scenario analytics outputs: `validation-<scenario>-analysis-report.txt`, `validation-<scenario>-output-manifest.properties`, recommendation history and job metadata in `analytics/validation-suite/analysis/<scenario>/`.

SECTION 4: PER-SCENARIO RESULTS
### explorer-heavy
- Dominant family rate: 0.3565; branch convergence: 0.4882; dead branch rate: 0.0000.
- Lineage count: 26; lineage extinction rate: 0.0000.
- Branch births/collapses: 28/0.
- Diversity timeline: [3.223049154216597, 3.4246968929998296]; turnover: 1.0370.
- Telemetry volume: 31555 events; competition allocations: 29308; mutation events: 864.

### ritualist-heavy
- Dominant family rate: 0.3762; branch convergence: 0.4865; dead branch rate: 0.0909.
- Lineage count: 33; lineage extinction rate: 0.0000.
- Branch births/collapses: 36/0.
- Diversity timeline: [3.686489312906069, 3.4306551777402388]; turnover: 1.4400.
- Telemetry volume: 31100 events; competition allocations: 28845; mutation events: 864.

### gatherer-heavy
- Dominant family rate: 0.4317; branch convergence: 0.5235; dead branch rate: 0.1111.
- Lineage count: 32; lineage extinction rate: 0.0000.
- Branch births/collapses: 30/0.
- Diversity timeline: [3.383892547006046, 3.1313855997935693]; turnover: 1.1111.
- Telemetry volume: 31798 events; competition allocations: 29549; mutation events: 864.

### mixed
- Dominant family rate: 0.4167; branch convergence: 0.5098; dead branch rate: 0.1000.
- Lineage count: 31; lineage extinction rate: 0.0000.
- Branch births/collapses: 32/0.
- Diversity timeline: [3.4454552466477493, 3.2511555223399813]; turnover: 1.0323.
- Telemetry volume: 34673 events; competition allocations: 32176; mutation events: 960.

### random-baseline
- Dominant family rate: 0.4062; branch convergence: 0.5154; dead branch rate: 0.1000.
- Lineage count: 30; lineage extinction rate: 0.0000.
- Branch births/collapses: 31/0.
- Diversity timeline: [3.3454010203901907, 3.193796826659776]; turnover: 1.1923.
- Telemetry volume: 31475 events; competition allocations: 29225; mutation events: 864.

SECTION 5: CROSS-SCENARIO COMPARISON
- Dominant-family concentration ranged from 0.3565 (explorer-heavy) to 0.4317 (gatherer-heavy).
- Branch convergence stayed narrow (0.4865..0.5235), indicating stable attractors across behavior mixes.
- Dead branch rates were 0.0000..0.1111; branch collapses remained 0 in all runs while branch births remained 28..36.
- Diversity fell from start->end in 4/5 runs; only explorer-heavy increased slightly (3.223->3.425).
- Random baseline landed near mixed/gatherer outcomes for dominant-family and convergence metrics, suggesting behavior-model separation is currently weak at this short horizon.

SECTION 6: NICHE DYNAMICS ASSESSMENT
- Niche formation evidence is weak: `phase6_experiment_outputs.niche_population_timelines` was empty in all five runs.
- Niche utility density timeline was not emitted as a timeline artifact in these runs; only final rollup/property snapshots were available.
- Niche saturation/specialization signal is present but shallow: high competition allocation counts exist, but no clear niche turnover timeline was exported.
- Assessment: niches exist in final rollups but timeline observability is insufficient to confidently validate saturation/turnover dynamics.

SECTION 7: LINEAGE DYNAMICS ASSESSMENT
- Lineages do emerge (26..33 lineages across runs) and branch formation occurs (28..36 births).
- Weak-lineage collapse signal is limited: branch collapse count stayed at 0 and lineage extinction rate stayed 0.0 in all runs.
- Lineage drift-window and branch-divergence metrics are present in rollup properties, showing instrumentation exists; however, survival/collapse separation is currently one-sided (birth-heavy, collapse-light).
- Assessment: lineage divergence mechanics are active, but lifecycle pressure likely underpowered in these short validations.

SECTION 8: COMPETITION / TURNOVER ASSESSMENT
- Competition pressure is definitely active (28.8k..32.2k competition allocation events per run).
- Turnover rates remained >1.0 (1.03..1.44), indicating displacement/churn is occurring.
- Diminishing-returns/displacement appears bounded rather than runaway: branch convergence remained near ~0.49-0.52, not near full lock-in.
- But branch collapse stayed 0 everywhere, so competitive elimination is weaker than expected relative to allocation pressure.

SECTION 9: TELEMETRY / ANALYTICS COHERENCE
- Coherence positives: telemetry event counts, rollup snapshot properties, harness world summary, and analytics CLI all executed and produced consistent high-level volumes.
- Coherence gaps:
  - `world-sim-data.json` includes non-JSON object string dumps (e.g., `OpportunitySignal[...]`, `LineagePopulationRollup[...]`) making strict JSON parsing fail.
  - Analytics CLI loaded only one rollup per run (`rollups_loaded=1`), so long-term trend/anomaly detection reported insufficient history.
  - Niche timeline exports were empty, reducing confidence in ecology trend analysis.
- Recommendation/anomaly output status: recommendations were generated (PROPOSED) for all runs; anomaly severity remained 0.000 with no runaway/collapse flags from the short-window analytics run.

SECTION 10: FAILURE MODES DETECTED
- Detected / likely:
  - Random baseline looked similar to behavior-specific scenarios on key aggregate metrics (dominant-family and convergence bands overlap).
  - Branch collapse absent in all runs (possible under-enforcement of collapse pressure).
  - Niche timeline observability failure (empty niche population timelines).
  - Analytics historical depth insufficient (single rollup only), limiting anomaly/tuning confidence.
- Not detected in this pass: absolute single-lineage dominance across all scenarios; branch formation did occur in every scenario.

SECTION 11: READINESS FOR ABILITY EXPANSION
- Verdict: **MOSTLY READY** (with telemetry/observability caveats).
- Why not fully READY: inability to cleanly parse world JSON artifacts, missing niche timelines, and insufficient multi-rollup analytics depth make it risky to add large ability batches blind.
- Safest next ability work now: narrowly scoped abilities that intentionally target underrepresented trigger/mechanic families already highlighted by balance findings.

SECTION 12: TOP 5 NEXT ACTIONS
1. Fix harness export contract so `world-sim-data.json` is valid JSON (no raw object `toString()` payloads).
2. Ensure niche population + utility density timelines are always populated and exported for every run.
3. Emit multiple rollup snapshots per run (or preserve seasonal snapshots as rollups) so analytics can perform true trend/anomaly detection.
4. Add explicit branch collapse pressure diagnostics (birth/collapse ratio target bands) to detect one-sided lineage growth.
5. Re-run this suite at a medium horizon (>=6 seasons, >=12 sessions) after the above fixes to verify behavior-model separation and long-run turnover.
