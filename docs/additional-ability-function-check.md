# Generated ability use and local inheritance check

Owner request: "now lets run our check with the additional abilities and see how
things function". Research uses PR #435 head `48553526e3020c41c2c8f1cf2790eb3b1e5d5c0b`,
including #434, without merging either PR. No production code, coefficients,
permissions, wild mutation or live server is changed.

## Registered protocol, before the Actions run

Eight jobs cross selectable catalog (three-ability control / seven-ability expanded),
production measured parent selection / equal-chance eligible-parent control, and
two independent JVM repeats. Each job contains seeds 4822, 5822, 6822 and 7822,
both steady and changing worlds, four nearby sites, eight items/site, 16 generations
and 12 rounds/generation at 20 game ticks. This is 32,768 descendant births plus
2,048 initial founders across all eight jobs.

The live `SeededAbilityResolver` creates profiles from actual item genomes. No
loadout, ability quota or gene grid is assigned. New loot uses the production
local sampler, inherited-affinity drift and ordinary genome mutation. Separate
deterministic item-identity streams retain the replay fix; each scenario isolates
and restores the singleton's recent-profile history.

Each site offers waypoint gestures, block inspections, animal inspections and world
scans in a site-specific cyclic order. Each opportunity is available with probability
0.75 from a separate counter-keyed SplittableRandom stream (study seed, generation,
site, birth slot, round, input kind). It does not consume parent-choice draws or
depend on genes, loadouts or observed results. Items advance 40 blocks/round; each
generation's return point can therefore test the inherited range at 0..440 blocks.
Round zero offers a mark, later rounds offer recall. Missing the mark can cause a
real no-op. Sites are 64 blocks apart inside the default 20-chunk neighborhood;
their breeding origins follow their prescribed travel.

Even sites start with repeater/light-12, young animals and cycling storm weather;
odd sites have stone/light-2, adults and paused clear weather. The changing run
swaps those conditions at generation eight; travel and inputs stay prescribed.
Reported animal/forecast countdown changes preserve existing observation identities.
Weather's registry-backed game-rule read is the same test-only substitution used
by the current effect tests; other API reads and the actual handlers execute.

Only abilities present in a generated profile execute. Actual handler successes
and no-ops enter the existing revision-fitness classifier and local pool. Shared
per-item recovery, applicability and producing revision are retained. Suppression,
missing abilities and absent opportunities earn no invented benefit. Offspring
start with empty recovery, waypoint and fitness state. Codec round trips preserve
owned state; evidence expiry removes breeding evidence without deleting owned items.

The neutral control maps the same selector draw onto equal chances among current,
positive, habitat/holder-filtered candidates; production selection is unchanged.
Every parent must be in the production candidate list. Empty pools use the actual
fresh-founder fallback. Holder-target weights keep exact block-material semantics;
animal/weather/navigation context is not given a new descriptor for this study.

## Required controls and reports

- All four CSVs reproduce byte for byte across independent jobs.
- Every prescribed input matches across catalogs and selection controls.
- Initial founder identities, ancestry and genes match across arms; generated
  abilities may differ with the catalog.
- Steady/changed histories match before generation eight for every seed and CSV.
- Birth/input/generation samples are complete; every attempt is accounted for as
  suppression, success or no-op. Sampler totals and fitness remain finite.
- Separate per-ability opportunities, attempts, suppression, benefit, recovery,
  failures and actual-output digests; final generated ability sets, ancestry,
  parent concentration, founder fallback and gene means. Benefit units remain separate.
- Report the existing first-three-window / 25% relative Simpson-loss signal for
  each seed/site. A zero baseline is not assessable. Distribution changes do not
  fail the experiment or justify tuning weights; a guardrail breach is reported.

The functional job also runs current handler/protection/recovery, equipment,
revision/inheritance and original seeded-diversity tests. The old diversity scenario
uses synthetic outcomes and the complete offline catalog; it is a separate regression,
not evidence that the added live handlers maintain ecosystem health.

## Limits

This is a controlled API-double scenario with fixed cohort replacement and a
prescribed breeding schedule. Parent evidence expires between generations; owned
profiles survive. It does not simulate natural item retirement, open-ended dynamic
niche birth/extinction, permanent item-stage evolution, player learning or native
protection/AuraSkills coexistence. It bypasses the wall-clock trigger-budget layer
and listener/equipment event transport; existing tests check those boundaries.

There are no crop-harvest opportunities in this scenario. Gentle Harvest therefore
receives no use evidence here; its real handler is checked separately. Passive
Fleetfoot has no measured outcome and is not credited invented speed utility.
Double Jump stays disabled. Missing these measurements prevents a global catalog
fitness or balanced-ecosystem verdict. Different benefit units are not subjective
player value. Native gameplay remains a separate disposable-server check.

