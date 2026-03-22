package obtuseloot.abilities;

import obtuseloot.evolution.MechanicNicheTag;

import java.util.Set;

public enum AbilityCategory {
    TRAVERSAL_MOBILITY(
            "Traversal / mobility",
            "Pathing, repositioning, chunk transitions, elevation changes, and threshold-crossing movement moments.",
            "Navigation anchors, route compression, relocation windows, terrain skipping, and movement-linked scouting.",
            "Supports explorers, raiders, skyblock progression, and fast-response group play.",
            Set.of(MechanicNicheTag.NAVIGATION, MechanicNicheTag.MOBILITY_UTILITY),
            Set.of("exploration", "mobility", "routing")),
    SENSING_INFORMATION(
            "Sensing / information",
            "Inspection, witnessing, scans, structure discovery, biome changes, and deliberate observation.",
            "Context reveals, threat reads, resource hints, pattern detection, and future-oriented scouting cues.",
            "Supports explorers, traders, scouts, planners, and adventure servers.",
            Set.of(MechanicNicheTag.ENVIRONMENTAL_SENSING, MechanicNicheTag.STRUCTURE_SENSING, MechanicNicheTag.INSPECT_INFORMATION),
            Set.of("watchful", "information", "discovery")),
    SURVIVAL_ADAPTATION(
            "Survival / adaptation",
            "Weather pressure, scarcity, harsh travel, recovery windows, and repeated exposure to demanding environments.",
            "Adaptation bonuses, resilience shaping, attrition smoothing, and harsh-biome readiness.",
            "Supports hardcore survival, solo resilience, and long-haul expeditions.",
            Set.of(MechanicNicheTag.ENVIRONMENTAL_ADAPTATION, MechanicNicheTag.GENERALIST),
            Set.of("survival", "adaptation", "hardcore")),
    COMBAT_TACTICAL_CONTROL(
            "Combat / tactical control",
            "Standoffs, witness pressure, low-health pivots, structure chokepoints, and coordinated group action.",
            "Target shaping, spacing control, tactical reveals, pursuit pressure, and bounded skirmish leverage.",
            "Supports fighters, factions/PvP, RPG tanks/skirmishers, and minigame control play.",
            Set.of(MechanicNicheTag.PROTECTION_WARDING, MechanicNicheTag.SOCIAL_WORLD_INTERACTION),
            Set.of("fighter", "tactical", "pvp")),
    DEFENSE_WARDING(
            "Defense / warding",
            "Base thresholds, perimeter upkeep, ritual tending, proximity checks, and reactive fortification moments.",
            "Wards, barriers, stabilization, anti-ambush cues, and safe-zone reinforcement.",
            "Supports base defense, builders, wardens, and settlement-focused SMP play.",
            Set.of(MechanicNicheTag.PROTECTION_WARDING, MechanicNicheTag.SUPPORT_COHESION, MechanicNicheTag.ENVIRONMENTAL_ADAPTATION),
            Set.of("defense", "warding", "fortress")),
    RESOURCE_FARMING_LOGISTICS(
            "Resource / farming / logistics",
            "Harvest loops, item pickup, repeated hauling, trade cadence, and throughput maintenance.",
            "Crop relays, stock routing, delivery hints, refill logic, and supply-chain smoothing.",
            "Supports farmers, traders, automation light-play, and progression servers.",
            Set.of(MechanicNicheTag.FARMING_WORLDKEEPING, MechanicNicheTag.SUPPORT_COHESION),
            Set.of("farming", "logistics", "economy")),
    CRAFTING_ENGINEERING_AUTOMATION(
            "Crafting / engineering / automation",
            "Repeated construction patterns, machine-adjacent actions, block inspection, and cadence-driven maintenance.",
            "Pattern amplification, fabrication hints, process chaining, automation planning, and workshop optimization.",
            "Supports builders, redstone/engineering players, skyblock progression, and technical SMPs.",
            Set.of(MechanicNicheTag.FARMING_WORLDKEEPING, MechanicNicheTag.INSPECT_INFORMATION, MechanicNicheTag.SUPPORT_COHESION),
            Set.of("builder", "engineering", "automation")),
    SOCIAL_SUPPORT_COORDINATION(
            "Social / support / coordination",
            "Group actions, trades, witnessing, greetings, co-presence, and planned cooperative moments.",
            "Shared relays, ally cues, convoy timing, supportive buffs, and coordination windows.",
            "Supports healers, traders, party play, rituals, and community-centered servers.",
            Set.of(MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.SUPPORT_COHESION),
            Set.of("support", "social", "teamplay")),
    RITUAL_STRANGE_UTILITY(
            "Ritual / strange utility",
            "Pattern repetition, altar completion, memory echoes, temporal shifts, and uncanny environmental signs.",
            "Ritual channels, anomaly shaping, weird utility conversion, omen control, and symbolic persistence.",
            "Supports RPG mystics, lore-heavy play, adventure servers, and social/ritual specialists.",
            Set.of(MechanicNicheTag.RITUAL_STRANGE_UTILITY, MechanicNicheTag.MEMORY_HISTORY, MechanicNicheTag.HIGH_COST_UTILITY),
            Set.of("ritual", "weirdness", "symbolism")),
    STEALTH_TRICKERY_DISRUPTION(
            "Stealth / trickery / disruption",
            "Silent movement, misdirection, contraband routing, witness confusion, and threshold interference.",
            "Decoys, spoofed signals, smuggling cues, denial fields, and infiltration-friendly setup.",
            "Supports rogues, spies, factions, infiltration, and mischievous adventure/minigame play.",
            Set.of(MechanicNicheTag.NAVIGATION, MechanicNicheTag.MOBILITY_UTILITY, MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.INSPECT_INFORMATION),
            Set.of("stealth", "trickery", "disruption"));

    private final String label;
    private final String identity;
    private final String typicalEffects;
    private final String gameplayRole;
    private final Set<MechanicNicheTag> niches;
    private final Set<String> motifs;

    AbilityCategory(String label, String identity, String typicalEffects, String gameplayRole, Set<MechanicNicheTag> niches, Set<String> motifs) {
        this.label = label;
        this.identity = identity;
        this.typicalEffects = typicalEffects;
        this.gameplayRole = gameplayRole;
        this.niches = niches;
        this.motifs = motifs;
    }

    public String label() { return label; }
    public String identity() { return identity; }
    public String typicalEffects() { return typicalEffects; }
    public String gameplayRole() { return gameplayRole; }
    public Set<MechanicNicheTag> niches() { return niches; }
    public Set<String> motifs() { return motifs; }
}
