package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AbilityRegistry {
    private final List<AbilityTemplate> templates;

    public AbilityRegistry() {
        this.templates = List.of(
                template("precision.echo_locator", "Echo Locator", AbilityFamily.PRECISION, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.SENSE_PING,
                        "Resonant pulses reveal nearby hollows when crossing new terrain", "deeper echo strata", "drift introduces phantom echoes", "awakening stabilizes pulse geometry", "fusion shares cave cues", "memory records hollow corridors",
                        metadata(Set.of("environmental-sensing", "information"), Set.of("movement-throttled"), Set.of("watchful", "exploration", "memory"), 0.72D, 0.81D, 0.78D, 0.22D, 0.20D, 0.41D)),
                template("precision.vein_whisper", "Vein Whisper", AbilityFamily.PRECISION, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.INSIGHT_REVEAL,
                        "Interprets chunk composition into soft ore-likelihood hints", "better material confidence", "drift shifts probabilities", "awakening filters false positives", "fusion triangulates material drift", "memory stores productive seams",
                        metadata(Set.of("informational-interpretation", "environmental-sensing"), Set.of("chunk-change"), Set.of("watchful", "exploration"), 0.68D, 0.76D, 0.84D, 0.15D, 0.18D, 0.46D)),
                template("precision.material_insight", "Material Insight", AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.INSIGHT_REVEAL,
                        "Inspected blocks whisper practical context and odd utility lore", "broader material lexicon", "drift adds cryptic readings", "awakening clarifies odd traits", "fusion links shared interpretations", "memory curates known materials",
                        metadata(Set.of("information", "world-interpretation"), Set.of("intentional-interact"), Set.of("watchful", "curious"), 0.61D, 0.44D, 0.90D, 0.25D, 0.24D, 0.51D)),

                template("mobility.footprint_memory", "Footprint Memory", AbilityFamily.MOBILITY, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Sneak-use marks a location and later points back to it", "longer recall range", "drift can misalign memory", "awakening anchors dimension-safe recall", "fusion broadcasts anchor to allies", "memory persists trusted waypoints",
                        metadata(Set.of("navigation", "memory-history"), Set.of("gesture-based"), Set.of("memory", "exploration", "worldkeeper"), 0.79D, 0.88D, 0.52D, 0.35D, 0.22D, 0.55D)),
                template("mobility.compass_stories", "Compass of Stories", AbilityFamily.MOBILITY, AbilityTrigger.ON_MEMORY_EVENT, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Points toward recorded notable events and lineage residues", "locks stronger event traces", "drift surfaces contradictory trails", "awakening resolves ancestral signatures", "fusion blends shared story maps", "memory prioritizes meaningful landmarks",
                        metadata(Set.of("navigation", "memory-history", "structure-awareness"), Set.of("memory-driven"), Set.of("memory", "lineage", "ritual"), 0.86D, 0.82D, 0.63D, 0.48D, 0.26D, 0.39D)),
                template("mobility.quiet_passage", "Quiet Passage", AbilityFamily.MOBILITY, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Doors, gates, and trapdoors respond with muted passage behavior", "longer hush duration", "drift can over-silence", "awakening extends to nearby hinges", "fusion synchronizes passage fields", "memory recognizes familiar thresholds",
                        metadata(Set.of("world-interaction", "social-flavor"), Set.of("block-interact"), Set.of("worldkeeper", "support"), 0.42D, 0.54D, 0.36D, 0.40D, 0.78D, 0.67D)),

                template("survival.gentle_harvest", "Gentle Harvest", AbilityFamily.SURVIVAL, AbilityTrigger.ON_BLOCK_HARVEST, AbilityMechanic.HARVEST_RELAY,
                        "Mature crops are replanted with respectful timing", "more crop families supported", "drift occasionally skips replant", "awakening stabilizes replant certainty", "fusion helps nearby harvested rows", "memory tracks fertile habits",
                        metadata(Set.of("farming-building", "world-interaction"), Set.of("block-break-filtered"), Set.of("worldkeeper", "support"), 0.57D, 0.38D, 0.34D, 0.20D, 0.59D, 0.91D)),
                template("survival.ember_keeper", "Ember Keeper", AbilityFamily.SURVIVAL, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.RITUAL_CHANNEL,
                        "Campfires retain gentle utility states when tended by hand", "longer ember memory", "drift causes flicker moods", "awakening steadies flame intent", "fusion shares ember calm", "memory ties warmth to safe sites",
                        metadata(Set.of("ritual-utility", "world-interaction"), Set.of("campfire-interact"), Set.of("reverent", "support", "ritual"), 0.46D, 0.41D, 0.31D, 0.88D, 0.52D, 0.74D)),

                template("chaos.dust_memory", "Dust Memory", AbilityFamily.CHAOS, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityMechanic.MEMORY_ECHO,
                        "Ancient spaces stir residue hints and subtle direction", "more residue vocabularies", "drift intensifies strange omens", "awakening clarifies true residue", "fusion harmonizes residue signatures", "memory binds old places to lineage",
                        metadata(Set.of("memory-history", "structure-awareness", "ritual-utility"), Set.of("chunk-structure-entry"), Set.of("ritual", "memory", "exploration"), 0.83D, 0.74D, 0.67D, 0.79D, 0.33D, 0.44D)),
                template("chaos.witness", "Witness", AbilityFamily.CHAOS, AbilityTrigger.ON_WITNESS_EVENT, AbilityMechanic.REVENANT_TRIGGER,
                        "Observed moments imprint into future reactive echoes", "broader witness lexicon", "drift echoes false witness residue", "awakening filters witness truth", "fusion cross-links observer traces", "memory chains witness motifs",
                        metadata(Set.of("memory-history", "social-flavor", "ritual-utility"), Set.of("witness-reactive"), Set.of("memory", "ritual", "watchful"), 0.73D, 0.64D, 0.58D, 0.84D, 0.69D, 0.35D)),

                template("consistency.path_thread", "Path Thread", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.NAVIGATION_ANCHOR,
                        "Moving through unfamiliar terrain leaves subtle return-thread trails", "longer route memory", "drift can braid false trails", "awakening steadies reliable routes", "fusion interleaves nearby trail maps", "memory stores trusted route threads",
                        metadata(Set.of("navigation", "memory-history"), Set.of("movement-throttled"), Set.of("exploration", "memory"), 0.75D, 0.90D, 0.62D, 0.28D, 0.30D, 0.55D)),
                template("survival.weather_omen", "Weather Omen", AbilityFamily.SURVIVAL, AbilityTrigger.ON_WORLD_SCAN, AbilityMechanic.INSIGHT_REVEAL,
                        "Sky pressure cues forecast short-term weather turns", "longer forecast horizon", "drift adds dramatic false omens", "awakening filters chaotic fronts", "fusion syncs omens across artifacts", "memory records recurring climate loops",
                        metadata(Set.of("environmental-sensing", "information"), Set.of("weather-poll"), Set.of("watchful", "support"), 0.66D, 0.58D, 0.84D, 0.31D, 0.42D, 0.63D)),
                template("precision.artifact_sympathy", "Artifact Sympathy", AbilityFamily.PRECISION, AbilityTrigger.ON_SOCIAL_INTERACT, AbilityMechanic.SOCIAL_ATTUNEMENT,
                        "Nearby artifacts reveal affinity hints during intentional greetings", "deeper affinity hints", "drift yields contradictory impressions", "awakening clarifies emotional residue", "fusion links sympathy contexts", "memory retains known signatures",
                        metadata(Set.of("social-flavor", "information"), Set.of("player-intent"), Set.of("support", "memory", "watchful"), 0.71D, 0.52D, 0.77D, 0.24D, 0.91D, 0.35D)),
                template("chaos.ritual_echo", "Ritual Echo", AbilityFamily.CHAOS, AbilityTrigger.ON_RITUAL_INTERACT, AbilityMechanic.RITUAL_CHANNEL,
                        "Completed rituals leave resonant echoes for follow-up interactions", "longer echo chains", "drift creates misleading echoes", "awakening separates true ritual signatures", "fusion braids compatible echoes", "memory archives ritual cadence",
                        metadata(Set.of("ritual-utility", "memory-history"), Set.of("ritual-completion"), Set.of("ritual", "memory"), 0.80D, 0.60D, 0.65D, 0.94D, 0.36D, 0.40D)),
                template("consistency.structure_echo", "Structure Echo", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_SENSE, AbilityMechanic.SENSE_PING,
                        "Ruins and dungeons project directional echo gradients", "higher structure certainty", "drift can offset bearings", "awakening locks ancient signatures", "fusion triangulates echoes", "memory keeps stable structure routes",
                        metadata(Set.of("structure-awareness", "navigation"), Set.of("chunk-structure-entry"), Set.of("exploration", "watchful"), 0.78D, 0.88D, 0.71D, 0.40D, 0.29D, 0.52D)),
                template("survival.herd_instinct", "Herd Instinct", AbilityFamily.SURVIVAL, AbilityTrigger.ON_ENTITY_INSPECT, AbilityMechanic.INSIGHT_REVEAL,
                        "Animal groups reveal migration and safety tendencies", "deeper migration context", "drift overstates danger", "awakening improves behavior confidence", "fusion blends herd observations", "memory stores seasonal movement",
                        metadata(Set.of("environmental-sensing", "social-world-behavior"), Set.of("entity-interact"), Set.of("support", "watchful", "worldkeeper"), 0.63D, 0.74D, 0.82D, 0.20D, 0.58D, 0.70D)),

                template("exploration.pathfinder_instinct", "Pathfinder Instinct", AbilityFamily.MOBILITY, AbilityTrigger.ON_CHUNK_ENTER, AbilityMechanic.ECOLOGICAL_PATHING,
                        "Entering unvisited chunks widens short-lived structure sensing radius", "tracks deeper unknown corridors", "drift can overfit stale routes", "awakening stabilizes frontier picks", "fusion shares frontier estimates", "memory logs exploration opportunities",
                        metadata(Set.of("exploration", "structure-discovery"), Set.of("coalesced-movement"), Set.of("exploration", "environmental", "memory"), 0.94D, 0.96D, 0.70D, 0.10D, 0.12D, 0.68D)),
                template("exploration.biome_attunement", "Biome Attunement", AbilityFamily.PRECISION, AbilityTrigger.ON_BIOME_CHANGE, AbilityMechanic.BIOME_RESONANCE,
                        "Biome transitions surface nearby biome-native resource hints", "extends native material memory", "drift can bias wrong biome assumptions", "awakening separates biome signatures", "fusion aligns neighboring biome reads", "memory stores biome yield quality",
                        metadata(Set.of("exploration", "resource-sensing"), Set.of("biome-transition"), Set.of("exploration", "environmental", "watchful"), 0.88D, 0.93D, 0.86D, 0.08D, 0.15D, 0.72D)),
                template("exploration.cartographer_echo", "Cartographer Echo", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_DISCOVERY, AbilityMechanic.CARTOGRAPHIC_ECHO,
                        "Structure discovery briefly reveals neighboring unexplored structure hints", "chains map breadcrumbs", "drift can add phantom lead-ins", "awakening improves chain confidence", "fusion links nearby cartographer signals", "memory stores structure chain routes",
                        metadata(Set.of("exploration", "map-intelligence"), Set.of("structure-event"), Set.of("exploration", "memory", "watchful"), 0.95D, 0.92D, 0.81D, 0.12D, 0.10D, 0.66D)),

                template("gathering.forager_memory", "Forager Memory", AbilityFamily.SURVIVAL, AbilityTrigger.ON_RESOURCE_HARVEST_STREAK, AbilityMechanic.FORAGING_MEMORY,
                        "Harvest streaks increase nearby matching-resource detection chance", "extends streak retention", "drift can lock onto low-value clusters", "awakening improves streak fidelity", "fusion shares active streak motifs", "memory tracks harvest chain density",
                        metadata(Set.of("gathering", "resource-detection"), Set.of("harvest-streak"), Set.of("gathering", "support", "memory"), 0.86D, 0.71D, 0.74D, 0.06D, 0.11D, 0.94D)),
                template("gathering.ecological_sense", "Ecological Sense", AbilityFamily.PRECISION, AbilityTrigger.ON_BLOCK_INSPECT, AbilityMechanic.RESOURCE_ECOLOGY_SCAN,
                        "Block inspection reveals localized biome resource balance indicators", "improves balance confidence", "drift can overstate scarcity", "awakening corrects ecosystem baselines", "fusion compares nearby ecological reads", "memory archives regional diversity profiles",
                        metadata(Set.of("gathering", "ecosystem-awareness"), Set.of("intentional-interact"), Set.of("gathering", "environmental", "watchful"), 0.80D, 0.69D, 0.93D, 0.12D, 0.08D, 0.89D)),
                template("gathering.gatherers_intuition", "Gatherer's Intuition", AbilityFamily.SURVIVAL, AbilityTrigger.ON_ITEM_PICKUP, AbilityMechanic.CLUSTER_INTUITION,
                        "Item pickups occasionally mark nearby resource clusters", "raises cluster confidence", "drift can select stale clusters", "awakening filters exhausted nodes", "fusion relays active cluster hints", "memory keeps productive pocket traces",
                        metadata(Set.of("gathering", "cluster-detection"), Set.of("pickup-event"), Set.of("gathering", "exploration", "support"), 0.82D, 0.74D, 0.78D, 0.05D, 0.16D, 0.91D)),

                template("ritual.ritual_attunement", "Ritual Attunement", AbilityFamily.CHAOS, AbilityTrigger.ON_REPEATED_BLOCK_PATTERN, AbilityMechanic.RITUAL_STABILIZATION,
                        "Repeated block patterns produce brief localized ritual stability", "extends stability windows", "drift can stabilize irrelevant patterns", "awakening isolates meaningful ritual motifs", "fusion synchronizes nearby pattern buffers", "memory records high-signal ritual cadence",
                        metadata(Set.of("ritual", "pattern-recognition"), Set.of("pattern-repeat"), Set.of("ritual", "memory", "support"), 0.78D, 0.55D, 0.76D, 0.97D, 0.23D, 0.52D)),
                template("ritual.altar_resonance", "Altar Resonance", AbilityFamily.CHAOS, AbilityTrigger.ON_RITUAL_COMPLETION, AbilityMechanic.ALTAR_SIGNAL_BOOST,
                        "Completed rituals temporarily amplify artifact evolution signal weight", "improves resonance carry-over", "drift can over-amplify weak rituals", "awakening controls resonance bounds", "fusion links compatible altar events", "memory anchors ritual activation context",
                        metadata(Set.of("ritual", "lineage-signaling"), Set.of("ritual-completion"), Set.of("ritual", "lineage", "support"), 0.83D, 0.51D, 0.71D, 0.99D, 0.34D, 0.49D)),
                template("ritual.temporal_attunement", "Temporal Attunement", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_TIME_OF_DAY_TRANSITION, AbilityMechanic.TEMPORAL_SPECIALIZATION,
                        "Day-night transitions bias utility toward diurnal/nocturnal specialization", "sharpens cycle specialization", "drift may invert cycle preference", "awakening smooths phase transitions", "fusion syncs nearby cycle bias", "memory tracks temporal utility shift",
                        metadata(Set.of("ritual", "temporal-adaptation"), Set.of("time-transition"), Set.of("ritual", "environmental", "exploration"), 0.76D, 0.67D, 0.84D, 0.88D, 0.19D, 0.64D)),

                template("social.observer_imprint", "Observer Imprint", AbilityFamily.PRECISION, AbilityTrigger.ON_PLAYER_WITNESS, AbilityMechanic.WITNESS_IMPRINT,
                        "Player witnesses imprint narrative memory into the artifact lineage", "expands witness memory context", "drift can record noisy witnesses", "awakening prioritizes reliable observers", "fusion links shared witness records", "memory accumulates witness interactions",
                        metadata(Set.of("social", "lineage-narrative"), Set.of("player-witness"), Set.of("social", "memory", "lineage"), 0.70D, 0.61D, 0.83D, 0.28D, 0.96D, 0.40D)),
                template("social.collective_insight", "Collective Insight", AbilityFamily.MOBILITY, AbilityTrigger.ON_PLAYER_GROUP_ACTION, AbilityMechanic.COLLECTIVE_RELAY,
                        "Group actions let nearby artifacts share temporary trigger opportunities", "extends relay breadth", "drift can relay redundant opportunities", "awakening improves relay relevance", "fusion deepens group synchronization", "memory stores cooperative trigger windows",
                        metadata(Set.of("social", "cooperation-relay"), Set.of("group-action"), Set.of("social", "support", "exploration"), 0.74D, 0.68D, 0.81D, 0.22D, 0.97D, 0.57D)),
                template("social.trader_whisper", "Trader Whisper", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_PLAYER_TRADE, AbilityMechanic.TRADE_SCENT,
                        "Trades briefly improve nearby resource detection quality", "improves trade-linked sensing duration", "drift can bias toward stale nodes", "awakening filters low-utility trade echoes", "fusion shares trade scent profiles", "memory tracks trade interaction effects",
                        metadata(Set.of("social", "resource-economy"), Set.of("trade-event"), Set.of("social", "gathering", "support"), 0.79D, 0.63D, 0.80D, 0.20D, 0.94D, 0.73D)),

                template("environment.weather_sensitivity", "Weather Sensitivity", AbilityFamily.SURVIVAL, AbilityTrigger.ON_WEATHER_CHANGE, AbilityMechanic.WEATHER_ATTUNEMENT,
                        "Weather shifts increase success of weather-dependent utility abilities", "improves adaptation horizon", "drift can react to false weather edges", "awakening filters noisy weather transitions", "fusion aligns weather adaptation signals", "memory tracks weather adaptation rate",
                        metadata(Set.of("environmental", "adaptation"), Set.of("weather-change"), Set.of("environmental", "support", "watchful"), 0.72D, 0.70D, 0.79D, 0.30D, 0.21D, 0.95D)),
                template("environment.structure_attunement", "Structure Attunement", AbilityFamily.CONSISTENCY, AbilityTrigger.ON_STRUCTURE_PROXIMITY, AbilityMechanic.STRUCTURE_ATTUNEMENT,
                        "Structure proximity boosts utility density for structure-bound abilities", "expands structure utility bands", "drift can over-focus dense ruins", "awakening improves proximity gating", "fusion triangulates nearby structure pressures", "memory tracks structure niche pressure",
                        metadata(Set.of("environmental", "structure-awareness"), Set.of("structure-proximity"), Set.of("environmental", "exploration", "watchful"), 0.91D, 0.84D, 0.74D, 0.28D, 0.17D, 0.82D)),
                template("environment.terrain_affinity", "Terrain Affinity", AbilityFamily.MOBILITY, AbilityTrigger.ON_ELEVATION_CHANGE, AbilityMechanic.TERRAIN_ADAPTATION,
                        "Elevation shifts adapt artifact utility toward local verticality patterns", "improves terrain adaptation memory", "drift can overweight noisy vertical moves", "awakening smooths terrain variance response", "fusion shares terrain adaptation gradients", "memory captures terrain variance interactions",
                        metadata(Set.of("environmental", "terrain-adaptation"), Set.of("elevation-shift"), Set.of("environmental", "exploration", "mobility"), 0.84D, 0.90D, 0.73D, 0.18D, 0.20D, 0.88D))
        );
    }

    private AbilityTemplate template(String id, String name, AbilityFamily family, AbilityTrigger trigger, AbilityMechanic mechanic,
                                     String effectPattern, String evolutionVariant, String driftVariant, String awakeningVariant, String fusionVariant,
                                     String memoryVariant, AbilityMetadata metadata) {
        AbilityMetadata enriched = metadata.triggerBudgetProfile() == null ? withBudgetDefaults(trigger, metadata) : metadata;
        return new AbilityTemplate(id, name, family, trigger, mechanic, effectPattern, evolutionVariant, driftVariant, awakeningVariant, fusionVariant,
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
}
