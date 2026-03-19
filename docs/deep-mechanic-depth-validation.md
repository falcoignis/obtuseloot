# Deep Mechanic Depth Validation

This pass used the current codebase exactly as-is and did not modify generator weights, ecology systems, or population systems. Measurements were taken from the live template registry plus generation probes run against `ProceduralAbilityGenerator` using compiled project classes and targeted memory/lineage profiles.

Probe commands used:

- `mvn -q -DskipTests test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/obtuseloot.cp`
- `java -Djava.util.Arrays.useLegacyMergeSort=true -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" MechanicDepthProbe`
- `java -Djava.util.Arrays.useLegacyMergeSort=true -cp "/tmp:$(cat /tmp/obtuseloot.cp):target/classes:target/test-classes" StealthMultiSearch`

## SECTION 1: TARGET CATEGORY DEPTH

### Survival / adaptation

| Metric | Value | Validation note |
|---|---:|---|
| Template count | 11 | Strong raw depth. |
| Trigger span | 9 | Broad trigger coverage across weather, structure, time, pickup, inspect, ritual, harvest, and scan moments. |
| Mechanic span | 10 | Almost one mechanic lane per template. |
| Family span | 4 | Mixes survival, precision, consistency, and chaos family roots. |
| Effect-token variety | 104 | Strong descriptive breadth. |
| Utility-domain variety | 18 | Covers weather adaptation, scarcity reading, shelter play, ritual utility, farming, and environmental rhythm. |
| Affinity/signature variety | 38 | Wide signature vocabulary. |
| Metadata-vector distinctiveness | avg distance `0.1694`, min `0.0367`, max `0.4083` | Numeric utility vectors are separated enough to avoid template flattening. |

Assessment: **deep and mechanically broad**. This category now clearly contains multiple sub-lanes: weather adaptation, shelter timing, attrition recovery, scarcity interpretation, camp utility, and environmental adaptation.

### Sensing / information

| Metric | Value | Validation note |
|---|---:|---|
| Template count | 9 | Good post-expansion depth. |
| Trigger span | 7 | Spreads across scan, inspect, social, witness, and movement-entry contexts. |
| Mechanic span | 6 | Multiple read/inference lanes rather than one scan mechanic. |
| Family span | 4 | Precision, mobility, consistency, and chaos all contribute. |
| Effect-token variety | 101 | High descriptive breadth. |
| Utility-domain variety | 16 | Strong forensic, path-analysis, storage-reading, social-reading, and discovery coverage. |
| Affinity/signature variety | 32 | Diverse signature language. |
| Metadata-vector distinctiveness | avg distance `0.1504`, min `0.0283`, max `0.2933` | Slightly tighter than survival, but still well separated. |

Assessment: **adequate to strong depth**. The category is no longer only “scan nearby things”; it now includes hidden-state reading, route inference, disturbance forensics, contraband detection, and witness afterimage interpretation.

### Stealth / trickery / disruption

| Metric | Value | Validation note |
|---|---:|---|
| Template count | 6 | Much better than the pre-pass thin state, but still the smallest target pool. |
| Trigger span | 4 | Narrower than the other targets. |
| Mechanic span | 5 | Good mechanic spread for the pool size. |
| Family span | 4 | Good cross-family support. |
| Effect-token variety | 88 | Strong descriptive identity for six templates. |
| Utility-domain variety | 11 | Covers infiltration routing, economic deception, misdirection, threshold disruption, contraband routing, and escape-routing. |
| Affinity/signature variety | 27 | Healthy signature breadth. |
| Metadata-vector distinctiveness | avg distance `0.1230`, min `0.0567`, max `0.2083` | Distinct, but tighter than survival and sensing. |

Assessment: **improved to adequate depth, but still comparatively fragile**. The category now has real internal lanes—ingress suppression, false-position planting, alarm jamming, dead drops, movement-noise splitting, and cover-story deception—but the pool is still compact enough that later filtering pressure could matter.

## SECTION 2: INTERNAL REDUNDANCY ANALYSIS

### Pairwise similarity summary

| Category | Avg pairwise similarity | Max pairwise similarity | Near-duplicate count (`>= 0.80`) | Distribution summary |
|---|---:|---:|---:|---|
| Survival / adaptation | `0.2941` | `0.5541` | 0 | 53 of 55 pairs are in the `0.2-0.4` band; only 2 pairs enter `0.4-0.6`. |
| Sensing / information | `0.3245` | `0.5251` | 0 | 30 of 36 pairs are in `0.2-0.4`; 6 pairs in `0.4-0.6`. |
| Stealth / trickery / disruption | `0.3226` | `0.4897` | 0 | 12 of 15 pairs are in `0.2-0.4`; 3 pairs in `0.4-0.6`. |

### Redundancy judgment

- **Survival / adaptation:** low redundancy. Despite thematically related shelter/weather templates, the pairwise ceiling stays well below the generator’s high-similarity threshold.
- **Sensing / information:** low redundancy. Several templates share interpretation flavor, but they split across chunk reading, witness afterimages, contraband detection, storage forensics, route grammar, and material/block reading.
- **Stealth / trickery / disruption:** low redundancy. Even with only six templates, the pool does **not** look copy-pasted; no pair approaches near-duplicate territory.

Conclusion: the expansion increased **structural breadth**, not just count inflation.

## SECTION 3: GENERATION REPRESENTATION

### Primary targeted probes

#### Survival / adaptation

