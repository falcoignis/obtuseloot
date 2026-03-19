# Deep Broad Skill Category Expansion

## SECTION 1: CATEGORY COVERAGE MAP

| Category | Identity | Typical triggers | Typical effects | Gameplay role | Player/server coverage |
|---|---|---|---|---|---|
| traversal / mobility | Fast repositioning, route memory, travel compression, vertical/path control | chunk enter, elevation change, ritual interact, memory recall | route hints, rebound paths, safe corridors, path anchors | exploration tempo and reposition control | explorer, fighter, skyblock, SMP, adventure |
| sensing / information | Interpreting terrain, structures, trade signals, and witnessed activity | world scan, block inspect, player witness, trade | scouting cues, resource hints, flank reads, market forensics | planning, scouting, anti-ambush play | explorer, trader, factions, RPG |
| survival / adaptation | Withstanding weather, scarcity, shelter pressure, harsh travel | weather change, time transition, entity inspect | resilience windows, shelter recall, omen filtering, attrition smoothing | solo stability and hardcore readiness | survival, hardcore, solo, SMP |
| combat / tactical control | Battlefield shaping without direct ecology redesign | structure proximity, witness events, social feints | pressure lanes, flank hints, bait windows, control zones | PvP/FvF tactical identity | fighter, factions/PvP, RPG, minigames |
| defense / warding | Safe-zone maintenance, perimeter warning, sanctum hardening | structure proximity, ritual completion, dusk/night shifts | wards, threshold alerts, fortification pulses | base defense and builder/warden identity | builder, support, SMP, factions |
| resource / farming / logistics | Throughput, refill cadence, convoy routing, harvest continuation | harvest streaks, item pickup, group hauling, trade | relay chains, stock alerts, delivery routing | farming/economy/logistics support | farmer, trader, skyblock, SMP |
| crafting / engineering / automation | Patterned construction, workshop insight, machine interpretation | block inspect, repeated block pattern | machine diagnostics, fabrication cadence, process chaining | technical builder progression | builder, automation, engineering, skyblock |
| social / support / coordination | Group timing, mutual aid, role clarity, co-presence benefits | group action, player witness, trade, greetings | shared windows, ally reads, rally cues, clutch recovery | support-centered group play | support, trader, ritual, RPG parties |
| ritual / strange utility | Symbolic repetition, oaths, omens, weird conversion of context | ritual completion, repeated patterns, time transitions, memory events | resonance, omen debt, symbolic utility, weird support | lore-heavy and mystic archetypes | ritual, RPG, adventure, social servers |
| stealth / trickery / disruption | Quiet ingress, smuggling, cover stories, misdirection | structure proximity, trade, social prep | hush routes, false trails, disruption-ready setup | infiltration and denial play | stealth, factions, trickster, adventure |

## SECTION 2: NEW SKILL CATEGORIES ADDED

Added a first-class `AbilityCategory` enum and attached categories directly to every `AbilityTemplate`, so categories are no longer implicit naming conventions. Each category now carries identity, effect language, gameplay role, niche tags, and motif tags that can participate in generator behavior. The added categories are:

- `TRAVERSAL_MOBILITY`
- `SENSING_INFORMATION`
- `SURVIVAL_ADAPTATION`
- `COMBAT_TACTICAL_CONTROL`
- `DEFENSE_WARDING`
- `RESOURCE_FARMING_LOGISTICS`
- `CRAFTING_ENGINEERING_AUTOMATION`
- `SOCIAL_SUPPORT_COORDINATION`
- `RITUAL_STRANGE_UTILITY`
- `STEALTH_TRICKERY_DISRUPTION`

## SECTION 3: NEW SKILLS / TEMPLATES ADDED

New templates added in this phase:

- Traversal / mobility
  - `mobility.rift_stride`
  - `mobility.skyline_fold`
- Sensing / information
  - `sensing.contraband_tell`
  - `sensing.battlefield_read`
- Survival / adaptation
  - `survival.hardiness_loop`
- Defense / warding
  - `survival.last_light_cache`
  - `warding.perimeter_hum`
  - `warding.sanctum_lock`
- Combat / tactical control
  - `tactical.killzone_lattice`
  - `tactical.feint_window`
- Resource / farming / logistics
  - `logistics.convoy_instinct`
  - `logistics.stockpile_tide`
- Crafting / engineering / automation
  - `engineering.redstone_sympathy`
  - `engineering.pattern_forge`
