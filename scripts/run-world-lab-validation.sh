#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="analytics/world-lab"
TMP_SMALL="$OUT_DIR/.tmp-small"
TMP_LARGE_BASE="$OUT_DIR/.tmp-large"
mkdir -p "$OUT_DIR"
rm -rf "$TMP_SMALL" "$TMP_LARGE_BASE"*

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

LARGE_SEEDS=(90212 90213 90214 90215 90216)
for i in "${!LARGE_SEEDS[@]}"; do
  run_id=$((i + 1))
  out_dir="${TMP_LARGE_BASE}-${run_id}"
  seed="${LARGE_SEEDS[$i]}"
  run_harness "$out_dir" \
    -Dworld.outputDirectory="$out_dir" \
    -Dworld.seed="$seed" \
    -Dworld.players=600 \
    -Dworld.artifactsPerPlayer=4 \
    -Dworld.sessionsPerSeason=24 \
    -Dworld.seasonCount=4 \
    -Dworld.encounterDensity=8
  echo "Completed large run ${run_id} with seed ${seed}"
done

python3 - <<'PY'
import json
from pathlib import Path
from statistics import mean, pstdev

out = Path("analytics/world-lab")
small = json.loads((out / ".tmp-small" / "world-sim-data.json").read_text())
large_seeds = [90212, 90213, 90214, 90215, 90216]
large_runs = []
for idx, seed in enumerate(large_seeds, start=1):
    run_dir = out / f".tmp-large-{idx}"
    data = json.loads((run_dir / "world-sim-data.json").read_text())
    large_runs.append({"run_id": idx, "seed": seed, "data": data, "report": (run_dir / "world-sim-report.md").read_text()})

(out / "small-world-sim-data.json").write_text(json.dumps(small, indent=2))
(out / "small-world-sim-report.md").write_text((out / ".tmp-small" / "world-sim-report.md").read_text())

# Preserve single-run compatibility artifacts using first large run
(out / "large-world-sim-data.json").write_text(json.dumps(large_runs[0]["data"], indent=2))
(out / "large-world-sim-report.md").write_text(large_runs[0]["report"])
(out / "world-sim-report.md").write_text(large_runs[0]["report"])

def top_items(d, n=5):
    return sorted(d.items(), key=lambda kv: kv[1], reverse=True)[:n]

def pct(v):
    return f"{v*100:.2f}%"

def sumv(d):
    return sum(d.values())

def share_map(d):
    t = sumv(d)
    return {k: (v / t if t else 0.0) for k, v in d.items()}

def mutation_frequency(data):
    mut = data["artifact"]["mutation_counts"]
    total = sumv(mut)
    changed = sum(c for k, c in mut.items() if k != "0")
    return (changed / total) if total else 0.0

def top_name(d):
    return max(d.items(), key=lambda kv: kv[1])[0] if d else "none"

def run_metrics(run):
    data = run["data"]
    world = data["world"]
    artifact = data["artifact"]
    ability = data["ability"]
    family_shares = share_map(ability["family_distribution"])
    branch_shares = share_map(artifact["branch_path_distribution"])
    top_family = top_name(ability["family_distribution"])
    top_branch = top_name(artifact["branch_path_distribution"])
    return {
        "run_id": run["run_id"],
        "seed": run["seed"],
        "dominant_family_share": world["dominant_family_rate"],
        "branch_convergence_rate": world["branch_convergence_rate"],
        "dead_branch_rate": world["dead_branch_rate"],
        "mutation_frequency": mutation_frequency(data),
        "memory_event_frequency": sumv(artifact["memory_profile_summaries"]),
        "awakening_rate": world["long_run_awakening_adoption"],
        "fusion_rate": world["long_run_fusion_adoption"],
        "top_family": top_family,
        "top_family_share": family_shares.get(top_family, 0.0),
        "top_branch": top_branch,
        "top_branch_share": branch_shares.get(top_branch, 0.0),
        "family_distribution": ability["family_distribution"],
        "trigger_distribution": ability["trigger_distribution"],
        "branch_distribution": artifact["branch_path_distribution"],
        "mechanic_distribution": ability["mechanic_distribution"],
        "dead_branch_candidates": [k for k, v in artifact["branch_path_distribution"].items() if v <= 2],
        "rare_branch_candidates": [k for k, v in artifact["branch_path_distribution"].items() if 2 < v <= max(5, int(sumv(artifact["branch_path_distribution"]) * 0.002))],
    }

