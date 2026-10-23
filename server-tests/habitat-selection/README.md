# Disposable native habitat fixture

Use only a fresh, explicitly authorized, unpublished Purpur 26.2/Java 25 server.
This probe creates synthetic gear and terrain, grants its named bot OP and shuts
its server down. Never install it on an existing game world. It is excluded from
normal Maven compilation and the production plugin JAR.

Build the core and its dependency classpath as described in
`../double-jump/README.md`, then compile `HabitatProbe.java` separately and package
its classes with this directory's `plugin.yml`. Use the same pinned protocol
client dependencies from `../double-jump/package.json`, plus checksum-verified
Purpur 26.2 build 2633 and ViaVersion/ViaBackwards 5.12.1.

Bind the server to loopback, disable RCON/query and the core's dashboard, use a
flat disposable world and disable online/secure-profile enforcement only here.
Publish no Docker ports. The Node client can share the server container's network
namespace (`--network container:<test-server>`) while connecting to 127.0.0.1:25598.
Mount only disposable data and the needed build cache; keep finite process deadlines.

1. Start with only the core and probe. Create `boot-only` under
   `plugins/ObtuseLootHabitatProbe/`. Require process exit 0, no `failure.txt`,
   `standalone.json`, `sensor-grid.json`, `unloaded-column.json` and sampling timings.
2. Remove `boot-only`, enable Double Jump in this disposable config and add the two
   official protocol adapters. Start the same server. After its startup marker, run
   `client.cjs <probe-data-directory>` with the pinned Node dependencies.
3. Require client/server exit 0, no server/client failure file and
   `client-results.json` with `passed: true`. Four native cases verify inherited
   impulse/recovery, actual measured headroom and captured context. Native loot
   controls cover low-only evidence, overlapping rooms and a raised birth-column
   ceiling. Observations must stay immutable after changing the roof.
4. Preserve `expected.yml`. Remove the two adapters, create `restart-only`, and
   restart the server. Require exit 0, no `failure.txt` and `restart.json` with
   `passed: true`: evidence, parental genes and actual newborn ancestry must persist.

Archive exact core/probe digests, frozen sources, dependency integrity lock, all
process exits/logs and every result. Retain failing baseline evidence separately;
never overwrite it with a passing result. Reset only synthetic fixture state when
repeating a failed fixture. The coarse descriptor and this protocol test do not
certify player usefulness, natural niches, optional integrations or anticheat behavior.
