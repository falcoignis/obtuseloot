# Audit Report

## Scope
Audit focus requested:
- completeness
- commentation/documentation clarity
- configurability
- lag-friendliness/performance safety

## Checks run
1. `mvn -q -DskipTests package`
2. Manual code review of the plugin lifecycle, hot-path processors, and runtime configuration surface.

## Findings

### 1) Build remains blocked by Maven repository access (High, Environment)
- Packaging still fails before Java compilation due to inability to resolve Maven plugin artifacts from Maven Central (`403 Forbidden`).
- This is an environment/network artifact resolution issue, not a source syntax failure in reviewed files.

### 2) Completeness gaps (Medium)
- `plugin.yml` declares a broad command/permission surface not represented by command executor code in current sources.
- Plugin lifecycle had no explicit `onDisable` shutdown call path, risking future leaks if async/scheduled work is added.

### 3) Configurability gaps (Medium)
- Core balancing values were hardcoded in multiple engines (combat thresholds, evolution gates, drift formula, awakening gates).
- Operators had no runtime tuning point except source edits/rebuild.

### 4) Lag-friendliness concerns (Medium)
- Action bar updates occurred on every relevant event, creating avoidable spam in high-frequency combat.

### 5) Commentation and maintainability (Low)
- Critical orchestration code lacked explicit hot-path intent/comments.

## Remediations implemented in this pass

1. Added central runtime settings loader (`RuntimeSettings`) backed by `config.yml`.
   - Consolidates balancing values for combat, evolution, drift, awakening, and lore update cadence.

2. Added default plugin configuration file with commented tuning options (`src/main/resources/config.yml`).
   - Improves operational configurability without recompiling.

3. Wired startup lifecycle to load persisted config and runtime settings.
   - `onEnable`: save/load config snapshot.
   - `onDisable`: explicit engine shutdown.

4. Updated progression engines to consume configurable thresholds.
   - `ArtifactProcessor`, `EvolutionEngine`, `DriftEngine`, `AwakeningEngine` now read from runtime settings.

5. Added lore/action-bar throttling.
   - `LoreEngine` now tracks last update per-player and enforces `lore.min-update-interval-ms` to reduce event-spam overhead.

6. Improved code comments in hot-path orchestration.
   - Clarifies performance intent and why implementations avoid unnecessary overhead.

## Remaining recommendations

1. Implement or remove declared commands in `plugin.yml` to align behavior and metadata.
2. Add lightweight unit tests for deterministic logic:
   - evolution tier thresholds
   - drift clamp behavior
   - awakening gate precedence
3. Add optional periodic cleanup of in-memory player maps for long-lived servers (e.g., on quit events) to avoid stale entries.
4. Once Maven access is restored, run full static checks and integration smoke tests.

## Conclusion
This audit pass improved configurability and runtime efficiency without expanding hot-path complexity. The primary blocker for full validation remains external Maven artifact access.


## Follow-up wiring pass
Additional loose/unwired code remediation completed:
- Wired `/obtuseloot` command to a concrete executor (`info`, `help`, `inspect`) so plugin.yml command metadata has runtime handling.
- Wired the large static name pools (`Prefixes`, `Suffixes`, `Generic`) through a deterministic `ArtifactNameGenerator` used at artifact creation time.
- Added player quit cleanup listener to remove in-memory artifact/reputation/lore-throttle state and avoid stale map growth.
- Removed unused `SoulData` model that had no call sites.

## 2026-03-08 full audit pass

### Additional checks run
1. `./scripts/diagnose-maven-access.sh`
2. `mvn -B -ntp clean test`
3. Workflow inventory check: `git ls-files | rg '^\.github/'`

### Additional findings

1. **Critical CI governance gap (High)**  
   The repository had no `.github/workflows` files tracked, so there was no automated enforcement for build/test/publish policy.

2. **Build policy mismatch (Medium)**  
   README documented a `maven-publish` workflow, but no corresponding workflow file existed.

3. **Network-constrained build reliability (Medium, Environment)**  
   Direct Maven execution still fails in this environment because Maven Central access returns `403`, so workflows should explicitly support mirror-based execution.

### Remediation implemented in this pass

