# Trait Interference Shuffle Report

- Shortlist threshold used: `score >= topScore * 0.94` (within 6% of top).
- Shortlist cap: `3` candidates.
- Selection method: weighted stochastic pick proportional to score among shortlist only.
- Determinism: seeded RNG from artifact deterministic seed, so same seed remains consistent.

## Candidate shuffle frequency
- Expected when at least two candidates are within the 6% near-top band.
- In steep score landscapes, resolution remains effectively greedy (top-only).

## Expected ecological impact
- Preserves strong candidates while allowing near-optimal alternatives to survive intermittently.
- Increases stepping-stone viability and branch turnover without permitting weak-candidate leapfrogging.

## Risk analysis
- **Power drift risk:** low (top-3 near-top shortlist only).
- **Noise risk:** low (deterministic seeded stochasticity).
- **Convergence persistence risk:** medium if score gaps consistently exceed 6%.
