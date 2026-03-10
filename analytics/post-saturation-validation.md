# Post-Saturation Validation

## Runs executed
- 1 large-world simulation (600 players, 4 artifacts/player, 24 sessions/season, 4 seasons, seed 90217).
- 1 open-endedness comparison run (Worlds A/B/C/D).

## Measured metrics
- Dominant family share: **39.73%** (chaos).
- Dominant branch share: **16.62%** (mobility.lane-dancer).
- Branch entropy (large-world run): **3.7813**.
- Lineage concentration (top-lineage share): **8.08%**.
- Novelty rate (World A mean noveltyRatePerSeason): **0.1250**.
- Cross-trait viability (off-diagonal interaction share): **78.89%**.

## Comparison against pre-saturation baseline
- Pre-saturation dominant family share (5-run average): 45.17% -> post: 39.73%.
- Pre-saturation dominant branch share (5-run average): 17.04% -> post: 16.62%.
- Open-endedness World A branch entropy: 2.5407 -> 2.3727.

## Direct answers
- Did the recurring attractor weaken? **Yes**. Chaos remains dominant but with reduced share in the targeted large-world run.
- Did branch entropy improve? **No** (based on open-endedness World A branch entropy trend summary).
