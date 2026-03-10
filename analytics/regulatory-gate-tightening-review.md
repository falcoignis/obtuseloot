# Regulatory Gate Tightening Review

## Before/after gate open rates

| Gate | Before share | After share | Δ |
|---|---:|---:|---:|
| disciplineGate | 0.156 | 0.157 | +0.001 |
| environmentGate | 0.094 | 0.093 | -0.001 |
| lineageMilestoneGate | 0.164 | 0.162 | -0.002 |
| memoryGate | 0.161 | 0.161 | -0.000 |
| mobilityGate | 0.159 | 0.159 | +0.000 |
| resonanceGate | 0.072 | 0.068 | -0.004 |
| survivalGate | 0.162 | 0.161 | -0.001 |
| volatilityGate | 0.032 | 0.038 | +0.007 |

## Gate profile diversity
- Before profile entropy: 2.106
- After profile entropy: 2.035
- Dominant profile share before/after: 0.254 / 0.197

## Lineage specialization impact
- Lineage concentration before/after: 0.081 / 0.083
- Lineage count before/after: 205 / 1015
- Gate tightening reduced broad always-open tendencies while preserving multi-profile viability.

## Risk analysis
- **Dead-branch risk:** low; branch dead-rate remained bounded in the large-world validation run.
- **Over-restriction risk:** low-to-medium; candidate pools remained viable through gate fallback behavior.
- **Specialization lock-in risk:** medium; continue monitoring dominant lineage share across repeated large runs.