## Results

Actions run [40999403843](https://github.com/falcoignis/obtuseloot/actions/runs/40999403843)
on `4949b22798f89b45b9f87484bb4bd1ef24485c5d` passed all ten jobs: eight independent
cohort jobs, the functional regression job and comparison. All **88** registered
replay/alignment/prefix checks passed. The sample contains all 32,768 descendant
births and 2,048 founders. All ten artifact ZIPs match published SHA-256 digests;
a separately capped local recomputation reproduced every summary field.

The functional job ran **109 tests, zero failures/errors, one opt-in research skip**
across eleven classes. Its original offline seeded-diversity regression passed
(baseline Simpson 0.837890625, lowest later 0.849609375, relative loss zero). That
regression remains separate from the generated live-handler scenario. Local smoke:
29 tests, zero failures/errors, one opt-in skip. The first local compile found an
ambiguous test import, corrected before the registered run; its log is retained.

### Real effects execute, with frequent unavailable/suppressed uses

Counts below combine four seeds and all observed generations in the measured
expanded arm. They describe actual handler results under the supplied API inputs,
not player usefulness. Each table column has 2,048 descendant births in its scenario.

| Added ability | Steady successes | Changed-world successes | Steady suppressions | Steady no-ops |
|---|---:|---:|---:|---:|
| Footprint Memory | 3,009 | 3,024 | 7,064 | 7,192 |
| Weather Omen | 40 | 34 | 154 | 0 |
| Herd Instinct | 110 | 110 | 290 | 0 |
| Redstone Sympathy | 146 | 139 | 576 | 348 |

The raw reports retain opportunities and attempts too; an absent ability/input is
not a failure. Producing revisions, finite sampling, candidate membership, codec
state, offspring state and evidence expiry assertions passed. Shared recovery was
retained; the experiment bypasses wall-clock budgets. It does not identify player
value or a complete native activation rate.

### More sets coexist with a dominant pair

The final offspring cohort has 128 items per row (four seeds x four sites x eight).
These offspring follow generation 15's uses and have not themselves been used.

| Catalog / sampler | World | Final distinct sets | Largest set share | Final mean effective parents | All descendant founder fallbacks |
|---|---|---:|---:|---:|---:|
| Control / measured | Steady | 2 | 57.03% | 17.49 | 0 |
| Control / measured | Changing | 2 | 57.03% | 16.61 | 0 |
| Expanded / measured | Steady | 7 | 92.19% | 10.60 | 8 |
| Expanded / measured | Changing | 8 | 90.63% | 14.44 | 24 |
| Expanded / neutral | Steady | 8 | 91.41% | 11.59 | 0 |
| Expanded / neutral | Changing | 8 | 91.41% | 10.40 | 0 |

The expanded largest set is **Fleetfoot + Footprint Memory** (118/128 steady,
116/128 changed, 117/128 in each neutral control). All four added effects had real
successes on generated loadouts, but their addition did not remove concentration.
It persists under equal chances among **eligible** parents, so weighted parent
choice alone is not the whole explanation. Eligibility and holder/habitat filters
remain outcome-conditioned in that control. This does not diagnose the generator
or justify a coefficient change.

Mean current-cohort lineage count ends at 1.0625/1.125 per site for the measured
expanded steady/changed runs. These are fixed-size cohort ancestry contractions,
not deletion of owned items or demonstrated native lineage extinction. The fresh
founder fallback occurred in the measured expanded scenario; every inherited birth
still selected a valid current candidate.

### The registered niche metric cannot establish health here

All 16 control seed/site cells and 12/16 expanded cells have zero early diversity
under the single-dominant-generated-niche metric, so their relative signal is
**not assessable**, not a pass. The other four expanded cells breach the registered
25% relative-loss signal in both measured scenarios and both neutral scenarios.
All final observed cohorts have zero dominant-niche Simpson. The original offline
regression's passing result must not be substituted for this result.

Supplementary **post hoc** ability-set Simpson, computed directly from birth
profiles for the observed cohorts, has nonzero early diversity in all cells.
For example, the measured control mean baseline is 0.4102 although its dominant-niche
baseline is zero. This exposes the metric's coarse aggregation; it does not replace
the registered rule or create a new passing verdict. In the measured expanded
scenario, observed-cohort mean ability-set Simpson is 0.2865 early and 0.2500/0.2246
at the last observed steady/changed cohort. The raw supplemental data and method are
preserved in the review archive.

**Conclusion:** mechanical execution, durable state and measured local reproduction
work in this controlled check. A healthy or balanced emergent ecosystem is **not
established**. The remaining work is to isolate generation/eligibility/recovery
concentration with controlled mechanism checks, not to force a desired distribution.
The temporary workflow is removed after preserving its source commit. No production
change, merge, release or live deployment is made by this research task.
