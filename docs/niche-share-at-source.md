# Niche population share at its source

`docs/niche-split-gate-diagnosis.md` found that no niche splits a second time because population share is diluted. Every niche counts an artifact as a member if any of its recorded signals touches that niche, and memberships per artifact climb from 2.6 to 10.3 of 14 tags. Saturation is `clamp(share - 0.14)`, so it fails for the same reason. The same `rollups()` also gave each niche the artifact's whole signal map, so attempts, yield and utility density were nearly the same pool in every niche. This note pre-registers a comparison of share definitions, changed at their source, before the arms run. The owner approved the design on 2026-10-26.

## Arms

Every share reader (bifurcation gate, competition factor, saturation model, support allocation) divides rollup `occupancy` by total occupancy. Thresholds are unchanged in every arm.

| Arm | Commit | Change |
|---|---|---|
| control | b807570 | Occupancy equals member count, so behavior matches main. Its windows, children and gates outputs must equal the #429 run byte for byte. |
| attributed | ec20c40 | Each signal's attempts, outcomes and utility count only toward the niches that signal belongs to. Membership is unchanged, so this arm separates attribution from share. |
| count_once | 705c819 | attributed, plus each artifact counts once, in its dominant niche. |
| weighted | b2175f3 | attributed, plus each artifact's occupancy is split across its niches by utility mass, summing to one. Member counts are unchanged. |

## Harness

`NicheScaffoldingAblationTest#nicheScaffoldingAblation`: 16 seeds, 48 artifacts, 24 windows of 6 s on a synthetic clock, with simulated outcomes. It now also writes the per-window mean and maximum competition factor. Because of the cooldowns, a second split can come at window 11 at the earliest. The generator's `variantFor` path and live play are not exercised. The runs execute on GitHub Actions.

## Pre-registered rules

Each rule is shown if it holds in at least 12 of 16 seeds. There is no calculated prediction.
- **Rule S (second splits possible):** more than one bifurcation within 24 windows.
- **Rule E (extinction possible):** at least one child-niche collapse.

Also reported, not rules: splits per seed, final Simpson diversity over effective niches, final top share, evaluations with saturation above zero from window 2 on, final mean competition factor, and the guardrail count (largest relative Simpson drop over 25% against the first 3 windows). The guardrail is never used for tuning.

Summarizer: `scripts/summarize-niche-share.py CONTROL ATTRIBUTED COUNT_ONCE WEIGHTED`.

## Result

RESULT_PENDING
