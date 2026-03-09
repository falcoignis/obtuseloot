# QA Notes — v0.9.6

## Build and packaging checks
1. Build on Java 21 with Maven (`mvn -B -ntp clean package`).
2. Confirm artifact name is `target/ObtuseLoot-0.9.6.jar`.
3. Confirm release workflow can produce/upload release assets from `target/*.jar`.

## Runtime smoke checks
- Start on a Bukkit/Purpur-compatible server and verify plugin load and enable.
- Confirm `plugin.yml` version alignment with Maven (`0.9.6`).
- Verify player state persistence lifecycle (join-load, autosave, quit-save, disable-save).

## Progression regression focus
- Validate evolution pathing from effective weighted reputation.
- Validate drift mutation events and instability expiry behavior.
- Validate awakening and fusion transitions still gate correctly.

## Debug and simulation spot checks
- Run representative `/obtuseloot debug` flows for inspect, simulate, evolve, drift, awaken, and fuse.
- Review analytics outputs under `analytics/world-lab/` and `analytics/meta/` for obvious regressions after simulation runs.
