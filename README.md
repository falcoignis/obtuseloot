# ObtuseLoot
### Evolving artifacts with identity — no rarity ladders, no disposable loot.

## Why Players Remember This Plugin
Most Minecraft loot is replaceable by the next bigger number. ObtuseLoot does the opposite: your gear develops a personal identity through real use. The artifact you carry into fights starts to define itself over time, becoming more distinct the longer you trust it. You are not farming for a color tier — you are shaping a legacy.

## What Makes ObtuseLoot Different
In ObtuseLoot, artifacts are not just stats on an item.

Each one has a consistent identity that grows through your choices, your fights, and your playstyle.

There is no traditional rarity system pushing everything into predictable tiers. Instead, items become notable because of what they have lived through with you.

## Core Systems
### Artifact Identity
Artifacts are meant to feel like *specific* items, not interchangeable drops. When one clicks with your build, it stays meaningful.

### Awakening
Some artifacts can undergo a rare, one-time awakening. This moment sharpens what the item is, making it more powerful and more defined.

### Convergence
As an artifact’s history deepens, it can shift into an entirely new form. It does not just improve — it can become something different.

### Significance
Instead of tier labels, items gain presence. Some feel ordinary, others feel unmistakably important — based on the journey behind them.

### Memory & Use
Artifacts remember how they are used. The more time you spend with one, the more it becomes fully itself.

## Features
- Fully procedural artifacts
- No duplicate “same sword, bigger numbers” loops
- Equipment-only system (real items only)
- Artifacts evolve through actual use
- Unique identities instead of fixed templates
- Awakening and transformation systems
- Persistent progression that stays with the item

## Commands
| Command | Description |
| ------- | ----------- |
| `/obtuseloot` / `/obtuseloot help` | Show command help |
| `/obtuseloot info` | Show plugin and ecosystem status summary |
| `/obtuseloot ecosystem ...` | View ecosystem health, dashboard, map, environment, and dump tools |
| `/obtuseloot give <player>` | Generate and give a fresh artifact |
| `/obtuseloot give-specific <player> <archetype\|family>` | Give a constrained artifact by archetype or family |
| `/obtuseloot convert` | Turn your held item into an artifact |
| `/obtuseloot reroll` | Reroll your held artifact identity |
| `/obtuseloot inspect` | View held artifact details |
| `/obtuseloot force-awaken` / `/obtuseloot force-converge` | Force major artifact transitions |
| `/obtuseloot refresh` / `/obtuseloot reset` / `/obtuseloot reload` | Admin profile reset and plugin reload commands |
| `/obtuseloot addname` / `/obtuseloot removename` | Edit artifact name pools |
| `/obtuseloot repair-state` / `/obtuseloot debug-profile` / `/obtuseloot dump-held` | Artifact repair and diagnostic tools |
| `/obtuseloot debug ...` (incl. `debug dashboard`) | Full debug command suite |
| `/ol ...` | Alias for matching `/obtuseloot` commands |

For full command details and permissions, see [`commands and permissions.md`](./commands%20and%20permissions.md).

## Installation
- Requires **Paper** or **Purpur**
- Build with Maven: `mvn clean package`
- Take the generated `.jar` from `target/`
- Drop it into your server’s `/plugins` folder
- Start or restart the server

## Roadmap
- PlaceholderAPI support
- Protection integrations (GriefDefender / WorldGuard)
- Player analytics integration (Plan)

---

You don’t grind for better loot here — you shape it.
