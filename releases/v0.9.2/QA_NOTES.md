# QA Notes — v0.9.2

## Focus Areas
- Seed system determinism and persistence.
- Debug command pathways (`/obtuseloot debug`, `simulate`, `seed`).
- Evolution/drift/awakening/fusion progression under scripted load.

## Simulation Tooling
- Gameplay simulation scaffold: `simulation/gameplay-simulator/`
- Chaos test scaffold: `simulation/chaos-tests/`
- Population simulator scaffold: `simulation/population-simulator/`
- World simulation lab scaffold: `simulation/world-simulation-lab/`

## Analytics Pipeline
Generated outputs are under `analytics/evolution`, `analytics/population`, `analytics/meta`, and `analytics/world-lab`.

## Multi-run Validation
Five-run comparison included in `analytics/meta/multirun-validation.*` and reflected in confidence scoring.