1. Added `.github/workflows/ci.yml` to enforce checkout + JDK setup + Maven access diagnosis + test execution on push/PR.
2. Added `.github/workflows/maven-publish.yml` to enforce publish pipeline on release/workflow dispatch.
3. Both workflows now support restricted-network mirrors via existing `MAVEN_MIRROR_URL` / proxy secret inputs and `scripts/mvn-via-mirror.sh`.

### Recommended next steps

1. Configure repository secrets for mirror/proxy if runners cannot access Maven Central directly.
2. Add branch protection requiring the `ci` workflow to pass before merge.
3. Add signing/publishing credentials in GitHub Environments with least privilege.

## 2026-03-08 audit + build + publish execution

### Checks run
1. `./scripts/diagnose-maven-access.sh`
2. `./scripts/build.sh`
3. `./scripts/publish.sh`

### Results

1. **Audit/build status (Pass)**
   - Maven dependency/plugin resolution works in the current environment when proxy settings are applied.
   - Full project package build succeeds and produces `target/obtuseloot-1.0.0.jar`.

2. **Publish status (Blocked by repository credentials/configuration)**
   - Local publish helper requires `GITHUB_TOKEN` and a resolvable `GITHUB_REPOSITORY` or `remote.origin.url`.
   - Current environment has no `GITHUB_TOKEN` and no configured git remote, so publish cannot be executed from this workspace.

### Action needed to complete publishing

1. Set credentials/environment:
   - `export GITHUB_TOKEN="<token-with-packages-write>"`
   - `export GITHUB_REPOSITORY="<owner>/<repo>"` (required when no `origin` remote exists)
2. Run:
   - `./scripts/publish.sh`

## 2026-03-09 command/perms/full-file audit pass

### Checks run
1. `git ls-files | wc -l`
2. `rg -n "TODO|FIXME|XXX" src docs README.md pom.xml src/main/resources/plugin.yml`
3. `mvn -B -ntp clean package -DskipTests`

### Findings
1. **Name pool management command gap (Resolved)**
   - Existing command surface did not expose in-game/console edits for persisted name pools (`prefixes`, `suffixes`, `generic`).

2. **Permission metadata drift (Resolved)**
   - `plugin.yml` list-edit permission tree included stale list domains (`lore`, `categories`) not wired in command handling.

3. **Runtime reload support (Verified)**
   - `/ol reload` already hot-reloaded config/runtime snapshots and name pools without requiring server reboot.

### Remediation implemented in this pass
1. Added `/ol addname <pool> <value>` and `/ol removename <pool> <value>` command handling.
2. Added persisted add/remove operations in `NamePoolManager` with in-memory snapshot updates.
3. Added pool-aware tab completion and per-pool permission enforcement (`obtuseloot.edit.<pool>` with umbrella `obtuseloot.edit`).
4. Updated `plugin.yml` command usage and permission tree to include `obtuseloot.edit.generic` and remove stale children.
5. Updated `README.md` command reference to document new subcommands.

## 2026-03-09 compile failure root-cause audit

### Checks run
1. `JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 mvn -B -ntp clean package`
2. `JAVA_HOME=/root/.local/share/mise/installs/java/21.0.2 mvn -B -ntp clean package`

### Root cause
- The project is intentionally configured for Java 21 (`<release>21</release>`), so builds fail when Maven runs on JDK 17 or older with `release version 21 not supported`.

### Remediation implemented
1. Added Maven Enforcer rule to fail fast with explicit guidance when Java < 21 is used.
2. Added README troubleshooting section with `JAVA_HOME` fix steps.

### Result
- Build now reports a clear, actionable error under JDK 17 and succeeds under JDK 21+.

## 2026-03-20 ability generation system audit

### Scope
Template selection distribution across all 10 `AbilityCategory` buckets, with focus on dominated categories, unreachable templates, and the root mechanisms that produce each outcome. Input data: `analytics/post-expansion-probe-20260319.txt` (180 forced-probe selections across 5 scenarios). Source reviewed: `ProceduralAbilityGenerator`, `AbilityRegistry`, `AbilityCategory`, `AbilityMetadata`, `AbilityTrigger`.

