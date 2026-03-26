# Commands and Permissions

This document lists all currently implemented ObtuseLoot commands and permission nodes based on the current code and `plugin.yml`. Command availability can depend on sender type (player vs console), and some commands use an optional player target when run from console.

## Command Summary

| Command | Description | Permission | Sender |
|---|---|---|---|
| `/obtuseloot` / `/obtuseloot help` | Show command help | `obtuseloot.help` | Both |
| `/obtuseloot info` | Show plugin status | `obtuseloot.info` | Both |
| `/obtuseloot dashboard` | Show ecosystem dashboard summary | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem` | Show ecosystem dashboard summary | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem health` | Show ecosystem health summary | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem dashboard` | Show ecosystem dashboard summary | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem map` | Show trait interaction map file path summary | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem map <lineage\|species\|collapse\|genome ...\|off>` | Start/adjust/stop live map renderer | `obtuseloot.info` | Both (with player-specific behavior for `off`) |
| `/obtuseloot ecosystem map genome <trait>` | Render genome-trait hotspot map mode | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem environment` | Show current environmental pressure multipliers | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem dump` | Write ecosystem safety JSON snapshot | `obtuseloot.info` | Both |
| `/obtuseloot ecosystem reset-metrics` | Reset rolling ecosystem safety metrics | `obtuseloot.admin` | Both |
| `/obtuseloot refresh [player]` | Recreate tracked artifact profile | `obtuseloot.admin` | Both |
| `/obtuseloot reset [player]` | Clear tracked artifact/reputation state | `obtuseloot.admin` | Both |
| `/obtuseloot reload` | Reload config/runtime/name pools | `obtuseloot.admin` | Both |
| `/obtuseloot addname <pool> <value>` | Add name pool entry | `obtuseloot.edit` or `obtuseloot.edit.<pool>` | Both |
| `/obtuseloot removename <pool> <value>` | Remove name pool entry | `obtuseloot.edit` or `obtuseloot.edit.<pool>` | Both |
| `/obtuseloot give <player>` | Generate and give fresh artifact | `obtuseloot.command.give` | Both |
| `/obtuseloot convert` | Convert held equipment to artifact | `obtuseloot.command.convert` | Player |
| `/obtuseloot reroll` | Reroll held artifact identity | `obtuseloot.command.reroll` | Player |
| `/obtuseloot inspect` | Inspect held artifact details | `obtuseloot.command.inspect` | Player |
| `/obtuseloot force-awaken` | Force awakening on held artifact | `obtuseloot.command.forceawaken` | Player |
| `/obtuseloot force-converge` | Force convergence on held artifact | `obtuseloot.command.forceconverge` | Player |
| `/obtuseloot repair-state` | Rebuild held artifact derived state | `obtuseloot.command.repairstate` | Player |
| `/obtuseloot debug-profile` | Deep structured held artifact diagnostics | `obtuseloot.command.debugprofile` | Player |
| `/obtuseloot give-specific <player> <archetype\|family>` | Generate constrained artifact and give | `obtuseloot.command.givespecific` | Both |
| `/obtuseloot dump-held` | Log structured held artifact snapshot | `obtuseloot.command.dumpheld` | Player |
| `/obtuseloot debug ...` | Full debug command suite | `obtuseloot.debug` (players), OP bypass supported | Both |
| `/obtuseloot debug dashboard` | Regenerate dashboard + trait heatmap | `obtuseloot.debug` | Both |
| `/ol ...` | Alias of `/obtuseloot` | Same as matching `/obtuseloot` form | Same as matching `/obtuseloot` form |

## Detailed Command Reference

### `/obtuseloot` or `/obtuseloot help`
- **What it does:** Shows the command reference.
- **Usage:** `/obtuseloot`, `/obtuseloot help`
- **Permission:** `obtuseloot.help`
- **Who can use it:** Both
- **Notes:** `plugin.yml` defines command-level permission `obtuseloot.command`, so servers using Bukkit command permission gating may require that as well.

### `/obtuseloot info`
- **What it does:** Shows a basic runtime-active message.
- **Usage:** `/obtuseloot info`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot dashboard`
- **What it does:** Shows ecosystem health/dashboard summary and dashboard link/path.
- **Usage:** `/obtuseloot dashboard`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem`
- **What it does:** Alias-form for ecosystem health/dashboard summary.
- **Usage:** `/obtuseloot ecosystem`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem health`
- **What it does:** Shows ecosystem health summary.
- **Usage:** `/obtuseloot ecosystem health`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem dashboard`
- **What it does:** Shows ecosystem dashboard summary.
- **Usage:** `/obtuseloot ecosystem dashboard`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem map`
- **What it does:** Prints map summary path.
- **Usage:** `/obtuseloot ecosystem map`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem map lineage|species|collapse`
- **What it does:** Hands off to ecosystem map renderer in selected mode.
- **Usage:** `/obtuseloot ecosystem map <lineage|species|collapse>`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both
- **Notes:** Actual rendering behavior is implemented in map renderer command handling.

### `/obtuseloot ecosystem map genome <trait>`
- **What it does:** Hands off to renderer for genome trait intensity hotspots.
- **Usage:** `/obtuseloot ecosystem map genome <trait>`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both

### `/obtuseloot ecosystem map off`
- **What it does:** Disables live ecosystem map rendering.
- **Usage:** `/obtuseloot ecosystem map off`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both
- **Notes:** Direct disable call is player-specific; non-player senders fall through to renderer handling.

### `/obtuseloot ecosystem environment`
- **What it does:** Shows current environmental event and per-trait multipliers.
- **Usage:** `/obtuseloot ecosystem environment`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both
- **Notes:** Implemented in code even though this form is missing from `plugin.yml` usage text.

### `/obtuseloot ecosystem dump`
- **What it does:** Writes `analytics/safety/ecosystem-safety-dump.json` and prints snapshot summary.
- **Usage:** `/obtuseloot ecosystem dump`
- **Permission:** `obtuseloot.info`
- **Who can use it:** Both
- **Notes:** Enforced cooldown applies.

### `/obtuseloot ecosystem reset-metrics`
- **What it does:** Clears ecosystem safety rolling metrics.
- **Usage:** `/obtuseloot ecosystem reset-metrics`
- **Permission:** `obtuseloot.admin`
- **Who can use it:** Both

### `/obtuseloot refresh [player]`
- **What it does:** Unloads and recreates artifact profile for target.
- **Usage:** `/obtuseloot refresh [player]`
- **Permission:** `obtuseloot.admin`
- **Who can use it:** Both
- **Notes:** If console omits player, it fails; players can omit target to act on self.

### `/obtuseloot reset [player]`
- **What it does:** Clears tracked artifact + reputation state for target.
- **Usage:** `/obtuseloot reset [player]`
- **Permission:** `obtuseloot.admin`
- **Who can use it:** Both
- **Notes:** If console omits player, it fails; players can omit target to act on self.

### `/obtuseloot reload`
- **What it does:** Reloads plugin config/runtime settings/name pools.
- **Usage:** `/obtuseloot reload`
- **Permission:** `obtuseloot.admin`
- **Who can use it:** Both

### `/obtuseloot addname <pool> <value>`
- **What it does:** Adds an entry to a name pool.
- **Usage:** `/obtuseloot addname <prefixes|suffixes> <value>`
- **Permission:** `obtuseloot.edit` or scoped `obtuseloot.edit.<pool>`
- **Who can use it:** Both
- **Notes:** Invalid pool or duplicate/invalid input returns no-change message.

### `/obtuseloot removename <pool> <value>`
- **What it does:** Removes an entry from a name pool.
- **Usage:** `/obtuseloot removename <prefixes|suffixes> <value>`
- **Permission:** `obtuseloot.edit` or scoped `obtuseloot.edit.<pool>`
- **Who can use it:** Both
- **Notes:** Removal can fail if entry missing/invalid or would empty the pool.

### `/obtuseloot give <player>`
- **What it does:** Generates and delivers a fresh artifact item.
- **Usage:** `/obtuseloot give <player>`
- **Permission:** `obtuseloot.command.give`
- **Who can use it:** Both
- **Notes:** Drops item at player location if inventory full.

### `/obtuseloot convert`
- **What it does:** Converts held non-artifact equipment into a fresh artifact identity.
- **Usage:** `/obtuseloot convert`
- **Permission:** `obtuseloot.command.convert`
- **Who can use it:** Player
- **Notes:** Requires valid held equipment item; rejects air, non-equipment, or already-artifact item.

### `/obtuseloot reroll`
- **What it does:** Rerolls held artifact identity while preserving equipment archetype.
- **Usage:** `/obtuseloot reroll`
- **Permission:** `obtuseloot.command.reroll`
- **Who can use it:** Player
- **Notes:** Requires holding a resolvable artifact item.

### `/obtuseloot inspect`
- **What it does:** Displays structured held artifact runtime details.
- **Usage:** `/obtuseloot inspect`
- **Permission:** `obtuseloot.command.inspect`
- **Who can use it:** Player
- **Notes:** Requires holding a resolvable artifact item.

### `/obtuseloot force-awaken`
- **What it does:** Runs real awakening pipeline on held artifact.
- **Usage:** `/obtuseloot force-awaken`
- **Permission:** `obtuseloot.command.forceawaken`
- **Who can use it:** Player
- **Notes:** Requires dormant held artifact; may fail if no valid awakening path exists.

### `/obtuseloot force-converge`
- **What it does:** Runs real convergence pipeline on held artifact.
- **Usage:** `/obtuseloot force-converge`
- **Permission:** `obtuseloot.command.forceconverge`
- **Who can use it:** Player
- **Notes:** Requires holding valid artifact; fails if convergence has no valid non-no-op path.

### `/obtuseloot repair-state`
- **What it does:** Rebuilds held artifact derived state (naming/lore/projections) without identity mutation.
- **Usage:** `/obtuseloot repair-state`
- **Permission:** `obtuseloot.command.repairstate`
- **Who can use it:** Player
- **Notes:** Requires holding a valid artifact.

### `/obtuseloot debug-profile`
- **What it does:** Prints deep diagnostic profile for held artifact.
- **Usage:** `/obtuseloot debug-profile`
- **Permission:** `obtuseloot.command.debugprofile`
- **Who can use it:** Player
- **Notes:** Requires holding a valid artifact.

### `/obtuseloot give-specific <player> <archetype|family>`
- **What it does:** Generates constrained artifact by exact archetype id or ability family.
- **Usage:** `/obtuseloot give-specific <player> <archetype|family>`
- **Permission:** `obtuseloot.command.givespecific`
- **Who can use it:** Both
- **Notes:** No generic fallback if selector is invalid.

### `/obtuseloot dump-held`
- **What it does:** Logs concise structured snapshot of held artifact identity.
- **Usage:** `/obtuseloot dump-held`
- **Permission:** `obtuseloot.command.dumpheld`
- **Who can use it:** Player
- **Notes:** Requires holding a valid artifact.

### `/obtuseloot debug help`
- **What it does:** Shows debug command list.
- **Usage:** `/obtuseloot debug help`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug inspect [player]`
- **What it does:** Debug-inspects full artifact + reputation state.
- **Usage:** `/obtuseloot debug inspect [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug rep set <stat> <value> [player]`
- **What it does:** Sets one reputation stat.
- **Usage:** `/obtuseloot debug rep set <precision|brutality|survival|mobility|chaos|consistency|kills|bosskills|recentkillchain|survivalstreak> <value> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug rep add <stat> <value> [player]`
- **What it does:** Adds to one reputation stat.
- **Usage:** `/obtuseloot debug rep add <stat> <value> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug rep reset [player]`
- **What it does:** Resets reputation.
- **Usage:** `/obtuseloot debug rep reset [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug evolve [player]`
- **What it does:** Forces evolution evaluation.
- **Usage:** `/obtuseloot debug evolve [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug drift [player]`
- **What it does:** Forces a drift mutation.
- **Usage:** `/obtuseloot debug drift [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug awaken [player]`
- **What it does:** Forces awakening transition via engine.
- **Usage:** `/obtuseloot debug awaken [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug fuse [player]`
- **What it does:** Attempts convergence/fusion transition.
- **Usage:** `/obtuseloot debug fuse [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug lore [player]`
- **What it does:** Prints action bar + lore lines.
- **Usage:** `/obtuseloot debug lore [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug reset [player]`
- **What it does:** Resets artifact, reputation, combat context.
- **Usage:** `/obtuseloot debug reset [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug save [player]`
- **What it does:** Saves artifact + reputation.
- **Usage:** `/obtuseloot debug save [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug reload`
- **What it does:** Reloads runtime config/settings and invalidates relevant caches.
- **Usage:** `/obtuseloot debug reload`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug instability clear [player]`
- **What it does:** Clears artifact instability.
- **Usage:** `/obtuseloot debug instability clear [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug archetype set <archetype> [player]`
- **What it does:** Sets debug archetype path.
- **Usage:** `/obtuseloot debug archetype set <unformed|vanguard|deadeye|ravager|strider|harbinger|warden|paragon> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug path set <evolutionPath> [player]`
- **What it does:** Sets evolution path string directly.
- **Usage:** `/obtuseloot debug path set <evolutionPath> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug seed show [player]`
- **What it does:** Displays artifact seed.
- **Usage:** `/obtuseloot debug seed show [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug seed reroll [player]`
- **What it does:** Rerolls artifact seed and resets progression identity state.
- **Usage:** `/obtuseloot debug seed reroll [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug seed set <seed> [player]`
- **What it does:** Sets explicit seed and resets progression identity state.
- **Usage:** `/obtuseloot debug seed set <seed> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug seed import <seed> [player]`
- **What it does:** Imports explicit seed and resets progression identity state.
- **Usage:** `/obtuseloot debug seed import <seed> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug seed export [player]`
- **What it does:** Prints exportable seed + identity summary.
- **Usage:** `/obtuseloot debug seed export [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug ability [show|refresh|explain|tree] [player]`
- **What it does:** Displays/refreshes/explains ability profile.
- **Usage:** `/obtuseloot debug ability [show|refresh|explain|tree] [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug memory [player]`
- **What it does:** Shows artifact memory snapshot and influence.
- **Usage:** `/obtuseloot debug memory [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug persistence backend`
- **What it does:** Shows active persistence backend/status.
- **Usage:** `/obtuseloot debug persistence backend`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug persistence test`
- **What it does:** Prints persistence backend test/status line.
- **Usage:** `/obtuseloot debug persistence test`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug persistence migrate yaml-to-sqlite|yaml-to-mysql|nbt-artifacts`
- **What it does:** Runs configured migration path.
- **Usage:** `/obtuseloot debug persistence migrate yaml-to-sqlite|yaml-to-mysql|nbt-artifacts`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both
- **Notes:** `nbt-artifacts` migration scans online players.

### `/obtuseloot debug ecosystem [bias|balance]`
- **What it does:** Shows ecosystem snapshot, bias, or balance weights.
- **Usage:** `/obtuseloot debug ecosystem [bias|balance]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug lineage [player]`
- **What it does:** Shows assigned lineage info.
- **Usage:** `/obtuseloot debug lineage [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug genome interactions`
- **What it does:** Generates trait interaction heatmap/matrix and prints top pairs.
- **Usage:** `/obtuseloot debug genome interactions`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both
- **Notes:** Requires loaded artifacts to produce output.

### `/obtuseloot debug projection [cache|stats]`
- **What it does:** Shows trait projection cache/performance telemetry.
- **Usage:** `/obtuseloot debug projection [cache|stats]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug subscriptions [stats|player]`
- **What it does:** Shows trigger subscription index stats or per-player subscriptions.
- **Usage:** `/obtuseloot debug subscriptions [stats|player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug artifact [storage|resolve|cache|stats] [player]`
- **What it does:** Shows artifact storage/cache/resolve diagnostics.
- **Usage:** `/obtuseloot debug artifact [storage|resolve|cache|stats] [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate help`
- **What it does:** Shows simulate command help.
- **Usage:** `/obtuseloot debug simulate help`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate hit <damage> [player]`
- **What it does:** Simulates combat hit progression.
- **Usage:** `/obtuseloot debug simulate hit <damage> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate move <distance> [player]`
- **What it does:** Injects movement into combat context.
- **Usage:** `/obtuseloot debug simulate move <distance> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate lowhp [player]`
- **What it does:** Marks low-health state for survival testing.
- **Usage:** `/obtuseloot debug simulate lowhp [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate kill [player]`
- **What it does:** Simulates kill progression.
- **Usage:** `/obtuseloot debug simulate kill [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate multikill <count> [player]`
- **What it does:** Simulates rapid multi-kill progression.
- **Usage:** `/obtuseloot debug simulate multikill <count> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate bosses <count> [player]`
- **What it does:** Simulates repeated boss kills.
- **Usage:** `/obtuseloot debug simulate bosses <count> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate chaos [player]`
- **What it does:** Prepares chaotic combat context.
- **Usage:** `/obtuseloot debug simulate chaos [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate cycle [player]`
- **What it does:** Runs multi-step simulation cycle.
- **Usage:** `/obtuseloot debug simulate cycle [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate resetcontext [player]`
- **What it does:** Clears combat context.
- **Usage:** `/obtuseloot debug simulate resetcontext [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug simulate path <precision|brutality|mobility|survival|chaos|boss|hybrid|awaken|drift> [player]`
- **What it does:** Applies predefined simulation profile.
- **Usage:** `/obtuseloot debug simulate path <precision|brutality|mobility|survival|chaos|boss|hybrid|awaken|drift> [player]`
- **Permission:** `obtuseloot.debug` (players), OP bypass exists
- **Who can use it:** Both

### `/obtuseloot debug dashboard`
- **What it does:** Regenerates dashboard and interaction heatmap/report.
- **Usage:** `/obtuseloot debug dashboard`
- **Permission:** `obtuseloot.debug`
- **Who can use it:** Both

## Permission Nodes

| Permission | Default | Grants Access To | Notes |
|---|---|---|---|
| `obtuseloot.help` | `true` | `/obtuseloot` help output | Explicitly checked in code. |
| `obtuseloot.info` | `true` | `info`, dashboard/ecosystem read/dump/map/environment commands | Explicitly checked in code. |
| `obtuseloot.inspect` | `op` | Not directly used by current command handlers | Defined in `plugin.yml`, but current command code uses `obtuseloot.command.inspect`. |
| `obtuseloot.admin` | `op` | `refresh`, `reset`, `reload`, `ecosystem reset-metrics` | Explicitly checked in code. |
| `obtuseloot.edit` | `op` | Full name-pool editing (`addname`/`removename`) | Treated as parent permission in code for scoped edit nodes. |
| `obtuseloot.edit.prefixes` | `op` | Edit only `prefixes` pool | Checked via scoped edit logic. |
| `obtuseloot.edit.suffixes` | `op` | Edit only `suffixes` pool | Checked via scoped edit logic. |
| `obtuseloot.debug` | `op` | Entire debug suite (`/obtuseloot debug ...`, including `debug dashboard`) | Player OPs bypass explicit permission check in debug command. |
| `obtuseloot.command` | `op` | Root command permission on `/obtuseloot` registration | Set in `plugin.yml` command registration (Bukkit-level gate). |
| `obtuseloot.command.give` | `op` | `/obtuseloot give <player>` | Explicitly checked in code. |
| `obtuseloot.command.convert` | `op` | `/obtuseloot convert` | Explicitly checked in code. |
| `obtuseloot.command.reroll` | `op` | `/obtuseloot reroll` | Explicitly checked in code. |
| `obtuseloot.command.inspect` | `op` | `/obtuseloot inspect` | Explicitly checked in code. |
| `obtuseloot.command.forceawaken` | `op` | `/obtuseloot force-awaken` | Explicitly checked in code. |
| `obtuseloot.command.forceconverge` | `op` | `/obtuseloot force-converge` | Explicitly checked in code. |
| `obtuseloot.command.repairstate` | `op` | `/obtuseloot repair-state` | Explicitly checked in code. |
| `obtuseloot.command.debugprofile` | `op` | `/obtuseloot debug-profile` | Explicitly checked in code. |
| `obtuseloot.command.givespecific` | `op` | `/obtuseloot give-specific <player> <archetype\|family>` | Explicitly checked in code. |
| `obtuseloot.command.dumpheld` | `op` | `/obtuseloot dump-held` | Explicitly checked in code. |

## Permission Hierarchy Notes

- The plugin does **not** define parent-child permission relationships in `plugin.yml` `permissions:` metadata.
- The code manually treats `obtuseloot.edit` as a parent for `obtuseloot.edit.<pool>` (`prefixes`/`suffixes`).
- Debug commands use a single gate (`obtuseloot.debug`) for the whole debug tree.
- `plugin.yml` sets command registration permission `obtuseloot.command` on `/obtuseloot`; this is an additional Bukkit-level gate that may block users before subcommand checks run.

## Notes and Caveats

- Alias `ol` is registered for `/obtuseloot`.
- Several commands are explicitly player-only (`convert`, `reroll`, `inspect`, `force-awaken`, `force-converge`, `repair-state`, `debug-profile`, `dump-held`).
- Many debug commands are usable from console, but console must provide a target player when the command path needs one.
- `plugin.yml` usage text is not fully synced with implementation (`/obtuseloot ecosystem environment` exists in code but is omitted from usage block).
- `obtuseloot.inspect` exists in `plugin.yml` permissions but is not currently used by the command handlers.