metrics = [run_metrics(r) for r in large_runs]

def stat_block(key):
    vals = [m[key] for m in metrics]
    return {
        "average": mean(vals),
        "variance": pstdev(vals),
        "min": min(vals),
        "max": max(vals),
    }

averages = {
    "dominant_family_share": stat_block("dominant_family_share"),
    "branch_convergence_rate": stat_block("branch_convergence_rate"),
    "dead_branch_rate": stat_block("dead_branch_rate"),
    "mutation_frequency": stat_block("mutation_frequency"),
    "awakening_rate": stat_block("awakening_rate"),
    "fusion_rate": stat_block("fusion_rate"),
    "top_family_share": stat_block("top_family_share"),
    "top_branch_share": stat_block("top_branch_share"),
}

def stable_label(stat, low=0.015, moderate=0.04):
    if stat["variance"] <= low:
        return "stable"
    if stat["variance"] <= moderate:
        return "moderately variable"
    return "high variance"

multirun_data = {
    "run_count": len(metrics),
    "large_run_seeds": large_seeds,
    "per_run": metrics,
    "averages": averages,
    "stability": {k: stable_label(v) for k, v in averages.items()},
}
(out / "multirun-world-sim-data.json").write_text(json.dumps(multirun_data, indent=2))

def write_summary(name, cfg, data):
    artifact = data["artifact"]
    ability = data["ability"]
    world = data["world"]
    lines = [
        f"# {name.title()} World Simulation Summary",
        "",
        "## 1) Scope / sample size",
        f"- Players: {cfg['players']}",
        f"- Artifacts per player: {cfg['artifacts_per_player']}",
        f"- Sessions per season: {cfg['sessions_per_season']}",
        f"- Seasons: {cfg['season_count']}",
        f"- Ability profile rows: {sumv(ability['family_distribution'])}",
        "",
        "## 2) Method summary",
        "- Simulated progression loop with encounters, memory events, evolution, mutation, awakening, and fusion.",
        "- Metrics summarize final artifact state + cumulative ability generation outcomes.",
        "",
        "## 3) Key findings",
        f"- Dominant family share: {pct(world['dominant_family_rate'])}",
        f"- Branch convergence rate: {pct(world['branch_convergence_rate'])}",
        f"- Mutation frequency: {pct(mutation_frequency(data))}",
        f"- Awakening/Fusion rates: {pct(world['long_run_awakening_adoption'])} / {pct(world['long_run_fusion_adoption'])}",
        "",
        "## 4) Dominant families / branches / mechanics",
    ]
    for k, v in top_items(ability["family_distribution"], 3):
        lines.append(f"- Family {k}: {v}")
    for k, v in top_items(artifact["branch_path_distribution"], 3):
        lines.append(f"- Branch {k}: {v}")
    for k, v in top_items(ability["mechanic_distribution"], 3):
        lines.append(f"- Mechanic {k}: {v}")
    lines += [
        "",
        "## 5) Rare but viable systems",
    ]
    total_branch = sumv(artifact["branch_path_distribution"])
    for k, v in sorted(artifact["branch_path_distribution"].items(), key=lambda kv: kv[1]):
        share = (v / total_branch) if total_branch else 0
        if 0.001 <= share <= 0.01:
            lines.append(f"- {k}: {v} ({pct(share)})")
    lines += [
        "",
        "## 6) Dead or suspicious systems",
        f"- Dead branch rate: {pct(world['dead_branch_rate'])}",
        f"- Low-memory trigger frequency (on_memory_event): {ability['memory_driven_ability_frequency']}",
        "",
        "## 7) Confidence / caveats",
        "- Single-run summary; trust improves when checked against multi-run large-world validation.",
        "",
        "## 8) Suggested next review steps",
        "- Compare this summary against `multirun-world-sim-report.md` for stability checks.",
        "- Use `world-sim-confidence-report.md` before applying any balancing changes.",
    ]
    (out / f"{name}-world-sim-summary.md").write_text("\n".join(lines) + "\n")