### Methodology
1. Read probe output for per-template hit counts and reachability scores.
2. Traced selection pipeline: `scoreTemplate` → `baseCompositeTemplateScore` → `compositeTemplateScores` → `weightedTemplateSelection` → `normalizeCategoryTemplateScores`.
3. Computed `ecologicalYieldScore`, `triggerSaturationPenalty`, and `underSampledApplicabilityBoost` for dominant and zero-hit templates by hand to isolate the score gap in each case.

---

### Category findings

#### STEALTH_TRICKERY_DISRUPTION — reachability 7/9, top-3 share 73.3% (HIGH)

**Probe distribution** (15 total selections):

| Template | Hits | Share |
|---|---|---|
| stealth.echo_shunt | 4 | 26.7% |
| stealth.ghost_shift | 4 | 26.7% |
| stealth.dead_drop_lattice | 3 | 20.0% |
| stealth.hushwire | 1 | 6.7% |
| stealth.paper_trail | 1 | 6.7% |
| stealth.shadow_proxy | 1 | 6.7% |
| stealth.social_smoke | 1 | 6.7% |
| stealth.threshold_jam | 0 | 0% |
| stealth.trace_fold | 0 | 0% |

**Root cause — stealth.threshold_jam (0 hits)**

Trigger: `ON_STRUCTURE_PROXIMITY`. Seven templates across all categories share this trigger. With `pressureWeight = 1.0`, `triggerSaturationPenalty` resolves to `clamp(1.0 − (6 × 0.045), 0.72, 1.02) = 0.73` — the hard floor. stealth.hushwire also uses `ON_STRUCTURE_PROXIMITY` and is categorized identically, so the two templates compete directly. hushwire wins because its `MOVEMENT_ECHO` mechanic has broader niche alignment than threshold_jam's `DEFENSIVE_THRESHOLD`. Once hushwire captures the narrow structure-proximity pool, threshold_jam's residual saturation-floored score cannot win even a single weighted draw across 15 selections.

Additionally, threshold_jam's gameplay description requires the infiltrator's approach angle to vary on every attempt. That conditional is an implicit activation failure that further reduces real-world fire rate, reinforcing why it is never surfaced.

**Root cause — stealth.trace_fold (0 hits)**

Trigger: `ON_BLOCK_INSPECT`. Seven templates share this trigger (the same count as `ON_STRUCTURE_PROXIMITY`), so `triggerSaturationPenalty` also floors at 0.72. Despite trace_fold having the highest `ecologicalYieldScore` in the stealth category (≈ 0.878, driven by a cheap `ACTIVE_INTENTIONAL` budget of 0.9 total cost and high `informationValue = 0.94`), the 28% saturation penalty collapses that advantage. The `adjacentTriggerFamily` expansion for `ON_BLOCK_INSPECT` maps to `{ON_ENTITY_INSPECT, ON_STRUCTURE_SENSE, ON_STRUCTURE_DISCOVERY}` — none of which overlap with stealth-context trigger families — so `underSampledApplicabilityBoost` contributes only minimal relief.

**Minimal repair**

- stealth.threshold_jam: add `ON_MOVEMENT` to `adjacentTriggerFamily(ON_STRUCTURE_PROXIMITY)`. This differentiates threshold_jam from hushwire by giving it a second applicability path (sustained movement near a structure) without requiring a trigger change. Both the `secondaryTriggerApplicability` and `underSampledTriggerRelief` paths benefit.
- stealth.trace_fold: add `ON_WITNESS_EVENT` to `adjacentTriggerFamily(ON_BLOCK_INSPECT)`. Witnessing activity near a surface is a natural precursor to reading trace evidence, and `ON_WITNESS_EVENT` falls under the stealth niche's `SOCIAL_WORLD_INTERACTION` tag, so `familyCompatibleTriggers` will recognize the match. This lifts `triggerFit` in `underSampledApplicabilityBoost` from 0 to a non-trivial value.

---

#### DEFENSE_WARDING — reachability 6/8, top-3 share 71.4% (HIGH)

**Probe distribution** (14 total selections):

