# ObtuseLoot project status (volatile; dated entries)

## 2026-10-21: authorized stack merge closeout
- The owner explicitly authorized merging the completed #408–#412 stack after the score-normalization results. This supersedes the historical lack of authorization recorded below for these follow-ups only; future merges still require owner authorization.
- #408, #409, #410 and #411 merged into `main`, in dependency order, with merge commits `5293f68465097a30e7160dc22a93e337f3dee962`, `19a1cb8084b20450da098d2b4e6da79d98a45499`, `d768825384bf21cd2f972329b3d8113d329aa5e7` and `551ed4e50cbbedb904b54cc8d219f39e53411fd3`. Their exact tested heads had successful CI before merging.
- #412 was retargeted to `main`; its tested head `998ac7e31f26c92601dcc0d1aa11fedeb6dd0559` also passed CI. This documentation closeout changes no executable inputs from that head, whose full Docker suite passed all 511 tests. Refresh GitHub for #412's final merge state and any subsequent CI result.
- No release or live deployment is authorized. Wild mutation remains held; the retained soft cap is the next separate ablation candidate.

## 2026-10-21: gamma/mean score-normalization ablation (tested, prepared for review)
- Owner approved this next ablation, keeping the soft cap for its own later comparison. Fresh clone of PR #411 at `6e886ec742c697af34d46b802c79095ff659014c`, branch `refactor/ablate-score-normalization`. #411 remains open at that same head with CI passing, verified before publication. No merge authorization for this follow-up.
- Removes the gamma curve and mean blending from within-category score preparation. A saved comparison verifies the soft-cap body and 1.28 constant against the prerequisite. Original-score mean and empty/singleton/uniform/nonfinite guards remain. The previous transform is restored only in a test control using the same production cap.
- The regression first failed as intended: score 1.0 was inflated to 2.05. Focused Docker test run: **43 tests across three classes, zero failures/errors/skips, Maven exit 0, 5:22**. All 514 source/resource/pom hashes matched the frozen snapshot afterward. Final full Docker `clean package`: **511 tests across 99 classes, zero failures/errors/skips, Maven exit 0, 29:00**. All 514 frozen hashes still matched; Java 25/API 26.2 packaging, retention of the soft cap and exclusion of both legacy test controls/optional APIs were verified. Generated tracked reports were restored.
- Focused/full paired outputs match exactly, including every occupancy window. Paired health: **8.02%** maximum relative diversity loss with legacy normalization, **0.84%** without it, both passing the existing 25% gate. The control changed scores at 960 preparation points and changed 364 generated profiles. Independently replayed occupancy windows and ordered ability/trigger/mechanic traces matched exactly. The earlier top-three ablation also passed in this new pipeline: 4.69% legacy / 0.84% ablated, with 354 changed weight arrays and 86 changed immediate choices. These are one-seed observations, not proof of general improvement or full emergence.
- Multiple-entry categories are affected, including mobility when Fleetfoot and opt-in Double Jump are both eligible. Generic catalog/slot/permission paths and effect execution remain; no ability-specific scoring rule, XP API, storage format, effect-strength change, server start or deployment is added. Wild mutation stays held. See `docs/score-normalization-ablation.md`; the full suite also reruns the earlier top-three ablation in this new surrounding pipeline.

