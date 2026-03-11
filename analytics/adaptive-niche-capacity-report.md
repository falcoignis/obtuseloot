# Adaptive Niche Capacity Report

- Enabled: true
- Bounds (min/max): {max=1.25, min=0.8}
- Capacity values by niche: {niche-1=1.0514707641071468, niche-2=1.030914318793936}
- Capacity changes over time: {niche-1=[1.0, 1.0, 1.0306590747268098, 1.0306590747268098, 1.0514707641071468], niche-2=[1.0, 1.0, 1.0207972966452397, 1.0207972966452397, 1.030914318793936]}
- Positive/negative adjustment contributors (seasonal): [{season=1, nicheId=niche-1, before=1.0, after=1.0306590747268098, delta=0.030659074726809843, noveltySignal=0.9770588932337955, interactionDiversity=1.0, nichePersistence=1.0, chronicOvercrowding=1.0, prolongedDominanceWithoutNovelty=0.28842306900680426}, {season=1, nicheId=niche-2, before=1.0, after=1.0207972966452397, delta=0.02079729664523966, noveltySignal=0.27436729396495785, interactionDiversity=0.014492753623188406, nichePersistence=1.0, chronicOvercrowding=0.0, prolongedDominanceWithoutNovelty=0.2574525748724281}, {season=2, nicheId=niche-1, before=1.0306590747268098, after=1.0514707641071468, delta=0.02081168938033695, noveltySignal=0.9888729237312489, interactionDiversity=1.0, nichePersistence=0.5, chronicOvercrowding=1.0, prolongedDominanceWithoutNovelty=0.29514994438668596}, {season=2, nicheId=niche-2, before=1.0207972966452397, after=1.030914318793936, delta=0.010117022148696364, noveltySignal=0.27436729396495785, interactionDiversity=0.014492753623188406, nichePersistence=0.5, chronicOvercrowding=0.0, prolongedDominanceWithoutNovelty=0.28012839142387197}]

## Most expanded niches
- Derived from positive seasonal deltas in `seasonAdjustments`.

## Most constrained niches
- Derived from negative seasonal deltas in `seasonAdjustments`.

## Expected ecosystem impact
- Durable and diverse niches can slowly earn capacity headroom; chronically overcrowded/stagnant niches lose some room.
- Fitness sharing remains active; niche capacity only modulates sharing load with bounded influence.

## Risk analysis
- If novelty signal is noisy, capacity can drift toward neutral; monitor PNNC/END before tightening weights.
- Bounds and maxSeasonDelta prevent violent swings or runaway advantage.
