# Windowed niche diversity and the relative health regression

`NicheDiversityMetrics` defines these measures from positive occupancy counts.
Zero-count historical keys do not represent occupied niches. Invalid negative or
non-finite counts are rejected. Calculations use sorted niche keys so immutable
collection iteration order cannot change floating-point reduction order.

For each occupancy snapshot, let `p_i = count_i / sum(count)`:

| Measure | Definition | Bounds |
| --- | --- | --- |
| `diversityIndex` | Gini-Simpson diversity, `1 - sum(p_i^2)` | 0 to 1; 0 for empty or monoculture |
| END | Shannon effective niche count, `exp(-sum(p_i * ln(p_i)))` | 1 to the number of occupied niches; 0 if empty |
| `turnoverRate` | Niches born plus niches extinct between adjacent snapshots, divided by their occupied-niche union | 0 to 1 |
| TNT | Total variation of adjacent population shares, `0.5 * sum(abs(p_i - q_i))` | 0 to 1; 1 when exactly one population is empty |

Birth and extinction here mean an occupancy transition, not proof of an
evolutionary origin or a lineage dying. A niche with zero occupants can reappear
in a later window. A first rollup establishes a baseline and reports zero
turnover. Restored occupancy provides the baseline after a restart. The branch
birth/collapse counters remain cumulative event counters and do not enter these
metrics. Occupancy is the telemetry buffer's last-known population; artifact
retirement and exact identity hydration are outside this change.

`EcosystemHealthGaugeAnalyzer` delegates END and TNT to the same definitions.
Its older absolute status labels are retained as report interpretations; they
are not used by the regression gate or fed back into selection.

`SeededEcosystemHealthTest` uses 32 artifacts over 12 windows, seed
`0x4845414C54484C`. Each artifact has one observed dominant niche, classified
from its generated abilities using the existing taxonomy; enum order resolves
ties. Each window is a fresh population count. The real procedural generator
and utility-feedback engine run with an isolated diversity index. Simulated
outcome observations use a seeded RNG and simulated timestamps. No quota,
named-niche survival requirement, or absolute diversity target is applied.

The gate compares every window after the first three to the mean Gini-Simpson
diversity of those three. It fails if any later window drops more than 25%.
An empty or monoculture baseline is insufficient evidence and is rejected.
Negative controls replace the final window with a monoculture or an empty
population and verify that the same gate fails.

This is a bounded regression scenario, not an emergence or gameplay validation.
It does not run Bukkit world effects, population birth/death, or wall-clock
bifurcation. Existing selection scaffolding remains. Wild mutation is held
pending the owner's later decision; existing designed randomness is unchanged.
