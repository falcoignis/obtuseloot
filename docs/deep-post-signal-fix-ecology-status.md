# Deep Post-Signal-Fix Ecology Validation

Run ID: `deep-post-signal-fix`

## SECTION 1: SIGNAL VALIDATION

- `TelemetryAggregationBuffer` uses `normalized(event.niche(), event.attributes().get("niche"))`, which prioritizes event-level niche over attribute-level niche.
- Regression test `aggregationPrefersEventNicheOverLegacyAttributeNicheForDynamicChildren` passed.

## SECTION 2: TRUE NICHE DISTRIBUTION

### explorer-heavy
- No child niches (`*_A#`, `*_B#`) detected in `rollup-snapshots.json` windows.

### ritualist-heavy
- No child niches (`*_A#`, `*_B#`) detected in `rollup-snapshots.json` windows.

### gatherer-heavy (parent: `RITUAL_STRANGE_UTILITY`)
- Parent share range across windows: 31.94% → 32.77%.
- Child niche shares at final window:
  - `RITUAL_STRANGE_UTILITY_A1`: 0.18%
  - `RITUAL_STRANGE_UTILITY_B1`: 0.22%
  - `RITUAL_STRANGE_UTILITY_A2`: 0.18%
  - `RITUAL_STRANGE_UTILITY_B2`: 0.20%
- Total child share (final): 0.78%.
- Time-series child-total share (w1→w10): 0.35%, 0.56%, 0.65%, 0.69%, 0.72%, 0.75%, 0.76%, 0.76%, 0.78%, 0.78%.

### mixed
- No child niches (`*_A#`, `*_B#`) detected in `rollup-snapshots.json` windows.

### random-baseline (parent: `RITUAL_STRANGE_UTILITY`)
- Parent share range across windows: 33.03% → 33.57%.
- Child niche shares at final window:
  - `RITUAL_STRANGE_UTILITY_A1`: 0.19%
  - `RITUAL_STRANGE_UTILITY_B1`: 0.21%
  - `RITUAL_STRANGE_UTILITY_A2`: 0.17%
  - `RITUAL_STRANGE_UTILITY_B2`: 0.20%
- Total child share (final): 0.77%.
- Time-series child-total share (w1→w10): 0.33%, 0.58%, 0.65%, 0.68%, 0.71%, 0.74%, 0.75%, 0.76%, 0.77%, 0.77%.

## SECTION 3: CHILD NICHE GROWTH

- Growth is present where child niches appear:
  - gatherer-heavy child total rises from 0.35% to 0.78%.
  - random-baseline child total rises from 0.33% to 0.77%.
- No growth signal in explorer-heavy, ritualist-heavy, or mixed due to no child niche appearance.

## SECTION 4: PARENT SHARE SHIFT

- In child-active scenarios, parent share remains dominant and generally stable/slightly increasing:
  - gatherer-heavy parent share 31.94% → 32.77% while child-total rises to 0.78%.
  - random-baseline parent share 33.03% → 33.57% while child-total rises to 0.77%.
- Result: measurable child emergence is present, but strong parent-share redistribution is not observed.

## SECTION 5: LINEAGE SPECIALIZATION

Final-window lineage concentration (top lineage and share in each child niche):

### gatherer-heavy
- `RITUAL_STRANGE_UTILITY_A1`: `wild-4233` (27.37%)
- `RITUAL_STRANGE_UTILITY_A2`: `wild-4233` (29.47%)
- `RITUAL_STRANGE_UTILITY_B1`: `wild-4233` (27.44%)
- `RITUAL_STRANGE_UTILITY_B2`: `wild-4233` (23.08%)

### random-baseline
- `RITUAL_STRANGE_UTILITY_A1`: `wild-4233` (19.87%)
- `RITUAL_STRANGE_UTILITY_A2`: `wild-4233` (25.90%)
- `RITUAL_STRANGE_UTILITY_B1`: `wild-4233` (23.50%)
- `RITUAL_STRANGE_UTILITY_B2`: `wild-4233` (17.17%)

Interpretation:
- Child niches are multi-lineage (9–13 active lineages in final window).
- A/B clustering by distinct lineage groups is weak; the same top lineage dominates both A and B sides.

## SECTION 6: ECOLOGICAL STRUCTURE

Per scenario structure summary:

- explorer-heavy: peak child share 0.00%, final 0.00%, active child niches 0, >1% no, >3% no, >5% no.
- ritualist-heavy: peak child share 0.00%, final 0.00%, active child niches 0, >1% no, >3% no, >5% no.
- gatherer-heavy: peak child share 0.78%, final 0.78%, active child niches 4, >1% no, >3% no, >5% no.
- mixed: peak child share 0.00%, final 0.00%, active child niches 0, >1% no, >3% no, >5% no.
- random-baseline: peak child share 0.77%, final 0.77%, active child niches 4, >1% no, >3% no, >5% no.

## POST-FIX ECOLOGY STATUS

**PARTIAL**

Rationale against success criteria:
- Child total share is >0 and grows across windows in two scenarios.
- No child niche exceeds 1% (therefore >3% and >5% are also unmet).
- Parent-share redistribution is weak; parent niches remain near or above initial share levels.