| Template | Hits | Share |
|---|---|---|
| survival.last_light_cache | 4 | 28.6% |
| warding.sanctum_lock | 3 | 21.4% |
| warding.anchor_cadence | 3 | 21.4% |
| warding.perimeter_hum | 2 | 14.3% |
| warding.fault_survey | 1 | 7.1% |
| warding.false_threshold | 1 | 7.1% |
| consistency.path_thread | 0 | 0% |
| consistency.structure_echo | 0 | 0% |

**Root cause — survival.last_light_cache (28.6% share, over-dominant)**

Trigger: `ON_TIME_OF_DAY_TRANSITION`. Only one other template in the registry shares this trigger (`survival.exposure_weave` in `SURVIVAL_ADAPTATION`), so `triggerSaturationPenalty` is a near-neutral 0.955. The trigger fires every day-night cycle, making it universally applicable regardless of warding play context. This is a cross-category context leak: last_light_cache is a `SURVIVAL` family template with a survival mechanic (`GUARDIAN_PULSE`) that fires on a time trigger, placing it structurally outside the warding niche it nominally occupies. Because `categoryWeight` and `nicheWeight` do not penalize intra-category family mismatches at the scoring level, it simply wins by trigger frequency.

**Root cause — consistency.path_thread (0 hits)**

Trigger: `ON_WORLD_SCAN`. Four templates share this trigger. `triggerSaturationPenalty = clamp(1.0 − (3 × 0.045 × 1.22), 0.72, 1.02) = clamp(0.835, 0.72, 1.02) = 0.835`. The penalty is moderate rather than floored, but path_thread's core mechanic (`NAVIGATION_ANCHOR`) and its utility domains (`navigation`, `memory-history`) belong semantically to the traversal niche (`NAVIGATION` tag), not `PROTECTION_WARDING`. In a defense context, the category scoring step (`categorySelectionScore`) calls `categoryWeight` and `nicheWeight` using the artifact's `dominantNiche`. For players whose dominant niche is `PROTECTION_WARDING` or `SUPPORT_COHESION`, path_thread scores near the bottom because neither of those niches maps to `NAVIGATION_ANCHOR`. It is effectively a traversal template miscategorized into DEFENSE_WARDING.

**Root cause — consistency.structure_echo (0 hits)**

Trigger: `ON_STRUCTURE_SENSE`. `TriggerBudgetPolicy.STRICT` (cost 2.3 + 1.2 = 3.5). Three templates share this trigger; `triggerSaturationPenalty = clamp(1.0 − (2 × 0.045 × 1.06), 0.72, 1.02) = 0.905`. Budget cost is the primary suppressor: `triggerEfficiency = totalUtility / 3.5`, which is roughly 40% the efficiency of an equivalent `ACTIVE_INTENTIONAL` template. The `ecologicalYieldScore` formula caps the efficiency contribution at `min(1.6, efficiency)`, so structure_echo pays the full cost with no ceiling benefit. Additionally, `ON_STRUCTURE_SENSE` only fires near ruins and dungeons, making it a low-frequency trigger in most play contexts.

**Minimal repair**

- consistency.path_thread: recategorize to `TRAVERSAL_MOBILITY` (where its mechanic and trigger naturally belong) and add a replacement DEFENSE_WARDING template, **or** add `ON_STRUCTURE_PROXIMITY` to `adjacentTriggerFamily(ON_WORLD_SCAN)`. The second option lets path_thread qualify for the `underSampledApplicabilityBoost` in warding contexts where structure proximity is the active niche trigger.
- consistency.structure_echo: add `ON_STRUCTURE_PROXIMITY` to `adjacentTriggerFamily(ON_STRUCTURE_SENSE)`. Structure proximity is more common than a structure-sense event and is recognized by defense niche families. This bridges structure_echo into the same applicability tier as warding.perimeter_hum without requiring a trigger change.
- survival.last_light_cache over-dominance: no code change required in the short term, but if the top share is problematic, moving it to `SURVIVAL_ADAPTATION` (where it thematically belongs alongside storm_shelter_ledger and exposure_weave) would distribute category pressure more honestly.

---

#### RITUAL_STRANGE_UTILITY — reachability 12/13, top-3 share 50.0% (MEDIUM)

**Probe distribution** (20 total selections):