- Social / support / coordination
  - `support.rally_ledger`
  - `support.mercy_link`
- Ritual / strange utility
  - `ritual.oath_circuit`
  - `ritual.moon_debt`
- Stealth / trickery / disruption
  - `stealth.hushwire`
  - `stealth.paper_trail`

These skills broaden coverage for explorers, builders, fighters, farmers, traders, support players, stealth players, hardcore/survival users, automation/engineering specialists, and social/ritual archetypes without modifying ecology or population-dynamics systems.

## SECTION 4: FILES MODIFIED

- `src/main/java/obtuseloot/abilities/AbilityCategory.java`
- `src/main/java/obtuseloot/abilities/AbilityTemplate.java`
- `src/main/java/obtuseloot/abilities/AbilityRegistry.java`
- `src/main/java/obtuseloot/abilities/AbilityDiversityIndex.java`
- `src/main/java/obtuseloot/abilities/ProceduralAbilityGenerator.java`
- `src/test/java/obtuseloot/abilities/NonCombatAbilityIntegrationTest.java`
- `src/test/java/obtuseloot/abilities/BroadSkillCategoryExpansionProbeTest.java`
- `docs/deep-broad-skill-category-expansion.md`

## SECTION 5: NICHE DISTRIBUTION BY CATEGORY

Implementation hooks now bias category selection by niche instead of only by family/mechanic:

- niche-weighted selection now uses both niche membership and category niche affinity
- crowded categories receive bounded pressure penalties alongside crowded niche penalties
- category-aware weighting supports visible separation between explorer / builder / fighter / ritualist / survivalist profiles

Targeted probe instrumentation for this lives in `BroadSkillCategoryExpansionProbeTest`.

## SECTION 6: LINEAGE DISTRIBUTION BY CATEGORY

Lineage-sensitive category bias is now integrated through category-aware scoring:

- exploration-biased lineages preferentially reinforce traversal/information categories
- support-biased lineages reinforce defense/logistics/coordination/engineering categories
- weirdness-biased lineages reinforce ritual/stealth/tactical-control categories
- ALPHA/BETA variant handling now nudges exploratory categories vs retention-oriented categories respectively

This preserves lineage variation inside the same niche instead of collapsing to a single family choice.

## SECTION 7: PLAYER / SERVER COVERAGE ASSESSMENT

Coverage is materially broader after this pass:

- Solo play: survival, traversal, sensing, stealth, ritual omen tools
- Group play: rally, convoy, mercy, oath, perimeter, sanctum, collective routing
- PvE: shelter, hardiness, battlefield reads, scouting, warding
- PvP: killzone control, feints, hushwire ingress, contraband forensics, perimeter alerts
- Economy/logistics: trader whisper + convoy instinct + stockpile tide + contraband tell
- Exploration: trail sense, skyline fold, rift stride, cartographic echoes, omen/memory tools
- Base defense: perimeter hum, sanctum lock, last light cache, killzone lattice
- Support play: mercy link, rally ledger, convoy instinct, collective insight, sanctum lock

Server-style balance intent:

- SMP: warding, logistics, engineering, social support
- Factions / PvP: tactical control, stealth, perimeter defense, market deception
- RPG: ritual, witness, oaths, battlefield reads, support timing
- Hardcore survival: hardiness, dusk shelter, weather adaptation, omen debt
- Skyblock / progression: route compression, logistics, engineering cadence, machine diagnostics
- Adventure / minigame: traversal bursts, witness-driven control, stealth ingress, ritual weirdness

## SECTION 8: STABILITY CHECK

Boundedness measures included in code changes:

- category crowding pressure joins niche crowding pressure, reducing runaway dominance
- category bias multipliers are clamped tightly in the generator
- ALPHA/BETA expression is bounded to modest category preference shifts rather than hard forcing
- novelty/motif similarity now considers category identity, improving separation without allowing one family to overtake the pool
- no ecology, extinction, bifurcation, continuity, or population-dynamics logic was modified

Validation status:

- compile path succeeded after integrating categories and templates
- registry/category integrity tests passed
- targeted deep probe harness was added for niche/lineage/category validation
- the deep broad probe is currently heavier than the quick unit pass and may need a dedicated longer-running validation window in CI/local runs for full numeric capture

BROAD_SKILL_EXPANSION_RESULT: PARTIAL
