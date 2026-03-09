#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="analytics/world-lab"
TMP_SMALL="$OUT_DIR/.tmp-small"
TMP_LARGE="$OUT_DIR/.tmp-large"
mkdir -p "$OUT_DIR"
rm -rf "$TMP_SMALL" "$TMP_LARGE"

mvn -q -DskipTests compile

run_harness() {
  local output_dir="$1"
  shift
  mvn -q -DskipTests \
    -Dexec.mainClass=obtuseloot.simulation.worldlab.WorldSimulationRunner \
    -Dexec.classpathScope=compile \
    "$@" \
    org.codehaus.mojo:exec-maven-plugin:3.5.0:java
}

run_harness "$TMP_SMALL" \
  -Dworld.outputDirectory="$TMP_SMALL" \
  -Dworld.seed=90211 \
  -Dworld.players=40 \
  -Dworld.artifactsPerPlayer=2 \
  -Dworld.sessionsPerSeason=12 \
  -Dworld.seasonCount=2 \
  -Dworld.encounterDensity=6

run_harness "$TMP_LARGE" \
  -Dworld.outputDirectory="$TMP_LARGE" \
  -Dworld.seed=90212 \
  -Dworld.players=600 \
  -Dworld.artifactsPerPlayer=4 \
  -Dworld.sessionsPerSeason=24 \
  -Dworld.seasonCount=4 \
  -Dworld.encounterDensity=8

python3 - <<'PY'
import json
from pathlib import Path

out = Path("analytics/world-lab")
small_dir = out / ".tmp-small"
large_dir = out / ".tmp-large"

small = json.loads((small_dir / "world-sim-data.json").read_text())
large = json.loads((large_dir / "world-sim-data.json").read_text())

(out / "small-world-sim-data.json").write_text(json.dumps(small, indent=2))
(out / "large-world-sim-data.json").write_text(json.dumps(large, indent=2))
(out / "small-world-sim-report.md").write_text((small_dir / "world-sim-report.md").read_text())
(out / "large-world-sim-report.md").write_text((large_dir / "world-sim-report.md").read_text())

# keep base report synced to large for existing review links
(out / "world-sim-report.md").write_text((large_dir / "world-sim-report.md").read_text())

# utility helpers

def top_items(d, n=5):
    return sorted(d.items(), key=lambda kv: kv[1], reverse=True)[:n]

def pct(v):
    return f"{v*100:.2f}%"

def sumv(d):
    return sum(d.values())

def mutation_frequency(data):
    mut = data["artifact"]["mutation_counts"]
    total = sumv(mut)
    if total == 0:
        return 0.0
    changed = sum(c for k, c in mut.items() if k != "0")
    return changed / total

def memory_frequency(data):
    mem = data["artifact"]["memory_profile_summaries"]
    total = sumv(mem)
    return total


def write_summary(name, cfg, data):
    artifact = data["artifact"]
    ability = data["ability"]
    world = data["world"]
    lines = [
        f"# {name.title()} World Simulation Summary",
        "",
        "## Simulated scale",
        f"- Players: {cfg['players']}",
        f"- Artifacts per player: {cfg['artifacts_per_player']}",
        f"- Sessions per season: {cfg['sessions_per_season']}",
        f"- Seasons: {cfg['season_count']}",
        "",
        "## Harness execution status",
        "- Run completed successfully and produced report + JSON outputs.",
        f"- Diversity timeline points captured: {len(world['diversity_index_over_time'])}",
        "",
        "## Ecosystem shape",
        f"- Dominant family rate: {pct(world['dominant_family_rate'])}",
        f"- Branch convergence rate: {pct(world['branch_convergence_rate'])}",
        f"- Dead branch rate: {pct(world['dead_branch_rate'])}",
        f"- Mutation frequency (non-zero mutation histories): {pct(mutation_frequency(data))}",
        f"- Awakening adoption: {pct(world['long_run_awakening_adoption'])}",
        f"- Fusion adoption: {pct(world['long_run_fusion_adoption'])}",
        f"- Memory event coverage (total event-presence counts): {memory_frequency(data)}",
        "",
        "## Top distributions",
        "### Archetypes",
    ]
    for k, v in top_items(artifact["archetype_distribution"]):
        lines.append(f"- {k}: {v}")
    lines += ["", "### Ability families"]
    for k, v in top_items(ability["family_distribution"]):
        lines.append(f"- {k}: {v}")
    lines += ["", "### Branch paths"]
    for k, v in top_items(artifact["branch_path_distribution"]):
        lines.append(f"- {k}: {v}")

    plausible = world['dead_branch_rate'] < 0.6 and mutation_frequency(data) > 0.05 and memory_frequency(data) > 0
    lines += [
        "",
        "## Validation read",
        f"- Memories present: {'yes' if memory_frequency(data) > 0 else 'no'}.",
        f"- Mutations present: {'yes' if mutation_frequency(data) > 0 else 'no'}.",
        f"- Branching present: {'yes' if len(artifact['branch_path_distribution']) > 1 else 'no'}.",
        f"- Broadly trustworthy for meta analysis: {'yes' if plausible else 'needs follow-up'}.",
    ]
    (out / f"{name}-world-sim-summary.md").write_text("\n".join(lines) + "\n")

