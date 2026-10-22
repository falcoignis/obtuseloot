# Disposable durable-genome server test

This fixture is excluded from the production plugin and normal Maven tests. Use only
in a new disposable local Purpur 26.2 server. It creates synthetic artifact profiles,
changes their owner, and stops the server after each phase. Never install on an existing
server or world. It needs no players or optional plugins.

Build the plugin and dependency classpath as described in `../double-jump/README.md`.
Compile `GenomeProbe.java` against `target/classes` and that classpath using the official
Java 25 Maven container. Package its classes with this folder's `plugin.yml` in a separate
probe JAR. Put the core and probe JARs in an empty server's `plugins/` directory.

Use checksum-verified official Purpur 26.2 build 2633 and an official Java 25 runtime
container. Bind to loopback, disable query/RCON, and set an external timeout. Start the
same disposable server three times, letting the fixture stop it each time. The phases
commit path evolution, drift and awakening, check re-evolution after administrative
reseeding/recreation, restart, transfer ownership, and restart again. Repeated
profile/subscription refreshes must retain the exact stored genome and
revision. Keep all three exit codes/logs and the probe's `result.json`; `failure.txt`
or a missing phase/result is a failure. Remove the disposable server afterward.

This tests real standalone bootstrap and YAML storage across actual process restarts.
It uses controlled state transitions; it does not demonstrate natural selection, test
native player input, or certify optional integrations. Movement behavior remains covered
by the separate Double Jump fixtures.
