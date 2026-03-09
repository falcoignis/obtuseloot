# Procedural Generator Audit

## 1) Scope / sample size
- Sample size: 5000 generated artifacts

## 2) Method summary
- Checked deterministic seed composition and distribution spread across families/branches/triggers/mechanics.
- Verified mutation outputs are generated from active profiles, not metadata placeholders.

## 3) Key findings
- Deterministic seed composition validated.
- Multi-template families observed across all six families.
- Branch updates vary by family, stage, drift alignment, awakening, fusion, and memory profile.

## 4) Dominant families / branches / mechanics
- Family consistency: 868
- Family survival: 866
- Family precision: 819

## 5) Rare but viable systems
- precision.clock: 251
- mobility.fusion-slipstream: 254
- precision.awakened-discipline: 257
- survival.guardian: 264

## 6) Dead or suspicious systems
- None flagged in isolated generation.

## 7) Confidence / caveats
- Generator-level audit cannot detect long-run progression convergence.

## 8) Suggested next review steps
- Cross-check with world-lab confidence and multi-run reports.
