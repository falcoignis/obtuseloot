# Organic niche splitting: no cooldowns, no caps

With utility-weighted occupancy (`docs/niche-share-at-source.md`), the 60 s global cooldown set the pace of niche splitting. The owner ruled on 2026-10-26: "I don't want the cooldown to set pace, it needs to be organic", then "we should probably get rid of niche caps too and let them set themselves organically somehow", and "we want as much of this to be organic evolution as possible". This note pre-registers the comparison before the arms run.

## Arms

All arms branch from the utility-weighted occupancy head (`8e05599`). Thresholds and the sustained-pressure requirement are unchanged.

| Arm | Commit | Change |
|---|---|---|
| control | 8e05599 | Weighted occupancy, cooldowns and caps in place. |
| no_cooldown | b74882c | Global and per-parent split cooldowns removed, along with the cooldown restamp on child retirement. |
| no_caps | 287b931 | no_cooldown, plus the global (8) and per-parent (4) niche caps removed. |
| relief | 0fa6a23 | no_caps, plus a parent's bifurcation share and saturation exclude the occupancy its members carry once they belong to its children, so a split relieves the pressure that caused it. |

A parent's rollup is keyed by its tag, so without relief a split does not lower the parent's measured pressure. Without caps, sustained pressure could then keep splitting the same parent. This is unverified; the no_caps arm measures it.

## Harness

`NicheScaffoldingAblationTest#nicheScaffoldingAblation`: 16 seeds, 48 artifacts, 24 windows of 6 s on a synthetic clock, with simulated outcomes. GitHub Actions, one job per arm and seed group, plus a full suite on each candidate head. The generator's `variantFor` path and live play are not exercised.

## Pre-registered rules

Each rule is shown if it holds in at least 12 of 16 seeds. There is no calculated prediction.
- **Rule P (organic pace):** the seed has at least one split, and fewer than half of its splits fall on windows 1, 11 and 21. Those are the windows where the 60 s global cooldown released in the control.
- **Rule S:** more than one split. **Rule E:** at least one child collapse. Both are carried over from the share comparison.

Also reported, not rules:
- splits per seed;
- maximum and final live children;
- the change in live children over the last 5 windows, as a sign of unbounded growth;
- deciding-gate counts from window 2;
- final Simpson diversity, and the guardrail (largest relative Simpson drop over 25% against the first 3 windows).

Live-children numbers are reported to detect runaway growth. They are not a target.

Summarizer: `scripts/summarize-organic-split.py CONTROL NO_COOLDOWN NO_CAPS RELIEF`.

## Result

GitHub Actions run 40273932781. The control's outputs are byte-identical to the weighted arm of the share comparison (16 of 16 files), and full suites at b74882c and 0fa6a23 passed (606 tests, 0 failures, 0 errors, 9 opt-in skipped).

| Arm | Splits per seed | Rule P | Rule S | Rule E | Child births / collapses per seed | Max live children | Final live | Live change, last 5 windows | Final Simpson | Guardrail |
|---|---|---|---|---|---|---|---|---|---|---|
| control | 2.81 | 4/16 | 15/16 | 11/16 | 5.6 / 1.3 | 6 | 4.31 | +1.69 | 0.8638 | 0/16 |
| no_cooldown | 6.19 | **16/16** | **15/16** | **15/16** | 12.4 / 6.1 | 8 | 6.31 | +0.06 | 0.8808 | 0/16 |
| no_caps | 20.44 | **16/16** | **15/16** | **15/16** | 40.9 / 26.0 | 18 | 14.88 | +0.44 | 0.8598 | 0/16 |
| relief | 16.25 | **15/16** | **15/16** | **14/16** | 32.5 / 20.6 | 20 | 11.88 | -0.38 | 0.8753 | 0/16 |

Deciding gates from window 2:
- **control:** global cooldown 3,465, low pressure 521, parent cooldown 268, parent cap 79.
- **no_cooldown:** global cap 2,366, low pressure 1,497. The 8-niche cap sets the pace once the clock is gone.
- **no_caps:** low pressure 1,668. Still slightly growing at the end.
- **relief:** low pressure 2,384. Slightly shrinking at the end.

Interpretation, limited to this harness:
- Removing the cooldowns alone hands pacing to the caps.
- Removing the caps as well lets pressure alone decide, with much more niche birth and extinction. Without relief the niche count is still creeping up at window 23.
- With relief, a split relieves its parent's pressure. That arm shows all three rules, ends with live children slightly falling, and turns over about 21 child niches per seed (mean lifetime of a collapsed child 6.9 windows). Diversity stays above the control (0.8753 vs 0.8638).
- 24 windows is short. Whether relief stays bounded over longer runs is untested, and so are the generator's `variantFor` path and live play.

## Longer-horizon check (pre-registered 2026-10-26, before running)

The owner asked for a longer run before adopting relief. no_caps and relief run for 72 windows instead of 24, on commits 7318378 (no_caps + horizon property) and ce52089 (relief + horizon property). The property only changes the window count; the default stays 24.

- **Rule B (bounded niche count):** a seed's mean live children over windows 48 to 71 is no higher than its mean over windows 24 to 47. Shown if this holds in at least 12 of 16 seeds. There is no calculated prediction.
- Also reported: splits, child births and collapses per seed, mean lifetime of collapsed children, maximum and final live children, final Simpson diversity, and the guardrail (largest relative Simpson drop over 25% against the first 3 windows). Live-children numbers detect runaway growth and are not a target.

Summarizer: `scripts/summarize-organic-long.py NO_CAPS RELIEF`.

### Longer-horizon result

GitHub Actions run 40348984997, 16 seeds per arm, 72 windows, all jobs passed.

| Arm | Rule B | Mean live, windows 24-47 | Mean live, windows 48-71 | Max live | Child births / collapses per seed | Final Simpson | Guardrail |
|---|---|---|---|---|---|---|---|
| no_caps | 10/16, not shown | 15.22 | 14.71 | 19 | 127.6 / 113.2 | 0.8575 | 0/16 |
| relief | 11/16, not shown | 12.08 | 11.85 | 20 | 92.5 / 80.9 | 0.8748 | 0/16 |

Mean live children by 12-window block:
- **no_caps:** 8.0, 14.1, 15.7, 14.7, 14.8, 14.6.
- **relief:** 7.2, 12.1, 12.1, 12.1, 11.9, 11.8.

Per-seed changes between the two halves are small:
- **relief:** from -1.08 to +0.50; the five increases are +0.04 to +0.50.
- **no_caps:** from -6.50 to +1.08.

Mean lifetime of a collapsed child is 7.0 windows in both arms. Two relief seeds had no collapse in 72 windows.

Rule B is not shown in either arm. Neither arm grows without bound: both settle on a plateau by about window 24, and the averages fall slightly in the second half. Post hoc observation, not a pre-registered claim: on a flat plateau, the "no higher than" comparison fails on small fluctuations in about a third of seeds. The pre-run guess in this note, that without relief sustained pressure would keep splitting the same parent, was wrong over this horizon: no_caps also levels off, about 3 niches higher. Relief gives the lower, flatter level, the higher diversity, and less churn, while still turning over about 81 child niches per seed.
