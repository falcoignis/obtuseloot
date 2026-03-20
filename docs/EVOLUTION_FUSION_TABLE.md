# Evolution + Fusion Recipe Table (Draft v2)

This document is the balancing reference for the behavioral evolution concept build.
It is intentionally opinionated and should be tuned from playtest telemetry.

## 1) Archetype Matrix

Archetype assignment unlocks at `evolution.archetype-threshold` and is selected by dominant pair score.
If first place does not beat second place by at least `evolution.archetype-switch-margin`, fallback is `paragon`.

### Scoring pairs

| Archetype | Primary vector formula | Gameplay identity | Typical player behavior |
|---|---|---|---|
| `vanguard` | `survival + consistency` | Anchor / frontline stabilizer | Long fights, safe clears, low-risk routing |
| `deadeye` | `precision + consistency` | Surgical burst / execution | High-damage timing, controlled combat cadence |
| `ravager` | `brutality + chaos` | Aggro pressure / volatility | Chain kills, risky trades, high variance |
| `strider` | `mobility + precision` | Hit-and-run skirmisher | Repositioning, kiting, aerial/parkour aggression |
| `harbinger` | `chaos + survival` | Attrition disruptor | Survives disorder while creating battlefield noise |
| `warden` | `survival + precision` | Durable controller | Defensive play with selective punishes |
| `paragon` (fallback) | no clear lead | Adaptive hybrid | Mixed behavior without decisive specialization |

### Stage naming by score

- `<archetype>-initiate` at archetype unlock
- `<archetype>-adept` at `evolution.tempered-threshold`
- `<archetype>-advanced` at `evolution.advanced-threshold`
- hybrid specialization at `evolution.hybrid-threshold` via behavioral tie-breakers

## 2) Hybrid Specialization Matrix

At hybrid threshold, each archetype branches into one of two identity outcomes based on secondary behavior.

| Archetype | Branch A condition | Branch A result | Branch B condition | Branch B result |
|---|---|---|---|---|
| `deadeye` | `mobility > survival` | `deadeye-tempest` | else | `deadeye-warden` |
| `vanguard` | `consistency > chaos` | `vanguard-aegis` | else | `vanguard-dreadnought` |
| `ravager` | `chaos > consistency` | `ravager-maelstrom` | else | `ravager-executioner` |
| `strider` | `precision > brutality` | `strider-falcon` | else | `strider-reaver` |
| `harbinger` | `chaos > mobility` | `harbinger-void` | else | `harbinger-storm` |
| `warden` | `survival > mobility` | `warden-scholar` | else | `warden-ranger` |
| `paragon` | `precision > brutality` | `paragon-lumen` | else | `paragon-umbra` |

## 3) Fusion Recipe Table

Fusion requires:
1. `rep.score >= fusion.min-score`
2. current archetype matches recipe archetype
3. each per-stat and boss-kill minimum is met

### Baseline recipes (currently in config)

| Fusion ID | Archetype | Precision | Brutality | Survival | Mobility | Chaos | Consistency | Boss Kills | Role intent |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| `stormglass-reaver` | `deadeye` | 150 | 40 | 60 | 120 | 20 | 110 | 2 | Burst marksman with rotational mobility |
| `aegis-paragon` | `vanguard` | 60 | 50 | 160 | 40 | 10 | 140 | 2 | Anti-burst anchor / team stabilizer |
| `cataclyst-tyrant` | `ravager` | 20 | 160 | 70 | 80 | 150 | 50 | 3 | High-volatility pressure carry |
| `horizon-keeper` | `warden` | 120 | 30 | 140 | 70 | 20 | 120 | 2 | Durable controller with ranged discipline |

### Candidate expansion recipes (recommended next playtest)

| Fusion ID | Archetype | Precision | Brutality | Survival | Mobility | Chaos | Consistency | Boss Kills | Role intent |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| `windscar-phantom` | `strider` | 140 | 60 | 70 | 160 | 40 | 90 | 2 | Ultra-mobile duelist with precision spikes |
| `voidhymn-oracle` | `harbinger` | 80 | 70 | 130 | 90 | 150 | 100 | 3 | Chaos-resistant disruptor / attrition mage |
| `crown-of-twins` | `paragon` | 120 | 120 | 120 | 120 | 80 | 120 | 4 | Generalist apex path for balanced players |

## 4) Tuning Heuristics

- Raise `evolution.archetype-dominance-delta` if archetypes switch too often.
- Raise `fusion.min-score` if too many players reach fused states in one session.
- Increase `min-boss-kills` first (before raising stat gates) to preserve combat identity while pacing endgame.
