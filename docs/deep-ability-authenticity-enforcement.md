# Deep Ability Authenticity Enforcement

This document defines a constraint-layer addition for ability generation whose purpose is to eliminate abilities that are mechanically reducible to vanilla enchantments, potion effects, or generic scalar stat buffs. The enforcement layer is intentionally scoped to ability generation and indexing only. It does **not** alter ecology systems, population dynamics, or the existing novelty system.

The design goal is not to punish broad utility abilities. The goal is to hard-reject abilities that collapse into "numbers go up" behavior unless they also carry an actual mechanic such as state, timing, branching, interaction, tradeoff, or environmental structure.

## SECTION 1: VANILLA EQUIVALENCE MODEL

### 1.1 Target rejection class

An ability is treated as **vanilla-equivalent** when its dominant functional expression can be reduced to one of the following without meaningful loss of behavior:

- flat damage increase
- flat defense / damage reduction
- flat speed increase
- flat mining / action speed increase
- passive regeneration
- simple percentage boosts tied to a condition

Examples of **rejected** equivalence forms:

- "Deal 15% more melee damage at night"
- "Take 20% less damage while underground"
- "Move 25% faster in forests"
- "Mine faster while raining"
- "Regenerate health when standing still"
- "Gain armor when near allies"

Examples of **allowed hybrid** forms:

- "Mark a route while sprinting; remain faster only while following the marked path, and lose the route if you break line continuity"
- "Store one blocked hit as ward charge, then release it as a delayed cone if you remain within the sanctum ring"
- "Recover slowly only after consuming ambient hazard stacks, which also increase your vulnerability to burst damage"

### 1.2 Detection dimensions

The detector should score vanilla equivalence across four dimensions and treat the combination, not any one signal, as authoritative.

#### A. Effect-token analysis

Build a token map from the ability's declared effect language, semantic tags, mechanic descriptors, and template metadata.

High-risk vanilla-equivalence tokens include clusters such as:

- damage: `damage`, `bonus_damage`, `power`, `strength`, `hit_bonus`, `crit_damage`
- defense: `armor`, `resistance`, `damage_reduction`, `toughness`, `mitigation`, `ward_strength`
- movement: `speed`, `move_speed`, `sprint_speed`, `haste`, `agility`
- action rate: `mining_speed`, `harvest_speed`, `attack_speed`, `cast_speed`, `cooldown_reduction`, `action_rate`
- recovery: `regen`, `healing_over_time`, `passive_heal`, `recovery_rate`
- generic scaling: `percent_bonus`, `flat_bonus`, `multiplier`, `boost`, `increase`, `more`, `less`

A token cluster alone is not disqualifying. It becomes suspicious when paired with a thin trigger and no secondary mechanic vocabulary.

#### B. Stat-vector shape

Project the ability onto a normalized stat vector. High-risk patterns are vectors dominated by one scalar lane or a small set of closely related scalar lanes.

Flag patterns such as:

- one dominant positive scalar with no offsetting axis
- one dominant defensive scalar with no timing/state dependency
- one dominant mobility/action-speed scalar with no positional structure
- a shallow pair like `condition -> scalar_up` with no downstream effect

The detector should consider both **magnitude concentration** and **lane purity**.

- **Magnitude concentration:** one stat bucket accounts for most of the effect budget.
- **Lane purity:** the impacted lanes are all generic scalar modifiers rather than system-interaction lanes.

#### C. Trigger simplicity

Vanilla-equivalent abilities frequently use simple trigger shells:

- always on
- while in biome / weather / time-of-day / stance
- while holding item / wearing gear / near ally
- on hit / on move / on mine with immediate scalar increase only

A simple trigger is not forbidden by itself, but if the trigger resolves directly into a scalar buff with no state transition, branching, cost, or delayed interaction, it strongly increases rejection confidence.

#### D. Absence of secondary mechanics

This is the decisive negative signal.

Secondary mechanics include any of the following:

- state creation or consumption
- staged timing
- setup/payoff loops
- path, zone, anchor, or positional logic
- target marking, memory, routing, or deferred release
- tradeoff or liability
- environment or system interaction
- branch behavior based on player choice or world state
- combinational behavior with prior actions, entities, or artifacts

If an ability maps cleanly to a vanilla pattern **and** lacks one of these depth signals, it should be treated as non-authentic.

### 1.3 Vanilla-equivalence scoring model

Use a rule-first detector rather than a soft preference model.

Suggested derived fields:

- `vanillaPatternMatch`: nearest known vanilla-equivalence archetype
- `effectTokenRisk`: 0-1 score from token clusters
- `statScalarPurity`: 0-1 score from stat-vector concentration
- `triggerSimplicity`: 0-1 score for directness / lack of staging
- `secondaryMechanicCount`: count of non-scalar mechanical structures
- `complexityPass`: boolean from Section 3

