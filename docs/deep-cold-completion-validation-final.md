# Deep Cold Completion Validation Final

## SECTION 1: HIGH-VOLUME PROBE RESULTS

- Total generations: 5000
- Scenarios: explorer, builder, fighter, ritualist, survivor

### Scenario category exposure snapshots

- **explorer**: Stealth / trickery / disruption=792, Resource / farming / logistics=531, Traversal / mobility=395, Sensing / information=110
- **builder**: Crafting / engineering / automation=709, Ritual / strange utility=356, Social / support / coordination=240, Sensing / information=205
- **fighter**: Survival / adaptation=1155, Combat / tactical control=284, Defense / warding=176, Resource / farming / logistics=166
- **ritualist**: Ritual / strange utility=558, Stealth / trickery / disruption=496, Sensing / information=412, Survival / adaptation=234
- **survivor**: Survival / adaptation=1242, Resource / farming / logistics=289, Defense / warding=242, Combat / tactical control=77

### Normal-probe category metrics

| Category | Total hits | Reached | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit |
| --- | ---: | ---: | --- | ---: | ---: | ---: |
| Traversal / mobility | 474 | 10/10 | none | 11.2% | 33.3% | 10 |
| Sensing / information | 745 | 10/10 | none | 14.6% | 42.7% | 10 |
| Survival / adaptation | 2736 | 14/14 | none | 8.2% | 24.2% | 14 |
| Combat / tactical control | 522 | 8/8 | none | 14.0% | 41.6% | 8 |
| Defense / warding | 610 | 5/5 | none | 21.1% | 61.1% | 5 |
| Resource / farming / logistics | 1038 | 8/8 | none | 14.1% | 41.3% | 8 |
| Crafting / engineering / automation | 1001 | 5/5 | none | 21.5% | 61.4% | 5 |
| Social / support / coordination | 499 | 8/8 | none | 13.4% | 39.9% | 8 |
| Ritual / strange utility | 950 | 11/11 | none | 11.6% | 33.3% | 11 |
| Stealth / trickery / disruption | 1425 | 9/9 | none | 12.2% | 36.0% | 9 |

## SECTION 2: CATEGORY-FORCED RESULTS

- Total forced generations: 5000
- Generations per category: 500

| Category | Total hits | Reached | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit |
| --- | ---: | ---: | --- | ---: | ---: | ---: |
| Traversal / mobility | 500 | 10/10 | none | 80.0% | 98.6% | 2 |
| Sensing / information | 500 | 10/10 | none | 77.0% | 98.6% | 2 |
| Survival / adaptation | 500 | 14/14 | none | 87.0% | 97.8% | 2 |
| Combat / tactical control | 500 | 8/8 | none | 92.2% | 99.0% | 2 |
| Defense / warding | 500 | 5/5 | none | 74.6% | 99.6% | 2 |
| Resource / farming / logistics | 500 | 2/8 | gathering.forager_memory, gathering.ecological_sense, gathering.gatherers_intuition, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit | 96.0% | 100.0% | 2 |
| Crafting / engineering / automation | 500 | 1/5 | engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate | 100.0% | 100.0% | 1 |
| Social / support / coordination | 500 | 2/8 | social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange | 68.6% | 100.0% | 2 |
| Ritual / strange utility | 500 | 11/11 | none | 61.0% | 98.4% | 2 |
| Stealth / trickery / disruption | 500 | 9/9 | none | 98.4% | 98.8% | 1 |

## SECTION 3: REACHABILITY MATRIX

| Category | Templates | Normal reached | Forced reached | Zero-hit forced templates |
| --- | ---: | ---: | ---: | --- |
| Traversal / mobility | 10 | 10/10 | 10/10 | none |
| Sensing / information | 10 | 10/10 | 10/10 | none |
| Survival / adaptation | 14 | 14/14 | 14/14 | none |
| Combat / tactical control | 8 | 8/8 | 8/8 | none |
| Defense / warding | 5 | 5/5 | 5/5 | none |
| Resource / farming / logistics | 8 | 8/8 | 2/8 | gathering.forager_memory, gathering.ecological_sense, gathering.gatherers_intuition, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit |
| Crafting / engineering / automation | 5 | 5/5 | 1/5 | engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate |
| Social / support / coordination | 8 | 8/8 | 2/8 | social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange |
| Ritual / strange utility | 11 | 11/11 | 11/11 | none |
| Stealth / trickery / disruption | 9 | 9/9 | 9/9 | none |

## SECTION 4: DISTRIBUTION ANALYSIS

### Traversal / mobility

- Total hits: 500
- Templates reached: 10 / 10
- Zero-hit templates: none
- Top template share: 80.0%
- Top-3 share: 98.6%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: mobility.skyline_fold=80.0%, mobility.rift_stride=18.4%, exploration.cartographers_echo=0.2%, mobility.footprint_memory=0.2%, consistency.path_thread=0.2%, mobility.quiet_passage=0.2%, exploration.biome_attunement=0.2%, mobility.compass_stories=0.2%, exploration.trail_sense=0.2%, evolution.niche_architect=0.2%

