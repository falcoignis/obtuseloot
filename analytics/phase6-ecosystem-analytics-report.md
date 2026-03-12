# Phase 6 — Ecosystem Analytics and Tuning

## Telemetry schema and rollup analysis

The telemetry pipeline already emits normalized contracts per `EcosystemTelemetryEventType` and enforces required fields through `TelemetryFieldContract`. The rollup snapshot schema contains ecosystem-level metrics plus niche and lineage maps:

- niche population, utility density, specialization pressure, and branch contribution.
- lineage population, branch count, momentum, specialization trajectory, drift window, and branch divergence.
- global diversity, turnover, carrying-capacity utilization, branch births/collapses, and competition pressure.

Phase 6 analytics now consume those rollups without requiring the runtime plugin loop.

## Implemented analytics modules

- `EcosystemTrendAnalyzer` computes niche growth/collapse, utility-density drift, competition pressure shares, diversity drift, and turnover drift.
- `LineageSuccessAnalyzer` evaluates lineage success rates, branch survival, and specialization-cascade risk.
- `EcosystemAnomalyDetector` flags runaway lineages, niche collapse, mutation stagnation, ecological dead zones, and branch explosion.
- `EcosystemTuningRecommender` outputs structured tuning profiles for:
  - `niche_saturation_sensitivity`
  - `mutation_amplitude_min` / `mutation_amplitude_max`
  - `lineage_momentum_decay`
  - `competition_reinforcement_scaling`
- `LongTermEvolutionAnalyzer` provides ecosystem turnover, lineage lifespan windows, niche emergence, and adaptation cycle strength.
- `TelemetrySchemaAnalyzer` provides contract-health checks for all telemetry event types.
- `EcosystemAnalyticsOrchestrator` composes all analyzers into one independent Phase 6 report object.

## Expected operational outputs

Running the orchestrator over historical rollup snapshots produces:

1. Ecosystem trend report (`NicheEvolutionReport`)
2. Lineage success report (`LineageSuccessReport`)
3. Ecosystem anomaly diagnostics (`EcosystemAnomalyReport`)
4. Parameter tuning profile (`TuningProfileRecommendation`)
5. Long-term evolution report (`LongTermEvolutionReport`)

These outputs are designed for offline ecosystem tuning workflows and can be used by external tooling/pipelines.
