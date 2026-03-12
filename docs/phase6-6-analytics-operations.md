# Phase 6.6 Offline Analytics Operations

## Operational entry point
Use `obtuseloot.analytics.ecosystem.AnalyticsCliMain`.

Commands:
- `analyze --dataset <path> --output <path> ...`
- `run-spec --spec <job.properties>` (cron/script friendly)
- `decide --history <history.log> --recommendation-id <id> --decision <...>`
- `export-accepted --history <history.log> --output-dir <path>`

## Dataset contract
Analytics resolves datasets via `TelemetryDatasetContract`.

### Runtime layout (required)
- `telemetry/ecosystem-events.log` (archive codec)
- `telemetry/rollup-snapshot.properties` **or** `rollups/*.properties`

### Harness layout (required for harness mode)
- `telemetry/ecosystem-events.log`
- `telemetry/rollup-snapshot.properties` (or legacy `rollup-snapshot.properties`)

### Optional files
- `scenario-metadata.properties`

### Compatibility notes
- `telemetry-events.log` (record `toString`) is legacy-only and is **not** ingested by analytics.
- Harness Phase 6.6 now writes archive-compatible telemetry and rollup files under `telemetry/`.

## Job workflow and audit files
Each run writes:
- `<job>-analysis-report.txt`
- `recommendation-history.log`
- `<job>-job-record.properties`
- `<job>-run-metadata.properties`
- `<job>-output-manifest.properties`

Recommendation governance remains human-reviewed:
1. analysis creates `PROPOSED`
2. operator sets decision with `decide`
3. accepted recommendations are exported to runtime-loadable tuning profiles

## Scheduling / automation
Use `run-spec` with a properties file and invoke from cron.
Example:

```properties
job.id=daily-ecosystem
dataset.path=/srv/obtuseloot/worldlab-output
output.path=/srv/obtuseloot/analytics-runs/daily
bucket.type=DAILY
bucket.retention=14
bucket.span=1
bucket.rollingWindowSnapshots=0
recommendation.exportAccepted=false
```

Script wrapper:
- `scripts/run-ecosystem-analysis.sh <spec.properties>`
