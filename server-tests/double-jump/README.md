# Disposable movement server test

These fixtures are excluded from the production plugin and normal Maven tests.
They are for a fresh, explicitly authorized local test server only. `Probe` creates
test gear and a platform, grants its named test player OP, and shuts down the test
server at completion or deadline. Never install it on an existing game world.

The run uses the official Purpur 26.2 build 2633 with Java 25, plus the plugin built
from the recorded tested commit. The protocol client is 26.1 because the reviewed
`minecraft-protocol` release does not support 26.2. Official ViaVersion and
ViaBackwards 5.12.1 translate that connection. Their presence is a test dependency,
not a runtime requirement of ObtuseLoot. All Java and client execution uses Docker.

Preparation in an empty workspace:

1. Download Purpur build 2633 and compare with its published digest from
   `https://api.purpurmc.org/v2/purpur/26.2/2633`. At review this was MD5, not a
   published SHA-256. Record a local SHA-256 separately for the audit trail.
2. Build the production plugin with `maven:3.9-eclipse-temurin-25`. Run Maven
   `dependency:build-classpath -Dmdep.outputFile=target/server-test-classpath.txt`.
   In that container, compile `Probe.java` with `javac -cp
   "target/classes:$(cat target/server-test-classpath.txt)" -d target/server-probe`.
   Package those classes and this folder's `plugin.yml` as a separate probe JAR.
3. Put both JARs in the empty server's `plugins/`. Create the probe data directory
   `plugins/ObtuseLootMovementProbe/` and a file named `boot-only` inside it.
4. Bind `server-ip=127.0.0.1`, `server-port=25598`; disable RCON/query and secure-profile
   enforcement for the offline test client. Set offline mode only in this isolated
   server. Use a flat world with explicit bedrock/dirt/grass layers and spawn protection
   zero. `boot-only` verifies the core enables with every supported optional integration
   absent, writes `boot-result.json`, then stops the disposable server.
5. After that run exits, remove `boot-only`, enable `abilities.double-jump.enabled`
   in this server's plugin config and install checksum-verified official ViaVersion /
   ViaBackwards release JARs. Start the same disposable server for the movement run.
6. Install this folder's exact client dependencies in a temporary client directory
   using `node:26` and `npm install --ignore-scripts --no-audit --no-fund`. Preserve its
   generated integrity lock with the results. Run `client.cjs <probe-data-directory>`
   in that Node container, mounting that directory for the request/result handshake.
   With host networking, the server still binds only loopback. Keep a finite server
   deadline (the reviewed run used `timeout --signal=TERM 240s java ...`).

The client sends native input and movement packets at 20 Hz, integrates the received
velocity, and asserts baseline versus extra-jump height, one impulse/outcome across two
copies and repeated input, effective permission denial even for OP, the custom protection
veto, ordinary velocity-event cancellation, deferred gear removal, foreign flight ownership
and elytra input reservation. The heritable-performance follow-up adds low/high kinetic
fixtures, verifies their proposed and persisted recovery costs, and retries after landing
before/after the long recovery deadline. Two real low-ceiling cases compare the same
headroom with different recovery. The client waits for the ordinary head collision to
release before pressing again. Native velocity veto checks allow Paper retry callbacks
while requiring one artifact proposal and zero received impulses/successes. Baseline fixtures explicitly express the midpoint
gene. These are controlled synthetic genomes, not a natural-selection demonstration. Results are `client-results.json`; any assertion writes
`client-failure.txt`. The probe writes `failure.txt` on server-side failure. Archive
those with the complete logs, exit codes, exact artifact digests and dependency lock.

This is a real server/protocol test with controlled client physics and test protection
listeners. It does not certify a graphical 26.2 client, AuraSkills, claim plugins or an
external anticheat. Keep the feature opt-in until the intended plugin combination is tested.
