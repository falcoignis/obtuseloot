# SECTION 1: CATEGORY DEPTH AUDIT

Audit basis:
- Template count per category in `AbilityRegistry`.
- Mechanical distinctiveness from trigger span, mechanic span, family span, and pairwise signature overlap using the same inputs that feed novelty/motif pressure (`category`, `family`, `trigger`, `mechanic`, effect tokens, utility domains, trigger classes, affinities, and metadata vectors).
- Anti-vanilla collapse risk defined as: too few templates, too much shared signature space, or too little trigger/mechanic spread to survive later authenticity enforcement.

## Pre-pass classification

| Category | Pre-pass template count | Pre-pass assessment | Why |
| --- | ---: | --- | --- |
| Ritual / strange utility | 13 | STRONG | Broad trigger + mechanic spread already existed and category identity was very specific. |
| Traversal / mobility | 8 | STRONG | Good routing/exploration spread with multiple movement-adjacent mechanic shapes. |
| Survival / adaptation | 8 | SHALLOW | Count was acceptable, but several entries skewed toward generic environmental support instead of deeper scarcity / shelter / attrition adaptation loops. |
| Sensing / information | 5 | SHALLOW | Category existed, but interpretation depth was narrow and overly scan/inspect-heavy; weak hidden-state coverage. |
| Defense / warding | 5 | ADEQUATE | Narrower count, but threshold / perimeter / sanctum identity was coherent and non-generic. |
| Resource / farming / logistics | 5 | ADEQUATE | Smaller pool, but templates already covered farming streaks, convoy routing, and throughput monitoring. |
| Social / support / coordination | 5 | ADEQUATE | Modest count, but multi-player coordination identity is already distinct enough for this phase. |
| Combat / tactical control | 3 | ADEQUATE | Still compact, but each current template covers a different combat-control lane; not a priority for authenticity risk in this pass. |
| Crafting / engineering / automation | 2 | ADEQUATE | Very small, but intentionally narrow and not one of the categories currently failing category-identity coverage. Left untouched to avoid unnecessary broadening. |
| Stealth / trickery / disruption | 2 | THIN | Too few templates, too much reliance on “silent ingress” + “trade concealment”, and high risk of collapsing under anti-vanilla filtering. |

## Core finding

The categories that materially risked failing later authenticity enforcement were:
1. **Stealth / trickery / disruption** — clearly thin.
2. **Sensing / information** — shallow because most coverage was direct reveal/scouting instead of interpretation, forensic reading, or hidden-state inference.
3. **Survival / adaptation** — broad enough by count, but still shallow in category identity because it lacked enough shelter / scarcity / temporal adaptation / attrition-management templates.

# SECTION 2: THIN / SHALLOW CATEGORIES TARGETED

This pass intentionally targeted only the weak categories identified above:
- **Survival / adaptation**
- **Sensing / information**
- **Stealth / trickery / disruption**

No ecology systems, population dynamics, or category architecture were changed.
No already-strong category was broadened.
No new category types were introduced.

# SECTION 3: NEW DEEP TEMPLATES ADDED

## Survival / adaptation additions

### 1. `survival.storm_shelter_ledger`
- Shelter thresholds remember **recent weather exposure**.
- Creates a **staged refuge window** before the next front lands.
- Adds **statefulness** (recent exposure), **temporal pressure** (before next front), and **spatial structure dependence** (shelter threshold).
- Reinforces survival identity around **shelter, weather, and fallback planning**.

### 2. `survival.exposure_weave`
- Repeated **day/night exposure in the same biome** creates a short adaptation weave.
- The weave **swaps benefits across phases**: travel resilience vs. camp efficiency.
- Adds **temporal specialization**, **conditional cycling**, and **tradeoff behavior**.
- Reinforces survival as **environmental adaptation**, not raw durability.

### 3. `survival.scarcity_compass`
- Uneven pickup streaks detect **local scarcity / strip-mining pressure**.
- Redirects the player toward **substitute sustenance or supply pivots**.
- Adds **route-reading**, **resource exhaustion detection**, and **conditional rerouting**.
- Reinforces survival as **scarcity management and attrition prevention**.

## Sensing / information additions

### 4. `sensing.faultline_ledger`
- Elevation changes expose **hidden stress lines** that imply buried caverns, liquid pockets, or worked stone.
- Adds **terrain interpretation**, **subsurface inference**, and **environmental hidden-state discovery**.
- Strongly improves anti-vanilla distinctiveness because it reads **derived signals**, not direct reveals.

### 5. `sensing.witness_lag`
- Witnessed actions leave an **afterimage** that suggests whether a target doubled back, hesitated, or staged a feint.
- Adds **forensic reconstruction**, **behavior reading**, and **conditional branching off observed action history**.
- Makes sensing feel like **interpreting intent**, not just detecting presence.

### 6. `sensing.cache_resonance`
- Inspecting storage/crafted blocks reveals **disturbance layers**, hidden-compartment suspicion, and stash cadence.
- Adds **hidden-state discovery**, **disturbance stratification**, and **forensic pattern reading**.
- Deepens sensing identity into **investigation and interpretation**.

### 7. `sensing.route_grammar`
- Chunk-to-chunk movement patterns are parsed into **travel grammar**.
- Reveals where routes **narrow, loop, or imply hidden destinations**.
- Adds **route inference**, **multi-step spatial interpretation**, and **movement syntax reading**.
- Broadens sensing coverage into **world-traffic analysis** instead of simple scan pinging.

## Stealth / trickery / disruption additions