## 2026-10-21: first selection-scaffolding ablation (tested, prepared for review)
- Fresh clone of Double Jump prerequisite #410 at `26c2f9816e1e1324ef3d176bcba8eead8974dd5e`, branch `refactor/ablate-top-three-compression`. #408, #409 and #410 were open; #410 CI passed when checked. This follow-up has no merge authorization.
- Removes the final top-three weight compressor and its two distribution targets. The ordinary sampler preserves supplied weights. Other aids remain; there is no live toggle restoring this layer. An exact legacy-policy test control exercises the same sampling boundary using the original single RNG draw, independent fixture state and the existing relative health settings.
- Regression first failed against old behavior (expected first candidate, got third). Initial focused pass: **38 tests across four classes**, Maven exit 0. Finalized control/current inheritance checks: **10 tests across two classes**, Maven exit 0. The first focused filter named an obsolete inheritance class; the second run used the verified current `LocalInheritancePoolTest`. Final full `clean package`: **507 tests across 98 classes, zero failures/errors/skips, Maven exit 0, 26:50**. All 513 source/resource/pom hashes matched the frozen manifest afterward. Java 25/API 26.2 packaging, exclusion of optional APIs/test controls and removal of the old compressor bytecode were verified. Generated tracked reports were restored.
- Focused/full paired outputs match exactly: legacy maximum relative loss **9.82%**, ablated **8.02%**, both under the existing 25% gate. The control altered weights at 141 sampling points and changed nine immediate choices. Treatment replay matched exactly. The final control preserves the former sequential summation order and shares health settings with the existing fixture.
- The current live pool has at most two implemented entries per category, so this layer was dormant there. This affects full-catalog simulations and future larger categories, not current live behavior. No engine-weight tuning, runtime integration/effect/permission/storage changes, server start or live deployment. This one-seed simulation is not proof of full emergence; wild mutation stays held. See `docs/top-three-compression-ablation.md`.

## 2026-10-21: opt-in native Double Jump (tested, prepared for review)
- Fresh clone of passing 26.2 prerequisite PR #409 at `e65e311e18624998d57614383fb12cac4ab3c87b`, branch `feat/native-double-jump`. #408 and #409 were open with passing CI when checked. No merge authorization for this follow-up.
- Adds one catalog-owned jump reaction, captured six-slot eligibility/effective permissions, per-player charge and game-tick cooldown, a protection veto, ordinary velocity/movement denial observation, and generic deferred measured feedback. It grants no flight, resets no fall distance and writes no XP/mana/stats. Wearing elytra reserves its jump input. Default disabled; .42 strength/60-tick cooldown are configurable implementation defaults.
- Review demonstrated a pending-history defect with a RED regression. The correction introduces generic `PENDING` status and records only the completed motion outcome. Final focused: 58 tests across six classes, zero failures/errors/skips, Maven exit 0. Final full `clean package`: **504 tests across 97 classes, zero failures/errors/skips, Maven exit 0, 20:29**. All 512 source/resource/build-file hashes and five server fixture hashes matched their tested manifests. All 556 production class/resource entries matched the server-tested build; only ZIP timestamps differ. Java 25/API 26.2 metadata, soft dependencies only and exclusion of optional APIs/probe classes were verified.
- Owner chose a disposable local Purpur 26.2 server. Java and protocol-client execution both use official Docker images. Corrected-build standalone boot passed without optional integrations. All eight protocol cases passed on Purpur build 2633 with a 26.1 client via checksum-verified ViaVersion/ViaBackwards 5.12.1 (client library lacks 26.2 support). One actual impulse, one completed attempt and one success across two copies/repeated input; denial, pre-event veto, native-velocity veto, deferred swap, foreign flight and elytra reservation produced no success. No production deployment.
- Native graphical 26.2, AuraSkills XP, claim-plugin and external-anticheat combinations remain unverified. The existing missing-dashboard-data startup warning is recorded, not hidden. See `docs/double-jump.md` and the excluded `server-tests/double-jump/` fixtures.

## 2026-10-21: Minecraft 26.2 target migration (tested, prepared for review)
- Owner selected Minecraft 26.2. Fresh clone of equipment PR #408 head `f2c56f8af50f347b59d49388909cfd9aa20cbe07`; #408 was open with CI passing when checked. This is a stacked follow-up, with no merge authorization.
- Pins stable Purpur API `26.2.build.2633-stable`, Java 25 compiler/toolchain, all seven Java CI jobs and plugin metadata. Updates current README and canonical guidance. Corrects the removed speed attribute name and adapts the test fixture for server-backed attribute registries.
- Final focused pass: 47 tests in five classes, zero failures/errors/skips, Maven exit 0. Full `clean package`: **493 tests in 96 classes, zero failures/errors/skips, Maven exit 0, 19:36**. All 505 source/resource/build-file hashes matched the frozen manifest. The JAR contains Java 25 bytecode, API 26.2 metadata and only soft plugin dependencies; optional API classes are not bundled. Generated tracked reports were restored. Both the official API and reviewed source JAR matched published SHA-256 digests.
- Double jump remains next; the exact 26.2 API includes native jump input. No PacketEvents dependency, skill XP hook, server deployment or runtime configuration change is included. See `docs/minecraft-26.2-target.md`.

