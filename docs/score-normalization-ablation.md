# Score normalization ablation

Within-category composite scores now enter the existing mean-relative soft cap
without the gamma curve or blending toward the mean. Both transform constants are
removed from production. The cap's formula, original-score mean, 1.28 ratio and
existing empty/singleton/uniform/nonfinite guards are retained.

For composite scores `[1, 2, 9]`, the old gamma/blend stage raised the lowest score
from `1` to `2.05` before capping. The regression first fails against that behavior.
The replacement preserves both below-mean scores and applies the original cap to
the leader. Preparation leaves the input array untouched.

This is the second selection ablation, after top-three compression. Removing the
soft cap is a separate follow-up. Cold balancing, small-category flattening,
unseen-template overrides and the remaining scoring aids still apply. This change
does not make selection solely outcome-driven.

## Controlled comparison

`ScoreNormalizationAblationTest` restores the gamma/blend policy from prerequisite
`6e886ec742c697af34d46b802c79095ff659014c` only in a test subclass. That control calls
the same production soft-cap method with the same original-score mean. Top-three
compression remains absent in both arms. There is no live restoration flag.

The shared isolated scenario uses 32 artifacts, 12 windows, the first three as each
arm's baseline, real Java generation and utility feedback, and simulated outcomes.
Each arm uses the existing 25% maximum relative diversity-loss setting. The
comparison requires changed score arrays and changed actual generated profiles;
changes to diagnostics alone cannot establish an active ablation. No template,
category or niche is required to reach a named count/share.

The treatment is replayed independently. Both occupancy windows and complete
ordered generated-profile traces (ability ID, mechanic and trigger) must match.
The original health regression retains its empty/monoculture negative controls.
Full validation also reruns the earlier top-three ablation under this new score
pipeline; historical results for that earlier pipeline remain historical.

## Recorded result

Focused validation passed 43 tests across three classes. Full Docker `clean package`
passed **511 tests across 99 classes**, zero failures/errors/skips, Maven exit 0,
29:00. All 514 source/resource/pom hashes matched the frozen snapshot afterward.
The paired focused/full outputs match exactly, including every occupancy window.

Maximum relative diversity loss was **8.02%** with legacy normalization and
**0.84%** without it. The control changed scores at 960 preparation points and
changed 364 generated profiles. Independently replayed treatment windows and
ordered profile traces matched. The preceding top-three ablation also passed in
this new pipeline (4.69% legacy / 0.84% ablated). These are one-seed observations.

## Scope and compatibility

The change applies when a category has multiple eligible templates. The default
implemented pool has one entry per category; enabling Double Jump can place it
alongside Fleetfoot in the mobility category, so their relative selection weights
can change. All entries continue through the shared catalog, regulatory gates,
slot policies and effective permissions; no ability-specific scoring exception is
introduced. Effect strength, cooldowns and execution handlers are outside this
slice.

The production change adds no event interception, XP/stat writes, storage format,
optional plugin dependency or API call. Standalone and integration regression
checks remain part of the full suite. Actual AuraSkills/claims/anticheat server
certification is not supplied by this simulation change.

A one-seed comparison with other aids still present does not establish full
emergence, general improvement, live population diversity, lineage birth and
extinction or open-ended niches. Wild mutation remains held. See the canonical
decision ledger and dated status for the current evidence and remaining work.
