# NSER Consistency Audit

## Previous inconsistency found
- Several world-lab/open-endedness reviews used `seasonalSnapshots[*].noveltyRate` while canonical NSER analytics used `NovelStrategyEmergenceAnalyzer`, producing conflicting NSER_latest and trend references.
- Dashboard generation also occurred before NSER-dependent analytics were rewritten, which could expose stale NSER values.

## Root cause
- Two parallel novelty pipelines were mixed in reporting: legacy `noveltyRate` snapshot fields and NSER analyzer outputs.
- Report generation order allowed dashboard/diagnostic surfaces to be regenerated before final NSER artifacts were stabilized.

## Corrected NSER pipeline
1. Compute NSER once via `NovelStrategyEmergenceAnalyzer.analyze(seasonalSnapshots)`.
2. Treat `nserResult.trend()` and its latest value as authoritative for all analytics/reporting layers.
3. Feed same NSER into ecosystem gauge, ecology diagnostic, world-lab reviews, open-endedness reviews, and dashboard regeneration.
4. Regenerate dashboard only after NSER/gauge/diagnostic artifacts are written.

## Agreement check
- Authoritative NSER_latest: 1.0
- ecosystem-health-gauge.json NSER_latest: 1.0
- ecology-diagnostic-state.json NSER: 1.0
- novel-strategy-emergence.json bySeason(last).NSER: 1.0
- All values aligned: true
