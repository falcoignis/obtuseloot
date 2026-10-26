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

GitHub Actions run 40269471357, arm commits b807570, ec20c40, 705c819 and b2175f3. The control's windows, children and gates outputs are byte-identical to the #429 run in all 12 files, so the occupancy refactor is behavior-neutral.

| Arm | Splits per seed | Rule S | Rule E | Final Simpson | Final top share | Mean competition | Guardrail |
|---|---|---|---|---|---|---|---|
| control | 0.56 | 0/16 | 2/16 | 0.8518 | 0.096 | 1.86 | 0/16 |
| attributed | 0.62 | 0/16 | 3/16 | 0.8523 | 0.095 | 1.85 | 0/16 |
| count_once | 3.00 | **16/16 shown** | **16/16 shown** | 0.8516 | 0.258 | 2.15 | 0/16 |
| weighted | 2.81 | **15/16 shown** | 11/16 not shown | 0.8638 | 0.157 | 1.96 | 0/16 |

- Attribution alone changes nothing measurable: membership still dilutes share.
- count_once splits at windows 1, 11 and 21 in every seed, exactly when the 60 s global cooldown releases. From window 2, the pressure gate blocks only 82 evaluations; the global cooldown blocks 3,734. Pressure no longer selects, and the cooldown sets the pace.
- weighted mostly follows the same rhythm, but some splits come one or two windows late (windows 2, 3, 12, 13, 22, 23), and the pressure gate still blocks 521 evaluations. It has the highest final Simpson diversity and a smaller rise in competition.
- Full suites: weighted 606 tests, 0 failures, 0 errors, 9 opt-in skipped. count_once 1 failure, in `NicheEcologySystemTest.saturationTrackingUsesRuntimeTelemetry`, which asserts the old membership count.

**Owner decision (2026-10-26): adopt weighted occupancy (option A).** Utility-weighted occupancy, with signal outcomes attributed to their own niches, merges; count-once stays a research arm. The 60 s global cooldown, which classifies as top-down but has not been ruled on, is now the main limit on splitting under either definition. It is the next question for the owner, not a tuning target. The generator's `variantFor` path and live play remain untested.