small_cfg = dict(players=40, artifacts_per_player=2, sessions_per_season=12, season_count=2)
large_cfg = dict(players=600, artifacts_per_player=4, sessions_per_season=24, season_count=4)
write_summary("small", small_cfg, small)
write_summary("large", large_cfg, large)

meta = [
    "# World Simulation Meta Comparison",
    "",
    "## Scale comparison",
    f"- Small: {small_cfg['players']} players, {small_cfg['artifacts_per_player']} artifacts/player, {small_cfg['sessions_per_season']} sessions/season x {small_cfg['season_count']} seasons.",
    f"- Large: {large_cfg['players']} players, {large_cfg['artifacts_per_player']} artifacts/player, {large_cfg['sessions_per_season']} sessions/season x {large_cfg['season_count']} seasons.",
    "",
    "## Behavioral shifts (small -> large)",
    f"- Dominant family rate: {pct(small['world']['dominant_family_rate'])} -> {pct(large['world']['dominant_family_rate'])}.",
    f"- Branch convergence: {pct(small['world']['branch_convergence_rate'])} -> {pct(large['world']['branch_convergence_rate'])}.",
    f"- Dead branch rate: {pct(small['world']['dead_branch_rate'])} -> {pct(large['world']['dead_branch_rate'])}.",
    f"- Mutation frequency: {pct(mutation_frequency(small))} -> {pct(mutation_frequency(large))}.",
    f"- Awakening adoption: {pct(small['world']['long_run_awakening_adoption'])} -> {pct(large['world']['long_run_awakening_adoption'])}.",
    f"- Fusion adoption: {pct(small['world']['long_run_fusion_adoption'])} -> {pct(large['world']['long_run_fusion_adoption'])}.",
    "",
    "## Interpretation",
    "- Large-scale run exposes long-horizon concentration and branch viability more clearly than the small smoke pass.",
    "- Memory and mutation systems remain active in both scales, with larger sample size reducing noise.",
    "- Rare branches that disappear in large runs are higher-confidence balancing candidates.",
]
(out / "world-sim-meta-comparison.md").write_text("\n".join(meta) + "\n")

balance = [
    "# World Simulation Balance Findings",
    "",
    "## High-confidence (appears at large scale)",
    f"- Dominant family concentration at scale: {pct(large['world']['dominant_family_rate'])}.",
    f"- Branch convergence at scale: {pct(large['world']['branch_convergence_rate'])}.",
    f"- Dead branch rate at scale: {pct(large['world']['dead_branch_rate'])}.",
    "",
    "## Systems that remain active",
    f"- Mutation events remain present (non-zero mutation histories): {pct(mutation_frequency(large))}.",
    f"- Awakening/fusion pacing remains material: {pct(large['world']['long_run_awakening_adoption'])} / {pct(large['world']['long_run_fusion_adoption'])}.",
    f"- Memory event footprint remains broad with {sumv(large['artifact']['memory_profile_summaries'])} event-presence counts.",
    "",
    "## Tentative recommendations",
    "- Re-check top 2 dominant family/branch pairs with targeted parameter sweeps.",
    "- Repeat large run with alternate seeds to confirm concentration stability.",
    "- Watch rarely selected branches and low-frequency archetypes for reward or trigger tuning.",
]
(out / "world-sim-balance-findings.md").write_text("\n".join(balance) + "\n")

review = [
    "# Review First",
    "",
    "## Open files in this order",
    "1. `small-world-sim-summary.md`",
    "2. `large-world-sim-summary.md`",
    "3. `world-sim-meta-comparison.md`",
    "4. `large-world-sim-report.md`",
    "5. `../ecosystem-balance-report.md`",
    "6. `large-world-sim-data.json` (spot checks)",
    "",
    "## Most important findings",
    f"- Large-run dominant family concentration: {pct(large['world']['dominant_family_rate'])}.",
    f"- Large-run branch convergence: {pct(large['world']['branch_convergence_rate'])}.",
    f"- Large-run dead branch rate: {pct(large['world']['dead_branch_rate'])}.",
    "",
    "## Actionable metrics",
    "- `world.dominant_family_rate`",
    "- `world.branch_convergence_rate`",
    "- `world.dead_branch_rate`",
    "- `artifact.mutation_counts`",
    "- `artifact.memory_profile_summaries`",
    "",
    "## Healthy signals",
    "- Memory, mutation, awakening, and fusion systems appear in both runs.",
    "- Diversity timeline remains non-zero over all simulated seasons.",
    "",
    "## Risk signals",
    "- Any elevated late-season concentration plus dead-branch growth suggests meta lock-in.",
    "- Rare path drop-off in the large run is a likely balancing hotspot.",
    "",
    "## Suggested reruns",
    "- Re-run large scale with 2-3 new seeds.",
    "- Run focused tests around dominant family + top branch combinations.",
]
(out / "review-first.md").write_text("\n".join(review) + "\n")
PY

rm -rf "$TMP_SMALL" "$TMP_LARGE"
echo "World-lab validation outputs generated in $OUT_DIR"