- Probe total hits: **146** target-category picks.
- Reachability: **11 / 11 templates reached**.
- Largest shares:
  - `survival.ember_keeper`: **39.73%**
  - `environment.weather_sensitivity`: **21.92%**
  - `survival.hardiness_loop`: **21.23%**
  - `environment.terrain_affinity`: **6.85%**
  - `survival.exposure_weave`: **6.16%**
- Long tail: six templates appeared only once each (**0.68%** share each).

Judgment: reachability is complete, but representation is **top-heavy**. The category survives generation, yet much of the tail is still technically alive rather than robustly exercised.

#### Sensing / information

- Probe total hits: **15** target-category picks.
- Reachability: **9 / 9 templates reached**.
- Distribution: no collapse to one or two templates; every template appeared at least once, and five templates appeared twice.

Judgment: structurally healthy and non-dominant once the category appears, but overall targeted sampling volume is still **light**. This category looks broad, but it is not yet strongly favored by generation pressure.

#### Stealth / trickery / disruption

- Primary probe total hits: **6** target-category picks.
- Primary reachability: **5 / 6 templates reached**.
- Primary distribution:
  - `stealth.echo_shunt`: **33.33%**
  - `stealth.dead_drop_lattice`, `stealth.hushwire`, `stealth.paper_trail`, `stealth.shadow_proxy`: **16.67%** each
  - `stealth.threshold_jam`: **0** in the primary probe set

This meant stealth did **not** fully pass reachability under the first probe battery.

### Supplemental stealth reachability probe

A focused supplemental stealth search using builder-leaning infiltrator profiles produced:

- `builder-spy -> {stealth.dead_drop_lattice=3, stealth.paper_trail=3, stealth.threshold_jam=3}`
- `rogue -> {stealth.dead_drop_lattice=1}`
- `jammer -> {stealth.echo_shunt=3}`
- `smuggler -> {stealth.echo_shunt=3}`

Combined with the primary probe, all six stealth templates were shown reachable, including `stealth.threshold_jam`.

Judgment: stealth reachability is **real but conditional**. It no longer looks dead, but some templates need narrower generator circumstances than the other target categories.

### Representation readiness summary

| Category | All templates reachable? | Dominated by 1-2 templates? | Meaningfully sampled? |
|---|---|---|---|
| Survival / adaptation | Yes | **Partially yes**; top 3 templates account for ~82.9% of hits | Yes, but unevenly |
| Sensing / information | Yes | No | Present and balanced when sampled, but low total volume |
| Stealth / trickery / disruption | Yes, after supplemental probe | No in share terms, but total volume is very low | **Borderline** |

## SECTION 4: CATEGORY IDENTITY ASSESSMENT

### Survival / adaptation identity

Identity is now clear and authentic. The template set expresses:

- **adaptation** through `environment.weather_sensitivity`, `survival.hardiness_loop`, and `survival.exposure_weave`
- **shelter / refuge timing** through `survival.storm_shelter_ledger` and `environment.structure_attunement`
- **scarcity / attrition management** through `survival.scarcity_compass`, `survival.weather_omen`, and `survival.herd_instinct`
- **camp / sustaining-world pressure** through `survival.ember_keeper` and `survival.gentle_harvest`

This category no longer reads like generic utility support. It reads like survival gameplay.

### Sensing / information identity

Identity is also clear. The set expresses:

- **interpretation** through `precision.material_insight` and `precision.vein_whisper`
- **inference / route reading** through `sensing.route_grammar` and `sensing.faultline_ledger`
- **hidden-state reading** through `sensing.cache_resonance` and `sensing.contraband_tell`
- **forensic witness analysis** through `sensing.witness_lag`
- **social signature reading** through `precision.artifact_sympathy`

This category now behaves like information processing rather than raw reveal spam.

### Stealth / trickery / disruption identity

Identity is stronger than before and is now legible in content:

- **concealment / infiltration** through `stealth.hushwire` and `stealth.threshold_jam`
- **deception / false signals** through `stealth.shadow_proxy` and `stealth.echo_shunt`
- **smuggling / covert logistics** through `stealth.paper_trail` and `stealth.dead_drop_lattice`
- **anti-detection disruption** through threshold jamming, alarm interference, route splitting, and false last-known positions

So the category identity is no longer in doubt. The remaining concern is not identity, but **sampling strength under pressure**.

## SECTION 5: AUTHENTICITY-ENFORCEMENT READINESS

### Category judgments

| Category | Judgment | Why |
|---|---|---|
| Survival / adaptation | **PARTIAL** | Deep and distinct, but generation is concentrated enough that rejecting simple/vanilla-like survivors could over-expose a thin tail. |
| Sensing / information | **READY** | Good template count, strong identity, low redundancy, full reachability, and balanced in-category representation. |
| Stealth / trickery / disruption | **PARTIAL** | Mechanically distinct and identity-strong, but still compact and only fully reachable after supplemental probe pressure. |

### Overall judgment

Overall readiness for authenticity enforcement is: **PARTIAL**.

Reasoning:

1. The expansion **did** strengthen all three previously weak categories.
2. None of the target categories still looks **THIN** in structure or redundancy.
3. Survival and sensing are materially improved.
4. Stealth is no longer critically thin, but still has the weakest probe robustness.
5. Because survival is top-heavy and stealth remains conditionally sampled, a later anti-vanilla filter could still prune too aggressively unless it is staged carefully.

Recommended interpretation: proceed only if authenticity enforcement is **graduated** rather than immediately harsh, especially for stealth and for the long-tail survival templates.

MECHANIC_DEPTH_VALIDATION_RESULT: PARTIAL
