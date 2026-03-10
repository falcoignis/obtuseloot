# Extended Multi-Run World Simulation Report

## Per-run summaries
- Run 1 (seed 91001): family **chaos** (42.59%), branch **mobility.lane-dancer** (20.72%), entropy F/B 1.485/2.521, lineage concentration 0.039, mutation 99.99%, awaken/fusion 97.28%/29.78%, niches 17.
- Run 2 (seed 91002): family **chaos** (41.88%), branch **mobility.lane-dancer** (15.89%), entropy F/B 1.455/2.552, lineage concentration 0.039, mutation 99.99%, awaken/fusion 97.41%/27.41%, niches 17.
- Run 3 (seed 91003): family **chaos** (44.70%), branch **mobility.lane-dancer** (11.39%), entropy F/B 1.464/2.587, lineage concentration 0.041, mutation 99.99%, awaken/fusion 97.21%/29.63%, niches 17.
- Run 4 (seed 91004): family **chaos** (46.10%), branch **mobility.lane-dancer** (15.98%), entropy F/B 1.416/2.542, lineage concentration 0.038, mutation 99.98%, awaken/fusion 97.40%/30.12%, niches 17.
- Run 5 (seed 91005): family **chaos** (42.22%), branch **mobility.lane-dancer** (15.85%), entropy F/B 1.537/2.604, lineage concentration 0.038, mutation 99.99%, awaken/fusion 97.28%/31.91%, niches 16.

## Aggregate averages + spread
- dominant_family_share: avg 0.4350, sd 0.0163, range [0.4188, 0.4610].
- dominant_branch_share: avg 0.1597, sd 0.0295, range [0.1139, 0.2072].
- family_entropy: avg 1.4714, sd 0.0398, range [1.4157, 1.5371].
- branch_entropy: avg 2.5613, sd 0.0303, range [2.5211, 2.6043].
- lineage_concentration: avg 0.0388, sd 0.0010, range [0.0378, 0.0405].
- mutation_frequency: avg 0.9999, sd 0.0000, range [0.9998, 0.9999].
- awakening_adoption: avg 0.9732, sd 0.0008, range [0.9721, 0.9741].
- fusion_adoption: avg 0.2977, sd 0.0144, range [0.2741, 0.3191].
- niche_count: avg 16.8000, sd 0.4000, range [16.0000, 17.0000].
- trait_variance: avg 1348909637.2000, sd 56919876.1082, range [1266780910.3333, 1406455307.3333].
- top_trigger_share: avg 0.2075, sd 0.0032, range [0.2050, 0.2132].
- top_mechanic_share: avg 0.2571, sd 0.0021, range [0.2553, 0.2597].

## Repeated winners
- Family chaos: 5/5 wins.
- Branch mobility.lane-dancer: 5/5 wins.

## Rare but viable systems
- brutality.awakened-variant
- brutality.quarry
- consistency.discipline
- mobility.fusion-slipstream
- mobility.relay
- precision.awakened-variant
- survival.awakened-remnant
- survival.awakened-variant

## Confidence / caveats
- Confidence is **moderate-high** for repeated winners (5 independent runs, low spread on top shares).
- Confidence is **moderate** for niche counts and trait variance because definitions are proxy metrics over aggregate snapshots.
- Caveat: all runs share one profile family; robustness to profile shifts still untested.
