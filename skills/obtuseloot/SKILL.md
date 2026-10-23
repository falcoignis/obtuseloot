---
name: obtuseloot
description: Work on ObtuseLoot (falcoignis/obtuseloot), the Paper/Purpur evolving-artifact plugin and its simulation/analytics tools. Use for /ol or /obtuseloot commands, abilities, genomes, evolution, niches, lineage, telemetry, persistence, world-lab, and repository work. Preserves owner decisions and project workflow; grants no live deployment authority.
---

# ObtuseLoot

ObtuseLoot is a Java Paper/Purpur plugin. Gear ("artifacts") evolves from how players use it: reputation, drift, awakening, convergence, procedurally generated abilities, lineages, and an ecology of niches. It also ships an offline simulation and analytics toolchain.

- Repo: `falcoignis/obtuseloot`. It is **public**, so never put secrets, homelab paths or HomesteadCraft internals into commits, PRs or issues.
- Not deployed yet. It is planned for HomesteadCraft.

## Where this skill lives

- **Canonical copy:** `skills/obtuseloot/` in the [`falcoignis/obtuseloot` repository](https://github.com/falcoignis/obtuseloot). This supersedes the earlier external-only location (owner decision, 2026-10-20).
- **Installed copies:** the Claude and Codex user skill folders are mirrors. Diff them against the checked-out repository skill before refreshing; preserve unique changes for review rather than overwriting them silently.
- **Other mirrors and backups are not authoritative.** Make skill and decision changes in this repository through the same reviewed commit/PR workflow as other project changes.

When a decision changes, update this skill's `references/decisions.md` in the repository, then refresh the installed copies. Keep private host configuration and credentials outside the repository.

## The goal, and why it shapes every change

The owner's core goal is **a truly emergent evolution engine**. Emergence counts only when all four criteria in `references/decisions.md` hold:
- diversity survives ablation of the aids;
- lineages are actually born and go extinct;
- niches can appear beyond the fixed set;
- survival is driven by measured in-game outcomes.

The owner's second goal is **a healthy ecosystem with a wide range of options and niches**. The two goals go together: diversity has to emerge and be measured, not be imposed. A collapse in diversity is a real defect to fix through mechanisms such as outcome-driven competition and niche partitioning. It is never fixed by adding a quota.

The codebase history went the other way. Between 2026-03-18 and 03-24, about 20 corrective layers were added to make distribution numbers pass: normalization, top-3 compression, cold boosts, forced bifurcation, and one-occupant reconciliation that satisfies its own success metric by construction. The docs then reported those numbers as success.

The owner has ruled that those layers are scaffolding to remove, and that the distribution targets are tuning artifacts. That gives these working rules:

- **Don't tune the engine to make a distribution test pass.** If a reachability or top-share assertion fails, first decide whether it reveals a bug (crash, wrong wiring, lost data) or just a different outcome distribution. A different distribution is not a defect. Bring it to the owner rather than adding a weight, floor or boost. Each such patch is one more layer that has to be removed later.
- **Don't add top-down control.** That means caps, quotas, floors, forced placement, normalization toward the mean, or hand-written niche adjacency. If one truly seems necessary, say plainly that it trades away emergence and ask first.
- **Prefer wiring real outcomes into selection.** The existing outcome-driven hooks are the utility-history blend, `ExperienceEvolutionEngine` feedback, lineage affinity from child-niche outcomes, and reputation. Build on those.
- **Treat every historical claim as history.** Docs, `analytics/` output and SUCCESS verdicts are not evidence about current code. Some cited datasets don't exist in git. Metrics changed meaning partway through. The open-endedness verdict is a string literal. Only a run on current code, with the run recorded, counts.

## Roadmap (owner-decided order)

1. **Green build and safety fixes:** signed seeds, analytics fixtures, storage-load failure (fatal at startup; at runtime refuse that player's artifact and retry), the permission root gate. Baseline safety fixes landed in PR #400; see `references/status.md` for the probe/health follow-up and current PR state.
2. **Real ability effects** with outcome feedback into selection. Prioritize social, interaction and exploration; keep combat effects minimal (owner, 2026-10-20). Gentle Harvest and Material Insight implement the first slice. The follow-up adds game-tick noncombat timing and a shared catalog for easy ability add/remove; see `docs/noncombat-evolution-timing.md`. See `references/status.md` for validation and refresh publication/merge state before relying on it.
3. **Per-item identity and reproduction.** The owner approved new loot inheriting from successful local lineages, with variation, and a configurable **20-chunk radius**. The follow-up adds persistent item genomes/reputation/ancestry and local measured-outcome parent selection. Read `docs/local-artifact-inheritance.md` and refresh the PR state in `references/status.md`. No migration is needed; no player data exists anywhere.
4. **Remove scaffolding,** each layer behind an ablation test. The first follow-up
   removes the final top-three weight compressor. The next removes gamma/mean
   score normalization while retaining the soft cap for its own later ablation.
   Read `docs/top-three-compression-ablation.md`, `docs/score-normalization-ablation.md`
   and refresh their review/validation state; other aids remain.
   On 2026-10-22 the owner selected the direct blanket gene-reinforcement ablation next;
   read `docs/blanket-gene-reinforcement-ablation.md`. The soft cap remains a later candidate.
   The owner then approved the remaining blanket ecological gene-multiplier ablation;
   read `docs/ecological-gene-multiplier-ablation.md`. Separate environmental modifiers remain.
   The first ecosystem-health follow-up adds a local read-only census and a multi-seed
   neutral/overlapping-habitat baseline; read `docs/local-ecosystem-health.md`. It changes
   no reproduction weights and does not declare the ecosystem healthy from diversity alone.
   The owner then approved habitat-aware competition; read `docs/habitat-aware-competition.md`.
   Native validation exposed a capture-height alias; the correction measures column
   span in a stable frame. Read `docs/native-habitat-validation.md` for current validation.
5. **Tooling:** split into two Maven modules (plugin, offline tooling), rewire the Python pipeline to Java, redefine the metrics, untrack `analytics/`, fast/full test tiers.

Unless the owner says otherwise, work from the top. When a request touches a later item, say where it sits in this order.

**Equipment/integration follow-up, 2026-10-21:** the owner selected passive speed first,
with strongest or additive stacking, followed by double jump. The follow-up implements
six-slot activation and PlaceholderAPI/effective LuckPerms permission support; refresh
its validation/PR state before relying on it. Core must work standalone. Detect AuraSkills
when present and preserve normal skill XP; extra XP coupling remains undecided. PacketEvents
is conditional on demonstrated need. Read `docs/equipment-ability-activation.md` and
`docs/plugin-compatibility.md`. Compatibility is a standing requirement for future features,
not a claim that boot detection proves cross-plugin certification. The next native Double Jump
follow-up is opt-in; read `docs/double-jump.md` and refresh its current test/PR state.
The owner selected a disposable local Purpur 26.2 server for movement validation.

## Working procedure

### Get the code

Work from a **fresh clone of GitHub per task** in a temp or scratchpad directory. Use the remote repository as the source; do not develop against stale external snapshots.

```bash
git clone https://github.com/falcoignis/obtuseloot.git "$WORK/obtuseloot"
```

The full clone includes about 299 MiB of tracked `analytics/` output until that is untracked.

For interrupted work, recover the recorded branch or backup before discarding a checkout; see `references/status.md`.

### Ignore the repo's own guidance where it's stale

The root `AGENTS.md` and `CLAUDE.md` are entrypoints to this canonical skill. The historical pre-migration `CLAUDE.md` at `fab9f2b` described an unrelated Maven proxy and stale permissions/listeners. Do not revive that environment-specific setup without evidence that the current environment needs it.

Other stale or misleading files (decision: refresh or archive them via PR):

| File | Problem |
|---|---|
| `docs/NEWCOMER_GUIDE.md` | Describes the removed SoulEngine architecture |
| `docs/EVOLUTION_FUSION_TABLE.md` | Fusion recipes and keys don't exist. Stage names are wrong (real: `-formed/-tempered/-advanced`, 4 generic hybrids). `paragon` is never produced |
| `releases/v0.9.50-beta/RELEASE_NOTES.md` | Claims features absent at the tag; says seven reputation axes (code has six) |
| `docs/pr-split-plan.md` | Describes a deleted script |
| `docs/AUDIT.md` | Enforcer and CI claims since reverted |
| `simulation/*/README.md` | Describe the Python pipeline as if it ran the plugin |

`commands and permissions.md` is at the **repo root**, not in `docs/`. It is mostly accurate.

### Build and test

Use the official Maven image in a disposable container. Never install or modify a custom image. The owner selected Minecraft 26.2 on 2026-10-21; use Java 25 for the build:

```bash
docker volume create obtuseloot-m2
docker run --rm -v obtuseloot-m2:/m2 alpine chown 1000:1000 /m2
docker run --rm --user 1000:1000 -v "$PWD":/src -w /src \
  -v obtuseloot-m2:/var/maven/.m2 -e MAVEN_CONFIG=/var/maven/.m2 \
  maven:3.9-eclipse-temurin-25 \
  mvn -B -ntp -Duser.home=/var/maven clean package
```

- **The Java tag follows the build target.** Target Minecraft 26.2 and Java 25 per the owner decision. Keep the Maven API dependency, compiler release, plugin API version and all Java CI jobs aligned. See `docs/minecraft-26.2-target.md` for migration evidence and limits.
- **Iterate on single tests** with `-Dtest=ClassName -Dsurefire.failIfNoSpecifiedTests=false`. The pom also has a `fast` profile (`-Pfast`, sets `maven.test.skip=true`) that skips test compilation and execution.
- **The full suite is long-running.** The PR #403 local run took about 21 minutes; its changes remove the target-only long-horizon test. Check the current branch before assuming that deletion is present. Run executable-code/test changes through the full suite before a PR; validate skill/documentation-only changes with the skill validator and reference checks.
- **Judge a run by Maven's exit code and the surefire reports.** Grepped log lines are not enough. A shell pipeline that prints the log after Maven fails can still exit 0.
- **Revert tracked files the tests rewrite, after every test run:** `git checkout -- docs/deep-cold-completion-validation-final.md analytics/`. The full suite rewrites that doc. Even a targeted run including `WorldSimulationHarnessValidationProfileTest` rewrote 5 tracked `analytics/` files on 2026-10-19 (regulatory-gate reports, world-lab gate review, a dashboard meta json). Check `git status` before every commit. The decision is that tests write their reports to `target/` or a temp dir instead; until that lands, never commit the regenerated version.
- **Probe reproducibility (issue #401).** PR #402 derives owner UUIDs from seeds. PR #403 adds enum-ordered genomes and isolates probe population history; its focused/full outputs matched across separate JVMs. Check which changes are merged on the working branch. Never attribute a count difference to a change without controlling inputs and repeating the run.
- **Historical sensing-probe flake:** 16 hits on 2026-10-08 and 27 on 2026-10-19. The owner removed its `>= 18` assertion in the baseline PR. Fix uncontrolled inputs rather than tuning the engine to hit a distribution target.
- **Cleanup:** remove `target/` and any temp clone when done. The `obtuseloot-m2` volume is a reusable cache; mention it rather than silently leaving it.

### Changes, commits, PRs

- **Grouped PRs.** Batch related fixes, for example one "restore green baseline" PR. The owner reviews and merges unless explicitly authorizing a particular merge. Never infer standing merge permission from a one-batch authorization; see `references/decisions.md`.
- **Show the owner the diff and the test result before pushing or opening a PR.**
- No branch naming convention has been decided. History uses `codex/<task>` and `claude/<task>-<id>`.
- Every commit carries `Co-Authored-By` plus the agent's `Claude-Session` or `Codex-Session` trailer. For agent-initiated GitHub merges, supply those trailers in the merge-message body using `gh pr merge --body-file`; default merge messages did not include them in the #413–#416 closeout.
- **Releases** use `v`-prefixed tags through `release.yml`. The next release bumps the version and gets corrected notes. Leave `0.9.50-beta` alone.

## Key engine facts

These are the facts most likely to matter. The full verified map, with the scaffolding inventory and the unverified-inference table, is `references/engine-map.md`.

- **Seeds.** Artifact seeds come from `nextLong()`, so about half are negative. Any non-zero `long` is valid, and `0` means "not an artifact". **Never mask, `abs` or remap a seed;** that merges distinct identities. Seeds are stored signed in SQL BIGINT and in the item PDC.
- **First measured effects:** Gentle Harvest replants with an existing drop after protection checks; Material Insight reports the inspected block's material, light and growth. Unfinished live effects have no measurement. Live selection uses implemented catalog entries and retains executable originals for unsupported variants; direct unsupported definitions return no-op. Offline classification is marked separately in telemetry and is not evidence of gameplay success. Read `docs/measured-ability-effects.md` in the repository for limitations and required server validation.
- **Heritable performance:** the owner requires inherited genes to change real benefits and tradeoffs. The first follow-up expresses Double Jump kinetic bias through a catalog-owned output/recovery model and feeds observed response/cost into item utility and local reproduction. Material Insight adds precision-driven information depth versus persisted per-item recovery; read `docs/material-insight-performance.md`. Other effects remain fixed/configurable; read `docs/heritable-ability-performance.md` and refresh its test/review state. This does not establish natural niche formation. The durable-evolution follow-up moves genetic changes out of profile lookup and into explicit state transitions, with saved revisions and activation flags; read `docs/durable-genome-evolution.md`.
- **Revision fitness:** measured genetic experience and local reproductive evidence belong to the producing seed/revision; only current genes enter reproduction. Offspring retain their parent seed/revision and start with empty fitness. Read `docs/revision-specific-fitness.md`.
- **Genetic reinforcement ablations:** aggregate-fitness and blanket ecological/context scaling no longer prescribe a direction for every gene. Ordinary mutation, measured local reproduction, existing ecological mutation controls and separate environmental modifiers remain. Controlled swap experiments report parentage, performance, costs and ancestry diversity without imposing target distributions; read `docs/blanket-gene-reinforcement-ablation.md` and `docs/ecological-gene-multiplier-ablation.md`.
- **Local health diagnostics:** `/ol debug ecosystem health [player]` reads current measured seed/revisions within the configured chunk radius/window. It separates breeding eligibility, lineage/genetic variation and observed performance without an overall healthy verdict. Eight-seed neutral and overlapping-habitat controls distinguish ancestry drift, specialization and habitat mixing. Read `docs/local-ecosystem-health.md`; the next habitat-aware follow-up conditions current-revision competition on captured overhead structure. Read `docs/habitat-aware-competition.md` for the first model. Native capture uses a stable column span in the follow-up; read `docs/native-habitat-validation.md` for current validation and limitations.
- **Ecology runs live, not only in simulation.** It is timed by the wall clock (5 s evaluation throttle, 60 s cooldown), and its state is probably not persisted (inference). The decision is to move it to game time and persist it.
- **Storage failure safety.** PR #400 replaced failure-as-missing-record behavior: startup storage failure is fatal; runtime failure refuses that player's state and retries. Failed saves stay dirty. Never regenerate an identity on a failed load; verify current code against the decision ledger.
- **Permissions.**
  - PR #400 removed the OP-only root gate so everyone can reach `help` and `info`; privileged subcommands keep their checks.
  - Console may bypass debug permission; OPs may not. PR #400 implements this requirement.
  - `obtuseloot.inspect` is a dead node.
- **Config the live plugin never reads.** `analytics.ecology.*` is never read live; the decision is to delete it. `/ol reload` refreshes only RuntimeSettings and the name pools; the decision is a full safe reload.
- **Dashboard.** The HTTP server binds every interface with no auth; the decision is to remove it and keep static reports fed by Java output.
- **Python tooling.** The chaos, population and meta "simulation" scripts and `benchmark.yml` run a Python generator of synthetic rows, not the plugin; the decision is to rewire them to Java.

## Evidence discipline for this project

- **Label inferences.** The engine map labels everything it didn't verify; keep that separation in your own reports. Several plausible-sounding conclusions in the repo's docs turned out to be wrong when checked against source.
- **Cite file:line from the clone you're working in.** Line numbers in this skill were taken at `fab9f2b` and will drift.
- **Check facts yourself.** Before saying CI is green, a feature exists, or a release has assets, check `gh` or the clone.

## Deployment

Read `references/deployment.md`. Anything touching HomesteadCraft must **load the `homesteadcraft` skill first** and follow its consent rules. This skill grants no write authority there. The test and deploy path is **undecided**, so ask.

## Reference files

| File | Read it when |
|---|---|
| `references/decisions.md` | Before any design, behavior or process choice. It is the owner's authoritative ledger, and it lists the still-open questions |
| `references/engine-map.md` | Before changing engine code. It has the pipeline, the ecology, the metric formulas, the scaffolding removal list and the unverified inferences |
| `references/status.md` | At the start of a task. It has the dated repo, CI and release state and the paused-branch details. Re-verify before relying on it |
| `references/deployment.md` | For anything about installing or configuring on a server |
| `references/ingest-2026-10-19-fab9f2b/A-core.md` | Commands, permissions, config keys and defaults, release history, stale-doc details |
| `references/ingest-2026-10-19-fab9f2b/B-audits.md` | The de-duplicated open-issue ledger (35 rows), CI/workflow behavior, the scripts inventory, the process history |
| `references/ingest-2026-10-19-fab9f2b/C-abilities.md` | Ability generation in depth, per-doc history of every corrective layer, acceptance metrics and where they're enforced |
| `references/ingest-2026-10-19-fab9f2b/D-ecology.md` | Niches, bifurcation, child-niche aids, the validation harness, rollup metrics, missing datasets |
| `references/ingest-2026-10-19-fab9f2b/scripts/jrand*.py` | Reproducing the correlated-seed analysis of the forced probes (Python reimplementation of `java.util.Random`) |
| External private review archives | Ask the owner for access if original baseline evidence is needed; private archive locations are not part of the published skill |