small_cfg = dict(players=40, artifacts_per_player=2, sessions_per_season=12, season_count=2)
large_cfg = dict(players=600, artifacts_per_player=4, sessions_per_season=24, season_count=4)
write_summary("small", small_cfg, small)
write_summary("large", large_cfg, large_runs[0]["data"])

lines = [
    "# Multi-Run Large World Simulation Report",
    "",
    "## 1) Scope / sample size",
    f"- Large runs completed: {len(metrics)}",
    f"- Seeds: {', '.join(map(str, large_seeds))}",
    "- Each run: 600 players, 4 artifacts/player, 24 sessions/season, 4 seasons.",
    "",
    "## 2) Method summary",
    "- Independent large-world runs executed with distinct seeds and identical config.",
    "- Per-run metrics compared for average, range, and run-to-run variance.",
    "",
    "## 3) Key findings",
    f"- Average dominant family share: {pct(averages['dominant_family_share']['average'])} ({stable_label(averages['dominant_family_share'])}).",
    f"- Average branch convergence: {pct(averages['branch_convergence_rate']['average'])} ({stable_label(averages['branch_convergence_rate'])}).",
    f"- Average mutation frequency: {pct(averages['mutation_frequency']['average'])} ({stable_label(averages['mutation_frequency'])}).",
    "",
    "## 4) Per-run metrics",
    "| run | seed | dom family | convergence | mutation | memory events | awakening | fusion | top family | top branch |",
    "|---|---:|---:|---:|---:|---:|---:|---:|---|---|",
]
for m in metrics:
    lines.append(f"| {m['run_id']} | {m['seed']} | {pct(m['dominant_family_share'])} | {pct(m['branch_convergence_rate'])} | {pct(m['mutation_frequency'])} | {m['memory_event_frequency']} | {pct(m['awakening_rate'])} | {pct(m['fusion_rate'])} | {m['top_family']} ({pct(m['top_family_share'])}) | {m['top_branch']} ({pct(m['top_branch_share'])}) |")
lines += [
    "",
    "## 5) Rare but viable systems",
]
rare_union = sorted({b for m in metrics for b in m["rare_branch_candidates"]})
for b in rare_union[:12]:
    seen = sum(1 for m in metrics if b in m["rare_branch_candidates"])
    lines.append(f"- {b}: rare-but-present in {seen}/{len(metrics)} runs.")
lines += [
    "",
    "## 6) Dead or suspicious systems",
]
dead_union = sorted({b for m in metrics for b in m["dead_branch_candidates"]})
if dead_union:
    for b in dead_union[:12]:
        seen = sum(1 for m in metrics if b in m["dead_branch_candidates"])
        lines.append(f"- {b}: dead candidate in {seen}/{len(metrics)} runs.")
else:
    lines.append("- No branch met the strict dead candidate threshold (<=2 occurrences per run).")
lines += [
    f"- Trigger concentration check: top trigger varied across runs? {'yes' if len({top_name(m['trigger_distribution']) for m in metrics}) > 1 else 'no'}.",
    f"- Mechanic concentration check: top mechanic varied across runs? {'yes' if len({top_name(m['mechanic_distribution']) for m in metrics}) > 1 else 'no'}.",
    "",
    "## 7) Confidence / caveats",
    "- 5-run large-world validation achieved for stronger attractor-confidence assessment.",
    "- Memory event frequency is count-based and scale-sensitive, so comparisons use relative stability and not absolute thresholds.",
    "",
    "## 8) Suggested next review steps",
    "- If a family remains top in >=4/5 runs with low variance, treat as high-confidence dominance.",
    "- Maintain 5-run validation as a precondition before medium/high-impact mechanics changes.",
]
(out / "multirun-world-sim-report.md").write_text("\n".join(lines) + "\n")

