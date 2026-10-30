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

Pending the registered Actions run. The preceding local smoke and handler checks
ran 29 tests with zero failures/errors and one opt-in research skip. The research
branch carries only tests, analysis and this temporary workflow; no merge is authorized.
