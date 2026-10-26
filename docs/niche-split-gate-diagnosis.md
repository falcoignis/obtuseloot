# Why no niche splits a second time

The niche scaffolding ablation (`docs/niche-scaffolding-ablation.md`) found that no seed split a second time in any arm. This diagnostic records which bifurcation gate decided every evaluation. It changes no selection rule.

## Method

- `NicheBifurcationRegistry` records, for each parent niche, the gate that decided its latest evaluation, together with the inputs it saw (`lastGate`). The possible gates are: global or parent cap, parent or global cooldown, low pressure, low specialization, low population, low share, sustaining, and split. Nothing in selection reads it.
- `NicheScaffoldingAblationTest` writes those observations per window to `gates.csv`.
- `scripts/summarize-niche-split-gates.py` groups them around each seed's first split.
- Same harness as the ablation: 16 seeds, 48 artifacts, 24 windows, current `main` (both aids removed). GitHub Actions run 40260927128.
- Neutrality: `windows.csv` and `children.csv` are byte-identical to the ablation's both_off arm in all 16 seeds.

## Result

| Phase | Deciding gates (rows) |
|---|---|
| Before the first split | low pressure 2,093; low population 37 |
| Split window | low pressure 34; split 9 |
| First 9 windows after it | global cooldown 972; parent cooldown 81 |
| Remaining windows | low pressure in every evaluated niche, except a few parent-cooldown rows after a child retired |

After the cooldowns expire, every niche fails the pressure gate. That gate needs saturation of at least 0.05 or a population share of at least 0.13. In those rows, share reached at most 0.105 and saturation was 0.

**Share is diluted by membership growth.** A niche's share is its member count divided by the summed members of all niches. An artifact is a member of every niche that any of its recorded mechanic signals maps to (`EcosystemRoleClassifier.classify`, and the per-membership count in `NichePopulationTracker.rollups`). As artifacts gain abilities and keep their signal history, memberships per artifact grow, and every niche's share converges toward one over the number of niches.

| Window | Mean memberships per artifact | Mean top share |
|---|---|---|
| 1 | 2.61 | 0.131 |
| 2 | 5.88 | 0.124 |
| 5 | 7.71 | 0.113 |
| 10 | 9.07 | 0.104 |
| 23 | 10.25 | 0.096 |

There are 14 niche tags, and about 13 are evaluated per window. All 9 splits happened in window 1, at shares from 0.131 to 0.147. After about window 2, no niche reaches the 0.13 share gate.

**The saturation path almost never opens.** Saturation was above zero in only 41 of 4,747 evaluations, all in windows 0 and 1. The 37 evaluations at or above 0.05 were blocked by low population.

## Interpretation and limits

The second-split blocker is structural, not a near miss. It comes from cumulative, unweighted membership counting rather than from the threshold value. Lowering the 0.13 threshold would be distribution tuning and is not proposed. Design directions for the owner:
- count each artifact once, for example by dominant niche, so share means a fraction of artifacts;
- weight each artifact's memberships by its utility mass, summing to one per artifact;
- let old signals decay, so membership reflects current use.

Why saturation stays at zero after window 1 is a separate open question. These results come from the offline harness with simulated outcomes; live play is untested. Whether other consumers of share would be affected by redefining it has not been checked.