## 2026-10-21: equipment/passive and optional integration follow-up (tested, prepared for review)
- Fresh clone from merged #407 at `4ea24fc4c2e72d8b007f69406c7425c6cee423d9`; its main CI subsequently passed. Branch `feat/equipment-ability-activation`. This batch has no merge authorization; #407's authorization ended with that completed batch.
- Implements six equipped slots (elytra shares chest), per-item subscriptions/evolution windows, captured deferred input identity, duplicate-hand callback handling, passive leases, shared reconciliation and eligible per-item credit. Existing combat/admin targeting remains main-hand based.
- Adds Fleetfoot, default +5%, strongest contributor by default, with owner-requested additive mode. Other movement/modifier owners remain untouched. No idle tick fitness/XP. Double jump remains the next separate slice.
- PlaceholderAPI expansion reads immutable slot snapshots. Effective server permissions support LuckPerms allow/deny/context changes without OP bypass. PAPI/LuckPerms/AuraSkills are soft dependencies; deliberate absent-API class-loader tests exercise standalone integration initialization and optional failure isolation.
- AuraSkills boot detection/coexistence baseline only: no XP listeners/awards, stat writes, mana changes or tracking removal. Official skill/API/source review records timing, right-click, item requirements, movement and crop XP/replant caveats in `docs/plugin-compatibility.md`. Normal AuraSkills XP must be preserved; extra XP coupling is unresolved.
- Final focused pass: **72 tests, 11 classes, zero failures/errors/skips**, Maven exit 0. The initial full run found a stale catalog inventory assertion (88 rather than 89). That check and the demonstrated disabled-index dispatch defect were corrected. Final full `clean package`: **493 tests across 96 classes, zero failures/errors/skips, Maven exit 0, 29:46**. All 505 source/resource/build-file hashes matched the verified manifest. The packaged JAR has only soft plugin dependencies and does not bundle optional API classes. Generated tracked reports were restored. No source weights or distribution targets tuned. No game server started/deployed; actual plugin compatibility tests remain pending.

## 2026-10-21: tested local inheritance foundation and equipment design
- Timing prerequisite #406 merged on 2026-10-21 at `daa58752ac1d2c870cc3981e8ae5ce565cdc1eae` after its passing CI. The inheritance follow-up is prepared for publication against `main`; refresh GitHub for its subsequent publication/merge state.
- Branch `feat/outcome-driven-ecosystem`, based on timing PR #406 source `87848ef9c1b2e306a67541249c7781b538491bf2`. At the start of this work #406 was open with CI passing. Refresh merge state before publishing/merging this follow-up. The owner subsequently authorized merging this completed/tested batch and its prerequisite on 2026-10-21; this is not standing merge permission.
- Owner emphasized a true functioning ecosystem as the main selling point, approved new loot inheriting from successful local lineages with variation, then specified **20 chunks** and **chunk-based configuration**. The implementation uses a same-world horizontal radius around actual loot generation. The 6000-game-tick recent-evidence window is an implementation default.
- Adds per-item profiles/genomes/reputation/ancestry, holder-safe activation/trades, and local mean-outcome-fitness parent sampling without whole-map fallback or niche-control bonuses. A memory cache bound does not delete durable items or limit the population. SQL/YAML share the artifact codec; the evidence clock/state persists and corrupt state is refused.
- Final full `clean package`: **469 tests across 91 classes, 0 failures, 0 errors, 0 skips**, Maven exit 0, **23:07**. All 487 source/resource/build-file hashes matched the tested manifest afterward. Focused checks: **91 passing**. Tests include causal outcome swaps, real loot events with API doubles, item conservation, held-item boss/death attribution, local range boundaries, persistence, corruption and save/load/cache failure. Tracked generated reports were restored.
- This is a reproduction foundation, **not proof of complete emergence**. Evidence expiry is reproductive inactivity, not lineage extinction. Heritable effect behavior is an unanswered owner question; current effects stay fixed. Dynamic niches, true lineage turnover, pressure replacement and scaffolding ablation remain. See `docs/local-artifact-inheritance.md`.
- Owner added passive abilities, off-hand/secondary click, armor and elytra requirements, with speed increases/double jump as examples. PlaceholderAPI and LuckPerms must be supported; PacketEvents is conditional on need. The next implementation design and acceptance checks are in `docs/equipment-ability-activation.md`. These features/integrations are not implemented in this batch.
- No server started or deployed. Current activation uses the main hand; worn/offhand parallel activation remains future work. Wild mutation stays held, and progression/awakening/convergence rules are unchanged.

