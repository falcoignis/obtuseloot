# Shared recovery: pre-registration

Written and committed before the 40-generation run. The owner chose shared
recovery as the cost of carrying more abilities on 2026-10-24 ("i like 1 and 2").
The second choice, holder-weighted per-target competition, is a separate later
run: it adds no cost to extra abilities, because a parent with two abilities
still scores on both.

## Change

Abilities on one item share recovery. An item is ready only when every recovery
recorded in its existing per-ability ready-time map has elapsed
(`Artifact.abilitiesReadyAt`). Each use still writes its own recovery under its
own ability id, so the storage format is unchanged and no constant is added.

- Material Insight checks the item gate instead of its own entry.
- Native Double Jump checks the item gate and writes its recovery to the item,
  on the persisted ecology clock. When that clock is unavailable (no plugin, as
  in unit tests) only the player's existing cooldown applies, as before.
- Abilities without a recovery (Gentle Harvest, passive speed) neither block nor
  are blocked. They remain a free extra. A player can also avoid the cost by
  carrying two single-ability items. Both gaps are disclosed, not addressed.

Everything else is the recovery-once formula of 42df264.

## Experiment inputs

`IndependentConditionsExperimentTest.sharedRecoveryAblation`. Same as the three
earlier runs: 16 seeds (4722..19722), 64 items per site, zero cues, steady
conditions, measured selection, 40 generations.

- The modeled jump uses the same item gate.
- Under a shared gate, the order of the two activities within a round matters
  for two-ability items. The old loop always tried insight first, which would be
  a fixed priority. The order is now drawn per item and round from the input-seed
  stream, never from the selection random. This is a test input.

## Calculation and regression check (generation 0, done before this document)

`sharedRecoveryGenerationZero` ran one generation on this code and, in a
42df264 worktree, on the same harness with per-ability recovery and insight first.

- The 42df264 harness reproduces the original 42df264 generation-0 births
  exactly (4096/4096 rows).
- Single-ability items' generation-0 activity rows are identical between the two
  builds (5474/5474). Only two-ability items change.
- Mean parent fitness at generation 0:

| Build | Both | Insight only | Jump only | Both / mean specialist | Fitness share of both |
| --- | ---: | ---: | ---: | ---: | ---: |
| Per-ability (42df264) | 11.49 | 9.09 | 2.87 | 1.92 | 0.493 |
| Shared | 7.03 | 9.09 | 2.87 | 1.17 | 0.373 |

The loadout share of two-ability items is about 1/3 (0.336 of candidates).
Two-ability items keep some edge, because either offered activity can fill
their recovery window. Generation-0 births on the shared build were written
but not inspected, and generation 0 is excluded from the rule below.

## Primary rule

For each seed, compute the share of births in generations 1..39 whose parent
has both abilities, on this run and on the 42df264 run
(`target/no-recovery-divisor/parentage.csv`). Founder births are excluded.

A seed passes when the shared-recovery share is lower. **Reduced generalist
advantage is shown** if at least 12/16 seeds pass.

Prediction (calculated from generation-0 fitness, not from selection dynamics):
the share falls from about 0.50 toward 0.37. Gene selection after generation 0
can move it either way.

## Reported against all three earlier formulas (descriptive, no verdict)

- Parent loadout shares over all generations (42df264: 50% two-ability).
- The ceiling rule of `docs/no-recovery-divisor-preregistration.md`
  (`D_kinetic < 0` and `|D_kinetic| > |D_precision|`; 42df264: 9/16).
- Founder Simpson diversity and founders per site at generation 39; precision
  and kinetic variance; mean genes per pool.
- The crop-minus-stone precision gene.

Interpretation, to be checked rather than assumed: on a two-ability item a
higher kinetic gene's longer recovery now also costs insight uses, and higher
precision's longer recovery costs jumps. That coupling may change the gene
results above.

## Tests

The recovery and jump unit tests pass (AirJumpStateTest, EquipmentAbilityServiceTest,
MaterialInsightLivePerformanceTest with a new shared-gate case, MixedAbilityEvolutionTest:
33/33). The native jump's item gate has no unit test with a live ecology clock.
The full suite has not run on this branch; tests pinning the earlier formulas
are expected to fail, as before.
