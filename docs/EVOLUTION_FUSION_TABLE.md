# Evolution + Fusion Recipe Table (Draft)

This table reflects the behavioral/archetype model now wired into runtime config.

## Archetypes

Archetype assignment is behavior-driven from reputation vectors once `evolution.archetype-score` is reached.

- `vanguard`: survival + consistency
- `deadeye`: precision + consistency
- `ravager`: brutality + chaos
- `strider`: mobility + precision
- `harbinger`: chaos + survival
- `warden`: survival + precision
- fallback: `paragon` when no dominant lead clears `evolution.archetype-dominance-delta`

## Evolution stages

- `<archetype>-initiate` at archetype unlock
- `<archetype>-adept` at `evolution.tempered-score`
- `<archetype>-ascendant` at `evolution.mythic-score`
- Hybrid specialization at `evolution.hybrid-score` via behavioral resolver

## Fusion recipes

Fusion requires `fusion.min-score` and a matching archetype plus minima:

| Fusion ID | Archetype | Precision | Brutality | Survival | Mobility | Chaos | Consistency | Boss Kills |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `stormglass-reaver` | `deadeye` | 150 | 40 | 60 | 120 | 20 | 110 | 2 |
| `aegis-paragon` | `vanguard` | 60 | 50 | 160 | 40 | 10 | 140 | 2 |
| `cataclyst-tyrant` | `ravager` | 20 | 160 | 70 | 80 | 150 | 50 | 3 |
| `horizon-keeper` | `warden` | 120 | 30 | 140 | 70 | 20 | 120 | 2 |

These values are intentionally conservative starter targets and are expected to be tuned via playtests.
