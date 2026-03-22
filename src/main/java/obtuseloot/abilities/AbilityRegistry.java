package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AbilityRegistry {
    private final List<AbilityTemplate> templates;

    public AbilityRegistry() {
        this.templates = List.of(
                template("precision.echo_locator", "Echo Locator", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.SENSE_PING,
                        "Resonant pulses reveal nearby hollows when crossing new terrain", "deeper echo strata", "drift introduces phantom echoes", "awakening stabilizes pulse geometry", "convergence shares cave cues", "memory records hollow corridors",
                        metadata(Set.of("environmental-sensing", "information"), Set.of("movement-throttled"), Set.of("watchful", "exploration", "memory"), 0.72D, 0.81D, 0.78D, 0.22D, 0.20D, 0.41D)),
                template("precision.vein_whisper", "Vein Whisper", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.INSIGHT_REVEAL,
                        "Interprets chunk composition into soft ore-likelihood hints", "better material confidence", "drift shifts probabilities", "awakening filters false positives", "convergence triangulates material drift", "memory stores productive seams",
                        metadata(Set.of("informational-interpretation", "environmental-sensing"), Set.of("chunk-change"), Set.of("watchful", "exploration"), 0.68D, 0.76D, 0.84D, 0.15D, 0.18D, 0.46D)),
                template("precision.material_insight", "Material Insight", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.INSIGHT_REVEAL,
                        "Inspected blocks whisper practical context and odd utility lore", "broader material lexicon", "drift adds cryptic readings", "awakening clarifies odd traits", "convergence links shared interpretations", "memory curates known materials",
                        metadata(Set.of("information", "world-interpretation"), Set.of("intentional-interact"), Set.of("watchful", "curious"), 0.61D, 0.44D, 0.90D, 0.25D, 0.24D, 0.51D)),

                template("mobility.footprint_memory", "Footprint Memory", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.MOBILITY, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Sneak-use marks a location and later points back to it", "longer recall range", "drift can misalign memory", "awakening anchors dimension-safe recall", "convergence broadcasts anchor to allies", "memory persists trusted waypoints",
                        metadata(Set.of("navigation", "memory-history"), Set.of("gesture-based"), Set.of("memory", "exploration", "worldkeeper"), 0.79D, 0.88D, 0.52D, 0.35D, 0.22D, 0.55D)),
                template("mobility.compass_stories", "Compass of Stories", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.MOBILITY, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Points toward recorded notable events and lineage residues", "locks stronger event traces", "drift surfaces contradictory trails", "awakening resolves ancestral signatures", "convergence blends shared story maps", "memory prioritizes meaningful landmarks",
                        metadata(Set.of("navigation", "memory-history", "structure-awareness"), Set.of("memory-driven"), Set.of("memory", "lineage", "ritual"), 0.86D, 0.82D, 0.63D, 0.48D, 0.26D, 0.39D)),
                template("mobility.quiet_passage", "Quiet Passage", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.MOBILITY, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Doors, gates, and trapdoors respond with muted passage behavior", "longer hush duration", "drift can over-silence", "awakening extends to nearby hinges", "convergence synchronizes passage fields", "memory recognizes familiar thresholds",
                        metadata(Set.of("world-interaction", "social-flavor"), Set.of("block-interact"), Set.of("worldkeeper", "support"), 0.42D, 0.54D, 0.36D, 0.40D, 0.78D, 0.67D)),

                template("survival.gentle_harvest", "Gentle Harvest", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_BLOCK_HARVEST, AbilityMechanic.HARVEST_RELAY,
                        "Mature crops are replanted with respectful timing", "more crop families supported", "drift occasionally skips replant", "awakening stabilizes replant certainty", "convergence helps nearby harvested rows", "memory tracks fertile habits",
                        metadata(Set.of("farming-building", "world-interaction"), Set.of("block-break-filtered"), Set.of("worldkeeper", "support"), 0.57D, 0.38D, 0.34D, 0.20D, 0.59D, 0.91D)),
                template("survival.ember_keeper", "Ember Keeper", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.RITUAL_CHANNEL,
                        "Campfires retain gentle utility states when tended by hand", "longer ember memory", "drift causes flicker moods", "awakening steadies flame intent", "convergence shares ember calm", "memory ties warmth to safe sites",
                        metadata(Set.of("ritual-utility", "world-interaction"), Set.of("campfire-interact"), Set.of("reverent", "support", "ritual"), 0.46D, 0.41D, 0.31D, 0.88D, 0.52D, 0.74D)),

                template("chaos.dust_memory", "Dust Memory", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityMechanic.MEMORY_ECHO,
                        "Ancient spaces stir residue hints and subtle direction", "more residue vocabularies", "drift intensifies strange omens", "awakening clarifies true residue", "convergence harmonizes residue signatures", "memory binds old places to lineage",
                        metadata(Set.of("memory-history", "structure-awareness", "ritual-utility"), Set.of("chunk-structure-entry"), Set.of("ritual", "memory", "exploration"), 0.83D, 0.74D, 0.67D, 0.79D, 0.33D, 0.44D)),
                template("chaos.witness", "Witness", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_WITNESS_EVENT, AbilityMechanic.REVENANT_TRIGGER,
                        "Observed moments imprint into future reactive echoes", "broader witness lexicon", "drift echoes false witness residue", "awakening filters witness truth", "convergence cross-links observer traces", "memory chains witness motifs",
                        metadata(Set.of("memory-history", "social-flavor", "ritual-utility"), Set.of("witness-reactive"), Set.of("memory", "ritual", "watchful"), 0.73D, 0.64D, 0.58D, 0.84D, 0.69D, 0.35D)),

                template("consistency.path_thread", "Path Thread", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Moving through unfamiliar terrain leaves subtle return-thread trails", "longer route memory", "drift can braid false trails", "awakening steadies reliable routes", "convergence interleaves nearby trail maps", "memory stores trusted route threads",
                        metadata(Set.of("navigation", "memory-history"), Set.of("movement-throttled"), Set.of("exploration", "memory"), 0.75D, 0.90D, 0.62D, 0.28D, 0.30D, 0.55D)),
                template("survival.weather_omen", "Weather Omen", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.INSIGHT_REVEAL,
                        "Sky pressure cues forecast short-term weather turns", "longer forecast horizon", "drift adds dramatic false omens", "awakening filters chaotic fronts", "convergence syncs omens across artifacts", "memory records recurring climate loops",
                        metadata(Set.of("environmental-sensing", "information"), Set.of("weather-poll"), Set.of("watchful", "support"), 0.66D, 0.58D, 0.84D, 0.31D, 0.42D, 0.63D)),
                template("precision.artifact_sympathy", "Artifact Sympathy", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Nearby artifacts reveal affinity hints during intentional greetings", "deeper affinity hints", "drift yields contradictory impressions", "awakening clarifies emotional residue", "convergence links sympathy contexts", "memory retains known signatures",
                        metadata(Set.of("social-flavor", "information"), Set.of("player-intent"), Set.of("support", "memory", "watchful"), 0.71D, 0.52D, 0.77D, 0.24D, 0.91D, 0.35D)),
                template("chaos.ritual_echo", "Ritual Echo", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.RITUAL_CHANNEL,
                        "Completed rituals leave resonant echoes for follow-up interactions", "longer echo chains", "drift creates misleading echoes", "awakening separates true ritual signatures", "convergence braids compatible echoes", "memory archives ritual cadence",
                        metadata(Set.of("ritual-utility", "memory-history"), Set.of("ritual-completion"), Set.of("ritual", "memory"), 0.80D, 0.60D, 0.65D, 0.94D, 0.36D, 0.40D)),
                template("consistency.structure_echo", "Structure Echo", AbilityCategory.SENSING_INFORMATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityMechanic.SENSE_PING,
                        "Ruins and dungeons project directional echo gradients", "higher structure certainty", "drift can offset bearings", "awakening locks ancient signatures", "convergence triangulates echoes", "memory keeps stable structure routes",
                        metadata(Set.of("structure-awareness", "navigation"), Set.of("chunk-structure-entry"), Set.of("exploration", "watchful"), 0.78D, 0.88D, 0.71D, 0.40D, 0.29D, 0.52D)),
                template("survival.herd_instinct", "Herd Instinct", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_ENTITY_INSPECT, AbilityMechanic.INSIGHT_REVEAL,
                        "Animal groups reveal migration and safety tendencies", "deeper migration context", "drift overstates danger", "awakening improves behavior confidence", "convergence blends herd observations", "memory stores seasonal movement",
                        metadata(Set.of("environmental-sensing", "social-world-behavior"), Set.of("entity-interact"), Set.of("support", "watchful", "worldkeeper"), 0.63D, 0.74D, 0.82D, 0.20D, 0.58D, 0.70D)),

                template("exploration.trail_sense", "Trail Sense", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.MOBILITY, AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.TRAIL_SENSE,
                        "Entering new or rarely visited chunks reveals bounded unexplored direction hints", "extends frontier hint horizon", "drift can bias stale frontier echoes", "awakening stabilizes uncharted bearings", "convergence shares local frontier vectors", "memory logs exploration chain opportunities",
                        metadata(Set.of("exploration", "pathfinding"), Set.of("coalesced-movement", "chunk-aware-cache"), Set.of("exploration", "environmental", "memory"), 0.95D, 0.97D, 0.74D, 0.08D, 0.12D, 0.70D)),
                template("exploration.biome_attunement", "Biome Attunement", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.PRECISION, AbilityTrigger.ON_BIOME_CHANGE, AbilityMechanic.BIOME_RESONANCE,
                        "Biome transitions surface nearby biome-native resource hints", "extends native material memory", "drift can bias wrong biome assumptions", "awakening separates biome signatures", "convergence aligns neighboring biome reads", "memory stores biome yield quality",
                        metadata(Set.of("exploration", "resource-sensing"), Set.of("biome-transition"), Set.of("exploration", "environmental", "watchful"), 0.88D, 0.93D, 0.86D, 0.08D, 0.15D, 0.72D)),
                template("exploration.cartographers_echo", "Cartographer's Echo", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHERS_ECHO,
                        "Structure discoveries surface bounded follow-on structure discovery vectors", "chains nearby discovery breadcrumbs", "drift can add low-confidence lead-ins", "awakening improves follow-on confidence", "convergence links neighboring discovery pressure", "memory stores structure chaining corridors",
                        metadata(Set.of("exploration", "structure-discovery"), Set.of("structure-event", "structure-cache"), Set.of("exploration", "memory", "watchful"), 0.96D, 0.93D, 0.82D, 0.10D, 0.10D, 0.66D)),

                template("gathering.forager_memory", "Forager Memory", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.SURVIVAL, AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGER_MEMORY,
                        "Harvest streaks reveal bounded nearby matching-resource extension opportunities", "extends local streak memory", "drift can lock onto low-value extensions", "awakening improves streak fidelity", "convergence shares active streak motifs", "memory tracks harvest-chain density",
                        metadata(Set.of("gathering", "resource-detection", "cluster-locality"), Set.of("harvest-streak", "bounded-search"), Set.of("gathering", "support", "memory"), 0.88D, 0.73D, 0.76D, 0.06D, 0.11D, 0.95D)),
                template("gathering.ecological_sense", "Ecological Sense", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.RESOURCE_ECOLOGY_SCAN,
                        "Block inspection reveals localized biome resource balance indicators", "improves balance confidence", "drift can overstate scarcity", "awakening corrects ecosystem baselines", "convergence compares nearby ecological reads", "memory archives regional diversity profiles",
                        metadata(Set.of("gathering", "ecosystem-awareness"), Set.of("intentional-interact"), Set.of("gathering", "environmental", "watchful"), 0.80D, 0.69D, 0.93D, 0.12D, 0.08D, 0.89D)),
                template("gathering.gatherers_intuition", "Gatherer's Intuition", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.SURVIVAL, AbilityTrigger.ON_ITEM_PICKUP, AbilityMechanic.CLUSTER_INTUITION,
                        "Item pickups occasionally mark nearby resource clusters", "raises cluster confidence", "drift can select stale clusters", "awakening filters exhausted nodes", "convergence relays active cluster hints", "memory keeps productive pocket traces",
                        metadata(Set.of("gathering", "cluster-detection"), Set.of("pickup-event"), Set.of("gathering", "exploration", "support"), 0.82D, 0.74D, 0.78D, 0.05D, 0.16D, 0.91D)),

                template("ritual.pattern_resonance", "Pattern Resonance", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE,
                        "Repeated player block patterns trigger short-lived local ritual resonance amplification", "extends resonance windows", "drift can resonate ambiguous motifs", "awakening isolates repeatable ritual signatures", "convergence synchronizes nearby pattern resonance", "memory records high-signal ritual cadence",
                        metadata(Set.of("ritual", "pattern-recognition", "lineage-signaling"), Set.of("pattern-repeat", "coalesced-pattern-events"), Set.of("ritual", "memory", "lineage"), 0.80D, 0.56D, 0.78D, 0.98D, 0.24D, 0.53D)),
                template("ritual.altar_resonance", "Altar Resonance", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_RITUAL_COMPLETION, AbilityMechanic.ALTAR_SIGNAL_BOOST,
                        "Completed rituals temporarily amplify artifact evolution signal weight", "improves resonance carry-over", "drift can over-amplify weak rituals", "awakening controls resonance bounds", "convergence links compatible altar events", "memory anchors ritual activation context",
                        metadata(Set.of("ritual", "lineage-signaling"), Set.of("ritual-completion"), Set.of("ritual", "lineage", "support"), 0.83D, 0.51D, 0.71D, 0.99D, 0.34D, 0.49D)),
                template("ritual.temporal_attunement", "Temporal Attunement", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityMechanic.TEMPORAL_SPECIALIZATION,
                        "Day-night transitions bias utility toward diurnal/nocturnal specialization", "sharpens cycle specialization", "drift may invert cycle preference", "awakening smooths phase transitions", "convergence syncs nearby cycle bias", "memory tracks temporal utility shift",
                        metadata(Set.of("ritual", "temporal-adaptation"), Set.of("time-transition"), Set.of("ritual", "environmental", "exploration"), 0.76D, 0.67D, 0.84D, 0.88D, 0.19D, 0.64D)),

                template("social.witness_imprint", "Witness Imprint", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT,
                        "Player-observed events imprint witness-aware narrative memory into lineage state", "expands witness memory context", "drift can record noisy observers", "awakening prioritizes reliable witness contexts", "convergence links shared witness records", "memory accumulates witness interactions",
                        metadata(Set.of("social", "lineage-narrative", "co-presence"), Set.of("player-witness", "event-local-proximity"), Set.of("social", "memory", "lineage"), 0.72D, 0.62D, 0.84D, 0.28D, 0.97D, 0.42D)),
                template("social.collective_insight", "Collective Insight", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.MOBILITY, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.COLLECTIVE_RELAY,
                        "Group actions let nearby artifacts share temporary trigger opportunities", "extends relay breadth", "drift can relay redundant opportunities", "awakening improves relay relevance", "convergence deepens group synchronization", "memory stores cooperative trigger windows",
                        metadata(Set.of("social", "cooperation-relay"), Set.of("group-action"), Set.of("social", "support", "exploration"), 0.74D, 0.68D, 0.81D, 0.22D, 0.97D, 0.57D)),
                template("social.trader_whisper", "Trader Whisper", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.TRADE_SCENT,
                        "Trades briefly improve nearby resource detection quality", "improves trade-linked sensing duration", "drift can bias toward stale nodes", "awakening filters low-utility trade echoes", "convergence shares trade scent profiles", "memory tracks trade interaction effects",
                        metadata(Set.of("social", "resource-economy"), Set.of("trade-event"), Set.of("social", "gathering", "support"), 0.79D, 0.63D, 0.80D, 0.20D, 0.94D, 0.73D)),

                template("evolution.entropy_pulse", "Entropy Pulse", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.UNSTABLE_DETONATION,
                        "Branch divergence spikes mutation intensity for short exploratory windows", "divergence-sensitive mutation cadence", "drift amplifies stochastic branches", "awakening bounds pulse instability", "convergence shares entropy probes", "memory marks divergence shocks",
                        metadata(Set.of("exploration-mutation", "lineage-divergence"), Set.of("lineage-divergence"), Set.of("exploration", "chaos", "lineage"), 0.82D, 0.91D, 0.42D, 0.63D, 0.18D, 0.37D)),
                template("evolution.resource_parasitism", "Resource Parasitism", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.RESOURCE_ECOLOGY_SCAN,
                        "Siphons opportunity from dominant concentrations to increase anti-meta pressure", "improves anti-dominance timing", "drift risks overextension", "awakening stabilizes siphon windows", "convergence triangulates dominant pressure", "memory tracks dominant collapse windows",
                        metadata(Set.of("anti-dominance", "competitive-pressure"), Set.of("dominance-interaction"), Set.of("chaos", "support", "lineage"), 0.66D, 0.58D, 0.44D, 0.31D, 0.36D, 0.71D)),
                template("evolution.ritual_amplifier", "Ritual Amplifier", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_RITUAL_COMPLETION, AbilityMechanic.RITUAL_STABILIZATION,
                        "Repeated ritual behaviors deepen utility and coherence with bounded drift", "longer ritual coherence runs", "drift lowers adaptation elasticity", "awakening stabilizes ritual loops", "convergence synchronizes ritual cohorts", "memory preserves ritual chains",
                        metadata(Set.of("ritual-specialization", "coherence"), Set.of("ritual-repeat"), Set.of("ritual", "consistency", "support"), 0.55D, 0.32D, 0.40D, 0.94D, 0.26D, 0.48D)),
                template("evolution.lineage_fortification", "Lineage Fortification", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_LOW_HEALTH, AbilityMechanic.GUARDIAN_PULSE,
                        "Collapse-prone branches gain temporary resilience at high upkeep cost", "better collapse grace timing", "drift inflates maintenance burden", "awakening hardens vulnerable branches", "convergence shares fortification cues", "memory records survival recoveries",
                        metadata(Set.of("collapse-defense", "lineage-resilience"), Set.of("instability-window"), Set.of("survival", "lineage", "support"), 0.47D, 0.29D, 0.33D, 0.22D, 0.34D, 0.92D)),
                template("evolution.niche_architect", "Niche Architect", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.ECOLOGICAL_PATHING,
                        "Sustained niche occupation reinforces utility density and niche identity", "stronger niche reinforcement slope", "drift reduces cross-niche mobility", "awakening tunes niche persistence", "convergence shares engineered niche signals", "memory captures niche construction history",
                        metadata(Set.of("niche-engineering", "specialization"), Set.of("niche-stability"), Set.of("worldkeeper", "consistency", "exploration"), 0.60D, 0.52D, 0.49D, 0.36D, 0.27D, 0.89D)),

                template("environment.weather_sensitivity", "Weather Sensitivity", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_WEATHER_CHANGE, AbilityMechanic.WEATHER_ATTUNEMENT,
                        "Weather shifts increase success of weather-dependent utility abilities", "improves adaptation horizon", "drift can react to false weather edges", "awakening filters noisy weather transitions", "convergence aligns weather adaptation signals", "memory tracks weather adaptation rate",
                        metadata(Set.of("environmental", "adaptation"), Set.of("weather-change"), Set.of("environmental", "support", "watchful"), 0.72D, 0.70D, 0.79D, 0.30D, 0.21D, 0.95D)),
                template("environment.structure_attunement", "Structure Attunement", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.STRUCTURE_ATTUNEMENT,
                        "Structure proximity boosts utility density for structure-bound abilities", "expands structure utility bands", "drift can over-focus dense ruins", "awakening improves proximity gating", "convergence triangulates nearby structure pressures", "memory tracks structure niche pressure",
                        metadata(Set.of("environmental", "structure-awareness"), Set.of("structure-proximity"), Set.of("environmental", "exploration", "watchful"), 0.91D, 0.84D, 0.74D, 0.28D, 0.17D, 0.82D)),
                template("environment.terrain_affinity", "Terrain Affinity", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.MOBILITY, AbilityTrigger.ON_ELEVATION_CHANGE, AbilityMechanic.TERRAIN_ADAPTATION,
                        "Elevation shifts adapt artifact utility toward local verticality patterns", "improves terrain adaptation memory", "drift can overweight noisy vertical moves", "awakening smooths terrain variance response", "convergence shares terrain adaptation gradients", "memory captures terrain variance interactions",
                        metadata(Set.of("environmental", "terrain-adaptation"), Set.of("elevation-shift"), Set.of("environmental", "exploration", "mobility"), 0.84D, 0.90D, 0.73D, 0.18D, 0.20D, 0.88D)),

                template("mobility.rift_stride", "Rift Stride", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.MOBILITY, AbilityTrigger.ON_ELEVATION_CHANGE, AbilityMechanic.MOVEMENT_ECHO,
                        "Vertical transitions cache a bounded rebound line for quick terrain correction", "extends rebound safety window", "drift can overshoot landing bands", "awakening stabilizes cliffside routing", "convergence links squad repositioning lanes", "memory stores hard-earned ascent lines",
                        metadata(Set.of("traversal", "vertical-routing"), Set.of("elevation-shift", "movement-cadence"), Set.of("exploration", "mobility", "fighter"), 0.78D, 0.96D, 0.58D, 0.10D, 0.22D, 0.61D)),
                template("mobility.skyline_fold", "Skyline Fold", AbilityCategory.TRAVERSAL_MOBILITY, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.CARTOGRAPHIC_ECHO,
                        "Open-sky chunk entry compresses broad routes into safe traversal bands", "widens corridor certainty", "drift can favor scenic detours", "awakening filters dead-end skylines", "convergence overlays convoy route folds", "memory ranks the safest long-haul routes",
                        metadata(Set.of("traversal", "route-compression"), Set.of("chunk-aware-cache", "open-sky-scan"), Set.of("exploration", "support", "logistics"), 0.82D, 0.94D, 0.70D, 0.08D, 0.34D, 0.66D)),

                template("sensing.contraband_tell", "Contraband Tell", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.MARK,
                        "Trades leave short-lived tells that hint whether goods came from hidden stockpiles", "improves suspicious-route confidence", "drift can overflag common wares", "awakening filters normal market churn", "convergence shares black-market heuristics", "memory tracks recurring suspicious signatures",
                        metadata(Set.of("market-sensing", "forensic-information"), Set.of("trade-event", "economy-read"), Set.of("trader", "information", "stealth"), 0.86D, 0.48D, 0.95D, 0.12D, 0.63D, 0.44D)),
                template("sensing.faultline_ledger", "Faultline Ledger", AbilityCategory.SENSING_INFORMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_ELEVATION_CHANGE, AbilityMechanic.SENSE_PING,
                        "Abrupt climbs and drops expose hidden stress lines that hint at buried caverns, liquids, or worked stone beneath the route", "deepens faultline confidence", "drift can chase noisy terrain scars", "awakening separates natural seams from player shaping", "convergence triangulates subterranean pressure with allies", "memory stores which vertical cuts concealed the truth",
                        metadata(Set.of("terrain-reading", "subsurface-information", "environmental-sensing"), Set.of("elevation-shift", "terrain-contrast", "subsurface-probe"), Set.of("watchful", "exploration", "information"), 0.84D, 0.88D, 0.94D, 0.10D, 0.18D, 0.58D)),
                template("sensing.witness_lag", "Witness Lag", AbilityCategory.SENSING_INFORMATION, AbilityFamily.CHAOS, AbilityTrigger.ON_WITNESS_EVENT, AbilityMechanic.WITNESS_IMPRINT,
                        "Freshly witnessed actions leave an afterimage that can reveal whether a player or mob doubled back, hesitated, or staged a feint", "extends afterimage coherence", "drift can animate innocent hesitation", "awakening distinguishes panic from deliberate bait", "convergence cross-checks multiple witness angles", "memory learns who habitually baits pursuit",
                        metadata(Set.of("behavior-reading", "forensic-information", "social-sensing"), Set.of("witness-reactive", "movement-reconstruction", "intent-read"), Set.of("watchful", "stealth", "pvp"), 0.82D, 0.67D, 0.96D, 0.20D, 0.62D, 0.40D)),
                template("sensing.cache_resonance", "Cache Resonance", AbilityCategory.SENSING_INFORMATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.MEMORY_ECHO,
                        "Inspecting storage or crafted blocks surfaces disturbance layers that hint at hidden compartments, recent access cadence, or decoy stashes", "improves disturbance stratification", "drift can romanticize clutter as concealment", "awakening filters lived-in noise from deliberate hiding", "convergence links multiple inspections into one stash theory", "memory keeps the cadence of real dead drops",
                        metadata(Set.of("forensic-information", "storage-reading", "memory-history"), Set.of("intentional-interact", "inventory-surface-read", "disturbance-layer"), Set.of("watchful", "trader", "memory"), 0.88D, 0.46D, 0.97D, 0.18D, 0.31D, 0.63D)),
                template("sensing.route_grammar", "Route Grammar", AbilityCategory.SENSING_INFORMATION, AbilityFamily.MOBILITY, AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.INSIGHT_REVEAL,
                        "Chunk-to-chunk movement patterns are parsed into likely travel grammar, exposing where routes narrow, loop, or imply hidden destinations", "widens route-sentence length", "drift can overread decorative paths", "awakening isolates purposeful traffic from wander", "convergence merges multiple route grammars into corridor predictions", "memory records the syntax of profitable journeys",
                        metadata(Set.of("path-analysis", "movement-information", "exploration"), Set.of("chunk-aware-cache", "route-sequencing", "travel-inference"), Set.of("watchful", "exploration", "trader"), 0.85D, 0.91D, 0.93D, 0.08D, 0.27D, 0.57D)),
                template("sensing.battlefield_read", "Battlefield Read", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.INSIGHT_REVEAL,
                        "Witnessed clashes expose temporary spacing and flank pressure hints", "deeper skirmish geometry reads", "drift can overreact to feints", "awakening isolates genuine pressure lines", "convergence broadcasts threat contours to allies", "memory stores recurring duel patterns",
                        metadata(Set.of("tactical-information", "skirmish-read"), Set.of("player-witness", "combat-adjacent"), Set.of("fighter", "support", "pvp"), 0.72D, 0.71D, 0.92D, 0.10D, 0.51D, 0.47D)),

                template("survival.hardiness_loop", "Hardiness Loop", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_WEATHER_CHANGE, AbilityMechanic.RECOVERY_WINDOW,
                        "Sudden weather swings grant a bounded attrition-recovery loop for harsh travel", "extends adaptation duration", "drift can trigger on mild fronts", "awakening smooths extreme climate pivots", "convergence shares resilience tempo nearby", "memory learns the cost of stubborn routes",
                        metadata(Set.of("survival", "attrition-control"), Set.of("weather-change", "harsh-travel"), Set.of("survival", "hardcore", "support"), 0.62D, 0.73D, 0.68D, 0.18D, 0.27D, 0.94D)),
                template("survival.storm_shelter_ledger", "Storm Shelter Ledger", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Shelter thresholds remember recent weather exposure and open a staged refuge window before the next front lands", "extends refuge grace depth", "drift can bind to unsafe ruins", "awakening separates shelter from trap geometry", "convergence links fallback shelters into a storm chain", "memory preserves shelters that truly bought time",
                        metadata(Set.of("survival", "shelter-management", "weather-adaptation"), Set.of("structure-proximity", "storm-pressure", "fallback-window"), Set.of("survival", "builder", "hardcore"), 0.74D, 0.69D, 0.81D, 0.22D, 0.28D, 0.97D)),
                template("survival.exposure_weave", "Exposure Weave", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityMechanic.TEMPORAL_SPECIALIZATION,
                        "Repeated day-night exposure in the same biome builds a short adaptation weave that swaps between travel resilience and camp efficiency", "improves weave duration", "drift can invert the preferred phase", "awakening preserves the useful half of the cycle", "convergence shares exposure rhythm across nearby allies", "memory records which climates demanded patience",
                        metadata(Set.of("survival", "temporal-adaptation", "environmental-rhythm"), Set.of("time-transition", "biome-repeat", "exposure-window"), Set.of("survival", "watchful", "support"), 0.69D, 0.76D, 0.72D, 0.24D, 0.33D, 0.92D)),
                template("survival.scarcity_compass", "Scarcity Compass", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.PRECISION, AbilityTrigger.ON_ITEM_PICKUP, AbilityMechanic.RESOURCE_ECOLOGY_SCAN,
                        "Uneven pickup streaks read local scarcity, warning when a route is strip-mined and pivoting attention toward substitute sustenance nearby", "extends scarcity lookback", "drift can panic on temporary shortages", "awakening filters false famine signals", "convergence pools scarcity reads across a convoy", "memory stores which detours prevented starvation spirals",
                        metadata(Set.of("survival", "scarcity-reading", "resource-substitution"), Set.of("pickup-event", "supply-imbalance", "route-pivot"), Set.of("survival", "trader", "watchful"), 0.78D, 0.71D, 0.87D, 0.10D, 0.29D, 0.90D)),
                template("survival.last_light_cache", "Last Light Cache", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityMechanic.GUARDIAN_PULSE,
                        "Nightfall tightens safe-radius instincts around recently tended shelters", "widens dusk shelter memory", "drift can bond to weak camps", "awakening sharpens panic-to-shelter transitions", "convergence synchronizes party fallback beacons", "memory preserves proven refuge rings",
                        metadata(Set.of("survival", "shelter-defense"), Set.of("time-transition", "shelter-proximity"), Set.of("survival", "support", "builder"), 0.68D, 0.77D, 0.69D, 0.20D, 0.31D, 0.96D)),

                template("tactical.killzone_lattice", "Killzone Lattice", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.BRUTALITY, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.BATTLEFIELD_FIELD,
                        "Fortified spaces sketch short-lived pressure lanes for chokepoint control", "improves choke coverage", "drift can overcommit to bad holds", "awakening sharpens lane overlap", "convergence chains allied pressure zones", "memory ranks successful defense geometries",
                        metadata(Set.of("tactical-control", "chokepoint-field"), Set.of("structure-proximity", "hold-position"), Set.of("fighter", "pvp", "builder"), 0.70D, 0.65D, 0.74D, 0.12D, 0.41D, 0.58D)),
                template("combat.rupture_window", "Rupture Window", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.BRUTALITY, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.BURST_STATE,
                        "Extended skirmish chains accumulate forward-commitment pressure that briefly opens a burst window before opponent recovery closes", "widens burst window duration", "drift can fire the window at a disengaged opponent", "awakening holds the window until commitment is real", "convergence relays burst timing to nearby allies", "memory tracks which chain lengths always opened clean windows",
                        metadata(Set.of("tactical-control", "chain-burst", "forward-pressure"), Set.of("chain-combat", "burst-timing", "commitment-window"), Set.of("fighter", "pvp", "aggression"), 0.68D, 0.62D, 0.88D, 0.06D, 0.28D, 0.42D)),
                template("stealth.intercept_line", "Intercept Line", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.BRUTALITY, AbilityTrigger.ON_ELEVATION_CHANGE, AbilityMechanic.RETALIATION,
                        "Dropping to lower ground marks a bounded intercept corridor that commits to a forward cut-off rather than evasion", "extends intercept corridor range", "drift can overcommit to guessed entry lines", "awakening sharpens true intercept geometry from feinted drops", "convergence layers allied cut-off vectors", "memory stores which elevation drops opened real intercept angles",
                        metadata(Set.of("stealth", "intercept-routing", "forward-pressure"), Set.of("elevation-shift", "intercept-window", "cut-off-commit"), Set.of("stealth", "pvp", "aggression"), 0.72D, 0.88D, 0.82D, 0.08D, 0.22D, 0.38D)),
                template("survival.press_through", "Press Through", AbilityCategory.SURVIVAL_ADAPTATION, AbilityFamily.BRUTALITY, AbilityTrigger.ON_MOVEMENT, AbilityMechanic.BURST_STATE,
                        "Sustained movement under harsh attrition builds forward-pressure momentum that converts survival instinct into a short aggressive stride window", "extends momentum stride duration", "drift can burn momentum in low-stakes travel", "awakening reserves the stride for genuine attrition conditions", "convergence shares momentum pressure with nearby allies", "memory records which harsh routes forged the most reliable pressure",
                        metadata(Set.of("survival", "forward-pressure", "attrition-momentum"), Set.of("movement-cadence", "attrition-window", "momentum-build"), Set.of("survival", "fighter", "aggression"), 0.65D, 0.82D, 0.74D, 0.10D, 0.24D, 0.86D)),
                template("tactical.feint_window", "Feint Window", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.CHAOS, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityMechanic.CHAIN_ESCALATION,
                        "Intentional player interaction can seed a fake opening before conflict starts", "extends bluff viability", "drift can bluff into dead angles", "awakening separates bait from panic", "convergence layers coordinated feints", "memory tracks which opponents bite",
                        metadata(Set.of("tactical-control", "pre-fight-bluff"), Set.of("player-intent", "duel-prep"), Set.of("fighter", "stealth", "social"), 0.61D, 0.63D, 0.73D, 0.26D, 0.66D, 0.32D)),
                template("tactical.reposition_snare", "Reposition Snare", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_REPOSITION, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Side-steps and disengages leave a short-lived punish lane that only tightens if the same escape angle is reused", "widens punish-lane memory", "drift can trap harmless kiting", "awakening isolates genuinely repeated escapes", "convergence layers crossfire punish windows", "memory records which duels were won by reading the second step",
                        metadata(Set.of("tactical-control", "reposition-punish", "angle-denial"), Set.of("reposition", "escape-angle-repeat", "duel-read"), Set.of("fighter", "pvp", "watchful"), 0.68D, 0.72D, 0.83D, 0.10D, 0.34D, 0.41D)),
                template("tactical.tempo_extract", "Tempo Extract", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.PRECISION, AbilityTrigger.ON_CHAIN_COMBAT, AbilityMechanic.BATTLEFIELD_FIELD,
                        "Extended skirmish chains expose a tempo window where one more push or disengage is favored before the fight flips", "deepens tempo-window confidence", "drift can call the swing too early", "awakening separates real momentum from noise", "convergence shares timing windows across allies", "memory keeps the cadence of fights that turned on patience",
                        metadata(Set.of("tactical-control", "tempo-reading", "skirmish-rhythm"), Set.of("chain-combat", "tempo-window", "pressure-swing"), Set.of("fighter", "support", "pvp"), 0.73D, 0.69D, 0.90D, 0.08D, 0.47D, 0.38D)),
                template("tactical.rush_damper", "Rush Damper", AbilityCategory.COMBAT_TACTICAL_CONTROL, AbilityFamily.SURVIVAL, AbilityTrigger.ON_LOW_HEALTH, AbilityMechanic.GUARDIAN_PULSE,
                        "Dropping low under pursuit creates a brief anti-rush hesitation field if the defender keeps changing elevation or cover depth", "extends anti-rush hesitation", "drift can trigger while already safe", "awakening reserves the pulse for real collapses", "convergence staggers allied retreat windows", "memory remembers which escapes bought breathing room",
                        metadata(Set.of("tactical-control", "anti-rush", "retreat-shaping"), Set.of("low-health", "cover-shift", "elevation-variation"), Set.of("fighter", "survival", "pvp"), 0.64D, 0.74D, 0.79D, 0.12D, 0.29D, 0.52D)),

                template("warding.perimeter_hum", "Perimeter Hum", AbilityCategory.DEFENSE_WARDING, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Owned thresholds emit a low hum when perimeter rhythm is broken", "expands ward footprint", "drift can answer harmless movement", "awakening filters routine traffic", "convergence links layered base wards", "memory encodes trusted perimeter cadence",
                        metadata(Set.of("warding", "base-defense"), Set.of("structure-proximity", "perimeter-check"), Set.of("builder", "support", "pvp"), 0.74D, 0.45D, 0.84D, 0.32D, 0.41D, 0.90D)),
                template("warding.sanctum_lock", "Sanctum Lock", AbilityCategory.DEFENSE_WARDING, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_RITUAL_COMPLETION, AbilityMechanic.RITUAL_STABILIZATION,
                        "Completed maintenance rites harden workshop and storage stability for a short cycle", "extends sanctum uptime", "drift can lock low-value rooms", "awakening narrows to critical infrastructure", "convergence shares sanctum state across linked bases", "memory prioritizes reliable strongholds",
                        metadata(Set.of("warding", "infrastructure-stability"), Set.of("ritual-completion", "base-upkeep"), Set.of("builder", "support", "ritual"), 0.67D, 0.41D, 0.71D, 0.66D, 0.38D, 0.94D)),
                template("warding.fault_survey", "Fault Survey", AbilityCategory.DEFENSE_WARDING, AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.INSIGHT_REVEAL,
                        "Inspecting walls, supports, or machine housings reveals stress seams that would fail first under intrusion or neglect", "deepens seam-read fidelity", "drift can obsess over cosmetic scars", "awakening separates true fault lines from old repairs", "convergence links multiple inspections into one fortification plan", "memory stores the weak spots that nearly cost the base",
                        metadata(Set.of("warding", "fault-detection", "infrastructure-reading"), Set.of("intentional-interact", "support-stress-read", "maintenance-check"), Set.of("builder", "information", "support"), 0.79D, 0.36D, 0.93D, 0.22D, 0.27D, 0.95D)),
                template("warding.anchor_cadence", "Anchor Cadence", AbilityCategory.DEFENSE_WARDING, AbilityFamily.MOBILITY, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.COLLECTIVE_RELAY,
                        "Repeated patrol or upkeep loops teach a base its anchor cadence, revealing where relief coverage is late or overstacked", "extends cadence lookback", "drift can canonize sloppy patrols", "awakening filters busywork from real coverage", "convergence synchronizes linked sentry circuits", "memory records which patrol rhythms actually deterred breaches",
                        metadata(Set.of("warding", "patrol-rhythm", "coverage-mapping"), Set.of("group-action", "patrol-loop", "coverage-check"), Set.of("builder", "support", "pvp"), 0.72D, 0.58D, 0.82D, 0.18D, 0.71D, 0.88D)),
                template("warding.false_threshold", "False Threshold", AbilityCategory.DEFENSE_WARDING, AbilityFamily.CHAOS, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.MARK,
                        "Repeated edge-testing by outsiders can seed a decoy safe entry that wastes the intruder's next committed approach", "extends decoy-threshold persistence", "drift can mislabel friendly traffic as probing", "awakening reserves false welcomes for hostile patterns", "convergence braids multiple decoy entries around a fort", "memory remembers which fake openings actually diverted raiders",
                        metadata(Set.of("warding", "threshold-deception", "anti-infiltration"), Set.of("structure-proximity", "edge-test-repeat", "intrusion-read"), Set.of("builder", "chaos", "pvp"), 0.70D, 0.44D, 0.87D, 0.24D, 0.32D, 0.89D)),

                template("logistics.convoy_instinct", "Convoy Instinct", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.COLLECTIVE_RELAY,
                        "Hauling as a group reveals the least-friction delivery leg for the convoy", "extends convoy relay distance", "drift can route through risky shortcuts", "awakening filters ambush-prone links", "convergence synchronizes multi-run supply timing", "memory records dependable haul circuits",
                        metadata(Set.of("logistics", "supply-routing"), Set.of("group-action", "haul-loop"), Set.of("trader", "support", "automation"), 0.76D, 0.71D, 0.73D, 0.08D, 0.86D, 0.92D)),
                template("logistics.stockpile_tide", "Stockpile Tide", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.SURVIVAL, AbilityTrigger.ON_ITEM_PICKUP, AbilityMechanic.HARVEST_RELAY,
                        "Pickup streaks expose whether nearby stores are draining or saturating", "improves refill timing", "drift can overread temporary spikes", "awakening distinguishes throughput from clutter", "convergence shares stock warnings to allies", "memory tracks stable refill thresholds",
                        metadata(Set.of("logistics", "throughput-monitoring"), Set.of("pickup-event", "stock-flow"), Set.of("farmer", "trader", "support"), 0.71D, 0.55D, 0.82D, 0.06D, 0.54D, 0.96D)),
                template("logistics.queue_sight", "Queue Sight", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.INSIGHT_REVEAL,
                        "Trade cadence reveals whether a supply line is bottlenecked at intake, transit, or handoff", "deepens queue-stage confidence", "drift can blame the wrong chokepoint", "awakening separates real backlog from momentary demand", "convergence compares multiple sellers into one throughput picture", "memory stores which markets jammed before prices moved",
                        metadata(Set.of("logistics", "queue-reading", "economy-flow"), Set.of("trade-event", "cadence-gap", "handoff-read"), Set.of("trader", "information", "support"), 0.83D, 0.49D, 0.95D, 0.08D, 0.77D, 0.78D)),
                template("logistics.relay_mesh", "Relay Mesh", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.MOBILITY, AbilityTrigger.ON_MOVEMENT, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Repeated hauling between storage nodes stitches a relay mesh that surfaces the least exposed handoff sequence", "adds one more viable relay hop", "drift can overvalue familiar detours", "awakening trims mesh branches that bleed time", "convergence lets crews split cargo without breaking the route", "memory records which handoff chains survived pressure",
                        metadata(Set.of("logistics", "handoff-routing", "supply-mesh"), Set.of("movement-cadence", "storage-link", "haul-repeat"), Set.of("trader", "mobility", "automation"), 0.79D, 0.76D, 0.81D, 0.06D, 0.63D, 0.92D)),
                template("logistics.spoilage_audit", "Spoilage Audit", AbilityCategory.RESOURCE_FARMING_LOGISTICS, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.MEMORY_ECHO,
                        "Inspecting farms, chests, or processors reveals where delay or overstock is silently turning output into waste", "improves waste-trail fidelity", "drift can moralize harmless stockpiles", "awakening distinguishes reserve stock from real decay", "convergence links audits across a workshop chain", "memory keeps the loss patterns worth fixing first",
                        metadata(Set.of("logistics", "waste-detection", "process-audit"), Set.of("intentional-interact", "storage-audit", "farm-check"), Set.of("farmer", "builder", "information"), 0.77D, 0.43D, 0.94D, 0.10D, 0.29D, 0.97D)),

                template("engineering.redstone_sympathy", "Redstone Sympathy", AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.STRUCTURE_ATTUNEMENT,
                        "Workshop inspection highlights likely machine intent and missing links", "improves machine-state confidence", "drift can admire decorative noise", "awakening separates logic from ornament", "convergence shares workshop diagnostics", "memory catalogs stable machine motifs",
                        metadata(Set.of("engineering", "machine-diagnostics"), Set.of("intentional-interact", "workshop-inspect"), Set.of("builder", "automation", "information"), 0.79D, 0.49D, 0.94D, 0.14D, 0.27D, 0.86D)),
                template("engineering.pattern_forge", "Pattern Forge", AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.PATTERN_RESONANCE,
                        "Repeated construction motifs condense into a bounded fabrication cadence bonus", "extends build cadence chains", "drift can reinforce inefficient loops", "awakening trims wasteful pattern echoes", "convergence shares template cadence with nearby builders", "memory favors repeatable workshop routines",
                        metadata(Set.of("engineering", "fabrication-cadence"), Set.of("pattern-repeat", "build-loop"), Set.of("builder", "automation", "support"), 0.72D, 0.58D, 0.75D, 0.26D, 0.46D, 0.93D)),
                template("engineering.sequence_splice", "Sequence Splice", AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, AbilityFamily.MOBILITY, AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.CHAIN_ESCALATION,
                        "Alternating build motifs expose the next safe step in a longer assembly sequence instead of only rewarding repetition", "extends splice depth", "drift can chain into decorative dead ends", "awakening isolates productive sequence turns", "convergence braids multiple builders into one assembly rhythm", "memory stores which build orders prevented rework",
                        metadata(Set.of("engineering", "sequence-chaining", "assembly-order"), Set.of("pattern-repeat", "sequence-turn", "build-variation"), Set.of("builder", "automation", "exploration"), 0.76D, 0.61D, 0.84D, 0.20D, 0.43D, 0.91D)),
                template("engineering.machine_rhythm", "Machine Rhythm", AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, AbilityFamily.CHAOS, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.TEMPORAL_SPECIALIZATION,
                        "Remembered workshop failures and successes settle into a machine rhythm that hints when to pulse, pause, or batch a finicky process", "deepens rhythm confidence", "drift can canonize lucky timing", "awakening separates true cycle rhythm from superstition", "convergence aligns shared workshop timing across artifacts", "memory keeps the process beats worth repeating",
                        metadata(Set.of("engineering", "machine-rhythm", "process-timing"), Set.of("memory-driven", "workshop-cycle", "batch-timing"), Set.of("builder", "automation", "memory"), 0.73D, 0.47D, 0.90D, 0.34D, 0.35D, 0.88D)),
                template("engineering.fault_isolate", "Fault Isolate", AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.STRUCTURE_ATTUNEMENT,
                        "Inspecting a stalled machine or half-built contraption narrows which segment is actually failing and what can be bypassed temporarily", "improves fault isolation confidence", "drift can blame downstream symptoms", "awakening separates root causes from cascading noise", "convergence shares bypass heuristics between linked workstations", "memory records the fixes that kept production alive",
                        metadata(Set.of("engineering", "fault-detection", "automation-recovery"), Set.of("intentional-interact", "machine-stall", "bypass-read"), Set.of("builder", "support", "automation"), 0.81D, 0.42D, 0.96D, 0.18D, 0.24D, 0.92D)),

                template("support.rally_ledger", "Rally Ledger", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Coordinated actions annotate who is anchoring the group's tempo and who is lagging behind", "improves group tempo clarity", "drift can overvalue loud participants", "awakening favors reliable anchors", "convergence aligns multiple subgroups", "memory stores effective team rhythms",
                        metadata(Set.of("support", "coordination"), Set.of("group-action", "party-tempo"), Set.of("support", "social", "rpg"), 0.70D, 0.63D, 0.85D, 0.18D, 0.97D, 0.62D)),
                template("support.mercy_link", "Mercy Link", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.SURVIVAL, AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.RECOVERY_WINDOW,
                        "Watching allies under pressure opens a short shared recovery timing window", "extends clutch-support timing", "drift can trigger for low-stakes scrapes", "awakening reserves windows for real danger", "convergence spreads aid cadence through the squad", "memory remembers who reciprocates",
                        metadata(Set.of("support", "clutch-recovery"), Set.of("player-witness", "pressure-read"), Set.of("support", "fighter", "social"), 0.65D, 0.56D, 0.74D, 0.16D, 0.95D, 0.79D)),
                template("support.role_call", "Role Call", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.PRECISION, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Brief social check-ins reveal who is carrying the wrong role load before a plan starts", "deepens role-read accuracy", "drift can overtrust confident talkers", "awakening favors proven responsibility over volume", "convergence aligns role swaps across subgroups", "memory stores which teams redistributed labor in time",
                        metadata(Set.of("support", "role-reading", "coordination-prep"), Set.of("player-intent", "plan-setup", "role-check"), Set.of("support", "social", "builder"), 0.74D, 0.52D, 0.89D, 0.16D, 0.98D, 0.61D)),
                template("support.convoy_accord", "Convoy Accord", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.MOBILITY, AbilityTrigger.ON_MOVEMENT, AbilityMechanic.COLLECTIVE_RELAY,
                        "Moving as a loose group creates an accord that keeps stragglers, scouts, and carriers from drifting out of useful formation", "extends accord coherence", "drift can over-clump flexible groups", "awakening keeps spacing adaptive instead of rigid", "convergence harmonizes multiple convoys on intersecting routes", "memory records which formations survived long hauls",
                        metadata(Set.of("support", "formation-control", "convoy-cohesion"), Set.of("movement-cadence", "group-spacing", "route-commitment"), Set.of("support", "mobility", "trader"), 0.69D, 0.71D, 0.78D, 0.10D, 0.97D, 0.83D)),
                template("support.cover_exchange", "Cover Exchange", AbilityCategory.SOCIAL_SUPPORT_COORDINATION, AbilityFamily.CHAOS, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.TRADE_SCENT,
                        "Seemingly routine item handoffs can quietly exchange responsibilities, safe words, or fallback plans without exposing the real coordinator", "extends cover-story durability", "drift can swap roles at the wrong moment", "awakening reserves the exchange for meaningful pressure", "convergence lets allied crews rotate responsibility without panic", "memory keeps the handoff patterns that protected the team",
                        metadata(Set.of("support", "responsibility-handoff", "social-camouflage"), Set.of("trade-event", "cover-story", "fallback-swap"), Set.of("support", "stealth", "social"), 0.77D, 0.48D, 0.87D, 0.18D, 0.94D, 0.69D)),

                template("ritual.oath_circuit", "Oath Circuit", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.ALTAR_SIGNAL_BOOST,
                        "Synchronized group gestures turn social commitment into bounded ritual throughput", "extends oath-chain length", "drift can bind empty gestures", "awakening filters unserious participants", "convergence braids multiple oath circles", "memory preserves successful communal rites",
                        metadata(Set.of("ritual", "social-rite"), Set.of("group-action", "ritual-coordination"), Set.of("ritual", "social", "support"), 0.73D, 0.52D, 0.68D, 0.98D, 0.82D, 0.55D)),
                template("ritual.moon_debt", "Moon Debt", AbilityCategory.RITUAL_STRANGE_UTILITY, AbilityFamily.CHAOS, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityMechanic.MEMORY_ECHO,
                        "Night transitions surface what the land still 'owes' from unfinished rites or expeditions", "deepens unpaid-ritual read", "drift can blame the wrong frontier", "awakening narrows debts to actionable omens", "convergence shares omen burdens among companions", "memory records places that call players back",
                        metadata(Set.of("ritual", "omen-memory"), Set.of("time-transition", "ritual-pressure"), Set.of("ritual", "exploration", "hardcore"), 0.84D, 0.69D, 0.77D, 0.97D, 0.41D, 0.49D)),

                template("stealth.hushwire", "Hushwire", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.CHAOS, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.MOVEMENT_ECHO,
                        "Approaching defended spaces lays a silent ingress line that suppresses obvious movement tells", "extends hush line reach", "drift can mute allies at bad times", "awakening narrows suppression to infiltration windows", "convergence chains multiple ingress routes", "memory stores the quietest breaches",
                        metadata(Set.of("stealth", "infiltration-routing"), Set.of("structure-proximity", "silent-approach"), Set.of("stealth", "pvp", "exploration"), 0.69D, 0.86D, 0.74D, 0.24D, 0.38D, 0.52D)),
                template("stealth.paper_trail", "Paper Trail", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.TRADE_SCENT,
                        "Market interactions can hide or misdirect the scent of recent movement and storage", "improves false-ledger durability", "drift can hide the wrong route", "awakening sharpens smuggler-safe traces", "convergence coordinates distributed cover stories", "memory tracks believable trade alibis",
                        metadata(Set.of("stealth", "economic-disruption"), Set.of("trade-event", "cover-story"), Set.of("stealth", "trader", "social"), 0.77D, 0.58D, 0.88D, 0.18D, 0.83D, 0.60D)),
                template("stealth.shadow_proxy", "Shadow Proxy", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.CHAOS, AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.MARK,
                        "Being seen lets the artifact plant a false last-known position that only matures if line of sight is broken and the route forks", "extends false-position persistence", "drift can seed obvious decoys", "awakening delays the proxy until a believable split appears", "convergence braids multiple false exits", "memory catalogs which pursuers trust the first clue",
                        metadata(Set.of("stealth", "misdirection", "pursuit-disruption"), Set.of("player-witness", "line-of-sight-break", "route-fork"), Set.of("stealth", "pvp", "chaos"), 0.83D, 0.89D, 0.92D, 0.22D, 0.44D, 0.36D)),
                template("stealth.threshold_jam", "Threshold Jam", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.DEFENSIVE_THRESHOLD,
                        "Hostile thresholds accumulate interference as an infiltrator skirts their edge, briefly blinding alarms only if the approach never repeats the same angle", "widens jammed threshold arc", "drift can teach a detectable pattern", "awakening rotates safe approach windows", "convergence staggers allied breach timings", "memory remembers which entries became too hot",
                        metadata(Set.of("stealth", "infiltration-disruption", "threshold-play"), Set.of("structure-proximity", "angle-variation", "alarm-interference"), Set.of("stealth", "builder", "pvp"), 0.80D, 0.84D, 0.91D, 0.14D, 0.29D, 0.49D)),
                template("stealth.dead_drop_lattice", "Dead Drop Lattice", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.NAVIGATION_ANCHOR,
                        "A suspicious trade can seed a hidden handoff route, but only if the carrier threads multiple low-traffic waypoints before doubling back", "adds one more viable handoff hop", "drift can overvalue exposed shortcuts", "awakening filters routes already watched by rivals", "convergence lets crews split and rejoin the lattice", "memory stores which drop chains stayed uncompromised",
                        metadata(Set.of("stealth", "contraband-routing", "navigation"), Set.of("trade-event", "waypoint-chain", "low-traffic-check"), Set.of("stealth", "trader", "exploration"), 0.86D, 0.93D, 0.89D, 0.16D, 0.71D, 0.55D)),
                template("stealth.echo_shunt", "Echo Shunt", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.MOBILITY, AbilityTrigger.ON_MOVEMENT, AbilityMechanic.MOVEMENT_ECHO,
                        "Alternating sprint and crouch cadence can split movement noise into one advancing trail and one stalling phantom branch", "extends phantom branch lifetime", "drift can split the real escape path", "awakening sharpens cadence windows for clean shunts", "convergence layers multiple phantom routes across a breach", "memory records when noise was worth spending",
                        metadata(Set.of("stealth", "movement-misdirection", "escape-routing"), Set.of("movement-cadence", "noise-splitting", "pursuit-break"), Set.of("stealth", "mobility", "pvp"), 0.78D, 0.95D, 0.84D, 0.12D, 0.24D, 0.42D)),
                template("stealth.ghost_shift", "Ghost Shift", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.MOBILITY, AbilityTrigger.ON_REPOSITION, AbilityMechanic.MARK,
                        "Hard lateral repositioning can overwrite a tracker's last assumed angle, but only if the move crosses a believable obstacle edge", "extends overwrite persistence", "drift can spend the shift on obvious jukes", "awakening waits for convincing obstacle breaks", "convergence layers multiple cross-angles into one escape story", "memory records which sidesteps actually shed pursuit",
                        metadata(Set.of("stealth", "anti-tracking", "angle-overwrite"), Set.of("reposition", "obstacle-edge", "tracker-reset"), Set.of("stealth", "mobility", "pvp"), 0.80D, 0.92D, 0.90D, 0.10D, 0.23D, 0.40D)),
                template("stealth.social_smoke", "Social Smoke", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.CHAOS, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Casual chatter before or after suspicious movement can blur who was coordinating with whom, creating a short social camouflage window", "extends camouflage duration", "drift can shroud the wrong accomplice", "awakening filters idle banter from cover behavior", "convergence braids multiple alibis into one crowd texture", "memory stores which rooms believed the act",
                        metadata(Set.of("stealth", "social-camouflage", "alibi-play"), Set.of("player-intent", "cover-story", "co-presence-blur"), Set.of("stealth", "social", "chaos"), 0.76D, 0.54D, 0.88D, 0.21D, 0.91D, 0.51D)),
                template("stealth.trace_fold", "Trace Fold", AbilityCategory.STEALTH_TRICKERY_DISRUPTION, AbilityFamily.CONSISTENCY, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.MEMORY_ECHO,
                        "Reading a surface before passing through it can fold your freshest trace into older disturbance, making pursuit forensics lose the correct timestamp", "deepens trace-fold confidence", "drift can bury the route in unrelated clutter", "awakening separates usable cover from noisy history", "convergence lets crews stagger their traces into one older story", "memory records which materials hid passage best",
                        metadata(Set.of("stealth", "forensic-evasion", "trace-folding"), Set.of("intentional-interact", "surface-read", "timestamp-blur"), Set.of("stealth", "builder", "trader"), 0.82D, 0.67D, 0.94D, 0.14D, 0.32D, 0.59D))
        );
    }

    private AbilityTemplate template(String id, String name, AbilityCategory category, AbilityFamily family, AbilityTrigger trigger, AbilityMechanic mechanic,
                                     String effectPattern, String evolutionVariant, String driftVariant, String awakeningVariant, String convergenceVariant,
                                     String memoryVariant, AbilityMetadata metadata) {
        AbilityMetadata enriched = metadata.triggerBudgetProfile() == null ? withBudgetDefaults(trigger, metadata) : metadata;
        return new AbilityTemplate(id, name, category, family, trigger, mechanic, effectPattern, evolutionVariant, driftVariant, awakeningVariant, convergenceVariant,
                memoryVariant, List.of(new AbilityModifier("support.signature", "non-combat tuning hook", 0.04, false)), enriched);
    }

    private AbilityMetadata metadata(Set<String> domains, Set<String> triggers, Set<String> affinities,
                                     double discovery, double exploration, double information, double ritual, double social, double world) {
        return AbilityMetadata.of(domains, triggers, affinities, discovery, exploration, information, ritual, social, world);
    }


    private AbilityMetadata withBudgetDefaults(AbilityTrigger trigger, AbilityMetadata metadata) {
        TriggerBudgetProfile profile = switch (trigger) {
            case ON_WORLD_SCAN, ON_CHUNK_ENTER, ON_BIOME_CHANGE, ON_TIME_OF_DAY_TRANSITION, ON_WEATHER_CHANGE, ON_ELEVATION_CHANGE -> new TriggerBudgetProfile(1.4D, 0.9D, 10.0D, 3.0D, 3, 1200L, 35, TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY, false, 280.0D);
            case ON_STRUCTURE_SENSE, ON_STRUCTURE_DISCOVERY, ON_STRUCTURE_PROXIMITY -> new TriggerBudgetProfile(2.3D, 1.2D, 8.0D, 2.3D, 2, 2200L, 28, TriggerBudgetPolicy.STRICT, false, 500.0D);
            case ON_BLOCK_INSPECT, ON_ENTITY_INSPECT, ON_BLOCK_HARVEST, ON_RITUAL_INTERACT, ON_SOCIAL_INTERACT,
                    ON_RESOURCE_HARVEST_STREAK, ON_ITEM_PICKUP, ON_REPEATED_BLOCK_PATTERN, ON_RITUAL_COMPLETION,
                    ON_PLAYER_GROUP_ACTION, ON_PLAYER_TRADE -> new TriggerBudgetProfile(0.7D, 0.2D, 14.0D, 6.4D, 6, 750L, 88, TriggerBudgetPolicy.ACTIVE_INTENTIONAL, true, 75.0D);
            case ON_MEMORY_EVENT, ON_WITNESS_EVENT, ON_PLAYER_WITNESS -> new TriggerBudgetProfile(1.0D, 0.5D, 11.0D, 3.8D, 3, 1300L, 46, TriggerBudgetPolicy.SOFT, false, 180.0D);
            default -> TriggerBudgetProfile.defaults();
        };
        return AbilityMetadata.of(metadata.utilityDomains(), metadata.triggerClasses(), metadata.affinities(), metadata.discoveryValue(),
                metadata.explorationValue(), metadata.informationValue(), metadata.ritualValue(), metadata.socialValue(), metadata.worldUtilityValue(), profile);
    }

    public List<AbilityTemplate> templates() { return templates; }

    public List<AbilityTemplate> byFamily(AbilityFamily family) {
        List<AbilityTemplate> out = new ArrayList<>();
        for (AbilityTemplate template : templates) {
            if (template.family() == family) {
                out.add(template);
            }
        }
        return out;
    }

    public List<AbilityTemplate> byCategory(AbilityCategory category) {
        List<AbilityTemplate> out = new ArrayList<>();
        for (AbilityTemplate template : templates) {
            if (template.category() == category) {
                out.add(template);
            }
        }
        return out;
    }
}
