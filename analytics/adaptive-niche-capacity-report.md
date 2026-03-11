# Adaptive Niche Capacity Report

- Enabled: true
- Bounds (min/max): {max=1.25, min=0.8}
- Capacity values by niche: {niche-1=1.0521781599577322, niche-2=1.0342772964861249}
- Capacity changes over time: {niche-1=[1.0, 1.0, 1.0311037444834963, 1.0311037444834963, 1.0521781599577322], niche-2=[1.0, 1.0, 1.0224825865981337, 1.0224825865981337, 1.0342772964861249]}
- Positive/negative adjustment contributors (seasonal): [{season=1, nicheId=niche-1, before=1.0, after=1.0311037444834963, delta=0.031103744483496287, noveltySignal=0.9818853974121996, interactionDiversity=1.0, nichePersistence=1.0, chronicOvercrowding=1.0, prolongedDominanceWithoutNovelty=0.2784272479623213}, {season=1, nicheId=niche-2, before=1.0, after=1.0224825865981337, delta=0.022482586598133736, noveltySignal=0.32223266745005874, interactionDiversity=0.021739130434782608, nichePersistence=1.0, chronicOvercrowding=0.0, prolongedDominanceWithoutNovelty=0.2539725344687908}, {season=2, nicheId=niche-1, before=1.0311037444834963, after=1.0521781599577322, delta=0.02107441547423594, noveltySignal=0.9912264995523724, interactionDiversity=1.0, nichePersistence=0.5, chronicOvercrowding=1.0, prolongedDominanceWithoutNovelty=0.2887459837445092}, {season=2, nicheId=niche-2, before=1.0224825865981337, after=1.0342772964861249, delta=0.01179470988799114, noveltySignal=0.32223266745005874, interactionDiversity=0.021739130434782608, nichePersistence=0.5, chronicOvercrowding=0.0, prolongedDominanceWithoutNovelty=0.27690175814021195}]

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
