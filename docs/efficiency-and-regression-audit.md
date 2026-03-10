# Efficiency and Regression Audit (Pass 2)

## 1. Efficiency findings
- **Potential repeated heavy generation at startup:** plugin startup initializes dashboard regeneration and may write analytics immediately; acceptable for admin environments but worth monitoring on constrained hosts.
- **Scheduled periodic analytics writes:** periodic environmental pressure report regeneration is scheduled; frequency appears moderate but still incurs recurring file I/O.
- **Simulation/report generation complexity:** world-lab/open-endedness and meta-analysis scripts perform large in-memory map/list computations and JSON/markdown writes; this is expected for offline analytics workloads.
- **No obvious pathological hot-loop inefficiency** found in inspected runtime command/combat wiring; configuration-driven thresholds are read through in-memory `RuntimeSettings` snapshots rather than repeated config file reads.

## 2. Confirmed regressions
1. **CI nightly artifact regression (fixed):** nightly workflow uploaded a version-pinned JAR filename (`target/ObtuseLoot-0.9.5.jar`) inconsistent with current versioning, causing missing artifact risk after version increments.

## 3. Likely regressions
- **Command documentation regression:** implemented command branches include `ecosystem environment`, but `plugin.yml` usage text does not list it, likely causing operator discoverability regressions.
- **Build fallback regression:** `scripts/build.sh` references `scripts/mvn-via-mirror.sh` which is absent; mirror fallback path likely regressed or was never completed.

## 4. Configurability findings
- Runtime configurability is generally healthy; core knobs in `config.yml` map through `RuntimeSettings` and are consumed in relevant systems.
- Dashboard web server toggles (`dashboard.webServerEnabled`, `dashboard.port`) are read directly in plugin bootstrap.
- Persistence backend selectivity appears wired via `storage.backend` + provider factories.
- No confirmed case found where a declared runtime setting is silently ignored.

## 5. Small safe fixes applied
1. Replaced hardcoded nightly artifact filename with wildcard in `.github/workflows/nightly-build.yml` to avoid future version-coupled upload failures.

## 6. Remaining recommended fixes
1. Synchronize `plugin.yml` command usage block with actual command handlers (`ecosystem environment`, and any newly added debug/dashboard aliases).
2. Either add `scripts/mvn-via-mirror.sh` or remove the fallback reference from `scripts/build.sh` to prevent operator confusion.
3. Consider documenting which analytics files are authoritative vs historical snapshots to reduce stale-report misuse.
4. Add lightweight CI check to fail when workflow artifact paths reference stale explicit versions.

## 7. Risk assessment
- **Changes made risk:** low (workflow path wildcard only).
- **Unfixed issue risk:** medium for CI/ops clarity (documentation and fallback script gaps), low-to-medium for gameplay runtime stability.
- **Performance risk posture:** moderate in analytics/simulation runs (expected heavy workloads), low in normal gameplay runtime based on inspected paths.
