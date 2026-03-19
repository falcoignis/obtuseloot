# Deep Low-Template Category Expansion

## SECTION 1: LOW-TEMPLATE CATEGORY AUDIT

Baseline audit threshold: categories with **6 or fewer** templates before this pass.

| Category | Baseline count | Structural fragility | Prior validation signal | Audit summary |
|---|---:|---|---|---|
| Crafting / engineering / automation | 2 | **Severe** — two templates could not cover process reading, sequencing, rhythm, and fault isolation at once. | No dedicated prior probe was recorded in the existing validation docs. | Critically underpopulated and too narrow for authenticity filtering. |
| Combat / tactical control | 3 | **Severe** — the category had battlefield reading, chokepoint pressure, and bluffing, but no anti-rush, tempo, or reposition-punish lane. | No dedicated prior probe was recorded in the existing validation docs. | Under-breadthed and vulnerable to single-lane identity. |
| Defense / warding | 5 | **High** — identity existed, but base defense was overconcentrated in threshold sensing and sanctum upkeep. | No dedicated prior probe was recorded in the existing validation docs. | Needed maintenance-reading, patrol rhythm, and deception sub-lanes. |
| Resource / farming / logistics | 5 | **High** — logistics identity existed, but throughput, routing, and waste analysis were still thin. | No dedicated prior probe was recorded in the existing validation docs. | Needed stronger non-harvest logistics lanes. |
| Social / support / coordination | 5 | **High** — the pool leaned on witness/group/trade support but lacked role assignment and formation management breadth. | No dedicated prior probe was recorded in the existing validation docs. | Needed more than reactive support windows. |
| Stealth / trickery / disruption | 6 | **Empirically fragile** — previous reports already flagged reachability pressure and internal concentration. | Prior validation recorded normal-probe fragility (`5 / 6` reachable in the first probe battery), later restored reachability with ongoing concentration issues. | Highest-priority low-count category because it was both compact and already probe-fragile. |

Priority ordering for this pass therefore focused on categories that were both low-count and either already empirically fragile (`stealth`) or structurally too narrow to survive later authenticity enforcement (`crafting`, `combat`, `defense`, `resource`, `social`).

## SECTION 2: TARGETED CATEGORIES

Targeted categories in this pass:

1. **Crafting / engineering / automation**
2. **Combat / tactical control**
3. **Defense / warding**
4. **Resource / farming / logistics**
5. **Social / support / coordination**
6. **Stealth / trickery / disruption**

Post-expansion counts:

| Category | Before | After | Result |
|---|---:|---:|---|
| Crafting / engineering / automation | 2 | 5 | No longer critically narrow. |
| Combat / tactical control | 3 | 6 | Reaches the minimum healthy small-pool floor. |
| Defense / warding | 5 | 8 | Gains real internal sub-lanes. |
| Resource / farming / logistics | 5 | 8 | Gains real logistics breadth beyond harvest continuation. |
| Social / support / coordination | 5 | 8 | Broadens from reactive aid into planning and cohesion. |
| Stealth / trickery / disruption | 6 | 9 | Exits the fragile micro-pool range that previously amplified dominance pressure. |

## SECTION 3: NEW TEMPLATES ADDED

### Crafting / engineering / automation
- `engineering.sequence_splice`
  - Adds **sequence chaining** instead of simple repeated-pattern reward.
- `engineering.machine_rhythm`
  - Adds **process timing / cadence inference** from remembered workshop cycles.
- `engineering.fault_isolate`
  - Adds **fault detection and bypass planning** for stalled builds and contraptions.

### Combat / tactical control
- `tactical.reposition_snare`
  - Adds **reposition punishment / angle denial**.
- `tactical.tempo_extract`
  - Adds **tempo-window reading** during longer skirmish chains.
- `tactical.rush_damper`
  - Adds **anti-rush retreat shaping** under collapse pressure.

### Defense / warding
- `warding.fault_survey`
  - Adds **structural fault reading** for fortification maintenance.
