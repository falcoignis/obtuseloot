# Small World Simulation Summary

## 1) Scope / sample size
- Players: 40
- Artifacts per player: 2
- Sessions per season: 12
- Seasons: 2
- Ability profile rows: 40894

## 2) Method summary
- Simulated progression loop with encounters, memory events, evolution, mutation, awakening, and fusion.
- Metrics summarize final artifact state + cumulative ability generation outcomes.

## 3) Key findings
- Dominant family share: 30.42%
- Branch convergence rate: 50.71%
- Mutation frequency: 86.88%
- Awakening/Fusion rates: 63.02% / 22.66%

## 4) Dominant families / branches / mechanics
- Family chaos: 12440
- Family brutality: 9894
- Family survival: 8448
- Branch brutality.quarry: 936
- Branch survival.shelter: 172
- Branch survival.guardian: 150
- Mechanic guardian_pulse: 10486
- Mechanic burst_state: 9337
- Mechanic revenant_trigger: 8039

## 5) Rare but viable systems
- chaos.awakened-variant: 3 (0.16%)
- consistency.discipline: 3 (0.16%)
- consistency.anchor: 3 (0.16%)
- chaos.sprawl: 4 (0.21%)
- precision.awakened-variant: 5 (0.26%)
- chaos.paradox: 6 (0.31%)
- consistency.boss-ledger: 7 (0.36%)
- chaos.awakened-entropy: 10 (0.52%)
- survival.awakened-remnant: 10 (0.52%)
- survival.awakened-variant: 14 (0.73%)

## 6) Dead or suspicious systems
- Dead branch rate: 0.00%
- Low-memory trigger frequency (on_memory_event): 4914

## 7) Confidence / caveats
- Single-run summary; trust improves when checked against multi-run large-world validation.

## 8) Suggested next review steps
- Compare this summary against `multirun-world-sim-report.md` for stability checks.
- Use `world-sim-confidence-report.md` before applying any balancing changes.
