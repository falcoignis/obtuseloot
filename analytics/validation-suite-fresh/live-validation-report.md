SECTION 1: HARNESS AND ANALYTICS EXECUTION PATHS
- Harness entry points inspected:
  - `scripts/run-world-simulation.sh` -> `WorldSimulationRunner`.
  - `scripts/run-world-lab-validation.sh` (multi-run world-lab wrapper).
  - `scripts/run-open-endedness-test.sh` -> `OpenEndednessTestRunner`.
- Analytics entry points inspected:
  - `scripts/run-ecosystem-analysis.sh` -> `AnalyticsCliMain run-spec`.
  - `AnalyticsCliMain` supports `analyze`, `run-spec`, `decide`, `export-accepted`.
- Entry points actually used:
  - `WorldSimulationRunner` for 5 scenario runs into `analytics/validation-suite-fresh/live-runs/*`.
  - `AnalyticsCliMain run-spec` for 5 matching analytics jobs into `analytics/validation-suite-fresh/live-analysis/*`.

SECTION 2: SCENARIO MATRIX
| Scenario | Behavior mix (E/R/G/RB) | Players | Artifacts/player | Seasons | Sessions/season | Encounter density | Scenario knobs |
|---|---|---:|---:|---:|---:|---:|---|
| explorer-heavy | 70/10/10/10 | 6 | 2 | 2 | 2 | 2 | mutation 1.20, competition 1.15, ecology 1.10, drift 1.25 |
| ritualist-heavy | 10/70/10/10 | 6 | 2 | 2 | 2 | 2 | mutation 0.95, competition 1.25, ecology 1.20, drift 0.90 |
| gatherer-heavy | 10/10/70/10 | 6 | 2 | 2 | 2 | 2 | mutation 1.10, competition 1.05, ecology 0.95, drift 1.40 |
| mixed | 30/30/30/10 | 6 | 2 | 2 | 2 | 2 | mutation 1.05, competition 1.20, ecology 1.15, drift 1.10 |
| random-baseline | 0/0/0/100 | 6 | 2 | 2 | 2 | 2 | mutation 1.00, competition 1.00, ecology 1.00, drift 1.00 |

SECTION 3: OUTPUT ARTIFACTS
- Harness outputs per scenario:
  - `world-sim-data.json` (phase6 metrics, lineages, niches, turnover, telemetry summaries).
  - `world-sim-report.md`, `world-sim-balance-findings.md`, `world-sim-meta-shifts.md`.
  - `scenario-metadata.properties`, `rollup-snapshots.json`, `telemetry/ecosystem-events.log`, `telemetry/rollup-snapshot.properties`, and `rollup_history/*`.
- Analytics outputs per scenario:
  - `live-<scenario>-analysis-report.txt`.
  - `live-<scenario>-output-manifest.properties`.
  - `live-<scenario>-job-record.properties` and `recommendation-history.log`.
- Aggregates generated in this pass:
  - `analytics/validation-suite-fresh/live-suite-summary.json`.
  - `analytics/validation-suite-fresh/live-analysis-summary.json`.

SECTION 4: PER-SCENARIO RESULTS
- explorer-heavy
  - Dominant family 0.4213, branch convergence 0.5063, dead branch rate 0.0000.
  - Lineage count 35, branch births/collapses 36/0, lineage extinction 0.0.
  - Diversity 3.513 -> 3.271, turnover 1.241.
  - Niche population timelines exported: 2; niche utility density timelines exported: 2.
  - Analytics: telemetry events 63,520; severity 0.000; no runaway lineages; recommendation PROPOSED.
- ritualist-heavy
  - Dominant family 0.3472, branch convergence 0.4934, dead branch rate 0.1111.
  - Lineage count 33, branch births/collapses 31/0, lineage extinction 0.0.
  - Diversity 3.607 -> 3.446, turnover 1.097.
  - Niche population timelines exported: 2; niche utility density timelines exported: 2.
  - Analytics: telemetry events 63,528; severity 0.000; no runaway lineages; recommendation PROPOSED.
- gatherer-heavy
  - Dominant family 0.3843, branch convergence 0.4915, dead branch rate 0.0909.
  - Lineage count 30, branch births/collapses 31/0, lineage extinction 0.0.
  - Diversity 3.686 -> 3.581, turnover 1.033.
  - Niche population timelines exported: 2; niche utility density timelines exported: 2.
  - Analytics: telemetry events 63,560; severity 0.000; no runaway lineages; recommendation PROPOSED.
- mixed
  - Dominant family 0.4271, branch convergence 0.5110, dead branch rate 0.0.
  - Lineage count 30, branch births/collapses 30/0, lineage extinction 0.0.
  - Diversity 3.173 -> 3.241, turnover 1.000.
  - Niche population timelines exported: 2; niche utility density timelines exported: 2.
  - Analytics: telemetry events 71,104; severity 0.000; no runaway lineages; recommendation PROPOSED.