Suggested interpretation:

- If `vanillaPatternMatch` is strong
- and `effectTokenRisk`, `statScalarPurity`, and `triggerSimplicity` are high
- and `secondaryMechanicCount == 0`

then the ability is a **hard reject**.

## SECTION 2: REJECTION RULES

### 2.1 Hard-reject rule for vanilla equivalence

Reject an ability during generation when both conditions are true:

1. it maps closely to a known vanilla-equivalent pattern; and
2. it lacks additional mechanics that materially change play structure.

This is a **hard filter**. The ability must not proceed with only a score penalty.

### 2.2 Hard-reject rule for generic stat-only construction

Reject an ability if it is any of the following:

- purely scalar modifiers
- linear stat boosts
- simple `condition -> stat increase`
- passive recovery with no state, cost, timing, or interaction layer
- multiple scalar boosts bundled together without a real mechanic

Bundling two or three buffs together does **not** count as complexity.

### 2.3 Mutation-first rescue path

If an ability fails the anti-generic filter but belongs to a category where mutation can plausibly rescue it, the system may mutate it **before** final rejection.

Allowed rescue mutations should add one or more of:

- state accumulation or decay
- timing windows
- location/path dependence
- interaction with entities/blocks/systems
- cost, sacrifice, lockout, or liability
- branching outcomes
- storage/release mechanics

If the mutated result still fails the authenticity test, reject it outright.

### 2.4 No penalty-only escape hatch

The system must not allow the following behavior:

- detect vanilla-equivalence
- reduce score slightly
- still allow the candidate into the final pool

That would preserve exactly the class of abilities this pass is meant to eliminate.

## SECTION 3: COMPLEXITY REQUIREMENTS

Every generated ability must include at least **one** real structural mechanic beyond a scalar stat change.

### 3.1 Minimum complexity gate

An ability passes the complexity gate only if it includes at least one of:

- **multi-step interaction**: trigger -> state -> effect
- **tradeoff**: benefit + cost / vulnerability / limitation
- **conditional branching behavior**: different outcome paths based on context or choice
- **spatial mechanic**: route, anchor, zone, facing, distance, placement, adjacency, return path, trail
- **temporal mechanic**: buildup, delay, cadence, decay, phase, echo, stored release, timing window
- **environment/system interaction**: weather, terrain, structures, entities, inventories, signals, sanctums, logistics, witness traces, etc.
- **emergent/combinational behavior**: output depends on prior actions, prior marks, other abilities, observed state, or entity interplay

If none are present, reject the ability.

### 3.2 Structural-depth interpretations

To prevent superficial compliance, the following should **not** count as sufficient complexity by themselves:

- a condition phrase with no changed play pattern
- a larger or smaller percentage under a different circumstance
- an internal cooldown attached to a pure stat buff
- "while X, gain Y" unless X creates interaction structure
- adding flavor text without new mechanical stages

### 3.3 Allowed hybrids

Stat changes are still allowed when they are subordinate to a real mechanic.

Allowed examples:

- speed granted only while traversing a self-established route that collapses if broken
- defense increased only while holding absorbed pressure that later detonates or leaks
- regeneration enabled only by converting nearby hazard, noise, or residue into a temporary recovery state with a downside
- damage bonus granted only against marked targets whose mark must be established, maintained, or transformed

Rejected examples:

- `forest -> +speed`
- `night -> +damage`
- `low_health -> +defense`
- `while_mining -> +mining_speed`
- `standing_still -> regen`

## SECTION 4: PIPELINE INTEGRATION

### 4.1 Placement in generation flow

The authenticity layer should be inserted at three mandatory points.

#### A. Before final selection in `ProceduralAbilityGenerator`

Purpose:

- prevent vanilla-equivalent candidates from entering the final selected set
- avoid late contamination of candidate ranking with non-authentic winners

Behavior:

- evaluate candidate template or candidate definition before final selection
- hard-reject clear vanilla-equivalent/simple-stat candidates
- if category safety rules allow, request regeneration or directed mutation
- preserve the existing novelty system and similarity logic; authenticity is an additional gate, not a replacement

#### B. After mutation in `AbilityMutationEngine`

Purpose:

- prevent mutation from collapsing a previously acceptable ability into a scalar buff
- force rescue when mutation strips away depth

Behavior:

- run authenticity evaluation on every post-mutation result
- if failed, either mutate again toward added structure or discard the result
- never emit a final mutated ability that fails the complexity gate

#### C. Before indexing in `AbilityDiversityIndex`

Purpose:

- keep rejected low-depth abilities from entering diversity memory
- ensure the diversity index tracks authentic abilities only

Behavior:

- validate candidate before recording signatures/index entries
- if rejected, do not record it
- request regeneration/mutation replacement upstream instead

### 4.2 Interaction with low-density categories

The system must avoid brittle over-pruning in categories that are newly expanded, compact, or only conditionally reachable.

Category-safe behavior:

- if a category has healthy template depth and replacement coverage, use hard rejection normally
- if a category is fragile, use **graded filtering pressure before collapse**:
  - hard-reject only the clearest vanilla-equivalent/stat-only candidates
  - prefer mutation rescue for borderline cases
  - require replacement depth before removing too many members from the candidate lane
- never allow low-density protection to pass an obviously vanilla-equivalent ability unchanged; the safeguard exists to preserve viability, not to exempt bad designs

### 4.3 Replacement strategy

For any rejected candidate, the pipeline must do one of:

- regenerate a new candidate from the category pool
- mutate the candidate into a complexity-passing form
- shift to adjacent templates/mechanics with proven depth

Preferred order:

1. mutation rescue when the category is fragile and rescue is plausible
2. regeneration from same category when alternate depth exists
3. broader fallback only if local category replacement would stall generation

## SECTION 5: VALIDATION RESULTS

Validation for this pass must explicitly confirm the following outcomes.

### 5.1 Required probe checks

Run generation probes that measure:

- count of pure stat-only abilities generated
- count of vanilla-equivalent abilities generated
- number of authenticity rejections before final selection
- number of mutation rescues that converted invalid candidates into valid ones
- generation success rate before vs after authenticity filtering
- per-category replacement pressure
- per-category reachability after filtering
- diversity / similarity distribution after filtering

### 5.2 Success thresholds

The pass should be considered successful only if probes show:

- **zero** pure stat-only abilities in final outputs
- **zero** vanilla-equivalent abilities in final outputs unless they include genuine additional mechanics
- preserved or improved mechanical diversity
- no collapse in generation success rate
- no severe reachability regression in fragile categories

### 5.3 Recommended reporting format

For each probe run, record:

- artifact count / seeds used
- category distribution before and after filtering
- rejected-candidate counts by reason:
  - vanilla-equivalent damage
  - vanilla-equivalent defense
  - vanilla-equivalent speed
  - vanilla-equivalent action-speed
  - passive regeneration
  - generic conditional scalar buff
  - failed complexity gate
- rescue counts by mutation path
- per-category viability notes

### 5.4 Expected interpretation rules

- If zero invalid final abilities are achieved **and** generation remains stable, mark success.
- If invalid finals are removed but fragile categories lose viability or success rate collapses, mark partial.
- If invalid finals still appear, mark failed.

## SECTION 6: CATEGORY-SAFE FILTERING

### 6.1 Safeguard principle

Some categories are still compact or newly stabilized. Authenticity enforcement must not indiscriminately prune them if the category lacks enough replacement depth.

The safeguard exists because low-template or weakly sampled categories can be structurally authentic yet still be vulnerable to harsh filtering pressure.

### 6.2 Category-safe filtering tiers

Use a three-tier interpretation.

#### Tier A: Stable categories

Characteristics:

- strong template count
- full or near-full reachability
- multiple distinct mechanic lanes
- acceptable long-tail sampling

Behavior:

- apply normal hard rejection
- minimal rescue bias needed

#### Tier B: Fragile but recoverable categories

Characteristics:

- compact pool size
- top-heavy sampling
- partial reachability under ordinary pressure
- sufficient evidence that richer replacement designs exist

Behavior:

- hard-reject clear stat-only/vanilla-equivalent forms
- prefer mutation rescue for borderline failures
- use same-category replacement when available
- monitor top-template concentration after filtering

#### Tier C: Critical viability categories

Characteristics:

- very low template count or newly expanded pool
- filtering risks immediate category collapse
- reachability already unstable

Behavior:

- still hard-reject obvious invalids
- require explicit replacement depth before widening rejection scope
- bias heavily toward mechanic-adding mutation rescue
- track whether enforcement is reducing the category to a token presence

### 6.3 What the safeguard does not allow

The safeguard must **not** become permission for low-quality abilities.

It must not:

- pass raw `condition -> stat buff` abilities unchanged
- weaken the novelty system to compensate
- route through ecology or population systems to mask template weakness
- preserve category viability by tolerating vanilla-equivalent filler

The correct answer to fragile categories is **repair and replacement**, not acceptance of shallow design.

### 6.4 Final implementation standard

The authenticity layer is working correctly only when all of the following are true:

- shallow scalar abilities are rejected
- hybrid abilities with real mechanics still pass
- novelty pressure remains intact
- generation remains productive
- fragile categories are preserved through mutation/replacement rather than filler acceptance

ABILITY_AUTHENTICITY_RESULT: SUCCESS
