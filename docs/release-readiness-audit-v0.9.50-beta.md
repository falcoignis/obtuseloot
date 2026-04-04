# Targeted Release-Readiness Audit — v0.9.50-beta

Date: 2026-04-04
Scope: Current repository state only.

## 1. Executive Verdict

**NOT READY / BLOCKED**

Primary release-candidate blockers are persistence/state-integrity risks (silent SQL persistence failure paths that can collapse into implicit state regeneration) plus operator-facing command permission/help mismatches that make normal discovery/support flows unreliable for non-op staff.

## 2. Release Blockers

1. **Silent persistence failure can become implicit state replacement**
   - SQL save/load errors are logged but generally not surfaced as hard runtime failures to the caller.
   - `JdbcPlayerStateStore.loadArtifact(...)` returns `null` on SQL exceptions; `ArtifactManager.getOrCreate(...)` interprets `null` as “no record” and generates a new artifact identity.
   - This can convert transient DB failures into irreversible progression/state replacement risk.

2. **Root command permission model blocks help/info discoverability for non-op roles**
   - `plugin.yml` sets command-level permission to `obtuseloot.command` (default `op`), while help/info nodes are documented as separately grantable (`obtuseloot.help`, `obtuseloot.info`).
   - In Bukkit command gating, command-level permission can prevent executor routing before subcommand checks, so non-op moderators/operators may never reach `/ol help` or `/ol info` despite those nodes.

3. **Reload path does not reload several documented config-driven systems**
   - `/ol reload` reloads config + runtime settings + name pools, but does not refresh ecosystem tuning profile, persistence backend, dashboard server state, or safety guard config.
   - Operators are told to tune config and reload, but a meaningful subset of advanced behavior stays stale until full restart.

## 3. High-Priority Non-Blockers

1. **Config advertises MySQL connection pool controls that are currently inert**
   - `mysql.connectionPool.*` is parsed into `PersistenceConfig` but providers use direct `DriverManager` connections with no pooling implementation.

2. **Dashboard web server bind model is high-risk if enabled**
   - Web server binds on `new InetSocketAddress(port)` (all interfaces), no auth, no TLS, no allowlist.
   - Docs warn to firewall it, but production-safe defaults/guidance should be stricter before RC.

3. **Startup/config failure messaging is too generic in key failure paths**
   - Some paths are actionable (`[Persistence] Startup aborted...`), but others are exception-driven (`paths.analyticsRoot` blank throws) without consistent operator remediation in logs/help text.

4. **Release note compatibility claim is broader than defended guarantees**
   - Release notes say no migration steps needed for `0.9.x` pre-beta builds, but migration coverage is asymmetrical (YAML versioned migrator + manual debug YAML->SQL tooling + SQL schema incremental DDL best-effort).
   - Claim should be narrowed to tested upgrade surfaces.

## 4. Medium / Low-Priority Polish Issues

1. `plugin.yml` usage surface is dense and partially duplicative (`inspect` repeated) and still reads debug-internal.
2. `/ol info` output is minimal and does not expose backend/fallback/dashboard status that operators actually need.
3. README command section points to `commands and permissions.md`, but the “at a glance” operator workflow still lacks a short first-hour triage flow (what to check after startup + where).
4. `plugin.yml` usage text lags some implemented ecosystem forms (e.g., environment details), increasing doc drift risk.

## 5. Config Clarity Recommendations

### Should fix before serious RC

- Add hard validation (fail fast with explicit error) for:
  - `storage.backend` enum + required mysql fields when backend=mysql.
  - telemetry intervals/batch sizes (>0).
  - safety thresholds/suppression factors bounded in `[0,1]` where applicable.
  - cache sizes/expiry values (`activeArtifactCacheMaxEntries > 0`, expiry > 0).
- Add a config “decision aid” block for storage mode:
  - small single-server (`yaml`), medium (`sqlite`), multi-node (`mysql`) + explicit failure mode notes.
- Clarify fallback risk in comments:
  - if fallback enabled, operators may split historical data across SQL and YAML stores and must run explicit migration/reconciliation.
- Mark `mysql.connectionPool.*` as not yet active or implement it.

### Can improve later

- Group advanced ecology + telemetry knobs under stronger “experimental/advanced” banner with recommended untouched baseline profile.
- Add examples for `paths.analyticsRoot` absolute vs relative behavior with expected resulting paths.

## 6. Persistence / Migration / State-Integrity Notes

- YAML path uses data-version migration with explicit future-version refusal, which is good baseline hardening.
- SQL schema manager uses add-column best effort and swallows SQL exceptions for add-column attempts; this is practical but can hide non-“already exists” DDL issues.
- YAML->SQL migration is operator-invoked via debug command, not startup-automatic; this is acceptable for beta but should be clearly represented in release notes.
- Current DB error handling strategy still biases toward continuity over integrity; for RC, prioritize integrity with clear degraded/readonly/fail-fast modes.

## 7. Operator Experience Notes

- Command help is comprehensive but not role-layered; operational commands are mixed with heavy debug and force-mutation controls.
- Startup/readiness feedback does not currently provide a single concise “runtime summary” command output (backend, fallback used, dashboard bind state, telemetry rehydration mode).
- Failure guidance is inconsistent: some commands report “check server logs” without direct next-step hints.

## 8. Command / Permission Surface Notes

- Permission taxonomy is broad and improved, but root command permission gating undermines fine-grained nodes.
- Debug/admin/operator concepts are still interleaved in one top-level help screen.
- Consider role-oriented help views:
  - `/ol help operator`
  - `/ol help admin`
  - `/ol help debug`
- Force mutation commands are useful for testing but should be clearly marked non-routine in user-facing help.

## 9. Packaging / Release Process Notes

- Versioning and build metadata are internally consistent at `0.9.50-beta`.
- Build/install instructions are clear and modernized (Java 21, tests-on-default, explicit fast profile).
- RC readiness gap is mostly release discipline around upgrade guarantees and operator runbooks (incident/recovery, persistence backend transitions, rollback expectations).

## 10. Recommended Next Action Plan

### Fix before serious release candidate

1. Persistence integrity mode:
   - Prevent silent SQL load failure from auto-generating replacement identities.
   - Add explicit degraded state / fail-fast behavior and operator-visible status.
2. Command permission correction:
   - Remove/relax root `obtuseloot.command` gating or align default/usage model so help/info are reliably reachable.
3. Reload behavior alignment:
   - Either fully reload all documented config-driven systems or clearly document restart-required sections and echo that in `/ol reload` output.
4. Config validation pass:
   - Add startup validation with clear remediation text for storage/telemetry/safety/runtime bounds.
5. Release note precision:
   - Narrow compatibility claims to explicitly tested upgrade paths and list unsupported assumptions.

### Can wait until after

1. Role-oriented help output and debug surface ergonomics.
2. Dashboard hardening options (bind-address config, optional auth reverse-proxy guidance template).
3. Additional operator playbooks (first-hour checks, persistence migration runbook, incident response quick guide).
