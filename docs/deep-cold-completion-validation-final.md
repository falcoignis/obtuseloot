# Deep Cold Completion Validation Final

## SECTION 1: HIGH-VOLUME PROBE RESULTS

- Total generations: 5000
- Scenarios: explorer, builder, fighter, ritualist, survivor

### Scenario category exposure snapshots

- **explorer**: Stealth / trickery / disruption=746, Resource / farming / logistics=313, Crafting / engineering / automation=229, Traversal / mobility=224
- **builder**: Crafting / engineering / automation=449, Resource / farming / logistics=365, Survival / adaptation=224, Ritual / strange utility=209
- **fighter**: Sensing / information=681, Survival / adaptation=300, Social / support / coordination=180, Combat / tactical control=156
- **ritualist**: Sensing / information=506, Stealth / trickery / disruption=499, Ritual / strange utility=329, Traversal / mobility=215
- **survivor**: Survival / adaptation=770, Combat / tactical control=435, Defense / warding=163, Resource / farming / logistics=155

### Normal-probe category metrics

| Category | Total hits | Reached | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit |
| --- | ---: | ---: | --- | ---: | ---: | ---: |
| Traversal / mobility | 679 | 8/8 | none | 43.0% | 81.3% | 8 |
| Sensing / information | 1519 | 6/9 | precision.vein_whisper, precision.material_insight, sensing.contraband_tell | 63.1% | 93.9% | 6 |
| Survival / adaptation | 1486 | 10/11 | survival.storm_shelter_ledger | 33.8% | 74.9% | 9 |
| Combat / tactical control | 801 | 6/6 | none | 57.8% | 87.5% | 6 |
| Defense / warding | 551 | 7/8 | consistency.structure_echo | 28.9% | 81.1% | 7 |
| Resource / farming / logistics | 1005 | 7/8 | gathering.ecological_sense | 40.7% | 87.2% | 7 |
| Crafting / engineering / automation | 1061 | 5/5 | none | 28.7% | 70.5% | 5 |
| Social / support / coordination | 602 | 8/8 | none | 30.9% | 78.9% | 8 |
| Ritual / strange utility | 815 | 8/13 | chaos.dust_memory, chaos.ritual_echo, ritual.altar_resonance, evolution.entropy_pulse, ritual.oath_circuit | 58.7% | 81.6% | 8 |
| Stealth / trickery / disruption | 1481 | 9/9 | none | 22.0% | 51.2% | 9 |

## SECTION 2: CATEGORY-FORCED RESULTS

- Total forced generations: 5000
- Generations per category: 500

| Category | Total hits | Reached | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit |
| --- | ---: | ---: | --- | ---: | ---: | ---: |
| Traversal / mobility | 500 | 8/8 | none | 68.8% | 99.0% | 2 |
| Sensing / information | 500 | 9/9 | none | 77.2% | 98.8% | 2 |
| Survival / adaptation | 500 | 11/11 | none | 87.6% | 98.4% | 2 |
| Combat / tactical control | 500 | 6/6 | none | 63.0% | 99.4% | 2 |
| Defense / warding | 500 | 8/8 | none | 51.4% | 99.0% | 3 |
| Resource / farming / logistics | 500 | 3/8 | gathering.forager_memory, gathering.ecological_sense, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit | 95.6% | 100.0% | 3 |
| Crafting / engineering / automation | 500 | 1/5 | engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate | 100.0% | 100.0% | 1 |
| Social / support / coordination | 500 | 2/8 | social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange | 76.2% | 100.0% | 2 |
| Ritual / strange utility | 500 | 13/13 | none | 96.8% | 98.0% | 2 |
| Stealth / trickery / disruption | 500 | 9/9 | none | 84.8% | 98.8% | 2 |

## SECTION 3: REACHABILITY MATRIX

| Category | Templates | Normal reached | Forced reached | Zero-hit forced templates |
| --- | ---: | ---: | ---: | --- |
| Traversal / mobility | 8 | 8/8 | 8/8 | none |
| Sensing / information | 9 | 6/9 | 9/9 | none |
| Survival / adaptation | 11 | 10/11 | 11/11 | none |
| Combat / tactical control | 6 | 6/6 | 6/6 | none |
| Defense / warding | 8 | 7/8 | 8/8 | none |
| Resource / farming / logistics | 8 | 7/8 | 3/8 | gathering.forager_memory, gathering.ecological_sense, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit |
| Crafting / engineering / automation | 5 | 5/5 | 1/5 | engineering.redstone_sympathy, engineering.sequence_splice, engineering.machine_rhythm, engineering.fault_isolate |
| Social / support / coordination | 8 | 8/8 | 2/8 | social.witness_imprint, social.collective_insight, social.trader_whisper, support.role_call, support.convoy_accord, support.cover_exchange |
| Ritual / strange utility | 13 | 8/13 | 13/13 | none |
| Stealth / trickery / disruption | 9 | 9/9 | 9/9 | none |

## SECTION 4: DISTRIBUTION ANALYSIS

