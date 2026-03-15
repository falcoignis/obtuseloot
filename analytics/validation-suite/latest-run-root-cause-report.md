# Latest Run Pointer Root Cause Report

## Pointer
- run_id: `constrained-five-scenario-final`
- dataset_root: `/workspace/ObtuseLoot/analytics/validation-suite-rerun/constrained-five-scenario-final/runs`

## Findings
- `latest-run.properties` currently points at an absolute path rooted at `/workspace/ObtuseLoot/...`.
- The rerun harness constructs `RUN_ROOT="$BASE_ROOT/$RUN_ID"`, sets `OUTPUT_ROOT="$RUN_ROOT/runs"`, and writes `dataset_root` from `realpath "$OUTPUT_ROOT"`.
- The harness writes `latest-run.properties` only after all scenario outputs pass strict required-artifact checks, so pointer updates are gated on successful output presence.
- In this checkout, `/workspace/ObtuseLoot/...` does not exist, and the run directory `analytics/validation-suite-rerun/constrained-five-scenario-final/` is also absent.
- The only directories in-repo that contain >=3 scenario names are analysis/report roots, not harness output roots.

## Classification
- **E (Other): stale pointer from another filesystem/checkout**.
  - The pointer value is not produced by current-path execution in this workspace and references a missing absolute root.