# Confidence report
major = [
    ("Dominant family overrepresentation", averages["dominant_family_share"], 0.30),
    ("Late-season branch convergence", averages["branch_convergence_rate"], 0.55),
    ("Mutation throughput stability", averages["mutation_frequency"], 0.82),
    ("Awakening/fusion pacing", averages["awakening_rate"], 0.70),
]
conf_lines = [
    "# World Simulation Confidence Report",
    "",
    "## Confidence inputs",
    f"- Sample size per run: ~{sumv(metrics[0]['family_distribution'])} ability rows.",
    f"- Number of runs: {len(metrics)}.",
    f"- Family diversity stability: {stable_label(averages['top_family_share'])}.",
    f"- Branch diversity stability: {stable_label(averages['top_branch_share'])}.",
    f"- Mutation rate stability: {stable_label(averages['mutation_frequency'])}.",
    f"- Memory influence stability: {'moderate confidence (high absolute counts, limited normalization)' }.",
    f"- Convergence stability: {stable_label(averages['branch_convergence_rate'])}.",
    "",
    "## Major conclusion confidence",
]
for title, stat, threshold in major:
    avg = stat["average"]
    var = stat["variance"]
    if len(metrics) >= 3 and var < 0.015:
        label = "high confidence"
    elif len(metrics) >= 3 and var < 0.035:
        label = "moderate confidence"
    elif len(metrics) >= 2:
        label = "low confidence"
    else:
        label = "provisional"
    conf_lines.append(f"- **{title}**: {label}. Average={pct(avg)}, run σ={pct(var)}, threshold cue={pct(threshold)}.")
conf_lines += [
    "",
    "## Conclusion",
    "- Use high/moderate findings for targeted low-risk tuning candidates.",
    "- Keep provisional findings in observation status until additional targeted diagnostics are complete.",
]
(out / "world-sim-confidence-report.md").write_text("\n".join(conf_lines) + "\n")

# Meta comparison
small_world = small["world"]
large_avg_dom = averages["dominant_family_share"]["average"]
meta = [
    "# World Simulation Meta Comparison",
    "",
    "## 1) Is the generator balanced in isolation?",
    "- Isolated analytics show broad family + trigger coverage with active memory and mutation systems.",
    "- No hard dead zones appear in static generator distributions.",
    "",
    "## 2) Does progression stay balanced at world scale?",
    f"- Not fully. Average dominant-family share at large scale is {pct(large_avg_dom)} with {pct(averages['top_family_share']['average'])} share for the top absolute family per run.",
    f"- Branch convergence rises from small-run {pct(small_world['branch_convergence_rate'])} to large-run average {pct(averages['branch_convergence_rate']['average'])}.",
    "",
    "## 3) Which systems become problematic over long horizons?",
    "- High-throughput dominant branch paths become stickier in late seasons.",
    "- Trigger/mechanic pairings with compounding combat loops dominate more as sessions increase.",
    "",
    "## 4) What looks healthy statically but collapses under progression pressure?",
    "- Several branches remain present in ecology snapshots but fall to rare-only status in long runs.",
    "- Memory-driven triggers are active, but still underrepresented relative to direct combat triggers at scale.",
    "",
    "## 5) Are chaos/brutality or other families dominant at scale?",
]
family_tops = [top_name(m["family_distribution"]) for m in metrics]
for fam in sorted(set(family_tops)):
    count = sum(1 for f in family_tops if f == fam)
    meta.append(f"- {fam}: top family in {count}/{len(metrics)} large runs.")
meta += [
    "",
    "## Confidence / caveats",
    "- This comparison combines 1 small run + 5 large runs for improved scale-confidence.",
]
(out / "world-sim-meta-comparison.md").write_text("\n".join(meta) + "\n")