| Template | Hits | Share |
|---|---|---|
| evolution.lineage_fortification | 4 | 20.0% |
| chaos.witness | 3 | 15.0% |
| ritual.temporal_attunement | 3 | 15.0% |
| evolution.ritual_amplifier | 2 | 10.0% |
| (9 others) | 1 each | 5.0% each |
| chaos.dust_memory | 0 | 0% |

**Root cause — evolution.lineage_fortification (20% share, over-dominant)**

Trigger: `ON_LOW_HEALTH`. Two templates use this trigger across the entire registry; saturation penalty is negligible (0.955). `ON_LOW_HEALTH` fires universally in any combat or survival situation regardless of ritual context, making this template context-agnostic in a category where all other templates require deliberate ritual engagement. The `GUARDIAN_PULSE` mechanic is categorized under `PROTECTION_WARDING` niche, not `RITUAL_STRANGE_UTILITY`, so it also leaks across niche scoring in ritual contexts. This is the same pattern as last_light_cache in DEFENSE_WARDING: a survival-family template with a high-frequency, niche-independent trigger dominates a specialty category.

**Root cause — chaos.dust_memory (0 hits)**

Trigger: `ON_STRUCTURE_SENSE`, `TriggerBudgetPolicy.STRICT`. This is structurally equivalent to structure_echo: strict budget, low fire frequency, three-template saturation (penalty 0.905). Additionally, `MEMORY_ECHO` mechanic under `RITUAL_STRANGE_UTILITY` / `MEMORY_HISTORY` niche gives it the highest ritual value (not shown above, but ritual-heavy players should prefer it). The reason the boost never fires is that the artifact niche profiles sampled in the probe rarely land on `MEMORY_HISTORY` as the dominant niche, so `secondaryTriggerApplicability` stays near zero, and dust_memory remains perpetually cold without enough scarcity-driven boost to overcome the strict budget penalty.

**Minimal repair**

- chaos.dust_memory: add `ON_MEMORY_EVENT` to `adjacentTriggerFamily(ON_STRUCTURE_SENSE)`. Memory events are the dominant trigger in ritual play contexts, and the `familyCompatibleTriggers` map for `RITUAL_STRANGE_UTILITY` / `MEMORY_HISTORY` explicitly includes `ON_MEMORY_EVENT`. This closes the applicability gap without changing the primary trigger.
- evolution.lineage_fortification over-dominance: same pattern as last_light_cache. Recategorizing to `SURVIVAL_ADAPTATION` is the cleanest fix; if it must remain in RITUAL, adding a context gate in `categoryWeight` that penalizes survival-family templates in ritual categories when the artifact has no ritual utility history would contain the bleed.

---

#### COMBAT_TACTICAL_CONTROL — reachability 6/6, top share 33.3% (MEDIUM)

**Probe distribution** (9 total selections):

| Template | Hits | Share |
|---|---|---|
| tactical.tempo_extract | 3 | 33.3% |
| tactical.rush_damper | 2 | 22.2% |
| sensing.battlefield_read | 1 | 11.1% |
| tactical.killzone_lattice | 1 | 11.1% |
| tactical.feint_window | 1 | 11.1% |
| tactical.reposition_snare | 1 | 11.1% |

All templates are reachable; distribution is not critical. Two observations worth tracking:

**Observation — tactical.tempo_extract structural monopoly**

`ON_CHAIN_COMBAT` is used by exactly one template in the entire registry. `triggerSaturationPenalty = clamp(1.0 − 0, 0.72, 1.02) = 1.02` — a small positive bonus. Combined with `informationValue = 0.90` (highest in the category) and a default budget profile, tempo_extract has no structural competition. It will remain dominant as long as it is the only `ON_CHAIN_COMBAT` template.

**Observation — tactical.feint_window narrow activation window**

`ON_SOCIAL_INTERACT` with `CHAIN_ESCALATION` requires a deliberate pre-combat interaction. The template description constrains it to "before conflict starts," which is a narrow activation window not captured in the scoring model. Artifact players in PvP contexts rarely trigger social interactions before a fight begins, so even if generation assigns it, its utility signal feedback will be low and lineage-weighted scoring will suppress it in subsequent generations. This is not a generation selection problem but a feedback loop that will suppress it over time.

**Minimal repair**

