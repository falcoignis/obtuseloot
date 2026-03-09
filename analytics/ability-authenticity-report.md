# Ability Authenticity Report

## Method
Audit source abilities declared in `AbilityRegistry` and classify each by its dominant `AbilityEffectType`.
`stat-only` is set to `true` only when every core effect is `FLAT_STAT`.

## Ability Inventory

| Ability | Trigger | Core mechanic type | Stat-only flag | Evolution interaction | Drift interaction | Awakening interaction | Fusion interaction |
|---|---|---|---|---|---|---|---|
| Marked Interval | onHit | enemy interaction | false | Stage ladder alters mark behavior from single-target mark to cross-target persistence. | Drift alignment contributes to resolver family ranking. | Stage 4 exposes timed vulnerability windows. | Stage 5 persists mark state across target swaps. |
| Ravenous Momentum | onKill | temporary state | false | Stage ladder escalates from burst to chain and intimidation effects. | Drift contributes to brutality family pick weighting. | Stage 4 extends burst while low-health. | Stage 5 converts burst into area shockwave behavior. |
| Lastline Rebuke | onLowHealth | conditional mechanic | false | Stage ladder adds additional reactive pulses and tempo steal. | Drift nudges survival-family resolution. | Stage 4 adds recovery echo trigger. | Stage 5 projects ward effects outward. |
| Slipstream Echo | onReposition | movement interaction | false | Stage ladder expands echo count and wake behavior. | Drift and mobility affinity influence assignment. | Stage 4 adds wake zone interaction. | Stage 5 turns wake into pull field. |
| Unstable Bloom | onDriftMutation | battlefield influence | false | Stage ladder increases anomaly complexity and boss scaling. | Direct trigger linkage to drift mutation events. | Stage 4 allows anomaly reroll behavior. | Stage 5 links anomaly behavior to kill-chain tempo. |
| Measured Cadence | onChainCombat | timing mechanic | false | Stage ladder modifies rhythm windows and fallback defensive beat. | Drift and consistency bias alter resolver pick likelihood. | Stage 4 preserves cadence through kill transitions. | Stage 5 synchronizes cadence timing outward. |

## Summary Metrics

- Behavioral abilities: 6 / 6 (100.0%)
- Stat-only abilities: 0 / 6 (0.0%)
- Ability trigger diversity: 6 unique triggers across 6 abilities (`onHit`, `onKill`, `onLowHealth`, `onReposition`, `onDriftMutation`, `onChainCombat`)
- Dead abilities (unreachable by trigger model): none detected in current trigger graph
- Overrepresented abilities: none (one ability per family in current catalog)

## Pass/Fail

**PASS** — Behavior-driven ability share is **100%**, above the required **80%** threshold.