- random-baseline
  - Dominant family 0.4120, branch convergence 0.5168, dead branch rate 0.1000.
  - Lineage count 35, branch births/collapses 35/0, lineage extinction 0.0.
  - Diversity 3.297 -> 3.203, turnover 1.146.
  - Niche population timelines exported: 2; niche utility density timelines exported: 2.
  - Analytics: telemetry events 62,726; severity 0.000; no runaway lineages; recommendation PROPOSED.

SECTION 5: CROSS-SCENARIO COMPARISON
- Dominant family concentration ranged 0.3472 .. 0.4271 (ritualist lowest, mixed highest).
- Branch convergence remained tight at 0.4915 .. 0.5168, indicating stable attractor behavior.
- Lineage emergence was consistent (30..35 lineages).
- Branch formation occurred in every scenario (30..36 births), but branch collapse remained 0 in all scenarios.
- Turnover remained >=1.0 in all scenarios (1.000 .. 1.241).
- Behavior profiles did affect shape somewhat (e.g., ritualist reduced dominant-family concentration), but random-baseline stayed close to explorer/mixed on convergence and dominance.

SECTION 6: NICHE DYNAMICS ASSESSMENT
- Do niches form clearly?
  - Yes, niche population and utility-density timelines were emitted in every scenario (2 timeline windows each).
- Are niches meaningfully different?
  - Partially. Cross-scenario dominant-family rates and turnover differ, but differences are still moderate at this horizon.
- Saturation/specialization signal:
  - Competition/turnover signals are active, but short horizon and zero branch collapse limit confidence in full saturation/turnover lifecycle.

SECTION 7: LINEAGE DYNAMICS ASSESSMENT
- Lineages emerge and persist in all runs (30..35 lineages).
- Branching occurs consistently (30..36 births).
- Collapse behavior is underexpressed (0 branch collapses and 0 lineage extinction in all scenarios).
- Conclusion: divergence mechanics are alive; elimination pressure is weak in this operating window.

SECTION 8: COMPETITION / TURNOVER ASSESSMENT
- Competition pressure appears active (non-zero dead branch rates in 3/5 scenarios; turnover >1 in 4/5 scenarios).
- Diminishing-return style bounding is present (convergence does not lock to 1.0; dominance is bounded <0.43).
- However, no actual branch collapse means displacement pressure is still softer than desired.

SECTION 9: TELEMETRY / ANALYTICS COHERENCE
- Coherence positives:
  - Harness emitted telemetry event logs, rollup snapshots, scenario metadata, and phase6 outputs.
  - Analytics CLI successfully consumed all five datasets and emitted recommendation histories.
- Coherence caveats:
  - `world-sim-data.json` still contains non-JSON object literal strings (`OpportunitySignal[...]`), requiring sanitization for strict JSON parsing.
  - Analytics long-term module reports insufficient history despite 2 rollups (`long_term_summary=Insufficient historical rollups for long-term analysis.`).
- Anomalies/tuning outputs:
  - No severe anomalies (`severity=0.000`, empty runaway/collapse lists).
  - Recommendations generated for all scenarios and left in `PROPOSED` state.

SECTION 10: FAILURE MODES DETECTED
- Detected:
  - Branch collapse absent in all five scenarios.
  - Behavior model separation is present but not strong enough; random baseline remains near targeted populations on key macro metrics.
  - Long-term analyzer remains data-limited at 2 rollups.
  - JSON contract violation in `world-sim-data.json` impairs strict machine parsing.
- Not detected:
  - Total single-lineage takeover across all scenarios.
  - Total single-niche lock-in across all scenarios.

SECTION 11: READINESS FOR ABILITY EXPANSION
- Verdict: MOSTLY READY.
- Rationale:
  - Core ecosystem loops are active (niches, lineage branching, turnover, competition telemetry).
  - Blocking caveats are observability and elimination-pressure quality (no collapses, limited long-term trend depth, malformed JSON export contract).
- Safe next ability categories:
  - Small, utility-expressive mechanics in underrepresented trigger/mechanic families highlighted in balance findings.
  - Avoid large ability-batch expansions until collapse pressure and long-horizon analytics are strengthened.

SECTION 12: TOP 5 NEXT ACTIONS
1. Fix `world-sim-data.json` serialization contract so strict JSON parsers work without sanitization.
2. Increase rollup retention / seasonal rollup export so long-term analyzer can produce real trend and anomaly findings.
3. Add/strengthen branch collapse pressure diagnostics and enforce target birth:collapse operating bands.
4. Re-run this same 5-scenario matrix at a medium horizon (>=6 seasons, >=12 sessions/season) to stress niche saturation and lineage elimination.
5. Calibrate behavior-profile separation tests so random baseline diverges more clearly from targeted populations on macro metrics.