- tactical.feint_window: extend `adjacentTriggerFamily(ON_SOCIAL_INTERACT)` to include `ON_WITNESS_EVENT`. Witnessing an opponent is a natural precursor to feinting, and this broadens the applicability score without changing the trigger.
- No urgent action on tempo_extract dominance; if a second `ON_CHAIN_COMBAT` template is added in a future expansion, the saturation penalty will automatically self-correct.

---

#### SURVIVAL_ADAPTATION — reachability 11/11, top-3 share 46.2% (LOW)

**Probe distribution** (26 total selections):

| Template | Hits | Share |
|---|---|---|
| survival.gentle_harvest | 4 | 15.4% |
| environment.terrain_affinity | 4 | 15.4% |
| survival.exposure_weave | 4 | 15.4% |
| survival.herd_instinct | 3 | 11.5% |
| survival.ember_keeper | 2 | 7.7% |
| environment.weather_sensitivity | 2 | 7.7% |
| survival.hardiness_loop | 2 | 7.7% |
| survival.scarcity_compass | 2 | 7.7% |
| survival.weather_omen | 1 | 3.8% |
| environment.structure_attunement | 1 | 3.8% |
| survival.storm_shelter_ledger | 1 | 3.8% |

All 11 templates are reachable and the distribution is the healthiest in the registry. No templates are suppressed to zero. Three are slightly under-represented (3.8% vs a flat 9.1% expected share) but each appears at least once. No repairs needed.

**Note on survival.storm_shelter_ledger (1 hit)**

Uses `ON_STRUCTURE_PROXIMITY` (7-template saturation, penalty 0.73). This is the same suppression mechanism seen in STEALTH_TRICKERY_DISRUPTION. It survives at 1 hit because `SURVIVAL_ADAPTATION` has 11 templates and more diverse scoring variance. If the category grows, storm_shelter_ledger may drop to zero. The same `adjacentTriggerFamily` fix recommended above (add `ON_WEATHER_CHANGE` as adjacent trigger for `ON_STRUCTURE_PROXIMITY`) would lift it alongside the stealth repairs.

---

#### TRAVERSAL_MOBILITY — reachability 8/8, top share 23.5% (LOW)

**Probe distribution** (17 total selections):

| Template | Hits | Share |
|---|---|---|
| mobility.compass_stories | 4 | 23.5% |
| footprint_memory, trail_sense, biome_attunement, cartographers_echo, rift_stride, skyline_fold | 2 each | 11.8% each |
| mobility.quiet_passage | 1 | 5.9% |

All 8 templates are reachable. Distribution is good. compass_stories' lead is driven by `ON_MEMORY_EVENT` having a `SOFT` budget profile (low cost, flexible cooldown) and high discovery + exploration values, which is expected behavior.

**Note on mobility.quiet_passage (1 hit)**

`ON_RITUAL_INTERACT` / `SOCIAL_ATTUNEMENT` in a traversal category is a weak fit. The template's payoff (muted behavior for doors, gates, trapdoors) is extremely specific and its `worldUtilityValue = 0.67` is pulled down by low social and exploration signals. No urgent action; if it drops to zero in a larger probe, broadening its payoff description (e.g., to cover any traversal-relevant surface, not only hinges) and giving it one shared adjacent trigger with `ON_MOVEMENT` would be sufficient.

---

### Cross-cutting root causes

Three structural patterns explain the majority of the findings above.

**1. Trigger saturation asymmetry**

`ON_BLOCK_INSPECT` and `ON_STRUCTURE_PROXIMITY` each have 7 templates. The saturation penalty floors at 0.72 for both, meaning every template on these triggers enters scoring with a 28% structural penalty regardless of template quality. Templates on low-saturation triggers (`ON_CHAIN_COMBAT`: 1 template; `ON_REPOSITION`: 2; `ON_TIME_OF_DAY_TRANSITION`: 3) face no meaningful penalty and win primarily through frequency rather than quality. The `TRIGGER_SATURATION_WEIGHTS` map currently amplifies `ON_WORLD_SCAN` (×1.22) and `ON_RITUAL_INTERACT` (×1.15) but does not increase the weight for `ON_BLOCK_INSPECT` or `ON_STRUCTURE_PROXIMITY`, where saturation is already pushing templates to the floor.