## 2026-10-20: tested noncombat timing follow-up
- Branch `feat/noncombat-evolution-timing` builds on main `08738a365fd3ab8a31f3a3c94d3728b11e6db9d2`. Prerequisites **#403, #404 and #405 are merged** (verified through GitHub). Canonical skill retrieval is from `main`; the migration branch exception is finished. Older entries below describe historical states.
- Owner requested evolution timing and easy ability add/remove. Measured outcomes now earn existing-family reputation with durable fractional credit/debt and trigger ordinary stage evaluation plus ability refresh on configurable game-tick windows (default 100). The exact cadence/credit defaults were proposed implementation choices, not separately confirmed owner values. Idle windows do not reroll. Quit/shutdown flush stage evaluation before saves, and stale identities are rejected.
- One catalog registration binds metadata to an optional live handler. Live selection uses implemented entries; offline tooling keeps the full catalog. Removing an entry removes execution and selection together; code changes require restart. Unsupported variants retain executable originals with a recorded fallback. Actual outcome keys distinguish inspected blocks/states and paid harvest ticks; repeated unchanged reports retain existing scoring penalties.
- Existing stage thresholds, awakening/convergence gates, designed drift chance and scoring weights remain unchanged. Wild mutation stays held. Minimal combat/social-interaction-exploration priority remains in force.
- Full local `clean package`: **445 tests across 86 classes, 0 failures, 0 errors, 0 skips**, Maven exit 0, **21:45**. Focused verification: **60 passing**. The existing seeded ecosystem health test passed. Tracked test-generated reports were restored. See `docs/noncombat-evolution-timing.md` for lifecycle, persistence, catalog instructions and limitations.
- No live server or deployment. Fresh SQL schema required (no player-data migration by owner decision). Actual report usefulness, broad live diversity and comprehensive anti-farming behavior are unproven. This new work is outside the previous batch's merge authorization; owner review/merge is required unless separately authorized.

