# Disposable native utility-ability test

This probe covers Footprint Memory, Weather Omen, Herd Instinct and Redstone Sympathy
(PR #435) on a real Purpur 26.2 server. It creates synthetic gear, a cow, circuit
blocks and stone platforms, grants its named player OP and shuts the server down.
Use it only on a fresh, explicitly authorized, unpublished server. Never install it on
an existing world. It is excluded from Maven's production plugin and normal tests.

Use the pinned official Purpur 26.2 / Java 25 Docker preparation from
`../double-jump/README.md` and the server isolation rules from `../material-insight/README.md`:
loopback-only `127.0.0.1:25598` inside the container, no published ports, RCON/query
and dashboard disabled, offline mode only in this isolated server, flat disposable world.
Compile `UtilityProbe.java` against the tested plugin/API classpath and package it with
this directory's `plugin.yml`. Keep the production JAR identical across all three starts.

1. Core and probe only, with `boot-only` in `plugins/ObtuseLootUtilityProbe/`. Requires
   every optional integration absent and all four live effects registered. It records the
   native `GameRule.DO_WEATHER_CYCLE` read in `standalone.json`.
2. Remove `boot-only`, add checksum-verified ViaVersion/ViaBackwards 5.12.1 for the
   controlled 26.1 client (minecraft-protocol 1.68.0 / minecraft-data 3.117.0 lack 26.2),
   start the server, and run `node client.cjs <probe-data-directory>` in the official
   Node image sharing only the server container's network namespace.
3. Remove the adapters, create `restart-only`, restart. `restart.json` must show the
   `fp-high` item's waypoint survived the restart and a codec round trip.
4. Run `python3 verify-results.py <probe-data-directory>`. All exits must be zero, with
   no timeout or failure file.

Each fixture item is searched until its equipped live abilities are exactly the tested
one, so no other implemented effect shares the click or the item's recovery. The tested
gene is set to 0 (`-low`) or 1 (all other roles). Expected values follow the catalog
tradeoff (gene 0 = 0.5x, gene 1 = 1.5x of the defaults in `docs/noncombat-ability-batch.md`).

## Registered expectations (written before the first run)

| Ability | Case | Required result |
|---|---|---|
| Footprint Memory | `fp-high` | Server sees sneaking. Sneak top-face click: "return point remembered", waypoint stored. Off-hand click during recovery: no report, recovery and waypoint unchanged. After recovery, 200 blocks east, sneak side-face off-hand click: "west, 200 blocks; 0 blocks up". Two samples, benefit 1, recovery 90. Waypoint survives restart. |
| | `fp-low` | Unsneaked click: no report, no recovery. Mark, then recall at 200 blocks (range 128): no report, recovery unchanged. Recall at 100: "west, 100 blocks; 0 blocks up". Recovery 30. |
| Herd Instinct | `herd-low` / `-high` / `-young` | Two clicks on one cow: exactly one report and one sample (no double fire, second click recovering). Facts 2 / 5 / 5, recovery 40 / 120 / 120. Adult reads "cow ... adult ... breeding ready"; baby reads "young; growth in". |
| | `herd-deny` / `-permission` | Cancelled entity event or ability permission off (OP retained): no report, no recovery. |
| Redstone Sympathy | `redstone-low` / `-high` | Piston: "piston \| extended false" (recovery 40) / "piston \| extended false \| facing up" (recovery 120). |
| | `redstone-stone`, `-deny-block`, `-deny-item`, `-permission` | No report, no recovery. |
| | `redstone-lever` / `-repeater` / `-wire` | Exactly one report. Contents are recorded, not asserted: vanilla right-click changes these blocks, and the report runs one tick later. |
| Weather Omen | `weather-low` | One chunk crossing: "world weather clear \| thunder false", recovery 100. |
| | `weather-high` / `-forced` / `-storm` | Five facts, recovery 300: running cycle with weather timers / forced clear window / storm and thunder. |
| | `weather-paused` | Three facts ending "natural weather cycle paused or unavailable". |
| | `weather-permission` | No report, no recovery. **Added after run 1:** the denied crossing must appear as a server move event; then the permission is restored on the same item and one more unvisited chunk crossing must deliver exactly one five-fact report. |

A failed expectation is a finding for the owner. Do not change production code or these
expectations to obtain a pass; keep failing evidence separately.

Not covered: a real two-player hand-over (trade is covered only by the codec round trip),
recovery shared between two abilities on one item (fixtures are single-ability by design),
walking-scan rejection of unloaded destinations,
graphical 26.2 clients, LuckPerms/PAPI/AuraSkills, claim plugins, anticheat,
player-perceived usefulness and natural ecology. No live deployment is authorized.
