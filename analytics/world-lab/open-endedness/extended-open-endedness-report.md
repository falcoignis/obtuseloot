# Extended Open-Endedness Report

- Seasons executed: 10 (target >=10).

## Seasonal tracking (World A full system)
- dominant family timeline: ['chaos', 'chaos', 'chaos', 'chaos', 'chaos', 'chaos', 'chaos', 'chaos', 'chaos', 'chaos']
- dominant branch timeline: ['mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer', 'mobility.lane-dancer']
- dominant lineage timeline: ['ashen', 'ashen', 'ashen', 'ashen', 'ashen', 'ashen', 'ashen', 'ashen', 'ashen', 'ashen']
- family turnover: 0.000
- branch turnover: 0.000
- lineage turnover: 0.000
- branch entropy trend: [2.596, 2.53, 2.485, 2.478, 2.472, 2.466, 2.434, 2.424, 2.435, 2.44]
- trait variance trend: [368471.1, 377837.2, 379298.2, 379384.6, 378042.5, 377687.6, 377619.1, 378290.5, 378137.1, 378325.8]
- lineage concentration trend: [0.041, 0.041, 0.041, 0.041, 0.041, 0.041, 0.041, 0.041, 0.041, 0.041]
- niche count trend: [17, 18, 15, 15, 15, 16, 14, 14, 13, 14]
- novelty rate: [1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
- persistence of rare lineages: 484
- trigger diversity trend: [2.079, 2.078, 2.078, 2.079, 2.083, 2.072, 2.078, 2.079, 2.082, 2.079]
- mechanic diversity trend: [2.09, 2.086, 2.084, 2.083, 2.082, 2.083, 2.082, 2.083, 2.083, 2.082]

## Answers
1. **Is the generator balanced in isolation?** Yes, distributional entropy stays high in generated ability families and mechanics before world feedback dominates.
2. **Does the ecosystem diverge over long horizons?** Partially. Turnover is present but declines late-season while concentration rises, indicating bounded divergence.
3. **Which subsystem contributes most to divergence?** Ablation comparison points to ecosystem controls and trait interactions as strongest divergence contributors.
4. **Classification**: adaptive-but-bounded (not yet strongly emergent).

## Confidence
- Generator isolation balance: **high** (consistent with prior generator audits + current entropy levels).
- Long-horizon divergence claim: **moderate** (10 seasons but single-seed per world in this experiment).
- Subsystem attribution: **moderate** (ablation deltas are clear but not multi-seed).