### Sensing / information

- Total hits: 500
- Templates reached: 10 / 10
- Zero-hit templates: none
- Top template share: 77.0%
- Top-3 share: 98.6%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: precision.echo_locator=77.0%, precision.vein_whisper=21.4%, sensing.witness_lag=0.2%, precision.artifact_sympathy=0.2%, precision.material_insight=0.2%, sensing.route_grammar=0.2%, sensing.contraband_tell=0.2%, sensing.faultline_ledger=0.2%, consistency.structure_echo=0.2%, sensing.cache_resonance=0.2%

### Survival / adaptation

- Total hits: 500
- Templates reached: 14 / 14
- Zero-hit templates: none
- Top template share: 87.0%
- Top-3 share: 97.8%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: survival.gentle_harvest=87.0%, survival.press_through=10.6%, survival.ember_keeper=0.2%, survival.herd_instinct=0.2%, survival.storm_shelter_ledger=0.2%, survival.scarcity_compass=0.2%, environment.weather_sensitivity=0.2%, survival.hardiness_loop=0.2%, environment.structure_attunement=0.2%, survival.last_light_cache=0.2%, survival.weather_omen=0.2%, evolution.lineage_fortification=0.2%, environment.terrain_affinity=0.2%, survival.exposure_weave=0.2%

### Combat / tactical control

- Total hits: 500
- Templates reached: 8 / 8
- Zero-hit templates: none
- Top template share: 92.2%
- Top-3 share: 99.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: tactical.reposition_snare=92.2%, tactical.tempo_extract=6.6%, combat.rupture_window=0.2%, stealth.intercept_line=0.2%, tactical.rush_damper=0.2%, tactical.feint_window=0.2%, sensing.battlefield_read=0.2%, tactical.killzone_lattice=0.2%

### Defense / warding

- Total hits: 500
- Templates reached: 5 / 5
- Zero-hit templates: none
- Top template share: 74.6%
- Top-3 share: 99.6%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: warding.fault_survey=74.6%, warding.sanctum_lock=24.8%, warding.false_threshold=0.2%, warding.perimeter_hum=0.2%, warding.anchor_cadence=0.2%

### Resource / farming / logistics

- Total hits: 500
- Templates reached: 2 / 8
- Zero-hit templates: gathering.forager_memory, gathering.ecological_sense, gathering.gatherers_intuition, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit
- Top template share: 96.0%
- Top-3 share: 100.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: logistics.convoy_instinct=96.0%, logistics.queue_sight=4.0%

### Crafting / engineering / automation

- Total hits: 500
- Templates reached: 1 / 5
- Zero-hit templates: engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate
- Top template share: 100.0%
- Top-3 share: 100.0%
- Templates with >1 hit: 1
- No uniform flattening: PASS
- Distribution: engineering.pattern_forge=100.0%

### Social / support / coordination

- Total hits: 500
- Templates reached: 2 / 8
- Zero-hit templates: social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange
- Top template share: 68.6%
- Top-3 share: 100.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: support.rally_ledger=68.6%, support.mercy_link=31.4%

### Ritual / strange utility

- Total hits: 500
- Templates reached: 11 / 11
- Zero-hit templates: none
- Top template share: 61.0%
- Top-3 share: 98.4%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: ritual.oath_circuit=61.0%, ritual.moon_debt=37.2%, chaos.witness=0.2%, ritual.temporal_attunement=0.2%, chaos.ritual_echo=0.2%, evolution.entropy_pulse=0.2%, ritual.pattern_resonance=0.2%, evolution.resource_parasitism=0.2%, chaos.dust_memory=0.2%, ritual.altar_resonance=0.2%, evolution.ritual_amplifier=0.2%

### Stealth / trickery / disruption

- Total hits: 500
- Templates reached: 9 / 9
- Zero-hit templates: none
- Top template share: 98.4%
- Top-3 share: 98.8%
- Templates with >1 hit: 1
- No uniform flattening: PASS
- Distribution: stealth.hushwire=98.4%, stealth.social_smoke=0.2%, stealth.dead_drop_lattice=0.2%, stealth.paper_trail=0.2%, stealth.ghost_shift=0.2%, stealth.threshold_jam=0.2%, stealth.trace_fold=0.2%, stealth.echo_shunt=0.2%, stealth.shadow_proxy=0.2%

## SECTION 5: FINAL JUDGMENT

- Reachability across all categories: FAIL
- Stealth reachable: 9/9
- Defense reachable: 5/8
- Ritual reachable: 11/13
- No template above 50%: FAIL
- No uniform flattening: PASS
- Long-tail templates appear multiple times: FAIL

COLD_COMPLETION_RESULT: FAILED