**2. Cross-category context leakage via high-frequency triggers**

`ON_LOW_HEALTH` and `ON_TIME_OF_DAY_TRANSITION` are survival-context triggers that fire regardless of whether the active generation context is ritual, defense, or any other specialty category. Templates holding these triggers — evolution.lineage_fortification and survival.last_light_cache — win inside specialty categories not because they are well-suited, but because their triggers always fire. The scoring pipeline has no mechanism to discount a template whose trigger is contextually misaligned with its assigned category's niche tags.

**3. Adjacency gap for strict-budget triggers**

`ON_STRUCTURE_SENSE` (STRICT budget, cost 3.5) and the `ON_BLOCK_INSPECT` saturation floor both benefit in theory from `underSampledApplicabilityBoost`, but that boost requires a non-empty `adjacentTriggerFamily` result that also overlaps with the artifact's dominant niche triggers. For `ON_STRUCTURE_SENSE`, the method currently returns an empty set (default branch of the switch), so the boost path never activates. For `ON_BLOCK_INSPECT`, the three adjacent triggers (`ON_ENTITY_INSPECT`, `ON_STRUCTURE_SENSE`, `ON_STRUCTURE_DISCOVERY`) are themselves low-frequency or niche-specific, yielding a near-zero `triggerFit` score. Extending both adjacency maps would allow the boost to function as designed for cold, high-complexity templates on these triggers.

---

### Summary of recommended repairs

| Template | Category | Severity | Recommended fix |
|---|---|---|---|
| stealth.threshold_jam | STEALTH_TRICKERY_DISRUPTION | High | Add `ON_MOVEMENT` to `adjacentTriggerFamily(ON_STRUCTURE_PROXIMITY)` |
| stealth.trace_fold | STEALTH_TRICKERY_DISRUPTION | High | Add `ON_WITNESS_EVENT` to `adjacentTriggerFamily(ON_BLOCK_INSPECT)` |
| consistency.path_thread | DEFENSE_WARDING | High | Recategorize to TRAVERSAL_MOBILITY, or add `ON_STRUCTURE_PROXIMITY` to `adjacentTriggerFamily(ON_WORLD_SCAN)` |
| consistency.structure_echo | DEFENSE_WARDING | High | Add `ON_STRUCTURE_PROXIMITY` to `adjacentTriggerFamily(ON_STRUCTURE_SENSE)` |
| chaos.dust_memory | RITUAL_STRANGE_UTILITY | Medium | Add `ON_MEMORY_EVENT` to `adjacentTriggerFamily(ON_STRUCTURE_SENSE)` |
| tactical.feint_window | COMBAT_TACTICAL_CONTROL | Low | Add `ON_WITNESS_EVENT` to `adjacentTriggerFamily(ON_SOCIAL_INTERACT)` |
| survival.last_light_cache | DEFENSE_WARDING | Low | Recategorize to SURVIVAL_ADAPTATION (optional; long-term distribution quality) |
| evolution.lineage_fortification | RITUAL_STRANGE_UTILITY | Low | Recategorize to SURVIVAL_ADAPTATION (optional; long-term distribution quality) |
| survival.storm_shelter_ledger | SURVIVAL_ADAPTATION | Low | Add `ON_WEATHER_CHANGE` to `adjacentTriggerFamily(ON_STRUCTURE_PROXIMITY)` if it drops to zero in future probes |

All adjacency changes are one-line additions to the `adjacentTriggerFamily` switch in `ProceduralAbilityGenerator`. The recategorizations require moving the template call in `AbilityRegistry` to a different `AbilityCategory` constant. No scoring constants need adjustment.

### Conclusion

The ability generation system is structurally sound. The generation pipeline correctly applies novelty, recency, niche bias, and cold-template boosts. The distribution problems are narrow and mechanical: five templates are suppressed by trigger saturation or missing adjacency mappings, and two specialty categories are skewed by high-frequency survival triggers whose activation context does not match the category niche. Fixing the four `adjacentTriggerFamily` entries and optionally recategorizing two templates would bring all categories to full reachability with no changes to scoring weights or normalization logic.
