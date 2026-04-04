# Telemetry ingestion contract (hardening pass)

This project treats telemetry as a typed ingestion pipeline, not best-effort logging.

## Ingestion boundaries

- `TelemetryEventFactory#create(...)` is the runtime emitter boundary for new events.
- `TelemetryAggregationService#record(...)` is the aggregation/archive boundary.
- `EcosystemHistoryArchive#append/read*` is the persisted event boundary.
- `RollupStateHydrator` and `TelemetryRollupSnapshotStore#readLatest()` are persisted rollup-state boundaries.

## Event invariants

- `EcosystemTelemetryEvent` requires:
  - `timestampMs > 0`
  - non-null `type`
  - `artifactSeed >= 0` (`0` is reserved for non-artifact events)
  - non-null `attributes`
- `TelemetryFieldContract.normalize(...)` defines the canonical per-event-type schema and required fields.
- `TelemetryFieldContract.validateEvent(...)` enforces that:
  - required fields are present for the event type
  - `schema_version` is present and non-blank

## Persistence invariants

- Archive appends validate each event and fail with batch index context for bad in-memory event input.
- Archive decoding validates each persisted line and includes line-number context for malformed persisted events.
- Snapshot read validates numeric property parsing and unknown event-type keys with explicit "bad persisted rollup state" errors.

## Failure-mode intent

- Bad emitter input should fail before aggregation.
- Bad persisted telemetry lines should fail with archive line numbers.
- Bad persisted rollup snapshot data should fail with property-level messages.