### Traversal / mobility

- Total hits: 500
- Templates reached: 8 / 8
- Zero-hit templates: none
- Top template share: 68.8%
- Top-3 share: 99.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: mobility.skyline_fold=68.8%, mobility.rift_stride=30.0%, exploration.biome_attunement=0.2%, mobility.footprint_memory=0.2%, mobility.quiet_passage=0.2%, mobility.compass_stories=0.2%, exploration.cartographers_echo=0.2%, exploration.trail_sense=0.2%

### Sensing / information

- Total hits: 500
- Templates reached: 9 / 9
- Zero-hit templates: none
- Top template share: 77.2%
- Top-3 share: 98.8%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: precision.echo_locator=77.2%, precision.vein_whisper=21.4%, sensing.witness_lag=0.2%, sensing.contraband_tell=0.2%, precision.artifact_sympathy=0.2%, sensing.route_grammar=0.2%, sensing.faultline_ledger=0.2%, sensing.cache_resonance=0.2%, precision.material_insight=0.2%

### Survival / adaptation

- Total hits: 500
- Templates reached: 11 / 11
- Zero-hit templates: none
- Top template share: 87.6%
- Top-3 share: 98.4%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: survival.gentle_harvest=87.6%, survival.scarcity_compass=10.6%, survival.ember_keeper=0.2%, survival.herd_instinct=0.2%, survival.hardiness_loop=0.2%, survival.storm_shelter_ledger=0.2%, environment.weather_sensitivity=0.2%, environment.terrain_affinity=0.2%, environment.structure_attunement=0.2%, survival.exposure_weave=0.2%, survival.weather_omen=0.2%

### Combat / tactical control

- Total hits: 500
- Templates reached: 6 / 6
- Zero-hit templates: none
- Top template share: 63.0%
- Top-3 share: 99.4%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: tactical.tempo_extract=63.0%, tactical.reposition_snare=36.2%, tactical.killzone_lattice=0.2%, tactical.feint_window=0.2%, tactical.rush_damper=0.2%, sensing.battlefield_read=0.2%

### Defense / warding

- Total hits: 500
- Templates reached: 8 / 8
- Zero-hit templates: none
- Top template share: 51.4%
- Top-3 share: 99.0%
- Templates with >1 hit: 3
- No uniform flattening: PASS
- Distribution: warding.sanctum_lock=51.4%, survival.last_light_cache=24.2%, warding.perimeter_hum=23.4%, warding.false_threshold=0.2%, consistency.path_thread=0.2%, warding.fault_survey=0.2%, consistency.structure_echo=0.2%, warding.anchor_cadence=0.2%

### Resource / farming / logistics

- Total hits: 500
- Templates reached: 3 / 8
- Zero-hit templates: gathering.forager_memory, gathering.ecological_sense, logistics.stockpile_tide, logistics.relay_mesh, logistics.spoilage_audit
- Top template share: 95.6%
- Top-3 share: 100.0%
- Templates with >1 hit: 3
- No uniform flattening: PASS
- Distribution: logistics.convoy_instinct=95.6%, logistics.queue_sight=4.0%, gathering.gatherers_intuition=0.4%

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
- Top template share: 76.2%
- Top-3 share: 100.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: support.rally_ledger=76.2%, support.mercy_link=23.8%

### Ritual / strange utility

- Total hits: 500
- Templates reached: 13 / 13
- Zero-hit templates: none
- Top template share: 96.8%
- Top-3 share: 98.0%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: ritual.oath_circuit=96.8%, ritual.moon_debt=1.0%, chaos.witness=0.2%, evolution.entropy_pulse=0.2%, chaos.ritual_echo=0.2%, evolution.resource_parasitism=0.2%, ritual.pattern_resonance=0.2%, evolution.ritual_amplifier=0.2%, chaos.dust_memory=0.2%, ritual.temporal_attunement=0.2%, evolution.niche_architect=0.2%, ritual.altar_resonance=0.2%, evolution.lineage_fortification=0.2%

### Stealth / trickery / disruption

- Total hits: 500
- Templates reached: 9 / 9
- Zero-hit templates: none
- Top template share: 84.8%
- Top-3 share: 98.8%
- Templates with >1 hit: 2
- No uniform flattening: PASS
- Distribution: stealth.hushwire=84.8%, stealth.paper_trail=13.8%, stealth.social_smoke=0.2%, stealth.dead_drop_lattice=0.2%, stealth.echo_shunt=0.2%, stealth.threshold_jam=0.2%, stealth.trace_fold=0.2%, stealth.ghost_shift=0.2%, stealth.shadow_proxy=0.2%

## SECTION 5: FINAL JUDGMENT

- Reachability across all categories: FAIL
- Stealth reachable: 9/9
- Defense reachable: 8/8
- Ritual reachable: 13/13
- No template above 50%: FAIL
- No uniform flattening: PASS
- Long-tail templates appear multiple times: FAIL

COLD_COMPLETION_RESULT: FAILED