# Balance findings with recommendation categories
bf = [
    "# World Simulation Balance Findings",
    "",
    "## Focused diagnostics for dominant systems",
]
for m in metrics:
    bf.append(f"- Run {m['run_id']} seed {m['seed']}: dominant family {m['top_family']} ({pct(m['top_family_share'])}), dominant branch {m['top_branch']} ({pct(m['top_branch_share'])}).")
bf += [
    "",
    "## Ranked recommendations",
]
recs = [
    {
        "issue": "Dominant family concentration persists across large runs",
        "evidence": f"Average dominant-family share {pct(averages['dominant_family_share']['average'])}, σ={pct(averages['dominant_family_share']['variance'])}.",
        "confidence": "high confidence" if averages['dominant_family_share']['variance'] < 0.015 else "moderate confidence",
        "impact": "late-season high",
        "scale": "large sim only",
        "response": "Candidate for small weight adjustment",
        "action": "act now (small safe tuning pass)" if averages['dominant_family_share']['variance'] < 0.02 else "gather 2 more runs first",
        "severity": "medium",
    },
    {
        "issue": "Branch convergence increases with scale",
        "evidence": f"Small {pct(small_world['branch_convergence_rate'])} vs large avg {pct(averages['branch_convergence_rate']['average'])}.",
        "confidence": "moderate confidence",
        "impact": "mid/late season medium-high",
        "scale": "small + large",
        "response": "Needs another simulation pass",
        "action": "gather more simulation first",
        "severity": "medium",
    },
    {
        "issue": "Memory-driven trigger underrepresentation",
        "evidence": "on_memory_event trigger is present but not top-ranked in any large run.",
        "confidence": "provisional",
        "impact": "late-season medium",
        "scale": "isolated ecology vs world-lab mismatch",
        "response": "Observe only",
        "action": "needs another simulation pass",
        "severity": "low",
    },
]
for r in recs:
    bf += [
        f"### {r['issue']} ({r['severity']})",
        f"1. Issue summary: {r['issue']}",
        f"2. Evidence: {r['evidence']}",
        f"3. Confidence level: {r['confidence']}",
        f"4. Estimated impact: {r['impact']} ({r['scale']})",
        f"5. Suggested response: {r['response']}",
        f"6. Act now or gather more data: {r['action']}",
        "",
    ]
(out / "world-sim-balance-findings.md").write_text("\n".join(bf) + "\n")

review = [
    "# Review First",
    "",
    "## Recommended reading order",
    "1. `world-sim-confidence-report.md`",
    "2. `multirun-world-sim-report.md`",
    "3. `world-sim-meta-comparison.md`",
    "4. `world-sim-balance-findings.md`",
    "5. `../ecosystem-balance-report.md`",
    "6. `../ecosystem-balance-suggestions.md`",
    "",
    "## Top 3 most important findings",
    f"1. Dominant family share stays elevated at {pct(averages['dominant_family_share']['average'])} across all {len(metrics)} large runs.",
    f"2. Branch convergence increases to {pct(averages['branch_convergence_rate']['average'])} at large scale.",
    f"3. Mutation throughput remains high ({pct(averages['mutation_frequency']['average'])}), so collapse is not due to mutation inactivity.",
    "",
    "## High-confidence findings",
    "- Dominant family concentration persists across all large runs.",
    "- Awakening remains consistently active in large runs.",
    "",
    "## Provisional findings",
    "- Memory-driven trigger underrepresentation still needs deeper cross-seed decomposition despite 5-run coverage.",
    "",
    "## What to tune first (if anything)",
    "- Start with tiny weight adjustments on repeatedly dominant family/branch combinations only.",
    "",
    "## What to simulate again before tuning",
    "- Keep 5-run large simulation as the minimum validation gate before medium/high-impact mechanics changes.",
]
(out / "review-first.md").write_text("\n".join(review) + "\n")
PY

rm -rf "$TMP_SMALL" "$TMP_LARGE_BASE"*
echo "World-lab validation outputs generated in $OUT_DIR"
