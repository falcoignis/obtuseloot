# Role Axis Validity Report

- Sample count: 160
- Axis means: {support_vs_damage=0.4155357142857139, burst_vs_persistence=0.49637885281333577, mobility_vs_stationary=0.1358482142857143, environment_dependent_vs_agnostic=0.07068452380952378, memory_driven_vs_direct_trigger=0.8309375000000025, interaction_heavy_vs_solo=0.6222103379979378}
- Axis variances: {support_vs_damage=0.010580070153061838, burst_vs_persistence=0.0031763684227378297, mobility_vs_stationary=0.021284101961096896, environment_dependent_vs_agnostic=0.005740128082482998, memory_driven_vs_direct_trigger=8.39746093746041E-4, interaction_heavy_vs_solo=0.0013476334503448317}
- Axis ranges: {support_vs_damage=0.28571428571428564, burst_vs_persistence=0.2841666666666667, mobility_vs_stationary=0.4178571428571428, environment_dependent_vs_agnostic=0.16666666666666666, memory_driven_vs_direct_trigger=0.09999999999999998, interaction_heavy_vs_solo=0.2712500000000001}
- Axis informative flags: {support_vs_damage=true, burst_vs_persistence=true, mobility_vs_stationary=true, environment_dependent_vs_agnostic=true, memory_driven_vs_direct_trigger=false, interaction_heavy_vs_solo=true}
- Dead/non-informative axes: [memory_driven_vs_direct_trigger]
- Axis computation: support/damage, burst/persistence, mobility/stationary, environment dependent/agnostic, memory/direct-trigger, interaction/solo; each bounded to [0,1].
- Interpretation: axes with variance >= 0.0025 or range >= 0.10 are treated as meaningful differentiators.
