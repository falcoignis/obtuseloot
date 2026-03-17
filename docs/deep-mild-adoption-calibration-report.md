SECTION 1: ACTIVE ADOPTION LEVERS

Active levers identified in code:
- Bifurcation pressure thresholds and gating in `NicheBifurcationRegistry`:
  - saturation threshold
  - minimum parent share gate
  - sustained windows
  - cooldown and dynamic niche caps
- Migration/displacement in `NichePopulationTracker`:
  - forced migration target/min/max rates from below-mean parent performers
  - soft migration pressure cap/chance for underperformers in crowded parents
- Lock-in behavior in `NichePopulationTracker`:
  - bounded lock duration window range
  - lock utility reinforcement multiplier
- Inversion multipliers in `NichePopulationTracker`:
  - child post-bifurcation boost
  - parent post-bifurcation penalty
  - decay horizon and clamped bounds
- Relative fitness/competition model in `NichePopulationTracker`:
  - share/saturation/density weighted competition factor (bounded)
- Child balancing logic in `NichePopulationTracker`:
  - underrepresented-child bias when child populations are imbalanced
- Child collapse logic in `NicheBifurcationRegistry`:
  - minimum child count and zero-window collapse horizon

Levers that were too weak for target adoption based on pre-pass outcome:
- Forced migration and soft migration were not pushing enough mass into children.
- Child reinforcement and inversion ceilings were conservative.
- Lock windows were short, making child occupancy transient.
- Balanced-child underpopulation bias was modest.

SECTION 2: CALIBRATION CHANGES

Applied mild-but-stronger calibration (no architecture changes, no new systems):

- `NichePopulationTracker`:
  - Forced migration:
    - rate: `0.40 -> 0.58`
    - min/max band: `0.30-0.50 -> 0.50-0.70`
  - Locking:
    - lock windows: `2-4 -> 4-6`
    - lock utility boost: `1.12 -> 1.30`
  - Inversion:
    - parent multiplier target: `0.93 -> 0.88`
    - child multiplier target: `1.12 -> 1.30`
    - clamp ranges:
      - child: `1.05-1.15 -> 1.10-1.32`
      - parent: `0.90-0.97 -> 0.86-0.95`
      - lock+inversion effective: `1.05-1.20 -> 1.10-1.35`
  - Underpopulated-child balancing:
    - bias toward underrepresented child: `0.65 -> 0.85`
  - Soft migration pressure:
    - migration cap: `4% -> 8%`
    - migration chance envelope raised (`max 0.18 -> 0.30`) and intercept/slope increased.

- `NicheBifurcationRegistry`:
  - Saturation threshold eased: `0.08 -> 0.06`
  - Parent share gate eased: `0.08 -> 0.06`
  - Child collapse zero-window horizon: `2 -> 3`

Safety maintained:
- Max dynamic niche cap unchanged.
- Per-parent/global cooldown unchanged.
- Migration remains bounded by explicit min/max and caps.
- Lock windows remain bounded.

SECTION 3: FILES MODIFIED

- `src/main/java/obtuseloot/evolution/NichePopulationTracker.java`
- `src/main/java/obtuseloot/evolution/NicheBifurcationRegistry.java`
- `src/test/java/obtuseloot/evolution/NicheEcologySystemTest.java`

SECTION 4: POST-RUN CHILD NICHE SHARES

Run: `deep-mild-adoption-calibration`

- explorer-heavy
  - child total share by window: `0.09, 0.48, 0.58, 0.65, 0.69, 0.70, 0.71, 0.72, 0.73, 0.73`%
  - peak child share: `0.73%`
  - final child share: `0.73%`
  - exceeds 1%: no
  - exceeds 3%: no
  - exceeds 5%: no

- ritualist-heavy
  - child total share by window: `0.07, 0.42, 0.52, 0.58, 0.63, 0.66, 0.68, 0.69, 0.71, 0.71`%
  - peak child share: `0.71%`
  - final child share: `0.71%`
  - exceeds 1%: no
  - exceeds 3%: no
  - exceeds 5%: no

- gatherer-heavy
  - child total share by window: `0.09, 0.46, 0.58, 0.63, 0.67, 0.69, 0.71, 0.72, 0.73, 0.74`%
  - peak child share: `0.74%`
  - final child share: `0.74%`
  - exceeds 1%: no
  - exceeds 3%: no
  - exceeds 5%: no

- mixed
  - child total share by window: `0.18, 0.43, 0.52, 0.57, 0.61, 0.63, 0.64, 0.65, 0.66, 0.67`%
  - peak child share: `0.67%`
  - final child share: `0.67%`
  - exceeds 1%: no
  - exceeds 3%: no
  - exceeds 5%: no

- random-baseline
  - child total share by window: `0.06, 0.46, 0.62, 0.70, 0.75, 0.78, 0.79, 0.82, 0.84, 0.85`%
  - peak child share: `0.85%`
  - final child share: `0.85%`
  - exceeds 1%: no
  - exceeds 3%: no
  - exceeds 5%: no

SECTION 5: PARENT SHARE SHIFT

Parent of observed bifurcation lineage in this run: `RITUAL_STRANGE_UTILITY`.

- explorer-heavy: `32.32% -> 33.12%` (`+0.79pp`)
- ritualist-heavy: `29.27% -> 29.54%` (`+0.26pp`)
- gatherer-heavy: `32.71% -> 33.22%` (`+0.51pp`)
- mixed: `27.25% -> 27.94%` (`+0.69pp`)
- random-baseline: `37.94% -> 38.83%` (`+0.89pp`)

Interpretation: parent penalty is still not strong enough to produce net post-bifurcation parent decline.

SECTION 6: LINEAGE SPECIALIZATION CHANGE

Lineage concentration within child niches became more distinct in coverage and segmentation:
- Child niches are now active in all five scenarios (previously only two scenarios had any child share).
- Final-window top-lineage dominance in child niches ranges roughly from ~13% to ~39% depending on scenario and child branch, with scenario-specific dominant lineages (e.g., `wild-4233`, `graveborn`, `wild-35642`, `wild-65015`).
- Distinct A/B child branches frequently show different dominant lineages in the same scenario, indicating improved specialization separation across sibling child niches.

SECTION 7: STABILITY CHECK

Safety review after calibration:
- No niche explosion observed in run outputs.
- Lock duration remains bounded (`4-6` windows).
- Migration remains bounded (forced min/max band and soft migration cap).
- Parent niches remain viable and dominant.
- Child collapse mechanism remains active (minimum child viability and zero-window collapse horizon retained).

SECTION 8: CALIBRATION RESULT

Outcome vs target (1-5% child adoption):
- Target not reached yet (best peak `0.85%`).
- However, child growth slope and breadth improved materially vs baseline:
  - all scenarios show non-zero child adoption trajectories with monotonic growth.
  - random-baseline improved from `0.77%` to `0.85%` peak/final.

CALIBRATION RESULT

PARTIAL