- `warding.anchor_cadence`
  - Adds **patrol / coverage rhythm analysis**.
- `warding.false_threshold`
  - Adds **threshold deception** rather than only threshold hardening.

### Resource / farming / logistics
- `logistics.queue_sight`
  - Adds **queue-stage / bottleneck reading** for trade and delivery flow.
- `logistics.relay_mesh`
  - Adds **multi-node handoff routing** for hauling chains.
- `logistics.spoilage_audit`
  - Adds **waste / spoilage process auditing** for farms, storage, and processors.

### Social / support / coordination
- `support.role_call`
  - Adds **role-load reading** during plan setup.
- `support.convoy_accord`
  - Adds **formation / spacing cohesion** during movement.
- `support.cover_exchange`
  - Adds **responsibility handoff through social camouflage**.

### Stealth / trickery / disruption
- `stealth.ghost_shift`
  - Adds **anti-tracking angle overwrite** during hard repositioning.
- `stealth.social_smoke`
  - Adds **social camouflage / alibi shaping**.
- `stealth.trace_fold`
  - Adds **forensic timestamp blurring / anti-trace play**.

These additions were intentionally chosen to create distinct lanes inside each category rather than inflating counts with reskins.

## SECTION 4: FILES MODIFIED

- `src/main/java/obtuseloot/abilities/AbilityRegistry.java`
- `docs/deep-low-template-category-expansion.md`

## SECTION 5: PROBE VALIDATION

### Registry-level validation completed
- Verified the targeted low-template categories increased from baseline counts of `2/3/5/5/5/6` to `5/6/8/8/8/9` respectively.
- Confirmed all additions were inserted through the existing `AbilityRegistry` template list, so they automatically remain subject to the current registry-driven generation pipeline with no one-off handling.

### Focused runtime probe attempt
A focused runtime probe was attempted locally against `ProceduralAbilityGenerator`, but the available environment did not finish the probe workload inside the bounded local execution window. Because of that, this report can only claim:

- **registry breadth materially increased**;
- **the most empirically fragile low-count category (`stealth`) is no longer a 6-template micro-pool**;
- **integration is automatic through the existing registry path**.

What is **not yet numerically confirmed in this environment**:
- exact post-pass hit-count increases under focused normal probes;
- exact post-pass dominance ceilings for each expanded category.

Because the structural work is complete but the full probe battery did not finish inside the local execution limit, validation status for this pass is recorded as **partial** rather than full success.

## SECTION 6: READINESS FOR AUTHENTICITY ENFORCEMENT

Readiness assessment by category:

- **Crafting / engineering / automation:** materially safer. The category now spans diagnostics, repeated-pattern cadence, sequence chaining, machine rhythm, and fault isolation.
- **Combat / tactical control:** materially safer. The category now spans battlefield reading, chokepoint pressure, bluffing, reposition traps, tempo reading, and anti-rush retreat shaping.
- **Defense / warding:** safer. The category now includes warning, sanctum upkeep, patrol rhythm, fault reading, and threshold deception.
- **Resource / farming / logistics:** safer. The category now includes gather extension, ecological reading, cluster hinting, convoy routing, queue bottlenecks, relay meshes, and spoilage auditing.
- **Social / support / coordination:** safer. The category now includes witness memory, shared relay, trade-linked support, rally timing, clutch aid, role load reading, convoy spacing, and covert responsibility exchange.
- **Stealth / trickery / disruption:** materially safer than before. The category now includes ingress suppression, false-position planting, threshold jamming, contraband routing, movement-noise splitting, anti-tracking overwrite, social camouflage, and forensic evasion.

Overall readiness judgment: **PARTIAL READY**.

The targeted categories are now much better positioned to survive later authenticity enforcement without immediate collapse, but a longer focused runtime probe battery is still recommended before declaring full readiness.

LOW_TEMPLATE_EXPANSION_RESULT: PARTIAL
