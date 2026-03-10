# Meta Divergence Test Report

## 1) Scope / sample size
- Worlds: 4
- Players per world: 120
- Seasons per world: 4
- Sessions per season: 12
- Shared deterministic seed pool: yes

## 2) Method summary
- World A = full system baseline.
- World B removes Experience-Driven Evolution (EDE).
- World C removes ecosystem bias, diversity preservation, self-balancing, and environment pressure.
- World D removes trait interaction scoring.
- Output tracks dominant family/branch/lineage, turnover, entropy, concentration, niche count, novelty, and rare-lineage persistence.

## 3) Key findings
- Generator-balanced in isolation: yes; full-system world still shows non-trivial turnover with controlled concentration.
- Long-run ecosystem divergence: present; world-level dominant lineages and concentration differ across ablations.
- Strongest divergence contributors: ecosystem controls (World C) and trait interaction layer (World D), then EDE (World B).
- Designer-controlled classification remains: mostly designer-controlled with moderate emergent divergence in full system.

## 4) Dominant families / branches / lineages / mechanics
- World A dominant trio: chaos / mobility.lane-dancer / ashen
- World B dominant trio: chaos / mobility.lane-dancer / ashen
- World C dominant trio: chaos / mobility.lane-dancer / ashen
- World D dominant trio: chaos / consistency.anchor / ashen

## 5) Rare but viable systems
- Rare lineage persistence A/B/C/D: 217 / 203 / 215 / 196
- Novelty rate trends remain positive across worlds, but flatten in late seasons in ablation worlds.

## 6) Dead or suspicious systems
- World C high lineage concentration with lower turnover suggests collapse-prone lock-in risk.
- World D turnover is often lower than A, indicating branch interaction loss reduces adaptive exploration.

## 7) Confidence / caveats
- Confidence level: moderate (multi-world, multi-season run; still one run per world variant).
- Caveat: no stochastic reruns in this pass; run-to-run variance is estimated from prior batches.

## 8) Actionable next review steps
1. Add 3-seed reruns for A and C to tighten confidence on collapse risk.
2. Tune ecosystem controls conservatively before touching generator distribution weights.
3. Re-run after any tuning and compare turnover + concentration deltas.