### 8. `stealth.shadow_proxy`
- When seen, the artifact can plant a **false last-known position**.
- Only matures if **line of sight breaks** and the route **forks**.
- Adds **multi-step interaction**, **conditional misdirection**, and **pursuit disruption**.
- Strongly reinforces stealth identity around **believable deception**, not invisibility-lite behavior.

### 9. `stealth.threshold_jam`
- Skirting hostile thresholds accumulates **alarm interference**.
- Works only if the infiltrator avoids **repeating the same approach angle**.
- Adds **spatial constraints**, **pattern-avoidance tradeoff**, and **infiltration disruption**.
- Strengthens stealth as **anti-detection and breach craft**.

### 10. `stealth.dead_drop_lattice`
- A suspicious trade seeds a **hidden handoff route**.
- Requires threading **multiple low-traffic waypoints** before doubling back.
- Adds **route choreography**, **contraband routing**, and **multi-step infiltration logistics**.
- Gives stealth a durable niche in **social/economic infiltration**, not just movement concealment.

### 11. `stealth.echo_shunt`
- Alternating sprint/crouch cadence splits movement noise into a **real trail** and a **phantom branch**.
- Adds **cadence timing**, **movement-state branching**, and **escape-route deception**.
- Deepens stealth identity in **misdirection through motion patterns**.

## Net result by targeted category

| Category | Before | After | Net new |
| --- | ---: | ---: | ---: |
| Survival / adaptation | 8 | 11 | +3 |
| Sensing / information | 5 | 9 | +4 |
| Stealth / trickery / disruption | 2 | 6 | +4 |

# SECTION 4: FILES MODIFIED

1. `src/main/java/obtuseloot/abilities/AbilityRegistry.java`
   - Added all new deep templates directly into the procedural template registry.
   - Because templates already carry category, family, trigger, mechanic, metadata, affinities, and utility domains, the new entries automatically participate in:
     - niche-weighted generation
     - lineage-biased mutation pressure
     - novelty scoring
     - motif clustering
     - category pressure balancing

2. `docs/deep-mechanic-depth-expansion.md`
   - Added this refinement report and validation summary.

# SECTION 5: CATEGORY DEPTH VALIDATION

## Validation method

Targeted validation probes were run against the updated registry using the same structural inputs the generator already consumes for diversity and selection:
- category membership
- trigger span
- mechanic span
- family span
- effect token overlap
- utility-domain / trigger-class / affinity signature overlap
- metadata-vector distinctiveness

A lightweight representation probe also sampled weighted template exposure inside each expanded category to confirm that the category can produce more than one meaningful template under selection pressure.

## Post-pass category representation

| Category | Templates | Trigger span | Mechanic span | Family span | Max pairwise similarity | Post-pass status |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Survival / adaptation | 11 | 9 | 10 | 4 | 0.520 | STRONG |
| Sensing / information | 9 | 7 | 6 | 4 | 0.512 | STRONG |
| Stealth / trickery / disruption | 6 | 4 | 5 | 4 | 0.472 | ADEQUATE / STABLE |

Interpretation:
- **Survival** now covers shelter timing, exposure cycles, scarcity reading, weather adaptation, terrain adaptation, structure adaptation, and attrition recovery rather than feeling like a loose bundle of “environmental support” templates.
- **Sensing** now spans terrain inference, witness reconstruction, cache forensics, route analysis, ore/material interpretation, artifact affinity reading, and contraband detection.
- **Stealth** now spans ingress suppression, economic cover stories, false-position planting, threshold interference, dead-drop routing, and motion-noise branching.

## Representation probe summary

Weighted category probes showed full template representation inside each expanded category:
- **Survival / adaptation:** 11 of 11 templates appeared in probe sampling.
- **Sensing / information:** 9 of 9 templates appeared in probe sampling.
- **Stealth / trickery / disruption:** 6 of 6 templates appeared in probe sampling.

This indicates the expanded categories are now large and distinct enough to be sampled meaningfully rather than collapsing to one obvious dominant template.

## Authenticity-filter survival judgment

### Survival / adaptation
Pass.
- Strong trigger/mechanic spread.
- Clear shelter / scarcity / attrition / environmental-response identity.
- Multiple templates depend on state, timing, or route conditions rather than flat resilience buffs.

### Sensing / information
Pass.
- Category now focuses on **interpreting evidence** rather than merely detecting objects.
- Several templates rely on delayed inference, disturbance history, route syntax, and witness reconstruction.
- This should survive anti-vanilla filtering well because the mechanics are not direct vanilla analogues.

### Stealth / trickery / disruption
Pass.
- Category moved from thin to stable.
- New templates create distinct stealth lanes: pursuit deception, threshold interference, contraband routing, and motion-noise branching.
- The category no longer depends on only two closely adjacent infiltration concepts.

# SECTION 6: READINESS FOR AUTHENTICITY ENFORCEMENT

Readiness judgment: **SUCCESS**.

Why this pass is now safe for later authenticity enforcement:
- The previously weak categories now have enough templates to support **broad player/server coverage**.
- The new templates are mechanically deeper and less likely to be filtered out as vanilla-adjacent or stat-only.
- Category identity is sharper, not blurrier:
  - survival = adaptation / scarcity / shelter / attrition management
  - sensing = reading traces / reconstruction / hidden-state inference
  - stealth = concealment / deception / infiltration / anti-detection disruption
- Integration was preserved automatically because all additions were made inside the same template registry structure already consumed by scoring, niche pressure, lineage influence, and diversity indexing.

Remaining note:
- Crafting/engineering and combat/tactical control remain narrower than the largest categories, but they were intentionally left unchanged in this targeted pass to avoid unnecessary broadening outside the stated objective.

MECHANIC_DEPTH_RESULT: SUCCESS
