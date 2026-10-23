# Disposable Material Insight test

This separate probe creates synthetic equipment, grants its named player OP and
changes terrain in an **empty disposable world only**. Never install it on an
existing server. It is excluded from Maven's production plugin and normal tests.

Use the pinned official Purpur 26.2 / Java 25 Docker preparation from
`../double-jump/README.md`. Compile `InsightProbe.java` against the tested plugin
and API classpath, then package the classes and this directory's `plugin.yml` as
a separate `InsightProbe.jar`. Keep the production JAR identical across boot,
interaction and restart checks. Preserve source hashes, dependency digests,
logs, process exits and raw probe results.

1. Bind only `127.0.0.1:25598` **inside the container**, publish no ports, disable
   RCON/query, and use offline mode only in that isolated empty server. Keep the
   dashboard disabled. Install only the core and probe; create the probe data file
   `boot-only`. It verifies every optional integration is absent, generates terrain,
   writes `standalone.json` and stops.
2. Remove `boot-only`, add official checksum-verified ViaVersion/ViaBackwards
   5.12.1 for the controlled 26.1 client, and restart the disposable container.
   The reviewed minecraft-protocol 1.68.0/minecraft-data 3.117.0 dependencies do
   not support client 26.2. Use the recorded integrity lock and `npm ci
   --ignore-scripts --no-audit --no-fund` in the official Node image.
3. Run `node client.cjs <probe-data-directory>` in a client container sharing
   only the server container's network namespace. The client acknowledges loaded
   state/chunk batches, confirms teleports, sends normal right-click packets,
   then asks the probe to persist and stop. Give the server a finite timeout.
4. After clean exit, remove the two adapters and create `restart-only`. Restart
   with only core/probe; it checks the same seed, genes and still-active committed
   deadline against the resumed persistent game clock, writes `restart.json`
   and stops. Copy `standalone.json` from step 1 beside the final results.
5. Run `python3 verify-results.py <probe-data-directory>`. All server/client
   exits must also be zero, without a timeout or failure files.

Cases cover low/high crop information, sparse stone without filler, offhand
recovery before/after its deadline, both explicit protection-denial flags and
an effective ability permission denial for OP. Raw client chat verifies five
actual delivered reports with fact counts 2/5/5/2/5. Crop state remains age 4;
protection flags retain their original denial. The last real high-precision
inspection commits recovery immediately before shutdown. `continuity.json` also
checks a same-storage-identity replacement and codec round trip retain that
deadline on the native server; this does not synthesize a native awakening event.

Failed preparations are retained separately: a compiler launched before its
classpath was ready, a client missing the mandatory player-loaded packet, and
permission attachment reuse after reconnect. The corrected probe checks attachment
ownership; production protection handling was unchanged to obtain the passing run.
The first unit fixture also selected an unsupported pickaxe archetype; the corrected
fixture uses the existing axe catalog entry. No pickaxe support is added here.

This validates a controlled real server/protocol path and a standalone restart.
It does not certify graphical 26.2 clients, actual LuckPerms/PAPI/AuraSkills,
claims/anticheat combinations, player-perceived information value or natural ecology.
Ownership/trade and failed-delivery paths additionally have API-double/codec tests;
they are not claimed as native multiplayer trade tests. No live deployment is authorized.