## 2026-10-20: first measured interaction effects
- Branch `feat/measured-ability-effects` builds on the baseline and canonical-skill PRs (#403 and #404). Refresh GitHub publication/merge state before acting. The owner explicitly authorized merging this current batch after completion and tests; this is not standing permission for future work.
- Gentle Harvest replants one mature crop with one existing matching drop, honoring drop cancellation, placement-plugin denial, soil and conservative spawn checks. Material Insight delivers material/light/growth information for the inspected block. Report delivery is measured; player benefit is not established.
- Live unimplemented effects and remapped mechanic/trigger variants return no-op. Offline classification remains explicit in telemetry. Utility-history snapshots and niche observations refresh immediately after outcomes instead of waiting for autosave/cache expiry.
- Full local `clean package`: **423 tests across 82 classes, 0 failures, 0 errors, 0 skips**, Maven exit 0, **21:32**. Focused checks: **35 passing**. Seeded health maximum relative diversity loss remains **2.12%**. No weights, distribution targets or progression gates changed.
- **Owner focus:** minimal combat effects; prioritize social, interaction and exploration. **Next: noncombat evolution timing**, deferred from this slice at the owner's request. Wild mutation stays held.
- No server started or deployed. Tests use stateful API doubles and real Bukkit event classes; actual server/protection-plugin validation is pending the owner's chosen destination. See repository `docs/measured-ability-effects.md`.

## 2026-10-20: canonical skill moves into this repository
- Owner requested the skill be copied into the repository and made canonical, with Claude informed. The source is now `skills/obtuseloot/`; root `AGENTS.md` and `CLAUDE.md` route here. Installed copies and external mirrors are noncanonical.
- Private workspace paths and host-access details were removed from the published copy; the original skill is preserved in a verified private backup. Historical evidence and owner decisions remain. This migration changes guidance only, not plugin behavior. Until the migration PR is merged, retrieve the canonical folder from branch `docs/canonical-obtuseloot-skill`; do not replace it from a `main` checkout that does not contain it yet.

## 2026-10-20: follow-up PR #403 OPEN (Codex continuation)
- **https://github.com/falcoignis/obtuseloot/pull/403**, branch `fix/drop-probe-distribution-targets`, tip `0237f1d558e165a2a5853ea5a4a0b892d43ea818`. Three commits on main `b00b412` (PR #402 merged). No duplicate seeded-owner cherry-pick. Owner reviews and merges; never merge automatically.
- Fixed JVM-dependent genome iteration with immutable enum-ordered snapshots. Also isolated probe population histories through the existing generator constructor: the original cold-bootstrap focused/full context difference disappeared. Final normalization and cold-bootstrap output match exactly across separate focused/full JVM runs. A regression checks fixture independence and absence of writes to the global population; mutation also matches across six separate JVMs.
- Shared occupancy metrics are used by scheduled rollups and the health gauge: bounded Gini-Simpson diversity, Shannon effective occupied niches, adjacent-window niche births/extinctions, and population-share change. Cumulative branch counters remain separate. Last-known telemetry occupancy and exact identity hydration remain outside this change.
- Added a fixed-seed real-generator/utility-feedback health scenario: 32 artifacts, 12 windows, first 3 baseline, 25% maximum relative diversity loss. Actual maximum loss **2.12%**; injected monoculture/empty controls fail. Outcomes are simulated; no Bukkit world effects or wall-clock bifurcation are exercised. No engine weights were tuned.
- Final `clean package`: **395 tests, 0 failures, 0 errors, 0 skipped**, Maven exit 0; 7 isolated focused tests passed. PR is OPEN/MERGEABLE; GitHub build subsequently completed successfully (verified 2026-10-20); PR remains open. Refresh PR state before acting.
- **Wild mutation is deferred from this PR**, per owner: "Hold wild mutation; do the test first". Chance and magnitude remain undecided.
- Verified backup, diff, review JAR and evidence: the owner's private review archive, with `README.md` and `SHA256SUMS`. Final backup `branch-0237f1d.bundle`; earlier `branch.bundle` is retained as a backup. Resume from a fresh GitHub clone/branch; the temporary task checkout and build output were removed. Existing `obtuseloot-m2` cache retained. No live deployment.


Point-in-time state. Re-verify before acting: `gh` for repo, CI and release facts, and a fresh clone for code facts. Add new dated entries at the top and keep the older ones as history.

## 2026-10-19 evening: PR #400 MERGED, main red, follow-up held
- The owner merged #400 at 18:18 UTC, before its CI finished. Main CI run 39301710859 failed on two flaky distribution asserts (cold-bootstrap Ritual reach 10/12; sensing top-3).
- `4088ada` (seed-derived owners) was pushed to the merged branch after the merge, so it is NOT on main. **Always check `gh pr view` state before pushing to a PR branch.**
- A new local branch `fix/drop-probe-distribution-targets` exists in the scratchpad clone (historical scratch checkout): `45fd158` (cherry-pick of 4088ada) and `c35254e` (drops all probe distribution/JSD targets, deletes LongHorizonSimulationTest). **HELD, not pushed:** the owner wants an ecosystem health test first. Order: #401 nondeterminism, then metrics, then the relative health test, then push. Backed up as a verified git bundle at a private archive named `fix-drop-probe-distribution-targets-c35254e.bundle` (sha256 51305ebd...). Restore with `git fetch <bundle> fix/drop-probe-distribution-targets:fix/drop-probe-distribution-targets` in a fresh clone.
- Issue #401 has a CI-evidence comment: Ritual was exactly 10/12 on both CI runs and 12/12 locally, which suggests an environment-dependent input (unverified).
- The old baseline checkout was clean and fully pushed; its machine-specific cleanup record is kept outside this repository.

## 2026-10-19 13:55 EDT: PR #400 open
- **PR https://github.com/falcoignis/obtuseloot/pull/400** (`fix/restore-green-baseline` -> `main`, 5 commits, pushed at owner's "Push"). The owner reviews and merges.
- Local full suite: 390/0/0. PR CI run 39300910009 **failed** on `ColdTemplateBootstrappingProbeTest` (Ritual reached 10 of 12; it passed locally twice).
- Fix commit `4088ada` was pushed to the PR (6 commits now). Probe helpers derive owners from seeds, and the cold-bootstrap reach, named-template and top-share targets were dropped. Probes are **still not repeatable**: Stealth and Defense counts differ across identical runs, source not found. Tracked in **issue #401** (https://github.com/falcoignis/obtuseloot/issues/401).
- After it merges, the next roadmap item is **2: real ability effects with outcome feedback**. Use fresh clones per task after preserving interrupted work.

## 2026-10-19 (late): baseline branch progress
- Commit `5e38780` (local only, on fix/restore-green-baseline): signed seeds and analytics fixtures. 24 targeted tests green.
- **Uncommitted, untested in the full suite:** storage-failure fix.
  - New `PlayerStateLoadException`/`PlayerStateSaveException`/`PlayerLoadGuard` (30 s per-player backoff).
  - JDBC and YAML stores throw instead of returning null. YAML parses strictly and retries failed async writes.
  - `ArtifactManager` and `ReputationManager` have `tryGet`, recovery listeners, and saves that keep data dirty on failure. Cache entries stay dirty and unevictable after a failed save.
  - Hot paths use `tryGet`. Commands catch at `DashboardCommandExecutor.onCommand`. Join wiring moved to `PlayerJoinLoadListener.completeLoad`, re-run on recovery.
  - `storage.fallbackToYamlOnFailure` removed (code, config.yml, README).
  - New tests: ArtifactManagerLoadFailureTest, ReputationManagerLoadFailureTest, PlayerStateStoreFailureTest. 18 targeted tests green.
- **Still to do on this branch:**
  1. Commit the storage fix.
  2. Remove the sensing `>= 18` assertion.
  3. Permission root-gate fix.
  4. Full suite (revert `analytics/` and the doc afterward).
  5. Show the owner the diff, then push and open the PR.
- **Update:** all 5 commits are in (`5e38780`, `69f0058` .gitignore anchor, `add0471` storage, `76b3f18` sensing assertion, `6a906b6` permissions). Full suite: **390 tests, 0 failures, 0 errors** (surefire reports). The first full run failed only at jar packaging, because Maven ran offline (`-o`) before the jar plugin was ever cached; an online `-DskipTests package` then succeeded. Lesson: don't use `-o` until a full online `package` has run once. Awaiting the owner's go to push and open the PR (draft body in the session scratchpad).
- The unanchored `artifacts/` rule in `.gitignore` silently ignored new files under `src/*/java/obtuseloot/artifacts/`. It is fixed on this branch; until it merges, `git add -f` any new file there on other branches.
- Found, not fixed: with `runtime.activeArtifactCache=false`, `ArtifactManager.unload` never saves, so a quit loses changes made since the last autosave.

## 2026-10-19

**Repo**
- `falcoignis/obtuseloot` is **PUBLIC**. Default branch is `main`, at `fab9f2b` ("Merge PR #399"), version `0.9.50-beta`.
- There are about 304 remote branches: about 247 `codex/<task>`, about 49 `claude/<task>-<5char>`, plus a few others. Ten are not merged into main. The decision is to review them once, then delete.
- Tracked generated `analytics/` is about 299 MiB across 943 files.

**CI**
- Main has been **red since PR #398** (2026-04-04, "Harden telemetry ingestion contracts"). The last green run was PR #397.
- Nightly Build has failed every run since then. GitHub shows it as `disabled_inactivity`, and the decision is to leave it disabled.

**Release**
- GitHub release `0.9.50-beta` is marked "Latest" with **zero assets**. Its tag has no `v`, while `release.yml` triggers only on `v*`.
- The tag points at `4c26d9c` (2026-03-24), which is 46 commits behind HEAD. HEAD still says `0.9.50-beta`.
- `RELEASE_NOTES.md` (edited 2026-03-26) claims loot stamping and the artifact command suite, but neither exists at the tag.

**Baseline build**
- Fresh clone `fab9f2b`, Docker `maven:3.9-eclipse-temurin-21`, 2026-10-19: 379 tests, 0 failures, **8 errors**, BUILD FAILURE.
  - 4 errors come from `WorldSimulationHarnessValidationProfileTest`: the telemetry layer rejects negative seeds (`Telemetry artifactSeed must be >= 0`).
  - 4 errors come from `AnalyticsOperationalCliTest` and `EcosystemAnalyticsRunnerTest`: their `ABILITY_EXECUTION` fixtures lack required fields. The field named in the error varies between runs, because the required set is an unordered `Set.copyOf`.
- `IntraCategoryNormalizationProbeTest` **passed** (sensing hits=27). On 2026-10-08 it failed (16 < 18) with the same code, so it is nondeterministic.
- `LongHorizonSimulationTest` alone took about 410 s. The full suite took more than 10 minutes.
- The test run **rewrites tracked `docs/deep-cold-completion-validation-final.md`**. Revert it after every run.

**Paused work: fix/restore-green-baseline**
- This branch is **uncommitted and untested**, in a historical local baseline checkout, and exists only on this machine. The owner said to keep it local until the work resumes.
- Changes so far:
  - Signed seeds. The `< 0` throws were removed in `TelemetryEventFactory` and `EcosystemTelemetryEvent`. `> 0` became `!= 0` in the factory (`artifact_seed`/`artifact_id`) and in `TelemetryAggregationBuffer` (2 sites). `<= 0` became `== 0` in `LineageRegistry`. `docs/telemetry-ingestion-contract.md` was updated.
  - Analytics fixtures were routed through `TelemetryFieldContract.normalize` with the real required fields (helper `abilityEvent` in both test classes).
  - Added `EcosystemTelemetryPipelineTest.signedArtifactSeedsRoundTripThroughArchiveAndAggregation`, covering `Long.MIN_VALUE`, `-1` and `Long.MAX_VALUE`.
- Next on that branch:
  - run the targeted tests, then the full suite;
  - ask the owner about the sensing `>= 18` floor;
  - add the storage-load-failure fix (fatal at startup, per-player refusal plus retry at runtime) and the permission root-gate fix (grouped PR);
  - show the diff, push, open the PR.
- After recovery, use fresh GitHub clones and remove only verified-disposable local workspaces.
- The Docker volume `obtuseloot-m2` holds a warm Maven cache from the baseline run. Remove it (`docker volume rm obtuseloot-m2`) once it is no longer wanted.

**Deployment**
- Not deployed anywhere. It is planned for HomesteadCraft, but the test and deploy path is undecided.

**Prior reviews**
- The owner's private 2026-10-08 audit archive (review.md, test-results.txt, build.log) is the Codex baseline audit.
- `ingest-2026-10-19-fab9f2b/` is the full documentation ingest.
