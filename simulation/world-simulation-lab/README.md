# World Simulation Lab

Server-scale seasonal simulation harness for long-horizon ecosystem validation.

## Run

```bash
mvn -q -DskipTests compile
mvn -q -DskipTests -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner -Dexec.classpathScope=compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

## Configuration defaults

`WorldSimulationConfig.defaults()` provides:

- `player_count=120`
- `artifacts_per_player=4`
- `sessions_per_season=18`
- `season_count=6`
- `boss_frequency=0.18`
- `encounter_density=7`
- `chaos_event_rate=0.20`
- `low_health_event_rate=0.15`
- `mutation_pressure_multiplier=1.0`
- `memory_event_multiplier=1.0`

## Outputs

- `analytics/world-lab/world-sim-report.md`
- `analytics/world-lab/world-sim-data.json`
- `analytics/world-lab/world-sim-balance-findings.md`
- `analytics/world-lab/world-sim-meta-shifts.md`
